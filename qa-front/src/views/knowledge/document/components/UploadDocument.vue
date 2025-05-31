<template>
  <div class="upload-container">
    <el-upload
      class="upload"
      drag
      :action="uploadUrl"
      :headers="headers"
      :on-preview="handlePreview"
      :on-remove="handleRemove"
      :before-remove="beforeRemove"
      :on-success="handleSuccess"
      :on-error="handleError"
      :before-upload="beforeUpload"
      :multiple="false"
      :show-file-list="true">
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">拖拽文件到此处或 <em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">
          支持上传PDF、Word、TXT文档，文件大小不超过20MB
        </div>
      </template>
    </el-upload>
    
    <div class="upload-actions">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :disabled="uploadFiles.length === 0">确认上传</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineEmits, computed } from 'vue';
import { getToken } from '@/utils/auth';
import Vue from 'vue';

const props = defineProps({
  teamId: {
    type: [String, Number],
    required: true
  }
});

const emit = defineEmits(['close', 'success']);

const uploadFiles = ref([]);
const uploadUrl = computed(() => {
  return `/api/knowledge/document/upload?teamId=${props.teamId}`;
});

// 获取认证头信息
const headers = computed(() => {
  return {
    Authorization: 'Bearer ' + getToken()
  };
});

// 上传前验证
const beforeUpload = (file) => {
  const validTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain'];
  const isValidType = validTypes.includes(file.type);
  const isLt20M = file.size / 1024 / 1024 < 20;

  if (!isValidType) {
    Vue.prototype.$message.error('只能上传PDF、Word或TXT文档!');
    return false;
  }
  
  if (!isLt20M) {
    Vue.prototype.$message.error('文件大小不能超过20MB!');
    return false;
  }
  
  return true;
};

// 处理文件上传成功
const handleSuccess = (response, file, fileList) => {
  if (response.code === 200) {
    Vue.prototype.$message.success('文件上传成功');
    uploadFiles.value = fileList;
  } else {
    Vue.prototype.$message.error(response.msg || '上传失败');
    // 从列表中移除上传失败的文件
    const index = fileList.indexOf(file);
    if (index !== -1) {
      fileList.splice(index, 1);
    }
  }
};

// 处理上传错误
const handleError = () => {
  Vue.prototype.$message.error('文件上传失败，请重试');
};

// 文件移除前的确认
const beforeRemove = (file) => {
  return new Promise((resolve) => {
    Vue.prototype.$confirm(`确定移除 ${file.name}？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
      resolve(true);
    }).catch(() => {
      resolve(false);
    });
  });
};

// 处理文件移除
const handleRemove = (file, fileList) => {
  uploadFiles.value = fileList;
};

// 文件预览
const handlePreview = (file) => {
  console.log(file);
};

// 取消上传
const handleClose = () => {
  emit('close');
};

// 确认上传
const handleSubmit = () => {
  if (uploadFiles.value.length > 0) {
    emit('success');
  } else {
    Vue.prototype.$message.warning('请先选择要上传的文档');
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

.upload-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  gap: 10px;
}
</style>
