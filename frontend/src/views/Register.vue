<template>
  <div class="register-container">
    <el-card class="register-card">
      <div class="header">
        <h2>创建新账号</h2>
        <p>加入我们的社区，开启二手书之旅</p>
      </div>

      <el-steps :active="activeStep" finish-status="success" align-center class="steps-bar">
        <el-step title="账号信息" />
        <el-step title="个人资料" />
        <el-step title="完成注册" />
      </el-steps>

      <el-form 
        v-if="activeStep === 0" 
        :model="form" 
        :rules="rules" 
        ref="accountFormRef" 
        label-position="top"
      >
        <el-form-item label="学号/账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入您的学号作为账号" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="设置密码" prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="6-20位，包含字母和数字" 
            size="large" 
            show-password
            @input="checkPasswordStrength"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
          <div class="password-strength" v-if="form.password">
            <span>强度：</span>
            <el-progress 
              :percentage="passwordScore" 
              :color="passwordColor" 
              :format="() => strengthText" 
              style="width: 200px"
            />
          </div>
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="请再次输入密码" 
            size="large"
          />
        </el-form-item>
        
        <el-button type="primary" size="large" class="full-btn" @click="nextStep">下一步</el-button>
      </el-form>

      <el-form 
        v-if="activeStep === 1" 
        :model="form" 
        :rules="rules" 
        ref="profileFormRef" 
        label-position="top"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="大家怎么称呼您" size="large" />
        </el-form-item>
        
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="form.phone" placeholder="方便买家联系您" size="large" />
        </el-form-item>
        
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="form.email" placeholder="用于找回密码" size="large" />
        </el-form-item>

        <div class="btn-group">
          <el-button size="large" @click="prevStep">上一步</el-button>
          <el-button type="primary" size="large" :loading="registering" @click="handleRegister">
            立即注册
          </el-button>
        </div>
      </el-form>

      <div v-if="activeStep === 2" class="success-box">
        <el-result
          icon="success"
          title="注册成功"
          sub-title="欢迎加入！正在跳转至登录页面..."
        >
          <template #extra>
            <el-button type="primary" @click="$router.push('/login')">立即登录</el-button>
          </template>
        </el-result>
      </div>

      <div class="footer-link">
        已有账号？<el-link type="primary" @click="$router.push('/login')">直接登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * Register.vue
 * 用户注册组件
 * 包含分步验证、密码强度检测及表单提交逻辑
 */
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const activeStep = ref(0)
const registering = ref(false)
const accountFormRef = ref(null)
const profileFormRef = ref(null)

// 表单数据
const form = reactive({
  username: