<script setup>
import { ref } from 'vue'

import ChatRoom from '@/components/ChatRoom.vue'

/**
 * 页面 1：AI 恋爱大师
 * 通过 SSE 调用后端接口：GET /api/ai/love_app/chat/sse?message=xxx&chatId=xxx
 * 进入页面后自动生成一个聊天室 id（同一浏览器标签页内复用，用于区分不同会话），
 * 点击“新建会话”可重新生成。
 */
const CHAT_ID_KEY = 'loveAppChatId'

const chatRoomKey = ref(0)
const chatId = ref(loadOrCreateChatId())

function createChatId() {
  if (window.crypto && window.crypto.randomUUID) {
    return window.crypto.randomUUID()
  }
  // 兜底方案
  return `chat-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function loadOrCreateChatId() {
  let id = sessionStorage.getItem(CHAT_ID_KEY)
  if (!id) {
    id = createChatId()
    sessionStorage.setItem(CHAT_ID_KEY, id)
  }
  return id
}

/** 建立 SSE 连接：EventSource 会自动携带流式响应所需的请求头 */
function createEventSource(message) {
  const params = new URLSearchParams({
    message,
    chatId: chatId.value,
  })
  // 开发环境经 Vite 代理转发到 http://localhost:8123
  return new EventSource(`/api/ai/love_app/chat/sse?${params.toString()}`)
}

/** 新建会话：重新生成聊天室 id 并清空聊天记录（通过 key 重建 ChatRoom 组件） */
function handleNewSession() {
  chatId.value = createChatId()
  sessionStorage.setItem(CHAT_ID_KEY, chatId.value)
  chatRoomKey.value += 1
}
</script>

<template>
  <ChatRoom
    :key="chatRoomKey"
    title="AI 恋爱大师"
    subtitle="懂你的情感顾问 · SSE 流式对话"
    ai-avatar="💘"
    :header-extra="`会话 ID：${chatId}`"
    welcome-message="你好呀，我是你的专属恋爱大师 💘 无论是脱单技巧、约会攻略、情感困惑，还是想让我帮你写表白情书，都可以直接跟我说～"
    :quick-prompts="[
      '帮我写一段打动人的表白情书',
      '第一次约会应该注意什么？',
      '和女朋友吵架了，怎么哄比较好？',
    ]"
    :create-event-source="createEventSource"
    @new-session="handleNewSession"
  />
</template>
