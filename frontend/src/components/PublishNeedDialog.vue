<template>
  <el-dialog 
    v-model="dialogVisible" 
    title="发布购书需求" 
    width="500px"
    :before-close="handleClose"
  >
    <el-form 
      :model="form" 
      :rules="rules" 
      ref="formRef"
      label-width="100px"
      class="publish-form"
    >
      <el-form-item label="书名" prop="title">
        <el-input 
          v-model="form.title" 
          placeholder="请输入您需要的书名"
          size="large"
        />
      </el-form-item>
      
      <el-form-item label="作者">
        <el-input 
          v-model="form.author" 
          placeholder="请输入作者（可选）"
        />
      </el-form-item>
      
      <el-form-item label="ISBN">
        <el-input 
          v-model="form.isbn" 
          placeholder="请输入ISBN号（可选）"
        />
      </el-form-item>
      
      <el-form-item label="最高预算">
        <el-input-number 
          v-model="form.maxPrice" 
          :min="0" 
          :precision="2"
          placeholder="请输入您的最高预算"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="分类" prop="category">
        <el-select 
          v-model="form.category" 
          placeholder="请选择分类"
          style="width: 100%"
        >
          <el-option 
            v-for="category in categories" 
            :key="category" 
            :label="category" 
            :value="category" 
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="需求描述">
        <el-input 
          v-model="form.description" 
          type="textarea" 
          :rows="4"
          placeholder="请详细描述您的需求，例如版本要求、新旧程度等"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button 
        type="primary" 
        @click="handleSubmit"
        :loading="loading"
      >
        发布需求
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
  name: 'PublishNeedDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'success', 'close'],
  setup(props, { emit }) {
    const dialogVisible = ref(false)
    const loading = ref(false)
    const formRef = ref()
    
    const form = reactive({
      title: '',
      author: '',
      isbn: '',
      maxPrice: null,
      category: '',
      description: ''
    })
    
    const categories = [
      '教材', '小说', '文学', '科技', '历史', '艺术', '考试', '工具书', '其他'
    ]
    
    const rules = {
      title: [
        { required: true, message: '请输入书名', trigger: 'blur' },
        { min: 1, max: 100, message: '书名长度在 1 到 100 个字符', trigger: 'blur' }
      ],
      category: [
        { required: true, message: '请选择分类', trigger: 'change' }
      ]
    }

    watch(() => props.modelValue, (newVal) => {
      dialogVisible.value = newVal
    })

    watch(dialogVisible, (newVal) => {
      if (!newVal) {
        // 重置表单
        Object.keys(form).forEach(key => {
          form[key] = ''
        })
        form.maxPrice = null
        if (formRef.value) {
          formRef.value.clearValidate()
        }
      }
    })

    const handleClose = () => {
      dialogVisible.value = false
      emit('update:modelValue', false)
      emit('close')
    }

    const handleSubmit = async () => {
      if (!formRef.value) return
      
      try {
        await formRef.value.validate()
        
        const user = JSON.parse(localStorage.getItem('user') || 'null')
        if (!user || !user.id) {
          ElMessage.warning('请先登录')
          return
        }

        loading.value = true

        const needData = {
          title: form.title,
          author: form.author,
          isbn: form.isbn,
          maxPrice: form.maxPrice,
          category: form.category,
          description: form.description,
          userId: user.id
        }

        await axios.post('/api/needs', needData)

        ElMessage.success('需求发布成功')
        emit('success')
        handleClose()
        
      } catch (error) {
        console.error('发布失败:', error)
        if (error.response && error.response.data) {
          ElMessage.error('发布失败: ' + error.response.data)
        } else if (error.errors) {
          // 验证错误
        } else {
          ElMessage.error('发布失败，请重试')
        }
      } finally {
        loading.value = false
      }
    }

    return {
      dialogVisible,
      loading,
      formRef,
      form,
      categories,
      rules,
      handleClose,
      handleSubmit
    }
  }
}
</script>

<style scoped>
.publish-form {
  padding: 0 20px;
}
</style>