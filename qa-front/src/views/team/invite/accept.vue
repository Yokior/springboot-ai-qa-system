<template>
  <div class="invite-accept-container">
    <el-card v-if="!loading && inviteInfo" class="invite-card">
      <div slot="header" class="header">
        <span class="title">团队邀请</span>
      </div>
      <div class="invite-content">
        <div class="team-avatar">
          <el-avatar :size="80" :src="getImageUrl(inviteInfo.teamAvatar)"></el-avatar>
        </div>
        <h2 class="team-name">{{ inviteInfo.teamName }}</h2>
        <div class="invite-info">
          <p><span class="label">邀请人:</span> {{ inviteInfo.inviterName }}</p>
          <p><span class="label">创建者:</span> {{ inviteInfo.ownerName }}</p>
          <p><span class="label">团队描述:</span> {{ inviteInfo.teamDescription || '暂无描述' }}</p>
          <p>
            <span class="label">邀请有效期至:</span>
            <span :class="{'expired': isExpired}">{{ formatTime(inviteInfo.expireTime) }}</span>
          </p>
        </div>
        <div class="actions">
          <el-button v-if="!isExpired" type="primary" @click="handleAcceptInvite">接受邀请</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </div>
      </div>
    </el-card>
    <el-empty v-else-if="!loading && !inviteInfo" description="无效的邀请链接或已过期" :image-size="200">
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </el-empty>
    <div v-else class="loading">
      <el-skeleton style="width: 500px" animated>
        <template slot="template">
          <el-skeleton-item variant="image" style="width: 100%; height: 160px" />
          <div style="padding: 14px;">
            <el-skeleton-item variant="h3" style="width: 50%" />
            <div style="margin-top: 16px">
              <el-skeleton-item variant="text" style="width: 100%" />
              <el-skeleton-item variant="text" style="width: 100%" />
              <el-skeleton-item variant="text" style="width: 60%" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script>
import { getTeamInviteInfo, acceptTeamInvite } from "@/api/team/my_team";

export default {
  data() {
    return {
      inviteCode: null,
      loading: true,
      inviteInfo: null,
      defaultAvatar: require('@/assets/images/profile.jpg')
    };
  },
  computed: {
    isExpired() {
      if (!this.inviteInfo || !this.inviteInfo.expireTime) return true;
      return new Date(this.inviteInfo.expireTime) < new Date();
    }
  },
  created() {
    this.inviteCode = this.$route.params.code;
    if (this.inviteCode) {
      this.getInviteInfo();
    } else {
      this.loading = false;
    }
  },
  methods: {
    getInviteInfo() {
      getTeamInviteInfo(this.inviteCode).then(response => {
        if (response.code === 200 && response.data) {
          this.inviteInfo = response.data;
        }
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },
    handleAcceptInvite() {
      if (this.isExpired) {
        this.$modal.msgError("邀请已过期");
        return;
      }
      if (this.$store.getters.token) {
        this.acceptInvite();
      } else {
        sessionStorage.setItem('pendingInviteCode', this.inviteCode);
        this.$router.push(`/login?redirect=${encodeURIComponent(this.$route.fullPath)}`);
      }
    },
    acceptInvite() {
      acceptTeamInvite(this.inviteCode).then(response => {
        if (response.code === 200) {
          this.$modal.msgSuccess("成功加入团队");
          this.$router.push(`/team/my_team/${this.inviteInfo.teamId}`);
        } else {
          this.$modal.msgError(response.msg || "加入团队失败");
        }
      }).catch(error => {
        console.error("接受邀请出错:", error);
        this.$modal.msgError("接受邀请时发生错误");
      });
    },
    handleCancel() {
      this.goHome();
    },
    goHome() {
      this.$router.push('/');
    },
    getImageUrl(avatar) {
      if (!avatar) return this.defaultAvatar;
      if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
        return avatar;
      }
      if (avatar.startsWith('data:image/')) {
        return avatar;
      }
      return process.env.VUE_APP_BASE_API + avatar;
    },
    formatTime(time) {
      if (!time) return '未知';
      try {
        // 确保time是Date对象
        let date = time;
        if (!(time instanceof Date)) {
          // 如果是时间戳或字符串，转换为Date对象
          date = new Date(time);
        }
        
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          console.error('无效的日期:', time);
          return '无效的日期';
        }
        
        // 使用更明确的方式格式化日期
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        
        return `${year}-${month}-${day} ${hours}:${minutes}`;
      } catch (error) {
        console.error('格式化日期时出错:', error, time);
        return '日期格式错误';
      }
    }
  }
};
</script>

<style scoped lang="scss">
.invite-accept-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 120px);
  padding: 20px;
  background-color: #f0f2f5;
}

.invite-card {
  width: 100%;
  max-width: 500px;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.16);
  }
  
  .el-card__header {
    background: linear-gradient(135deg, #1a365d, #153254);
    padding: 15px 20px;
    border-bottom: none;
  }
}

.header {
  .title {
    color: white;
    font-size: 18px;
    font-weight: bold;
  }
}

.invite-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 20px;
}

.team-avatar {
  margin-bottom: 20px;
  
  .el-avatar {
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
    border: 3px solid white;
  }
}

.team-name {
  font-size: 24px;
  margin: 0 0 20px;
  color: #333;
  text-align: center;
}

.invite-info {
  width: 100%;
  margin-bottom: 25px;
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
  
  p {
    margin: 10px 0;
    color: #606266;
    font-size: 14px;
    line-height: 1.6;
    display: flex;
    flex-wrap: wrap;
  }
  
  .label {
    color: #909399;
    margin-right: 8px;
    min-width: 80px;
  }
  
  .expired {
    color: #F56C6C;
  }
}

.actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 10px;
  
  .el-button {
    min-width: 100px;
  }
}

.loading {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.el-empty {
  margin-top: 50px;
}

@media screen and (max-width: 576px) {
  .invite-card {
    max-width: 100%;
  }
  
  .invite-info {
    padding: 10px;
  }
  
  .team-name {
    font-size: 20px;
    margin-bottom: 15px;
  }
}
</style>
