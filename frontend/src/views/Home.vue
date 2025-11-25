<template>
  <div class="home">
    <!-- 英雄区域 -->
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">发现你的下一本好书</h1>
        <p class="hero-subtitle">校园二手书交易平台，让知识流动起来</p>
        <div class="search-container">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索书籍或需求..."
            size="large"
            @keyup.enter="searchAll"
          >
            <template #append>
              <el-button type="primary" @click="searchAll" :icon="Search">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="main-container">
      <div class="container">
        <!-- 快速操作 -->
        <div class="quick-actions">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card class="action-card" @click="$router.push('/books')">
                <div class="action-content">
                  <div class="action-icon">📚</div>
                  <h3>书籍市场</h3>
                  <p>浏览同学们出售的二手书</p>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="action-card" @click="$router.push('/needs')">
                <div class="action-content">
                  <div class="action-icon">🛒</div>
                  <h3>求购专区</h3>
                  <p>查看同学们的购书需求</p>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="action-card" @click="showPublishDialog = true" v-if="user">
                <div class="action-content">
                  <div class="action-icon">➕</div>
                  <h3>发布书籍</h3>
                  <p>出售你的闲置书籍</p>
                </div>
              </el-card>
              <el-card class="action-card" @click="$router.push('/login')" v-else>
                <div class="action-content">
                  <div class="action-icon">🔐</div>
                  <h3>登录注册</h3>
                  <p>开始交易之旅</p>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 最新书籍 -->
        <section class="section">
          <div class="section-header">
            <h2>最新上架</h2>
            <el-button type="text" @click="$router.push('/books')">查看全部 →</el-button>
          </div>
          <el-row :gutter="20" v-if="books.length > 0">
            <el-col :xs="12" :sm="8" :md="6" v-for="book in books.slice(0, 8)" :key="book.id">
              <BookCard :book="book" />
            </el-col>
          </el-row>
          <el-empty v-else description="暂无书籍" />
        </section>

        <!-- 最新需求 -->
        <section class="section">
          <div class="section-header">
            <h2>最新需求</h2>
            <el-button type="text" @click="$router.push('/needs')">查看全部 →</el-button>
          </div>
          <el-row :gutter="20" v-if="needs.length > 0">
            <el-col :xs="24" :sm="12" :md="8" v-for="need in needs.slice(0, 6)" :key="need.id">
              <NeedCard :need="need" />
            </el-col>
          </el-row>
          <el-empty v-else description="暂无需求" />
        </section>
      </div>
    </div>

    <!-- 发布书籍对话框 -->
    <PublishBookDialog 
      v-model="showPublishDialog" 
      @success="handlePublishSuccess"
      @close="showPublishDialog = false" />
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { Search } from '@element-plus/icons-vue'
import BookCard from '../components/BookCard.vue'
import NeedCard from '../components/NeedCard.vue'
import PublishBookDialog from '../components/PublishBookDialog.vue'

export default {
  name: 'Home',
  components: { BookCard, NeedCard, PublishBookDialog },
  emits: ['login-success'],
  setup(props, { emit }) {
    const books = ref([])
    const needs = ref([])
    const searchKeyword = ref('')
    const showPublishDialog = ref(false)
    const router = useRouter()
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

    const fetchBooks = async () => {
      try {
        const response = await axios.get('/api/books')
        books.value = response.data
      } catch (error) {
        console.error('获取书籍列表失败', error)
      }
    }

    const fetchNeeds = async () => {
      try {
        const response = await axios.get('/api/needs')
        needs.value = response.data
      } catch (error) {
        console.error('获取需求列表失败', error)
      }
    }

    const searchAll = () => {
      if (searchKeyword.value.trim()) {
        router.push(`/books?search=${encodeURIComponent(searchKeyword.value)}`)
      }
    }

    const handlePublishSuccess = () => {
      fetchBooks()
      showPublishDialog.value = false
    }

    onMounted(() => {
      fetchBooks()
      fetchNeeds()
    })

    return {
      books,
      needs,
      searchKeyword,
      showPublishDialog,
      user,
      Search,
      searchAll,
      handlePublishSuccess
    }
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
}

.hero-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 80px 20px;
  text-align: center;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 3rem;
  font-weight: 700;
  margin-bottom: 1rem;
  background: linear-gradient(45deg, #fff, #e3f2fd);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  font-size: 1.2rem;
  margin-bottom: 2rem;
  opacity: 0.9;
}

.search-container {
  max-width: 500px;
  margin: 0 auto;
}

.main-container {
  padding: 60px 0;
  background: #f8fafc;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.quick-actions {
  margin-bottom: 60px;
}

.action-card {
  cursor: pointer;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: white !important;
}

.action-content {
  padding: 20px;
}

.action-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.action-card h3 {
  margin-bottom: 0.5rem;
  color: #2c3e50;
}

.action-card p {
  color: #666;
  margin: 0;
}

.section {
  margin-bottom: 60px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.section-header h2 {
  font-size: 1.8rem;
  color: #2c3e50;
  margin: 0;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2rem;
  }
  
  .hero-subtitle {
    font-size: 1rem;
  }
  
  .action-card {
    height: 140px;
    margin-bottom: 20px;
  }
  
  .action-icon {
    font-size: 2rem;
  }
}
</style>