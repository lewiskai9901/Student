/**
 * V2 Components
 * 基于 DDD 架构的 UI 组件库
 */

// 布局组件
export { default as PageContainer } from './PageContainer.vue'
export { default as ActionBar } from './ActionBar.vue'

// 搜索组件
export { default as SearchBar } from './SearchBar.vue'

// 数据展示组件
export { default as StatusTag } from './StatusTag.vue'
export { default as DescriptionList } from './DescriptionList.vue'
export { default as DetailDrawer } from './DetailDrawer.vue'

// 交互组件
export { default as ConfirmButton } from './ConfirmButton.vue'

// 从 design-system 重新导出常用组件
export { default as StatCard } from '../design-system/cards/StatCard.vue'
export { default as FormCard } from '../design-system/cards/FormCard.vue'
export { default as DataTable } from '../design-system/tables/DataTable.vue'
export { default as FilterBar } from '../design-system/tables/FilterBar.vue'
export { default as LoadingState } from '../design-system/feedback/LoadingState.vue'
export { default as EmptyState } from '../design-system/feedback/EmptyState.vue'
export { default as GradientButton } from '../design-system/buttons/GradientButton.vue'
