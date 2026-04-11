<template>
  <div class="sched-tree">
    <div class="tree-header">
      <span class="tree-title">{{ modeLabel }}</span>
      <button v-if="selectedId" class="tree-clear" @click="clearSelection">清除</button>
    </div>
    <div class="tree-body">
      <!-- CLASS mode: dept → grade → class -->
      <template v-if="mode === 'class'">
        <div v-for="dept in classTree" :key="dept.id">
          <div class="tree-node tree-l1" :class="{ active: selectedId === dept.id }" @click="selectNode(dept.id, dept.name, 'DEPARTMENT', collectIds(dept))">{{ dept.name }}</div>
          <div v-for="grade in dept.children" :key="grade.id" style="padding-left: 14px;">
            <div class="tree-node tree-l2" :class="{ active: selectedId === grade.id }" @click="selectNode(grade.id, grade.name, 'GRADE', collectIds(grade))">{{ grade.name }}</div>
            <div v-for="cls in grade.children" :key="cls.id" style="padding-left: 14px;">
              <div class="tree-node tree-l3" :class="{ active: selectedId === cls.id }" @click="selectNode(cls.id, cls.name, 'CLASS')">{{ cls.name }}</div>
            </div>
          </div>
        </div>
      </template>

      <!-- TEACHER mode: dept → teachers -->
      <template v-if="mode === 'teacher'">
        <div v-for="dept in teacherTree" :key="dept.name">
          <div class="tree-node tree-l1">{{ dept.name }}</div>
          <div v-for="t in dept.teachers" :key="t.id" style="padding-left: 14px;">
            <div class="tree-node tree-l3" :class="{ active: selectedId === t.id }" @click="selectNode(t.id, t.name, 'TEACHER')">{{ t.name }}</div>
          </div>
        </div>
        <div v-if="teacherTree.length === 0" class="tree-empty">暂无教师数据</div>
      </template>

      <!-- CLASSROOM mode: building → floor → room -->
      <template v-if="mode === 'classroom'">
        <div v-for="bldg in classroomTree" :key="bldg.id">
          <div class="tree-node tree-l1">{{ bldg.name }}</div>
          <div v-for="floor in bldg.children" :key="floor.id" style="padding-left: 14px;">
            <div class="tree-node tree-l2">{{ floor.name }}</div>
            <div v-for="room in floor.children" :key="room.id" style="padding-left: 14px;">
              <div class="tree-node tree-l3" :class="{ active: selectedId === room.id }" @click="selectNode(room.id, room.fullName, 'CLASSROOM')">
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
import { ref, computed, watch, onMounted } from 'vue'
import { orgUnitApi } from '@/api/organization'
import { http as request } from '@/utils/request'

const props = defineProps<{
  mode: 'class' | 'teacher' | 'classroom'
  semesterId?: number | string
}>()

const emit = defineEmits<{
  select: [node: { type: string; id: number | string; name: string; classIds?: (number | string)[] }]
}>()

const selectedId = ref<number | string | null>(null)
const classTree = ref<any[]>([])
const teacherTree = ref<{ name: string; teachers: { id: number; name: string }[] }[]>([])
const classroomTree = ref<any[]>([])

const modeLabel = computed(() => ({ class: '组织筛选', teacher: '教师列表', classroom: '教室列表' }[props.mode]))

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
    // Load all users, filter TEACHER type
    const res = await request.get('/users', { params: { pageNum: 1, pageSize: 1000 } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || data.list || []
    const teachers = items.filter((u: any) => u.userType === 'TEACHER')

    if (teachers.length === 0) { teacherTree.value = []; return }

    // Load org tree to get dept names for teacher grouping
    let deptMap: Record<string, string> = {} // orgUnitId → deptName
    try {
      const tree = await orgUnitApi.getTree()
      const treeData = Array.isArray(tree) ? tree : (tree as any).data || []
      function walkDepts(nodes: any[], deptName?: string) {
        for (const n of nodes) {
          if (n.unitType === 'DEPARTMENT') deptMap[String(n.id)] = n.unitName
          // Propagate dept name to children
          if (n.children) walkDepts(n.children, n.unitType === 'DEPARTMENT' ? n.unitName : deptName)
          if (deptName) deptMap[String(n.id)] = deptName
        }
      }
      walkDepts(treeData)
    } catch { /* */ }

    // Match teachers to depts via access_relations or primaryOrgUnitId
    // Since API doesn't return primaryOrgUnitId, try DB mapping
    // Fallback: query access_relations for each teacher
    // Simpler: just load from /users/with-departments if available, or group alphabetically

    // Try to match by employeeNo pattern or just list all
    const groups: Record<string, { id: number; name: string }[]> = {}
    for (const t of teachers) {
      // Try to guess dept from user data - we'll use a simple grouping
      const dept = '全部教师'
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

    // Find teaching buildings (level 1, name contains 教学)
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
              id: r.id,
              name: r.placeName,
              fullName: shortName + '-' + (r.placeName || '').replace(/教室$/, ''),
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

// ==================== Selection ====================

function collectIds(node: any): (number | string)[] {
  const ids: (number | string)[] = []
  function walk(n: any) {
    if (!n.children || n.children.length === 0) { ids.push(n.id); return }
    for (const c of n.children) walk(c)
  }
  walk(node)
  return ids
}

function selectNode(id: number | string, name: string, type: string, classIds?: (number | string)[]) {
  selectedId.value = id
  emit('select', { type, id, name, classIds: classIds || [id] })
}

function clearSelection() {
  selectedId.value = null
  emit('select', { type: '', id: '', name: '' })
}

// ==================== Lifecycle ====================

function loadData() {
  if (props.mode === 'class') loadClassTree()
  else if (props.mode === 'teacher') loadTeacherTree()
  else if (props.mode === 'classroom') loadClassroomTree()
}

watch(() => props.mode, () => { selectedId.value = null; loadData() })
watch(() => props.semesterId, () => loadData())
onMounted(loadData)
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
  justify-content: space-between;
}
.tree-node:hover { background: #f3f4f6; }
.tree-node.active { background: #eff6ff; color: #2563eb; font-weight: 500; }
.tree-l1 { font-weight: 600; font-size: 12.5px; color: #111827; cursor: default; }
.tree-l2 { color: #6b7280; font-size: 11.5px; }
.tree-l3 { color: #374151; font-size: 11.5px; }
.tree-cap { font-size: 10px; color: #9ca3af; margin-left: 4px; }
.tree-empty { padding: 20px; color: #9ca3af; font-size: 12px; text-align: center; }
</style>
