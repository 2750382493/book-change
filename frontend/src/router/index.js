import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import BookList from '../views/BookList.vue'
import BookDetail from '../views/BookDetail.vue'
import UserProfile from '../views/UserProfile.vue'
import Messages from '../views/Messages.vue'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/books', name: 'BookList', component: BookList },
  { path: '/books/:id', name: 'BookDetail', component: BookDetail },
  { path: '/profile', name: 'UserProfile', component: UserProfile },
  { path: '/messages', name: 'Messages', component: Messages }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router