import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      // 主页：用于切换不同的应用
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      // 页面 1：AI 恋爱大师（SSE 流式对话）
      path: '/love-app',
      name: 'loveApp',
      component: () => import('@/views/LoveAppChat.vue'),
    },
    {
      // 页面 2：AI 超级智能体（SSE 流式对话）
      path: '/manus',
      name: 'manus',
      component: () => import('@/views/ManusChat.vue'),
    },
    // 兜底：重定向回主页
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

export default router
