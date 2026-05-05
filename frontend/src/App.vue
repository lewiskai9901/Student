<template>
  <div id="app" :style="themeStyle">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useConfigStore } from '@/stores/config'
import { themeApi } from '@/composables/useTheme'

const configStore = useConfigStore()

// 根据配置动态设置主题颜色
const themeStyle = computed(() => ({
  '--el-color-primary': configStore.themeColor
}))

// Phase 5: 应用启动立即初始化主题 (light/dark/system) 避免 FOUC
themeApi.init()

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