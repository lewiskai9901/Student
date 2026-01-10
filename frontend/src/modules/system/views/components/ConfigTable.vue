<template>
  <el-table
    v-loading="loading"
    :data="data"
    border
    stripe
    style="width: 100%"
    :empty-text="loading ? '加载中...' : '暂无配置数据'"
  >
    <el-table-column prop="configLabel" label="配置标签" width="150" show-overflow-tooltip />
    <el-table-column prop="configKey" label="配置键" width="200" show-overflow-tooltip />
    <el-table-column prop="configValue" label="配置值" min-width="180" show-overflow-tooltip>
      <template #default="{ row }">
        <span v-if="row.configType === 'boolean'">
          <el-tag :type="row.configValue === 'true' ? 'success' : 'info'" size="small">
            {{ row.configValue === 'true' ? '是' : '否' }}
          </el-tag>
        </span>
        <span v-else>{{ row.configValue }}</span>
      </template>
    </el-table-column>
    <el-table-column prop="configType" label="类型" width="100" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.configType === 'string'" type="primary" size="small">字符串</el-tag>
        <el-tag v-else-if="row.configType === 'number'" type="success" size="small">数字</el-tag>
        <el-tag v-else-if="row.configType === 'boolean'" type="warning" size="small">布尔值</el-tag>
        <el-tag v-else-if="row.configType === 'json'" type="info" size="small">JSON</el-tag>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column prop="configDesc" label="描述" min-width="150" show-overflow-tooltip />
    <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
    <el-table-column prop="isSystem" label="系统内置" width="100" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.isSystem === 1" type="danger" size="small">是</el-tag>
        <el-tag v-else type="info" size="small">否</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="status" label="状态" width="100" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.status === 1" type="success" size="small">启用</el-tag>
        <el-tag v-else type="info" size="small">禁用</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="180" fixed="right" align="center">
      <template #default="{ row }">
        <el-button link type="warning" size="small" @click="$emit('edit', row)">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button
          link
          type="danger"
          size="small"
          :disabled="row.isSystem === 1"
          @click="$emit('delete', row)"
        >
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { Edit, Delete } from '@element-plus/icons-vue'

defineProps<{
  loading: boolean
  data: any[]
}>()

defineEmits<{
  edit: [row: any]
  delete: [row: any]
}>()
</script>

<style lang="scss" scoped>
// 样式继承自父组件
</style>
