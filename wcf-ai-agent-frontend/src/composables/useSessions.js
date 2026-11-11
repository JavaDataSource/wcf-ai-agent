import { ref } from 'vue'

/**
 * 会话管理组合式函数：
 * - 会话列表与每个会话的消息持久化在 localStorage，刷新页面不丢失；
 * - 每个会话有唯一 id，AI 恋爱大师直接把它作为后端的 chatId，实现真正的多会话隔离。
 */
export function useSessions(storageKey, buildWelcome) {
  const sessions = ref([]) // 会话列表：[{ id, title, time, titleFixed }]
  const activeId = ref('') // 当前会话 id
  const messages = ref([]) // 当前会话的消息列表

  const sessionsStoreKey = `${storageKey}:sessions`
  const activeStoreKey = `${storageKey}:active`
  const messagesKey = (id) => `${storageKey}:msgs:${id}`

  /** 生成 HH:mm 格式的当前时间 */
  function createTime() {
    const now = new Date()
    return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  }

  /** 生成会话 id（同时用作后端 chatId） */
  function createId() {
    if (window.crypto && window.crypto.randomUUID) {
      return window.crypto.randomUUID()
    }
    return `chat-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  }

  function loadJson(key, fallback) {
    try {
      return JSON.parse(localStorage.getItem(key)) ?? fallback
    } catch {
      return fallback
    }
  }

  function saveJson(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value))
    } catch {
      // localStorage 不可用时静默降级，不影响聊天功能
    }
  }

  function persistSessions() {
    saveJson(sessionsStoreKey, sessions.value)
  }

  function persistMessages() {
    // 剥离瞬时的 loading 标记，恢复时统一按“已完成内容”展示
    const plain = messages.value.map((m) => ({ role: m.role, content: m.content, time: m.time, error: m.error }))
    saveJson(messagesKey(activeId.value), plain)
  }

  /** 重置为仅含欢迎语的初始消息 */
  function resetMessages() {
    messages.value = []
    const welcome = buildWelcome()
    if (welcome) {
      messages.value.push({ role: 'ai', content: welcome, time: createTime() })
    }
  }

  /** 从持久化数据恢复消息，过滤掉从未输出过内容的空气泡（如中途刷新页面留下的占位） */
  function loadMessages(id) {
    const loaded = loadJson(messagesKey(id), [])
    messages.value = loaded.filter((m) => m.role === 'user' || (m.content && m.content.trim()))
  }

  /** 新建会话：置于列表最前并成为当前会话 */
  function newSession() {
    const session = { id: createId(), title: '新对话', time: createTime(), titleFixed: false }
    sessions.value.unshift(session)
    activeId.value = session.id
    saveJson(activeStoreKey, session.id)
    resetMessages()
    persistMessages()
    persistSessions()
  }

  /** 切换会话：先持久化当前会话，再加载目标会话 */
  function switchSession(id) {
    if (id === activeId.value) {
      return
    }
    persistMessages()
    activeId.value = id
    saveJson(activeStoreKey, id)
    loadMessages(id)
    if (!messages.value.length) {
      resetMessages()
    }
  }

  /** 会话有新动态：更新时间、用首条用户消息作为标题、移到列表最前 */
  function touchSession(firstUserMessage) {
    const session = sessions.value.find((s) => s.id === activeId.value)
    if (!session) {
      return
    }
    session.time = createTime()
    if (!session.titleFixed && firstUserMessage) {
      session.title = firstUserMessage.length > 14 ? `${firstUserMessage.slice(0, 14)}…` : firstUserMessage
      session.titleFixed = true
    }
    sessions.value = [session, ...sessions.value.filter((s) => s.id !== session.id)]
    persistSessions()
  }

  /** 初始化：恢复上次查看的会话，没有记录则用列表第一条，全新用户则新建 */
  function init() {
    sessions.value = loadJson(sessionsStoreKey, [])
    if (sessions.value.length) {
      const savedActive = loadJson(activeStoreKey, '')
      const exists = sessions.value.some((s) => s.id === savedActive)
      activeId.value = exists ? savedActive : sessions.value[0].id
      loadMessages(activeId.value)
      if (!messages.value.length) {
        resetMessages()
      }
    } else {
      newSession()
    }
  }

  return { sessions, activeId, messages, createTime, newSession, switchSession, touchSession, persistMessages, init }
}
