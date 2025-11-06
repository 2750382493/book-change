<template>
  <div class="messages">
    <h1>消息中心</h1>
    
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <h3>对话列表</h3>
          <div v-if="conversations.length === 0" class="no-data">
            <p>暂无消息</p>
          </div>
          <div v-else>
            <div v-for="conv in conversations" :key="conv.id" 
                 class="conversation-item"
                 :class="{ active: activeConversation && activeConversation.id === conv.id }"
                 @click="selectConversation(conv)">
              <div class="conversation-header">
                <span class="user-name">{{ getOtherUserName(conv) }}</span>
                <span class="book-title">{{ conv.book.title }}</span>
              </div>
              <p class="last-message">{{ getLastMessage(conv) }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="16">
        <el-card v-if="activeConversation">
          <h3>与 {{ getOtherUserName(activeConversation) }} 的对话</h3>
          <div class="message-list">
            <div v-for="msg in messages" :key="msg.id" 
                 class="message-item"
                 :class="{ 'own-message': msg.fromUser.id === currentUser.id }">
              <div class="message-content">
                <p>{{ msg.content }}</p>
                <span class="message-time">{{ formatTime(msg.createTime) }}</span>
              </div>
            </div>
          </div>
          
          <div class="message-input">
            <el-input
              v-model="newMessage"
              type="textarea"
              :rows="3"
              placeholder="输入消息..."
            />
            <el-button type="primary" @click="sendMessage" style="margin-top: 10px;">发送</el-button>
          </div>
        </el-card>
        
        <el-card v-else>
          <div class="no-conversation">
            <p>请选择一个对话</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'Messages',
  setup() {
    const conversations = ref([])
    const messages = ref([])
    const activeConversation = ref(null)
    const newMessage = ref('')
    const currentUser = ref({})

    const loadCurrentUser = () => {
      const userData = localStorage.getItem('user')
      if (userData) {
        currentUser.value = JSON.parse(userData)
        fetchConversations()
      }
    }

    const fetchConversations = async () => {
      // 这里需要根据实际API调整
      // 暂时使用获取所有消息然后分组的方式
      try {
        const response = await axios.get(`/api/messages/unread/${currentUser.value.id}`)
        // 简单处理：将消息按发送者分组作为对话
        const messages = response.data
        const convMap = new Map()
        
        messages.forEach(msg => {
          const otherUser = msg.fromUser.id === currentUser.value.id ? msg.toUser : msg.fromUser
          const key = `${otherUser.id}-${msg.book.id}`
          
          if (!convMap.has(key)) {
            convMap.set(key, {
              id: key,
              otherUser,
              book: msg.book,
              messages: []
            })
          }
          convMap.get(key).messages.push(msg)
        })
        
        conversations.value = Array.from(convMap.values())
      } catch (error) {
        console.error('获取对话列表失败', error)
      }
    }

    const getOtherUserName = (conversation) => {
      return conversation.otherUser.nickname || conversation.otherUser.username
    }

    const getLastMessage = (conversation) => {
      const lastMsg = conversation.messages[conversation.messages.length - 1]
      return lastMsg ? lastMsg.content : '暂无消息'
    }

    const selectConversation = async (conversation) => {
      activeConversation.value = conversation
      // 加载该对话的完整消息历史
      try {
        const response = await axios.get('/api/messages/conversation', {
          params: {
            user1Id: currentUser.value.id,
            user2Id: conversation.otherUser.id,
            bookId: conversation.book.id
          }
        })
        messages.value = response.data
      } catch (error) {
        console.error('获取消息历史失败', error)
      }
    }

    const sendMessage = async () => {
      if (!newMessage.value.trim()) {
        ElMessage.warning('消息内容不能为空')
        return
      }

      try {
        const message = {
          fromUser: currentUser.value,
          toUser: activeConversation.value.otherUser,
          book: activeConversation.value.book,
          content: newMessage.value
        }
        
        await axios.post('/api/messages', message)
        newMessage.value = ''
        // 重新加载消息
        selectConversation(activeConversation.value)
      } catch (error) {
        ElMessage.error('发送失败')
        console.error('发送消息失败', error)
      }
    }

    const formatTime = (timeString) => {
      return new Date(timeString).toLocaleString()
    }

    onMounted(() => {
      loadCurrentUser()
    })

    return {
      conversations,
      messages,
      activeConversation,
      newMessage,
      currentUser,
      getOtherUserName,
      getLastMessage,
      selectConversation,
      sendMessage,
      formatTime
    }
  }
}
</script>

<style scoped>
.messages {
  padding: 20px;
}
.conversation-item {
  padding: 10px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}
.conversation-item:hover {
  background-color: #f5f7fa;
}
.conversation-item.active {
  background-color: #e6f7ff;
}
.conversation-header {
  display: flex;
  justify-content: space-between;
}
.user-name {
  font-weight: bold;
}
.book-title {
  color: #909399;
  font-size: 12px;
}
.last-message {
  color: #909399;
  font-size: 12px;
  margin: 5px 0 0;
}
.message-list {
  height: 400px;
  overflow-y: auto;
  border: 1px solid #eee;
  padding: 10px;
  margin-bottom: 20px;
}
.message-item {
  margin-bottom: 15px;
}
.own-message {
  text-align: right;
}
.message-content {
  display: inline-block;
  max-width: 70%;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
}
.own-message .message-content {
  background: #409eff;
  color: white;
}
.message-time {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 5px;
}
.no-data, .no-conversation {
  text-align: center;
  padding: 50px;
  color: #909399;
}
</style>