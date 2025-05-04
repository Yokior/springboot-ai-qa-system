<template>
  <div>
    <svg-icon icon-class="question" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'YokiorDoc',
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