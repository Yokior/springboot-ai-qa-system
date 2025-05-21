<template>
  <div class="container">
    <!-- 会话列表侧边栏 -->
    <div class="sessions-sidebar">
      <div class="sidebar-header">
        <h3>会话列表</h3>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="createNewSession" :disabled="loading">新会话</el-button>
      </div>
      <div class="sessions-list">
        <div 
          v-for="session in sessionList" 
          :key="session.id" 
          :class="['session-item', sessionId === session.id ? 'active' : '']"
          @click="switchSession(session.id)"
        >
          <div class="session-title">
            <i class="el-icon-chat-line-square"></i>
            <span>{{ session.title || '新对话' }}</span>
          </div>
          <div class="session-actions">
            <el-tooltip content="删除会话" placement="top">
              <i class="el-icon-delete" @click.stop="confirmDeleteSession(session.id)"></i>
            </el-tooltip>
          </div>
        </div>
        <div v-if="sessionList.length === 0" class="no-sessions">
          <p>暂无会话记录</p>
        </div>
      </div>
    </div>

    <div class="chat-container">
      <!-- 聊天历史记录 -->
      <div class="chat-messages" ref="messagesContainer">
        <div v-for="(message, index) in messages" :key="index" 
             :class="['message', message.type === 'user' ? 'user-message' : 'ai-message']">
          <div class="message-avatar">
            <el-avatar :size="40" :icon="message.type === 'user' ? 'el-icon-user' : 'el-icon-s-platform'" 
                      :src="message.type === 'user' ? userAvatar : aiAvatar"></el-avatar>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="sender-name">{{ message.type === 'user' ? '我' : 'AI 助手' }}</span>
              <span class="message-time">{{ formatTime(message.time) }}</span>
            </div>
            <div class="message-text" v-html="formatMessage(message.content)"></div>
          </div>
        </div>
        <!-- 加载指示器 -->
        <div v-if="loading && !streamingResponse" class="loading-message">
          <div class="message-avatar">
            <el-avatar :size="40" :icon="'el-icon-s-platform'" :src="aiAvatar"></el-avatar>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="sender-name">AI 助手</span>
            </div>
            <div class="loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 提示输入区域 -->
      <div class="prompt-area">
        <el-input
          type="textarea"
          :rows="2"
          placeholder="请输入您的问题..."
          v-model="userInput"
          :disabled="loading || !sessionId"
          @keyup.ctrl.enter.native="sendMessage"
        ></el-input>
        <div class="action-buttons">
          <el-tooltip content="清空对话" placement="top">
            <el-button 
              icon="el-icon-delete" 
              circle 
              :disabled="loading || messages.length === 0"
              @click="clearCurrentChat">
            </el-button>
          </el-tooltip>
          <el-button 
            type="primary" 
            :disabled="loading || !userInput.trim() || !sessionId" 
            @click="sendMessage">
            发送
            <i class="el-icon-s-promotion"></i>
          </el-button>
        </div>
        <div class="tips">按 Ctrl + Enter 快速发送</div>
      </div>
    </div>
  </div>
</template>

<script>
import { parseTime } from '@/utils/yokior'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import * as marked from 'marked'
import { sendChatMessage, sendStreamChatMessage, createSession, deleteSession, getChatHistory } from '@/api/ai/index'
import { getToken } from '@/utils/auth'

export default {
  name: 'AiChat',
  data() {
    return {
      userInput: '',
      messages: [],
      loading: false,
      userAvatar: '',
      aiAvatar: require('@/assets/avatar/ai-avatar.png'),
      sessionId: null, // 当前会话ID
      sessionList: [], // 会话列表
      error: null, // 错误信息
      streamingResponse: false, // 是否正在接收流式响应
      currentStreamingMessage: null // 当前正在接收的流式消息
    }
  },
  created() {
    // 设置用户头像
    this.userAvatar = this.getCache('userAvatar')
    
    // 配置 marked 解析器
    marked.marked.setOptions({
      highlight: function(code, lang) {
        const language = hljs.getLanguage(lang) ? lang : 'plaintext';
        return hljs.highlight(code, { language }).value;
      },
      langPrefix: 'hljs language-',
      breaks: true
    });
    
    // 检查认证并初始化会话
    this.checkAuthAndInitSessions();
  },
  methods: {
    // 检查认证并初始化会话
    checkAuthAndInitSessions() {
      if (!getToken()) {
        this.$message.error('您未登录或登录已过期，请重新登录');
        return;
      }
      
      // 加载会话列表
      this.loadSessionList();
    },
    
    // 加载会话列表
    loadSessionList() {
      this.loading = true;
      
      // 这里应该有一个获取会话列表的API，暂时使用本地存储模拟
      setTimeout(() => {
        try {
          // 重置状态
          this.messages = [];
          this.currentStreamingMessage = null;
          this.streamingResponse = false;
          
          // 尝试从本地存储获取会话列表
          const savedSessions = localStorage.getItem('ai-chat-sessions');
          if (savedSessions) {
            this.sessionList = JSON.parse(savedSessions);
            
            // 获取上次使用的会话ID
            const lastSessionId = localStorage.getItem('ai-chat-last-session');
            if (lastSessionId && this.sessionList.some(s => s.id === lastSessionId)) {
              // 设置当前会话ID，但不立即加载(避免重复加载)
              this.sessionId = lastSessionId;
              this.loadChatHistory();
            } else if (this.sessionList.length > 0) {
              // 如果没有上次使用的会话ID或它不存在，使用列表中的第一个会话
              this.sessionId = this.sessionList[0].id;
              this.loadChatHistory();
            } else {
              // 如果没有任何会话，创建一个新会话
              this.loading = false; // 先重置加载状态再创建
              this.createNewSession();
            }
          } else {
            // 如果本地没有存储会话列表，创建一个新会话
            this.loading = false; // 先重置加载状态再创建
            this.createNewSession();
          }
        } catch (error) {
          console.error('加载会话列表失败:', error);
          this.loading = false;
          this.createNewSession();
        }
      }, 500);
    },
    
    // 切换会话
    switchSession(sessionId) {
      if (this.sessionId === sessionId || this.loading) return;
      
      // 重置状态
      this.messages = [];
      this.currentStreamingMessage = null;
      this.streamingResponse = false;
      
      this.sessionId = sessionId;
      this.loading = true;
      
      // 保存最后使用的会话ID
      localStorage.setItem('ai-chat-last-session', sessionId);
      
      // 加载会话消息
      this.loadChatHistory();
    },
    
    // 创建新会话
    createNewSession() {
      if (this.loading) return;
      
      // 先重置当前状态
      this.messages = [];
      this.currentStreamingMessage = null;
      this.streamingResponse = false;
      
      this.loading = true;
      createSession().then(response => {
        if (response.code === 200) {
          const newSessionId = response.data.sessionId;
          
          // 添加到会话列表
          const newSession = {
            id: newSessionId,
            title: '新对话',
            createTime: new Date().toISOString()
          };
          
          this.sessionList.unshift(newSession);
          this.saveSessionList();
          
          // 设置为当前会话
          this.sessionId = newSessionId;
          
          // 保存最后使用的会话ID
          localStorage.setItem('ai-chat-last-session', newSessionId);
          
          this.$message.success('已创建新会话');
          this.loading = false;
        } else {
          this.$message.error(response.msg || '创建会话失败');
          this.loading = false;
        }
      }).catch(error => {
        console.error('创建会话失败:', error);
        this.$message.error('无法连接到服务器，请稍后再试');
        this.loading = false;
      });
    },
    
    // 确认删除会话
    confirmDeleteSession(sessionId) {
      this.$confirm('确定要删除这个会话吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.deleteSessionById(sessionId);
      }).catch(() => {});
    },
    
    // 删除会话
    deleteSessionById(sessionId) {
      if (this.loading) return;
      
      this.loading = true;
      deleteSession(sessionId).then(() => {
        // 从会话列表中移除
        const index = this.sessionList.findIndex(s => s.id === sessionId);
        if (index !== -1) {
          this.sessionList.splice(index, 1);
          this.saveSessionList();
        }
        
        // 如果删除的是当前会话，切换到另一个会话
        if (this.sessionId === sessionId) {
          if (this.sessionList.length > 0) {
            this.switchSession(this.sessionList[0].id);
          } else {
            this.sessionId = null;
            this.messages = [];
            this.loading = false;
          }
        } else {
          this.loading = false;
        }
        
        this.$message.success('会话已删除');
      }).catch(error => {
        console.error('删除会话失败:', error);
        this.$message.error('删除会话失败，请稍后再试');
        this.loading = false;
      });
    },
    
    // 保存会话列表到本地存储
    saveSessionList() {
      localStorage.setItem('ai-chat-sessions', JSON.stringify(this.sessionList));
    },
    
    // 更新会话标题
    updateSessionTitle(sessionId, firstMessage) {
      // 使用第一条用户消息作为会话标题
      if (!firstMessage || !sessionId) return;
      
      const session = this.sessionList.find(s => s.id === sessionId);
      if (session && session.title === '新对话') {
        // 截取前20个字符作为标题
        let title = firstMessage.trim();
        if (title.length > 20) {
          title = title.substring(0, 20) + '...';
        }
        session.title = title;
        this.saveSessionList();
      }
    },
    
    // 加载聊天历史
    loadChatHistory() {
      if (!this.sessionId) {
        this.loading = false;
        return;
      }
      
      // 重置当前状态
      this.messages = [];
      this.currentStreamingMessage = null;
      this.streamingResponse = false;
      
      getChatHistory(this.sessionId).then(response => {
        if (response.code === 200) {
          const historyMessages = response.data.messages || [];
          
          // 将历史消息转换为前端消息格式
          historyMessages.forEach(msg => {
            this.messages.push({
              type: msg.role === 'user' ? 'user' : 'ai',
              content: msg.content,
              time: new Date(msg.createTime)
            });
          });
          
          // 更新会话标题
          if (historyMessages.length > 0) {
            const firstUserMessage = historyMessages.find(msg => msg.role === 'user');
            if (firstUserMessage) {
              this.updateSessionTitle(this.sessionId, firstUserMessage.content);
            }
          }
          
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        } else {
          this.$message.error(response.msg || '获取聊天历史失败');
        }
        this.loading = false;
      }).catch(error => {
        console.error('获取聊天历史失败:', error);
        this.$message.error('无法连接到服务器，请稍后再试');
        this.loading = false;
      });
    },
    
    // 清空当前对话
    clearCurrentChat() {
      if (!this.sessionId || this.messages.length === 0) return;
      
      this.$confirm('确定要清空当前对话内容吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.messages = [];
        this.$message.success('对话内容已清空');
        
        // 重置会话标题
        const session = this.sessionList.find(s => s.id === this.sessionId);
        if (session) {
          session.title = '新对话';
          this.saveSessionList();
        }
      }).catch(() => {});
    },
    
    // 发送消息
    sendMessage() {
      if (!this.userInput.trim() || this.loading || !this.sessionId) return;
      
      // 添加用户消息
      const userMessage = {
        type: 'user',
        content: this.userInput,
        time: new Date()
      };
      this.messages.push(userMessage);
      
      // 更新会话标题（如果是第一条消息）
      if (this.messages.length === 1 || (this.messages.length === 2 && this.messages[0].type === 'ai')) {
        this.updateSessionTitle(this.sessionId, this.userInput);
      }
      
      const question = this.userInput;
      this.userInput = '';
      
      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom();
      });
      
      // 设置加载状态
      this.loading = true;
      
      // 使用流式API发送请求
      this.sendStreamChatRequest(question);
    },
    
    // 发送流式聊天请求
    sendStreamChatRequest(question) {
      // 检查认证状态
      if (!getToken()) {
        this.$message.error('您未登录或登录已过期，请重新登录');
        this.loading = false;
        return;
      }
      
      // 初始化空的AI回复消息
      const aiMessage = {
        type: 'ai',
        content: '',
        time: new Date()
      };
      this.messages.push(aiMessage);
      this.currentStreamingMessage = aiMessage;
      this.streamingResponse = true;
      
      // 请求数据
      const requestData = {
        prompt: question,
        sessionId: this.sessionId,
        options: {}
      };
      
      // 定义消息处理函数
      const onMessage = (content) => {
        try {
          // 尝试解析JSON，有些服务器返回的是JSON格式
          const jsonContent = JSON.parse(content);
          if (jsonContent.choices && jsonContent.choices[0] && jsonContent.choices[0].delta) {
            // 提取DeepSeek格式的内容
            const chunk = jsonContent.choices[0].delta.content || '';
            if (chunk) {
              this.currentStreamingMessage.content += chunk;
            }
          } else if (jsonContent.content) {
            // 直接使用content字段
            this.currentStreamingMessage.content += jsonContent.content;
          }
        } catch (e) {
          // 不是JSON，直接添加原文本
          this.currentStreamingMessage.content += content;
        }
        
        // 滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      };
      
      // 定义错误处理函数
      const onError = (error) => {
        this.handleError('网络错误，无法连接到服务器: ' + error.message);
        this.streamingResponse = false;
        this.loading = false;
      };
      
      // 定义完成回调函数
      const onComplete = () => {
        this.streamingResponse = false;
        this.loading = false;
        this.currentStreamingMessage = null;
      };
      
      // 发送流式聊天请求
      sendStreamChatMessage(requestData, onMessage, onError, onComplete);
    },
    
    // 处理错误
    handleError(errorMsg) {
      this.error = errorMsg;
      this.$message.error(errorMsg);
      this.loading = false;
      this.streamingResponse = false;
      
      // 如果当前有流式消息但内容为空，则添加错误信息
      if (this.currentStreamingMessage && !this.currentStreamingMessage.content) {
        this.currentStreamingMessage.content = `抱歉，出现了一个错误: ${errorMsg}`;
      } else {
        // 否则添加新的错误消息
        this.messages.push({
          type: 'ai',
          content: `抱歉，出现了一个错误: ${errorMsg}`,
          time: new Date()
        });
      }
    },
    
    // 格式化消息内容，支持Markdown
    formatMessage(content) {
      return marked.marked(content);
    },
    
    // 格式化时间
    formatTime(time) {
      return parseTime(time, '{h}:{i}');
    },
    
    // 滚动到聊天窗口底部
    scrollToBottom() {
      const container = this.$refs.messagesContainer;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    },
    
    // 获取缓存中的用户头像
    getCache(key) {
      const userInfo = this.$store.getters.userInfo;
      return userInfo && userInfo.avatar ? process.env.VUE_APP_BASE_API + userInfo.avatar : '';
    }
  }
}
</script>

<style lang="scss" scoped>
.container {
  display: flex;
  height: calc(100vh - 120px);
  padding: 20px;
  background-color: #f5f7fa;
}

.sessions-sidebar {
  width: 250px;
  margin-right: 20px;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  h3 {
    margin: 0;
    font-size: 16px;
    color: #333;
  }
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}

.session-item {
  padding: 10px 15px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f5f5f5;
  
  &:hover {
    background-color: #f9f9f9;
    
    .session-actions {
      opacity: 1;
    }
  }
  
  &.active {
    background-color: #ecf5ff;
    border-right: 3px solid #409EFF;
  }
}

.session-title {
  display: flex;
  align-items: center;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  
  i {
    margin-right: 8px;
    color: #909399;
  }
  
  span {
    font-size: 14px;
    color: #606266;
  }
}

.session-actions {
  opacity: 0;
  transition: opacity 0.2s;
  
  i {
    font-size: 16px;
    color: #909399;
    margin-left: 8px;
    
    &:hover {
      color: #f56c6c;
    }
  }
}

.no-sessions {
  padding: 20px;
  text-align: center;
  color: #909399;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9f9f9;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message-avatar {
  margin-right: 12px;
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  background-color: #fff;
  border-radius: 8px;
  padding: 10px 15px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.user-message .message-content {
  background-color: #e9f1ff;
}

.ai-message .message-content {
  background-color: #fff;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.sender-name {
  font-weight: bold;
  color: #333;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-text {
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-text :deep(pre) {
  background-color: #f6f8fa;
  border-radius: 5px;
  padding: 10px;
  overflow-x: auto;
  font-family: 'Courier New', Courier, monospace;
}

.message-text :deep(code) {
  background-color: #f6f8fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', Courier, monospace;
}

.prompt-area {
  padding: 15px;
  border-top: 1px solid #eee;
  background-color: #fff;
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.tips {
  color: #999;
  font-size: 12px;
  text-align: right;
  margin-top: 5px;
}

.loading-message {
  display: flex;
  margin-bottom: 20px;
}

.loading-dots {
  display: flex;
  align-items: center;
}

.loading-dots span {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 3px;
  background-color: #999;
  border-radius: 50%;
  animation: dot-pulse 1.5s infinite ease-in-out;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-pulse {
  0% { transform: scale(0); }
  50% { transform: scale(1); }
  100% { transform: scale(0); }
}
</style> 