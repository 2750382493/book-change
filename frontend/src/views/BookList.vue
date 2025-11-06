<template>
  <div class="book-list">
    <h1>书籍列表</h1>
    
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索书籍..."
        @keyup.enter="searchBooks"
        style="width: 300px; margin-right: 10px;"
      />
      <el-button type="primary" @click="searchBooks">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6" v-for="book in books" :key="book.id">
        <el-card class="book-card">
          <img :src="book.images && book.images[0] ? book.images[0] : '/default-book.jpg'" 
               class="book-image" />
          <div class="book-info">
            <h3>{{ book.title }}</h3>
            <p class="author">作者: {{ book.author || '未知' }}</p>
            <p class="price">¥{{ book.price }}</p>
            <p class="seller">卖家: {{ book.seller.nickname || book.seller.username }}</p>
            <el-button type="primary" @click="viewBook(book.id)">查看详情</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div v-if="books.length === 0" class="no-data">
      <p>暂无书籍</p>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

export default {
  name: 'BookList',
  setup() {
    const books = ref([])
    const searchKeyword = ref('')
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

    const resetSearch = () => {
      searchKeyword.value = ''
      fetchBooks()
    }

    const viewBook = (bookId) => {
      router.push(`/books/${bookId}`)
    }

    onMounted(() => {
      fetchBooks()
    })

    return {
      books,
      searchKeyword,
      searchBooks,
      resetSearch,
      viewBook
    }
  }
}
</script>

<style scoped>
.book-list {
  padding: 20px;
}
.search-bar {
  margin-bottom: 20px;
}
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
.author, .seller {
  color: #909399;
  font-size: 14px;
  margin: 5px 0;
}
.no-data {
  text-align: center;
  padding: 50px;
  color: #909399;
}
</style>