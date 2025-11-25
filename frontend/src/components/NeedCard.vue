<template>
  <el-card class="need-card">
    <div class="need-header">
      <h3 class="need-title">{{ need.title }}</h3>
      <el-tag v-if="need.fulfilled" type="success">已满足</el-tag>
      <el-tag v-else type="warning">求购中</el-tag>
    </div>
    
    <div class="need-content">
      <p class="need-author" v-if="need.author">作者: {{ need.author }}</p>
      <p class="need-isbn" v-if="need.isbn">ISBN: {{ need.isbn }}</p>
      <p class="need-description" v-if="need.description">{{ need.description }}</p>
      
      <div class="need-meta">
        <div class="need-price" v-if="need.maxPrice">
          最高预算: <span class="price">¥{{ need.maxPrice }}</span>
        </div>
        <div class="need-category" v-if="need.category">
          分类: <el-tag size="small">{{ need.category }}</el-tag>
        </div>
      </div>
    </div>
    
    <div class="need-footer">
      <div class="user-info">
        <el-avatar :size="32" :src="need.user.avatar" class="user-avatar">
          {{ need.user.nickname ? need.user.nickname.charAt(0) : need.user.username.charAt(0) }}
        </el-avatar>
        <div class="user-details">
          <span class="user-name">{{ need.user.nickname || need.user.username }}</span>
          <span class="create-time">{{ formatTime(need.createTime) }}</span>
        </div>
      </div>
      
      <div class="need-actions" v-if="showActions">
        <el-button 
          v-if="!need.fulfilled"
          type="success" 
          size="small"
          @click="$emit('fulfilled', need.id)"
        >
          标记满足
        </el-button>
        <el-button 
          type="danger" 
          size="small"
          @click="handleDelete"
        >
          删除
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'NeedCard',
  props: {
    need: {
      type: Object,
      required: true
    },
    showActions: {
      type: Boolean,
      default: false
    }
  },
  emits: ['fulfilled', 'delete'],
  setup(props, { emit }) {
    const formatTime = (timeString) => {
      const date = new Date(timeString)
      const now = new Date()
      const diff = now - date
      
      if (diff < 60 * 1000) {
        return '刚刚'
      } else if (diff < 60 * 60 * 1000) {
        return Math.floor(diff / (60 * 1000)) + '分钟前'
      } else if (diff < 24 * 60 * 60 * 1000) {
        return Math.floor(diff / (60 * 60 * 1000)) + '小时前'
      } else {
        return date.toLocaleDateString()
      }
    }

    const handleDelete = async () => {
      try {
        await ElMessageBox.confirm(
          '确定要删除这个需求吗？',
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        emit('delete', props.need.id)
      } catch {
        // 用户取消删除
      }
    }

    return {
      formatTime,
      handleDelete
    }
  }
}
</script>

<style scoped>
.need-card {
  border-radius: 12px;
  transition: all 0.3s ease;
  height: 100%;
}

.need-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.need-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.need-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
  flex: 1;
  margin-right: 12px;
  line-height: 1.4;
}

.need-content {
  margin-bottom: 16px;
}

.need-author, .need-isbn {
  font-size: 0.9rem;
  color: #666;
  margin: 0 0 6px 0;
}

.need-description {
  font-size: 0.9rem;
  color: #555;
  line-height: 1.5;
  margin: 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.need-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.need-price {
  font-size: 0.9rem;
  color: #666;
}

.need-price .price {
  font-weight: bold;
  color: #f56c6c;
  font-size: 1.1rem;
}

.need-category {
  font-size: 0.9rem;
  color: #666;
}

.need-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 0.9rem;
  font-weight: 500;
  color: #2c3e50;
}

.create-time {
  font-size: 0.8rem;
  color: #999;
}

.need-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .need-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .need-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .need-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .need-actions {
    width: 100%;
  }
  
  .need-actions .el-button {
    flex: 1;
  }
}
</style>