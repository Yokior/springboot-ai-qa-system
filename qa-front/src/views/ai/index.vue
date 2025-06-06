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
import hljs from 'highlight.js/lib/common'; // 改用common包，包含常用语言
import 'highlight.js/styles/vs2015.css'; // 导入VS Code风格的代码高亮样式
import { mapState } from 'vuex';
import { sendChatMessage, sendStreamChatMessage, createSession, deleteSession, getChatHistory, getUserSessions, clearChatHistory } from '@/api/ai/index';
import { getToken } from '@/utils/auth';
import { parseTime } from '@/utils/yokior';
import { getInfo } from '@/api/login';
import { listMy_team } from '@/api/team/my_team';

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
            selectedTeamId: null, // 当前选择的团队ID
            teamList: [] // 团队列表
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
        // 当前选中的团队对象
        currentSelectedTeam() {
            return this.teamList.find(team => team.teamId === this.selectedTeamId) || null;
        }
    },
    watch: {
        messages: {
            deep: true,
            handler() {
                this.$nextTick(() => {
                    this.addCopyButtonsToCodeBlocks();
                });
            }
        },
        'currentStreamingMessage.content': function () {
            if (this.streamingResponse) {
                this.$nextTick(() => {
                    this.addCopyButtonsToCodeBlocks();
                });
            }
        }
    },
    created() {
        // Lifecycle hooks should contain minimal logic.
        // Initialization logic is moved to mounted.
    },
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

        // 为整个文档添加事件委托来处理代码块按钮点击
        document.addEventListener('click', (e) => {
            let targetButton = null;
            let actionType = null;

            // 检查是否点击了复制按钮
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
                e.preventDefault();
                e.stopPropagation();

                const container = targetButton.closest('.vs-code-container');
                if (!container) return;

                const originalCode = container.dataset.originalCode;
                const language = container.dataset.language || '';

                if (!originalCode) {
                    this.$message.error('无法获取代码内容');
                    return;
                }

                if (actionType === 'copy') {
                    this.copyCodeToClipboard(originalCode, container);
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
                this.userAvatar = '';
            }
        }).catch(error => {
            console.error("获取用户信息失败:", error);
            this.userAvatar = ''; // 出错时也设置默认
        });

        this.checkAuthAndInitSessions();
        this.fetchTeamList();
    },
    updated() {
        // 组件更新后处理代码块
        this.addCopyButtonsToCodeBlocks();
    },
    methods: {
        // 检查认证并初始化会话
        checkAuthAndInitSessions() {
            if (!getToken()) {
                this.$message.error('您未登录或登录已过期，请重新登录');
                // this.$router.push('/login'); 
                return;
            }

            // 加载会话列表
            this.loadSessionList();
        },

        // 加载会话列表
        loadSessionList() {
            this.loading = true;
            this.messages = [];
            this.currentStreamingMessage = null;
            this.streamingResponse = false;

            getUserSessions().then(response => {
                if (response.code === 200 && response.data) {
                    // 直接使用返回的会话对象列表，包含 sessionId、title 和 createTime
                    this.sessionList = response.data.map(session => ({
                        id: session.sessionId,
                        title: session.title || '新对话',
                        createTime: session.createTime
                    })).sort((a, b) => new Date(b.createTime) - new Date(a.createTime));

                    const lastSessionId = localStorage.getItem('ai-chat-last-session');

                    if (lastSessionId && this.sessionList.some(s => s.id === lastSessionId)) {
                        this.sessionId = lastSessionId;
                        this.loadChatHistory();
                    } else if (this.sessionList.length > 0) {
                        this.sessionId = this.sessionList[0].id;
                        localStorage.setItem('ai-chat-last-session', this.sessionId);
                        this.loadChatHistory();
                    } else {
                        this.loading = false;
                        this.createNewSession();
                    }
                } else {
                    this.$message.error(response.msg || '获取会话列表失败');
                    this.loading = false;
                    this.sessionList = [];
                    this.createNewSession();
                }
            }).catch(error => {
                console.error('加载会话列表失败:', error);
                this.$message.error('无法连接到服务器获取会话列表，请尝试刷新或稍后再试');
                this.loading = false;
                this.sessionList = [];
                this.createNewSession();
            });
        },

        // 切换会话
        switchSession(sessionId) {
            if (this.sessionId === sessionId || this.loading) return;

            this.messages = [];
            this.currentStreamingMessage = null;
            this.streamingResponse = false;

            this.sessionId = sessionId;
            this.loading = true;

            localStorage.setItem('ai-chat-last-session', sessionId);

            this.loadChatHistory();
        },

        // 格式化日期为字符串，格式：YYYY-MM-DD HH:mm:ss
        formatDateToString(date) {
            const d = date || new Date();
            return d.getFullYear() + '-' + 
                  String(d.getMonth() + 1).padStart(2, '0') + '-' + 
                  String(d.getDate()).padStart(2, '0') + ' ' + 
                  String(d.getHours()).padStart(2, '0') + ':' + 
                  String(d.getMinutes()).padStart(2, '0') + ':' + 
                  String(d.getSeconds()).padStart(2, '0');
        },

        // 创建新会话
        createNewSession() {
            if (this.loading) return;

            this.messages = [];
            this.currentStreamingMessage = null;
            this.streamingResponse = false;

            this.loading = true;
            createSession().then(response => {
                if (response.code === 200) {
                    const newSessionId = response.data.sessionId;
                    const formattedDate = this.formatDateToString(new Date());

                    const newSession = {
                        id: newSessionId,
                        title: '新对话',
                        createTime: formattedDate
                    };

                    this.sessionList.unshift(newSession);

                    this.sessionId = newSessionId;

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

        // ...
        // 确认删除会话
        confirmDeleteSession(sessionId) {
            this.$confirm('确定要删除这个会话吗?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.deleteSessionById(sessionId);
            }).catch(() => { });
        },
        // ...

        // 请完整替换此方法
        deleteSessionById(sessionId) {

            if (this.loading) {
                console.warn('[诊断] 1.1 deleteSessionById 在 loading 为 true 时被调用，已阻止。');
                return;
            }

            this.loading = true;

            // 设置一个8秒的超时强制解锁，防止无限卡死
            const failsafeTimeout = setTimeout(() => {
                if (this.loading) {
                    console.error('[诊断] Failsafe: 8秒超时！强制设置 this.loading = false。这表明有逻辑分支未正常结束。');
                    this.loading = false;
                }
            }, 8000);

            deleteSession(sessionId).then(() => {
                const index = this.sessionList.findIndex(s => s.id === sessionId);
                if (index !== -1) {
                    this.sessionList.splice(index, 1);
                }
                this.$message.success('会话已删除');

                if (this.sessionId === sessionId) {
                    localStorage.removeItem('ai-chat-last-session');
                    this.messages = [];

                    if (this.sessionList.length > 0) {
                        this.sessionId = this.sessionList[0].id;
                        localStorage.setItem('ai-chat-last-session', this.sessionId);
                        // 调用诊断版的 loadChatHistory
                        this.loadChatHistory(failsafeTimeout); // 传入超时计时器ID
                    } else {
                        this.sessionId = null;
                        createSession().then(response => {
                            clearTimeout(failsafeTimeout); // 清除超时
                            if (response.code === 200) {
                                const newSessionId = response.data.sessionId;
                                const formattedDate = this.formatDateToString(new Date());
                                const newSession = { 
                                    id: newSessionId, 
                                    title: '新对话', 
                                    createTime: formattedDate 
                                };
                                this.sessionList.unshift(newSession);
                                this.sessionId = newSessionId;
                                localStorage.setItem('ai-chat-last-session', newSessionId);
                                this.$message.success('已创建新会话');
                                this.loading = false;
                            } else {
                                this.loading = false;
                            }
                        }).catch(error => {
                            clearTimeout(failsafeTimeout); // 清除超时
                            this.loading = false;
                        });
                    }
                } else {
                    clearTimeout(failsafeTimeout); // 清除超时
                    this.loading = false;
                }
            }).catch(error => {
                clearTimeout(failsafeTimeout); // 清除超时
                this.loading = false;
            });
        },

        // 更新会话标题
        updateSessionTitle(sessionId, firstMessage) {
            if (!firstMessage || !sessionId) return;

            const session = this.sessionList.find(s => s.id === sessionId);
            if (session && session.title === '新对话') {
                let title = firstMessage.trim();
                if (title.length > 20) {
                    title = title.substring(0, 20) + '...';
                }
                session.title = title;
            }
        },

        // 请完整替换此方法
        loadChatHistory(failsafeTimeout = null) {

            if (!this.sessionId) {
                if (failsafeTimeout) clearTimeout(failsafeTimeout);
                this.loading = false;
                return;
            }

            // 注意：这个方法依赖调用者提前设置 this.loading = true
            getChatHistory(this.sessionId).then(response => {
                if (failsafeTimeout) clearTimeout(failsafeTimeout); // 清除超时
                // ... 省略消息处理逻辑
                const historyMessages = response.data.messages || [];
                historyMessages.forEach(msg => {
                    this.messages.push({
                        type: msg.role === 'user' ? 'user' : 'ai',
                        content: msg.content,
                        time: new Date(msg.createTime)
                    });
                });
                // ...
                this.loading = false;
            }).catch(error => {
                if (failsafeTimeout) clearTimeout(failsafeTimeout); // 清除超时
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

                clearChatHistory(this.sessionId).then((res) => {
                    if (res.code === 200) {
                        this.$message.success('对话历史已清空');
                    } else {
                        this.$message.error('清空对话历史失败');
                    }
                }).catch(() => {
                    this.$message.error('清空对话历史失败');
                });

                // 更新会话标题
                const session = this.sessionList.find(s => s.id === this.sessionId);
                if (session) {
                    session.title = '新对话';
                }
            }).catch(() => { });
        },

        // 发送消息
        sendMessage() {
            if (!this.userInput.trim() || this.loading || !this.sessionId) return;

            const userMessage = {
                type: 'user',
                content: this.userInput,
                time: new Date()
            };
            this.messages.push(userMessage);

            if (this.messages.length === 1 || (this.messages.length === 2 && this.messages[0].type === 'ai')) {
                this.updateSessionTitle(this.sessionId, this.userInput);
            }

            const question = this.userInput;
            this.userInput = '';

            this.$nextTick(() => {
                this.scrollToBottom();
            });

            this.loading = true;

            this.sendStreamChatRequest(question);
        },

        // 发送流式聊天请求
        sendStreamChatRequest(question) {
            if (!getToken()) {
                this.$message.error('您未登录或登录已过期，请重新登录');
                this.loading = false;
                return;
            }

            const requestData = {
                prompt: question,
                sessionId: this.sessionId,
                options: {}
            };

            if (this.selectedTeamId) {
                requestData.options.teamId = this.selectedTeamId;
            }

            const onMessage = (content) => {
                if (!this.streamingResponse) {
                    this.streamingResponse = true;

                    const aiMessage = {
                        type: 'ai',
                        content: '',
                        time: new Date()
                    };
                    this.messages.push(aiMessage);
                    this.currentStreamingMessage = aiMessage;
                }

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
                    console.warn('currentStreamingMessage is null when trying to append content.');
                }

                this.$nextTick(() => {
                    this.scrollToBottom();
                });
            };

            const onError = (error) => {
                this.handleError('网络错误，无法连接到服务器: ' + error.message);
                this.streamingResponse = false;
                this.loading = false;
            };

            const onComplete = () => {
                this.streamingResponse = false;
                this.loading = false;
                this.currentStreamingMessage = null;
            };

            sendStreamChatMessage(requestData, onMessage, onError, onComplete);
        },

        // 处理错误
        handleError(errorMsg) {
            this.error = errorMsg;
            this.$message.error(errorMsg);
            this.loading = false;
            this.streamingResponse = false;

            if (this.currentStreamingMessage && !this.currentStreamingMessage.content) {
                this.currentStreamingMessage.content = `抱歉，出现了一个错误: ${errorMsg}`;
            } else {
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
                let processedContent = this.preprocessCodeBlocks ? this.preprocessCodeBlocks(content) : content;
                const html = marked(processedContent);

                const tempElement = document.createElement('div');
                tempElement.innerHTML = html;

                const preElements = tempElement.querySelectorAll('pre');
                preElements.forEach(pre => {
                    const codeElement = pre.querySelector('code');
                    if (codeElement) {
                        this.buildVSCodeStyleBlock(pre, codeElement);
                    }
                });

                return tempElement.innerHTML;
            } catch (error) {
                console.error('格式化消息出错:', error);
                return content;
            }
        },

        // ... [ The rest of the methods from copyToClipboard to getAvatarUrl are unique and correct, so they remain unchanged ] ...
        // ... [ I am including them here for completeness ] ...

        // 复制文本到剪贴板 (Legacy - replaced by copyCodeToClipboard but kept for reference if needed elsewhere)
        copyToClipboard(text) {
            if (!text) {
                this.$message.error('没有可复制的内容');
                return;
            }

            const textarea = document.createElement('textarea');
            textarea.value = text.trim();
            textarea.style.position = 'fixed';
            textarea.style.left = '-9999px';
            textarea.style.top = '-9999px';
            document.body.appendChild(textarea);

            try {
                textarea.select();
                textarea.setSelectionRange(0, 99999);
                const successful = document.execCommand('copy');

                if (successful) {
                    this.$message({
                        message: '代码已复制到剪贴板',
                        type: 'success',
                        duration: 1500,
                        customClass: 'code-copy-message'
                    });
                    this.showCopyAnimation();
                } else {
                    this.$message.error('复制失败，请手动复制');
                }
            } catch (err) {
                console.error('复制失败:', err);
                this.$message.error('复制失败: ' + err);
            } finally {
                document.body.removeChild(textarea);
            }
        },

        // 显示复制成功动画
        showCopyAnimation() {
            const animation = document.createElement('div');
            animation.className = 'copy-success-animation';
            animation.innerHTML = '<i class="el-icon-check"></i>';
            document.body.appendChild(animation);

            setTimeout(() => {
                animation.classList.add('show');
            }, 10);

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

            if (content.includes('```')) return content;

            if (content.includes('// HelloWorld.java') && content.includes('public class HelloWorld')) {
                return "```java\n" + content + "\n```";
            }

            let processedContent = content;

            const codePatterns = {
                java: [/public\s+class\s+\w+/, /public\s+static\s+void\s+main/, /import\s+java\./, /public\s+static\s+void\s+\w+\s*\(/, /private\s+\w+\s+\w+\s*\(/],
                javascript: [/function\s+\w+\s*\(/, /const\s+\w+\s*=/, /let\s+\w+\s*=/, /var\s+\w+\s*=/, /export\s+default/, /import\s+.*\s+from/, /^\s*\w+\.\w+\s*\(/m],
                python: [/def\s+\w+\s*\(/, /import\s+\w+/, /from\s+\w+\s+import/, /class\s+\w+\s*\(/, /class\s+\w+:/],
                html: [/<html/i, /<body/i, /<div/i, /<script/i, /<\/\w+>/i],
                css: [/\w+\s*{\s*[\w\-]+\s*:/, /\.\w+\s*{/, /#\w+\s*{/, /@media\s+/, /@import\s+/],
                sql: [/SELECT\s+.*\s+FROM/i, /INSERT\s+INTO/i, /UPDATE\s+.*\s+SET/i, /DELETE\s+FROM/i, /CREATE\s+TABLE/i]
            };

            let detectedLanguage = null;
            let highestMatches = 0;

            Object.entries(codePatterns).forEach(([lang, patterns]) => {
                const matches = patterns.filter(pattern => pattern.test(content)).length;
                if (matches > highestMatches) {
                    highestMatches = matches;
                    detectedLanguage = lang;
                }
            });

            if (highestMatches < 2) {
                return content;
            }

            if (detectedLanguage) {
                try {
                    processedContent = this.formatCodeByLanguage(processedContent, detectedLanguage);

                    if (content.includes('---')) {
                        return this.processSplitCodeSections(content, codePatterns);
                    } else {
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
            switch (language) {
                case 'java': return this.formatJavaCode(code);
                case 'javascript': return this.formatJavascriptCode(code);
                case 'python': return this.formatPythonCode(code);
                case 'html': return this.formatHtmlCode(code);
                case 'css': return this.formatCssCode(code);
                case 'sql': return this.formatSqlCode(code);
                default: return code;
            }
        },

        // ... [ specific formatters for java, js, python, etc. are correct and unique ] ...
        formatJavaCode(code) { return code.replace(/public/g, "\npublic ").replace(/private/g, "\nprivate ").replace(/protected/g, "\nprotected ").replace(/class\s+/g, "class ").replace(/for\s*\(/g, "for (").replace(/if\s*\(/g, "if (").replace(/else\s*{/g, "else {").replace(/\)\s*{/g, ") {").replace(/;\s*/g, ";\n").replace(/}\s*/g, "}\n").replace(/{\s*/g, "{\n  ").replace(/\s{2,}/g, "  ").replace(/import\s+/g, "import ").replace(/package\s+/g, "package "); },
        formatJavascriptCode(code) { return code.replace(/function\s+/g, "\nfunction ").replace(/const\s+/g, "\nconst ").replace(/let\s+/g, "\nlet ").replace(/var\s+/g, "\nvar ").replace(/for\s*\(/g, "for (").replace(/if\s*\(/g, "if (").replace(/else\s*{/g, "else {").replace(/\)\s*{/g, ") {").replace(/;\s*/g, ";\n").replace(/}\s*/g, "}\n").replace(/{\s*/g, "{\n  ").replace(/\s{2,}/g, "  ").replace(/import\s+/g, "import ").replace(/export\s+/g, "export "); },
        formatPythonCode(code) { return code.replace(/def\s+/g, "\ndef ").replace(/class\s+/g, "\nclass ").replace(/import\s+/g, "\nimport ").replace(/from\s+/g, "\nfrom ").replace(/:\s*/g, ":\n  ").replace(/if\s+/g, "\nif ").replace(/elif\s+/g, "\nelif ").replace(/else\s*:/g, "\nelse:").replace(/for\s+/g, "\nfor ").replace(/while\s+/g, "\nwhile ").replace(/try\s*:/g, "\ntry:").replace(/except\s+/g, "\nexcept ").replace(/finally\s*:/g, "\nfinally:").replace(/\n\s*\n/g, "\n\n"); },
        formatHtmlCode(code) { return code.replace(/</g, "\n<").replace(/>/g, ">\n").replace(/\n\s*\n/g, "\n"); },
        formatCssCode(code) { return code.replace(/{/g, " {\n  ").replace(/}/g, "\n}\n").replace(/;/g, ";\n  ").replace(/\n\s*\n/g, "\n"); },
        formatSqlCode(code) { return code.replace(/SELECT/ig, "\nSELECT").replace(/FROM/ig, "\nFROM").replace(/WHERE/ig, "\nWHERE").replace(/ORDER BY/ig, "\nORDER BY").replace(/GROUP BY/ig, "\nGROUP BY").replace(/HAVING/ig, "\nHAVING").replace(/JOIN/ig, "\nJOIN").replace(/INSERT/ig, "\nINSERT").replace(/UPDATE/ig, "\nUPDATE").replace(/DELETE/ig, "\nDELETE").replace(/CREATE/ig, "\nCREATE").replace(/DROP/ig, "\nDROP").replace(/ALTER/ig, "\nALTER").replace(/;/g, ";\n"); },

        processSplitCodeSections(content, codePatterns) {
            const sections = content.split('---');
            return sections.map(section => {
                let sectionLanguage = null;
                let highestMatches = 0;

                Object.entries(codePatterns).forEach(([lang, patterns]) => {
                    const matches = patterns.filter(pattern => pattern.test(section)).length;
                    if (matches > highestMatches) {
                        highestMatches = matches;
                        sectionLanguage = lang;
                    }
                });

                if (highestMatches < 2) return section;

                const titleMatch = section.match(/#{1,6}\s*(.+?)(?:\n|$)/);
                const title = titleMatch ? titleMatch[0] : '';
                let code = titleMatch ? section.replace(titleMatch[0], '') : section;

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

        formatTime(time) { return parseTime(time, '{h}:{i}'); },
        scrollToBottom() { const container = this.$refs.messagesContainer; if (container) container.scrollTop = container.scrollHeight; },
        getCache(key) { const userInfo = this.$store.getters.userInfo; return userInfo && userInfo.avatar ? process.env.VUE_APP_BASE_API + userInfo.avatar : ''; },

        formatDate(dateString) {
            if (!dateString) return '';
            
            // 尝试创建日期对象
            let date;
            try {
                // 处理字符串日期格式 "YYYY-MM-DD HH:mm:ss"
                date = new Date(dateString.replace(/-/g, '/'));
                
                // 检查日期是否有效
                if (isNaN(date.getTime())) {
                    console.error('无效的日期:', dateString);
                    return dateString; // 如果无法解析，则返回原始字符串
                }
            } catch (e) {
                console.error('日期解析错误:', e);
                return dateString; // 出错时返回原始字符串
            }
            
            const now = new Date();
            const diff = now - date;

            if (diff < 24 * 60 * 60 * 1000 && date.getDate() === now.getDate() && date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear()) {
                return parseTime(date, '{h}:{i}');
            }
            if (diff < 7 * 24 * 60 * 60 * 1000) {
                const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
                return days[date.getDay()];
            }
            return parseTime(date, '{y}-{m}-{d}');
        },

        addCopyButtonsToCodeBlocks() {
            this.injectCodeBlockStyles();
            setTimeout(() => {
                const preElements = document.querySelectorAll('.message-content pre');
                preElements.forEach((pre) => {
                    if (pre.classList.contains('vs-code-processed')) return;
                    pre.classList.add('vs-code-processed');
                    const codeElement = pre.querySelector('code');
                    if (!codeElement) return;
                    this.buildVSCodeStyleBlock(pre, codeElement);
                });
            }, 100);
        },

        buildVSCodeStyleBlock(pre, codeElement) {
            this.injectCodeBlockStyles();
            const code = codeElement.textContent;
            let language = '';
            if (codeElement.className) {
                const match = codeElement.className.match(/language-(\w+)/);
                if (match) language = match[1];
            }
            const highlightedHtml = codeElement.innerHTML;
            const lineCount = code.split('\n').length;

            const container = document.createElement('div');
            container.className = 'vs-code-container';
            if (language) {
                container.classList.add(`language-${language}`);
                container.setAttribute('data-language', language);
            }
            container.setAttribute('data-original-code', code);

            const header = document.createElement('div');
            header.className = 'vs-code-header';
            const langLabel = document.createElement('div');
            langLabel.className = 'vs-code-lang';
            langLabel.textContent = language ? language.toUpperCase() : 'TEXT';
            header.appendChild(langLabel);
            header.innerHTML += `<div class="vs-code-toolbar"><button class="vs-code-btn vs-code-copy-btn" title="复制代码"><i class="el-icon-document-copy"></i></button><button class="vs-code-btn vs-code-download-btn" title="下载代码"><i class="el-icon-download"></i></button></div>`;
            container.appendChild(header);

            const content = document.createElement('div');
            content.className = 'vs-code-content';
            const lineNumbers = document.createElement('div');
            lineNumbers.className = 'vs-code-line-numbers';
            for (let i = 1; i <= lineCount; i++) {
                const lineNumber = document.createElement('div');
                lineNumber.className = 'vs-code-line-number';
                lineNumber.textContent = i;
                lineNumbers.appendChild(lineNumber);
            }
            content.appendChild(lineNumbers);

            const textArea = document.createElement('div');
            textArea.className = 'vs-code-text';
            const lines = this.splitHighlightedCode(highlightedHtml, lineCount);
            lines.forEach(line => {
                const codeLine = document.createElement('div');
                codeLine.className = 'vs-code-line';
                codeLine.innerHTML = line || ' ';
                textArea.appendChild(codeLine);
            });
            content.appendChild(textArea);
            container.appendChild(content);

            const cornerCopyBtn = document.createElement('button');
            cornerCopyBtn.className = 'vs-code-corner-copy-btn';
            cornerCopyBtn.innerHTML = '<i class="el-icon-document-copy"></i> 复制代码';
            cornerCopyBtn.title = '复制代码';
            container.appendChild(cornerCopyBtn);

            pre.parentNode.replaceChild(container, pre);
        },

        splitHighlightedCode(html, lineCount) {
            if (!html.trim()) return Array(lineCount).fill('');
            let lines = html.split('\n');
            if (lines.length < lineCount) {
                lines = lines.concat(Array(lineCount - lines.length).fill(''));
            } else if (lines.length > lineCount) {
                lines = lines.slice(0, lineCount);
            }
            return lines;
        },

        copyCodeToClipboard(code, container) {
            if (!code || typeof code !== 'string') {
                this.$message.error('没有可复制的内容');
                return;
            }
            try {
                if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(code.trim())
                        .then(() => {
                            container.classList.add('vs-code-copied');
                            setTimeout(() => {
                                container.classList.remove('vs-code-copied');
                            }, 1000);
                            this.$message.success('代码已复制到剪贴板');
                        })
                        .catch(err => {
                            console.error('使用Clipboard API复制失败:', err);
                            this.fallbackCopy(code, container);
                        });
                } else {
                    this.fallbackCopy(code, container);
                }
            } catch (error) {
                console.error('复制失败:', error);
                this.$message.error('复制失败: ' + error.message);
            }
        },

        fallbackCopy(code, container) {
            const textarea = document.createElement('textarea');
            textarea.value = code.trim();
            textarea.style.position = 'fixed';
            textarea.style.opacity = '0';
            textarea.style.top = '0';
            textarea.style.left = '0';
            document.body.appendChild(textarea);
            try {
                textarea.focus();
                textarea.select();
                const successful = document.execCommand('copy');
                if (successful) {
                    container.classList.add('vs-code-copied');
                    setTimeout(() => {
                        container.classList.remove('vs-code-copied');
                    }, 1000);
                    this.$message.success('代码已复制到剪贴板');
                } else {
                    this.$message.error('复制失败，请手动复制');
                }
            } catch (err) {
                console.error('execCommand复制失败:', err);
                this.$message.error('复制失败，请手动复制');
            } finally {
                document.body.removeChild(textarea);
            }
        },

        downloadCode(code, language) {
            if (!code || typeof code !== 'string') {
                this.$message.error('没有可下载的内容');
                return;
            }
            try {
                const extensions = { java: '.java', javascript: '.js', js: '.js', typescript: '.ts', ts: '.ts', html: '.html', css: '.css', python: '.py', py: '.py', ruby: '.rb', php: '.php', go: '.go', rust: '.rs', c: '.c', cpp: '.cpp', 'c++': '.cpp', csharp: '.cs', 'c#': '.cs', swift: '.swift', kotlin: '.kt', scala: '.scala', perl: '.pl', bash: '.sh', shell: '.sh', sql: '.sql', json: '.json', xml: '.xml', yaml: '.yml', markdown: '.md', md: '.md' };
                const ext = extensions[language ? language.toLowerCase() : ''] || '.txt';
                const blob = new Blob([code.trim()], { type: 'text/plain' });
                const a = document.createElement('a');
                const url = URL.createObjectURL(blob);
                a.href = url;
                a.download = `code-snippet${ext}`;
                document.body.appendChild(a);
                a.click();
                setTimeout(() => {
                    document.body.removeChild(a);
                    URL.revokeObjectURL(url);
                    this.$message.success(`代码已下载为 code-snippet${ext}`);
                }, 100);
            } catch (error) {
                console.error('下载失败:', error);
                this.$message.error('下载失败: ' + error.message);
            }
        },

        injectCodeBlockStyles() {
            if (document.getElementById('vs-code-styles')) return;
            const styleElement = document.createElement('style');
            styleElement.id = 'vs-code-styles';
            const css = `
        .message-content pre { padding: 0 !important; margin: 15px 0 !important; overflow: hidden !important; background: none !important; border: none !important; box-shadow: none !important; }
        .vs-code-container[data-language="java"] .hljs-keyword { color: #569cd6 !important; font-weight: bold !important; }
        .vs-code-container[data-language="java"] .hljs-class { color: #4ec9b0 !important; }
        .vs-code-container[data-language="java"] .hljs-string { color: #ce9178 !important; }
        .vs-code-container[data-language="java"] .hljs-comment { color: #6a9955 !important; font-style: italic !important; }
        .vs-code-container { background-color: #1e1e1e; border-radius: 6px; overflow: hidden; font-family: Consolas, Monaco, monospace; font-size: 14px; margin: 0; box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2); border: 1px solid #333; position: relative; }
        .vs-code-header { background-color: #2d2d2d; color: #cccccc; display: flex; justify-content: space-between; align-items: center; padding: 5px 10px; border-bottom: 1px solid #3e3e3e; height: 30px; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
        .vs-code-lang { font-size: 12px; color: #cccccc; opacity: 0.8; }
        .vs-code-toolbar { display: flex; }
        .vs-code-btn { background: transparent; border: none; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; color: #cccccc; opacity: 0.7; border-radius: 4px; cursor: pointer; transition: all 0.2s; margin-left: 4px; font-size: 14px; }
        .vs-code-btn:hover { opacity: 1; background-color: #3e3e3e; }
        .vs-code-corner-copy-btn { position: absolute; top: 45px; right: 15px; z-index: 100; background-color: rgba(0, 122, 204, 0.8); color: #fff; border: none; border-radius: 4px; padding: 6px 12px; font-size: 13px; cursor: pointer; display: flex; align-items: center; gap: 5px; opacity: 0; transition: all 0.2s ease; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; box-shadow: 0 2px 5px rgba(0, 0, 0, 0.3); }
        .vs-code-container:hover .vs-code-corner-copy-btn { opacity: 1; }
        .vs-code-corner-copy-btn:hover { background-color: rgba(0, 122, 204, 1); transform: translateY(-1px); box-shadow: 0 3px 8px rgba(0, 0, 0, 0.4); }
        .vs-code-content { display: flex; padding: 0; overflow-x: auto; background-color: #1e1e1e; }
        .vs-code-line-numbers { padding: 12px 8px; text-align: right; min-width: 40px; border-right: 1px solid #404040; color: #858585; font-size: 12px; line-height: 1.5; user-select: none; background-color: #1e1e1e; }
        .vs-code-line-number { height: 20px; font-family: Consolas, Monaco, monospace; }
        .vs-code-text { flex: 1; padding: 12px 16px; color: #d4d4d4; overflow-x: auto; white-space: pre; font-family: Consolas, Monaco, monospace; font-size: 14px; line-height: 1.5; background-color: #1e1e1e; }
        .vs-code-line { height: 20px; white-space: pre; font-family: Consolas, Monaco, monospace; }
        .vs-code-copied { animation: vs-code-copy-success 1s ease; }
        @keyframes vs-code-copy-success { 0% { box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2); } 50% { box-shadow: 0 0 0 4px rgba(76, 175, 80, 0.4); } 100% { box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2); } }
        /* VS Code dark theme syntax highlighting */
        .vs-code-text .hljs-keyword { color: #569cd6; font-weight: bold; } .vs-code-text .hljs-built_in, .vs-code-text .hljs-type { color: #4ec9b0; } .vs-code-text .hljs-literal { color: #569cd6; } .vs-code-text .hljs-number { color: #b5cea8; } .vs-code-text .hljs-regexp { color: #d16969; } .vs-code-text .hljs-string { color: #ce9178; } .vs-code-text .hljs-subst, .vs-code-text .hljs-symbol, .vs-code-text .hljs-class, .vs-code-text .hljs-function, .vs-code-text .hljs-title { color: #dcdcaa; } .vs-code-text .hljs-params { color: #9cdcfe; } .vs-code-text .hljs-comment { color: #6a9955; font-style: italic; } .vs-code-text .hljs-doctag { color: #608b4e; } .vs-code-text .hljs-meta { color: #9cdcfe; } .vs-code-text .hljs-section { color: gold; } .vs-code-text .hljs-name, .vs-code-text .hljs-tag { color: #569cd6; } .vs-code-text .hljs-attr { color: #9cdcfe; }
        `;
            styleElement.textContent = css;
            document.head.appendChild(styleElement);
        },

        // 获取团队列表
        fetchTeamList() {
            listMy_team().then(response => {
                if (response.code === 200) {
                    if (response.rows && response.rows.length > 0) {
                        this.teamList = response.rows;
                        const lastSelectedTeamId = localStorage.getItem('ai-chat-last-selected-team');
                        if (lastSelectedTeamId === '') {
                            this.selectedTeamId = '';
                        } else if (lastSelectedTeamId && this.teamList.some(team => String(team.teamId) === String(lastSelectedTeamId))) {
                            this.selectedTeamId = String(lastSelectedTeamId);
                        } else {
                            this.selectedTeamId = String(this.teamList[0].teamId);
                            localStorage.setItem('ai-chat-last-selected-team', this.selectedTeamId);
                        }
                    } else {
                        this.teamList = [];
                        this.selectedTeamId = '';
                        localStorage.setItem('ai-chat-last-selected-team', '');
                        this.$message.warning('未找到任何团队，已切换到不使用知识库模式');
                    }
                } else {
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

        // 处理团队选择变化
        handleTeamChange(newTeamId) {
            if (newTeamId) {
                const selectedTeam = this.teamList.find(team => String(team.teamId) === newTeamId);
                if (selectedTeam) {
                    this.$message.info(`知识库已切换到: ${selectedTeam.name}`);
                }
            } else {
                this.$message.info('已切换到不使用知识库模式');
            }
            localStorage.setItem('ai-chat-last-selected-team', newTeamId || '');
        },

        // 获取头像URL
        getAvatarUrl(avatar) {
            if (!avatar) return '';
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