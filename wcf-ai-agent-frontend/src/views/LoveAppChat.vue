<script setup>
import ChatRoom from '@/components/ChatRoom.vue'

/**
 * 页面 1：AI 恋爱大师（粉色温柔风主题）
 * 通过 SSE 调用后端接口：GET /api/ai/love_app/chat/sse?message=xxx&chatId=xxx
 * 会话由 ChatRoom 统一管理（localStorage 持久化），会话 id 即后端的 chatId，
 * 同一会话内多轮对话共享上下文，不同会话相互隔离。
 */

/** 建立 SSE 连接：EventSource 会自动携带流式响应所需的请求头 */
function createEventSource(message, chatId) {
  const params = new URLSearchParams({
    message,
    chatId,
  })
  // 开发环境经 Vite 代理转发到 http://localhost:8123
  return new EventSource(`/api/ai/love_app/chat/sse?${params.toString()}`)
}
</script>

<template>
  <ChatRoom
    theme="love"
    storage-key="loveApp"
    title="AI 恋爱大师"
    subtitle="你的专属情感顾问"
    tagline="温柔风 · 有情绪回应"
    input-placeholder="输入你的问题，慢慢说都可以"
    welcome-message="你好呀，我是你的专属恋爱大师 💘 无论是脱单技巧、约会攻略、情感困惑，还是想让我帮你写表白情书，都可以直接跟我说～"
    :quick-prompts="[
      '帮我写一段打动人的表白情书',
      '第一次约会应该注意什么？',
      '和女朋友吵架了，怎么哄比较好？',
    ]"
    :create-event-source="createEventSource"
  />
</template>
