/**
 * V62 检查平台 - 模板（根分区）Store
 *
 * "模板" = 根 TemplateSection。此 store 管理根分区列表、
 * 当前选中的根分区、分区树、版本、分类和选项集。
 */
import type { LongId } from '@/types/common'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  TemplateSection,
  TemplateVersion,
  TemplateCatalog,
  TemplateCatalogTreeNode,
  ResponseSet,
  CreateRootSectionRequest,
  UpdateRootSectionRequest,
  InspPageResult,
} from '@/types/insp/template'
import type { TemplateStatus } from '@/types/insp/enums'
import {
  listRootSections,
  getRootSection,
  createRootSection,
  updateRootSection as updateRootSectionApi,
  deleteRootSection as deleteRootSectionApi,
  publishRootSection as publishRootSectionApi,
  deprecateRootSection as deprecateRootSectionApi,
  archiveRootSection as archiveRootSectionApi,
  duplicateRootSection as duplicateRootSectionApi,
  getVersions,
  getSections,
} from '@/api/inspection/template'
import { getCatalogs, getCatalogTree } from '@/api/inspection/catalog'
import { getResponseSets } from '@/api/inspection/responseSet'

export const useInspTemplateStore = defineStore('inspTemplate', () => {
  // State
  const catalogs = ref<TemplateCatalog[]>([])
  const catalogTree = ref<TemplateCatalogTreeNode[]>([])
  const rootSections = ref<TemplateSection[]>([])
  const currentRootSection = ref<TemplateSection | null>(null)
  const sectionTree = ref<TemplateSection[]>([])
  const versions = ref<TemplateVersion[]>([])
  const responseSets = ref<ResponseSet[]>([])
  const totalRootSections = ref(0)

  // Actions

  async function loadRootSections(params?: {
    page?: number
    size?: number
    status?: TemplateStatus
    catalogId?: LongId
    keyword?: string
  }): Promise<InspPageResult<TemplateSection>> {
    const result = await listRootSections(params)
    rootSections.value = result.records
    totalRootSections.value = result.total
    return result
  }

  async function loadRootSection(id: LongId): Promise<TemplateSection> {
    const section = await getRootSection(id)
    currentRootSection.value = section
    return section
  }

  async function addRootSection(data: CreateRootSectionRequest): Promise<TemplateSection> {
    return await createRootSection(data)
  }

  async function editRootSection(id: LongId, data: UpdateRootSectionRequest): Promise<void> {
    await updateRootSectionApi(id, data)
  }

  async function removeRootSection(id: LongId): Promise<void> {
    await deleteRootSectionApi(id)
  }

  async function publish(id: LongId): Promise<TemplateVersion> {
    return await publishRootSectionApi(id)
  }

  async function deprecate(id: LongId): Promise<void> {
    await deprecateRootSectionApi(id)
  }

  async function archive(id: LongId): Promise<void> {
    await archiveRootSectionApi(id)
  }

  async function duplicate(id: LongId): Promise<TemplateSection> {
    return await duplicateRootSectionApi(id)
  }

  async function loadVersions(rootSectionId: LongId): Promise<TemplateVersion[]> {
    const list = await getVersions(rootSectionId)
    versions.value = list
    return list
  }

  async function loadSectionTree(rootSectionId: LongId): Promise<TemplateSection[]> {
    const list = await getSections(rootSectionId)
    sectionTree.value = list
    return list
  }

  async function loadCatalogs(): Promise<TemplateCatalog[]> {
    const list = await getCatalogs()
    catalogs.value = list
    return list
  }

  async function loadCatalogTree(): Promise<TemplateCatalogTreeNode[]> {
    const tree = await getCatalogTree()
    catalogTree.value = tree
    return tree
  }

  async function loadResponseSets(): Promise<ResponseSet[]> {
    const result = await getResponseSets({ page: 1, size: 1000 })
    responseSets.value = result.records
    return result.records
  }

  return {
    // State
    catalogs,
    catalogTree,
    rootSections,
    currentRootSection,
    sectionTree,
    versions,
    responseSets,
    totalRootSections,
    // Aliases for backward compat (views that still reference 'templates')
    get templates() { return rootSections.value },
    get currentTemplate() { return currentRootSection.value },
    get totalTemplates() { return totalRootSections.value },
    // Actions
    loadRootSections,
    loadRootSection,
    addRootSection,
    editRootSection,
    removeRootSection,
    publish,
    deprecate,
    archive,
    duplicate,
    loadVersions,
    loadSectionTree,
    loadCatalogs,
    loadCatalogTree,
    loadResponseSets,
    // Aliases for backward compat
    loadTemplates: loadRootSections,
    loadTemplate: loadRootSection,
    addTemplate: addRootSection,
    editTemplate: editRootSection,
    removeTemplate: removeRootSection,
  }
})
