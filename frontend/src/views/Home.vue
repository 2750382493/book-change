<template>
  <div class="home">
    <el-row :gutter="20">
      <el-col :span="18">
        <h1>校园二手书交易平台</h1>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索书籍..."
          @keyup.enter="searchBooks"
        >
          <template #append>
            <el-button @click="searchBooks">搜索</el-button>
          </template>
        </el-input>
        
        <el-divider />
        
        <h2>最新书籍</h2>
        <el-row :gutter="20">
          <el-col :span="8" v-for="book in books" :key="book.id">
            <el-card class="book-card">
              <img :src="book.images && book.images[0] ? book.images[0] : '/default-book.jpg'" 
                   class="book-image" />
              <div class="book-info">
                <h3>{{ book.title }}</h3>
                <p class="price">¥{{ book.price }}</p>
                <p class="seller">卖家: {{ book.seller.nickname || book.seller.username }}</p>
                <el-button type="primary" @click="viewBook(book.id)">查看详情</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
      
      <el-col :span="6">
        <el-card v-if="!user">
          <h3>欢迎来到二手书交易平台</h3>
          <p>请登录或注册以开始交易</p>
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
          <el-button @click="$router.push('/register')">注册</el-button>
        </el-card>
        
        <el-card v-else>
          <h3>发布书籍</h3>
          <el-button type="success" @click="showPublishDialog = true">发布新书</el-button>
        </el-card>
        
        <el-card style="margin-top: 20px;">
          <h3>书籍分类</h3>
          <el-tag v-for="category in categories" :key="category" 
                  style="margin: 5px; cursor: pointer;"
                  @click="searchByCategory(category)">
            {{ category }}
          </el-tag>
        </el-card>
      </el-col>
    </el-row>

    <!-- 发布书籍对话框 -->
    <el-dialog v-model="showPublishDialog" title="发布新书" width="600px">
      <el-form :model="newBook" label-width="80px">
        <el-form-item label="书名" required>
          <el-input v-model="newBook.title" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="newBook.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="newBook.price" :min="0" :precision="2" placeholder="请输入价格" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="newBook.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="category in categories" :key="category" 
                       :label="category" :value="category" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newBook.description" type="textarea" :rows="3" placeholder="请输入书籍描述" />
        </el-form-item>
        <el-form-item label="新旧程度">
          <el-select v-model="newBook.condition" placeholder="请选择新旧程度" style="width: 100%">
            <el-option label="全新" value="全新" />
            <el-option label="良好" value="良好" />
            <el-option label="一般" value="一般" />
            <el-option label="有磨损" value="有磨损" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            action="#"
            :on-change="handleImageChange"
            :auto-upload="false"
            list-type="picture-card"
            :file-list="imageFiles"
            :limit="3"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" @click="publishBook" :loading="publishing">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

export default {
  name: 'Home',
  components: { Plus },
  emits: ['login-success'],
  setup(props, { emit }) {
    const books = ref([])
    const searchKeyword = ref('')
    const showPublishDialog = ref(false)
    const publishing = ref(false)
    const newBook = ref({
      title: '',
      author: '',
      price: 0,
      category: '',
      description: '',
      condition: '良好'
    })
    const imageFiles = ref([])
    const categories = ref(['教材', '小说', '文学', '科技', '历史', '艺术', '其他'])
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
    const router = useRouter()

    const fetchBooks = async () => {
      try {
        const response = await axios.get('/api/books')
        books.value = response.data
      } catch (error) {
        console.error('获取书籍列表失败', error)
      }
    }

    const searchBooks = async () => {
      try {
        const response = await axios.get(`/api/books/search?keyword=${searchKeyword.value}`)
        books.value = response.data
      } catch (error) {
        console.error('搜索书籍失败', error)
      }
    }

    const searchByCategory = async (category) => {
      try {
        const response = await axios.get(`/api/books/category/${category}`)
        books.value = response.data
      } catch (error) {
        console.error('按分类搜索失败', error)
      }
    }

    const viewBook = (bookId) => {
      router.push(`/books/${bookId}`)
    }

    const handleImageChange = (file, fileList) => {
      imageFiles.value = fileList
    }

    const publishBook = async () => {
      if (!user.value || !user.value.id) {
        ElMessage.warning('请先登录')
        return
      }

      if (!newBook.value.title || !newBook.value.price) {
        ElMessage.warning('请填写书名和价格')
        return
      }

      publishing.value = true

      try {
        console.log('发布书籍数据:', newBook.value)
        console.log('用户ID:', user.value.id)

        // 使用简化接口创建书籍
        const bookData = {
          title: newBook.value.title,
          author: newBook.value.author,
          price: newBook.value.price,
          category: newBook.value.category,
          description: newBook.value.description,
          condition: newBook.value.condition,
          sellerId: user.value.id
        }

        const response = await axios.post('/api/books/simple', bookData)
        const bookId = response.data.id
        
        ElMessage.success('书籍发布成功')

        // 上传图片
        if (imageFiles.value.length > 0) {
          for (const file of imageFiles.value) {
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
              ElMessage.warning('部分图片上传失败，但书籍已发布')
            }
          }
        }

        // 重置表单
        newBook.value = {
          title: '',
          author: '',
          price: 0,
          category: '',
          description: '',
          condition: '良好'
        }
        imageFiles.value = []
        showPublishDialog.value = false
        
        // 刷新书籍列表
        fetchBooks()
        
      } catch (error) {
        console.error('发布书籍失败:', error)
        console.error('错误详情:', error.response?.data)
        
        if (error.response && error.response.data) {
          ElMessage.error('发布失败: ' + (typeof error.response.data === 'string' ? error.response.data : JSON.stringify(error.response.data)))
        } else {
          ElMessage.error('发布失败，请检查网络连接')
        }
      } finally {
        publishing.value = false
      }
    }

    onMounted(() => {
      fetchBooks()
    })

    return {
      books,
      searchKeyword,
      showPublishDialog,
      publishing,
      newBook,
      imageFiles,
      categories,
      user,
      searchBooks,
      searchByCategory,
      viewBook,
      handleImageChange,
      publishBook
    }
  }
}
</script>

<style scoped>
.book-card {
  margin-bottom: 20px;
}
.book-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}
.book-info {
  padding: 10px 0;
}
.price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 18px;
}
.seller {
  color: #909399;
  font-size: 14px;
}
</style>