<script setup>
import ChatRoom from '@/components/ChatRoom.vue'

/**
 * 页面 2：AI 超级智能体（Manus，蓝色科技风主题）
 * 通过 SSE 调用后端接口：GET /api/ai/manus/chat?message=xxx
 * 后端每次 SSE 消息对应智能体的一个执行步骤，因此开启 per-event-bubble，
 * 让每个步骤单独用一个气泡展示（AI 恋爱大师保持默认的合并流式模式）。
 */

/** 建立 SSE 连接：该接口只需 message 参数 */
function createEventSource(message) {
  const params = new URLSearchParams({ message })
  // 开发环境经 Vite 代理转发到 http://localhost:8123
  return new EventSource(`/api/ai/manus/chat?${params.toString()}`)
}
</script>

<template>
  <ChatRoom
    theme="manus"
    storage-key="manus"
    title="AI 超级智能体"
    subtitle="自主规划 · 多工具协同 · 实时输出"
    input-placeholder="请输入你的需求，Enter 发送"
    per-event-bubble
    welcome-message="你好！我是 AI 超级智能体 Manus 🚀 我会自主拆解任务、规划步骤并调用工具来帮你解决问题，直接输入你的需求开始吧～"
    :quick-prompts="[
      '帮我查一下北京今天的天气',
      '帮我制定一份一周的学习计划',
      '帮我分析当下 AI 行业的就业前景',
    ]"
    :create-event-source="createEventSource"
  />
</template>
