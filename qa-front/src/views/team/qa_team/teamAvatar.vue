<template>
  <div>
    <div class="team-info-head" @click="editCropper()">
      <img v-bind:src="avatarUrl" v-if="avatarUrl" class="img-circle img-lg" />
      <div v-else class="empty-avatar" title="点击上传团队头像">
        <i class="el-icon-plus"></i>
      </div>
    </div>
    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body @opened="modalOpened" @close="closeDialog">
      <el-row>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <vue-cropper
            ref="cropper"
            :img="options.img"
            :info="true"
            :autoCrop="options.autoCrop"
            :autoCropWidth="options.autoCropWidth"
            :autoCropHeight="options.autoCropHeight"
            :fixedBox="options.fixedBox"
            :outputType="options.outputType"
            @realTime="realTime"
            v-if="visible"
          />
        </el-col>
        <el-col :xs="24" :md="12" :style="{height: '350px'}">
          <div class="avatar-upload-preview">
            <img :src="previews.url" :style="previews.img" />
          </div>
        </el-col>
      </el-row>
      <br />
      <el-row>
        <el-col :lg="2" :sm="3" :xs="3">
          <el-upload action="#" :http-request="requestUpload" :show-file-list="false" :before-upload="beforeUpload">
            <el-button size="small">
              选择
              <i class="el-icon-upload el-icon--right"></i>
            </el-button>
          </el-upload>
        </el-col>
        <el-col :lg="{span: 1, offset: 2}" :sm="2" :xs="2">
          <el-button icon="el-icon-plus" size="small" @click="changeScale(1)"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :sm="2" :xs="2">
          <el-button icon="el-icon-minus" size="small" @click="changeScale(-1)"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :sm="2" :xs="2">
          <el-button icon="el-icon-refresh-left" size="small" @click="rotateLeft()"></el-button>
        </el-col>
        <el-col :lg="{span: 1, offset: 1}" :sm="2" :xs="2">
          <el-button icon="el-icon-refresh-right" size="small" @click="rotateRight()"></el-button>
        </el-col>
        <el-col :lg="{span: 2, offset: 6}" :sm="2" :xs="2">
          <el-button type="primary" size="small" @click="uploadImg()">提 交</el-button>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
import { VueCropper } from "vue-cropper"
import { debounce } from '@/utils'
import { uploadTeamAvatar } from "@/api/team/qa_team"

export default {
  components: { VueCropper },
  props: {
    value: {
      type: String,
      default: ''
    },
    teamId: {
      type: [Number, String],
      required: true
    }
  },
  data() {
    return {
      // 实际头像地址
      avatarUrl: '',
      // 是否显示弹出层
      open: false,
      // 是否显示cropper
      visible: false,
      // 弹出层标题
      title: "修改团队头像",
      options: {
        img: '', // 裁剪图片的地址
        autoCrop: true, // 是否默认生成截图框
        autoCropWidth: 200, // 默认生成截图框宽度
        autoCropHeight: 200, // 默认生成截图框高度
        fixedBox: true, // 固定截图框大小 不允许改变
        outputType: "png", // 默认生成截图为PNG格式
        filename: 'team_avatar' // 文件名称
      },
      previews: {},
      resizeHandler: null
    }
  },
  watch: {
    value: {
      handler(val) {
        if (val) {
          // 如果是完整URL，直接使用
          if (val.startsWith('http://') || val.startsWith('https://')) {
            this.avatarUrl = val;
          } else if (val.startsWith('data:image/')) {
            // 如果是base64数据，直接使用
            this.avatarUrl = val;
          } else {
            // 否则拼接基础API路径
            this.avatarUrl = process.env.VUE_APP_BASE_API + val;
          }
        } else {
          this.avatarUrl = '';
        }
        // 更新裁剪器的图片，默认使用空图像
        this.options.img = this.avatarUrl || '';
      },
      immediate: true
    }
  },
  methods: {
    // 编辑头像
    editCropper() {
      this.open = true
    },
    // 打开弹出层结束时的回调
    modalOpened() {
      this.visible = true
      if (!this.resizeHandler) {
        this.resizeHandler = debounce(() => {
          this.refresh()
        }, 100)
      }
      window.addEventListener("resize", this.resizeHandler)
    },
    // 刷新组件
    refresh() {
      this.$refs.cropper && this.$refs.cropper.refresh()
    },
    // 覆盖默认的上传行为
    requestUpload() {
    },
    // 向左旋转
    rotateLeft() {
      this.$refs.cropper.rotateLeft()
    },
    // 向右旋转
    rotateRight() {
      this.$refs.cropper.rotateRight()
    },
    // 图片缩放
    changeScale(num) {
      num = num || 1
      this.$refs.cropper.changeScale(num)
    },
    // 上传预处理
    beforeUpload(file) {
      if (file.type.indexOf("image/") == -1) {
        this.$modal.msgError("文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。")
      } else {
        const reader = new FileReader()
        reader.readAsDataURL(file)
        reader.onload = () => {
          this.options.img = reader.result
          this.options.filename = file.name
        }
      }
    },
    // 上传图片
    uploadImg() {
      if (!this.teamId) {
        this.$modal.msgError("团队ID不能为空，请先保存团队基本信息")
        return
      }
      
      this.$refs.cropper.getCropBlob(data => {
        let formData = new FormData()
        formData.append("teamAvatarFile", data, this.options.filename)
        formData.append("teamId", this.teamId)
        
        this.$modal.loading("正在上传头像，请稍候...")
        
        uploadTeamAvatar(formData).then(response => {
          this.$modal.closeLoading()
          
          if (response.code === 200) {
            this.open = false
            let imgUrl = response.imgUrl || (response.data && response.data.imgUrl) || '';
            this.avatarUrl = imgUrl ? process.env.VUE_APP_BASE_API + imgUrl : '';
            this.$emit('input', imgUrl || '')
            this.$emit('upload-success', imgUrl || '')
            this.$modal.msgSuccess("上传成功")
            this.visible = false
          } else {
            this.$modal.msgError(response.msg || "上传失败，请重试")
          }
        }).catch(error => {
          this.$modal.closeLoading()
          console.error("头像上传出错:", error);
          this.$modal.msgError("头像上传发生错误，请检查网络或联系管理员")
        })
      })
    },
    // 实时预览
    realTime(data) {
      this.previews = data
    },
    // 关闭窗口
    closeDialog() {
      this.options.img = this.avatarUrl || ''
      this.visible = false
      window.removeEventListener("resize", this.resizeHandler)
    }
  }
}
</script>
<style scoped lang="scss">
.team-info-head {
  position: relative;
  display: inline-block;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  background-color: #f5f7fa;
  border: 1px dashed #d9d9d9;
  
  &:hover {
    border-color: #409EFF;
    
    &:after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      top: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
    }
    
    &:before {
      content: '+';
      position: absolute;
      left: 0;
      right: 0;
      top: 0;
      bottom: 0;
      color: #fff;
      font-size: 30px;
      font-style: normal;
      text-align: center;
      line-height: 120px;
      z-index: 1;
    }
  }
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.empty-avatar {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  
  i {
    font-size: 30px;
    color: #c0c4cc;
  }
}

.avatar-upload-preview {
  position: absolute;
  top: 50%;
  transform: translate(50%, -50%);
  width: 200px;
  height: 200px;
  border-radius: 50%;
  box-shadow: 0 0 4px #ccc;
  overflow: hidden;
}
</style> 