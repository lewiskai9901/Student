<template>
  <div class="flex h-full bg-gray-50">
    <!-- Left Sidebar -->
    <div class="flex w-[280px] flex-shrink-0 flex-col border-r border-gray-200 bg-white">
      <div class="flex-shrink-0 border-b border-gray-200 px-4 py-3">
        <h2 class="text-sm font-semibold text-gray-900">类型层级</h2>
      </div>

      <div class="flex-shrink-0 px-3 py-3">
        <div class="relative">
          <Search class="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索类型..."
            class="h-8 w-full rounded-md border border-gray-300 bg-gray-50 pl-8 pr-3 text-sm text-gray-900 placeholder-gray-400 transition-colors focus:border-blue-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
      </div>

      <div class="flex-1 overflow-y-auto px-2 pb-3">
        <div v-if="loading" class="flex items-center justify-center py-10">
          <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
        </div>
        <div v-else-if="filteredTree.length > 0">
          <TypeSidebarNode
            v-for="node in filteredTree"
            :key="node.id"
            :node="node"
            :level="0"
            :selected-id="selectedId"
            :search-keyword="searchKeyword"
            @select="handleSelectNode"
          />
        </div>
        <div v-else class="flex flex-col items-center py-10 text-gray-400">
          <MapPin class="h-8 w-8" />
          <p class="mt-2 text-xs">
            {{ searchKeyword ? '未找到匹配的类型' : '暂无类型数据' }}
          </p>
        </div>
      </div>

      <div class="flex-shrink-0 border-t border-gray-200 px-3 py-3">
        <button
          class="flex h-9 w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-gray-300 text-sm font-medium text-gray-600 transition-colors hover:border-blue-400 hover:bg-blue-50 hover:text-blue-600"
          @click="handleAdd()"
        >
          <Plus class="h-4 w-4" />
          新增类型
        </button>
      </div>
    </div>

    <!-- Right Content Panel -->
    <div class="flex flex-1 flex-col overflow-hidden">
      <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
        <div>
          <h1 class="text-lg font-semibold text-gray-900">场所类型配置</h1>
          <p class="mt-0.5 text-sm text-gray-500">配置场所类型的分类、行为特征和层级关系</p>
        </div>
        <button
          @click="loadData"
          class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          刷新
        </button>
      </div>

      <!-- Stat Bar -->
      <div class="flex items-center gap-4 border-b border-gray-200 bg-white px-6 py-2.5">
        <span class="text-sm text-gray-500">总数</span>
        <span class="text-sm font-semibold text-gray-900">{{ allFlatTypes.length }}</span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">启用</span>
        <span class="text-sm font-semibold text-gray-900">{{ enabledCount }}</span>
        <div class="h-3 w-px bg-gray-200" />
        <span class="text-sm text-gray-500">禁用</span>
        <span class="text-sm font-semibold text-gray-900">{{ disabledCount }}</span>
      </div>

      <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
        <!-- Detail Panel (when type selected) -->
        <div v-if="selectedType" class="space-y-5">
          <!-- Type Header Card -->
          <div class="rounded-xl border border-gray-200 bg-white p-5">
            <div class="flex items-start justify-between">
              <div class="flex items-center gap-3">
                <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50">
                  <MapPin class="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <h2 class="text-base font-semibold text-gray-900">{{ selectedType.typeName }}</h2>
                    <code class="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-xs text-gray-500">{{ selectedType.typeCode }}</code>
                    <span
                      v-if="selectedType.category"
                      class="rounded px-1.5 py-0.5 text-xs font-medium"
                      :class="categoryColor(selectedType.category)"
                    >
                      {{ categoryLabel(selectedType.category) }}
                    </span>
                    <span v-if="selectedType.system" class="flex items-center gap-0.5 rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">
                      <Lock class="h-3 w-3" />
                      预置
                    </span>
                  </div>
                  <p v-if="selectedType.description" class="mt-0.5 text-sm text-gray-500">{{ selectedType.description }}</p>
                  <p v-else class="mt-0.5 text-sm text-gray-400">暂无描述</p>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <button
                  class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
                  @click="handleEdit(selectedType)"
                >
                  <Pencil class="h-3.5 w-3.5" />
                  编辑
                </button>
                <button
                  v-if="!selectedType.system"
                  class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 bg-white px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50"
                  @click="handleDelete(selectedType)"
                >
                  <Trash2 class="h-3.5 w-3.5" />
                  删除
                </button>
              </div>
            </div>
          </div>

          <!-- Properties Grid -->
          <div class="grid grid-cols-2 gap-4">
            <!-- Hierarchy -->
            <div class="rounded-xl border border-gray-200 bg-white p-4">
              <h3 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">层级配置</h3>
              <div class="space-y-2.5">
                <div v-if="selectedType.maxDepth" class="flex items-center justify-between">
                  <span class="text-sm text-gray-500">最大深度</span>
                  <span class="text-sm text-gray-900">{{ selectedType.maxDepth }}</span>
                </div>
                <div v-if="selectedType.allowedChildTypeCodes?.length">
                  <span class="text-sm text-gray-500">允许的子类型</span>
                  <div class="mt-1.5 flex flex-wrap gap-1">
                    <span
                      v-for="code in selectedType.allowedChildTypeCodes"
                      :key="code"
                      class="rounded bg-blue-50 px-1.5 py-0.5 text-xs text-blue-600"
                    >
                      {{ resolveTypeName(code) }}
                    </span>
                  </div>
                </div>
                <div v-if="childTypes.length > 0">
                  <span class="text-sm text-gray-500">当前子类型</span>
                  <div class="mt-1.5 flex flex-wrap gap-1">
                    <span
                      v-for="child in childTypes"
                      :key="child.id"
                      class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600"
                    >
                      {{ child.typeName }}
                    </span>
                  </div>
                </div>
                <div v-if="!selectedType.maxDepth && !selectedType.allowedChildTypeCodes?.length" class="text-sm text-gray-400">
                  暂无层级配置
                </div>
              </div>
            </div>

            <!-- Features & Status -->
            <div class="rounded-xl border border-gray-200 bg-white p-4">
              <h3 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">属性</h3>
              <div class="space-y-2.5">
                <div class="flex items-center justify-between">
                  <span class="text-sm text-gray-500">状态</span>
                  <button
                    class="relative inline-flex h-5 w-9 items-center rounded-full transition-colors cursor-pointer"
                    :class="selectedType.enabled ? 'bg-blue-600' : 'bg-gray-300'"
                    @click="handleToggleStatus(selectedType)"
                  >
                    <span
                      class="inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform"
                      :class="selectedType.enabled ? 'translate-x-4' : 'translate-x-0.5'"
                    />
                  </button>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-sm text-gray-500">排序号</span>
                  <span class="text-sm text-gray-900">{{ selectedType.sortOrder }}</span>
                </div>
                <div v-if="selectedType.capacityUnit" class="flex items-center justify-between">
                  <span class="text-sm text-gray-500">容量单位</span>
                  <span class="text-sm text-gray-900">{{ selectedType.capacityUnit }}</span>
                </div>
                <div v-if="selectedType.defaultCapacity" class="flex items-center justify-between">
                  <span class="text-sm text-gray-500">默认容量</span>
                  <span class="text-sm text-gray-900">{{ selectedType.defaultCapacity }}</span>
                </div>
                <div v-if="activeFeatures.length > 0">
                  <span class="text-sm text-gray-500">行为特征</span>
                  <div class="mt-1.5 flex flex-wrap gap-1">
                    <span
                      v-for="feat in activeFeatures"
                      :key="feat.key"
                      class="rounded bg-emerald-50 px-1.5 py-0.5 text-xs text-emerald-600"
                    >
                      {{ feat.label }}
                    </span>
                  </div>
                </div>
                <div v-else class="flex items-center justify-between">
                  <span class="text-sm text-gray-500">行为特征</span>
                  <span class="text-xs text-gray-400">无</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Cross References -->
          <div
            v-if="(selectedType.defaultUserTypeCodes?.length || 0) + (selectedType.defaultOrgTypeCodes?.length || 0) > 0"
            class="rounded-xl border border-gray-200 bg-white p-4"
          >
            <h3 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">关联类型</h3>
            <div class="space-y-2.5">
              <div v-if="selectedType.defaultUserTypeCodes?.length">
                <span class="text-sm text-gray-500">关联用户类型</span>
                <div class="mt-1.5 flex flex-wrap gap-1">
                  <span
                    v-for="code in selectedType.defaultUserTypeCodes"
                    :key="code"
                    class="rounded bg-violet-50 px-1.5 py-0.5 text-xs text-violet-600"
                  >
                    {{ resolveCrossTypeName(code, userTypes) }}
                  </span>
                </div>
              </div>
              <div v-if="selectedType.defaultOrgTypeCodes?.length">
                <span class="text-sm text-gray-500">关联组织类型</span>
                <div class="mt-1.5 flex flex-wrap gap-1">
                  <span
                    v-for="code in selectedType.defaultOrgTypeCodes"
                    :key="code"
                    class="rounded bg-orange-50 px-1.5 py-0.5 text-xs text-orange-600"
                  >
                    {{ resolveCrossTypeName(code, orgTypes) }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Quick Actions -->
          <div v-if="selectedType.allowedChildTypeCodes?.length" class="rounded-xl border border-gray-200 bg-white p-4">
            <h3 class="mb-3 text-xs font-semibold uppercase tracking-wider text-gray-400">快捷操作</h3>
            <button
              class="inline-flex items-center gap-1.5 rounded-lg border border-dashed border-gray-300 px-3 py-2 text-sm font-medium text-gray-600 hover:border-blue-400 hover:bg-blue-50 hover:text-blue-600 transition-colors"
              @click="handleAdd(selectedType.typeCode)"
            >
              <Plus class="h-4 w-4" />
              添加子类型
            </button>
          </div>
        </div>

        <!-- Empty state (no selection) -->
        <div v-else class="flex flex-col items-center justify-center py-20 text-gray-400">
          <MapPin class="h-12 w-12 text-gray-300" />
          <h3 class="mt-4 text-sm font-semibold text-gray-500">请从左侧选择一个类型</h3>
          <p class="mt-1 text-sm text-gray-400">点击类型节点查看详情和编辑</p>
        </div>
      </div>
    </div>

    <!-- Edit Drawer -->
    <div v-if="dialogVisible" class="fixed inset-0 z-50 flex justify-end">
      <div class="absolute inset-0 bg-black/20" @click="dialogVisible = false" />
      <div class="relative w-[480px] bg-white shadow-xl flex flex-col animate-slide-in-right">
        <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
          <div>
            <h2 class="text-base font-semibold text-gray-900">{{ dialogTitle }}</h2>
            <p v-if="!isEdit && formData.parentTypeCode" class="mt-0.5 text-xs text-gray-400">
              将作为 {{ resolveTypeName(formData.parentTypeCode) }} 的子类型
            </p>
          </div>
          <button @click="dialogVisible = false" class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto px-6 py-5 space-y-5">
          <!-- Name -->
          <div>
            <label class="mb-1.5 block text-xs font-medium text-gray-500">类型名称</label>
            <input
              ref="nameInputRef"
              v-model="formData.typeName"
              placeholder="输入类型名称"
              maxlength="30"
              class="h-10 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-900 placeholder-gray-300 transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <!-- Code -->
          <div>
            <div class="flex items-center justify-between mb-1.5">
              <label class="text-xs font-medium text-gray-500">编码</label>
              <button
                v-if="!isEdit"
                @click="showCodeEdit = !showCodeEdit"
                class="text-[11px] text-gray-400 hover:text-blue-500 transition-colors"
              >
                {{ showCodeEdit ? '自动生成' : '自定义' }}
              </button>
            </div>
            <div v-if="!showCodeEdit || isEdit" class="flex items-center gap-2">
              <code class="rounded-md bg-gray-50 border border-gray-100 px-3 py-2 text-xs font-mono text-gray-500 flex-1 select-all">
                {{ formData.typeCode || '输入名称后自动生成' }}
              </code>
            </div>
            <input
              v-else
              v-model="formData.typeCode"
              placeholder="自定义编码 (大写英文)"
              maxlength="50"
              class="h-9 w-full rounded-lg border border-gray-200 bg-white px-3 font-mono text-xs text-gray-700 placeholder-gray-300 uppercase transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
            />
          </div>

          <!-- Description -->
          <div>
            <label class="mb-1.5 block text-xs font-medium text-gray-500">描述</label>
            <textarea
              v-model="formData.description"
              rows="2"
              placeholder="可选，简要描述该类型的用途"
              class="w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-300 transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 resize-none"
            />
          </div>

          <div class="border-t border-gray-100" />

          <!-- Category -->
          <div>
            <label class="mb-1.5 block text-xs font-medium text-gray-500">分类</label>
            <el-select
              v-model="formData.category"
              placeholder="选择分类"
              clearable
              style="width: 100%"
              :disabled="isEdit"
            >
              <el-option
                v-for="cat in availableCategories"
                :key="cat.code"
                :label="cat.label"
                :value="cat.code"
              >
                <span class="text-sm">{{ cat.label }}</span>
                <span class="ml-2 text-xs text-gray-400">{{ cat.code }}</span>
              </el-option>
            </el-select>
          </div>

          <!-- Numeric fields row -->
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="mb-1.5 block text-xs font-medium text-gray-500">排序</label>
              <el-input-number v-model="formData.sortOrder" :min="0" :max="999" size="default" controls-position="right" style="width: 100%" />
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-gray-500">最大深度</label>
              <el-input-number v-model="formData.maxDepth" :min="0" :max="10" size="default" controls-position="right" placeholder="不限" style="width: 100%" />
            </div>
          </div>

          <div class="border-t border-gray-100" />

          <!-- Features -->
          <div>
            <label class="mb-2 block text-xs font-medium text-gray-500">行为特征</label>
            <div class="space-y-2">
              <div
                v-for="(label, key) in PLACE_FEATURE_LABELS"
                :key="key"
                class="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2.5"
              >
                <div>
                  <span class="text-sm text-gray-700">{{ label }}</span>
                  <p class="text-[11px] text-gray-400 mt-0.5">{{ featureDescriptions[key] }}</p>
                </div>
                <el-switch v-model="formData.features[key]" />
              </div>
            </div>
          </div>

          <!-- Capacity (shown when hasCapacity is on) -->
          <div v-if="formData.features.hasCapacity" class="grid grid-cols-2 gap-3">
            <div>
              <label class="mb-1.5 block text-xs font-medium text-gray-500">容量单位</label>
              <el-select v-model="formData.capacityUnit" placeholder="单位" style="width: 100%">
                <el-option label="人" value="人" />
                <el-option label="床位" value="床位" />
                <el-option label="工位" value="工位" />
                <el-option label="平方米" value="平方米" />
              </el-select>
            </div>
            <div>
              <label class="mb-1.5 block text-xs font-medium text-gray-500">默认容量</label>
              <el-input-number v-model="formData.defaultCapacity" :min="0" size="default" controls-position="right" style="width: 100%" />
            </div>
          </div>

          <div class="border-t border-gray-100" />

          <!-- Allowed Child Types -->
          <div>
            <label class="mb-1.5 block text-xs font-medium text-gray-500">允许的子类型</label>
            <el-select
              v-model="formData.allowedChildTypeCodes"
              multiple
              placeholder="选择允许的子类型编码"
              style="width: 100%"
              clearable
            >
              <el-option
                v-for="type in allTypeCodesForChildSelect"
                :key="type.typeCode"
                :label="type.typeName"
                :value="type.typeCode"
              >
                <span class="text-sm">{{ type.typeName }}</span>
                <span class="ml-2 text-xs text-gray-400">{{ type.typeCode }}</span>
              </el-option>
            </el-select>
          </div>

          <!-- Cross-domain references -->
          <div>
            <label class="mb-1.5 block text-xs font-medium text-gray-500">关联用户类型</label>
            <el-select
              v-model="formData.defaultUserTypeCodes"
              multiple
              filterable
              placeholder="选择关联用户类型"
              style="width: 100%"
            >
              <template v-if="userTypes.length === 0" #empty>
                <p class="px-4 py-2 text-center text-xs text-gray-400">暂无可用类型</p>
              </template>
              <el-option
                v-for="ut in userTypes"
                :key="ut.typeCode"
                :label="ut.typeName"
                :value="ut.typeCode"
              >
                <span class="text-sm">{{ ut.typeName }}</span>
                <span class="ml-2 text-xs text-gray-400">{{ ut.typeCode }}</span>
              </el-option>
            </el-select>
          </div>
          <div>
            <label class="mb-1.5 block text-xs font-medium text-gray-500">关联组织类型</label>
            <el-select
              v-model="formData.defaultOrgTypeCodes"
              multiple
              filterable
              placeholder="选择关联组织类型"
              style="width: 100%"
            >
              <template v-if="orgTypes.length === 0" #empty>
                <p class="px-4 py-2 text-center text-xs text-gray-400">暂无可用类型</p>
              </template>
              <el-option
                v-for="ot in orgTypes"
                :key="ot.typeCode"
                :label="ot.typeName"
                :value="ot.typeCode"
              >
                <span class="text-sm">{{ ot.typeName }}</span>
                <span class="ml-2 text-xs text-gray-400">{{ ot.typeCode }}</span>
              </el-option>
            </el-select>
          </div>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-end gap-3 border-t border-gray-100 px-6 py-4">
          <button
            @click="dialogVisible = false"
            class="rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 transition"
          >
            取消
          </button>
          <button
            :disabled="submitLoading || !formData.typeName"
            @click="handleSubmit"
            class="rounded-lg bg-gray-900 px-5 py-2 text-sm font-medium text-white hover:bg-gray-800 transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <svg v-if="submitLoading" class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" class="opacity-25"/><path d="M4 12a8 8 0 018-8" stroke="currentColor" stroke-width="4" stroke-linecap="round" class="opacity-75"/></svg>
            {{ isEdit ? '保存' : '创建' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, defineComponent, h, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, RefreshCw, Pencil, Trash2,
  MapPin, Lock, ChevronRight, Search
} from 'lucide-vue-next'
import { universalPlaceTypeApi } from '@/api/universalPlaceType'
import { userTypeApi } from '@/api/userType'
import { orgTypeApi } from '@/api/orgType'
import type {
  PlaceTypeTreeNode, CreatePlaceTypeRequest, UpdatePlaceTypeRequest, PlaceCategory, UniversalPlaceType
} from '@/types/universalPlace'

// --- Feature labels & descriptions ---
const PLACE_FEATURE_LABELS: Record<string, string> = {
  hasCapacity: '有容量',
  bookable: '可预订',
  assignable: '可分配',
  occupiable: '可入住'
}

const featureDescriptions: Record<string, string> = {
  hasCapacity: '该场所有人数/工位容量限制',
  bookable: '支持在线预订功能',
  assignable: '可分配给组织或个人',
  occupiable: '支持入住/退房管理'
}

// --- Category colors ---
const CATEGORY_COLORS: Record<string, string> = {
  SITE: 'bg-blue-50 text-blue-600',
  BUILDING: 'bg-purple-50 text-purple-600',
  FLOOR: 'bg-green-50 text-green-600',
  ROOM: 'bg-amber-50 text-amber-600',
  AREA: 'bg-cyan-50 text-cyan-600',
  POINT: 'bg-gray-100 text-gray-600'
}
const categoryColor = (cat: string) => CATEGORY_COLORS[cat] || 'bg-gray-100 text-gray-600'

// --- Inline Sidebar Node Component ---
const TypeSidebarNode = defineComponent({
  name: 'TypeSidebarNode',
  props: {
    node: { type: Object as () => PlaceTypeTreeNode, required: true },
    level: { type: Number, default: 0 },
    selectedId: { type: Number, default: null },
    searchKeyword: { type: String, default: '' }
  },
  emits: ['select'],
  setup(props, { emit }) {
    const isExpanded = ref(true)
    const hasChildren = computed(() => props.node.children && props.node.children.length > 0)
    const isSelected = computed(() => props.selectedId === props.node.id)

    watch(() => props.searchKeyword, (val) => { if (val) isExpanded.value = true })

    const toggleExpand = (e: Event) => {
      e.stopPropagation()
      isExpanded.value = !isExpanded.value
    }

    const renderHighlight = (name: string, keyword: string) => {
      if (!keyword) return [h('span', {}, name)]
      const lower = name.toLowerCase()
      const idx = lower.indexOf(keyword.toLowerCase())
      if (idx === -1) return [h('span', {}, name)]
      const parts: any[] = []
      if (idx > 0) parts.push(h('span', {}, name.slice(0, idx)))
      parts.push(h('span', { class: 'rounded bg-yellow-200 px-0.5' }, name.slice(idx, idx + keyword.length)))
      if (idx + keyword.length < name.length) parts.push(h('span', {}, name.slice(idx + keyword.length)))
      return parts
    }

    const renderNode = () => {
      const row = h('div', {
        class: [
          'group flex cursor-pointer items-center gap-1 rounded-md px-2 py-1.5 transition-colors',
          isSelected.value ? 'bg-blue-50 text-blue-700' : 'text-gray-700 hover:bg-gray-100',
          !props.node.enabled && 'opacity-60'
        ],
        style: { paddingLeft: `${props.level * 16 + 8}px` },
        onClick: () => emit('select', props.node)
      }, [
        hasChildren.value
          ? h('button', {
              class: 'flex h-5 w-5 flex-shrink-0 items-center justify-center rounded transition-colors hover:bg-gray-200/60',
              onClick: toggleExpand
            }, [h(ChevronRight, { class: ['h-3.5 w-3.5 transition-transform duration-150', isExpanded.value ? 'rotate-90' : ''] })])
          : h('span', { class: 'w-5 flex-shrink-0' }),

        h('span', { class: 'flex h-6 w-6 flex-shrink-0 items-center justify-center rounded bg-gray-100' }, [
          h(MapPin, { class: 'h-3.5 w-3.5 text-gray-500' })
        ]),

        h('span', { class: 'min-w-0 flex-1 truncate text-sm font-medium', title: props.node.typeName },
          renderHighlight(props.node.typeName, props.searchKeyword)
        ),

        !props.node.enabled
          ? h('span', { class: 'flex-shrink-0 rounded bg-gray-200 px-1 py-0.5 text-[10px] leading-none text-gray-500' }, '停')
          : null
      ])

      const children = hasChildren.value && isExpanded.value
        ? props.node.children.map((child: PlaceTypeTreeNode) =>
            h(TypeSidebarNode, {
              node: child,
              level: props.level + 1,
              selectedId: props.selectedId,
              searchKeyword: props.searchKeyword,
              onSelect: (n: PlaceTypeTreeNode) => emit('select', n)
            })
          )
        : []

      return h('div', {}, [row, ...children])
    }

    return renderNode
  }
})

// --- Cross-domain type refs ---
const userTypes = ref<{ typeCode: string; typeName: string }[]>([])
const orgTypes = ref<{ typeCode: string; typeName: string }[]>([])

async function loadCrossTypes() {
  try {
    const [ut, ot] = await Promise.all([
      userTypeApi.getEnabled().catch(() => []),
      orgTypeApi.getEnabled().catch(() => [])
    ])
    userTypes.value = (ut || []).map((t: any) => ({ typeCode: t.typeCode, typeName: t.typeName }))
    orgTypes.value = (ot || []).map((t: any) => ({ typeCode: t.typeCode, typeName: t.typeName }))
  } catch (e) {
    console.warn('Failed to load cross-domain types', e)
  }
}

// --- State ---
const loading = ref(false)
const submitLoading = ref(false)
const treeData = ref<PlaceTypeTreeNode[]>([])
const allFlatTypes = ref<UniversalPlaceType[]>([])
const categories = ref<PlaceCategory[]>([])
const selectedId = ref<number | null>(null)
const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref<number>()
const showCodeEdit = ref(false)
const nameInputRef = ref<HTMLInputElement>()

const formData = reactive({
  typeCode: '',
  typeName: '',
  parentTypeCode: '',
  category: '',
  description: '',
  sortOrder: 0,
  maxDepth: null as number | null,
  features: {} as Record<string, boolean>,
  allowedChildTypeCodes: [] as string[],
  defaultUserTypeCodes: [] as string[],
  defaultOrgTypeCodes: [] as string[],
  capacityUnit: '人',
  defaultCapacity: undefined as number | undefined,
})

// --- Computed ---
const enabledCount = computed(() => allFlatTypes.value.filter(t => t.enabled).length)
const disabledCount = computed(() => allFlatTypes.value.filter(t => !t.enabled).length)

const selectedType = computed(() => {
  if (!selectedId.value) return null
  return allFlatTypes.value.find(t => t.id === selectedId.value) || null
})

const activeFeatures = computed(() => {
  if (!selectedType.value?.features) return []
  return Object.entries(selectedType.value.features)
    .filter(([_, v]) => v)
    .map(([k]) => ({ key: k, label: PLACE_FEATURE_LABELS[k] || k }))
})

const childTypes = computed(() => {
  if (!selectedType.value) return []
  return allFlatTypes.value.filter(t => t.parentTypeCode === selectedType.value!.typeCode)
})

const allTypeCodesForChildSelect = computed(() => {
  const editingCode = isEdit.value ? formData.typeCode : ''
  return allFlatTypes.value.filter(t => t.typeCode !== editingCode)
})

const availableCategories = computed(() => {
  if (!formData.parentTypeCode) {
    return categories.value.filter(c => c.root)
  }
  const parent = allFlatTypes.value.find(t => t.typeCode === formData.parentTypeCode)
  if (!parent?.category) return categories.value
  const parentCat = categories.value.find(c => c.code === parent.category)
  if (!parentCat) return categories.value
  return categories.value.filter(c => parentCat.allowedChildCategories.includes(c.code))
})

const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑场所类型'
  if (formData.parentTypeCode) {
    const parent = allFlatTypes.value.find(t => t.typeCode === formData.parentTypeCode)
    return parent ? `新增子类型 — ${parent.typeName}` : '新增场所类型'
  }
  return '新增场所类型'
})

// --- Tree filter ---
const filterTree = (nodes: PlaceTypeTreeNode[], keyword: string): PlaceTypeTreeNode[] => {
  if (!keyword) return nodes
  const lower = keyword.toLowerCase()
  return nodes.reduce<PlaceTypeTreeNode[]>((acc, node) => {
    const matchSelf = node.typeName.toLowerCase().includes(lower) || node.typeCode.toLowerCase().includes(lower)
    const filteredChildren = node.children ? filterTree(node.children, keyword) : []
    if (matchSelf || filteredChildren.length > 0) {
      acc.push({ ...node, children: filteredChildren.length > 0 ? filteredChildren : node.children })
    }
    return acc
  }, [])
}

const filteredTree = computed(() => filterTree(treeData.value, searchKeyword.value))

// --- Helpers ---
const flattenTree = (nodes: PlaceTypeTreeNode[]): UniversalPlaceType[] => {
  const result: UniversalPlaceType[] = []
  const traverse = (list: PlaceTypeTreeNode[]) => {
    for (const node of list) {
      result.push(node)
      if (node.children?.length) traverse(node.children)
    }
  }
  traverse(nodes)
  return result
}

function categoryLabel(code: string): string {
  return categories.value.find(c => c.code === code)?.label || code
}

function resolveTypeName(typeCode: string): string {
  const t = allFlatTypes.value.find(f => f.typeCode === typeCode)
  return t ? t.typeName : typeCode
}

function resolveCrossTypeName(code: string, types: { typeCode: string; typeName: string }[]): string {
  return types.find(t => t.typeCode === code)?.typeName || code
}

// --- Actions ---
const loadData = async () => {
  loading.value = true
  try {
    const [tree, cats] = await Promise.all([
      universalPlaceTypeApi.getTree(),
      universalPlaceTypeApi.getCategories()
    ])
    treeData.value = tree
    allFlatTypes.value = flattenTree(tree)
    categories.value = cats
  } catch (e) {
    console.warn('Failed to load place types', e)
    ElMessage.error('加载场所类型失败')
  } finally {
    loading.value = false
  }
  loadCrossTypes()
}

const handleSelectNode = (node: PlaceTypeTreeNode) => {
  selectedId.value = node.id as number
}

const handleAdd = (parentTypeCode?: string) => {
  isEdit.value = false
  currentId.value = undefined
  resetForm()
  formData.typeCode = generateTypeCode()
  if (parentTypeCode) {
    formData.parentTypeCode = parentTypeCode
  }
  dialogVisible.value = true
}

const handleEdit = (item: UniversalPlaceType) => {
  isEdit.value = true
  currentId.value = item.id as number
  Object.assign(formData, {
    typeCode: item.typeCode,
    typeName: item.typeName,
    parentTypeCode: item.parentTypeCode || '',
    category: item.category || '',
    description: item.description || '',
    sortOrder: item.sortOrder,
    maxDepth: item.maxDepth,
    features: item.features ? { ...item.features } : {},
    allowedChildTypeCodes: item.allowedChildTypeCodes ? [...item.allowedChildTypeCodes] : [],
    defaultUserTypeCodes: item.defaultUserTypeCodes ? [...item.defaultUserTypeCodes] : [],
    defaultOrgTypeCodes: item.defaultOrgTypeCodes ? [...item.defaultOrgTypeCodes] : [],
    capacityUnit: item.capacityUnit || '人',
    defaultCapacity: item.defaultCapacity,
  })
  dialogVisible.value = true
}

const handleDelete = async (item: UniversalPlaceType) => {
  try {
    await ElMessageBox.confirm(
      `确定删除类型"${item.typeName}"吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await universalPlaceTypeApi.delete(item.id as number)
    ElMessage.success('删除成功')
    if (selectedId.value === item.id) selectedId.value = null
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.message || '删除失败')
  }
}

const handleToggleStatus = async (item: UniversalPlaceType) => {
  try {
    if (item.enabled) {
      await universalPlaceTypeApi.disable(item.id as number)
    } else {
      await universalPlaceTypeApi.enable(item.id as number)
    }
    ElMessage.success('状态已更新')
    loadData()
  } catch (e) {
    console.warn('Failed to toggle status', e)
    ElMessage.error('操作失败')
  }
}

const handleSubmit = async () => {
  if (!formData.typeName) {
    ElMessage.error('请填写类型名称')
    return
  }
  if (!formData.category) {
    ElMessage.error('请选择分类')
    return
  }

  submitLoading.value = true
  try {
    const cleanFeatures = Object.fromEntries(
      Object.entries(formData.features).filter(([_, v]) => v !== undefined)
    )

    if (isEdit.value && currentId.value) {
      const updateData: UpdatePlaceTypeRequest = {
        typeName: formData.typeName,
        category: formData.category || undefined,
        description: formData.description || undefined,
        features: Object.keys(cleanFeatures).length > 0 ? cleanFeatures : undefined,
        allowedChildTypeCodes: formData.allowedChildTypeCodes.length > 0 ? formData.allowedChildTypeCodes : undefined,
        maxDepth: formData.maxDepth ?? undefined,
        defaultUserTypeCodes: formData.defaultUserTypeCodes.length > 0 ? formData.defaultUserTypeCodes : undefined,
        defaultOrgTypeCodes: formData.defaultOrgTypeCodes.length > 0 ? formData.defaultOrgTypeCodes : undefined,
        capacityUnit: formData.features.hasCapacity ? formData.capacityUnit : undefined,
        defaultCapacity: formData.features.hasCapacity ? formData.defaultCapacity : undefined,
        sortOrder: formData.sortOrder,
      }
      await universalPlaceTypeApi.update(currentId.value, updateData)
      ElMessage.success('更新成功')
    } else {
      const createData: CreatePlaceTypeRequest = {
        typeCode: formData.typeCode || undefined,
        typeName: formData.typeName,
        category: formData.category,
        parentTypeCode: formData.parentTypeCode || undefined,
        description: formData.description || undefined,
        features: Object.keys(cleanFeatures).length > 0 ? cleanFeatures : undefined,
        allowedChildTypeCodes: formData.allowedChildTypeCodes.length > 0 ? formData.allowedChildTypeCodes : undefined,
        maxDepth: formData.maxDepth ?? undefined,
        defaultUserTypeCodes: formData.defaultUserTypeCodes.length > 0 ? formData.defaultUserTypeCodes : undefined,
        defaultOrgTypeCodes: formData.defaultOrgTypeCodes.length > 0 ? formData.defaultOrgTypeCodes : undefined,
        capacityUnit: formData.features.hasCapacity ? formData.capacityUnit : undefined,
        defaultCapacity: formData.features.hasCapacity ? formData.defaultCapacity : undefined,
        sortOrder: formData.sortOrder,
      }
      await universalPlaceTypeApi.create(createData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const resetForm = () => {
  Object.assign(formData, {
    typeCode: '',
    typeName: '',
    parentTypeCode: '',
    category: '',
    description: '',
    sortOrder: 0,
    maxDepth: null,
    features: {},
    allowedChildTypeCodes: [],
    defaultUserTypeCodes: [],
    defaultOrgTypeCodes: [],
    capacityUnit: '人',
    defaultCapacity: undefined,
  })
  showCodeEdit.value = false
}

const generateTypeCode = () => {
  return `PT_${crypto.randomUUID().slice(0, 8).toUpperCase()}`
}

// Auto-populate features from category defaults when category changes (new mode only)
watch(() => formData.category, (newCat) => {
  if (!isEdit.value && newCat) {
    const cat = categories.value.find(c => c.code === newCat)
    if (cat?.defaultFeatures) {
      formData.features = { ...cat.defaultFeatures }
    }
  }
})

// Auto-generate code when name changes
watch(() => formData.typeName, () => {
  if (!isEdit.value && !showCodeEdit.value && formData.typeName) {
    if (!formData.typeCode || formData.typeCode.startsWith('PT_')) {
      formData.typeCode = generateTypeCode()
    }
  }
})

// Focus name input on dialog open
watch(dialogVisible, (val) => {
  if (val) {
    nextTick(() => nameInputRef.value?.focus())
  }
})

onMounted(loadData)
</script>

<style scoped>
@keyframes slide-in-right {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}
.animate-slide-in-right {
  animation: slide-in-right 0.2s ease-out;
}
</style>
