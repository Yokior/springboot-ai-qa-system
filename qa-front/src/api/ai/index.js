import request from '@/utils/request'

// 发送聊天消息
export function sendChatMessage(data) {
  return request({
    url: '/api/ai/chat',
    method: 'post',
    data: data
  })
}

// 获取聊天历史
export function getChatHistory(sessionId) {
  return request({
    url: `/api/ai/sessions/${sessionId}/messages`,
    method: 'get'
  })
}

// 创建新会话
export function createSession() {
  return request({
    url: '/api/ai/sessions',
    method: 'post'
  })
}

// 删除会话
export function deleteSession(sessionId) {
  return request({
    url: `/api/ai/sessions/${sessionId}`,
    method: 'delete'
  })
} 