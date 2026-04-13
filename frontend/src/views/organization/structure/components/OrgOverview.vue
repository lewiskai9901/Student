<template>
  <div class="ov-root">
    <!-- Stat Cards -->
    <div class="ov-cards">
      <div class="ov-card">
        <div class="ov-card-val">{{ stats.total }}</div>
        <div class="ov-card-label">组织总数</div>
      </div>
      <div class="ov-card">
        <div class="ov-card-val" style="color: #10b981;">{{ stats.active }}</div>
        <div class="ov-card-label">正常</div>
      </div>
      <div class="ov-card">
        <div class="ov-card-val" style="color: #f59e0b;">{{ stats.inactive }}</div>
        <div class="ov-card-label">冻结/撤销</div>
      </div>
    </div>

    <!-- Type Distribution -->
    <div class="ov-panel">
      <div class="ov-panel-header">
        <h3 class="ov-panel-title">类型分布</h3>
      </div>
      <div class="ov-panel-body">
        <div v-if="typeDistribution.length > 0" class="ov-type-list">
          <template v-for="(item, index) in typeDistribution" :key="item.name">
            <span class="ov-type-item">
              {{ item.name }} <b>{{ item.count }}</b>
            </span>
            <i v-if="index < typeDistribution.length - 1" class="ov-sep" />
          </template>
        </div>
        <div v-else class="ov-empty">暂无类型数据</div>
      </div>
    </div>

    <!-- Getting Started Tip -->
    <div v-if="stats.total === 0" class="ov-empty-box">
      <h3 class="ov-empty-title">开始创建组织架构</h3>
      <p class="ov-empty-desc">
        点击左侧底部"新增组织"按钮创建第一个组织单元，建立学校组织架构。
      </p>
    </div>

    <!-- Quick Guide -->
    <div v-else class="ov-tip">
      <p class="ov-tip-title">使用提示</p>
      <p class="ov-tip-desc">
        在左侧树形目录中点击任意组织节点，可查看详细信息和下级组织列表。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'


import type { DepartmentResponse } from '@/api/organization'

interface Props {
  treeData: DepartmentResponse[]
}

const props = defineProps<Props>()

// Count stats from tree
const countNodes = (nodes: DepartmentResponse[]): {
  total: number; active: number; inactive: number
} => {
  let total = 0
  let active = 0
  let inactive = 0
  const traverse = (list: DepartmentResponse[]) => {
    for (const node of list) {
      total++
      if (node.status === 'ACTIVE') active++
      else inactive++
      if (node.children) traverse(node.children)
    }
  }
  traverse(nodes)
  return { total, active, inactive }
}

const stats = computed(() => countNodes(props.treeData))

// Type distribution
const typeDistribution = computed(() => {
  const typeMap = new Map<string, { name: string; color: string; count: number }>()

  const traverse = (nodes: DepartmentResponse[]) => {
    for (const node of nodes) {
      const key = node.unitType
      const existing = typeMap.get(key)
      if (existing) {
        existing.count++
      } else {
        typeMap.set(key, {
          name: node.typeName || node.unitType,
          color: ({ ROOT: '#3b82f6', BRANCH: '#8b5cf6', FUNCTIONAL: '#10b981', GROUP: '#ef4444', CONTAINER: '#f59e0b' } as Record<string, string>)[node.category || ''] || '#6b7280',
          count: 1
        })
      }
      if (node.children) traverse(node.children)
    }
  }
  traverse(props.treeData)

  return Array.from(typeMap.values()).sort((a, b) => b.count - a.count)
})

</script>

<style scoped>
@import '@/styles/teaching-ui.css';

.ov-root { display: flex; flex-direction: column; gap: 8px; }

/* Stat cards */
.ov-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.ov-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
.ov-card-val { font-size: 16px; font-weight: 700; color: #111827; }
.ov-card-label { font-size: 11px; color: #6b7280; }

/* Panel */
.ov-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}
.ov-panel-header {
  padding: 6px 12px;
  border-bottom: 1px solid #f3f4f6;
}
.ov-panel-title {
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin: 0;
}
.ov-panel-body { padding: 8px 12px; }

/* Type list */
.ov-type-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.ov-type-item { font-size: 12px; color: #6b7280; }
.ov-type-item b { font-weight: 600; color: #111827; margin-left: 2px; }
.ov-sep { display: block; width: 1px; height: 10px; background: #d1d5db; }

/* Empty */
.ov-empty { padding: 10px 0; text-align: center; font-size: 12px; color: #9ca3af; }
.ov-empty-box {
  padding: 24px 16px;
  text-align: center;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  background: #fafafa;
}
.ov-empty-title { font-size: 13px; font-weight: 600; color: #374151; margin: 0 0 4px; }
.ov-empty-desc { font-size: 12px; color: #6b7280; margin: 0; }

/* Tip */
.ov-tip {
  padding: 8px 12px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
}
.ov-tip-title { font-size: 12px; font-weight: 600; color: #1e40af; margin: 0 0 2px; }
.ov-tip-desc { font-size: 12px; color: #1d4ed8; margin: 0; }
</style>
