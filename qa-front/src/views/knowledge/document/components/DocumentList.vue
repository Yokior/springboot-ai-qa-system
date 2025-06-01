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
            @click="handleView(scope.row)">
            查看
          </el-button>
          <el-button
            link
            type="danger"
            size="small"
            @click="handleDelete(scope.row)">
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

<script>
import { listDocuments, deleteDocument } from '@/api/knowledge/document';

export default {
  name: "DocumentList",
  props: {
    teamId: {
      type: [String, Number],
      required: true
    },
    status: {
      type: [String, Array, null],
      default: null
    }
  },
  data() {
    return {
      loading: false,
      documentList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        teamId: undefined,
        processingStatus: undefined
      }
    };
  },
  watch: {
    teamId: {
      handler(val) {
        if (val) {
          this.queryParams.teamId = val;
          this.queryParams.pageNum = 1;
          this.getList();
        }
      },
      immediate: true
    },
    status: {
      handler(val) {
        if (Array.isArray(val)) {
          this.queryParams.processingStatus = val.join(',');
        } else {
          this.queryParams.processingStatus = val;
        }
        this.queryParams.pageNum = 1;
        this.getList();
      },
      immediate: true
    }
  },
  mounted() {
    console.log('DocumentList组件已加载, teamId:', this.teamId);
    if (this.teamId) {
      this.getList();
    }
  },
  methods: {
    // 获取文档列表
    async getList() {
      if (!this.queryParams.teamId) {
        console.log('未设置团队ID，不加载文档列表');
        return;
      }
      
      console.log('加载文档列表，参数:', JSON.stringify(this.queryParams));
      this.loading = true;
      try {
        const res = await listDocuments(this.queryParams);
        console.log('文档列表返回结果:', res);
        if (res && res.rows) {
          this.documentList = res.rows;
          this.total = res.total;
        } else {
          this.documentList = [];
          this.total = 0;
          console.error('获取文档列表返回数据格式错误:', res);
        }
      } catch (error) {
        console.error('获取文档列表失败:', error);
        this.$message.error('获取文档列表失败: ' + (error.message || '未知错误'));
        this.documentList = [];
        this.total = 0;
      } finally {
        this.loading = false;
      }
    },

    // 格式化文件大小
    formatFileSize(size) {
      if (!size) return '0 B';
      
      const units = ['B', 'KB', 'MB', 'GB', 'TB'];
      let index = 0;
      let fileSize = size;
      
      while (fileSize >= 1024 && index < units.length - 1) {
        fileSize /= 1024;
        index++;
      }
      
      return `${fileSize.toFixed(2)} ${units[index]}`;
    },

    // 获取文件类型标签样式
    getFileTypeTag(fileType) {
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
    },

    // 获取状态标签样式
    getStatusTag(status) {
      if (!status) return '';
      
      const statusMap = {
        'PENDING': 'info',
        'PROCESSING_TEXT': 'warning',
        'PROCESSING_NLP': 'warning',
        'COMPLETED': 'success',
        'FAILED': 'danger'
      };
      
      return statusMap[status] || '';
    },

    // 格式化状态显示
    formatStatus(status) {
      if (!status) return '';
      
      const statusMap = {
        'PENDING': '待处理',
        'PROCESSING_TEXT': '解析中',
        'PROCESSING_NLP': 'NLP处理中',
        'COMPLETED': '已完成',
        'FAILED': '处理失败'
      };
      
      return statusMap[status] || status;
    },

    // 查看文档详情
    handleView(row) {
      this.$message.info('查看文档详情功能待实现');
    },

    // 删除文档
    handleDelete(row) {
      this.$confirm(`确认删除文档 "${row.filename}"?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteDocument(row.docId);
          this.$message.success('删除成功');
          this.getList();
        } catch (error) {
          console.error('删除文档失败:', error);
          this.$message.error('删除失败: ' + (error.message || '未知错误'));
        }
      }).catch(() => {});
    },

    // 分页处理
    handleSizeChange(val) {
      this.queryParams.pageSize = val;
      this.getList();
    },

    handleCurrentChange(val) {
      this.queryParams.pageNum = val;
      this.getList();
    }
  }
};
</script>

<style scoped>
.file-name {
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>
