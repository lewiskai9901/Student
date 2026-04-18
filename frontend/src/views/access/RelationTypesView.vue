<template>
  <div class="p-4">
    <div class="flex items-center justify-between mb-4">
      <div>
        <h2 class="text-lg font-semibold">关系类型字典</h2>
        <p class="text-xs text-gray-500 mt-1">
          v3 统一关系模型,所有 access_relations 的 relation 字段必须来自此字典。
          {{ stats.total }} 个类型 · CORE {{ stats.core }} · COMMON_EXT {{ stats.common }} · DOMAIN {{ stats.domain }}
        </p>
      </div>
    </div>

    <div class="tm-stat-bar mb-3">
      <span><em>成员关系</em> <strong>{{ groupBy('category', 'MEMBERSHIP').length }}</strong></span>
      <span class="sep">|</span>
      <span><em>管理关系</em> <strong>{{ groupBy('category', 'OWNERSHIP').length }}</strong></span>
      <span class="sep">|</span>
      <span><em>关联关系</em> <strong>{{ groupBy('category', 'ASSOCIATION').length }}</strong></span>
      <span class="sep">|</span>
      <span><em>委托关系</em> <strong>{{ groupBy('category', 'DELEGATION').length }}</strong></span>
      <span class="sep">|</span>
      <span><em>订阅关系</em> <strong>{{ groupBy('category', 'SUBSCRIPTION').length }}</strong></span>
    </div>

    <div v-for="tier in ['CORE', 'COMMON_EXT', 'DOMAIN']" :key="tier" class="mb-6">
      <div class="flex items-center gap-2 mb-2">
        <el-tag :type="tagType(tier)">{{ tierLabel(tier) }}</el-tag>
        <span class="text-xs text-gray-500">
          {{ groupByTier(tier).length }} 个 · {{ tierDesc(tier) }}
        </span>
      </div>
      <el-table :data="groupByTier(tier)" stripe size="small">
        <el-table-column label="关系码" prop="relationCode" width="130" />
        <el-table-column label="中文名" prop="relationName" width="100" />
        <el-table-column label="主体 → 资源" width="180">
          <template #default="{ row }">
            <span class="text-xs">
              <el-tag size="small">{{ row.fromType }}</el-tag>
              <span class="mx-1">→</span>
              <el-tag size="small">{{ row.toType }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="category" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="传递" prop="isTransitive" width="60" align="center">
          <template #default="{ row }">
            <span class="text-xs">{{ row.isTransitive ? '✓' : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" prop="registeredBy" width="150" />
        <el-table-column label="说明" prop="description" />
      </el-table>
    </div>

    <div v-if="!loading && types.length === 0" class="text-center text-gray-400 py-10">
      暂无关系类型数据
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'
import { ElMessage } from 'element-plus'

const types = ref<RelationTypeDef[]>([])
const loading = ref(false)

const stats = computed(() => ({
  total: types.value.length,
  core: types.value.filter(t => t.tier === 'CORE').length,
  common: types.value.filter(t => t.tier === 'COMMON_EXT').length,
  domain: types.value.filter(t => t.tier === 'DOMAIN').length
}))

function groupByTier(tier: string) {
  return types.value.filter(t => t.tier === tier)
}

function groupBy(field: keyof RelationTypeDef, value: string) {
  return types.value.filter(t => t[field] === value)
}

function tierLabel(tier: string): string {
  return {
    CORE: '核心内置',
    COMMON_EXT: '通用扩展',
    DOMAIN: '行业插件'
  }[tier] || tier
}

function tierDesc(tier: string): string {
  return {
    CORE: '系统硬编码,所有行业通用,不可删除',
    COMMON_EXT: '跨行业扩展(教育/医疗/养老共享)',
    DOMAIN: '具体行业插件注册'
  }[tier] || ''
}

function tagType(tier: string): 'primary' | 'success' | 'warning' | 'info' {
  return {
    CORE: 'primary' as const,
    COMMON_EXT: 'success' as const,
    DOMAIN: 'warning' as const
  }[tier] || 'info'
}

function categoryLabel(cat: string): string {
  return {
    OWNERSHIP: '管理',
    MEMBERSHIP: '归属',
    ASSOCIATION: '关联',
    DELEGATION: '委托',
    SUBSCRIPTION: '订阅'
  }[cat] || cat
}

async function load() {
  loading.value = true
  try {
    types.value = await relationTypeApi.list()
  } catch (e: any) {
    ElMessage.error('加载关系字典失败: ' + (e?.message || e))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.tm-stat-bar {
  display: flex;
  gap: 12px;
  padding: 8px 12px;
  background: #f9fafb;
  border-radius: 6px;
  font-size: 12px;
  color: #6b7280;
}
.tm-stat-bar em {
  font-style: normal;
  margin-right: 4px;
}
.tm-stat-bar strong {
  color: #111827;
  font-weight: 600;
}
.tm-stat-bar .sep {
  color: #d1d5db;
}
</style>
