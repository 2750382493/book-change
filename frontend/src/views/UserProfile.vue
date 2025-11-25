<template>
  <div class="user-profile">
    <div class="profile-header">
      <div class="container">
        <div class="user-info">
          <el-avatar :size="80" :src="user.avatar" class="user-avatar">
            {{ user.nickname ? user.nickname.charAt(0) : user.username.charAt(0) }}
          </el-avatar>
          <div class="user-details">
            <h1>{{ user.nickname || user.username }}</h1>
            <p class="user-username">@{{ user.username }}</p>
            <div class="user-stats">
              <div class="stat">
                <span class="stat-number">{{ myBooks.length }}</span>
                <span class="stat-label">在售书籍</span>
              </div>
              <div class="stat">
                <span class="stat-number">{{ myNeeds.length }}</span>
                <span class="stat-label">购书需求</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="profile-content">
      <div class="container">
        <el-tabs v-model="activeTab" type="card" class="profile-tabs">
          <!-- 在售书籍标签 -->
          <el-tab-pane label="在售书籍" name="books">
            <div class="tab-header">
              <h3>我的在售书籍</h3>
              <el-button type="primary" @click="showPublishDialog = true">
                <el-icon><Plus /></el-icon>发布新书
              </el-button>
            </div>
            <el-row :gutter="20" v-if="myBooks.length > 0">
              <el-col :xs="12" :sm="8" :md="6" v-for="book in myBooks" :key="book.id">
                <BookCard :book="book" :show-actions="true" @sold="handleBookSold" />
              </el-col>
            </el-row>
            <el-empty v-else description="暂无在售书籍">
              <el-button type="primary" @click="showPublishDialog = true">发布第一本书</el-button>
            </el-empty>
          </el-tab-pane>

          <!-- 购书需求标签 -->
          <el-tab-pane label="购书需求" name="needs">
            <div class="tab-header">
              <h3>我的购书需求</h3>
              <el-button type="success" @click="showNeedDialog = true">
                <el-icon><Plus /></el-icon>发布需求
              </el-button>
            </div>
            <el-row :gutter="20" v-if="myNeeds.length > 0">
              <el-col :xs="24" :sm="12" v-for="need in myNeeds" :key="need.id">
                <NeedCard :need="need" :show-actions="true" 
                         @fulfilled="handleNeedFulfilled" 
                         @delete="handleNeedDelete" />
              </el-col>
            </el-row>
            <el-empty v-else description="暂无购书需求">
              <el-button type="success" @click="showNeedDialog = true">发布第一个需求</el-button>
            </el-empty>
          </el-tab-pane>

          <!-- 个人信息标签 -->
          <el-tab-pane label="个人信息" name="profile">
            <el-card>
              <h3>个人信息</h3>
              <el-form :model="userForm" label-width="100px" class="profile-form">
                <el-form-item label="用户名">
                  <el-input v-model="userForm.username" disabled />
                </el-form-item>
                <el-form-item label="昵称">
                  <el-input v-model="userForm.nickname" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="userForm.phone" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="userForm.email" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="updateProfile">更新信息</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 发布书籍对话框 -->
    <PublishBookDialog 
      v-model="showPublishDialog" 
      @success="handlePublishSuccess" />

    <!-- 发布需求对话框 -->
    <PublishNeedDialog 
      v-model="showNeedDialog" 
      @success="handleNeedSuccess" />
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import BookCard from '../components/BookCard.vue'
import NeedCard from '../components/NeedCard.vue'
import PublishBookDialog from '../components/PublishBookDialog.vue'
import PublishNeedDialog from '../components/PublishNeedDialog.vue'

export default {
  name: 'UserProfile',
  components: { BookCard, NeedCard, PublishBookDialog, PublishNeedDialog, Plus },
  setup() {
    const userForm = ref({})
    const myBooks = ref([])
    const myNeeds = ref([])
    const activeTab = ref('books')
    const showPublishDialog = ref(false)
    const showNeedDialog = ref(false)

    const loadUserData = () => {
      const userData = localStorage.getItem('user')
      if (userData) {
        userForm.value = JSON.parse(userData)
        fetchMyBooks()
        fetchMyNeeds()
      }
    }

    const fetchMyBooks = async () => {
      try {
        const response = await axios.get(`/api/books/user/${userForm.value.id}`)
        myBooks.value = response.data
      } catch (error) {
        console.error('获取我的书籍失败', error)
      }
    }

    const fetchMyNeeds = async () => {
      try {
        const response = await axios.get(`/api/needs/user/${userForm.value.id}`)
        myNeeds.value = response.data
      } catch (error) {
        console.error('获取我的需求失败', error)
      }
    }

    const updateProfile = async () => {
      try {
        const response = await axios.put('/api/users/update', userForm.value)
        localStorage.setItem('user', JSON.stringify(response.data))
        ElMessage.success('更新成功')
      } catch (error) {
        ElMessage.error('更新失败')
        console.error('更新用户信息失败', error)
      }
    }

    const handleBookSold = async (bookId) => {
      try {
        await axios.put(`/api/books/${bookId}/sold`)
        ElMessage.success('标记为已售成功')
        fetchMyBooks()
      } catch (error) {
        ElMessage.error('操作失败')
        console.error('标记已售失败', error)
      }
    }

    const handleNeedFulfilled = async (needId) => {
      try {
        await axios.put(`/api/needs/${needId}/fulfilled`)
        ElMessage.success('标记为已完成')
        fetchMyNeeds()
      } catch (error) {
        ElMessage.error('操作失败')
        console.error('标记需求完成失败', error)
      }
    }

    const handleNeedDelete = async (needId) => {
      try {
        await axios.delete(`/api/needs/${needId}`)
        ElMessage.success('删除成功')
        fetchMyNeeds()
      } catch (error) {
        ElMessage.error('删除失败')
        console.error('删除需求失败', error)
      }
    }

    const handlePublishSuccess = () => {
      fetchMyBooks()
      showPublishDialog.value = false
    }

    const handleNeedSuccess = () => {
      fetchMyNeeds()
      showNeedDialog.value = false
    }

    onMounted(() => {
      loadUserData()
    })

    return {
      userForm,
      myBooks,
      myNeeds,
      activeTab,
      showPublishDialog,
      showNeedDialog,
      updateProfile,
      handleBookSold,
      handleNeedFulfilled,
      handleNeedDelete,
      handlePublishSuccess,
      handleNeedSuccess
    }
  }
}
</script>

<style scoped>
.user-profile {
  min-height: 100vh;
  background: #f8fafc;
}

.profile-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 60px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 30px;
}

.user-avatar {
  border: 4px solid rgba(255, 255, 255, 0.2);
}

.user-details h1 {
  margin: 0 0 8px 0;
  font-size: 2rem;
}

.user-username {
  margin: 0 0 20px 0;
  opacity: 0.8;
}

.user-stats {
  display: flex;
  gap: 30px;
}

.stat {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 1.5rem;
  font-weight: bold;
}

.stat-label {
  font-size: 0.9rem;
  opacity: 0.8;
}

.profile-content {
  padding: 40px 0;
}

.profile-tabs {
  background: white;
  border-radius: 12px;
  padding: 20px;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tab-header h3 {
  margin: 0;
  color: #2c3e50;
}

.profile-form {
  max-width: 500px;
}

@media (max-width: 768px) {
  .user-info {
    flex-direction: column;
    text-align: center;
    gap: 20px;
  }
  
  .user-stats {
    justify-content: center;
  }
  
  .tab-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
}
</style>