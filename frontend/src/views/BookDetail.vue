<template>
  <div class="book-detail" v-if="book">
    <el-row :gutter="40">
      <el-col :span="12">
        <el-carousel v-if="book.images && book.images.length > 0">
          <el-carousel-item v-for="image in book.images" :key="image">
            <img :src="image" class="book-image" />
          </el-carousel-item>
        </el-carousel>
        <img v-else src="/default-book.jpg" class="book-image" />
      </el-col>
      
      <el-col :span="12">
        <h1>{{ book.title }}</h1>
        <p class="author">作者: {{ book.author }}</p>
        <p class="price">价格: ¥{{ book.price }}</p>
        <p class="condition">新旧程度: {{ book.condition }}</p>
        <p class="description">{{ book.description }}</p>
        
        <div class="seller-info">
          <h3>卖家信息</h3>
          <p>姓名: {{ book.seller.nickname || book.seller.username }}</p>
          <p v-if="book.seller.phone">电话: {{ book.seller.phone }}</p>
          <p v-if="book.seller.email">邮箱: {{ book.seller.email }}</p>
        </div>
        
        <div class="actions">
          <el-button type="primary" size="large" @click="contactSeller">
            联系卖家
          </el-button>
          <el-button v-if="isOwner" type="danger" size="large" @click="markAsSold">
            标记已售
          </el-button>
        </div>
      </el-col>
    </el-row>

    <!-- 私信对话框 -->
    <el-dialog v-model="showMessageDialog" title="联系卖家">
      <el-input
        v-model="messageContent"
        type="textarea"
        :rows="4"
        placeholder="请输入您想对卖家说的话..."
      />
      <template #footer>
        <el-button @click="showMessageDialog = false">取消</el-button>
        <el-button type="primary" @click="sendMessage">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'BookDetail',
  setup() {
    const book = ref(null)
    const showMessageDialog = ref(false)
    const messageContent = ref('')
    const route = useRoute()
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

    const isOwner = computed(() => {
      return user.value && book.value && user.value.id === book.value.seller.id
    })

    const fetchBookDetail = async () => {
      try {
        const response = await axios.get(`/api/books/${route.params.id}`)
        book.value = response.data
      } catch (error) {
        console.error('获取书籍详情失败', error)
        ElMessage.error('书籍不存在')
      }
    }

    const contactSeller = () => {
      if (!user.value) {
        ElMessage.warning('请先登录')
        return
      }
      showMessageDialog.value = true
    }

    const sendMessage = async () => {
      try {
        const message = {
          fromUser: user.value,
          toUser: book.value.seller,
          book: book.value,
          content: messageContent.value
        }
        await axios.post('/api/messages', message)
        ElMessage.success('消息发送成功')
        showMessageDialog.value = false
        messageContent.value = ''
      } catch (error) {
        ElMessage.error('消息发送失败')
        console.error('发送消息失败', error)
      }
    }

    const markAsSold = async () => {
      try {
        await axios.put(`/api/books/${book.value.id}/sold`)
        ElMessage.success('已标记为已售')
        book.value.sold = true
      } catch (error) {
        ElMessage.error('操作失败')
        console.error('标记已售失败', error)
      }
    }

    onMounted(() => {
      fetchBookDetail()
    })

    return {
      book,
      showMessageDialog,
      messageContent,
      isOwner,
      contactSeller,
      sendMessage,
      markAsSold
    }
  }
}
</script>

<style scoped>
.book-detail {
  padding: 20px;
}
.book-image {
  width: 100%;
  max-height: 500px;
  object-fit: contain;
}
.author, .price, .condition {
  font-size: 16px;
  margin: 10px 0;
}
.description {
  margin: 20px 0;
  line-height: 1.6;
}
.seller-info {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 4px;
  margin: 20px 0;
}
.actions {
  margin-top: 30px;
}
</style>