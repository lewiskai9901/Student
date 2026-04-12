<template>
  <div class="sched-tree">
    <div class="tree-header">
      <span class="tree-title">{{ modeLabel }}</span>
      <span v-if="multiple && selectedIdSet.size > 0" class="tree-count">已选 {{ leafSelectedCount }}</span>
      <button v-if="(multiple && selectedIdSet.size > 0) || (!multiple && selectedId)" class="tree-clear" @click="clearSelection">清除</button>
    </div>
    <div class="tree-body">
      <!-- CLASS mode: dept → grade → class -->
      <template v-if="mode === 'class'">
        <div v-for="dept in classTree" :key="dept.id">
          <div class="tree-node tree-l1" :class="{ active: !multiple && selectedId === dept.id, selected: multiple && isNodeSelected(dept) }" @click="handleNodeClick(dept, 'DEPARTMENT')">
            <span v-if="multiple" class="tree-check" :class="{ checked: isNodeSelected(dept), indeterminate: isNodeIndeterminate(dept) }"></span>
            {{ dept.name }}
          </div>
          <div v-for="grade in dept.children" :key="grade.id" style="padding-left: 14px;">
            <div class="tree-node tree-l2" :class="{ active: !multiple && selectedId === grade.id, selected: multiple && isNodeSelected(grade) }" @click="handleNodeClick(grade, 'GRADE')">
              <span v-if="multiple" class="tree-check" :class="{ checked: isNodeSelected(grade), indeterminate: isNodeIndeterminate(grade) }"></span>
              {{ grade.name }}
            </div>
            <div v-for="cls in grade.children" :key="cls.id" style="padding-left: 14px;">
              <div class="tree-node tree-l3" :class="{ active: !multiple && selectedId === cls.id, selected: multiple && selectedIdSet.has(cls.id) }" @click="handleLeafClick(cls, 'CLASS')">
                <span v-if="multiple" class="tree-check" :class="{ checked: selectedIdSet.has(cls.id) }"></span>
                {{ cls.name }}
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- TEACHER mode: dept → teachers -->
      <template v-if="mode === 'teacher'">
        <div v-for="dept in teacherTree" :key="dept.name">
          <div class="tree-node tree-l1" :class="{ selected: multiple && isTeacherDeptSelected(dept) }" @click="multiple && toggleTeacherDept(dept)">
            <span v-if="multiple" class="tree-check" :class="{ checked: isTeacherDeptSelected(dept), indeterminate: isTeacherDeptIndeterminate(dept) }"></span>
            {{ dept.name }}
          </div>
          <div v-for="t in dept.teachers" :key="t.id" style="padding-left: 14px;">
            <div class="tree-node tree-l3" :class="{ active: !multiple && selectedId === t.id, selected: multiple && selectedIdSet.has(t.id) }" @click="handleLeafClick({ id: t.id, name: t.name }, 'TEACHER')">
              <span v-if="multiple" class="tree-check" :class="{ checked: selectedIdSet.has(t.id) }"></span>
              {{ t.name }}
            </div>
          </div>
        </div>
        <div v-if="teacherTree.length === 0" class="tree-empty">暂无教师数据</div>
      </template>

      <!-- CLASSROOM mode: building → floor → room -->
      <template v-if="mode === 'classroom'">
        <div v-for="bldg in classroomTree" :key="bldg.id">
          <div class="tree-node tree-l1" :class="{ selected: multiple && isNodeSelected(bldg) }" @click="multiple && toggleBranch(bldg)">
            <span v-if="multiple" class="tree-check" :class="{ checked: isNodeSelected(bldg), indeterminate: isNodeIndeterminate(bldg) }"></span>
            {{ bldg.name }}
          </div>
          <div v-for="floor in bldg.children" :key="floor.id" style="padding-left: 14px;">
            <div class="tree-node tree-l2" :class="{ selected: multiple && isNodeSelected(floor) }" @click="multiple && toggleBranch(floor)">
              <span v-if="multiple" class="tree-check" :class="{ checked: isNodeSelected(floor), indeterminate: isNodeIndeterminate(floor) }"></span>
              {{ floor.name }}
            </div>
            <div v-for="room in floor.children" :key="room.id" style="padding-left: 14px;">
              <div class="tree-node tree-l3" :class="{ active: !multiple && selectedId === room.id, selected: multiple && selectedIdSet.has(room.id) }" @click="handleLeafClick({ id: room.id, name: room.fullName }, 'CLASSROOM')">
                <span v-if="multiple" class="tree-check" :class="{ checked: selectedIdSet.has(room.id) }"></span>
                {{ room.fullName }}
                <span v-if="room.capacity" class="tree-cap">{{ room.capacity }}人</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="classroomTree.length === 0" class="tree-empty">暂无教室数据</div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { orgUnitApi } from '@/api/organization'
import { http as request } from '@/utils/request'

const props = defineProps<{
  mode: 'class' | 'teacher' | 'classroom'
  semesterId?: number | string
  multiple?: boolean
}>()

const emit = defineEmits<{
  select: [node: { type: string; id: number | string; name: string; classIds?: (number | string)[] }]
  multiSelect: [items: { id: number | string; name: string }[]]
}>()

// 单选状态
const selectedId = ref<number | string | null>(null)
// 多选状态
const selectedIdSet = ref<Set<number | string>>(new Set())
const selectedNodes = ref<Map<number | string, { id: number | string; name: string }>>(new Map())

const classTree = ref<any[]>([])
const teacherTree = ref<{ name: string; teachers: { id: number; name: string }[] }[]>([])
const classroomTree = ref<any[]>([])

const modeLabel = computed(() => ({ class: '组织筛选', teacher: '教师列表', classroom: '教室列表' }[props.mode]))
const leafSelectedCount = computed(() => selectedIdSet.value.size)

// ==================== Class Tree ====================

async function loadClassTree() {
  try {
    const tree = await orgUnitApi.getTree()
    const data = Array.isArray(tree) ? tree : (tree as any).data || []
    const depts: any[] = []
    function extractDepts(nodes: any[]) {
      for (const n of nodes) {
        if (n.unitType === 'DEPARTMENT') {
          depts.push({
            id: n.id, name: n.unitName,
            children: (n.children || []).filter((g: any) => g.unitType === 'GRADE').map((g: any) => ({
              id: g.id, name: g.unitName,
              children: (g.children || []).filter((c: any) => c.unitType === 'CLASS').map((c: any) => ({
                id: c.id, name: c.unitName,
              })),
            })),
          })
        } else if (n.children) {
          extractDepts(n.children)
        }
      }
    }
    extractDepts(data)
    classTree.value = depts
  } catch { classTree.value = [] }
}

// ==================== Teacher Tree ====================

async function loadTeacherTree() {
  try {
    const res = await request.get('/teaching/schedule-teachers')
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : []

    const groups: Record<string, { id: number; name: string }[]> = {}
    for (const t of items) {
      const dept = t.deptName || '未分配'
      if (!groups[dept]) groups[dept] = []
      groups[dept].push({ id: t.id, name: t.realName || t.username })
    }
    teacherTree.value = Object.entries(groups).map(([name, tList]) => ({
      name, teachers: tList.sort((a, b) => a.name.localeCompare(b.name, 'zh'))
    }))
  } catch { teacherTree.value = [] }
}

// ==================== Classroom Tree ====================

function buildShortName(buildingName: string): string {
  const m = buildingName.match(/第([一二三四五六七八九十]+)/)
  if (m) return m[1] + '教'
  return buildingName.replace(/教学楼$/, '').substring(0, 2)
}

async function loadClassroomTree() {
  try {
    const res = await request.get('/places', { params: { pageSize: 500 } })
    const data = (res as any).data || res
    const allItems: any[] = Array.isArray(data) ? data : data.list || data.records || []
    const buildings = allItems.filter((p: any) => p.level === 1 && /教学/.test(p.placeName || ''))
    const tree: any[] = []
    for (const bldg of buildings) {
      const shortName = buildShortName(bldg.placeName)
      const floors = allItems.filter((p: any) => String(p.parentId) === String(bldg.id) && p.level === 2)
        .sort((a: any, b: any) => (a.placeName || '').localeCompare(b.placeName || ''))
      const floorNodes: any[] = []
      for (const floor of floors) {
        const rooms = allItems.filter((p: any) => String(p.parentId) === String(floor.id) && p.capacity > 0)
          .sort((a: any, b: any) => (a.placeName || '').localeCompare(b.placeName || ''))
        if (rooms.length > 0) {
          floorNodes.push({
            id: floor.id,
            name: floor.placeName.replace(bldg.placeName, '').trim() || floor.placeName,
            children: rooms.map((r: any) => ({
              id: r.id, name: r.placeName,
              fullName: r.placeCode || r.place_code || (shortName + '-' + (r.placeName || '').replace(/教室$/, '')),
              capacity: r.capacity,
            })),
          })
        }
      }
      if (floorNodes.length > 0) {
        tree.push({ id: bldg.id, name: bldg.placeName, children: floorNodes })
      }
    }
    classroomTree.value = tree
  } catch { classroomTree.value = [] }
}

// ==================== Selection - Single Mode ====================

function collectIds(node: any): (number | string)[] {
  const ids: (number | string)[] = []
  function walk(n: any) {
    if (!n.children || n.children.length === 0) { ids.push(n.id); return }
    for (const c of n.children) walk(c)
  }
  walk(node)
  return ids
}

function selectNodeSingle(id: number | string, name: string, type: string, classIds?: (number | string)[]) {
  selectedId.value = id
  emit('select', { type, id, name, classIds: classIds || [id] })
}

// ==================== Selection - Multi Mode ====================

function collectLeaves(node: any): { id: number | string; name: string }[] {
  const leaves: { id: number | string; name: string }[] = []
  function walk(n: any) {
    if (!n.children || n.children.length === 0) {
      leaves.push({ id: n.id, name: n.fullName || n.name })
      return
    }
    for (const c of n.children) walk(c)
  }
  walk(node)
  return leaves
}

function isNodeSelected(node: any): boolean {
  const leaves = collectLeaves(node)
  return leaves.length > 0 && leaves.every(l => selectedIdSet.value.has(l.id))
}

function isNodeIndeterminate(node: any): boolean {
  const leaves = collectLeaves(node)
  const checked = leaves.filter(l => selectedIdSet.value.has(l.id))
  return checked.length > 0 && checked.length < leaves.length
}

function isTeacherDeptSelected(dept: any): boolean {
  return dept.teachers.length > 0 && dept.teachers.every((t: any) => selectedIdSet.value.has(t.id))
}
function isTeacherDeptIndeterminate(dept: any): boolean {
  const checked = dept.teachers.filter((t: any) => selectedIdSet.value.has(t.id))
  return checked.length > 0 && checked.length < dept.teachers.length
}
function toggleTeacherDept(dept: any) {
  const allSelected = isTeacherDeptSelected(dept)
  if (allSelected) {
    dept.teachers.forEach((t: any) => { selectedIdSet.value.delete(t.id); selectedNodes.value.delete(t.id) })
  } else {
    dept.teachers.forEach((t: any) => {
      selectedIdSet.value.add(t.id)
      selectedNodes.value.set(t.id, { id: t.id, name: t.name })
    })
  }
  selectedIdSet.value = new Set(selectedIdSet.value)
  emitMulti()
}

function toggleBranch(node: any) {
  const leaves = collectLeaves(node)
  const allSelected = isNodeSelected(node)
  if (allSelected) {
    leaves.forEach(l => { selectedIdSet.value.delete(l.id); selectedNodes.value.delete(l.id) })
  } else {
    leaves.forEach(l => {
      selectedIdSet.value.add(l.id)
      selectedNodes.value.set(l.id, l)
    })
  }
  selectedIdSet.value = new Set(selectedIdSet.value)
  emitMulti()
}

function toggleLeaf(id: number | string, name: string) {
  if (selectedIdSet.value.has(id)) {
    selectedIdSet.value.delete(id)
    selectedNodes.value.delete(id)
  } else {
    selectedIdSet.value.add(id)
    selectedNodes.value.set(id, { id, name })
  }
  selectedIdSet.value = new Set(selectedIdSet.value)
  emitMulti()
}

function emitMulti() {
  emit('multiSelect', Array.from(selectedNodes.value.values()))
}

// ==================== Unified Click Handlers ====================

function handleNodeClick(node: any, type: string) {
  if (props.multiple) {
    toggleBranch(node)
  } else {
    selectNodeSingle(node.id, node.name, type, collectIds(node))
  }
}

function handleLeafClick(node: { id: number | string; name: string }, type: string) {
  if (props.multiple) {
    toggleLeaf(node.id, node.name)
  } else {
    selectNodeSingle(node.id, node.name, type)
  }
}

function clearSelection() {
  selectedId.value = null
  selectedIdSet.value = new Set()
  selectedNodes.value = new Map()
  if (props.multiple) emitMulti()
  else emit('select', { type: '', id: '', name: '' })
}

// ==================== Lifecycle ====================

function loadData() {
  if (props.mode === 'class') loadClassTree()
  else if (props.mode === 'teacher') loadTeacherTree()
  else if (props.mode === 'classroom') loadClassroomTree()
}

watch(() => props.mode, () => {
  selectedId.value = null
  selectedIdSet.value = new Set()
  selectedNodes.value = new Map()
  loadData()
}, { immediate: true })

watch(() => props.semesterId, () => loadData())
</script>

<style scoped>
.sched-tree {
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  height: 100%;
}
.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px 8px;
  border-bottom: 1px solid #f3f4f6;
}
.tree-title { font-size: 13px; font-weight: 600; color: #111827; }
.tree-count { font-size: 11px; color: #2563eb; font-weight: 500; }
.tree-clear {
  font-size: 11px; color: #6b7280; cursor: pointer;
  background: none; border: none; padding: 2px 6px;
}
.tree-clear:hover { color: #2563eb; }
.tree-body { padding: 8px 0; flex: 1; overflow-y: auto; }
.tree-node {
  padding: 5px 10px;
  font-size: 12px;
  cursor: pointer;
  color: #374151;
  border-radius: 4px;
  margin: 1px 4px;
  transition: all 0.1s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tree-node:hover { background: #f3f4f6; }
.tree-node.active { background: #eff6ff; color: #2563eb; font-weight: 500; }
.tree-node.selected { background: #eff6ff; color: #2563eb; }
.tree-l1 { font-weight: 600; font-size: 12.5px; color: #111827; }
.tree-l2 { color: #6b7280; font-size: 11.5px; }
.tree-l3 { color: #374151; font-size: 11.5px; }
.tree-cap { font-size: 10px; color: #9ca3af; margin-left: auto; }
.tree-empty { padding: 20px; color: #9ca3af; font-size: 12px; text-align: center; }

/* 多选 checkbox 样式 */
.tree-check {
  display: inline-block;
  width: 14px; height: 14px;
  border: 1.5px solid #d1d5db;
  border-radius: 3px;
  background: #fff;
  position: relative;
  flex-shrink: 0;
}
.tree-check.checked {
  background: #2563eb;
  border-color: #2563eb;
}
.tree-check.checked::after {
  content: '';
  position: absolute;
  left: 3px; top: 0;
  width: 4px; height: 8px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
.tree-check.indeterminate {
  background: #2563eb;
  border-color: #2563eb;
}
.tree-check.indeterminate::after {
  content: '';
  position: absolute;
  left: 2px; top: 5px;
  width: 8px; height: 2px;
  background: #fff;
}
</style>
