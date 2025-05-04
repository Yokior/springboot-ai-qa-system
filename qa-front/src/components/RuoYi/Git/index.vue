<template>
  <div>
    <svg-icon icon-class="github" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'YokiorGit',
  data() {
    return {
        url: 'https://github.com/Yokior/springboot-ai-qa-system'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>