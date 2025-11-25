<template>
  <el-card class="book-card" :body-style="{ padding: '0px' }">
    <div class="book-image-container">
      <img 
        :src="getImageUrl(book.images && book.images[0] ? book.images[0] : '/default-book.jpg')" 
        class="book-image"
        @error="handleImageError"
        alt="书籍封面"
      />
      <div class="book-overlay">
        <el-tag v-if="book.sold" type="danger" class="status-tag">已售出</el-tag>
        <el-tag v-else type="success" class="status-tag">在售</el-tag>
      </div>
    </div>

    <div class="book-content">
      <h3 class="book-title">{{ book.title }}</h3>
      <p class="book-author" v-if="book.author">作者: {{ book.author }}</p>
      <p class="book-category" v-if="book.category">{{ book.category }}</p>
      
      <div class="book-footer">
        <div class="book-price">¥{{ book.price }}</div>
        <div class="book-actions">
          <el-button type="primary" size="small" @click="viewBook">查看详情</el-button>
          <el-button 
            v-if="showActions && !book.sold" 
            type="danger" 
            size="small"
            @click="$emit('sold', book.id)"
          >
            标记已售
          </el-button>
        </div>
      </div>
      
      <div class="seller-info">
        <el-avatar :size="24" :src="book.seller.avatar" class="seller-avatar">
          {{ book.seller.nickname ? book.seller.nickname.charAt(0) : book.seller.username.charAt(0) }}
        </el-avatar>
        <span class="seller-name">{{ book.seller.nickname || book.seller.username }}</span>
      </div>
    </div>
  </el-card>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'BookCard',
  props: {
    book: {
      type: Object,
      required: true
    },
    showActions: {
      type: Boolean,
      default: false
    }
  },
  emits: ['sold'],
  setup(props) {
    const router = useRouter()

    const getImageUrl = (imagePath) => {
      // 如果已经是完整URL，直接返回
      if (imagePath.startsWith('http')) {
        return imagePath
      }
      // 如果是相对路径，添加后端基础URL
      if (imagePath.startsWith('/uploads/')) {
        return `http://localhost:8080${imagePath}`
      }
      // 默认图片
      return imagePath
    }

    const viewBook = () => {
      router.push(`/books/${props.book.id}`)
    }

    const handleImageError = (event) => {
      console.error('图片加载失败:', event.target.src)
      event.target.src = '/default-book.jpg'
    }

    return {
      viewBook,
      handleImageError,
      getImageUrl
    }
  }
}
</script>
<style scoped>
.book-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.book-image-container {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.book-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.book-card:hover .book-image {
  transform: scale(1.05);
}

.book-overlay {
  position: absolute;
  top: 10px;
  right: 10px;
}

.status-tag {
  border: none;
  font-weight: bold;
}

.book-content {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.book-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #2c3e50;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-author {
  font-size: 0.9rem;
  color: #666;
  margin: 0 0 4px 0;
}

.book-category {
  font-size: 0.8rem;
  color: #999;
  margin: 0 0 12px 0;
}

.book-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.book-price {
  font-size: 1.3rem;
  font-weight: bold;
  color: #f56c6c;
}

.book-actions {
  display: flex;
  gap: 8px;
}

.seller-info {
  display: flex;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.seller-avatar {
  margin-right: 8px;
}

.seller-name {
  font-size: 0.85rem;
  color: #666;
}

@media (max-width: 768px) {
  .book-image-container {
    height: 160px;
  }
  
  .book-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .book-actions {
    width: 100%;
  }
  
  .book-actions .el-button {
    flex: 1;
  }
}
</style>