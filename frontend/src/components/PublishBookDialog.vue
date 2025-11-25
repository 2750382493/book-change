<template>
  <el-dialog 
    v-model="dialogVisible" 
    title="发布新书" 
    width="600px"
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
          placeholder="请输入书名"
          size="large"
        />
      </el-form-item>
      
      <el-form-item label="作者" prop="author">
        <el-input 
          v-model="form.author" 
          placeholder="请输入作者"
        />
      </el-form-item>
      
      <el-form-item label="价格" prop="price">
        <el-input-number 
          v-model="form.price" 
          :min="0" 
          :precision="2"
          placeholder="请输入价格"
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
      
      <el-form-item label="新旧程度" prop="condition">
        <el-select 
          v-model="form.condition" 
          placeholder="请选择新旧程度"
          style="width: 100%"
        >
          <el-option label="全新" value="全新" />
          <el-option label="良好" value="良好" />
          <el-option label="一般" value="一般" />
          <el-option label="有磨损" value="有磨损" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="书籍描述" prop="description">
        <el-input 
          v-model="form.description" 
          type="textarea" 
          :rows="4"
          placeholder="请输入书籍描述、使用情况等"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="书籍图片">
        <el-upload
          action="#"
          list-type="picture-card"
          :file-list="fileList"
          :on-change="handleChange"
          :on-remove="handleRemove"
          :auto-upload="false"
          :limit="3"
          accept="image/*"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="upload-tip">最多上传3张图片，每张不超过2MB</div>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button 
        type="primary" 
        @click="handleSubmit"
        :loading="loading"
      >
        发布书籍
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'PublishBookDialog',
  components: { Plus },
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
    const fileList = ref([])
    
    const form = reactive({
      title: '',
      author: '',
      price: 0,
      category: '',
      condition: '良好',
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
      price: [
        { required: true, message: '请输入价格', trigger: 'blur' },
        { type: 'number', min: 0, message: '价格必须大于0', trigger: 'blur' }
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
          if (key === 'condition') {
            form[key] = '良好'
          } else if (key === 'price') {
            form[key] = 0
          } else {
            form[key] = ''
          }
        })
        fileList.value = []
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

    const handleChange = (file, files) => {
      fileList.value = files
    }

    const handleRemove = (file, files) => {
      fileList.value = files
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

        // 创建书籍
        const bookData = {
          title: form.title,
          author: form.author,
          price: form.price,
          category: form.category,
          description: form.description,
          condition: form.condition,
          sellerId: user.id
        }

        const bookResponse = await axios.post('/api/books/simple', bookData)
        const bookId = bookResponse.data.id

        // 上传图片
        if (fileList.value.length > 0) {
          for (const file of fileList.value) {
            const formData = new FormData()
            formData.append('file', file.raw)
            
            try {
              await axios.post(`/api/books/upload-image/${bookId}`, formData, {
                headers: {
                  'Content-Type': 'multipart/form-data'
                }
              })
            } catch (uploadError) {
              console.error('图片上传失败:', uploadError)
              // 继续上传其他图片，不中断流程
            }
          }
        }

        ElMessage.success('书籍发布成功')
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
      fileList,
      categories,
      rules,
      handleClose,
      handleChange,
      handleRemove,
      handleSubmit
    }
  }
}
</script>

<style scoped>
.publish-form {
  padding: 0 20px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

:deep(.el-upload--picture-card) {
  width: 100px;
  height: 100px;
  line-height: 100px;
}

:deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 100px;
  height: 100px;
}
</style>