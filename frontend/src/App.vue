<template>
  <div id="app" :style="themeStyle">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useConfigStore } from '@/stores/config'

const configStore = useConfigStore()

// 根据配置动态设置主题颜色
const themeStyle = computed(() => ({
  '--el-color-primary': configStore.themeColor
}))

// 加载配置
onMounted(async () => {
  await configStore.loadSystemConfig()
})
</script>

<style lang="scss">
#app {
  height: 100vh;
  width: 100vw;
}
</style>