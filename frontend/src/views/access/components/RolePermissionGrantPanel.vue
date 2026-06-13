<template>
  <div class="flex h-full flex-col">
    <!-- 工具条 -->
    <div class="flex items-center gap-2 border-b border-gray-200 bg-white px-4 py-2.5">
      <button class="ge-tool" @click="expandAll"><FolderOpen class="h-3.5 w-3.5" />展开全部</button>
      <button class="ge-tool" @click="collapseAll"><Folder class="h-3.5 w-3.5" />折叠全部</button>
      <button class="ge-tool" :disabled="readonly" @click="checkAll"><CheckSquare class="h-3.5 w-3.5" />全选</button>
      <button class="ge-tool" :disabled="readonly" @click="uncheckAll"><Square class="h-3.5 w-3.5" />取消全选</button>
      <span class="ml-auto text-xs text-gray-500">已选 <b class="text-blue-600">{{ selectedIds.length }}</b> 项</span>
      <button
        class="inline-flex h-8 items-center gap-1.5 rounded-md bg-blue-600 px-4 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        :disabled="readonly || saving"
        @click="save"
      >
        <Loader2 v-if="saving" class="h-3.5 w-3.5 animate-spin" />
        保存
      </button>
    </div>

    <!-- 模块分组 -->
    <div class="flex-1 overflow-y-auto bg-gray-50 p-4">
      <div v-if="loading" class="flex h-40 items-center justify-center text-gray-400">
        <Loader2 class="h-5 w-5 animate-spin" />
      </div>
      <template v-else>
        <div v-for="m in modules" :key="m.code" class="mb-3 last:mb-0 overflow-hidden rounded-lg border border-gray-200 bg-white">
          <div class="flex cursor-pointer items-center justify-between px-3 py-2.5 hover:bg-gray-50" @click="toggle(m.code)">
            <div class="flex items-center gap-2">
              <ChevronRight class="h-4 w-4 text-gray-400 transition-transform" :class="{ 'rotate-90': expanded.includes(m.code) }" />
              <span class="text-sm font-medium text-gray-900">{{ m.name }}</span>
              <span class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">{{ m.count }}</span>
            </div>
            <label class="flex items-center gap-1.5 text-xs text-gray-500" @click.stop>
              <input type="checkbox" :checked="m.checked" :disabled="readonly" class="h-4 w-4 rounded border-gray-300" @change="toggleModule(m)" />
              全选
            </label>
          </div>
          <div v-show="expanded.includes(m.code)" class="space-y-1 border-t border-gray-100 p-3">
            <template v-for="p in m.permissions" :key="p.id">
              <label class="flex items-center gap-2 text-sm text-gray-800">
                <input type="checkbox" :checked="isSelected(p.id)" :disabled="readonly" class="h-4 w-4 rounded border-gray-300" @change="togglePerm(p.id)" />
                {{ p.permissionName }}
                <code class="font-mono text-[10px] text-gray-400">{{ p.permissionCode }}</code>
              </label>
              <div v-if="p.children?.length" class="ml-6 space-y-1">
                <label v-for="c in p.children" :key="c.id" class="flex items-center gap-2 text-sm text-gray-600">
                  <input type="checkbox" :checked="isSelected(c.id)" :disabled="readonly" class="h-4 w-4 rounded border-gray-300" @change="togglePerm(c.id)" />
                  {{ c.permissionName }}
                  <code class="font-mono text-[10px] text-gray-400">{{ c.permissionCode }}</code>
                </label>
              </div>
            </template>
          </div>
        </div>
        <div v-if="!modules.length" class="py-10 text-center text-sm text-gray-400">暂无权限</div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ChevronRight, FolderOpen, Folder, CheckSquare, Square, Loader2 } from 'lucide-vue-next'
import { getPermissions, getRolePermissionIds, setRolePermissions } from '@/api/access'
import { getModuleLabel } from '@/views/access/permissionModuleLabels'
import type { LongId } from '@/types/common'

const props = defineProps<{ roleId: LongId; readonly?: boolean }>()
const emit = defineEmits<{ saved: [] }>()

const loading = ref(false)
const saving = ref(false)
const tree = ref<any[]>([])
const selectedIds = ref<(string | number)[]>([])
const expanded = ref<string[]>([])

function allIdsOf(perms: any[]): (string | number)[] {
  const ids: (string | number)[] = []
  const walk = (list: any[]) => list.forEach((p) => { ids.push(p.id); if (p.children?.length) walk(p.children) })
  walk(perms)
  return ids
}

interface Mod { code: string; name: string; count: number; permissions: any[]; checked: boolean }
const modules = computed<Mod[]>(() => {
  if (!tree.value.length) return []
  const map = new Map<string, any[]>()
  tree.value.forEach((p: any) => {
    const code = (p.permissionCode || '').split(':')[0]
    if (!map.has(code)) map.set(code, [])
    map.get(code)!.push(p)
  })
  const selStr = selectedIds.value.map(String)
  const out: Mod[] = []
  map.forEach((perms, code) => {
    const ids = allIdsOf(perms)
    out.push({
      code,
      name: getModuleLabel(code),
      count: ids.length,
      permissions: perms,
      checked: ids.length > 0 && ids.every((id) => selStr.includes(String(id))),
    })
  })
  return out.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})

function toggle(code: string) {
  const i = expanded.value.indexOf(code)
  i > -1 ? expanded.value.splice(i, 1) : expanded.value.push(code)
}
function expandAll() { expanded.value = modules.value.map((m) => m.code) }
function collapseAll() { expanded.value = [] }
function checkAll() {
  const all: (string | number)[] = []
  modules.value.forEach((m) => all.push(...allIdsOf(m.permissions)))
  selectedIds.value = all
}
function uncheckAll() { selectedIds.value = [] }
function toggleModule(m: Mod) {
  const ids = allIdsOf(m.permissions)
  const idStr = ids.map(String)
  if (m.checked) {
    selectedIds.value = selectedIds.value.filter((id) => !idStr.includes(String(id)))
  } else {
    const selStr = selectedIds.value.map(String)
    selectedIds.value.push(...ids.filter((id) => !selStr.includes(String(id))))
  }
}
function isSelected(id: LongId): boolean {
  return selectedIds.value.some((x) => String(x) === String(id))
}
function togglePerm(id: LongId) {
  const i = selectedIds.value.findIndex((x) => String(x) === String(id))
  i > -1 ? selectedIds.value.splice(i, 1) : selectedIds.value.push(id)
}

async function loadTree() {
  if (tree.value.length) return
  tree.value = await getPermissions()
}
async function loadSelected() {
  loading.value = true
  try {
    await loadTree()
    selectedIds.value = await getRolePermissionIds(props.roleId)
  } catch (e) {
    ElMessage.error('加载权限失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await setRolePermissions(props.roleId, selectedIds.value)
    ElMessage.success('权限分配成功')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.message || '分配失败')
  } finally {
    saving.value = false
  }
}

watch(() => props.roleId, loadSelected)
onMounted(loadSelected)
</script>

<style scoped>
.ge-tool {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  height: 2rem;
  border-radius: 0.375rem;
  border: 1px solid #d1d5db;
  padding: 0 0.75rem;
  font-size: 0.75rem;
  color: #374151;
  background: #fff;
}
.ge-tool:hover {
  background: #f9fafb;
}
.ge-tool:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
