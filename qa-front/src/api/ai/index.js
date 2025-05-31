import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 发送聊天消息
export function sendChatMessage(data) {
  return request({
    url: '/api/ai/chat',
    method: 'post',
    data: data
  })
}

// 发送流式聊天消息
export function sendStreamChatMessage(data, onMessage, onError, onComplete) {
  // 创建URL对象
  const url = `${process.env.VUE_APP_BASE_API}/api/ai/chat/stream`
  
  // 从 auth.js 获取 token
  const token = getToken()
  
  if (!token) {
    if (onError) onError(new Error('未登录或登录已过期，请重新登录'))
    return
  }
  
  // 创建Authorization头，确保格式为 "Bearer {token}"
  const authHeader = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  
  // 创建请求配置
  const fetchOptions = {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': authHeader
    },
    body: JSON.stringify(data)
  }
  
  // 创建SSE连接
  fetch(url, fetchOptions)
    .then(response => {
      if (!response.ok) {
        // 根据状态码提供错误信息
        let errorMsg = `HTTP错误! 状态: ${response.status}`
        if (response.status === 401) {
          errorMsg = '认证失败: Token无效或已过期'
        } else if (response.status === 403) {
          errorMsg = '权限不足: 无法访问此资源'
        }
        throw new Error(errorMsg)
      }
      
      // 获取响应的reader
      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      
      // 读取数据流
      function readStream() {
        reader.read().then(({ done, value }) => {
          if (done) {
            // 处理可能遗留在缓冲区的最后一条消息
            if (buffer.trim()) {
              processBuffer(buffer)
            }
            if (onComplete) onComplete()
            return
          }
          
          // 解码并处理数据
          const text = decoder.decode(value, { stream: true })
          buffer += text
          
          // 处理可能包含多条消息的缓冲区
          processBuffer(buffer)
          buffer = ''
          
          // 继续读取
          readStream()
        }).catch(error => {
          if (onError) onError(error)
        })
      }
      
      // 处理SSE格式的消息
      function processBuffer(buffer) {
        // 按行分割
        const lines = buffer.split('\n')
        
        for (let i = 0; i < lines.length; i++) {
          const line = lines[i].trim()
          
          if (!line) continue // 跳过空行
          
          // 仅处理data行
          if (line.startsWith('data:')) {
            const content = line.substring(5).trim()
            
            if (content === '[DONE]') {
              if (onComplete) onComplete()
            } else {
              if (onMessage) onMessage(content)
            }
          }
        }
      }
      
      // 开始读取流
      readStream()
    })
    .catch(error => {
      if (onError) onError(error)
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

// 获取用户会话列表
export function getUserSessions() {
  return request({
    url: '/api/ai/sessions',
    method: 'get'
  });
} 