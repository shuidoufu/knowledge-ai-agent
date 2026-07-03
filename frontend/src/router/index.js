import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '../utils/auth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '应用中心' },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/love',
    name: 'LoveApp',
    component: () => import('../views/LoveChat.vue'),
    meta: { title: 'AI 恋爱大师', requiresAuth: true },
  },
  {
    path: '/manus',
    name: 'ManusApp',
    component: () => import('../views/ManusChat.vue'),
    meta: { title: 'AI 超级智能体', requiresAuth: true },
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('../views/ChangePassword.vue'),
    meta: { title: '修改密码', requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta?.title) {
    document.title = to.meta.title + ' - AI 应用中心'
  }
  // 需要登录的页面：未登录则跳转登录页，并记录 returnUrl 便于登录后返回
  if (to.meta?.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { returnUrl: to.fullPath } }
  }
})

export default router
