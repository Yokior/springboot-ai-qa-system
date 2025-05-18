<template>
  <div class="container">
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
        <div v-if="loading" class="loading-message">
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
          :disabled="loading"
          @keyup.ctrl.enter.native="sendMessage"
        ></el-input>
        <div class="action-buttons">
          <el-tooltip content="清空对话" placement="top">
            <el-button 
              icon="el-icon-delete" 
              circle 
              :disabled="loading || messages.length === 0"
              @click="clearConversation">
            </el-button>
          </el-tooltip>
          <el-button 
            type="primary" 
            :disabled="loading || !userInput.trim()" 
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
import marked from 'marked'
import { sendChatMessage, createSession, deleteSession, getChatHistory } from '@/api/ai/index'

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
      error: null // 错误信息
    }
  },
  created() {
    // 设置用户头像
    this.userAvatar = this.getCache('userAvatar')
    
    // 配置 marked 解析器
    marked.setOptions({
      highlight: function(code, lang) {
        const language = hljs.getLanguage(lang) ? lang : 'plaintext';
        return hljs.highlight(code, { language }).value;
      },
      langPrefix: 'hljs language-',
      breaks: true
    });
    
    // 创建新会话
    this.createNewSession();
  },
  methods: {
    // 创建新会话
    createNewSession() {
      createSession().then(response => {
        if (response.code === 200) {
          this.sessionId = response.data.sessionId;
          this.loadChatHistory(); // 加载聊天历史
        } else {
          this.$message.error(response.msg || '创建会话失败');
        }
      }).catch(error => {
        console.error('创建会话失败:', error);
        this.$message.error('无法连接到服务器，请稍后再试');
      });
    },
    
    // 加载聊天历史
    loadChatHistory() {
      if (!this.sessionId) return;
      
      this.loading = true;
      getChatHistory(this.sessionId).then(response => {
        if (response.code === 200) {
          this.messages = response.data.messages || [];
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
    
    // 发送消息
    sendMessage() {
      if (!this.userInput.trim() || this.loading) return;
      if (!this.sessionId) {
        this.$message.error('会话未创建，请刷新页面重试');
        return;
      }
      
      // 添加用户消息
      const userMessage = {
        type: 'user',
        content: this.userInput,
        time: new Date()
      };
      this.messages.push(userMessage);
      
      const question = this.userInput;
      this.userInput = '';
      
      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom();
      });
      
      // 发送请求到后端
      this.loading = true;
      sendChatMessage({
        prompt: question,
        sessionId: this.sessionId,
        options: {
          // 可选参数
          // temperature: 0.7,
          // maxTokens: 2000
        }
      }).then(response => {
        if (response.code === 200) {
          // 添加AI响应消息
          this.messages.push({
            type: 'ai',
            content: response.data.content,
            time: new Date()
          });
          
          // 更新会话ID（如果后端返回了新的）
          if (response.data.sessionId) {
            this.sessionId = response.data.sessionId;
          }
        } else {
          this.handleError(response.msg || '获取AI回复失败');
        }
        this.loading = false;
        
        // 滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }).catch(error => {
        console.error('发送消息失败:', error);
        this.handleError('网络错误，无法连接到服务器');
      });
    },
    
    // 处理错误
    handleError(errorMsg) {
      this.error = errorMsg;
      this.$message.error(errorMsg);
      this.loading = false;
      
      // 添加错误消息到聊天
      this.messages.push({
        type: 'ai',
        content: `抱歉，出现了一个错误: ${errorMsg}`,
        time: new Date()
      });
    },
    
    // 格式化消息内容，支持Markdown
    formatMessage(content) {
      return marked(content);
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
    
    // 清空对话
    clearConversation() {
      this.$confirm('确定要清空所有对话记录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        if (this.sessionId) {
          // 删除当前会话
          deleteSession(this.sessionId).then(() => {
            this.messages = [];
            this.createNewSession(); // 创建新会话
            this.$message({
              type: 'success',
              message: '对话已清空'
            });
          }).catch(error => {
            console.error('删除会话失败:', error);
            this.$message.error('删除会话失败，请稍后再试');
          });
        } else {
          this.messages = [];
          this.createNewSession();
        }
      }).catch(() => {});
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
  justify-content: center;
  height: calc(100vh - 120px);
  padding: 20px;
  background-color: #f5f7fa;
}

.chat-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1000px;
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