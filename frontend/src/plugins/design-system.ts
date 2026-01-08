/**
 * 设计系统全局插件
 * 自动注册所有设计系统组件为全局组件
 */
import type { App } from 'vue'
import StatCard from '@/components/design-system/cards/StatCard.vue'
import FormCard from '@/components/design-system/cards/FormCard.vue'
import GradientButton from '@/components/design-system/buttons/GradientButton.vue'
import LoadingState from '@/components/design-system/feedback/LoadingState.vue'
import EmptyState from '@/components/design-system/feedback/EmptyState.vue'
import DataTable from '@/components/design-system/tables/DataTable.vue'
import FilterBar from '@/components/design-system/tables/FilterBar.vue'

export default {
  install(app: App) {
    // 注册所有设计系统组件为全局组件
    app.component('StatCard', StatCard)
    app.component('FormCard', FormCard)
    app.component('GradientButton', GradientButton)
    app.component('LoadingState', LoadingState)
    app.component('EmptyState', EmptyState)
    app.component('DataTable', DataTable)
    app.component('FilterBar', FilterBar)
  }
}
