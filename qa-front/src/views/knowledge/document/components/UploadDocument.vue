<template>
  <div class="upload-container">
    <div class="upload-area" v-if="!selectedFile">
      <el-upload
        class="upload"
        drag
        action="#"
        :auto-upload="false"
        :http-request="uploadFile"
        :on-change="handleFileChange"
        :before-upload="beforeUpload"
        :multiple="false"
        :show-file-list="false"
        ref="upload">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">拖拽文件到此处或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            支持上传PDF、Word、TXT、MD文档，文件大小不超过20MB
          </div>
        </template>
      </el-upload>
    </div>
    
    <div class="selected-file" v-if="selectedFile">
      <div class="file-info">
        <i class="el-icon-document"></i>
        <span class="file-name">{{ selectedFile.name }}</span>
        <span class="file-size">({{ formatFileSize(selectedFile.size) }})</span>
      </div>
      <el-button type="text" icon="el-icon-delete" @click="removeFile">移除</el-button>
    </div>
    
    <div class="upload-actions">
      <el-button @click="handleClose">取消</el-button>
      <el-button 
        type="primary" 
        @click="handleSubmit" 
        :loading="uploading" 
        :disabled="!selectedFile">
        确认上传
      </el-button>
    </div>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth';
import { uploadDocument } from '@/api/knowledge/document';

export default {
  name: "UploadDocument",
  props: {
    teamId: {
      type: [String, Number],
      required: true
    }
  },
  data() {
    return {
      selectedFile: null,
      uploading: false
    };
  },
  computed: {
    // 获取认证头信息
    headers() {
      return {
        Authorization: 'Bearer ' + getToken()
      };
    }
  },
  methods: {
    // 处理文件选择变化
    handleFileChange(file) {
      this.selectedFile = file.raw;
    },
    
    // 移除已选择的文件
    removeFile() {
      this.selectedFile = null;
      // 重置上传组件
      this.$refs.upload.clearFiles();
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
    
    // 自定义上传方法
    async uploadFile() {
      if (!this.selectedFile) {
        this.$message.warning('请先选择要上传的文档');
        return;
      }
      
      try {
        this.uploading = true;
        
        // 创建FormData对象
        const formData = new FormData();
        formData.append('file', this.selectedFile);
        
        // 调用API上传文件
        const response = await uploadDocument(formData, this.teamId);
        
        // 处理上传结果
        if (response.code === 200) {
          this.$message.success('文件上传成功');
          this.$emit('success');
        } else {
          this.$message.error(response.msg || '上传失败');
        }
      } catch (error) {
        console.error('文件上传错误:', error);
        this.$message.error('文件上传失败，请重试');
      } finally {
        this.uploading = false;
      }
    },

    // 上传前验证
    beforeUpload(file) {
      const validTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain'];
      const isValidType = validTypes.includes(file.type);
      const isLt20M = file.size / 1024 / 1024 < 20;

      if (!isValidType) {
        this.$message.error('只能上传PDF、Word或TXT文档!');
        return false;
      }
      
      if (!isLt20M) {
        this.$message.error('文件大小不能超过20MB!');
        return false;
      }
      
      return true;
    },

    // 取消上传
    handleClose() {
      this.$emit('close');
    },

    // 确认上传
    handleSubmit() {
      if (this.selectedFile) {
        this.uploadFile();
      } else {
        this.$message.warning('请先选择要上传的文档');
      }
    }
  }
};
</script>

<style scoped>
.upload-container {
  padding: 0 20px;
}

.upload {
  width: 100%;
}

.selected-file {
  margin-top: 20px;
  padding: 15px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  background-color: #f8f8f8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  font-weight: 500;
  color: #303133;
}

.file-size {
  color: #909399;
  font-size: 12px;
}

.upload-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  gap: 10px;
}
</style>
