<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>登录</h2>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <p>还没有账号？<el-link @click="$router.push('/register')">立即注册</el-link></p>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'Login',
  emits: ['login-success'],
  setup(props, { emit }) {
    const loginForm = ref({
      username: '',
      password: ''
    })
    const loginFormRef = ref()
    const router = useRouter()

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ]
    }

    const handleLogin = async () => {
      try {
        await loginFormRef.value.validate()
        const response = await axios.post('/api/auth/login', loginForm.value)
        
        if (response.data.user && response.data.token) {
          emit('login-success', response.data.user, response.data.token)
          ElMessage.success('登录成功')
          router.push('/')
        }
      } catch (error) {
        ElMessage.error('登录失败，请检查用户名和密码')
        console.error('登录失败', error)
      }
    }

    return {
      loginForm,
      loginFormRef,
      rules,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}
.login-card {
  width: 400px;
  padding: 20px;
}
</style>