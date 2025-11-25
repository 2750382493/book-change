<template>
  <div id="app">
    <el-container class="layout-container">
      <el-header class="header">
        <div class="header-content">
          <div class="logo">
            <h1>📚 校园二手书</h1>
          </div>
          <el-menu 
            mode="horizontal" 
            :router="true"
            class="nav-menu"
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b">
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/books">书籍市场</el-menu-item>
            <el-menu-item index="/needs">求购专区</el-menu-item>
            <el-submenu v-if="user" index="user-menu">
              <template #title>
                <el-avatar :size="32" :src="user.avatar" style="margin-right: 8px;">
                  {{ user.nickname ? user.nickname.charAt(0) : user.username.charAt(0) }}
                </el-avatar>
                {{ user.nickname || user.username }}
              </template>
              <el-menu-item index="/profile">
                <el-icon><User /></el-icon>个人中心
              </el-menu-item>
              <el-menu-item index="/messages">
                <el-icon><ChatDotRound /></el-icon>
                消息
                <el-badge v-if="unreadCount > 0" :value="unreadCount" class="badge" />
              </el-menu-item>
              <el-menu-item @click="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-menu-item>
            </el-submenu>
            <div v-else class="auth-buttons">
              <el-button @click="$router.push('/login')" type="primary" text>登录</el-button>
              <el-button @click="$router.push('/register')" type="success" text>注册</el-button>
            </div>
          </el-menu>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view @login-success="handleLoginSuccess" />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { 
  User, 
  ChatDotRound, 
  SwitchButton 
} from '@element-plus/icons-vue'

export default {
  name: 'App',
  components: { User, ChatDotRound, SwitchButton },
  setup() {
    const user = ref(null)
    const unreadCount = ref(0)
    const router = useRouter()

    const checkAuth = () => {
      const token = localStorage.getItem('token')
      const userData = localStorage.getItem('user')
      if (token && userData) {
        user.value = JSON.parse(userData)
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
        fetchUnreadCount()
      }
    }

    const fetchUnreadCount = async () => {
      if (user.value) {
        try {
          const response = await axios.get(`/api/messages/unread/count/${user.value.id}`)
          unreadCount.value = response.data
        } catch (error) {
          console.error('获取未读消息数失败', error)
        }
      }
    }

    const handleLoginSuccess = (userData, token) => {
      user.value = userData
      localStorage.setItem('user', JSON.stringify(userData))
      localStorage.setItem('token', token)
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
      fetchUnreadCount()
      router.push('/')
    }

    const logout = () => {
      user.value = null
      localStorage.removeItem('user')
      localStorage.removeItem('token')
      delete axios.defaults.headers.common['Authorization']
      router.push('/')
    }

    onMounted(() => {
      checkAuth()
    })

    return {
      user,
      unreadCount,
      handleLoginSuccess,
      logout
    }
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e6e6e6;
  padding: 0;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.logo h1 {
  margin: 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: bold;
}

.nav-menu {
  border: none;
  background: transparent !important;
}

.nav-menu .el-menu-item {
  font-weight: 500;
  transition: all 0.3s ease;
}

.nav-menu .el-menu-item:hover {
  background: rgba(84, 92, 100, 0.1) !important;
  transform: translateY(-2px);
}

.auth-buttons {
  display: flex;
  gap: 10px;
}

.main-content {
  padding: 0;
  background: transparent;
}

.badge {
  margin-left: 8px;
}
</style>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f5f7fa;
}

#app {
  min-height: 100vh;
}

/* 美化卡片 */
.el-card {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08) !important;
  transition: all 0.3s ease;
}

.el-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12) !important;
}

/* 美化按钮 */
.el-button {
  border-radius: 8px !important;
  font-weight: 500;
  transition: all 0.3s ease;
}

.el-button--primary {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  border: none !important;
}

.el-button--success {
  background: linear-gradient(135deg, #56ab2f, #a8e6cf) !important;
  border: none !important;
}

/* 美化输入框 */
.el-input__inner {
  border-radius: 8px !important;
}

/* 美化标签 */
.el-tag {
  border-radius: 6px !important;
}
</style>