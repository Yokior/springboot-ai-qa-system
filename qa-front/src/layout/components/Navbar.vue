<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb v-if="!topNav" id="breadcrumb-container" class="breadcrumb-container" />
    <top-nav v-if="topNav" id="topmenu-container" class="topmenu-container" />

    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <search id="header-search" class="right-menu-item" />

        <el-tooltip content="源码地址" effect="dark" placement="bottom">
          <ruo-yi-git id="ruoyi-git" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-tooltip content="文档地址" effect="dark" placement="bottom">
          <ruo-yi-doc id="ruoyi-doc" class="right-menu-item hover-effect" />
        </el-tooltip>

        <screenfull id="screenfull" class="right-menu-item hover-effect" />

        <el-tooltip content="布局大小" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect" />
        </el-tooltip>

      </template>

      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
        <div class="avatar-wrapper">
          <img :src="avatar" class="user-avatar">
          <span class="user-name">{{ name }}</span>
          <i class="el-icon-arrow-down" />
        </div>
        <el-dropdown-menu slot="dropdown" class="user-dropdown">
          <router-link to="/user/profile">
            <el-dropdown-item>
              <i class="el-icon-user-solid"></i>
              个人中心
            </el-dropdown-item>
          </router-link>
          <el-dropdown-item @click.native="setting = true">
            <i class="el-icon-setting"></i>
            布局设置
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="logout">
            <i class="el-icon-switch-button"></i>
            退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Search from '@/components/HeaderSearch'
import RuoYiGit from '@/components/RuoYi/Git'
import RuoYiDoc from '@/components/RuoYi/Doc'

export default {
  components: {
    Breadcrumb,
    TopNav,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search,
    RuoYiGit,
    RuoYiDoc
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device',
      'name'
    ]),
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'showSettings',
          value: val
        })
      }
    },
    topNav: {
      get() {
        return this.$store.state.settings.topNav
      }
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/index'
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/element-variables.scss";

.navbar {
  height: 60px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);

  .hamburger-container {
    line-height: 60px;
    height: 100%;
    float: left;
    padding: 0 16px;
    cursor: pointer;
    transition: all .3s;
    -webkit-tap-highlight-color:transparent;

    &:hover {
      background: rgba(0, 0, 0, .03)
    }
  }

  .breadcrumb-container {
    float: left;
    padding-left: 16px;
    line-height: 60px;
  }

  .topmenu-container {
    position: absolute;
    left: 70px;
    line-height: 60px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 60px;
    margin-right: 16px;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 12px;
      height: 100%;
      font-size: 18px;
      color: #303133;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: all .3s;

        &:hover {
          background: rgba(0, 0, 0, .03);
          color: $--color-primary;
        }
      }
    }

    .avatar-container {
      margin-right: 10px;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        position: relative;
        height: 60px;
        padding: 0 12px;
        
        .user-avatar {
          cursor: pointer;
          width: 36px;
          height: 36px;
          border-radius: 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
        
        .user-name {
          margin: 0 8px;
          font-size: 14px;
          font-weight: 500;
          color: #303133;
        }

        .el-icon-arrow-down {
          cursor: pointer;
          font-size: 12px;
          color: #909399;
          transition: all .3s;
        }
        
        &:hover {
          background: rgba(0, 0, 0, .03);
          
          .el-icon-arrow-down {
            color: $--color-primary;
            transform: rotate(180deg);
          }
        }
      }
    }
  }
}

.user-dropdown {
  .el-dropdown-item {
    padding: 12px 20px;
    display: flex;
    align-items: center;
    
    i {
      margin-right: 8px;
      font-size: 16px;
    }
  }
}
</style>
