<template>
  <div class="dept-tree">
    <div class="tree-header">
      <span style="font-size: 13px; font-weight: 600; color: #111827;">组织筛选</span>
      <button v-if="selectedId" class="tree-clear" @click="clearSelection">清除</button>
    </div>
    <div class="tree-body">
      <div v-for="dept in departments" :key="dept.id">
        <div class="tree-node tree-dept" :class="{ active: selectedId === dept.id }" @click="selectNode(dept, 'DEPARTMENT')">
          {{ dept.unitName }}
        </div>
        <template v-if="dept.children">
          <div v-for="grade in dept.children" :key="grade.id" style="padding-left: 16px;">
            <div class="tree-node tree-grade" :class="{ active: selectedId === grade.id }" @click="selectNode(grade, 'GRADE')">
              {{ grade.unitName }}
            </div>
            <template v-if="grade.children">
              <div v-for="cls in grade.children" :key="cls.id" style="padding-left: 16px;">
                <div class="tree-node tree-class" :class="{ active: selectedId === cls.id }" @click="selectNode(cls, 'CLASS')">
                  {{ cls.unitName }}
                </div>
              </div>
            </template>
          </div>
        </template>
      </div>
      <div v-if="departments.length === 0" style="padding: 20px; color: #9ca3af; font-size: 12px; text-align: center;">
        暂无组织数据
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { orgUnitApi } from '@/api/organization'

const props = defineProps<{ semesterId?: number | string }>()

const emit = defineEmits<{
  select: [node: { type: string; id: number | string; name: string }]
}>()

const departments = ref<any[]>([])
const selectedId = ref<number | string | null>(null)

async function loadTree() {
  try {
    const tree = await orgUnitApi.getTree()
    // tree might be [{id, unitName, unitType:'SCHOOL', children:[{unitType:'DEPARTMENT',...}]}]
    // or it might directly be departments
    // Handle both cases
    const data = Array.isArray(tree) ? tree : (tree as any).data || []
    // Find departments: either at root level or under SCHOOL node
    let depts: any[] = []
    for (const node of data) {
      if (node.unitType === 'DEPARTMENT') {
        depts.push(node)
      } else if (node.children) {
        // SCHOOL level - get its department children
        depts.push(...(node.children || []).filter((c: any) => c.unitType === 'DEPARTMENT'))
      }
    }
    departments.value = depts
  } catch {
    departments.value = []
  }
}

function selectNode(node: any, type: string) {
  selectedId.value = node.id
  emit('select', { type, id: node.id, name: node.unitName })
}

function clearSelection() {
  selectedId.value = null
  emit('select', { type: '', id: '', name: '' })
}

onMounted(loadTree)
</script>

<style scoped>
.dept-tree {
  width: 200px;
  min-width: 200px;
  border-right: 1px solid #e5e7eb;
  background: #fafbfc;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px 8px;
  border-bottom: 1px solid #f3f4f6;
}
.tree-clear {
  font-size: 11px;
  color: #6b7280;
  cursor: pointer;
  background: none;
  border: none;
  padding: 2px 6px;
}
.tree-clear:hover { color: #2563eb; }
.tree-body { padding: 8px 0; flex: 1; overflow-y: auto; }
.tree-node {
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  color: #374151;
  border-radius: 4px;
  margin: 1px 6px;
  transition: all 0.1s;
}
.tree-node:hover { background: #f3f4f6; }
.tree-node.active { background: #eff6ff; color: #2563eb; font-weight: 500; }
.tree-dept { font-weight: 500; }
.tree-grade { color: #6b7280; }
.tree-class { color: #9ca3af; font-size: 11px; }
</style>
