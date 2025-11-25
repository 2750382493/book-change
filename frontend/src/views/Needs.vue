<template>
  <div class="needs-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="container">
        <h1>求购专区</h1>
        <p>查看同学们的购书需求，帮助他人找到心仪的书籍</p>
        <div class="header-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索需求..."
            style="width: 300px;"
            @keyup.enter="searchNeeds"
          >
            <template #append>
              <el-button @click="searchNeeds" :icon="Search">搜索</el-button>
            </template>
          </el-input>
          <el-button 
            v-if="user" 
            type="primary" 
            @click="showPublishDialog = true"
          >
            <el-icon><Plus /></el-icon>发布需求
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="page-content">
      <div class="container">
        <!-- 分类筛选 -->
        <div class="filter-section">
          <el-radio-group v-model="filterCategory" @change="handleCategoryChange">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button 
              v-for="category in categories" 
              :key="category" 
              :label="category"
            >
              {{ category }}
            </el-radio-button>
          </el-radio-group>
        </div>

        <!-- 需求列表 -->
        <div class="needs-list">
          <el-row :gutter="20" v-if="needs.length > 0">
            <el-col 
              :xs="24" 
              :sm="12" 
              :lg="8" 
              v-for="need in filteredNeeds" 
              :key="need.id"
              class="need-col"
            >
              <NeedCard :need="need" />
            </el-col>
          </el-row>
          
          <el-empty v-else description="暂无需求">
            <el-button 
              v-if="user" 
              type="primary" 
              @click="showPublishDialog = true"
            >
              发布第一个需求
            </el-button>
            <el-button v-else type="primary" @click="$router.push('/login')">
              登录后发布需求
            </el-button>
          </el-empty>
        </div>

        <!-- 分页 -->
        <div class="pagination-section" v-if="needs.length > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[12, 24, 36, 48]"
            :total="filteredNeeds.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>

    <!-- 发布需求对话框 -->
    <PublishNeedDialog 
      v-model="showPublishDialog" 
      @success="handlePublishSuccess"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus } from '@element-plus/icons-vue'
import axios from 'axios'
import NeedCard from '../components/NeedCard.vue'
import PublishNeedDialog from '../components/PublishNeedDialog.vue'

export default {
  name: 'Needs',
  components: { NeedCard, PublishNeedDialog, Search, Plus },
  setup() {
    const router = useRouter()
    const needs = ref([])
    const searchKeyword = ref('')
    const filterCategory = ref('')
    const currentPage = ref(1)
    const pageSize = ref(12)
    const showPublishDialog = ref(false)
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

    const categories = [
      '教材', '小说', '文学', '科技', '历史', '艺术', '考试', '工具书', '其他'
    ]

    const fetchNeeds = async () => {
      try {
        const response = await axios.get('/api/needs')
        needs.value = response.data
      } catch (error) {
        console.error('获取需求列表失败', error)
      }
    }

    const searchNeeds = async () => {
      if (searchKeyword.value.trim()) {
        try {
          const response = await axios.get(`/api/needs/search?keyword=${encodeURIComponent(searchKeyword.value)}`)
          needs.value = response.data
        } catch (error) {
          console.error('搜索需求失败', error)
        }
      } else {
        fetchNeeds()
      }
    }

    const handleCategoryChange = () => {
      currentPage.value = 1
    }

    const handleSizeChange = (newSize) => {
      pageSize.value = newSize
      currentPage.value = 1
    }

    const handleCurrentChange = (newPage) => {
      currentPage.value = newPage
    }

    const handlePublishSuccess = () => {
      fetchNeeds()
    }

    // 计算属性：筛选后的需求
    const filteredNeeds = computed(() => {
      let filtered = needs.value
      
      // 按分类筛选
      if (filterCategory.value) {
        filtered = filtered.filter(need => need.category === filterCategory.value)
      }
      
      return filtered
    })

    // 计算属性：分页后的需求
    const paginatedNeeds = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return filteredNeeds.value.slice(start, end)
    })

    onMounted(() => {
      fetchNeeds()
    })

    return {
      needs: paginatedNeeds,
      searchKeyword,
      filterCategory,
      currentPage,
      pageSize,
      showPublishDialog,
      user,
      categories,
      Search,
      Plus,
      searchNeeds,
      handleCategoryChange,
      handleSizeChange,
      handleCurrentChange,
      handlePublishSuccess,
      filteredNeeds
    }
  }
}
</script>

<style scoped>
.needs-page {
  min-height: 100vh;
  background: #f8fafc;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 60px 0;
  text-align: center;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header h1 {
  font-size: 2.5rem;
  margin-bottom: 1rem;
  font-weight: 700;
}

.page-header p {
  font-size: 1.1rem;
  margin-bottom: 2rem;
  opacity: 0.9;
}

.header-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.page-content {
  padding: 40px 0;
}

.filter-section {
  margin-bottom: 30px;
  text-align: center;
}

.needs-list {
  min-height: 400px;
}

.need-col {
  margin-bottom: 20px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

@media (max-width: 768px) {
  .page-header h1 {
    font-size: 2rem;
  }
  
  .page-header p {
    font-size: 1rem;
  }
  
  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .header-actions .el-input {
    width: 100% !important;
  }
  
  .filter-section {
    overflow-x: auto;
    padding-bottom: 10px;
  }
  
  .filter-section .el-radio-group {
    flex-wrap: nowrap;
    white-space: nowrap;
  }
}
</style>