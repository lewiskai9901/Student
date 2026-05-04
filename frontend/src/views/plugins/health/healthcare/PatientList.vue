<template>
  <div class="p-6">
    <h1 class="text-xl font-semibold mb-4">病人列表</h1>
    <p class="text-gray-600">
      这是 HEALTH 行业插件的示例页面. 后端 PatientPlugin + HealthcareMenuPlugin 已注册,
      前端路由由 router/plugins/health.ts 条件加载.
    </p>
    <div class="mt-6 rounded border border-gray-200 bg-gray-50 p-4 text-sm text-gray-700">
      后续可扩展: 病历查询 / 床位分配 / 药品管理 / 医嘱登记 等.
    </div>
  </div>
</template>

<script setup lang="ts">
// 占位组件, 证明 HEALTH 插件的前端装载链路可行.
</script>
