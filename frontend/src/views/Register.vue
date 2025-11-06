<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2>注册</h2>
      <el-form :model="registerForm" :rules="rules" ref="registerFormRef">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="昵称" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="registerForm.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" style="width: 100%">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <p>已有账号？<el-link @click="$router.push('/login')">立即登录</el-link></p>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'Register',
  emits: ['login-success'],
  setup(props, { emit }) {
    const registerForm = ref({
      username: '',
      password: '',
      confirmPassword: '',
      nickname: '',
      phone: '',
      email: ''
    })
    const registerFormRef = ref()
    const router = useRouter()

    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== registerForm.value.password) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度至少 6 个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
      ],
      nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' }
      ]
    }

    const handleRegister = async () => {
      try {
        await registerFormRef.value.validate()
        const { confirmPassword, ...submitData } = registerForm.value
        const response = await axios.post('/api/auth/register', submitData)
        
        if (response.data.user && response.data.token) {
          emit('login-success', response.data.user, response.data.token)
          ElMessage.success('注册成功')
          router.push('/')
        }
      } catch (error) {
        if (error.response && error.response.data) {
          ElMessage.error(error.response.data)
        } else {
          ElMessage.error('注册失败，请重试')
        }
        console.error('注册失败', error)
      }
    }

    return {
      registerForm,
      registerFormRef,
      rules,
      handleRegister
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}
.register-card {
  width: 400px;
  padding: 20px;
}
</style>