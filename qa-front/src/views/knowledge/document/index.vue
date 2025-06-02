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
      
      <el-tabs v-model="activeName">
        <el-tab-pane label="全部文档" name="all">
          <document-list :team-id="selectedTeamId" :status="null" />
        </el-tab-pane>
        <el-tab-pane label="处理中" name="processing">
          <document-list :team-id="selectedTeamId" :status="['PENDING', 'PROCESSING_TEXT', 'PROCESSING_NLP']" />
        </el-tab-pane>
        <el-tab-pane label="已完成" name="completed">
          <document-list :team-id="selectedTeamId" :status="['COMPLETED']" />
        </el-tab-pane>
        <el-tab-pane label="处理失败" name="failed">
          <document-list :team-id="selectedTeamId" :status="['FAILED']" />
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
      currentTeam: null
    };
  },
  computed: {
    // 判断当前用户是否有上传权限（创建者或管理员）
    hasUploadPermission() {
      if (!this.selectedTeamId || !this.teamList.length) return false;
      
      const currentTeam = this.teamList.find(team => team.teamId === this.selectedTeamId);
      if (!currentTeam) return false;
      
      return ['creator', 'admin'].includes(currentTeam.role);
    }
  },
  created() {
    console.log('文档管理页面已加载');
    this.getTeamList();
  },
  methods: {
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
    
    handleUploadSuccess() {
      this.uploadVisible = false;
      this.$message.success('文档上传成功，开始处理');
      // 可以选择刷新当前文档列表
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
