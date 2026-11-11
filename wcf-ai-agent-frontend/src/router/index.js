import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'

const SITE_NAME = 'AI 应用广场'

/**
 * 更新 <meta> 标签（SPA 单页应用的按页 SEO）。
 * SPA 没有 SSR，搜索引擎主要读取入口 HTML 与当前 document.title，
 * 因此路由切换时同步更新标题、描述、关键词与 Open Graph 信息。
 */
function setMeta(attr, key, content) {
  if (!content) {
    return
  }
  let el = document.head.querySelector(`meta[${attr}="${key}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      // 主页：用于切换不同的应用
      path: '/',
      name: 'home',
      component: HomeView,
      meta: {
        title: 'AI 应用广场 | AI 恋爱大师 · AI 超级智能体',
        description:
          'AI 应用广场：与 AI 恋爱大师聊聊情感话题、写表白情书，或让 AI 超级智能体 Manus 自主规划任务、调用工具帮你完成复杂工作，全程 SSE 流式输出。',
        keywords: 'AI 应用,AI 恋爱大师,AI 超级智能体,Manus,智能体,Spring AI,Vue 3,SSE 流式',
      },
    },
    {
      // 页面 1：AI 恋爱大师（SSE 流式对话）
      path: '/love-app',
      name: 'loveApp',
      component: () => import('@/views/LoveAppChat.vue'),
      meta: {
        title: 'AI 恋爱大师 - 你的专属情感顾问',
        description:
          '和 AI 恋爱大师聊聊恋爱话题：表白情书、约会攻略、情感困惑，温柔风 AI 情感顾问，回复全程 SSE 流式输出，支持多会话隔离。',
        keywords: 'AI 恋爱大师,情感顾问,表白情书,恋爱聊天,AI 陪聊,SSE 流式对话',
      },
    },
    {
      // 页面 2：AI 超级智能体（SSE 流式对话）
      path: '/manus',
      name: 'manus',
      component: () => import('@/views/ManusChat.vue'),
      meta: {
        title: 'AI 超级智能体 Manus - 自主规划 · 多工具协同',
        description:
          'AI 超级智能体 Manus：自主拆解任务、规划步骤并调用搜索、网页抓取、文件生成等工具，每个执行步骤实时流式展示。',
        keywords: 'AI 超级智能体,Manus,智能体,自主规划,工具调用,任务拆解,实时输出',
      },
    },
    // 兜底：重定向回主页
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// 路由切换时同步更新页面 SEO 信息
router.afterEach((to) => {
  const { title, description, keywords } = to.meta
  document.title = title || SITE_NAME
  setMeta('name', 'description', description)
  setMeta('name', 'keywords', keywords)
  setMeta('property', 'og:title', title)
  setMeta('property', 'og:description', description)
  setMeta('name', 'twitter:title', title)
  setMeta('name', 'twitter:description', description)
})

export default router
