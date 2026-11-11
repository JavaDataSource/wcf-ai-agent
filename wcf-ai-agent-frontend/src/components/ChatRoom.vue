<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'

import AppIcon from '@/components/AppIcon.vue'
import { useSessions } from '@/composables/useSessions'

/**
 * 通用聊天室组件（页面 1 / 页面 2 复用），仿设计稿的卡片式布局：
 * - 左侧边栏：品牌区 + 新建会话按钮 + 历史会话列表（后端 MySQL 持久化，可切换）；
 * - 右侧聊天区：AI 消息在左（白底气泡 + “AI/♥”头像）、用户消息在右（主题色气泡 + “你”头像），气泡下带时间戳；
 * - 底部：快捷提问 chips + 大输入框 + 渐变发送按钮；
 * - 主题：manus 蓝色科技风（顶部通栏标题、底部通栏输入区）、love 粉色温柔风（品牌在侧边栏、输入区只在聊天列下方）。
 *
 * 流式输出两种模式：
 * - 合并模式（默认）：所有 SSE 数据流入同一个气泡，打字机效果（AI 恋爱大师）；
 * - 分气泡模式（perEventBubble）：每条 SSE 消息是一个完整步骤，独占一个气泡（AI 超级智能体）。
 *
 * 浏览器端 Axios（XHR）无法读取流式响应，SSE 使用原生 EventSource，
 * 由各应用页面通过 createEventSource(message, chatId) 传入各自的连接方式。
 */
const props = defineProps({
  /** 应用名称（侧边栏/顶部标题） */
  title: { type: String, required: true },
  /** 品牌副标题 */
  subtitle: { type: String, default: '' },
  /** 聊天区标题旁的标语（love 主题的会话头部展示） */
  tagline: { type: String, default: '' },
  /** 主题：manus 蓝色科技风 | love 粉色温柔风 */
  theme: { type: String, default: 'manus' },
  /** 应用标识：会话历史按应用隔离存储（后端 agent_chat_session.app_key），不同应用不能重复 */
  appKey: { type: String, required: true },
  /** 进入新会话时 AI 的欢迎语 */
  welcomeMessage: { type: String, default: '' },
  /** 输入框占位文案 */
  inputPlaceholder: { type: String, default: '输入你的问题，Enter 发送' },
  /** 空会话时的快捷提问 */
  quickPrompts: { type: Array, default: () => [] },
  /** 每条 SSE 消息是否单独渲染一个气泡（Manus 的步骤日志需要开启） */
  perEventBubble: { type: Boolean, default: false },
  /** 创建 SSE 连接：入参为用户消息和会话 id（即 chatId），返回 EventSource 实例 */
  createEventSource: { type: Function, required: true },
})

const isLoveTheme = computed(() => props.theme === 'love')

// ---------- 会话管理 ----------
const { sessions, activeId, messages, createTime, newSession, switchSession, touchSession, syncMessages, init } =
  useSessions(props.appKey, () => props.welcomeMessage)

init()

const activeSessionTitle = computed(
  () => sessions.value.find((s) => s.id === activeId.value)?.title || props.title,
)

// ---------- 聊天状态 ----------
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
// 上次持久化消息的时间戳（流式输出过程中节流持久化）
let lastPersistAt = 0
// 收到最后一条 SSE 数据的时间（用于空闲超时检测）
let lastDataAt = 0
// 连接持续 3 分钟没有任何数据视为失联，主动收尾
const INACTIVITY_TIMEOUT = 180000

const TYPE_INTERVAL = 30 // 每 30ms 输出一批字符

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
  messages.value.push({ role: 'user', content: message, time: createTime() })

  // 放入占位的 AI 消息，并保存它的响应式代理，后续所有更新都通过该代理进行
  messages.value.push({ role: 'ai', content: '', loading: true, time: createTime() })
  currentAiMessage = messages.value[messages.value.length - 1]

  pendingText = ''
  streamClosed = false
  isStreaming.value = true
  // 更新会话标题/时间，并把这条用户消息同步到后端
  touchSession(message)
  syncMessages()
  lastPersistAt = Date.now()
  lastDataAt = Date.now()
  scrollToBottom()

  let eventSource
  try {
    eventSource = props.createEventSource(message, activeId.value)
  } catch (error) {
    console.error('创建 SSE 连接失败', error)
    finalizeStream('创建连接失败，请检查后端服务是否已启动')
    return
  }
  currentEventSource = eventSource

  // 每收到一段数据先进入缓冲区，由打字机定时器平滑地输出到气泡
  eventSource.onmessage = (event) => {
    // 后端发送的流式结束标记：视为正常结束并主动关闭连接，
    // 避免 EventSource 在流结束后自动重连、导致智能体被重复执行
    if (event.data === '[DONE]') {
      handleStreamEnd(true)
      return
    }
    lastDataAt = Date.now()
    if (!event.data) {
      return
    }
    if (props.perEventBubble) {
      // 分气泡模式：每条 SSE 消息都是一个完整步骤
      if (currentAiMessage && !currentAiMessage.content && !pendingText) {
        // 当前是 handleSend 创建的、还没输出过内容的占位气泡，直接复用它
        pendingText = event.data
      } else {
        // 先把上一个气泡立即输出完，再为新消息单独开一个气泡
        flushPendingTextImmediately()
        messages.value.push({ role: 'ai', content: '', loading: true, time: createTime() })
        currentAiMessage = messages.value[messages.value.length - 1]
        pendingText = event.data
      }
      scrollToBottom()
    } else {
      // 合并模式：数据先进缓冲区，由打字机定时器平滑地输出到同一个气泡
      pendingText += event.data
    }
  }

  // 服务端结束流或连接异常时都会触发 error：必须手动 close，否则浏览器会自动重连
  eventSource.onerror = handleStreamEnd

  // 启动打字机定时器：负责把缓冲区文本平滑输出到气泡，以及流结束后的收尾检测
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
    // 流式过程中节流同步（每 2 秒），页面意外关闭也不丢已生成内容
    const now = Date.now()
    if (now - lastPersistAt > 2000) {
      lastPersistAt = now
      syncMessages()
    }
  }
  // 兜底 1：个别场景（如经代理的长连接中断）下连接已关闭但浏览器未触发 onerror，
  // 这里主动检测连接状态并收尾，避免界面一直卡在“输出中”
  if (!streamClosed && currentEventSource && currentEventSource.readyState === EventSource.CLOSED) {
    handleStreamEnd()
  }
  // 兜底 2：连接长时间没有任何数据（被静默挂起，readyState 一直是 OPEN），超时主动收尾
  if (!streamClosed && currentEventSource && Date.now() - lastDataAt > INACTIVITY_TIMEOUT) {
    handleStreamEnd(false, '连接长时间无响应，已自动断开，请稍后重试')
  }
  // 流已结束且缓冲区也输出完毕，本轮对话才算真正完成
  if (streamClosed && !pendingText) {
    finalizeStream()
  }
}

/** SSE 流结束（收到结束标记 / 连接异常 / 连接被关闭 / 空闲超时）：统一收尾 */
function handleStreamEnd(isCleanEnd = false, errorText = '连接中断或后端服务未启动，请稍后重试') {
  if (currentEventSource) {
    currentEventSource.close()
    currentEventSource = null
  }
  if (!isCleanEnd && !currentAiMessage.content && !pendingText) {
    // 非正常结束且一条数据都没收到，视为连接失败，立即提示错误
    finalizeStream(errorText)
    return
  }
  // 已收到过数据（或服务端正常结束）：等缓冲区输出完毕再收尾
  streamClosed = true
}

/** 立即把缓冲区中的剩余文本全部输出到当前气泡（用于切换气泡/收尾时） */
function flushPendingTextImmediately() {
  if (!currentAiMessage) {
    return
  }
  if (pendingText) {
    currentAiMessage.content += pendingText
    pendingText = ''
  }
  currentAiMessage.loading = false
  scrollToBottom()
}

/** 结束本轮对话：关闭连接、停止打字机、持久化并重置状态 */
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
  syncMessages()
  scrollToBottom()
}

/** 手动停止本轮对话 */
function handleStop() {
  if (currentAiMessage && !currentAiMessage.content && !pendingText) {
    if (props.perEventBubble && messages.value[messages.value.length - 1] === currentAiMessage) {
      // 分气泡模式下，还没输出内容的空占位气泡直接移除
      messages.value.pop()
    } else {
      currentAiMessage.content = '（已停止生成）'
    }
  }
  finalizeStream()
}

/** 新建会话：若正在输出则先结束当前对话 */
async function handleNewSession() {
  finalizeStream()
  await newSession()
  scrollToBottom()
}

/** 切换历史会话 */
async function handleSwitchSession(id) {
  if (id === activeId.value) {
    return
  }
  finalizeStream()
  await switchSession(id)
  scrollToBottom()
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

// 离开页面时同步剩余消息，并清理 SSE 连接和打字机定时器
onBeforeUnmount(() => {
  if (currentEventSource) {
    currentEventSource.close()
    currentEventSource = null
  }
  if (typeTimer) {
    clearInterval(typeTimer)
    typeTimer = null
  }
  syncMessages()
})
</script>

<template>
  <div class="chat-page" :class="`theme-${theme}`">
    <div class="shell">
      <!-- 顶部通栏（Manus 主题：品牌区放顶部） -->
      <header v-if="!isLoveTheme" class="top-header">
        <div class="brand">
          <div class="brand-logo">
            <AppIcon type="bot" :size="22" />
          </div>
          <div class="brand-text">
            <h1>{{ title }}</h1>
            <p>{{ subtitle }}</p>
          </div>
        </div>
        <div class="header-actions">
          <router-link class="home-btn" to="/" aria-label="返回主页">
            <AppIcon type="home" :size="18" />
          </router-link>
          <button class="ghost-btn" type="button" @click="handleNewSession">新建对话</button>
        </div>
      </header>

      <div class="shell-body">
        <!-- 左侧边栏：新建会话 + 历史会话列表 -->
        <aside class="sidebar">
          <div v-if="isLoveTheme" class="brand">
            <div class="brand-logo">
              <AppIcon type="heart" :size="24" />
            </div>
            <div class="brand-text">
              <h1>{{ title }}</h1>
              <p>{{ subtitle }}</p>
            </div>
          </div>

          <button class="new-session-btn" type="button" @click="handleNewSession">
            ＋ {{ isLoveTheme ? '开始一段新对话' : '新建会话' }}
          </button>

          <div class="history-label">{{ isLoveTheme ? '最近倾诉' : '历史会话' }}</div>

          <ul class="history-list">
            <li
              v-for="session in sessions"
              :key="session.id"
              :class="{ active: session.id === activeId }"
              @click="handleSwitchSession(session.id)"
            >
              <span class="history-title">{{ session.title }}</span>
              <span class="history-meta">{{ session.time }}</span>
            </li>
          </ul>

          <router-link class="home-link" to="/">🏠 返回主页</router-link>
        </aside>

        <!-- 右侧主区域 -->
        <main class="main">
          <!-- 会话标题栏（love 主题：当前会话标题 + 重新开始） -->
          <header v-if="isLoveTheme" class="main-header">
            <div class="main-header-title">
              <h2>{{ activeSessionTitle }}</h2>
              <p v-if="tagline">{{ tagline }}</p>
            </div>
            <div class="header-actions">
              <router-link class="home-btn" to="/" aria-label="返回主页">
                <AppIcon type="home" :size="18" />
              </router-link>
              <button class="ghost-btn" type="button" @click="handleNewSession">重新开始</button>
            </div>
          </header>

          <!-- 聊天记录：用户在右，AI 在左 -->
          <section ref="messageListRef" class="chat-messages">
            <div v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.role">
              <div class="avatar" :class="msg.role">
                <!-- AI 默认头像：Manus 用机器人、恋爱大师用爱心；用户展示“你” -->
                <AppIcon v-if="msg.role === 'ai'" :type="isLoveTheme ? 'heart' : 'bot'" :size="20" />
                <template v-else>你</template>
              </div>
              <div class="msg-col">
                <div class="bubble" :class="{ error: msg.error }">
                  <div class="bubble-text">{{ msg.content }}</div>
                  <!-- AI 正在输出时的打字动画 -->
                  <div v-if="msg.loading && !msg.content" class="typing">
                    <i></i><i></i><i></i>
                  </div>
                </div>
                <div class="msg-time">{{ msg.time }}</div>
              </div>
            </div>
          </section>
        </main>
      </div>

      <!-- 底部输入区：Manus 通栏；love 通过缩进只覆盖聊天列 -->
      <footer class="input-area">
        <!-- 快捷提问 -->
        <div v-if="!isStreaming && quickPrompts.length" class="chips">
          <button v-for="prompt in quickPrompts" :key="prompt" type="button" @click="handleQuickPrompt(prompt)">
            {{ prompt }}
          </button>
        </div>

        <div class="input-bar">
          <textarea
            ref="inputRef"
            v-model="userInput"
            class="chat-input"
            rows="1"
            :placeholder="inputPlaceholder"
            @keydown.enter.exact.prevent="handleSend"
            @input="resizeInput"
          ></textarea>
          <button v-if="isStreaming" class="send-btn stop" type="button" @click="handleStop">停止</button>
          <button v-else class="send-btn" type="button" :disabled="!userInput.trim()" @click="handleSend">
            发送
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  --sidebar-w: 272px;
  padding: 24px;
  display: flex;
}

/* ---------- 主题变量 ---------- */
.theme-manus.chat-page {
  background: #eef1f6;
  --page-accent: #4d7cfe;
  --accent-grad: linear-gradient(135deg, #4d7cfe 0%, #2bb3ff 100%);
  --user-bubble-bg: linear-gradient(135deg, #4d7cfe 0%, #2bb3ff 100%);
  --user-bubble-color: #fff;
  --sidebar-bg: #fafbfd;
  --active-item-bg: linear-gradient(90deg, #eaf1ff 0%, #e6f4ff 100%);
  --ghost-border: #d7dce6;
  --ghost-color: #4b5563;
  --chip-border: #dfe3ec;
  --chip-color: #5a6270;
  --input-border: #e2e6ee;
  --divider: #eef0f4;
}

.theme-love.chat-page {
  background: linear-gradient(135deg, #ffe9ee 0%, #fff3e9 100%);
  --page-accent: #f26d7e;
  --accent-grad: linear-gradient(135deg, #f97362 0%, #f7a76c 100%);
  --user-bubble-bg: #fdf0dd;
  --user-bubble-color: #6b4a3f;
  --sidebar-bg: #fdf1f4;
  --active-item-bg: #fff0f3;
  --ghost-border: #f3b7c5;
  --ghost-color: #d95d75;
  --chip-border: #f6c9d4;
  --chip-color: #d95d75;
  --input-border: #f6d3da;
  --divider: #f9e8ec;
}

/* ---------- 外壳卡片 ---------- */
.shell {
  flex: 1;
  max-width: 1180px;
  /* 高度 = 视口 - 全局页脚 - 上下内边距，保证页脚可见且聊天区内部滚动 */
  height: calc(100vh - var(--footer-height) - 48px);
  min-height: 420px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(31, 41, 55, 0.1);
  overflow: hidden;
}

.shell-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* ---------- 品牌区 ---------- */
.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--divider);
  flex-shrink: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-logo {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--accent-grad);
  flex-shrink: 0;
  user-select: none;
}

.brand-text h1 {
  font-size: 18px;
  line-height: 1.3;
}

.brand-text p {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

/* ---------- 侧边栏 ---------- */
.sidebar {
  width: var(--sidebar-w);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 16px 14px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--divider);
  overflow-y: auto;
}

.theme-love .sidebar {
  gap: 16px;
}

.new-session-btn {
  width: 100%;
  padding: 12px;
  font-size: 14px;
  border-radius: 12px;
  background: transparent;
  transition: all 0.2s;
}

.theme-manus .new-session-btn {
  border: 1.5px dashed #c8d2e0;
  color: #5a6270;
}

.theme-manus .new-session-btn:hover {
  border-color: var(--page-accent);
  color: var(--page-accent);
  background: #f3f7ff;
}

.theme-love .new-session-btn {
  border: 1px solid #f3b7c5;
  color: var(--ghost-color);
  border-radius: 999px;
  background: #fff;
}

.theme-love .new-session-btn:hover {
  border-color: var(--page-accent);
  background: var(--active-item-bg);
}

.history-label {
  font-size: 12px;
  color: #646a75;
  padding: 0 4px;
}

.history-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-list li {
  padding: 12px 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.theme-manus .history-list li:hover {
  background: #f0f4fa;
}

.theme-manus .history-list li.active {
  background: var(--active-item-bg);
}

.theme-love .history-list li {
  background: #fff;
  border: 1px solid transparent;
  border-radius: 14px;
  box-shadow: 0 4px 14px rgba(224, 138, 158, 0.1);
}

.theme-love .history-list li.active {
  border-color: #f3aebd;
  background: var(--active-item-bg);
}

.history-title {
  display: block;
  font-size: 14px;
  color: #2b3240;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-list li.active .history-title {
  color: var(--page-accent);
  font-weight: 600;
}

.history-meta {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #646a75;
}

.home-link {
  margin-top: auto;
  text-align: center;
  padding: 10px;
  border-radius: 10px;
  font-size: 13px;
  color: #6b7280;
  transition: all 0.2s;
}

.home-link:hover {
  color: var(--page-accent);
  background: var(--active-item-bg);
}

/* ---------- 主区域 ---------- */
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 26px;
  border-bottom: 1px solid var(--divider);
  flex-shrink: 0;
}

.main-header-title h2 {
  font-size: 17px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-header-title p {
  font-size: 12px;
  color: #9aa0ad;
  margin-top: 2px;
}

.ghost-btn {
  padding: 10px 22px;
  border-radius: 999px;
  font-size: 14px;
  background: #fff;
  border: 1px solid var(--ghost-border);
  color: var(--ghost-color);
  flex-shrink: 0;
  transition: all 0.2s;
}

.ghost-btn:hover {
  border-color: var(--page-accent);
  color: var(--page-accent);
}

/* ---------- 聊天记录 ---------- */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 28px 30px 16px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 22px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
  user-select: none;
}

.avatar.ai {
  background: var(--accent-grad);
  color: #fff;
}

.avatar.user {
  background: #eef1f6;
  color: var(--page-accent);
  font-size: 13px;
}

.theme-love .avatar.user {
  background: #ffe3e0;
  color: #e0646d;
}

.msg-col {
  max-width: 76%;
  min-width: 0;
}

.message-row.user .msg-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.bubble {
  padding: 13px 18px;
  border-radius: 16px;
  background: #fff;
  border-top-left-radius: 4px;
  box-shadow: 0 4px 16px rgba(31, 41, 55, 0.06);
}

.message-row.user .bubble {
  background: var(--user-bubble-bg);
  color: var(--user-bubble-color);
  border-top-left-radius: 16px;
  border-top-right-radius: 4px;
}

.bubble.error {
  background: #fef2f2;
  color: #b91c1c;
}

.bubble-text {
  font-size: 15px;
  line-height: 1.75;
  /* AI 与用户的文字统一在气泡内左对齐 */
  text-align: left;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-time {
  margin-top: 6px;
  font-size: 12px;
  color: #6f7680;
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
  background: var(--page-accent);
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

/* ---------- 底部输入区 ---------- */
.input-area {
  border-top: 1px solid var(--divider);
  padding: 14px 26px 20px;
  background: #fff;
  flex-shrink: 0;
}

/* love 主题：品牌在侧边栏，输入区只覆盖聊天列 */
.theme-love .input-area {
  margin-left: var(--sidebar-w);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.chips button {
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13px;
  background: #fff;
  border: 1px solid var(--chip-border);
  color: var(--chip-color);
  transition: all 0.2s;
}

.chips button:hover {
  border-color: var(--page-accent);
  color: var(--page-accent);
}

.input-bar {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  padding: 10px 10px 10px 22px;
  border: 1px solid var(--input-border);
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.05);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-bar:focus-within {
  border-color: var(--page-accent);
  box-shadow: 0 8px 24px rgba(77, 124, 254, 0.14);
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.6;
  color: #2b3240;
  min-height: 26px;
  max-height: 160px;
  background: transparent;
}

.chat-input::placeholder {
  color: #8a93a6;
}

.send-btn {
  padding: 13px 32px;
  border-radius: 16px;
  font-size: 15px;
  color: #fff;
  background: var(--accent-grad);
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  opacity: 0.88;
}

.send-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.send-btn.stop {
  background: linear-gradient(135deg, #ff9d6b 0%, #ff6b6b 100%);
}

/* ---------- 顶部/会话标题栏右侧操作区 ---------- */
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

/* 返回主页按钮：桌面端侧边栏已有入口，默认隐藏；小屏侧边栏隐藏时展示 */
.home-btn {
  display: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--ghost-border);
  color: var(--ghost-color);
  background: #fff;
  transition: all 0.2s;
  flex-shrink: 0;
}

.home-btn:hover {
  border-color: var(--page-accent);
  color: var(--page-accent);
}

/* ---------- 小屏适配 ---------- */
@media (max-width: 1024px) {
  /* 平板：收窄侧边栏 */
  .chat-page {
    --sidebar-w: 224px;
  }

  .sidebar {
    padding: 16px 12px 12px;
  }

  .msg-col {
    max-width: 82%;
  }
}

@media (max-width: 768px) {
  /* 手机/小平板：隐藏侧边栏，会话操作走顶部按钮 */
  .sidebar {
    display: none;
  }

  .theme-love .input-area {
    margin-left: 0;
  }

  .home-btn {
    display: flex;
  }

  .msg-col {
    max-width: 88%;
  }
}

@media (max-width: 640px) {
  .chat-page {
    padding: 12px;
  }

  .shell {
    height: calc(100vh - var(--footer-height-mobile) - 24px);
    min-height: 380px;
    border-radius: 16px;
  }

  .top-header,
  .main-header {
    padding: 12px 16px;
  }

  .brand-text p {
    display: none;
  }

  .ghost-btn {
    padding: 8px 16px;
    font-size: 13px;
  }

  .avatar {
    width: 36px;
    height: 36px;
  }

  .chat-messages {
    padding: 18px 14px 10px;
  }

  .message-row {
    gap: 10px;
    margin-bottom: 18px;
  }

  .bubble {
    padding: 11px 14px;
  }

  .bubble-text {
    font-size: 14px;
  }

  .input-area {
    padding: 10px 14px 14px;
  }

  /* 快捷提问横向滑动 */
  .chips {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
  }

  .chips::-webkit-scrollbar {
    display: none;
  }

  .chips button {
    flex-shrink: 0;
  }

  .input-bar {
    padding: 8px 8px 8px 16px;
    border-radius: 16px;
  }

  .send-btn {
    padding: 11px 22px;
    font-size: 14px;
  }
}
</style>
