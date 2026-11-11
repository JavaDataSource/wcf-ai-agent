<script setup>
/**
 * 应用图标组件：各应用的 AI 默认头像图形，全站（头像、品牌区、主页卡片）复用。
 * - bot：机器人（AI 超级智能体）
 * - heart：爱心（AI 恋爱大师）
 * - home：房子（返回主页）
 * 颜色跟随 currentColor，由父元素控制。
 */
defineProps({
  type: { type: String, required: true },
  size: { type: Number, default: 22 },
})
</script>

<template>
  <!-- 机器人 -->
  <svg
    v-if="type === 'bot'"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <rect x="4.5" y="8.5" width="15" height="11" rx="3" />
    <path d="M12 8.5V4.5" />
    <circle cx="12" cy="3.5" r="1.2" fill="currentColor" stroke="none" />
    <circle cx="9.2" cy="13.5" r="1.3" fill="currentColor" stroke="none" />
    <circle cx="14.8" cy="13.5" r="1.3" fill="currentColor" stroke="none" />
    <path d="M2.5 13v3M21.5 13v3" />
  </svg>

  <!-- 爱心 -->
  <svg v-else-if="type === 'heart'" :width="size" :height="size" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <path
      d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
    />
  </svg>

  <!-- 房子 -->
  <svg
    v-else-if="type === 'home'"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path d="M3 10.5L12 3l9 7.5" />
    <path d="M5.5 9.5V20h13V9.5" />
    <path d="M10 20v-5h4v5" />
  </svg>
</template>
