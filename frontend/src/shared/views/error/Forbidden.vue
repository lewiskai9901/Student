<template>
  <div class="forbidden">
    <el-result
      icon="error"
      title="403"
      sub-title="很抱歉,您没有权限访问该页面"
    >
      <template #extra>
        <el-button type="primary" @click="goHome">
          返回首页
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style lang="scss" scoped>
.forbidden {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
