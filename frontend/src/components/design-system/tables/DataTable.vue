<template>
  <div class="data-table">
    <!-- 表格容器 -->
    <div class="data-table__wrapper">
      <table class="data-table__table">
        <!-- 表头 -->
        <thead class="data-table__thead">
          <tr>
            <!-- 多选列 -->
            <th v-if="selectable" class="data-table__th data-table__th--checkbox">
              <input
                type="checkbox"
                class="data-table__checkbox"
                :checked="isAllSelected"
                :indeterminate="isSomeSelected"
                @change="toggleSelectAll"
              />
            </th>

            <!-- 数据列 -->
            <th
              v-for="column in columns"
              :key="column.key"
              :class="[
                'data-table__th',
                {
                  'data-table__th--sortable': column.sortable,
                  'data-table__th--sorted': sortKey === column.key
                }
              ]"
              :style="{ width: column.width, textAlign: column.align || 'left' }"
              @click="column.sortable ? handleSort(column.key) : null"
            >
              <div class="data-table__th-content">
                <span>{{ column.label }}</span>
                <div v-if="column.sortable" class="data-table__sort-icons">
                  <ChevronUp
                    :class="[
                      'data-table__sort-icon',
                      { 'data-table__sort-icon--active': sortKey === column.key && sortOrder === 'asc' }
                    ]"
                  />
                  <ChevronDown
                    :class="[
                      'data-table__sort-icon',
                      { 'data-table__sort-icon--active': sortKey === column.key && sortOrder === 'desc' }
                    ]"
                  />
                </div>
              </div>
            </th>
          </tr>
        </thead>

        <!-- 表体 -->
        <tbody v-if="!loading && sortedData.length > 0" class="data-table__tbody">
          <tr
            v-for="(row, index) in sortedData"
            :key="getRowKey(row, index)"
            :class="[
              'data-table__tr',
              {
                'data-table__tr--selected': isRowSelected(row),
                'data-table__tr--clickable': rowClickable
              }
            ]"
            @click="handleRowClick(row)"
          >
            <!-- 多选列 -->
            <td v-if="selectable" class="data-table__td data-table__td--checkbox">
              <input
                type="checkbox"
                class="data-table__checkbox"
                :checked="isRowSelected(row)"
                @change="toggleSelectRow(row)"
                @click.stop
              />
            </td>

            <!-- 数据列 -->
            <td
              v-for="column in columns"
              :key="column.key"
              class="data-table__td"
              :style="{ textAlign: column.align || 'left' }"
            >
              <!-- 自定义插槽 -->
              <slot
                v-if="column.slot"
                :name="column.slot"
                :row="row"
                :column="column"
                :value="getCellValue(row, column.key)"
              >
                {{ getCellValue(row, column.key) }}
              </slot>

              <!-- 默认显示 -->
              <span v-else class="data-table__cell-content">
                {{ formatCellValue(row, column) }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 加载状态 -->
      <div v-if="loading" class="data-table__loading">
        <LoadingState text="加载中..." />
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && sortedData.length === 0" class="data-table__empty">
        <EmptyState
          :icon="emptyIcon"
          :title="emptyText"
          :description="emptyDescription"
        />
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="pagination && sortedData.length > 0" class="data-table__pagination">
      <div class="data-table__pagination-info">
        共 <span class="data-table__pagination-total">{{ pagination.total }}</span> 条
      </div>

      <div class="data-table__pagination-controls">
        <!-- 上一页 -->
        <button
          class="data-table__pagination-btn"
          :disabled="pagination.current === 1"
          @click="handlePageChange(pagination.current - 1)"
        >
          <ChevronLeft class="w-4 h-4" />
        </button>

        <!-- 页码 -->
        <div class="data-table__pagination-pages">
          <button
            v-for="page in visiblePages"
            :key="page"
            :class="[
              'data-table__pagination-page',
              { 'data-table__pagination-page--active': page === pagination.current }
            ]"
            @click="typeof page === 'number' ? handlePageChange(page) : null"
          >
            {{ page }}
          </button>
        </div>

        <!-- 下一页 -->
        <button
          class="data-table__pagination-btn"
          :disabled="pagination.current === totalPages"
          @click="handlePageChange(pagination.current + 1)"
        >
          <ChevronRight class="w-4 h-4" />
        </button>
      </div>

      <div class="data-table__pagination-size">
        <select
          v-model="currentPageSize"
          class="data-table__pagination-select"
          @change="handlePageSizeChange"
        >
          <option v-for="size in pageSizeOptions" :key="size" :value="size">
            {{ size }} 条/页
          </option>
        </select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ChevronUp, ChevronDown, ChevronLeft, ChevronRight, Database } from 'lucide-vue-next'
import LoadingState from '../feedback/LoadingState.vue'
import EmptyState from '../feedback/EmptyState.vue'
import type { Component } from 'vue'

interface Column {
  key: string
  label: string
  width?: string
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  slot?: string
  formatter?: (value: any, row: any) => string
}

interface PaginationConfig {
  current: number
  pageSize: number
  total: number
}

interface Props {
  columns: Column[]
  data: any[]
  loading?: boolean
  rowKey?: string
  selectable?: boolean
  rowClickable?: boolean
  pagination?: PaginationConfig
  emptyText?: string
  emptyDescription?: string
  emptyIcon?: Component
  pageSizeOptions?: number[]
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  rowKey: 'id',
  selectable: false,
  rowClickable: false,
  emptyText: '暂无数据',
  emptyDescription: '当前没有可显示的数据',
  emptyIcon: Database,
  pageSizeOptions: () => [10, 20, 50, 100]
})

const emit = defineEmits<{
  'row-click': [row: any]
  'selection-change': [selectedRows: any[]]
  'page-change': [page: number]
  'page-size-change': [pageSize: number]
  'sort-change': [key: string, order: 'asc' | 'desc']
}>()

// 排序
const sortKey = ref<string>('')
const sortOrder = ref<'asc' | 'desc'>('asc')

// 选中行
const selectedRows = ref<any[]>([])

// 当前页码大小
const currentPageSize = ref(props.pagination?.pageSize || 10)

// 排序后的数据
const sortedData = computed(() => {
  let result = [...props.data]

  if (sortKey.value) {
    result.sort((a, b) => {
      const aVal = getCellValue(a, sortKey.value)
      const bVal = getCellValue(b, sortKey.value)

      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortOrder.value === 'asc' ? aVal - bVal : bVal - aVal
      }

      const aStr = String(aVal).toLowerCase()
      const bStr = String(bVal).toLowerCase()

      if (sortOrder.value === 'asc') {
        return aStr.localeCompare(bStr)
      } else {
        return bStr.localeCompare(aStr)
      }
    })
  }

  return result
})

// 总页数
const totalPages = computed(() => {
  if (!props.pagination) return 1
  return Math.ceil(props.pagination.total / props.pagination.pageSize)
})

// 可见页码
const visiblePages = computed(() => {
  if (!props.pagination) return []

  const current = props.pagination.current
  const total = totalPages.value
  const pages: (number | string)[] = []

  if (total <= 7) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    pages.push(1)

    if (current > 3) {
      pages.push('...')
    }

    const start = Math.max(2, current - 1)
    const end = Math.min(total - 1, current + 1)

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }

    if (current < total - 2) {
      pages.push('...')
    }

    pages.push(total)
  }

  return pages
})

// 是否全选
const isAllSelected = computed(() => {
  return props.data.length > 0 && selectedRows.value.length === props.data.length
})

// 是否部分选中
const isSomeSelected = computed(() => {
  return selectedRows.value.length > 0 && selectedRows.value.length < props.data.length
})

// 获取行键值
const getRowKey = (row: any, index: number) => {
  return row[props.rowKey] || index
}

// 获取单元格值
const getCellValue = (row: any, key: string) => {
  return key.split('.').reduce((obj, k) => obj?.[k], row)
}

// 格式化单元格值
const formatCellValue = (row: any, column: Column) => {
  const value = getCellValue(row, column.key)

  if (column.formatter) {
    return column.formatter(value, row)
  }

  if (value === null || value === undefined) {
    return '-'
  }

  return value
}

// 是否选中行
const isRowSelected = (row: any) => {
  const rowKey = getRowKey(row, 0)
  return selectedRows.value.some(r => getRowKey(r, 0) === rowKey)
}

// 切换选中行
const toggleSelectRow = (row: any) => {
  const rowKey = getRowKey(row, 0)
  const index = selectedRows.value.findIndex(r => getRowKey(r, 0) === rowKey)

  if (index > -1) {
    selectedRows.value.splice(index, 1)
  } else {
    selectedRows.value.push(row)
  }

  emit('selection-change', selectedRows.value)
}

// 全选/取消全选
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedRows.value = []
  } else {
    selectedRows.value = [...props.data]
  }

  emit('selection-change', selectedRows.value)
}

// 排序处理
const handleSort = (key: string) => {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortOrder.value = 'asc'
  }

  emit('sort-change', key, sortOrder.value)
}

// 行点击
const handleRowClick = (row: any) => {
  if (props.rowClickable) {
    emit('row-click', row)
  }
}

// 页码改变
const handlePageChange = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  emit('page-change', page)
}

// 页码大小改变
const handlePageSizeChange = () => {
  emit('page-size-change', currentPageSize.value)
}

// 监听分页变化
watch(() => props.pagination?.pageSize, (newSize) => {
  if (newSize) {
    currentPageSize.value = newSize
  }
})
</script>

<style scoped>
/* ============================================
   表格容器
   ============================================ */
.data-table {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.data-table__wrapper {
  position: relative;
  background: white;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

/* ============================================
   表格
   ============================================ */
.data-table__table {
  width: 100%;
  border-collapse: collapse;
  font-family: var(--font-body);
}

/* ============================================
   表头
   ============================================ */
.data-table__thead {
  background: linear-gradient(180deg, var(--color-gray-50) 0%, var(--color-gray-100) 100%);
  border-bottom: 2px solid var(--color-gray-200);
}

.data-table__th {
  padding: 1rem;
  font-family: var(--font-display);
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-gray-700);
  text-align: left;
  white-space: nowrap;
  user-select: none;
  border-bottom: 1px solid var(--color-gray-200);
}

.data-table__th--checkbox {
  width: 48px;
  text-align: center;
}

.data-table__th--sortable {
  cursor: pointer;
  transition: all var(--transition-base);
}

.data-table__th--sortable:hover {
  background: rgba(37, 99, 235, 0.05);
  color: var(--color-primary-700);
}

.data-table__th--sorted {
  color: var(--color-primary-600);
}

.data-table__th-content {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.data-table__sort-icons {
  display: flex;
  flex-direction: column;
  gap: -4px;
}

.data-table__sort-icon {
  width: 0.875rem;
  height: 0.875rem;
  color: var(--color-gray-400);
  transition: all var(--transition-fast);
}

.data-table__sort-icon--active {
  color: var(--color-primary-600);
}

/* ============================================
   表体
   ============================================ */
.data-table__tbody {
  background: white;
}

.data-table__tr {
  position: relative;
  transition: all var(--transition-fast);
  border-bottom: 1px solid var(--color-gray-100);
}

.data-table__tr::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 0;
  background: linear-gradient(90deg, var(--color-primary-500), transparent);
  transition: width var(--transition-fast);
}

.data-table__tr:hover {
  background: rgba(37, 99, 235, 0.02);
}

.data-table__tr:hover::before {
  width: 4px;
}

.data-table__tr--selected {
  background: rgba(37, 99, 235, 0.05);
}

.data-table__tr--clickable {
  cursor: pointer;
}

.data-table__tr--clickable:active {
  background: rgba(37, 99, 235, 0.08);
}

.data-table__td {
  padding: 1rem;
  font-size: 0.9375rem;
  color: var(--color-gray-700);
}

.data-table__td--checkbox {
  text-align: center;
}

.data-table__cell-content {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ============================================
   复选框
   ============================================ */
.data-table__checkbox {
  width: 1.125rem;
  height: 1.125rem;
  cursor: pointer;
  accent-color: var(--color-primary-600);
}

/* ============================================
   加载/空状态
   ============================================ */
.data-table__loading,
.data-table__empty {
  padding: 3rem 1rem;
}

/* ============================================
   分页
   ============================================ */
.data-table__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: white;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-xl);
  gap: 1rem;
}

.data-table__pagination-info {
  font-size: 0.875rem;
  color: var(--color-gray-600);
}

.data-table__pagination-total {
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--color-primary-600);
}

.data-table__pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.data-table__pagination-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  background: white;
  color: var(--color-gray-600);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.data-table__pagination-btn:hover:not(:disabled) {
  border-color: var(--color-primary-500);
  color: var(--color-primary-600);
  background: rgba(37, 99, 235, 0.05);
}

.data-table__pagination-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.data-table__pagination-pages {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.data-table__pagination-page {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2rem;
  height: 2rem;
  padding: 0 0.5rem;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: transparent;
  font-family: var(--font-mono);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-gray-600);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.data-table__pagination-page:hover {
  border-color: var(--color-primary-300);
  color: var(--color-primary-600);
  background: rgba(37, 99, 235, 0.05);
}

.data-table__pagination-page--active {
  background: var(--gradient-primary);
  color: white;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
  border-color: transparent;
}

.data-table__pagination-page--active:hover {
  background: var(--gradient-primary-hover);
}

.data-table__pagination-size {
  min-width: 120px;
}

.data-table__pagination-select {
  width: 100%;
  height: 2rem;
  padding: 0 0.75rem;
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  background: white;
  font-size: 0.875rem;
  color: var(--color-gray-700);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.data-table__pagination-select:hover {
  border-color: var(--color-primary-400);
}

.data-table__pagination-select:focus {
  outline: none;
  border-color: var(--color-primary-500);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 768px) {
  .data-table__pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .data-table__pagination-controls {
    justify-content: center;
  }

  .data-table__th,
  .data-table__td {
    padding: 0.75rem;
    font-size: 0.875rem;
  }
}
</style>
