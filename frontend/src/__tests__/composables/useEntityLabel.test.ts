import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useEntityLabel, getEntityLabel, entityLabel } from '@/composables/useEntityLabel'
import { usePluginsStore } from '@/stores/plugins'

describe('useEntityLabel composable', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('CORE 兜底 (无插件加载)', () => {
    it('subject="主体" / group="组织"', () => {
      const labels = getEntityLabel()
      expect(labels.subject).toBe('主体')
      expect(labels.group).toBe('组织')
      expect(labels.parent).toBe('父级组织')
      expect(labels.place).toBe('场所')
      expect(labels.campus).toBe('机构')
      expect(labels.organizer).toBe('负责人')
      expect(labels.evaluator).toBe('检查员')
    })
  })

  describe('EDU 启用 → 学校预设', () => {
    beforeEach(() => {
      usePluginsStore().markLoaded('EDU')
    })

    it('subject="学生" / group="班级"', () => {
      const labels = getEntityLabel()
      expect(labels.subject).toBe('学生')
      expect(labels.group).toBe('班级')
      expect(labels.parent).toBe('年级')
      expect(labels.place).toBe('宿舍')
      expect(labels.campus).toBe('学校')
      expect(labels.organizer).toBe('班主任')
      expect(labels.evaluator).toBe('教师')
    })

    it('useEntityLabel() 返回响应式 ref', () => {
      const { subject, group } = useEntityLabel()
      expect(subject.value).toBe('学生')
      expect(group.value).toBe('班级')
    })
  })

  describe('HEALTH 启用 → 医院预设', () => {
    beforeEach(() => {
      usePluginsStore().markLoaded('HEALTH')
    })

    it('subject="患者" / group="科室"', () => {
      const labels = getEntityLabel()
      expect(labels.subject).toBe('患者')
      expect(labels.group).toBe('科室')
      expect(labels.parent).toBe('医院')
      expect(labels.place).toBe('病房')
      expect(labels.organizer).toBe('主治医生')
      expect(labels.evaluator).toBe('护士')
    })
  })

  describe('EDU + HEALTH 同时启用 → EDU 优先', () => {
    it('当两个都启用时, 显示 EDU 预设', () => {
      const store = usePluginsStore()
      store.markLoaded('EDU')
      store.markLoaded('HEALTH')
      const labels = getEntityLabel()
      expect(labels.subject).toBe('学生')  // EDU 优先
      expect(labels.group).toBe('班级')
    })
  })

  describe('entityLabel(key) 静态读取', () => {
    it('读取单字段', () => {
      usePluginsStore().markLoaded('EDU')
      expect(entityLabel('subject')).toBe('学生')
      expect(entityLabel('group')).toBe('班级')
    })

    it('CORE 时也能读', () => {
      expect(entityLabel('subject')).toBe('主体')
    })
  })

  describe('响应式更新', () => {
    it('启用 EDU 后响应式 ref 自动更新', () => {
      const { subject } = useEntityLabel()
      expect(subject.value).toBe('主体')

      usePluginsStore().markLoaded('EDU')
      expect(subject.value).toBe('学生')

      usePluginsStore().markUnloaded('EDU')
      expect(subject.value).toBe('主体')
    })

    it('切换插件 (EDU → HEALTH) 标签同步切换', () => {
      const { subject, place } = useEntityLabel()
      const store = usePluginsStore()

      store.markLoaded('EDU')
      expect(subject.value).toBe('学生')
      expect(place.value).toBe('宿舍')

      store.markUnloaded('EDU')
      store.markLoaded('HEALTH')
      expect(subject.value).toBe('患者')
      expect(place.value).toBe('病房')
    })
  })
})
