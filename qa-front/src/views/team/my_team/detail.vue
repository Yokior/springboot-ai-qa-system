<template>
  <div class="app-container team-detail-container">
    <el-page-header @back="goBack" :content="pageTitle" class="page-header"></el-page-header>
    <el-card class="box-card content-card">
      <div slot="header" class="clearfix">
        <span>团队详情 (占位)</span>
      </div>
      <div v-if="teamId">
        <p>当前查看的团队 ID 是: <strong>{{ teamId }}</strong></p>
        <p>这里将来会显示团队的详细信息、成员列表、关联的知识库文档等。</p>
        <el-alert
          title="开发中"
          type="info"
          description="此页面为占位符，具体功能待后续开发。"
          show-icon
          :closable="false">
        </el-alert>
      </div>
      <div v-else>
        <p>无效的团队 ID。</p>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: "TeamDetail",
  data() {
    return {
      teamId: null,
      pageTitle: "团队详情"
    };
  },
  created() {
    // 从路由参数中获取 teamId
    this.teamId = this.$route.params.teamId;
    if (this.teamId) {
      this.pageTitle = `团队 #${this.teamId} 详情`;
      // 在这里可以添加获取团队详细信息的 API 调用
      // this.getTeamDetails();
    } else {
       this.pageTitle = "无效的团队";
    }
  },
  methods: {
    goBack() {
      // 返回上一页，通常是团队列表页
      this.$router.go(-1);
    },
    // getTeamDetails() {
    //   // 调用 API 获取团队详情
    //   console.log(`Fetching details for team ${this.teamId}`);
    // }
  }
};
</script>

<style scoped lang="scss">
.team-detail-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
  background-color: #fff;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.content-card {
  border-radius: 8px;

  p {
    line-height: 1.8;
    margin-bottom: 15px;
  }

  strong {
    color: #409EFF;
  }
}
</style> 