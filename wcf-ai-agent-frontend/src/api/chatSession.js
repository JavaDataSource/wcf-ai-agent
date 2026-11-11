import request from '@/utils/request'

/**
 * 会话历史接口（后端 ChatSessionController，数据存本地 MySQL：
 * 表 agent_chat_session / agent_chat_message）。
 *
 * - appKey 用于区分应用（loveApp / manus），同一套接口服务所有聊天应用；
 * - 后端返回的时间形如 2026-11-11T13:37:25（LocalDateTime ISO 格式）。
 */

/** 会话列表（按最近活跃倒序） */
export function listSessions(appKey) {
  return request.get('/chat/session/list', { params: { appKey } })
}

/** 新建会话 */
export function createSession(appKey) {
  return request.post('/chat/session', { appKey })
}

/** 修改会话标题 */
export function updateSessionTitle(id, title) {
  return request.put(`/chat/session/${id}`, { title })
}

/** 删除会话（连同其全部消息） */
export function deleteSession(id) {
  return request.delete(`/chat/session/${id}`)
}

/** 拉取某个会话的消息列表 */
export function getSessionMessages(id) {
  return request.get(`/chat/session/${id}/messages`)
}

/** 追加保存消息（只需 role/content，时间由服务端填充） */
export function saveSessionMessages(id, messages) {
  return request.post(`/chat/session/${id}/messages`, { messages })
}
