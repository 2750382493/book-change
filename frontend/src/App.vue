<template>
  <div id="app">
    <el-container>
      <el-header>
        <el-menu mode="horizontal" :router="true">
          <el-menu-item index="/">校园二手书交易</el-menu-item>
          <el-menu-item index="/books">书籍列表</el-menu-item>
          <el-menu-item v-if="!user" index="/login">登录</el-menu-item>
          <el-menu-item v-if="!user" index="/register">注册</el-menu-item>
          <el-submenu v-if="user" :index="user.id">
            <template #title>{{ user.nickname || user.username }}</template>
            <el-menu-item index="/profile">个人中心</el-menu-item>
            <el-menu-item index="/messages">
              消息
              <el-badge v-if="unreadCount > 0" :value="unreadCount" />
            </el-menu-item>
            <el-menu-item @click="logout">退出登录</el-menu-item>
          </el-submenu>
        </el-menu>
      </el-header>
      
      <el-main>
        <router-view @login-success="handleLoginSuccess" />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

export default {
  name: 'App',
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

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

.el-header {
  padding: 0;
}

.el-menu {
  border-bottom: none;
}
</style>