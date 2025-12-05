<template>
  <div class="book-detail-container">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/books' }">二手书市场</el-breadcrumb-item>
      <el-breadcrumb-item>图书详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else-if="book" class="content-wrapper">
      <el-row :gutter="40">
        <el-col :xs="24" :md="10">
          <div class="gallery">
            <el-image 
              :src="currentImage || '/default-book.jpg'" 
              class="main-image" 
              fit="contain"
              :preview-src-list="book.images"
            />
            <div class="thumbnail-list" v-if="book.images && book.images.length > 1">
              <div 
                v-for="(img, idx) in book.images" 
                :key="idx"
                class="thumbnail-item"
                :class="{ active: currentImage === img }"
                @click="currentImage = img"
              >
                <img :src="img" />
              </div>
            </div>
          </div>
        </el-col>

        <el-col :xs="24" :md="14">
          <div class="book-header">
            <h1 class="title">{{ book.title }}</h1>
            <div class="tags">
              <el-tag effect="dark">{{ book.category || '综合' }}</el-tag>
              <el-tag type="success">{{ book.condition || '良好' }}</el-tag>
              <el-tag type="info">浏览 {{ book.views || 0 }} 次</el-tag>
            </div>
          </div>

          <div class="price-box">
            <span class="currency">¥</span>
            <span class="amount">{{ book.price }}</span>
            <span class="original-price" v-if="book.originalPrice">原价 ¥{{ book.originalPrice }}</span>
          </div>

          <div class="info-list">
            <div class="info-item">
              <span class="label">作者：</span>
              <span class="value">{{ book.author }}</span>
            </div>
            <div class="info-item">
              <span class="label">ISBN：</span>
              <span class="value">{{ book.isbn || '暂无' }}</span>
            </div>
            <div class="info-item">
              <span class="label">发布时间：</span>
              <span class="value">{{ formatDate(book.createTime) }}</span>
            </div>
          </div>

          <el-card class="seller-card" shadow="never">
            <div class="seller-header">
              <el-avatar :size="50" :src="book.seller?.avatar">
                {{ book.seller?.nickname?.charAt(0) }}
              </el-avatar>
              <div class="seller-meta">
                <div class="name">{{ book.seller?.nickname || book.seller?.username }}</div>
                <div class="reputation">
                  信誉分：<el-rate v-model="sellerRate" disabled show-score text-color="#ff9900" />
                </div>
              </div>
            </div>
            <div class="seller-actions">
              <el-button type="primary" size="large" icon="Message" @click="contactSeller" :disabled="isOwner">
                联系卖家
              </el-button>
              <el-button size="large" icon="Star" @click="toggleFavorite">
                {{ isFavorite ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </el-card>

          <div v-if="isOwner" class="owner-actions">
            <el-alert title="这是您发布的图书" type="info" show-icon :closable="false" style="margin-bottom: 10px"/>
            <el-button type="success" @click="editBook">编辑</el-button>
            <el-button type="danger" @click="handleDelete">下架</el-button>
            <el-button type="warning" @click="markAsSold" v-if="!book.sold">标记已售</el-button>
          </div>
        </el-col>
      </el-row>

      <el-row class="bottom-section">
        <el-col :span="24">
          <el-tabs v-model="activeTab" class="detail-tabs">
            <el-tab-pane label="图书详情" name="detail">
              <div class="description-content">
                <h3>书籍简介</h3>
                <p>{{ book.description || '卖家很懒，没有留下详细描述。' }}</p>
                </div>
            </el-tab-pane>
            <el-tab-pane label="相关推荐" name="recommend">
              <el-empty description="暂无相关推荐" />
            </el-tab-pane>
          </el-tabs>
        </el-col>
      </el-row>
    </div>
    
    <el-empty v-else description="图书不存在或已下架" />

    <el-dialog v-model="showMessageDialog" title="发送私信" width="500px">
      <div class="message-preview">
        <p>正在咨询：<strong>{{ book?.title }}</strong></p>
      </div>
      <el-input
        v-model="messageContent"
        type="textarea"
        :rows="4"
        placeholder="你好，这本书还在吗？我想买..."
      />
      <template #footer>
        <el-button @click="showMessageDialog = false">取消</el-button>
        <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * BookDetail.vue
 * 图书详情页
 * 展示图书详细信息、卖家信息，提供联系、收藏等交互功能
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

// 数据状态
const book = ref(null)
const loading = ref(true)
const currentImage = ref('')
const activeTab = ref('detail')
const sellerRate = ref(4.8) // 模拟评分
const isFavorite = ref(false)

// 私信相关
const showMessageDialog = ref(false)
const messageContent = ref('')
const sending = ref(false)

// 当前用户
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

const isOwner = computed(() => {
  return book.value && currentUser.id === book.value.seller?.id
})

// 初始化
const init = async () => {
  const bookId = route.params.id