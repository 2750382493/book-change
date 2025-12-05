<template>
  <div class="home-container">
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">让知识在校园里流动</h1>
        <p class="hero-subtitle">安全、便捷、透明的校园二手书交易平台</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/books')" class="action-btn">
            我要买书 <el-icon class="el-icon--right"><Search /></el-icon>
          </el-button>
          <el-button type="success" size="large" @click="$router.push('/profile')" class="action-btn">
            我要卖书 <el-icon class="el-icon--right"><Sell /></el-icon>
          </el-button>
        </div>
      </div>
      <div class="hero-image">
        <img src="@/assets/hero-illustration.svg" alt="Reading" v-if="false" /> </div>
    </div>

    <div class="stats-bar">
      <el-row :gutter="20" justify="center">
        <el-col :span="6" v-for="(stat, index) in stats" :key="index">
          <div class="stat-item">
            <el-icon :size="32" :color="stat.color"><component :is="stat.icon" /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="main-content">
      <section class="section">
        <div class="section-header">
          <h2>📚 最新上架</h2>
          <el-link type="primary" @click="$router.push('/books')">查看更多 ></el-link>
        </div>
        
        <div v-if="loading" class="skeleton-wrapper">
          <el-skeleton :rows="3" animated count="4" />
        </div>
        
        <el-row :gutter="20" v-else>
          <el-col :xs="24" :sm="12" :md="6" v-for="book in recentBooks" :key="book.id">
            <el-card shadow="hover" class="book-card" @click="viewBook(book.id)">
              <div class="card-image">
                <img :src="book.images?.[0] || '/default-book.jpg'" lazy />
                <div class="book-tag" v-if="book.condition === '全新'">全新</div>
              </div>
              <div class="card-body">
                <h3 class="book-title">{{ book.title }}</h3>
                <p class="book-author">{{ book.author }}</p>
                <div class="book-footer">
                  <span class="price">¥{{ book.price }}</span>
                  <span class="time">{{ formatDate(book.createTime) }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </section>

      <section class="section highlight-bg">
        <div class="section-header">
          <h2>🔥 急需书籍</h2>
          <el-link type="primary" @click="$router.push('/needs')">去帮助同学 ></el-link>
        </div>
        
        <el-carousel :interval="4000" type="card" height="200px" v-if="!loading">
          <el-carousel-item v-for="need in urgentNeeds" :key="need.id">
            <div class="need-card" @click="$router.push('/needs')">
              <h3>{{ need.title }}</h3>
              <p>作者：{{ need.author || '不限' }}</p>
              <div class="need-price">最高回收价：<span>¥{{ need.maxPrice }}</span></div>
              <el-tag effect="dark" type="danger">急需</el-tag>
            </div>
          </el-carousel-item>
        </el-carousel>
      </section>
    </div>

    <footer class="site-footer">
      <div class="footer-content">
        <p>&copy; 2023 校园二手书交易平台 | 计算机学院软件工程项目组</p>
        <p>联系我们: support@campusbook.com</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
/**
 * Home.vue
 * 平台门户首页
 * 展示核心功能入口、最新数据动态及推广信息
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Sell, Goods, List, UserFilled } from '@element-plus/icons-vue' // 需确保引入图标
import axios from 'axios'
import dayjs from 'dayjs' // 建议引入 dayjs 处理时间，或者手写 formatter

const router = useRouter()

// 状态数据
const loading = ref(true)
const recentBooks = ref([])
const urgentNeeds = ref([])
const stats = ref([
  { label: '在售图书', value: '1,204', icon: 'Goods', color: '#409EFF' },
  { label: '成功交易', value: '856', icon: 'List', color: '#67C23A' },
  { label: '注册用户', value: '2,300+', icon: 'UserFilled', color: '#E6A23C' },
])

// 初始化数据加载
const initData = async () => {
  try {
    loading.value = true
    // 并发请求提高效率
    const [booksRes, needsRes] = await Promise.all([
      axios.get('/api/books?sort=createTime,desc&size=8'),
      axios.get('/api/needs?sort=urgency,desc&size=5')
    ])
    
    // 截取部分数据用于展示
    recentBooks.value = booksRes.data.slice(0, 8)
    urgentNeeds.value = needsRes.data.slice(0, 5)
  } catch (error) {
    console.error('首页数据加载失败:', error)
  } finally {
    loading.value = false
  }
}

// 格式化时间：展示“3小时前”等友好格式
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = (now - date) / 1000 // 秒
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return date.toLocaleDateString()
}

const viewBook = (id) => {
  router.push(`/books/${id}`)
}

onMounted(() => {
  initData()
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.hero-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 80px 20px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.hero-title {
  font-size: 3rem;
  margin-bottom: 1rem;
  font-weight: 700;
}

.hero-subtitle {
  font-size: 1.5rem;
  opacity: 0.9;
  margin-bottom: 2rem;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.action-btn {
  padding: 12px 30px;
  font-size: 1.1rem;
}

.stats-bar {
  background: white;
  padding: 30px 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  margin-bottom: 40px;
}

.stat-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  color: #909399;
  font-size: 0.9rem;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 0 20px;
  flex: 1;
}

.section {
  margin-bottom: 60px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-left: 5px solid #409EFF;
  padding-left: 15px;
}

.section-header h2 {
  font-size: 1.8rem;
  color: #303133;
  margin: 0;
}

.book-card {
  cursor: pointer;
  transition: transform 0.3s;
  height: 100%;
  border: none;
}

.book-card:hover {
  transform: translateY(-5px);
}

.card-image {
  height: 200px;
  overflow: hidden;
  position: relative;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #f56c6c;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.card-body {
  padding: 15px;
}

.book-title {
  font-size: 1.1rem;
  margin: 0 0 5px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-author {
  color: #909399;
  font-size: 0.9rem;
  margin-bottom: 10px;
}

.book-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #f56c6c;
  font-size: 1.2rem;
  font-weight: bold;
}

.time {
  font-size: 0.8rem;
  color: #C0C4CC;
}

.need-card {
  background: #fff;
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  cursor: pointer;
}

.need-price span {
  font-size: 1.4rem;
  color: #F56C6C;
  font-weight: bold;
}

.site-footer {
  background: #2c3e50;
  color: white;
  padding: 40px 0;
  text-align: center;
  margin-top: auto;
}
</style>