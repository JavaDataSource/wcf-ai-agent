import { ref } from 'vue'

import {
  createSession as apiCreateSession,
  getSessionMessages,
  listSessions,
  saveSessionMessages,
  updateSessionTitle,
} from '@/api/chatSession'

/**
 * 会话管理组合式函数（持久化到后端本地 MySQL，不再使用 localStorage）：
 * - 会话列表与消息通过 REST 接口存到数据库（表 agent_chat_session / agent_chat_message），
 *   换浏览器 / 换设备 / 清空浏览器数据都不再丢失；
 * - 消息采用“追加式”同步：用 lastSyncedIndex 游标只把新增消息发给后端；
 * - 欢迎语只在前端展示（标记 welcome，不入库）；
 * - 后端不可用时自动降级为纯内存会话（标记 local），聊天功能照常、仅不做持久化。
 */
export function useSessions(appKey, buildWelcome) {
  const sessions = ref([]) // 会话列表：[{ id, title, time, titleFixed, local? }]
  const activeId = ref('') // 当前会话 id（恋爱大师同时用作后端 chatId）
  const messages = ref([]) // 当前会话的消息列表

  // 已同步到后端的消息数量游标（追加式同步）
  let lastSyncedIndex = 0

  /** 生成 HH:mm 格式的当前时间 */
  function createTime() {
    const now = new Date()
    return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  }

  /** 从后端时间字符串（2026-11-11T13:37:25）取 HH:mm */
  function fmtTime(dateTime) {
    const timePart = String(dateTime || '').split('T')[1] || ''
    return timePart.slice(0, 5) || createTime()
  }

  /** 生成会话 id（仅降级模式的本地会话需要；正常模式的 id 由后端生成） */
  function createId() {
    if (window.crypto && window.crypto.randomUUID) {
      return window.crypto.randomUUID()
    }
    return `chat-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  }

  /** 当前会话是否为“降级本地会话”（后端不可用时创建，不做任何持久化） */
  function isActiveLocal() {
    return sessions.value.some((s) => s.id === activeId.value && s.local)
  }

  /** 重置为仅含欢迎语的初始消息（欢迎语只在本地展示，不入库） */
  function resetMessages() {
    messages.value = []
    const welcome = buildWelcome()
    if (welcome) {
      messages.value.push({ role: 'ai', content: welcome, time: createTime(), welcome: true })
    }
  }

  /** 把新增消息追加同步到后端；失败仅告警，不影响聊天 */
  function syncMessages() {
    if (!activeId.value || isActiveLocal()) {
      return
    }
    // 消息数组一般只增不减（停止对话时可能弹掉空占位），游标越界时收敛一下
    if (lastSyncedIndex > messages.value.length) {
      lastSyncedIndex = messages.value.length
    }
    // 从游标处收集“已定稿”（loading = false）的消息：
    // - 欢迎语只在本地展示，永远不入库，直接跳过；
    // - 正在流式输出的气泡内容还会变化，等它定稿后的下一轮同步再入库，
    //   因此遇到未定稿的气泡就停止收集（后续消息保持顺序，不会被越过）。
    const batch = []
    let i = lastSyncedIndex
    while (i < messages.value.length) {
      const m = messages.value[i]
      if (m.welcome) {
        i += 1
        continue
      }
      if (m.loading) {
        break
      }
      if (m.role === 'user' || (m.content && m.content.trim())) {
        batch.push(m)
      }
      i += 1
    }
    lastSyncedIndex = i
    if (!batch.length) {
      return
    }
    saveSessionMessages(
      activeId.value,
      batch.map((m) => ({ role: m.role, content: m.content })),
    ).catch((error) => console.warn('[会话保存失败]', error?.message || error))
  }

  /** 加载某个会话的消息；会话还没聊过时展示欢迎语 */
  async function loadMessages(id) {
    let loaded = []
    try {
      loaded = (await getSessionMessages(id)) || []
    } catch (error) {
      console.warn('[加载消息失败]', error?.message || error)
    }
    messages.value = loaded
      .map((m) => ({ role: m.role, content: m.content, time: fmtTime(m.createdAt) }))
      .filter((m) => m.role === 'user' || (m.content && m.content.trim()))
    lastSyncedIndex = messages.value.length
    if (!messages.value.length) {
      resetMessages()
    }
  }

  /** 后端不可用时的降级：创建纯内存会话，保证界面可用 */
  function fallbackSession() {
    const session = { id: createId(), title: '新对话', time: createTime(), titleFixed: false, local: true }
    sessions.value.unshift(session)
    activeId.value = session.id
    resetMessages()
  }

  /** 新建会话：后端建库记录，本地置顶并成为当前会话 */
  async function newSession() {
    let created
    try {
      created = await apiCreateSession(appKey)
    } catch (error) {
      console.warn('[新建会话失败，已降级为本地会话]', error?.message || error)
      fallbackSession()
      return
    }
    const session = {
      id: created.id,
      title: created.title || '新对话',
      time: fmtTime(created.updatedAt || created.createdAt),
      titleFixed: false,
    }
    sessions.value.unshift(session)
    activeId.value = session.id
    resetMessages()
  }

  /** 切换会话：先把当前会话未同步的消息发出去，再加载目标会话 */
  async function switchSession(id) {
    if (id === activeId.value) {
      return
    }
    syncMessages()
    activeId.value = id
    await loadMessages(id)
  }

  /** 会话有新动态：置顶排序；首个用户消息到来时更新标题（后端同步落库） */
  async function touchSession(firstUserMessage) {
    const session = sessions.value.find((s) => s.id === activeId.value)
    if (!session) {
      return
    }
    session.time = createTime()
    sessions.value = [session, ...sessions.value.filter((s) => s.id !== session.id)]
    if (!session.titleFixed && firstUserMessage) {
      session.title = firstUserMessage.length > 14 ? `${firstUserMessage.slice(0, 14)}…` : firstUserMessage
      session.titleFixed = true
      if (!session.local) {
        updateSessionTitle(session.id, session.title).catch((error) =>
          console.warn('[更新标题失败]', error?.message || error),
        )
      }
    }
  }

  /**
   * 初始化：拉取会话列表并恢复最近活跃的会话；
   * 没有任何会话（或后端不可用降级）则新建一个。
   */
  async function init() {
    try {
      const list = (await listSessions(appKey)) || []
      sessions.value = list.map((s) => ({
        id: s.id,
        title: s.title || '新对话',
        time: fmtTime(s.updatedAt || s.createdAt),
        // 标题已是具体内容视为已固定（“新对话”才会继续被首条消息重命名）
        titleFixed: !!s.title && s.title !== '新对话',
      }))
      if (!sessions.value.length) {
        await newSession()
        return
      }
      activeId.value = sessions.value[0].id
      await loadMessages(activeId.value)
    } catch (error) {
      console.warn('[初始化会话失败，已降级为本地会话]', error?.message || error)
      fallbackSession()
    }
  }

  return { sessions, activeId, messages, createTime, newSession, switchSession, touchSession, syncMessages, init }
}
