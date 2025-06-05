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
        <div class="session-details">
          <h2 class="session-title-text">{{ currentSession ? currentSession.title || '新对话' : 'AI 助手' }}</h2>
          <div class="knowledge-base-selector">
            <span class="kb-label"><i class="el-icon-notebook-2"></i> 知识库:</span>
            <el-select v-model="selectedTeamId" @change="handleTeamChange" placeholder="选择知识库" size="small" class="team-select-dropdown">
              <el-option label="不使用知识库" value=""></el-option>
              <el-option 
                v-for="team in teamList" 
                :key="team.teamId" 
                :label="team.name" 
                :value="String(team.teamId)">
                <div class="team-option">
                  <el-avatar 
                    :size="30" 
                    :src="getAvatarUrl(team.avatar)" 
                    class="team-option-avatar">
                    {{ team.name ? team.name.substring(0, 1) : 'T' }}
                  </el-avatar>
                  <span class="team-option-name">{{ team.name }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
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
import { marked } from 'marked';
import hljs from 'highlight.js/lib/common';  // 改用common包，包含常用语言
import 'highlight.js/styles/vs2015.css';  // 导入VS Code风格的代码高亮样式
import { mapState } from 'vuex';
import { sendChatMessage, sendStreamChatMessage, createSession, deleteSession, getChatHistory, getUserSessions } from '@/api/ai/index';
import { getToken } from '@/utils/auth';
import { parseTime } from '@/utils/yokior';
import { getInfo } from '@/api/login';  // 恢复getInfo导入
import { listMy_team } from '@/api/team/my_team'; // Corrected import

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
      currentStreamingMessage: null, // 当前正在接收的流式消息
      selectedTeamId: null, // + 新增：当前选择的团队ID
      teamList: [] // + 新增：团队列表
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
    },
    // + 新增：当前选中的团队对象
    currentSelectedTeam() {
      return this.teamList.find(team => team.teamId === this.selectedTeamId) || null;
    }
  },
  // 正确定义watch属性，将它放在组件的顶层而不是methods中
  watch: {
    messages: {
      deep: true,
      handler() {
        this.$nextTick(() => {
          this.addCopyButtonsToCodeBlocks();
        });
      }
    },
    'currentStreamingMessage.content': function() {
      if (this.streamingResponse) {
        this.$nextTick(() => {
          this.addCopyButtonsToCodeBlocks();
        });
      }
    }
    // Removed watcher for selectedTeamId
  },
  // 将生命周期钩子放在组件顶层
  mounted() {
    // 配置marked和highlight.js
    marked.setOptions({
      highlight: (code, lang) => {
        if (lang && hljs.getLanguage(lang)) {
          try {
            return hljs.highlight(code, { language: lang }).value;
          } catch (e) {
            console.error('高亮错误:', e);
          }
        }
        
        try {
          return hljs.highlightAuto(code).value;
        } catch (e) {
          console.error('自动高亮错误:', e);
        }
        
        return code; // 如果无法高亮，返回原始代码
      }
    });
    
    // 为整个文档添加事件委托来处理按钮点击
    document.addEventListener('click', (e) => {
      // console.log('点击元素:', e.target); // Debugging line, consider removing for production
      
      // 查找最近的按钮
      let targetButton = null;
      let actionType = null;

      // 检查是否点击了复制按钮 (包括标题栏和右上角)
      if (e.target.matches('.vs-code-copy-btn, .vs-code-corner-copy-btn') || 
          e.target.closest('.vs-code-copy-btn, .vs-code-corner-copy-btn')) {
        targetButton = e.target.matches('.vs-code-copy-btn, .vs-code-corner-copy-btn') 
          ? e.target 
          : e.target.closest('.vs-code-copy-btn, .vs-code-corner-copy-btn');
        actionType = 'copy';
      } 
      // 检查是否点击了下载按钮
      else if (e.target.matches('.vs-code-download-btn') || e.target.closest('.vs-code-download-btn')) {
        targetButton = e.target.matches('.vs-code-download-btn')
          ? e.target
          : e.target.closest('.vs-code-download-btn');
        actionType = 'download';
      }

      if (targetButton && actionType) {
        // console.log(`${actionType}按钮被点击:`, targetButton); // Debugging line
        
        e.preventDefault();
        e.stopPropagation();
        
        // 找到最近的代码容器
        const container = targetButton.closest('.vs-code-container');
        if (!container) {
          // console.warn('未找到.vs-code-container'); // Debugging line
          return;
        }

        // 从data-original-code属性获取原始代码
        const originalCode = container.dataset.originalCode;
        const language = container.dataset.language || '';
        
        if (!originalCode) {
          // console.warn('未在.vs-code-container上找到data-original-code'); // Debugging line
          this.$message.error('无法获取代码内容');
          return;
        }

        if (actionType === 'copy') {
          this.copyCodeToClipboard(originalCode, container); // Pass container for animation
        } else if (actionType === 'download') {
          this.downloadCode(originalCode, language);
        }
      }
    });


    // 获取用户信息，包括头像
    getInfo().then(res => {
      if (res.user && res.user.avatar) {
        this.userAvatar = process.env.VUE_APP_BASE_API + res.user.avatar;
      } else {
        // 如果没有头像，可以使用默认图标或一个占位符图片
        this.userAvatar = ''; // 或者 this.userAvatar = require('@/assets/avatar/default-user.png');
      }
    }).catch(error => {
      console.error("获取用户信息失败:", error);
      this.userAvatar = ''; // 出错时也设置默认
    });

    this.checkAuthAndInitSessions();
    this.fetchTeamList(); // + 新增：在组件挂载时获取团队列表
  },
  updated() {
    // 组件更新后处理代码块
    this.addCopyButtonsToCodeBlocks();
  },
  created() {
    // 获取用户头像
    // this.fetchUserAvatar(); // 移到 mounted
    
    // 配置marked
    // marked.setOptions({...}); // 移到 mounted
    
    // 全局复制函数
    window.copyCodeBlock = (text) => {
      if (!text) {
        this.$message.error('没有可复制的内容');
        return;
      }
      
      try {
        // 创建临时文本区域
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        
        textarea.focus();
        textarea.select();
        
        const successful = document.execCommand('copy');
        if (successful) {
          this.$message.success('代码已复制到剪贴板');
        } else {
          this.$message.error('复制失败，请手动复制');
        }
        
        document.body.removeChild(textarea);
      } catch(err) {
        console.error('复制失败:', err);
        this.$message.error('复制失败，请手动复制');
      }
    };
    
    // 全局下载函数
    window.downloadCodeBlock = (text, language) => {
      if (!text) {
        this.$message.error('没有可下载的内容');
        return;
      }
      
      try {
        // 确定文件扩展名
        const extensions = {
          java: '.java',
          javascript: '.js',
          js: '.js',
          typescript: '.ts',
          ts: '.ts',
          html: '.html',
          css: '.css',
          python: '.py',
          // 其他扩展名...
        };
        
        // 默认扩展名
        const ext = extensions[language ? language.toLowerCase() : ''] || '.txt';
        
        // 创建Blob对象
        const blob = new Blob([text], { type: 'text/plain' });
        
        // 创建下载链接
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `code-snippet${ext}`;
        
        // 触发点击
        document.body.appendChild(a);
        a.click();
        
        // 清理
        setTimeout(() => {
          document.body.removeChild(a);
          this.$message.success(`代码已下载为 code-snippet${ext}`);
        }, 100);
      } catch(err) {
        console.error('下载失败:', err);
        this.$message.error('下载失败: ' + err.message);
      }
    };
    
    // 检查认证并初始化会话
    // this.checkAuthAndInitSessions(); // 删除此处调用，因为在mounted中已经调用了
    
    // this.fetchTeamList(); // + 新增：获取团队列表 - 移到 mounted
  },
  methods: {
    // 获取用户头像
    fetchUserAvatar() {
      if (getToken()) {
        getInfo().then(res => {
          if (res.code === 200 && res.user && res.user.avatar) {
            // 设置用户头像
            this.userAvatar = process.env.VUE_APP_BASE_API + res.user.avatar;
          }
        }).catch(err => {
          console.error('获取用户信息失败:', err);
        });
      }
    },
    
    // 检查认证并初始化会话
    checkAuthAndInitSessions() {
      if (!getToken()) {
        this.$message.error('您未登录或登录已过期，请重新登录');
        // 可以选择跳转到登录页
        // this.$router.push('/login'); 
        return;
      }
      
      // 加载会话列表
      this.loadSessionList();
    },
    
    // 加载会话列表
    loadSessionList() {
      this.loading = true;
      this.messages = []; // 清空消息
      this.currentStreamingMessage = null;
      this.streamingResponse = false;

      getUserSessions().then(response => {
        if (response.code === 200 && response.data) {
          const sessionIds = response.data;
          // 将会话ID列表转换为前端的会话对象列表
          // createTime 初始设为当前时间，后续由 loadChatHistory 更新为更准确的时间
          this.sessionList = sessionIds.map(id => ({
            id: id,
            title: '新对话', 
            createTime: new Date().toISOString() 
          })).sort((a, b) => new Date(b.createTime) - new Date(a.createTime)); // 可选：按临时时间初步排序

          const lastSessionId = localStorage.getItem('ai-chat-last-session');
          
          if (lastSessionId && this.sessionList.some(s => s.id === lastSessionId)) {
            this.sessionId = lastSessionId;
            this.loadChatHistory(); // loadChatHistory 会处理 loading 状态
          } else if (this.sessionList.length > 0) {
            // 如果没有上次会话ID或无效，则加载列表中的第一个
            this.sessionId = this.sessionList[0].id;
            localStorage.setItem('ai-chat-last-session', this.sessionId); // 更新最后会话ID
            this.loadChatHistory(); // loadChatHistory 会处理 loading 状态
          } else {
            // 如果没有会话列表，则创建一个新会话
            this.loading = false; // 先重置加载状态
            this.createNewSession();
          }
        } else {
          this.$message.error(response.msg || '获取会话列表失败');
          this.loading = false;
          this.sessionList = []; // 清空列表
          // 获取列表失败时也尝试创建一个新会话
          this.createNewSession();
        }
      }).catch(error => {
        console.error('加载会话列表失败:', error);
        this.$message.error('无法连接到服务器获取会话列表，请尝试刷新或稍后再试');
        this.loading = false;
        this.sessionList = []; // 清空列表
        // 网络错误时也尝试创建一个新会话
        this.createNewSession();
      });
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
          // this.saveSessionList(); // 移除对localStorage的保存
          
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
          // this.saveSessionList(); // 移除对localStorage的保存
        }
        
        // 如果删除的是当前会话，切换到另一个会话
        if (this.sessionId === sessionId) {
          localStorage.removeItem('ai-chat-last-session'); // 清除被删除的会话ID
          if (this.sessionList.length > 0) {
            // 切换到列表中的第一个会话
            this.switchSession(this.sessionList[0].id); 
          } else {
            // 如果没有其他会话了，则创建一个新的
            this.sessionId = null;
            this.messages = [];
            this.createNewSession(); // 创建新会话会设置loading=false
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
    
    // 保存会话列表到本地存储 (此方法不再需要，将被移除)
    // saveSessionList() {
    //   localStorage.setItem('ai-chat-sessions', JSON.stringify(this.sessionList));
    // },
    
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
        // this.saveSessionList(); // 移除对localStorage的保存
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
          
          // 更新会话标题和可能的创建时间
          const currentSessionInList = this.sessionList.find(s => s.id === this.sessionId);
          if (currentSessionInList && historyMessages.length > 0) {
            // 使用第一条消息的创建时间更新会话的 createTime，使其更准确
            if (historyMessages[0].createTime) {
              // 只有当初始的createTime是占位符时才更新，或者总是更新
              // 这里我们假设如果后端有createTime就用它
              currentSessionInList.createTime = new Date(historyMessages[0].createTime).toISOString();
            }

            const firstUserMessage = historyMessages.find(msg => msg.role === 'user');
            if (firstUserMessage) {
              this.updateSessionTitle(this.sessionId, firstUserMessage.content);
            } else if (currentSessionInList.title === '新对话' && historyMessages[0] && historyMessages[0].content) {
              // 如果没有用户消息，但有AI消息，且标题是默认的，可以尝试用AI消息更新
              let aiTitle = historyMessages[0].content.substring(0, 20);
              if (historyMessages[0].content.length > 20) aiTitle += "...";
              this.updateSessionTitle(this.sessionId, `AI: ${aiTitle}`);
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
        // 清空API目前没有，这里只清空前端messages和重置标题
        this.messages = []; 
        this.$message.success('对话内容已清空');
        
        // 重置会话标题
        const session = this.sessionList.find(s => s.id === this.sessionId);
        if (session) {
          session.title = '新对话';
          // this.saveSessionList(); // 移除对localStorage的保存
        }
      }).catch(() => {});
    },
    
    // 发送消息
    sendMessage() {
      if (!this.userInput.trim() || this.loading || !this.sessionId) return;
      // 不再检查必须选择团队，因为现在允许不选择团队
      
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
      
      // 确保在请求开始时，currentStreamingMessage 为 null 并且没有预先添加AI消息
      // this.currentStreamingMessage = null; // 应该在 onComplete 或 onError 中被设为null，或者在切换会话时

      const requestData = {
        prompt: question,
        sessionId: this.sessionId,
        options: {}
      };
      
      // 只有当选择了团队时才添加teamId到options
      if (this.selectedTeamId) {
        requestData.options.teamId = this.selectedTeamId;
      }
      
      // 定义消息处理函数
      const onMessage = (content) => {
        if (!this.streamingResponse) { 
          this.streamingResponse = true; // 首先改变状态，这将隐藏打字动画
          
          // 仅在此时创建并添加新的AI消息对象到messages数组
          const aiMessage = {
            type: 'ai',
            content: '',
            time: new Date() // 时间戳为第一条回复到达的时间
          };
          this.messages.push(aiMessage);
          this.currentStreamingMessage = aiMessage; // 设置当前正在流式处理的消息
        }
        
        // 确保 currentStreamingMessage 存在才尝试添加内容
        if (this.currentStreamingMessage) {
          try {
            const jsonContent = JSON.parse(content);
            if (jsonContent.choices && jsonContent.choices[0] && jsonContent.choices[0].delta) {
              const chunk = jsonContent.choices[0].delta.content || '';
              if (chunk) {
                this.currentStreamingMessage.content += chunk;
              }
            } else if (jsonContent.content !== undefined) {
              this.currentStreamingMessage.content += jsonContent.content;
            } else if (typeof content === 'string' && content.trim()) {
              this.currentStreamingMessage.content += content;
            }
          } catch (e) {
            if (typeof content === 'string' && content.trim()) {
              this.currentStreamingMessage.content += content;
            }
          }
        } else {
          // 如果 currentStreamingMessage 因为某些原因仍为null (理论上不应发生在此流程中)
          // 可以考虑记录一个警告或错误
          console.warn('currentStreamingMessage is null when trying to append content.');
        }
        
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
      if (!content) return '';
      
      try {
        // 预处理代码块
        let processedContent = this.preprocessCodeBlocks ? this.preprocessCodeBlocks(content) : content;
        
        // 使用marked处理markdown
        const html = marked(processedContent);
        
        // 创建一个临时元素来解析HTML
        const tempElement = document.createElement('div');
        tempElement.innerHTML = html;
        
        // 处理pre元素，应用VS Code风格
        const preElements = tempElement.querySelectorAll('pre');
        preElements.forEach(pre => {
          const codeElement = pre.querySelector('code');
          if (codeElement) {
            this.buildVSCodeStyleBlock(pre, codeElement);
          }
        });
        
        // 返回处理后的HTML
        return tempElement.innerHTML;
      } catch (error) {
        console.error('格式化消息出错:', error);
        return content;
      }
    },
    
    // 复制文本到剪贴板
    copyToClipboard(text) {
      if (!text) {
        this.$message.error('没有可复制的内容');
        return;
      }
      
      // 创建临时文本区域
      const textarea = document.createElement('textarea');
      textarea.value = text.trim();
      textarea.style.position = 'fixed';
      textarea.style.left = '-9999px';
      textarea.style.top = '-9999px';
      document.body.appendChild(textarea);
      
      try {
        // 选择文本并复制
        textarea.select();
        textarea.setSelectionRange(0, 99999);
        const successful = document.execCommand('copy');
        
        if (successful) {
          // 显示成功消息
          this.$message({
            message: '代码已复制到剪贴板',
            type: 'success',
            duration: 1500,
            customClass: 'code-copy-message'
          });
          
          // 显示复制成功动画
          this.showCopyAnimation();
        } else {
          this.$message.error('复制失败，请手动复制');
        }
      } catch (err) {
        console.error('复制失败:', err);
        this.$message.error('复制失败: ' + err);
      } finally {
        // 移除临时文本区域
        document.body.removeChild(textarea);
      }
    },
    
    // 显示复制成功动画
    showCopyAnimation() {
      // 创建动画元素
      const animation = document.createElement('div');
      animation.className = 'copy-success-animation';
      animation.innerHTML = '<i class="el-icon-check"></i>';
      document.body.appendChild(animation);
      
      // 使用setTimeout允许DOM渲染
      setTimeout(() => {
        animation.classList.add('show');
      }, 10);
      
      // 动画结束后移除元素
      setTimeout(() => {
        animation.classList.remove('show');
        setTimeout(() => {
          if (document.body.contains(animation)) {
            document.body.removeChild(animation);
          }
        }, 300);
      }, 1500);
    },
    
    // 预处理代码块
    preprocessCodeBlocks(content) {
      if (!content) return '';

      // 已经包含Markdown代码块的不需要处理
      if (content.includes('```')) return content;
      
      // 检查是否是Java样例代码（常见的错误情况）
      if (content.includes('// HelloWorld.java') && content.includes('public class HelloWorld')) {
        return "```java\n" + content + "\n```";
      }
      
      let processedContent = content;
      
      // 常见编程语言的特征模式
      const codePatterns = {
        java: [
          /public\s+class\s+\w+/,
          /public\s+static\s+void\s+main/,
          /import\s+java\./,
          /public\s+static\s+void\s+\w+\s*\(/,
          /private\s+\w+\s+\w+\s*\(/
        ],
        javascript: [
          /function\s+\w+\s*\(/,
          /const\s+\w+\s*=/,
          /let\s+\w+\s*=/,
          /var\s+\w+\s*=/,
          /export\s+default/,
          /import\s+.*\s+from/,
          /^\s*\w+\.\w+\s*\(/m
        ],
        python: [
          /def\s+\w+\s*\(/,
          /import\s+\w+/,
          /from\s+\w+\s+import/,
          /class\s+\w+\s*\(/,
          /class\s+\w+:/
        ],
        html: [
          /<html/i,
          /<body/i,
          /<div/i,
          /<script/i,
          /<\/\w+>/i
        ],
        css: [
          /\w+\s*{\s*[\w\-]+\s*:/,
          /\.\w+\s*{/,
          /#\w+\s*{/,
          /@media\s+/,
          /@import\s+/
        ],
        sql: [
          /SELECT\s+.*\s+FROM/i,
          /INSERT\s+INTO/i,
          /UPDATE\s+.*\s+SET/i,
          /DELETE\s+FROM/i,
          /CREATE\s+TABLE/i
        ]
      };
      
      // 检测代码语言
      let detectedLanguage = null;
      let highestMatches = 0;
      
      Object.entries(codePatterns).forEach(([lang, patterns]) => {
        const matches = patterns.filter(pattern => pattern.test(content)).length;
        if (matches > highestMatches) {
          highestMatches = matches;
          detectedLanguage = lang;
        }
      });
      
      // 如果匹配度不够高，可能不是代码
      if (highestMatches < 2) {
        return content;
      }
      
      // 处理代码块
      if (detectedLanguage) {
        try {
          // 通用格式化（添加换行和缩进）
          processedContent = this.formatCodeByLanguage(processedContent, detectedLanguage);
          
          // 检测是否有多个代码块（用"---"分隔）
          if (content.includes('---')) {
            return this.processSplitCodeSections(content, codePatterns);
          } else {
            // 将格式化后的代码包装在Markdown代码块中
            processedContent = "```" + detectedLanguage + "\n" + processedContent + "\n```";
          }
        } catch (e) {
          console.error('代码格式化失败:', e);
        }
      }

      return processedContent;
    },
    
    // 根据编程语言格式化代码
    formatCodeByLanguage(code, language) {
      switch(language) {
        case 'java':
          return this.formatJavaCode(code);
        case 'javascript':
          return this.formatJavascriptCode(code);
        case 'python':
          return this.formatPythonCode(code);
        case 'html':
          return this.formatHtmlCode(code);
        case 'css':
          return this.formatCssCode(code);
        case 'sql':
          return this.formatSqlCode(code);
        default:
          return code;
      }
    },
    
    // 格式化Java代码
    formatJavaCode(code) {
      return code
        .replace(/public/g, "\npublic ")
        .replace(/private/g, "\nprivate ")
        .replace(/protected/g, "\nprotected ")
        .replace(/class\s+/g, "class ")
        .replace(/for\s*\(/g, "for (")
        .replace(/if\s*\(/g, "if (")
        .replace(/else\s*{/g, "else {")
        .replace(/\)\s*{/g, ") {")
        .replace(/;\s*/g, ";\n")
        .replace(/}\s*/g, "}\n")
        .replace(/{\s*/g, "{\n  ")
        .replace(/\s{2,}/g, "  ")
        .replace(/import\s+/g, "import ")
        .replace(/package\s+/g, "package ");
    },
    
    // 格式化JavaScript代码
    formatJavascriptCode(code) {
      return code
        .replace(/function\s+/g, "\nfunction ")
        .replace(/const\s+/g, "\nconst ")
        .replace(/let\s+/g, "\nlet ")
        .replace(/var\s+/g, "\nvar ")
        .replace(/for\s*\(/g, "for (")
        .replace(/if\s*\(/g, "if (")
        .replace(/else\s*{/g, "else {")
        .replace(/\)\s*{/g, ") {")
        .replace(/;\s*/g, ";\n")
        .replace(/}\s*/g, "}\n")
        .replace(/{\s*/g, "{\n  ")
        .replace(/\s{2,}/g, "  ")
        .replace(/import\s+/g, "import ")
        .replace(/export\s+/g, "export ");
    },
    
    // 格式化Python代码
    formatPythonCode(code) {
      return code
        .replace(/def\s+/g, "\ndef ")
        .replace(/class\s+/g, "\nclass ")
        .replace(/import\s+/g, "\nimport ")
        .replace(/from\s+/g, "\nfrom ")
        .replace(/:\s*/g, ":\n  ")
        .replace(/if\s+/g, "\nif ")
        .replace(/elif\s+/g, "\nelif ")
        .replace(/else\s*:/g, "\nelse:")
        .replace(/for\s+/g, "\nfor ")
        .replace(/while\s+/g, "\nwhile ")
        .replace(/try\s*:/g, "\ntry:")
        .replace(/except\s+/g, "\nexcept ")
        .replace(/finally\s*:/g, "\nfinally:")
        .replace(/\n\s*\n/g, "\n\n");
    },
    
    // 格式化HTML代码
    formatHtmlCode(code) {
      return code
        .replace(/</g, "\n<")
        .replace(/>/g, ">\n")
        .replace(/\n\s*\n/g, "\n");
    },
    
    // 格式化CSS代码
    formatCssCode(code) {
      return code
        .replace(/{/g, " {\n  ")
        .replace(/}/g, "\n}\n")
        .replace(/;/g, ";\n  ")
        .replace(/\n\s*\n/g, "\n");
    },
    
    // 格式化SQL代码
    formatSqlCode(code) {
      return code
        .replace(/SELECT/ig, "\nSELECT")
        .replace(/FROM/ig, "\nFROM")
        .replace(/WHERE/ig, "\nWHERE")
        .replace(/ORDER BY/ig, "\nORDER BY")
        .replace(/GROUP BY/ig, "\nGROUP BY")
        .replace(/HAVING/ig, "\nHAVING")
        .replace(/JOIN/ig, "\nJOIN")
        .replace(/INSERT/ig, "\nINSERT")
        .replace(/UPDATE/ig, "\nUPDATE")
        .replace(/DELETE/ig, "\nDELETE")
        .replace(/CREATE/ig, "\nCREATE")
        .replace(/DROP/ig, "\nDROP")
        .replace(/ALTER/ig, "\nALTER")
        .replace(/;/g, ";\n");
    },
    
    // 处理分段代码块
    processSplitCodeSections(content, codePatterns) {
      const sections = content.split('---');
      
      // 处理每个部分
      return sections.map(section => {
        // 检测代码语言
        let sectionLanguage = null;
        let highestMatches = 0;
        
        Object.entries(codePatterns).forEach(([lang, patterns]) => {
          const matches = patterns.filter(pattern => pattern.test(section)).length;
          if (matches > highestMatches) {
            highestMatches = matches;
            sectionLanguage = lang;
          }
        });
        
        if (highestMatches < 2) {
          return section; // 可能不是代码
        }
        
        // 查找可能的标题
        const titleMatch = section.match(/#{1,6}\s*(.+?)(?:\n|$)/);
        const title = titleMatch ? titleMatch[0] : '';
        
        // 分离标题和代码
        let code = titleMatch ? section.replace(titleMatch[0], '') : section;
        
        // 格式化代码
        if (sectionLanguage) {
          try {
            code = this.formatCodeByLanguage(code, sectionLanguage);
            return title + "\n```" + sectionLanguage + "\n" + code.trim() + "\n```";
          } catch (e) {
            console.error('部分代码格式化失败:', e);
            return section;
          }
        } else {
          return section;
        }
      }).join("\n\n");
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
    
    // 获取缓存中的用户头像 (保留此方法作为备用)
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
    },
    
    // 为代码块添加工具栏、复制按钮和行号 - 全新实现
    addCopyButtonsToCodeBlocks() {
      // 注入样式表
      this.injectCodeBlockStyles();
      
      // 确保DOM已经渲染完成
      setTimeout(() => {
        // 选择所有的pre元素
        const preElements = document.querySelectorAll('.message-content pre');
        
        // 为每个pre元素添加工具栏和行号
        preElements.forEach((pre) => {
          // 如果已经处理过，跳过
          if (pre.classList.contains('vs-code-processed')) return;
          
          // 标记为已处理
          pre.classList.add('vs-code-processed');
          
          // 获取代码元素
          const codeElement = pre.querySelector('code');
          if (!codeElement) return;
          
          // 重新构建整个代码块结构
          this.buildVSCodeStyleBlock(pre, codeElement);
        });
      }, 100);
    },
    
    // 构建VS Code风格的代码块
    buildVSCodeStyleBlock(pre, codeElement) {
      // 确保首先注入样式
      this.injectCodeBlockStyles();
      
      // 获取原始代码和语言
      const code = codeElement.textContent;
      let language = '';
      
      // 从class中提取语言
      if (codeElement.className) {
        const match = codeElement.className.match(/language-(\w+)/);
        if (match) {
          language = match[1];
        }
      }

      // 获取高亮后的HTML
      const highlightedHtml = codeElement.innerHTML;
      
      // 统计代码行数
      const lineCount = code.split('\n').length;
      
      // 创建主容器
      const container = document.createElement('div');
      container.className = 'vs-code-container';
      if (language) {
        container.classList.add(`language-${language}`);
        container.setAttribute('data-language', language);
      }
      
      // 设置原始代码属性用于复制
      container.setAttribute('data-original-code', code);
      
      // 创建头部（包含语言标签和工具栏）
      const header = document.createElement('div');
      header.className = 'vs-code-header';
      
      // 语言标签
      const langLabel = document.createElement('div');
      langLabel.className = 'vs-code-lang';
      langLabel.textContent = language ? language.toUpperCase() : 'TEXT';
      header.appendChild(langLabel);
      
      // 工具栏 - 使用简单的HTML字符串
      header.innerHTML += `
        <div class="vs-code-toolbar">
          <button class="vs-code-btn vs-code-copy-btn" title="复制代码">
            <i class="el-icon-document-copy"></i>
          </button>
          <button class="vs-code-btn vs-code-download-btn" title="下载代码">
            <i class="el-icon-download"></i>
          </button>
        </div>
      `;
      
      container.appendChild(header);
      
      // 创建内容区域（行号+代码）
      const content = document.createElement('div');
      content.className = 'vs-code-content';
      
      // 创建行号区域
      const lineNumbers = document.createElement('div');
      lineNumbers.className = 'vs-code-line-numbers';
      
      // 添加行号
      for (let i = 1; i <= lineCount; i++) {
        const lineNumber = document.createElement('div');
        lineNumber.className = 'vs-code-line-number';
        lineNumber.textContent = i;
        lineNumbers.appendChild(lineNumber);
      }
      
      content.appendChild(lineNumbers);
      
      // 创建代码文本区域
      const textArea = document.createElement('div');
      textArea.className = 'vs-code-text';
      
      // 分割高亮后的HTML为行
      const lines = this.splitHighlightedCode(highlightedHtml, lineCount);
      
      // 添加每一行代码
      lines.forEach(line => {
        const codeLine = document.createElement('div');
        codeLine.className = 'vs-code-line';
        codeLine.innerHTML = line || ' '; // 确保空行也显示
        textArea.appendChild(codeLine);
      });
      
      content.appendChild(textArea);
      container.appendChild(content);
      
      // 添加右上角的复制按钮 - 使用简单的HTML
      const cornerCopyBtn = document.createElement('button');
      cornerCopyBtn.className = 'vs-code-corner-copy-btn';
      cornerCopyBtn.innerHTML = '<i class="el-icon-document-copy"></i> 复制代码';
      cornerCopyBtn.title = '复制代码';
      container.appendChild(cornerCopyBtn);
      
      // 替换原来的pre标签
      pre.parentNode.replaceChild(container, pre);
    },
    
    // 将高亮后的HTML分割为行
    splitHighlightedCode(html, lineCount) {
      // 如果html为空，返回空行数组
      if (!html.trim()) {
        return Array(lineCount).fill('');
      }
      
      // 将HTML按换行符分割
      let lines = html.split('\n');
      
      // 调整行数以匹配原始代码的行数
      if (lines.length < lineCount) {
        // 如果行数不足，添加空行
        lines = lines.concat(Array(lineCount - lines.length).fill(''));
      } else if (lines.length > lineCount) {
        // 如果行数过多，截断多余的行
        lines = lines.slice(0, lineCount);
      }
      
      return lines;
    },
    
    // 复制代码到剪贴板
    copyCodeToClipboard(code, container) {
      if (!code || typeof code !== 'string') {
        this.$message.error('没有可复制的内容');
        return;
      }
      
      try {
        // 现代浏览器API - navigator.clipboard
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(code.trim())
            .then(() => {
              // 添加复制成功的动画效果
              container.classList.add('vs-code-copied');
              setTimeout(() => {
                container.classList.remove('vs-code-copied');
              }, 1000);
              
              // 显示提示信息
              this.$message.success('代码已复制到剪贴板');
            })
            .catch(err => {
              console.error('使用Clipboard API复制失败:', err);
              // 回退到传统方法
              this.fallbackCopy(code, container);
            });
        } else {
          // 回退到传统方法
          this.fallbackCopy(code, container);
        }
      } catch (error) {
        console.error('复制失败:', error);
        this.$message.error('复制失败: ' + error.message);
      }
    },
    
    // 兼容老浏览器的复制方法
    fallbackCopy(code, container) {
      // 创建临时文本区域
      const textarea = document.createElement('textarea');
      textarea.value = code.trim();
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      // 确保元素在视区内，某些浏览器要求
      textarea.style.top = '0';
      textarea.style.left = '0';
      
      document.body.appendChild(textarea);
      
      try {
        // 选择文本
        textarea.focus();
        textarea.select();
        
        // 尝试复制
        const successful = document.execCommand('copy');
        
        if (successful) {
          // 添加复制成功的动画效果
          container.classList.add('vs-code-copied');
          setTimeout(() => {
            container.classList.remove('vs-code-copied');
          }, 1000);
          
          // 显示提示信息
          this.$message.success('代码已复制到剪贴板');
        } else {
          this.$message.error('复制失败，请手动复制');
        }
      } catch (err) {
        console.error('execCommand复制失败:', err);
        this.$message.error('复制失败，请手动复制');
      } finally {
        // 移除临时文本区域
        document.body.removeChild(textarea);
      }
    },
    
    // 下载代码文件
    downloadCode(code, language) {
      if (!code || typeof code !== 'string') {
        this.$message.error('没有可下载的内容');
        return;
      }
      
      try {
        // 确定文件扩展名
        const extensions = {
          java: '.java',
          javascript: '.js',
          js: '.js',
          typescript: '.ts',
          ts: '.ts',
          html: '.html',
          css: '.css',
          python: '.py',
          py: '.py',
          ruby: '.rb',
          php: '.php',
          go: '.go',
          rust: '.rs',
          c: '.c',
          cpp: '.cpp',
          'c++': '.cpp',
          csharp: '.cs',
          'c#': '.cs',
          swift: '.swift',
          kotlin: '.kt',
          scala: '.scala',
          perl: '.pl',
          bash: '.sh',
          shell: '.sh',
          sql: '.sql',
          json: '.json',
          xml: '.xml',
          yaml: '.yml',
          markdown: '.md',
          md: '.md'
        };
        
        // 默认扩展名
        const ext = extensions[language ? language.toLowerCase() : ''] || '.txt';
        
        // 创建Blob对象
        const blob = new Blob([code.trim()], { type: 'text/plain' });
        
        // 创建下载链接
        const a = document.createElement('a');
        const url = URL.createObjectURL(blob);
        a.href = url;
        a.download = `code-snippet${ext}`;
        
        // 添加到DOM以便在Firefox等浏览器触发下载
        document.body.appendChild(a);
        
        // 模拟点击
        a.click();
        
        // 给浏览器一些时间来处理下载
        setTimeout(() => {
          // 清理
          document.body.removeChild(a);
          URL.revokeObjectURL(url);
          
          // 显示提示信息
          this.$message.success(`代码已下载为 code-snippet${ext}`);
        }, 100);
      } catch (error) {
        console.error('下载失败:', error);
        this.$message.error('下载失败: ' + error.message);
      }
    },
    
    // 注入代码块样式表
    injectCodeBlockStyles() {
      // 检查是否已注入
      if (document.getElementById('vs-code-styles')) return;
      
      // 创建样式元素
      const styleElement = document.createElement('style');
      styleElement.id = 'vs-code-styles';
      
      // VS Code风格的CSS样式
      const css = `
        /* 覆盖原有样式 */
        .message-content pre {
          padding: 0 !important;
          margin: 15px 0 !important;
          overflow: hidden !important;
          background: none !important;
          border: none !important;
          box-shadow: none !important;
        }
        
        /* 特定语言样式覆盖 - Java */
        .vs-code-container[data-language="java"] .hljs-keyword {
          color: #569cd6 !important;
          font-weight: bold !important;
        }
        
        .vs-code-container[data-language="java"] .hljs-class {
          color: #4ec9b0 !important;
        }
        
        .vs-code-container[data-language="java"] .hljs-string {
          color: #ce9178 !important;
        }
        
        .vs-code-container[data-language="java"] .hljs-comment {
          color: #6a9955 !important;
          font-style: italic !important;
        }
        
        /* VS Code样式容器 */
        .vs-code-container {
          background-color: #1e1e1e;
          border-radius: 6px;
          overflow: hidden;
          font-family: Consolas, Monaco, monospace;
          font-size: 14px;
          margin: 0;
          box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2);
          border: 1px solid #333;
          position: relative;
        }
        
        /* 代码标题栏 */
        .vs-code-header {
          background-color: #2d2d2d;
          color: #cccccc;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 5px 10px;
          border-bottom: 1px solid #3e3e3e;
          height: 30px;
          font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        
        /* 语言标签 */
        .vs-code-lang {
          font-size: 12px;
          color: #cccccc;
          opacity: 0.8;
        }
        
        /* 工具栏 */
        .vs-code-toolbar {
          display: flex;
        }
        
        /* 按钮样式 */
        .vs-code-btn {
          background: transparent;
          border: none;
          width: 28px;
          height: 28px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #cccccc;
          opacity: 0.7;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.2s;
          margin-left: 4px;
          font-size: 14px;
        }
        
        /* 按钮悬停效果 */
        .vs-code-btn:hover {
          opacity: 1;
          background-color: #3e3e3e;
        }
        
        /* 右上角复制按钮 */
        .vs-code-corner-copy-btn {
          position: absolute;
          top: 45px;
          right: 15px;
          z-index: 100;
          background-color: rgba(0, 122, 204, 0.8);
          color: #fff;
          border: none;
          border-radius: 4px;
          padding: 6px 12px;
          font-size: 13px;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 5px;
          opacity: 0;
          transition: all 0.2s ease;
          font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
          box-shadow: 0 2px 5px rgba(0, 0, 0, 0.3);
        }
        
        /* 显示右上角复制按钮（当鼠标悬停在代码块上时） */
        .vs-code-container:hover .vs-code-corner-copy-btn {
          opacity: 1;
        }
        
        /* 右上角复制按钮悬停效果 */
        .vs-code-corner-copy-btn:hover {
          background-color: rgba(0, 122, 204, 1);
          transform: translateY(-1px);
          box-shadow: 0 3px 8px rgba(0, 0, 0, 0.4);
        }
        
        /* 复制按钮复制成功状态 */
        .vs-code-button-copied {
          background-color: rgba(58, 166, 74, 0.9) !important;
          color: white !important;
        }
        
        /* 代码内容区域 */
        .vs-code-content {
          display: flex;
          padding: 0;
          overflow-x: auto;
          background-color: #1e1e1e;
        }
        
        /* 行号区域 */
        .vs-code-line-numbers {
          padding: 12px 8px;
          text-align: right;
          min-width: 40px;
          border-right: 1px solid #404040;
          color: #858585;
          font-size: 12px;
          line-height: 1.5;
          user-select: none;
          background-color: #1e1e1e;
        }
        
        /* 单行行号 */
        .vs-code-line-number {
          height: 20px;
          font-family: Consolas, Monaco, monospace;
        }
        
        /* 代码文本区域 */
        .vs-code-text {
          flex: 1;
          padding: 12px 16px;
          color: #d4d4d4;
          overflow-x: auto;
          white-space: pre;
          font-family: Consolas, Monaco, monospace;
          font-size: 14px;
          line-height: 1.5;
          background-color: #1e1e1e;
        }
        
        /* 代码行 */
        .vs-code-line {
          height: 20px;
          white-space: pre;
          font-family: Consolas, Monaco, monospace;
        }
        
        /* 复制成功动画 */
        .vs-code-copied {
          animation: vs-code-copy-success 1s ease;
        }
        
        @keyframes vs-code-copy-success {
          0% { box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2); }
          50% { box-shadow: 0 0 0 4px rgba(76, 175, 80, 0.4); }
          100% { box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2); }
        }
        
        /* 语法高亮 - VS Code暗色主题 */
        .vs-code-text .hljs-keyword { color: #569cd6; font-weight: bold; }
        .vs-code-text .hljs-built_in { color: #4ec9b0; }
        .vs-code-text .hljs-type { color: #4ec9b0; }
        .vs-code-text .hljs-literal { color: #569cd6; }
        .vs-code-text .hljs-number { color: #b5cea8; }
        .vs-code-text .hljs-regexp { color: #d16969; }
        .vs-code-text .hljs-string { color: #ce9178; }
        .vs-code-text .hljs-subst { color: #d4d4d4; }
        .vs-code-text .hljs-symbol { color: #d4d4d4; }
        .vs-code-text .hljs-class { color: #4ec9b0; }
        .vs-code-text .hljs-function { color: #dcdcaa; }
        .vs-code-text .hljs-title { color: #dcdcaa; }
        .vs-code-text .hljs-params { color: #9cdcfe; }
        .vs-code-text .hljs-comment { color: #6a9955; font-style: italic; }
        .vs-code-text .hljs-doctag { color: #608b4e; }
        .vs-code-text .hljs-meta { color: #9cdcfe; }
        .vs-code-text .hljs-section { color: gold; }
        .vs-code-text .hljs-name { color: #569cd6; }
        .vs-code-text .hljs-tag { color: #569cd6; }
        .vs-code-text .hljs-attr { color: #9cdcfe; }
        .vs-code-text .hljs-bullet { color: #d4d4d4; }
        .vs-code-text .hljs-code { color: #d4d4d4; }
        .vs-code-text .hljs-emphasis { color: #d4d4d4; font-style: italic; }
        .vs-code-text .hljs-formula { color: #d4d4d4; }
        .vs-code-text .hljs-link { color: #9cdcfe; }
        .vs-code-text .hljs-quote { color: #d4d4d4; }
        .vs-code-text .hljs-selector-tag { color: #d4d4d4; }
        .vs-code-text .hljs-selector-id { color: #d4d4d4; }
        .vs-code-text .hljs-selector-class { color: #d4d4d4; }
        .vs-code-text .hljs-selector-attr { color: #d4d4d4; }
        .vs-code-text .hljs-selector-pseudo { color: #d4d4d4; }
        .vs-code-text .hljs-template-tag { color: #d4d4d4; }
        .vs-code-text .hljs-template-variable { color: #d4d4d4; }
        .vs-code-text .hljs-addition { color: #d4d4d4; }
        .vs-code-text .hljs-deletion { color: #d4d4d4; }
        
        /* Java特定颜色 */
        .language-java .vs-code-text .hljs-keyword { color: #569cd6; font-weight: bold; }
        .language-java .vs-code-text .hljs-class { color: #4ec9b0; }
        .language-java .vs-code-text .hljs-title { color: #4ec9b0; }
        .language-java .vs-code-text .hljs-function { color: #dcdcaa; }
        .language-java .vs-code-text .hljs-string { color: #ce9178; }
        .language-java .vs-code-text .hljs-comment { color: #6a9955; }
        .language-java .vs-code-text .hljs-number { color: #b5cea8; }
        .language-java .vs-code-text .hljs-params { color: #9cdcfe; }
      `;
      
      styleElement.textContent = css;
      document.head.appendChild(styleElement);
    },
    // + 新增：获取团队列表
    fetchTeamList() {
      listMy_team().then(response => {
        if (response.code === 200) {
          if (response.rows && response.rows.length > 0) {
            this.teamList = response.rows;
            
            // 尝试从localStorage获取上次选择的团队ID
            const lastSelectedTeamId = localStorage.getItem('ai-chat-last-selected-team');
            
            // 处理"不使用知识库"选项的情况
            if (lastSelectedTeamId === '') {
              this.selectedTeamId = ''; // 选择"不使用知识库"
            } else if (lastSelectedTeamId && this.teamList.some(team => String(team.teamId) === String(lastSelectedTeamId))) {
              this.selectedTeamId = String(lastSelectedTeamId); // 确保使用字符串进行比较和赋值
            } else {
              // 默认选择第一个团队, 确保是字符串
              this.selectedTeamId = String(this.teamList[0].teamId);
                localStorage.setItem('ai-chat-last-selected-team', this.selectedTeamId);
            }
          } else { // code === 200 但是 rows 为空
            this.teamList = [];
            this.selectedTeamId = ''; // 设置为空字符串表示"不使用知识库"
            localStorage.setItem('ai-chat-last-selected-team', '');
            this.$message.warning('未找到任何团队，已切换到不使用知识库模式');
          }
        } else { // code !== 200，请求本身失败
          this.teamList = [];
          this.selectedTeamId = '';
          localStorage.setItem('ai-chat-last-selected-team', '');
          this.$message.error(response.msg || '获取团队列表失败');
        }
      }).catch(error => {
        this.teamList = [];
        this.selectedTeamId = '';
        localStorage.setItem('ai-chat-last-selected-team', '');
        console.error('获取团队列表失败:', error);
        this.$message.error('无法连接到服务器获取团队列表，请稍后再试');
      });
    },
    // 检查认证并初始化会话
    checkAuthAndInitSessions() {
      if (!getToken()) {
        this.$message.error('您未登录或登录已过期，请重新登录');
        // 可以选择跳转到登录页
        // this.$router.push('/login'); 
        return;
      }
      
      // 加载会话列表
      this.loadSessionList();
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
          // this.saveSessionList(); // 移除对localStorage的保存
          
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
          // this.saveSessionList(); // 移除对localStorage的保存
        }
        
        // 如果删除的是当前会话，切换到另一个会话
        if (this.sessionId === sessionId) {
          localStorage.removeItem('ai-chat-last-session'); // 清除被删除的会话ID
          if (this.sessionList.length > 0) {
            // 切换到列表中的第一个会话
            this.switchSession(this.sessionList[0].id); 
          } else {
            // 如果没有其他会话了，则创建一个新的
            this.sessionId = null;
            this.messages = [];
            this.createNewSession(); // 创建新会话会设置loading=false
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
    
    // 保存会话列表到本地存储 (此方法不再需要，将被移除)
    // saveSessionList() {
    //   localStorage.setItem('ai-chat-sessions', JSON.stringify(this.sessionList));
    // },
    
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
        // this.saveSessionList(); // 移除对localStorage的保存
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
          
          // 更新会话标题和可能的创建时间
          const currentSessionInList = this.sessionList.find(s => s.id === this.sessionId);
          if (currentSessionInList && historyMessages.length > 0) {
            // 使用第一条消息的创建时间更新会话的 createTime，使其更准确
            if (historyMessages[0].createTime) {
              // 只有当初始的createTime是占位符时才更新，或者总是更新
              // 这里我们假设如果后端有createTime就用它
              currentSessionInList.createTime = new Date(historyMessages[0].createTime).toISOString();
            }

            const firstUserMessage = historyMessages.find(msg => msg.role === 'user');
            if (firstUserMessage) {
              this.updateSessionTitle(this.sessionId, firstUserMessage.content);
            } else if (currentSessionInList.title === '新对话' && historyMessages[0] && historyMessages[0].content) {
              // 如果没有用户消息，但有AI消息，且标题是默认的，可以尝试用AI消息更新
              let aiTitle = historyMessages[0].content.substring(0, 20);
              if (historyMessages[0].content.length > 20) aiTitle += "...";
              this.updateSessionTitle(this.sessionId, `AI: ${aiTitle}`);
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
        // 清空API目前没有，这里只清空前端messages和重置标题
        this.messages = []; 
        this.$message.success('对话内容已清空');
        
        // 重置会话标题
        const session = this.sessionList.find(s => s.id === this.sessionId);
        if (session) {
          session.title = '新对话';
          // this.saveSessionList(); // 移除对localStorage的保存
        }
      }).catch(() => {});
    },
    
    // 处理团队选择变化
    handleTeamChange(newTeamId) { // newTeamId will be a string from el-select
      // this.selectedTeamId is already updated by v-model to newTeamId.
      
      if (newTeamId) {
      const selectedTeam = this.teamList.find(team => String(team.teamId) === newTeamId);
      if (selectedTeam) {
        this.$message.info(`知识库已切换到: ${selectedTeam.name}`);
      }
      } else {
        this.$message.info('已切换到不使用知识库模式');
      }
      
      localStorage.setItem('ai-chat-last-selected-team', newTeamId || ''); // Store as string
      
      // 可在此处添加切换团队后需要的其他逻辑, 例如:
      // this.messages = [];
      // this.userInput = '';
      // if (this.sessionId) {
      //   this.loadChatHistory(); // 或者调用特定方法以适应新的团队上下文
      // }
    },
    
    // 获取头像URL
    getAvatarUrl(avatar) {
      if (!avatar) return '';
      // 如果头像路径是相对路径，则加上基础URL
      if (avatar.startsWith('/')) {
        return process.env.VUE_APP_BASE_API + avatar;
      }
      return avatar;
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

/* 复制成功动画 */
.copy-success-animation {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) scale(0.5) !important;
  width: 80px !important;
  height: 80px !important;
  background-color: rgba(82, 196, 26, 0.9) !important;
  border-radius: 50% !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  z-index: 9999 !important;
  opacity: 0 !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 5px 20px rgba(82, 196, 26, 0.4) !important;
}

.copy-success-animation.show {
  opacity: 1 !important;
  transform: translate(-50%, -50%) scale(1) !important;
}

.copy-success-animation i {
  color: white !important;
  font-size: 36px !important;
  animation: pulse 1s ease-in-out !important;
}

/* 让代码块内的复制按钮默认隐藏，鼠标悬停时显示 */
.message-content >>> pre .code-copy-button {
  opacity: 0 !important;
  transition: opacity 0.2s !important;
}

.message-content >>> pre:hover .code-copy-button {
  opacity: 0.8 !important;
}

/* 代码块主样式 - VS Code风格 */
.message-content >>> pre {
  position: relative !important;
  background-color: #1e1e1e !important; /* VS Code暗色主题背景色 */
  color: #d4d4d4 !important;
  border-radius: 8px !important;
          margin: 15px 0 !important;
  padding: 0 !important; /* 移除内边距，让顶部标签栏能够延伸 */
  border: none !important;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2) !important;
          overflow: hidden !important;
  max-width: 100% !important;
  margin-bottom: 15px !important;
}

/* 顶部语言标签栏 */
.message-content >>> .code-lang-label {
  position: relative !important;
  display: flex !important; /* 使用flex布局，便于放置工具栏 */
  justify-content: space-between !important; /* 语言名称左侧，工具栏右侧 */
  align-items: center !important;
  width: 100% !important;
  height: 30px !important; /* 固定高度 */
  background-color: #2d2d2d !important;
  color: #d4d4d4 !important;
  font-size: 12px !important;
  padding: 0 10px !important;
  text-align: left !important;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif !important;
  border-bottom: 1px solid #3e3e3e !important;
  z-index: 5 !important;
}

/* 调整工具栏位置 */
.message-content >>> .code-toolbar {
  position: static !important; /* 修改为static，避免absolute定位问题 */
  display: flex !important;
  align-items: center !important;
  margin-left: auto !important; /* 将工具栏推到右侧 */
}

/* 调整复制按钮和下载按钮样式 */
.message-content >>> .code-copy-button,
.message-content >>> .code-download-button {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 28px !important;
  height: 28px !important;
  background-color: transparent !important;
          border: none !important;
  border-radius: 4px !important;
  color: #d4d4d4 !important;
  font-size: 14px !important;
  cursor: pointer !important;
  opacity: 0.7 !important;
  transition: all 0.2s !important;
  margin: 0 2px !important;
}

.message-content >>> .code-copy-button:hover,
.message-content >>> .code-download-button:hover {
  opacity: 1 !important;
  background-color: #3e3e3e !important;
}

/* 行号样式 */
.message-content >>> pre code::before {
  content: attr(data-line-numbers) !important;
  position: absolute !important;
  left: 0 !important;
  top: 12px !important;
  width: 40px !important;
  padding-right: 8px !important;
  text-align: right !important;
  color: #858585 !important;
  font-size: 12px !important;
  font-family: Consolas, Monaco, monospace !important;
  line-height: 1.5 !important;
  border-right: 1px solid #404040 !important;
  user-select: none !important;
  pointer-events: none !important;
  white-space: pre !important;
  z-index: 2 !important;
}

/* 代码区域样式 */
.message-content >>> pre code {
  font-family: Consolas, Monaco, monospace !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
  padding: 12px 16px 12px 50px !important; /* 左侧留出行号空间 */
  color: #d4d4d4 !important;
  background-color: transparent !important;
  overflow-x: auto !important;
  white-space: pre !important;
  display: block !important;
}

/* 添加行高亮效果 */
.message-content >>> pre code .highlighted-line {
  background-color: rgba(255, 255, 255, 0.07) !important;
  display: block !important;
  margin: 0 -16px 0 -50px !important;
  padding: 0 16px 0 50px !important;
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
  align-items: center; // Vertically align session-details and header-actions
  border-bottom: 1px solid #eaeaea;
  min-height: 70px; // Ensure enough height for two lines if title wraps

  .session-details {
    display: flex;
    flex-direction: column; 
    align-items: flex-start; 
    gap: 8px; 
    flex-grow: 1; // Allow this section to take available space
    min-width: 0; // Prevent overflow issues with long titles
  }
  
  .session-title-text { // Moved out of .current-session-info if that class is removed
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    // max-width: 200px; // Consider removing if flex-grow handles width well or adjust based on layout
  }

  // .current-session-info and .team-selector might be removed or styles merged into new classes
  // Ensure to remove or comment out their specific styles if they are no longer used
  // For example:
  // .current-session-info {
  //   display: flex;
  //   align-items: center;
  //   gap: 15px; 
  // }
  // .team-selector { 
  //    min-width: 150px; 
  // }

  .knowledge-base-selector {
    display: flex;
    align-items: center;
    gap: 8px;

    .kb-label {
      font-size: 13px;
      color: #525252; 
      display: flex;
      align-items: center;
      gap: 5px; 
      font-weight: 500;
      i {
        font-size: 16px;
        color: #409EFF; 
      }
    }

    .team-select-dropdown {
      width: 220px; 
      
      :deep(.el-input__inner) { 
        border-radius: 6px; 
        border-color: #dcdfe6;
        height: 32px; 
        line-height: 32px;
        font-size: 13px; // Match label font size for consistency
        &:focus {
          border-color: #409EFF;
          box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
        }
      }
      :deep(.el-input__icon) {
        line-height: 32px; 
      }
    }
  }

  .header-actions {
    display: flex;
    flex-shrink: 0; // Prevent actions from shrinking
  }

  // h2 is now .session-title-text
  // h2 {
  //   margin: 0;
  //   font-size: 16px;
  //   font-weight: 600;
  //   color: #303133;
  // }
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

/* START OF REVISED TYPING INDICATOR STYLES */
/* Ensure to remove or replace any existing .typing-indicator, .typing-dots, and @keyframes typing-dot styles */
.typing-indicator {
  display: flex;
  align-items: flex-start; /* Align avatar and bubble to the top */
  margin-bottom: 16px; /* Consistent with .message margin */

  /* .avatar class is globally styled (e.g., margin: 0 8px;), which provides spacing */

  .indicator-bubble {
    background-color: #fff; /* Matches incoming message bubble background */
    padding: 10px 15px;    /* Adjusted padding for a compact look for dots */
    border-radius: 4px 16px 16px 16px; /* Matches incoming message bubble border-radius */
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04); /* Matches incoming message bubble shadow */
    display: flex; /* Needed if dots container needs to align items, though typing-dots is also flex */
    align-items: center; /* Vertically center dots if bubble is taller than dots */
  }
}

.typing-dots {
  display: flex;
  align-items: center; /* Vertically center the dots themselves */

  span {
    display: inline-block;
    width: 8px;
    height: 8px;
    margin-right: 4px;
    background-color: #c0c4cc; /* Element UI secondary text color, good for dots */
    border-radius: 50%;
    animation: typing-dot-animation 1.4s infinite ease-in-out; /* Using a unique animation name */

    &:nth-child(1) {
      animation-delay: 0s;
    }

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
      margin-right: 0; /* No margin for the last dot */
    }
  }
}

@keyframes typing-dot-animation { /* Definition for the unique animation name */
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.7; /* Slight opacity variation for effect */
  }
  30% {
    transform: translateY(-5px); /* Animation jump height */
    opacity: 1;
  }
}
/* END OF REVISED TYPING INDICATOR STYLES */

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
    padding: 12px 45px 12px 16px; // 调整右边距给发送按钮留空间
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

/* 代码高亮样式 */
.message-content >>> .hljs {
  border-radius: 6px !important;
  background-color: #f8f9fa !important;
  color: #24292e !important;
  display: block !important;
}

.message-content >>> .hljs-keyword {
  color: #569cd6 !important; /* 蓝色 - class, public, static */
}

.message-content >>> .hljs-class, 
.message-content >>> .hljs-title {
  color: #4ec9b0 !important; /* 浅绿色 - 类名 */
}

.message-content >>> .hljs-function {
  color: #dcdcaa !important; /* 黄色 - 方法名 */
}

.message-content >>> .hljs-string {
  color: #ce9178 !important; /* 橙色 - 字符串 */
}

.message-content >>> .hljs-comment {
  color: #6a9955 !important; /* 绿色 - 注释 */
}

.message-content >>> .hljs-number {
  color: #b5cea8 !important; /* 淡绿色 - 数字 */
}

.message-content >>> .hljs-params {
  color: #9cdcfe !important; /* 淡蓝色 - 参数 */
}

.message-content >>> .hljs-variable {
  color: #9cdcfe !important; /* 淡蓝色 - 变量 */
}

.message-content >>> .hljs-operator {
  color: #d4d4d4 !important; /* 白色 - 操作符 */
}

.message-content >>> .hljs-punctuation {
  color: #d4d4d4 !important; /* 白色 - 标点符号 */
}

.message-content >>> .hljs-property {
  color: #9cdcfe !important; /* 淡蓝色 - 属性 */
}

.message-content >>> .hljs-tag {
  color: #569cd6 !important; /* 蓝色 - 标签 */
}

.message-content >>> .hljs-attr {
  color: #9cdcfe !important; /* 淡蓝色 - 属性名 */
}

/* 确保复制成功消息样式 */
.code-copy-message {
  background-color: #f0f9eb !important;
  border-color: #e1f3d8 !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
}

@keyframes pulse {
  0% { transform: scale(0.5); opacity: 0; }
  50% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(1); opacity: 1; }
}

/* 修复代码块内容超出容器的问题 */
.message-content >>> pre {
  position: relative !important;
  max-width: calc(100% - 10px) !important;
  margin: 10px 5px !important;
  white-space: pre-wrap !important;
  word-break: break-all !important;
  overflow-wrap: break-word !important;
}

/* 确保代码块内的语言标签和复制按钮位置正确 */
.message-content >>> pre .code-copy-button,
.message-content >>> pre .code-lang-label {
  position: absolute !important;
  z-index: 5 !important;
}

/* 确保按钮位于语言标签之上 */
.message-content >>> pre .code-copy-button {
  z-index: 6 !important;
}

/* 覆盖其他可能影响按钮的样式 */
.message-content * >>> button.code-copy-button {
  position: absolute !important;
  top: 8px !important;
  right: 8px !important;
  border: 1px solid #e0e0e0 !important;
  background-color: rgba(255, 255, 255, 0.8) !important; 
}

/* 顶部工具栏样式 */
.message-content >>> pre {
  position: relative !important;
}

/* 顶部工具栏右侧按钮区域 */
.message-content >>> .code-toolbar {
  position: absolute !important;
  top: 0 !important;
  right: 0 !important;
  display: flex !important;
  padding: 2px !important;
}

/* 复制按钮放在顶部工具栏中 */
.message-content >>> .code-copy-button {
  position: relative !important;
  top: auto !important;
  right: auto !important;
  margin: 2px !important;
  width: 24px !important;
  height: 24px !important;
  border-radius: 4px !important;
}

/* 复制成功效果 */
.message-content >>> pre.copied {
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.5) !important;
  animation: success-pulse 1s ease !important;
}

@keyframes success-pulse {
  0% { box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.1) !important; }
  50% { box-shadow: 0 0 0 4px rgba(76, 175, 80, 0.4) !important; }
  100% { box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2) !important; }
}

/* 代码下载按钮 */
.message-content >>> .code-download-button {
  width: 24px !important;
  height: 24px !important;
  border: none !important;
  background-color: transparent !important;
  color: #d4d4d4 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  opacity: 0.6 !important;
  cursor: pointer !important;
  margin: 2px !important;
  border-radius: 4px !important;
}

.message-content >>> .code-download-button:hover {
  opacity: 1 !important;
  background-color: #404040 !important;
}

/* Java特定语法高亮 */
.message-content >>> pre.language-java .hljs-keyword {
  color: #569cd6 !important; /* 关键字蓝色 */
}

.message-content >>> pre.language-java .hljs-type {
  color: #4ec9b0 !important; /* 类型名称绿色 */
}

.message-content >>> pre.language-java .hljs-class {
  color: #4ec9b0 !important; /* 类名绿色 */
}

.message-content >>> pre.language-java .hljs-title {
  color: #4ec9b0 !important; /* 类名绿色 */
}

.message-content >>> pre.language-java .hljs-function {
  color: #dcdcaa !important; /* 方法名黄色 */
}

.message-content >>> pre.language-java .hljs-built_in {
  color: #569cd6 !important; /* 内置对象蓝色 */
}

.message-content >>> pre.language-java .hljs-meta {
  color: #569cd6 !important; /* 元信息蓝色 */
}

/* 确保图标正确显示 */
.message-content >>> .code-copy-button i,
.message-content >>> .code-download-button i {
  font-size: 16px !important;
  line-height: 1 !important;
}

.team-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.team-option-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  overflow: hidden;
}

.team-option-name {
  font-size: 14px;
  color: #333;
}

/* 团队选择组件样式 */
.team-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
  transition: all 0.2s;
}

.team-option-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid #eaeaea;
  background-color: #f5f7fa;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409EFF;
  font-weight: bold;
  font-size: 14px;
}

.team-option-name {
  font-size: 14px;
  color: #333;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 改进下拉选项显示 */
:deep(.el-select-dropdown__item) {
  padding: 0 15px;
}

:deep(.el-select-dropdown__item.hover),
:deep(.el-select-dropdown__item:hover) {
  background-color: #f0f7ff;
}

:deep(.el-select-dropdown__item.selected) {
  background-color: #ecf5ff;
  color: #409EFF;
  font-weight: bold;
}
</style> 