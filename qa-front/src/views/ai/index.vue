<template>
  <div class="chat-application">
    <!-- 左侧会话列表 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">对话列表</h2>
        <el-button 
          type="primary" 
          class="new-chat-btn" 
          icon="el-icon-plus" 
          size="small" 
          @click="createNewSession" 
          :disabled="loading">
          新对话
        </el-button>
      </div>
      
      <div class="search-box">
        <i class="el-icon-search search-icon"></i>
        <input 
          type="text" 
          placeholder="搜索对话..." 
          class="search-input"
          v-model="searchQuery">
      </div>
      
      <div class="session-list">
        <transition-group name="session-fade" tag="div">
          <div 
            v-for="session in filteredSessions" 
            :key="session.id" 
            :class="['session-item', sessionId === session.id ? 'active' : '']"
            @click="switchSession(session.id)">
            <div class="session-icon">
              <i class="el-icon-chat-dot-round"></i>
            </div>
            <div class="session-info">
              <div class="session-title">{{ session.title || '新对话' }}</div>
              <div class="session-date">{{ formatDate(session.createTime) }}</div>
            </div>
            <div class="session-actions">
              <div class="action-btn delete" @click.stop="confirmDeleteSession(session.id)">
                <i class="el-icon-delete"></i>
              </div>
            </div>
          </div>
        </transition-group>
        
        <div v-if="filteredSessions.length === 0" class="empty-state">
          <i class="el-icon-chat-line-square empty-icon"></i>
          <p v-if="searchQuery">未找到符合"{{ searchQuery }}"的对话</p>
          <p v-else>暂无对话记录</p>
          <el-button type="primary" size="small" @click="createNewSession" :disabled="loading">
            开始新对话
          </el-button>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-area">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="current-session">
          <h2 v-if="currentSession">{{ currentSession.title || '新对话' }}</h2>
          <h2 v-else>AI 助手</h2>
        </div>
        <div class="header-actions">
          <el-tooltip content="清空对话" placement="bottom">
            <button 
              class="action-button clear-btn" 
              @click="clearCurrentChat" 
              :disabled="loading || messages.length === 0">
              <i class="el-icon-delete"></i>
            </button>
          </el-tooltip>
        </div>
      </div>
      
      <!-- 聊天消息区域 -->
      <div class="messages-container" ref="messagesContainer">
        <div class="welcome-screen" v-if="messages.length === 0 && !loading">
          <div class="welcome-icon">
            <i class="el-icon-s-platform"></i>
          </div>
          <h2>欢迎使用 AI 助手</h2>
          <p>有任何问题都可以向我询问</p>
        </div>
        
        <div class="messages-wrapper">
          <div 
            v-for="(message, index) in messages" 
            :key="index" 
            :class="['message', message.type === 'user' ? 'outgoing' : 'incoming']">
            <div class="avatar">
              <el-avatar 
                :size="36" 
                :src="message.type === 'user' ? userAvatar : aiAvatar"
                :icon="message.type === 'user' ? 'el-icon-user' : 'el-icon-s-platform'">
              </el-avatar>
            </div>
            <div class="message-bubble">
              <div class="message-content" v-html="formatMessage(message.content)"></div>
              <span class="message-time">{{ formatTime(message.time) }}</span>
            </div>
          </div>
        </div>
        
        <!-- 加载指示器 -->
        <div v-if="loading && !streamingResponse" class="typing-indicator">
          <div class="avatar">
            <el-avatar :size="36" :src="aiAvatar" icon="el-icon-s-platform"></el-avatar>
          </div>
          <div class="indicator-bubble">
            <div class="typing-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 底部输入区域 -->
      <div class="composer">
        <div class="input-container">
          <el-input
            type="textarea"
            class="message-input"
            placeholder="输入您的问题... (Ctrl + Enter 发送)"
            v-model="userInput"
            :rows="2"
            :autosize="{ minRows: 1, maxRows: 6 }"
            :disabled="loading || !sessionId"
            @keyup.ctrl.enter.native="sendMessage"
            resize="none"
          ></el-input>
          <button 
            class="send-button" 
            @click="sendMessage"
            :disabled="loading || !userInput.trim() || !sessionId">
            <i class="el-icon-s-promotion"></i>
          </button>
        </div>
        <div class="input-tips">
          <span class="shortcut-tip">Ctrl + Enter 快速发送</span>
        </div>
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
      searchQuery: '', // 搜索关键词
      error: null, // 错误信息
      streamingResponse: false, // 是否正在接收流式响应
      currentStreamingMessage: null // 当前正在接收的流式消息
    }
  },
  computed: {
    // 当前会话对象
    currentSession() {
      return this.sessionList.find(s => s.id === this.sessionId) || null;
    },
    // 过滤后的会话列表
    filteredSessions() {
      if (!this.searchQuery) return this.sessionList;
      
      const query = this.searchQuery.toLowerCase();
      return this.sessionList.filter(session => 
        (session.title || '新对话').toLowerCase().includes(query)
      );
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
    },
    
    // 格式化日期
    formatDate(dateString) {
      if (!dateString) return '';
      
      const date = new Date(dateString);
      const now = new Date();
      const diff = now - date;
      
      // 今天的消息只显示时间
      if (diff < 24 * 60 * 60 * 1000 && 
          date.getDate() === now.getDate() &&
          date.getMonth() === now.getMonth() &&
          date.getFullYear() === now.getFullYear()) {
        return parseTime(date, '{h}:{i}');
      }
      
      // 一周内的消息显示星期几
      if (diff < 7 * 24 * 60 * 60 * 1000) {
        const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        return days[date.getDay()];
      }
      
      // 其他情况显示完整日期
      return parseTime(date, '{y}-{m}-{d}');
    }
  }
}
</script>

<style lang="scss" scoped>
.chat-application {
  display: flex;
  height: calc(100vh - 100px);
  background-color: #f5f7fa;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
}

/* 侧边栏样式 */
.sidebar {
  width: 280px;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #eaeaea;
}

.sidebar-header {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eaeaea;
}

.sidebar-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.new-chat-btn {
  border-radius: 8px;
  font-weight: 500;
}

.search-box {
  padding: 12px 16px;
  position: relative;
}

.search-icon {
  position: absolute;
  left: 24px;
  top: 22px;
  color: #909399;
}

.search-input {
  width: 100%;
  padding: 10px 10px 10px 30px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  font-size: 14px;
  background-color: #f5f7fa;
  transition: all 0.3s;
  
  &:focus {
    outline: none;
    border-color: #409EFF;
    background-color: #fff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  }
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 5px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  
  &:hover {
    background-color: #f5f7fa;
    
    .session-actions {
      opacity: 1;
    }
  }
  
  &.active {
    background-color: #ecf5ff;
    
    .session-icon {
      background-color: #409EFF;
      color: white;
    }
    
    .session-title {
      color: #409EFF;
      font-weight: 500;
    }
  }
}

.session-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background-color: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  color: #909399;
  flex-shrink: 0;
  transition: all 0.3s;
  
  i {
    font-size: 18px;
  }
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-date {
  font-size: 12px;
  color: #909399;
}

.session-actions {
  display: flex;
  opacity: 0;
  transition: opacity 0.2s;
}

.action-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
  cursor: pointer;
  
  &:hover {
    background-color: rgba(0, 0, 0, 0.05);
  }
  
  &.delete:hover {
    background-color: rgba(245, 108, 108, 0.1);
    color: #f56c6c;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #909399;
  text-align: center;
  
  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    color: #dcdfe6;
  }
  
  p {
    margin-bottom: 16px;
  }
}

/* 聊天区域样式 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.chat-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eaeaea;
  
  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}

.header-actions {
  display: flex;
}

.action-button {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background-color: transparent;
  border: none;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: 8px;
  
  &:hover {
    background-color: #f5f7fa;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  &.clear-btn:hover {
    color: #f56c6c;
  }
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f9fafc;
}

.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #606266;
  text-align: center;
  
  .welcome-icon {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background-color: #ecf5ff;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;
    
    i {
      font-size: 32px;
      color: #409EFF;
    }
  }
  
  h2 {
    font-size: 20px;
    margin-bottom: 8px;
    color: #303133;
  }
  
  p {
    color: #909399;
  }
}

.messages-wrapper {
  padding-bottom: 10px;
}

.message {
  display: flex;
  margin-bottom: 16px;
  
  &.outgoing {
    flex-direction: row-reverse;
    
    .message-bubble {
      background-color: #ecf5ff;
      border-radius: 16px 4px 16px 16px;
      margin-left: 12px;
      margin-right: 0;
      
      .message-time {
        text-align: left;
      }
    }
  }
  
  &.incoming .message-bubble {
    background-color: #fff;
    border-radius: 4px 16px 16px 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  }
}

.avatar {
  margin: 0 8px;
}

.message-bubble {
  max-width: 80%;
  padding: 6px 10px;
  margin-right: 12px;
  position: relative;
}

.message-content {
  color: #303133;
  line-height: 1.4;
  font-size: 14px;
  word-break: break-word;
  
  :deep(pre) {
    background-color: #f6f8fa;
    border-radius: 6px;
    padding: 10px;
    margin: 6px 0;
    overflow-x: auto;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
    font-size: 13px;
  }
  
  :deep(code) {
    background-color: #f6f8fa;
    padding: 1px 4px;
    border-radius: 4px;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
    font-size: 13px;
  }
  
  :deep(p) {
    margin: 4px 0;
  }
  
  :deep(a) {
    color: #409EFF;
    text-decoration: none;
    
    &:hover {
      text-decoration: underline;
    }
  }
  
  :deep(ul), :deep(ol) {
    padding-left: 20px;
    margin: 8px 0;
  }
}

.message-time {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
  text-align: right;
}

.typing-indicator {
  display: flex;
  margin-bottom: 16px;
  
  .indicator-bubble {
    background-color: #fff;
    padding: 14px 18px;
    border-radius: 18px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    margin-right: 12px;
  }
}

.typing-dots {
  display: flex;
  align-items: center;
  
  span {
    display: inline-block;
    width: 8px;
    height: 8px;
    margin-right: 4px;
    background-color: #dcdfe6;
    border-radius: 50%;
    animation: typing-dot 1.4s infinite ease-in-out;
    
    &:nth-child(1) {
      animation-delay: 0s;
    }
    
    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    
    &:nth-child(3) {
      animation-delay: 0.4s;
      margin-right: 0;
    }
  }
}

@keyframes typing-dot {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

.composer {
  padding: 16px 20px;
  border-top: 1px solid #eaeaea;
  background-color: #fff;
}

.input-container {
  display: flex;
  align-items: center;
  position: relative;
}

.message-input {
  width: 100%;
  border-radius: 12px;
  resize: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #dcdfe6;
  transition: all 0.3s;
  
  :deep(.el-textarea__inner) {
    padding: 12px 45px 12px 16px;
    resize: none;
    font-size: 14px;
    line-height: 1.5;
    border-radius: 12px;
    
    &:focus {
      border-color: #409EFF;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }
}

.send-button {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #409EFF;
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
  z-index: 10;
  
  &:hover {
    background-color: #66b1ff;
  }
  
  &:disabled {
    background-color: #a0cfff;
    cursor: not-allowed;
  }
}

.input-tips {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.session-fade-enter-active, .session-fade-leave-active {
  transition: all 0.3s;
}

.session-fade-enter, .session-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style> 