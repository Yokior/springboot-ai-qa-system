<template>
  <div class="document-list">
    <el-table
      v-loading="loading"
      :data="documentList"
      style="width: 100%">
      <el-table-column prop="filename" label="文件名" show-overflow-tooltip>
        <template #default="scope">
          <el-tooltip :content="scope.row.filename" placement="top">
            <div class="file-name">
              <i class="el-icon-document"></i>
              {{ scope.row.filename }}
            </div>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="fileType" label="文件类型" width="120">
        <template #default="scope">
          <el-tag
            :type="getFileTypeTag(scope.row.fileType)"
            size="small">
            {{ scope.row.fileType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="文件大小" width="120">
        <template #default="scope">
          {{ formatFileSize(scope.row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="processingStatus" label="处理状态" width="120">
        <template #default="scope">
          <el-tag
            :type="getStatusTag(scope.row.processingStatus)"
            size="small">
            {{ formatStatus(scope.row.processingStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uploadTime" label="上传时间" width="180" />
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button
            link
            type="primary"
            size="small"
            @click="handleView(scope.row)"
            v-hasPerms="['knowledge:document:query']">
            查看
          </el-button>
          <el-button
            link
            type="danger"
            size="small"
            @click="handleDelete(scope.row)"
            v-hasPerms="['knowledge:document:remove']">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      v-if="total > 0"
      :current-page="queryParams.pageNum"
      :page-size="queryParams.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, defineProps } from 'vue';
import Vue from 'vue';
import { listDocuments, deleteDocument } from '@/api/knowledge/document';

const props = defineProps({
  teamId: {
    type: [String, Number],
    required: true
  },
  status: {
    type: [String, Array, null],
    default: null
  }
});

const loading = ref(false);
const documentList = ref([]);
const total = ref(0);
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  teamId: undefined,
  processingStatus: undefined
});

// 监听团队ID的变化
watch(() => props.teamId, (val) => {
  if (val) {
    queryParams.value.teamId = val;
    queryParams.value.pageNum = 1;
    getList();
  }
}, { immediate: true });

// 监听状态的变化
watch(() => props.status, (val) => {
  queryParams.value.processingStatus = val;
  queryParams.value.pageNum = 1;
  getList();
}, { immediate: true });

// 获取文档列表
const getList = async () => {
  if (!queryParams.value.teamId) return;
  
  loading.value = true;
  try {
    const res = await listDocuments(queryParams.value);
    documentList.value = res.rows;
    total.value = res.total;
  } catch (error) {
    console.error('获取文档列表失败:', error);
    Vue.prototype.$message.error('获取文档列表失败');
  } finally {
    loading.value = false;
  }
};

// 格式化文件大小
const formatFileSize = (size) => {
  if (!size) return '0 B';
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let index = 0;
  let fileSize = size;
  
  while (fileSize >= 1024 && index < units.length - 1) {
    fileSize /= 1024;
    index++;
  }
  
  return `${fileSize.toFixed(2)} ${units[index]}`;
};

// 获取文件类型标签样式
const getFileTypeTag = (fileType) => {
  if (!fileType) return '';
  
  const typeMap = {
    'pdf': 'danger',
    'docx': 'primary',
    'doc': 'primary',
    'txt': 'info',
    'xlsx': 'success',
    'xls': 'success',
    'pptx': 'warning',
    'ppt': 'warning'
  };
  
  return typeMap[fileType.toLowerCase()] || '';
};

// 获取状态标签样式
const getStatusTag = (status) => {
  if (!status) return '';
  
  const statusMap = {
    'PENDING': 'info',
    'PROCESSING_TEXT': 'warning',
    'PROCESSING_NLP': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger'
  };
  
  return statusMap[status] || '';
};

// 格式化状态显示
const formatStatus = (status) => {
  if (!status) return '';
  
  const statusMap = {
    'PENDING': '待处理',
    'PROCESSING_TEXT': '解析中',
    'PROCESSING_NLP': 'NLP处理中',
    'COMPLETED': '已完成',
    'FAILED': '处理失败'
  };
  
  return statusMap[status] || status;
};

// 查看文档详情
const handleView = (row) => {
  Vue.prototype.$message.info('查看文档详情功能待实现');
};

// 删除文档
const handleDelete = (row) => {
  Vue.prototype.$confirm(`确认删除文档 "${row.filename}"?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDocument(row.docId);
      Vue.prototype.$message.success('删除成功');
      getList();
    } catch (error) {
      console.error('删除文档失败:', error);
      Vue.prototype.$message.error('删除失败');
    }
  }).catch(() => {});
};

// 分页处理
const handleSizeChange = (val) => {
  queryParams.value.pageSize = val;
  getList();
};

const handleCurrentChange = (val) => {
  queryParams.value.pageNum = val;
  getList();
};

onMounted(() => {
  if (props.teamId) {
    getList();
  }
});
</script>

<style scoped>
.file-name {
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>
