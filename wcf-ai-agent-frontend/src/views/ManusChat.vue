<script setup>
import { ref } from 'vue'

import ChatRoom from '@/components/ChatRoom.vue'

/**
 * 页面 2：AI 超级智能体（Manus）
 * 通过 SSE 调用后端接口：GET /api/ai/manus/chat?message=xxx
 * 页面风格与 AI 恋爱大师一致，实时展示智能体的执行 / 回复内容。
 */
const chatRoomKey = ref(0)

/** 建立 SSE 连接：该接口只需 message 参数 */
function createEventSource(message) {
  const params = new URLSearchParams({ message })
  // 开发环境经 Vite 代理转发到 http://localhost:8123
  return new EventSource(`/api/ai/manus/chat?${params.toString()}`)
}

/** 新建会话：清空聊天记录（通过 key 重建 ChatRoom 组件） */
function handleNewSession() {
  chatRoomKey.value += 1
}
</script>

<template>
  <ChatRoom
    :key="chatRoomKey"
    title="AI 超级智能体"
    subtitle="自主规划 · 多工具协同 · 实时输出"
    ai-avatar="🚀"
    header-extra="模式：Manus 自主执行"
    :message-separator="'\n'"
    welcome-message="你好！我是 AI 超级智能体 Manus 🚀 我会自主拆解任务、规划步骤并调用工具来帮你解决问题，直接输入你的需求开始吧～"
    :quick-prompts="[
      '帮我查一下北京今天的天气',
      '帮我制定一份一周的学习计划',
      '帮我分析当下 AI 行业的就业前景',
    ]"
    :create-event-source="createEventSource"
    @new-session="handleNewSession"
  />
</template>
