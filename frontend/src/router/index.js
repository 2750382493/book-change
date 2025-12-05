import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

// 路由组件懒加载，提升首屏加载速度
const Home = () => import('../views/Home.vue')
const Login = () => import('../views/Login.vue')
const Register = () => import('../views/Register.vue')
const BookList = () => import('../views/BookList.vue')
const BookDetail = () => import('../views/BookDetail.vue')
const UserProfile = () => import('../views/UserProfile.vue')
const Messages = () => import('../views/Messages.vue')
const Needs = () => import('../views/Needs.vue')

/**
 * 路由配置表
 * meta.requiresAuth: 标记该路由是否需要登录权限
 * meta.title: 页面标题
 */
const routes = [
  { 
    path: '/', 
    name: 'Home', 
    component: Home,
    meta: { title: '首页 - 校园二手书交易平台' }
  },
  { 
    path: '/login', 
    name: 'Login', 
    component: Login,
    meta: { title: '用户登录' }
  },
  { 
    path: '/register', 
    name: 'Register', 
    component: Register,
    meta: { title: '用户注册' }
  },
  { 
    path: '/books', 
    name: 'BookList', 
    component: BookList,
    meta: { title: '二手书市场' }
  },
  { 
    path: '/books/:id', 
    name: 'BookDetail', 
    component: BookDetail,
    meta: { title: '图书详情' }
  },
  { 
    path: '/profile', 
    name: 'UserProfile', 
    component: UserProfile,
    meta: { 
      title: '个人中心',
      requiresAuth: true // 需要登录
    }
  },
  { 
    path: '/messages', 
    name: 'Messages', 
    component: Messages,
    meta: { 
      title: '我的消息',
      requiresAuth: true 
    }
  },
  { 
    path: '/needs', 
    name: 'Needs', 
    component: Needs,
    meta: { title: '求书广场' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 路由切换时滚动条复位
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

/**
 * 全局前置守卫
 * 用于处理登录权限校验和页面标题修改
 */
router.beforeEach((to, from, next) => {
  // 1. 设置网页标题
  document.title = to.meta.title || '校园二手书交易平台'

  // 2. 获取用户登录状态 (Token)
  const token = localStorage.getItem('token')
  
  // 3. 权限校验逻辑
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      ElMessage.warning('请先登录后再访问')
      next({
        path: '/login',
        query: { redirect: to.fullPath } // 登录后跳转回原页面
      })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router