import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/utils/request'
import { semesterApi } from '@/api/calendar'

interface CachedItem<T> {
  data: T
  timestamp: number
}

const CACHE_TTL = 5 * 60 * 1000 // 5 minutes

export const useSharedDataStore = defineStore('sharedData', () => {
  // Cached data
  const semesters = ref<CachedItem<any[]> | null>(null)
  const currentSemester = ref<any | null>(null)
  const orgTree = ref<CachedItem<any[]> | null>(null)
  const classList = ref<CachedItem<any[]> | null>(null)
  const courseList = ref<CachedItem<any[]> | null>(null)
  const majorList = ref<CachedItem<any[]> | null>(null)
  const teacherList = ref<CachedItem<any[]> | null>(null)

  function isExpired(item: CachedItem<any> | null): boolean {
    if (!item) return true
    return Date.now() - item.timestamp > CACHE_TTL
  }

  // Semesters
  async function getSemesters(force = false): Promise<any[]> {
    if (!force && !isExpired(semesters.value)) return semesters.value!.data
    const data = await semesterApi.list()
    semesters.value = { data, timestamp: Date.now() }
    // Also set current semester
    const current = data.find((s: any) => s.isCurrent)
    if (current) currentSemester.value = current
    return data
  }

  async function getCurrentSemester(force = false): Promise<any> {
    if (!force && currentSemester.value) return currentSemester.value
    await getSemesters(force)
    return currentSemester.value
  }

  // Org tree
  async function getOrgTree(force = false): Promise<any[]> {
    if (!force && !isExpired(orgTree.value)) return orgTree.value!.data
    const data = await http.get('/org-units/tree')
    orgTree.value = { data, timestamp: Date.now() }
    return data
  }

  // Flat org list (departments)
  async function getDepartments(force = false): Promise<any[]> {
    const tree = await getOrgTree(force)
    // Flatten tree to list
    const result: any[] = []
    const flatten = (nodes: any[]) => {
      for (const n of nodes) {
        result.push({ id: n.id, name: n.unitName || n.name, level: n.treeLevel })
        if (n.children?.length) flatten(n.children)
      }
    }
    flatten(Array.isArray(tree) ? tree : [])
    return result
  }

  // Class list
  async function getClassList(force = false): Promise<any[]> {
    if (!force && !isExpired(classList.value)) return classList.value!.data
    const res = await http.get('/students/classes', { params: { page: 1, size: 500 } })
    const data = res?.records || res?.data?.records || (Array.isArray(res) ? res : [])
    classList.value = { data, timestamp: Date.now() }
    return data
  }

  // Course list (all)
  async function getCourseList(force = false): Promise<any[]> {
    if (!force && !isExpired(courseList.value)) return courseList.value!.data
    const { courseApi } = await import('@/api/academic')
    const data = await courseApi.listAll()
    courseList.value = { data: Array.isArray(data) ? data : [], timestamp: Date.now() }
    return courseList.value.data
  }

  // Major list (enabled)
  async function getMajorList(force = false): Promise<any[]> {
    if (!force && !isExpired(majorList.value)) return majorList.value!.data
    const { majorApi } = await import('@/api/academic')
    const data = await majorApi.getAllEnabled()
    majorList.value = { data: Array.isArray(data) ? data : [], timestamp: Date.now() }
    return majorList.value.data
  }

  // Teacher list
  async function getTeacherList(force = false): Promise<any[]> {
    if (!force && !isExpired(teacherList.value)) return teacherList.value!.data
    const data = await http.get('/users', { params: { role: 'TEACHER', size: 500 } })
    const items = data?.records || (Array.isArray(data) ? data : [])
    teacherList.value = { data: items, timestamp: Date.now() }
    return teacherList.value.data
  }

  // Invalidate specific cache
  function invalidate(key: 'semesters' | 'orgTree' | 'classList' | 'courseList' | 'majorList' | 'teacherList') {
    switch (key) {
      case 'semesters': semesters.value = null; currentSemester.value = null; break
      case 'orgTree': orgTree.value = null; break
      case 'classList': classList.value = null; break
      case 'courseList': courseList.value = null; break
      case 'majorList': majorList.value = null; break
      case 'teacherList': teacherList.value = null; break
    }
  }

  // Invalidate all
  function invalidateAll() {
    semesters.value = null
    currentSemester.value = null
    orgTree.value = null
    classList.value = null
    courseList.value = null
    majorList.value = null
    teacherList.value = null
  }

  return {
    getSemesters, getCurrentSemester,
    getOrgTree, getDepartments,
    getClassList, getCourseList, getMajorList, getTeacherList,
    invalidate, invalidateAll,
  }
})
