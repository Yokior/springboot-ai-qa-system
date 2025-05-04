<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}" :style="{ backgroundColor: sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground }">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <h1 v-else class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' ? variables.logoTitleColor : variables.logoLightTitleColor }">{{ title }} </h1>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <div class="logo-container">
          <img v-if="logo" :src="logo" class="sidebar-logo" />
          <div class="logo-divider" v-if="logo"></div>
          <h1 class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' ? variables.logoTitleColor : variables.logoLightTitleColor }">{{ title }} </h1>
        </div>
      </router-link>
    </transition>
  </div>
</template>

<script>
import logoImg from '@/assets/logo/logo.png'
import variables from '@/assets/styles/variables.scss'

export default {
  name: 'SidebarLogo',
  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  },
  computed: {
    variables() {
      return variables
    },
    sideTheme() {
      return this.$store.state.settings.sideTheme
    }
  },
  data() {
    return {
      title: '智能问答系统',
      logo: logoImg
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/variables.scss";
@import "~@/assets/styles/element-variables.scss";

.sidebarLogoFade-enter-active {
  transition: opacity 0.3s ease;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 64px;
  line-height: 64px;
  text-align: center;
  overflow: hidden;
  box-shadow: 0 1px 0 0 rgba(0, 0, 0, 0.1);

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .logo-container {
      display: flex;
      align-items: center;
      height: 64px;
    }

    & .sidebar-logo {
      width: 36px;
      height: 36px;
      border-radius: 8px;
      vertical-align: middle;
      transition: all 0.3s;
    }
    
    .logo-divider {
      height: 24px;
      width: 1px;
      background-color: rgba(255, 255, 255, 0.3);
      margin: 0 12px;
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: #fff;
      font-weight: 600;
      line-height: 64px;
      font-size: 16px;
      font-family: 'Inter', sans-serif;
      vertical-align: middle;
      transition: all 0.3s;
      white-space: nowrap;
    }
  }

  &.collapse {
    .sidebar-logo {
      margin-right: 0px;
    }
  }
}
</style>
