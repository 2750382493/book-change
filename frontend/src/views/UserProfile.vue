<template>
  <div class="user-profile">
    <h1>个人中心</h1>
    
    <el-card>
      <h2>个人信息</h2>
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updateProfile">更新信息</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px;">
      <h2>我发布的书籍</h2>
      <el-table :data="myBooks" style="width: 100%">
        <el-table-column prop="title" label="书名" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="scope">
            ¥{{ scope.row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="sold" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.sold ? 'success' : 'warning'">
              {{ scope.row.sold ? '已售' : '在售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button type="text" @click="viewBook(scope.row.id)">查看</el-button>
            <el-button v-if="!scope.row.sold" type="text" @click="markAsSold(scope.row.id)">标记已售</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'UserProfile',
  setup() {
    const userForm = ref({})
    const myBooks = ref([])
    const router = useRouter()

    const loadUserData = () => {
      const userData = localStorage.getItem('user')
      if (userData) {
        userForm.value = JSON.parse(userData)
        fetchMyBooks()
      }
    }

    const fetchMyBooks = async () => {
      try {
        const response = await axios.get(`/api/books/user/${userForm.value.id}`)
        myBooks.value = response.data
      } catch (error) {
        console.error('获取我的书籍失败', error)
      }
    }

    const updateProfile = async () => {
      try {
        const response = await axios.put('/api/users/update', userForm.value)
        localStorage.setItem('user', JSON.stringify(response.data))
        ElMessage.success('更新成功')
      } catch (error) {
        ElMessage.error('更新失败')
        console.error('更新用户信息失败', error)
      }
    }

    const viewBook = (bookId) => {
      router.push(`/books/${bookId}`)
    }

    const markAsSold = async (bookId) => {
      try {
        await axios.put(`/api/books/${bookId}/sold`)
        ElMessage.success('标记已售成功')
        fetchMyBooks()
      } catch (error) {
        ElMessage.error('操作失败')
        console.error('标记已售失败', error)
      }
    }

    onMounted(() => {
      loadUserData()
    })

    return {
      userForm,
      myBooks,
      updateProfile,
      viewBook,
      markAsSold
    }
  }
}
</script>

<style scoped>
.user-profile {
  padding: 20px;
}
</style>