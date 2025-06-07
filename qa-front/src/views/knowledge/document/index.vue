<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="header-title">团队知识库 - 文档管理</span>
          <el-button 
            v-if="hasUploadPermission" 
            type="primary" 
            @click="handleUpload" 
            size="small" 
            icon="el-icon-upload">
            上传文档
          </el-button>
        </div>
      </template>
      
      <!-- 团队选择区域 -->
      <div class="team-selection-area">
        <div class="section-title">选择团队</div>
        <div class="team-cards-container">
          <el-scrollbar class="team-scrollbar">
            <div class="team-cards">
              <div 
                v-for="team in teamList" 
                :key="team.teamId" 
                class="team-card" 
                :class="{ 'active': selectedTeamId === team.teamId }"
                @click="selectTeam(team.teamId)"
              >
                <div class="team-avatar">
                  <el-avatar 
                    :size="40" 
                    :src="getAvatarUrl(team.avatar)" 
                    @error="() => avatarError(team)"
                  >
                    {{ team.name ? team.name.substring(0, 1).toUpperCase() : 'T' }}
                  </el-avatar>
                </div>
                <div class="team-info">
                  <div class="team-name">{{ team.name }}</div>
                  <div class="team-role">{{ formatRole(team.role) }}</div>
                </div>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>
      
      <el-tabs v-model="activeName" @tab-click="handleTabClick">
        <el-tab-pane label="全部文档" name="all">
          <document-list :team-id="selectedTeamId" :status="null" ref="allDocList" />
        </el-tab-pane>
        <el-tab-pane label="处理中" name="processing">
          <document-list :team-id="selectedTeamId" :status="currentProcessingStatus" ref="processingDocList" />
        </el-tab-pane>
        <el-tab-pane label="已完成" name="completed">
          <document-list :team-id="selectedTeamId" :status="'COMPLETED'" ref="completedDocList" />
        </el-tab-pane>
        <el-tab-pane label="处理失败" name="failed">
          <document-list :team-id="selectedTeamId" :status="'FAILED'" ref="failedDocList" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <el-dialog :visible.sync="uploadVisible" title="上传文档" width="500px">
      <upload-document :team-id="selectedTeamId" @close="uploadVisible = false" @success="handleUploadSuccess" />
    </el-dialog>
  </div>
</template>

<script>
import DocumentList from './components/DocumentList.vue';
import UploadDocument from './components/UploadDocument.vue';
import { listMy_team } from '@/api/team/my_team';

export default {
  name: "DocumentManagement",
  components: {
    DocumentList,
    UploadDocument
  },
  data() {
    return {
      selectedTeamId: '',
      teamList: [],
      activeName: 'all',
      uploadVisible: false,
      currentTeam: null,
      // 当前处理中的状态，默认为PENDING
      currentProcessingStatus: 'PENDING',
      // 处理中的状态列表
      processingStatusList: ['PENDING', 'PROCESSING_TEXT', 'PROCESSING_NLP'],
      // 当前处理中状态的索引
      currentStatusIndex: 0,
      // 添加自动刷新定时器
      refreshTimer: null,
      // 是否刚上传过文档
      justUploaded: false
    };
  },
  computed: {
    // 判断当前用户是否有上传权限（创建者或管理员）
    hasUploadPermission() {
      if (!this.selectedTeamId || !this.teamList.length) return false;
      
      const currentTeam = this.teamList.find(team => team.teamId === this.selectedTeamId);
      if (!currentTeam) return false;
      
      return ['creator', 'admin'].includes(currentTeam.role);
    },
    
    // 获取当前活动的文档列表组件
    activeDocumentList() {
      const refMap = {
        'all': 'allDocList',
        'processing': 'processingDocList',
        'completed': 'completedDocList',
        'failed': 'failedDocList'
      };
      
      const refName = refMap[this.activeName];
      return this.$refs[refName];
    }
  },
  created() {
    console.log('文档管理页面已加载');
    this.getTeamList();
  },
  mounted() {
    // 添加页面可见性变化监听器
    document.addEventListener('visibilitychange', this.handleVisibilityChange);
  },
  beforeDestroy() {
    // 清除定时器和事件监听器
    this.clearRefreshTimer();
    document.removeEventListener('visibilitychange', this.handleVisibilityChange);
  },
  methods: {
    // 处理页面可见性变化
    handleVisibilityChange() {
      if (document.visibilityState === 'visible') {
        // 页面变为可见时，刷新当前列表
        this.refreshCurrentList();
      }
    },
    
    // 设置自动刷新定时器
    setupRefreshTimer() {
      // 清除现有定时器
      this.clearRefreshTimer();
      
      // 如果当前是"处理中"标签页，设置定时刷新
      if (this.activeName === 'processing') {
        this.refreshTimer = setInterval(() => {
          // 轮询不同的处理中状态
          this.rotateProcessingStatus();
          this.refreshCurrentList();
        }, 5000); // 每5秒刷新一次
      }
    },
    
    // 轮询不同的处理中状态
    rotateProcessingStatus() {
      this.currentStatusIndex = (this.currentStatusIndex + 1) % this.processingStatusList.length;
      this.currentProcessingStatus = this.processingStatusList[this.currentStatusIndex];
      console.log('切换到处理状态:', this.currentProcessingStatus);
    },
    
    // 清除刷新定时器
    clearRefreshTimer() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    },
    
    // 处理标签页点击
    handleTabClick() {
      // 如果切换到处理中标签页，重置状态索引
      if (this.activeName === 'processing') {
        this.currentStatusIndex = 0;
        this.currentProcessingStatus = this.processingStatusList[0];
      }
      
      // 切换标签页时刷新对应列表
      this.refreshCurrentList();
      
      // 设置自动刷新（仅对"处理中"标签页）
      this.setupRefreshTimer();
    },
    
    // 刷新当前显示的列表
    refreshCurrentList() {
      if (this.activeDocumentList) {
        this.activeDocumentList.getList();
      }
    },
    
    // 获取当前用户所在的团队列表
    async getTeamList() {
      try {
        const res = await listMy_team();
        if (res && res.rows) {
          this.teamList = res.rows;
          if (this.teamList.length > 0) {
            this.selectedTeamId = this.teamList[0].teamId;
          } else {
            this.$message.warning('您还没有加入任何团队，请先创建或加入团队');
          }
        } else {
          this.$message.error('获取团队列表失败: 返回数据格式错误');
        }
      } catch (error) {
        console.error('获取团队列表失败:', error);
        this.$message.error('获取团队列表失败: ' + (error.message || '未知错误'));
      }
    },
    
    // 获取头像URL
    getAvatarUrl(avatar) {
      if (!avatar) return '';
      // 如果头像路径是相对路径，则加上基础URL
      if (avatar.startsWith('/')) {
        return process.env.VUE_APP_BASE_API + avatar;
      }
      return avatar;
    },
    
    // 头像加载失败处理
    avatarError(team) {
      console.error('头像加载失败:', team.avatar);
    },
    
    // 格式化角色显示
    formatRole(role) {
      const roleMap = {
        'creator': '创建者',
        'admin': '管理员',
        'member': '普通成员'
      };
      return roleMap[role] || role;
    },
    
    // 选择团队
    selectTeam(teamId) {
      this.selectedTeamId = teamId;
      console.log('切换到团队:', this.selectedTeamId);
      // 团队切换后刷新当前列表
      this.refreshCurrentList();
    },
    
    handleUpload() {
      if (!this.selectedTeamId) {
        this.$message.warning('请先选择一个团队');
        return;
      }
      
      // 检查权限
      if (!this.hasUploadPermission) {
        this.$message.warning('您没有上传文档的权限，只有团队创建者和管理员可以上传文档');
        return;
      }
      
      this.uploadVisible = true;
    },
    
    // 处理上传成功
    handleUploadSuccess() {
      this.uploadVisible = false;
      this.justUploaded = true;
      
      // 切换到"处理中"标签页，并设置状态为PROCESSING_TEXT
      this.activeName = 'processing';
      this.currentProcessingStatus = 'PROCESSING_TEXT';
      this.currentStatusIndex = this.processingStatusList.indexOf('PROCESSING_TEXT');
      
      // 先等待DOM更新
      this.$nextTick(() => {
        // 立即刷新处理中的文档列表
        if (this.$refs.processingDocList) {
          this.$refs.processingDocList.getList();
        }
        
        // 启动自动刷新定时器
        this.setupRefreshTimer();
        
        // 显示提示信息
        this.$message({
          message: '文档已上传，正在处理中，页面将自动刷新显示最新状态',
          type: 'success',
          duration: 5000
        });
      });
    },
    
    // 刷新文档列表
    refreshDocumentList() {
      // 刷新所有文档列表
      if (this.$refs.allDocList) {
        this.$refs.allDocList.getList();
      }
      
      if (this.$refs.processingDocList) {
        this.$refs.processingDocList.getList();
      }
      
      if (this.$refs.completedDocList) {
        this.$refs.completedDocList.getList();
      }
      
      if (this.$refs.failedDocList) {
        this.$refs.failedDocList.getList();
      }
    }
  }
};
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  font-weight: bold;
  font-size: 16px;
}

.team-selection-area {
  margin-bottom: 20px;
  position: relative;
}

.section-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 10px;
  font-weight: 500;
  position: relative;
  z-index: 1;
  background-color: #fff;
}

.team-scrollbar {
  height: 120px;
  overflow: visible;
}

.team-cards-container {
  width: 100%;
  overflow: visible;
  position: relative;
  z-index: 2;
}

.team-cards {
  display: flex;
  flex-wrap: nowrap;
  gap: 15px;
  padding-bottom: 10px;
  padding-top: 5px;
}

.team-card {
  min-width: 180px;
  height: 80px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  padding: 12px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #f5f7fa;
  position: relative;
  z-index: 3;
}

.team-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px 0 rgba(0, 0, 0, 0.1);
  z-index: 4;
}

.team-card.active {
  border-color: #409EFF;
  background-color: rgba(64, 158, 255, 0.1);
}

.team-avatar {
  margin-right: 12px;
}

.team-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.team-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.team-role {
  font-size: 12px;
  color: #909399;
}
</style>
