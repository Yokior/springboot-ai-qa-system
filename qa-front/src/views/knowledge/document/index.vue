<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="header-title">团队知识库 - 文档管理</span>
          <el-select v-model="selectedTeamId" placeholder="请选择团队" @change="handleTeamChange" size="small" style="margin-left: 10px; margin-right: 10px;">
            <el-option
              v-for="item in teamList"
              :key="item.teamId"
              :label="item.name"
              :value="item.teamId">
            </el-option>
          </el-select>
          <el-button type="primary" @click="handleUpload" v-hasPerms="['knowledge:document:upload']" size="small" icon="el-icon-upload">
            上传文档
          </el-button>
        </div>
      </template>
      
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
    
    <el-dialog v-model="uploadVisible" title="上传文档" width="500px">
      <upload-document :team-id="selectedTeamId" @close="uploadVisible = false" @success="handleUploadSuccess" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import DocumentList from './components/DocumentList.vue';
import UploadDocument from './components/UploadDocument.vue';
import { listMy_team } from '@/api/team/my_team';
import Vue from 'vue';

const selectedTeamId = ref('');
const teamList = ref([]);
const activeName = ref('all');
const uploadVisible = ref(false);

// 获取当前用户所在的团队列表
const getTeamList = async () => {
  try {
    const res = await listMy_team();
    teamList.value = res.data;
    if (teamList.value.length > 0) {
      selectedTeamId.value = teamList.value[0].teamId;
    }
  } catch (error) {
    console.error('获取团队列表失败:', error);
    Vue.prototype.$message.error('获取团队列表失败');
  }
};

const handleTeamChange = () => {
  // 切换团队时重新加载文档列表
  // DocumentList组件会监听teamId的变化自动刷新
};

const handleUpload = () => {
  if (!selectedTeamId.value) {
    Vue.prototype.$message.warning('请先选择一个团队');
    return;
  }
  uploadVisible.value = true;
};

const handleUploadSuccess = () => {
  uploadVisible.value = false;
  Vue.prototype.$message.success('文档上传成功，开始处理');
  // 可以选择刷新当前文档列表
};

onMounted(() => {
  getTeamList();
});
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

/* Add some spacing for the select and button */
.el-select {
  margin-left: 10px;
  margin-right: 10px;
}
</style>
