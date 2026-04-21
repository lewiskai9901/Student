<template>
  <div class="p-4">
    <div class="mb-4">
      <h2 class="text-lg font-semibold">关系字典</h2>
      <p class="text-xs text-gray-500 mt-1">
        所有可用关系由 Java 插件代码注册,非管理员运行时配置。共 {{ types.length }} 条
        · 核心 {{ stats.core }} · 行业 {{ stats.domain }}
      </p>
    </div>

    <!-- 分类统计条 -->
    <div class="tm-stat-bar mb-4">
      <span v-for="cat in categories" :key="cat.code">
        <em>{{ cat.label }}</em>
        <strong>{{ byCategory(cat.code).length }}</strong>
      </span>
    </div>

    <!-- 按"来源插件"分组 (registered_by) -->
    <div v-for="src in sourcesSorted" :key="src" class="mb-5">
      <div class="flex items-center gap-2 mb-2">
        <el-tag :type="sourceTagType(src)" effect="plain">{{ sourceLabel(src) }}</el-tag>
        <span class="text-xs text-gray-500">{{ sourceDesc(src) }}</span>
        <span class="text-xs text-gray-400">· {{ bySource(src).length }} 条</span>
      </div>
      <el-table
        :data="bySource(src)"
        stripe size="small"
        class="rounded"
        :row-class-name="relationRowClass"
      >
        <el-table-column label="关系码" prop="relationCode" width="180">
          <template #default="{ row }">
            <code class="relation-code">{{ row.relationCode }}</code>
            <span
              v-if="isPluginDisabled(row)"
              class="disabled-by-plugin-badge ml-1"
              title="所属插件已禁用 — 此关系级联软失效"
            >插件禁用</span>
          </template>
        </el-table-column>
        <el-table-column label="中文名" prop="relationName" width="90" />
        <el-table-column label="主体 → 资源" width="200">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabel(row.fromType) }}</el-tag>
            <span class="mx-2 text-gray-400">→</span>
            <el-tag size="small">{{ typeLabel(row.toType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类别" prop="category" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="传递" width="60" align="center">
          <template #default="{ row }">
            <span v-if="row.isTransitive" class="text-green-600">✓</span>
            <span v-else class="text-gray-300">—</span>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="description" show-overflow-tooltip />
      </el-table>
    </div>

    <div v-if="!loading && !types.length" class="text-center text-gray-400 py-10">暂无关系类型</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { relationTypeApi, type RelationTypeDef } from '@/api/relationType'
import { ElMessage } from 'element-plus'

const types = ref<RelationTypeDef[]>([])
const loading = ref(false)

const stats = computed(() => ({
  core: types.value.filter(t => t.tier === 'CORE').length,
  domain: types.value.filter(t => t.tier === 'DOMAIN').length
}))

const categories = [
  { code: 'OWNERSHIP',    label: '管理' },
  { code: 'MEMBERSHIP',   label: '成员' },
  { code: 'ASSOCIATION',  label: '关联' },
  { code: 'DELEGATION',   label: '委托' },
  { code: 'SUBSCRIPTION', label: '订阅' }
]
function byCategory(code: string) { return types.value.filter(t => t.category === code) }
function categoryLabel(code: string): string {
  return categories.find(c => c.code === code)?.label || code
}

// 来源(registered_by)分组 — CORE 置顶,其他按字母序
const sourcesSorted = computed(() => {
  const set = new Set(types.value.map(t => t.registeredBy))
  const arr = Array.from(set)
  return arr.sort((a, b) => {
    if (a === 'CORE') return -1
    if (b === 'CORE') return 1
    return a.localeCompare(b)
  })
})
function bySource(src: string) { return types.value.filter(t => t.registeredBy === src) }

function sourceLabel(src: string): string {
  if (src === 'CORE') return '核心内置'
  if (src.endsWith('Plugin')) return src.replace('Plugin', ' 插件')
  return src
}
function sourceDesc(src: string): string {
  if (src === 'CORE') return '平台自带,任何行业通用,不可删除'
  if (src === 'EducationPlugin') return '教育行业插件,可随插件卸载'
  return `由 ${src} 注册`
}
function sourceTagType(src: string): 'primary' | 'warning' | 'success' | 'info' {
  if (src === 'CORE') return 'primary'
  if (src === 'EducationPlugin') return 'warning'
  return 'success'
}

function typeLabel(t: string): string {
  return { user: '用户', org_unit: '组织', place: '场所' }[t as string] || t
}

/** pluginEnabled 后端可能返回 0/1 (tinyint) 或 boolean — 统一判定 */
function isPluginDisabled(row: RelationTypeDef): boolean {
  const v = row.pluginEnabled
  if (v === false) return true
  if (v === 0) return true
  return false
}

function relationRowClass({ row }: { row: RelationTypeDef }): string {
  return isPluginDisabled(row) ? 'row-disabled-by-plugin' : ''
}

async function load() {
  loading.value = true
  try {
    // 管理员视角: 显示所属插件被禁的关系 (灰显)
    types.value = await relationTypeApi.list({ includeDisabled: true })
  }
  catch (e: any) { ElMessage.error('加载失败: ' + (e?.message || e)) }
  finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.tm-stat-bar {
  display: flex;
  gap: 16px;
  padding: 8px 14px;
  background: #f9fafb;
  border-radius: 6px;
  font-size: 12px;
  color: #6b7280;
}
.tm-stat-bar em { font-style: normal; margin-right: 4px; }
.tm-stat-bar strong { color: #111827; font-weight: 600; }
.relation-code {
  font-family: 'JetBrains Mono', Menlo, monospace;
  font-size: 12px;
  color: #2563eb;
  background: #eff6ff;
  padding: 1px 6px;
  border-radius: 3px;
}

/* 插件级联禁用行灰显 (需要用 :deep 穿透 Element Plus 行) */
:deep(.row-disabled-by-plugin) {
  opacity: 0.55;
  background-color: #fafaf9 !important;
}
:deep(.row-disabled-by-plugin:hover > td) {
  background-color: #fef3c7 !important;
}

.disabled-by-plugin-badge {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  color: #a16207;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  line-height: 1.4;
  cursor: help;
  vertical-align: middle;
}
</style>
