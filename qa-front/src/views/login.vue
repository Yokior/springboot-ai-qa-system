<template>
  <div class="login">
    <!-- 添加容器用于定位和动画 -->
    <div class="login-form-container">
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
        <h3 class="title">{{title}}</h3>
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            auto-complete="off"
            placeholder="账号"
            prefix-icon="el-icon-user-solid"
          >
            <!-- 使用 el-icon 代替 svg-icon 以简化 -->
            <!-- <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" /> -->
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter.native="handleLogin"
            prefix-icon="el-icon-lock"
            show-password
          >
            <!-- <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" /> -->
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled" class="captcha-item">
          <el-input
            v-model="loginForm.code"
            auto-complete="off"
            placeholder="验证码"
            @keyup.enter.native="handleLogin"
            prefix-icon="el-icon-key"
          >
            <!-- <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" /> -->
          </el-input>
          <div class="login-code">
            <img :src="codeUrl" @click="getCode" class="login-code-img"/>
          </div>
        </el-form-item>
        <el-form-item>
             <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
             <div style="float: right;" v-if="register">
                <router-link class="link-type" :to="'/register'">立即注册</router-link>
             </div>
        </el-form-item>
        <el-form-item style="width:100%;">
          <el-button
            :loading="loading"
            size="medium"
            type="primary"
            style="width:100%;"
            @click.native.prevent="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登 录 中...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE || "智能问答系统", // 提供默认标题
      codeUrl: "",
      loginForm: {
        username: "admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关
      register: false, // 您可以根据需要设置为 true 来显示注册链接
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          // 确保返回的是 Base64 字符串
          this.codeUrl = res.img.startsWith('data:image') ? res.img : "data:image/gif;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      }).catch(() => {
         // 处理获取验证码失败的情况，例如禁用验证码输入
         this.captchaEnabled = false;
         console.error("获取验证码失败");
      })
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        // 保留已有值，防止覆盖uuid等
        ...this.loginForm,
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 })
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
          } else {
            Cookies.remove("username")
            Cookies.remove("password")
            Cookies.remove('rememberMe')
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{})
          }).catch(() => {
            this.loading = false
            if (this.captchaEnabled) {
              this.getCode()
            }
          })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss">
.login {
  display: flex;
  justify-content: center; // 水平居中
  align-items: center; // 垂直居中
  height: 100vh;
  // 设置新的背景图片
  background: url("~@/assets/images/login-background.jpg") no-repeat center center fixed; // 使用 ~@ 引用 src 下的资源
  background-size: cover;
  overflow: hidden;
}

.login-form-container {
  width: 420px;
  max-width: 90%;
  margin: 0 auto; // 确保在 flex 布局下也居中（虽然 justify-content 做了）
  position: relative; // 如果需要绝对定位内部元素
  // 入场动画
  animation: formFadeIn 0.7s ease-out forwards;
  opacity: 0;
}

@keyframes formFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-form {
  padding: 35px 35px 25px 35px;
  border-radius: 12px; // 更圆润的边角

  // --- 毛玻璃效果 ---
  // 后备方案：半透明背景
  background: rgba(25, 40, 70, 0.4); // 深蓝紫色调的半透明，与背景更协调

  // 优先使用 backdrop-filter 实现毛玻璃
  @supports (backdrop-filter: blur(10px)) or (-webkit-backdrop-filter: blur(10px)) {
    background: rgba(25, 40, 70, 0.3); // 使用 filter 时可以更透明
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
  }
  // --- 毛玻璃效果结束 ---

  border: 1px solid rgba(255, 255, 255, 0.15); // 非常细微的亮色边框
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.2); // 稍微加深阴影

  .el-form-item {
    margin-bottom: 25px; // 增加项间距
    border: none;
    background: transparent; // 确保表单项本身无背景
  }

  .el-input {
    height: 45px;
    line-height: 45px;
    input {
      height: 45px;
      line-height: 45px;
      background: rgba(0, 0, 0, 0.2); // 深色半透明输入框背景
      border-radius: 8px;
      border: 1px solid rgba(255, 255, 255, 0.1); // 输入框边框
      color: #eee; // 亮色输入文字
      padding-left: 35px; // 为图标留出空间
      transition: background-color 0.3s ease, border-color 0.3s ease;

      &::placeholder {
        color: rgba(255, 255, 255, 0.5);
      }

      &:focus {
        background: rgba(0, 0, 0, 0.3);
        border-color: rgba(255, 255, 255, 0.3);
        outline: none;
      }
    }
    // 输入框前缀图标样式
    .el-input__prefix {
      left: 10px;
      display: flex;
      align-items: center;
      height: 100%;
      .el-input__icon {
         color: rgba(255, 255, 255, 0.6);
         font-size: 16px; // 稍微增大图标
      }
    }
     // 密码框 '显示密码' 图标的样式
    .el-input__suffix {
       right: 10px;
       .el-input__suffix-inner {
         display: flex;
         align-items: center;
         height: 100%;
         .el-input__icon {
           color: rgba(255, 255, 255, 0.6);
           cursor: pointer;
            &:hover {
             color: rgba(255, 255, 255, 0.9);
           }
         }
       }
    }
  }
}

.title {
  margin: 0 auto 35px auto; // 增加标题下边距
  text-align: center;
  color: #fff;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 1px;
  text-shadow: 0 1px 3px rgba(0,0,0,0.2); // 给标题加一点阴影
}

.login-code {
  // width: 33%; // 由 input 的 width 决定 - 不再需要，由flex控制
  height: 45px;
  margin-left: 10px; // 添加左边距代替之前的 margin-right
  img {
    cursor: pointer;
    // vertical-align: middle; // flex布局下通常不需要
    border-radius: 8px; // 匹配输入框圆角
    height: 100%; // 高度占满容器
    display: block; // 避免图片下方可能的空隙
    transition: opacity 0.3s ease;
     &:hover {
      opacity: 0.85; // 悬停时稍微变暗
    }
  }
}

// 新增：使用Flexbox对齐验证码输入框和图片
.captcha-item {
  // 应用Flexbox布局到整个FormItem
  // 注意：el-form-item内部可能有多层div，直接对其应用flex可能效果不佳
  // 更可靠的方式是针对其直接子元素（通常是.el-form-item__content）
  .el-form-item__content {
      display: flex;
      align-items: center; // 垂直居中对齐
      width: 100%; // 确保内容区域占满

      .el-input {
        flex: 1; // 输入框占据剩余空间
        // 移除可能冲突的内联样式残留影响
        display: block !important; 
        width: auto !important; // 让flex接管宽度
        margin-right: 0 !important; // 间距由 login-code 的 margin-left 控制
      }

      .login-code {
        flex-shrink: 0; // 防止图片容器被压缩
        // height 和 margin-left 已在 .login-code 规则中定义
      }
  }
}

.el-checkbox {
  // color: #fff; // 直接设置父元素颜色可能被覆盖
  // font-weight: 500;

  // 直接定位标签文字元素
  .el-checkbox__label {
      color: #fff !important; // 使用更具体的选择器，并尝试 !important 强制覆盖
      font-weight: 500;
      padding-left: 5px; // 增加文字和选框的间距，可选
  }

  .el-checkbox__input.is-checked .el-checkbox__inner {
    // 使用一个与按钮或主题协调的颜色
    background-color: #5a7cff; // 例如使用按钮渐变色之一
    border-color: #5a7cff;
  }
  .el-checkbox__inner {
    background: rgba(0, 0, 0, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.2);
  }
  &:hover .el-checkbox__inner {
     border-color: rgba(255, 255, 255, 0.5);
  }
  // 调整记住密码和立即注册的布局
  & + div { // 选择紧跟在 checkbox 后面的 div (注册链接容器)
      line-height: 19px; // 使其与 checkbox 在垂直方向上大致对齐
  }
}

.el-button--primary {
  font-size: 16px;
  font-weight: 500;
  // 调整渐变色以匹配新背景
  background: linear-gradient(135deg, #4f70fc 0%, #8b54ff 100%); // 紫蓝色渐变
  border: none;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;

  &:hover {
     background: linear-gradient(135deg, #5a7cff 0%, #9d6bff 100%); // 悬停时变亮
     box-shadow: 0 6px 20px rgba(139, 84, 255, 0.3); // 添加紫色调光晕
     transform: translateY(-1px); // 轻微上移
  }
   &:active {
     transform: translateY(1px) scale(0.99); // 按下效果
     box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
   }
}

.link-type {
  color: #b0c4ff; // 淡蓝色链接
  transition: color 0.3s ease;
   &:hover {
     color: #fff;
     text-decoration: underline;
   }
}

// 隐藏旧的页脚（如果不需要）
.el-login-footer {
 display: none;
}
</style>
