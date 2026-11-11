<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'

/**
 * 通用聊天室组件（页面 1 / 页面 2 复用）：
 * - 上方为聊天记录：用户消息在右、AI 消息在左；
 * - 下方为输入框，按 Enter 发送、Shift + Enter 换行；
 * - 通过 SSE（EventSource）流式接收后端返回，在气泡中做“打字机”式流式输出。
 *
 * 由于浏览器端的 Axios（XHR）无法读取流式响应，SSE 场景使用原生 EventSource，
 * 两个应用页面只需通过 createEventSource 属性传入各自的连接方式即可。
 */
const props = defineProps({
  /** 页面标题 */
  title: { type: String, required: true },
  /** 标题下方的副标题 */
  subtitle: { type: String, default: '' },
  /** AI 头像（emoji） */
  aiAvatar: { type: String, default: '🤖' },
  /** 用户头像（emoji） */
  userAvatar: { type: String, default: '🙋' },
  /** 头部右侧附加信息，如会话 ID */
  headerExtra: { type: String, default: '' },
  /** 进入页面时 AI 的欢迎语 */
  welcomeMessage: { type: String, default: '' },
  /** 空会话时的快捷提问 */
  quickPrompts: { type: Array, default: () => [] },
  /** 每个 SSE 事件后追加的分隔符：当后端每次 send 是一条独立消息（如 Manus 的步骤日志）时，用换行分隔显示 */
  messageSeparator: { type: String, default: '' },
  /**
   * 创建 SSE 连接的函数：入参为用户消息，返回 EventSource 实例。
   * 由各应用页面自行决定调用哪个后端接口。
   */
  createEventSource: { type: Function, required: true },
})

const emit = defineEmits(['new-session'])

// 消息结构：{ role: 'user' | 'ai', content: string, loading?: boolean, error?: boolean }
const messages = ref([])
const userInput = ref('')
const isStreaming = ref(false)
const messageListRef = ref(null)
const inputRef = ref(null)

// ---------- 以下为无需响应式的内部状态 ----------
// 当前打开的 SSE 连接，离开页面 / 停止对话时需要关闭
let currentEventSource = null
// 打字机定时器
let typeTimer = null
// 打字机缓冲区：后端推送的文本先进缓冲区，再由定时器逐段输出到气泡
let pendingText = ''
/**
 * 正在输出的 AI 消息。
 * 注意：必须保存从 messages.value 中取出的“响应式代理”对象！
 * 若直接修改 push 进数组的原始对象，Vue 侦测不到变化，气泡不会实时刷新。
 */
let currentAiMessage = null
// SSE 流是否已结束（结束后等缓冲区输出完才算本轮完成）
let streamClosed = false

const TYPE_INTERVAL = 30 // 每 30ms 输出一批字符

// 进入页面时的 AI 欢迎语
if (props.welcomeMessage) {
  messages.value.push({ role: 'ai', content: props.welcomeMessage })
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

/** 发送消息：先追加用户消息和 AI 占位消息，再通过 SSE 流式接收 AI 回复 */
function handleSend() {
  const message = userInput.value.trim()
  if (!message || isStreaming.value) {
    return
  }
  userInput.value = ''
  resizeInput()
  messages.value.push({ role: 'user', content: message })

  // 放入占位的 AI 消息，并保存它的响应式代理，后续所有更新都通过该代理进行
  messages.value.push({ role: 'ai', content: '', loading: true })
  currentAiMessage = messages.value[messages.value.length - 1]

  pendingText = ''
  streamClosed = false
  isStreaming.value = true
  scrollToBottom()

  let eventSource
  try {
    eventSource = props.createEventSource(message)
  } catch (error) {
    console.error('创建 SSE 连接失败', error)
    finalizeStream('创建连接失败，请检查后端服务是否已启动')
    return
  }
  currentEventSource = eventSource

  // 每收到一段数据先进入缓冲区，由打字机定时器平滑地输出到气泡
  eventSource.onmessage = (event) => {
    if (event.data) {
      pendingText += event.data + props.messageSeparator
    }
  }

  // 服务端结束流或连接异常时都会触发 error：必须手动 close，否则浏览器会自动重连
  eventSource.onerror = () => {
    eventSource.close()
    currentEventSource = null
    if (!currentAiMessage.content && !pendingText) {
      // 一条数据都没收到，视为连接失败，立即提示错误
      finalizeStream('连接中断或后端服务未启动，请稍后重试')
    } else {
      // 已收到过数据，视为服务端正常结束：等缓冲区输出完毕再收尾
      streamClosed = true
    }
  }

  startTypewriter()
}

/** 启动打字机定时器 */
function startTypewriter() {
  typeTimer = setInterval(flushTypewriter, TYPE_INTERVAL)
}

/** 定时把缓冲区里的文本逐段输出到气泡中（打字机效果） */
function flushTypewriter() {
  if (!currentAiMessage) {
    return
  }
  if (pendingText) {
    // 自适应速率：积压越多输出越快，保证流畅的同时不会越攒越多
    const step = Math.max(2, Math.ceil(pendingText.length / 15))
    currentAiMessage.content += pendingText.slice(0, step)
    pendingText = pendingText.slice(step)
    scrollToBottom()
  }
  // 流已结束且缓冲区也输出完毕，本轮对话才算真正完成
  if (streamClosed && !pendingText) {
    finalizeStream()
  }
}

/** 结束本轮对话：关闭连接、停止打字机并重置状态 */
function finalizeStream(errorText = '') {
  if (currentEventSource) {
    currentEventSource.close()
    currentEventSource = null
  }
  if (typeTimer) {
    clearInterval(typeTimer)
    typeTimer = null
  }
  if (currentAiMessage) {
    currentAiMessage.loading = false
    if (errorText && !currentAiMessage.content) {
      currentAiMessage.error = true
      currentAiMessage.content = errorText
    }
  }
  currentAiMessage = null
  pendingText = ''
  streamClosed = false
  isStreaming.value = false
  scrollToBottom()
}

/** 手动停止本轮对话 */
function handleStop() {
  if (currentAiMessage && !currentAiMessage.content && !pendingText) {
    currentAiMessage.content = '（已停止生成）'
  }
  finalizeStream()
}

/** 点击快捷提问直接发送 */
function handleQuickPrompt(prompt) {
  if (isStreaming.value) {
    return
  }
  userInput.value = prompt
  handleSend()
}

/** 输入框高度自适应（最多 160px） */
function resizeInput() {
  const el = inputRef.value
  if (!el) {
    return
  }
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 160)}px`
}

// 离开页面时关闭 SSE 连接和打字机定时器
onBeforeUnmount(() => {
  if (currentEventSource) {
    currentEventSource.close()
    currentEventSource = null
  }
  if (typeTimer) {
    clearInterval(typeTimer)
    typeTimer = null
  }
})
</script>

<template>
  <div class="chat-page">
    <!-- 顶部标题栏 -->
    <header class="chat-header">
      <div class="header-left">
        <router-link class="back-btn" to="/" title="返回主页">←</router-link>
        <div class="header-title">
          <h1>{{ title }}</h1>
          <p v-if="subtitle">{{ subtitle }}</p>
        </div>
      </div>
      <div class="header-right">
        <span v-if="headerExtra" class="header-extra">{{ headerExtra }}</span>
        <button class="new-session-btn" type="button" @click="emit('new-session')">＋ 新建会话</button>
      </div>
    </header>

    <!-- 聊天记录：用户在右，AI 在左 -->
    <main ref="messageListRef" class="chat-messages">
      <div class="message-list">
        <div v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.role">
          <div class="avatar">{{ msg.role === 'user' ? userAvatar : aiAvatar }}</div>
          <div class="bubble" :class="{ error: msg.error }">
            <div class="bubble-text">{{ msg.content }}</div>
            <!-- AI 正在输出时的打字动画 -->
            <div v-if="msg.loading && !msg.content" class="typing">
              <i></i><i></i><i></i>
            </div>
          </div>
        </div>

        <!-- 快捷提问 -->
        <div v-if="!isStreaming && quickPrompts.length" class="quick-prompts">
          <button
            v-for="prompt in quickPrompts"
            :key="prompt"
            type="button"
            @click="handleQuickPrompt(prompt)"
          >
            {{ prompt }}
          </button>
        </div>
      </div>
    </main>

    <!-- 底部输入区 -->
    <footer class="chat-input-area">
      <div class="input-wrapper">
        <textarea
          ref="inputRef"
          v-model="userInput"
          class="chat-input"
          rows="1"
          placeholder="输入你的问题，Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="handleSend"
          @input="resizeInput"
        ></textarea>
        <button v-if="isStreaming" class="send-btn stop" type="button" @click="handleStop">
          停止
        </button>
        <button v-else class="send-btn" type="button" :disabled="!userInput.trim()" @click="handleSend">
          发送
        </button>
      </div>
      <p class="input-tip">内容由 AI 生成，仅供参考</p>
    </footer>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(180deg, #eef2ff 0%, var(--color-bg) 260px);
}

/* ---------- 顶部 ---------- */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--color-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 18px;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  transition: all 0.2s;
  flex-shrink: 0;
}

.back-btn:hover {
  color: var(--color-primary);
  background: #e6ebff;
}

.header-title h1 {
  font-size: 18px;
  line-height: 1.3;
}

.header-title p {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.header-extra {
  max-width: 380px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 5px 12px;
}

.new-session-btn {
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
  background: var(--color-primary-gradient);
  transition: opacity 0.2s;
}

.new-session-btn:hover {
  opacity: 0.85;
}

/* ---------- 聊天记录 ---------- */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 16px 8px;
}

.message-list {
  max-width: 860px;
  margin: 0 auto;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: 20px;
  background: var(--color-surface);
  box-shadow: 0 2px 8px rgba(31, 35, 41, 0.08);
  flex-shrink: 0;
  user-select: none;
}

.bubble {
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 4px 16px 16px 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: 0 2px 6px rgba(31, 35, 41, 0.04);
}

.message-row.user .bubble {
  border-radius: 16px 4px 16px 16px;
  background: var(--color-primary-gradient);
  border: none;
  color: #fff;
}

.bubble.error {
  background: #fef2f2;
  border-color: #fecaca;
  color: var(--color-danger);
}

.bubble-text {
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 打字动画 */
.typing {
  display: inline-flex;
  gap: 5px;
  padding: 4px 2px;
}

.typing i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-primary);
  opacity: 0.4;
  animation: typing-blink 1.2s infinite ease-in-out;
}

.typing i:nth-child(2) {
  animation-delay: 0.2s;
}

.typing i:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing-blink {
  0%,
  80%,
  100% {
    opacity: 0.25;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

/* 快捷提问 */
.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 4px 0 8px 52px;
}

.quick-prompts button {
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--color-primary);
  background: var(--color-surface);
  border: 1px solid #d9e0ff;
  transition: all 0.2s;
}

.quick-prompts button:hover {
  background: #eef1ff;
  border-color: var(--color-primary);
}

/* ---------- 输入区 ---------- */
.chat-input-area {
  padding: 12px 16px 14px;
  background: rgba(255, 255, 255, 0.9);
  border-top: 1px solid var(--color-border);
  backdrop-filter: blur(8px);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  max-width: 860px;
  margin: 0 auto;
  padding: 10px 10px 10px 18px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 14px rgba(31, 35, 41, 0.06);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-wrapper:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 4px 18px rgba(79, 110, 247, 0.15);
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text);
  min-height: 24px;
  max-height: 160px;
  background: transparent;
}

.chat-input::placeholder {
  color: #b0b6c3;
}

.send-btn {
  padding: 10px 22px;
  border-radius: 12px;
  font-size: 14px;
  color: #fff;
  background: var(--color-primary-gradient);
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  opacity: 0.85;
}

.send-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.send-btn.stop {
  background: linear-gradient(135deg, #ff9d6b 0%, #ff6b6b 100%);
}

.input-tip {
  max-width: 860px;
  margin: 8px auto 0;
  text-align: center;
  font-size: 12px;
  color: #b0b6c3;
}

/* 小屏适配 */
@media (max-width: 640px) {
  .header-extra {
    display: none;
  }

  .bubble {
    max-width: 84%;
  }
}
</style>
