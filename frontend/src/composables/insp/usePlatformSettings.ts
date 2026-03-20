/**
 * V7 检查平台 - 平台设置管理 Composable
 *
 * 管理通知规则、Webhook、问题分类、审计日志等平台级配置
 */
import { ref, computed } from 'vue'
import { useInspPlatformStore } from '@/stores/insp/inspPlatformStore'
import type { NotificationRule, WebhookSubscription, IssueCategory } from '@/types/insp/platform'

export function usePlatformSettings() {
  const platformStore = useInspPlatformStore()
  const activeTab = ref<string>('notifications')
  const isEditing = ref(false)
  const editingItem = ref<NotificationRule | WebhookSubscription | IssueCategory | null>(null)

  // ==================== 通知规则 ====================

  async function loadNotificationRules(projectId?: number) {
    await platformStore.fetchNotificationRules(projectId)
  }

  async function saveNotificationRule(data: Partial<NotificationRule>) {
    if (data.id) {
      await platformStore.updateNotificationRule(data.id, data)
    } else {
      await platformStore.createNotificationRule(data)
    }
    isEditing.value = false
    editingItem.value = null
  }

  async function deleteNotificationRule(id: number) {
    await platformStore.deleteNotificationRule(id)
  }

  async function toggleNotificationRule(id: number, enabled: boolean) {
    if (enabled) {
      await platformStore.enableNotificationRule(id)
    } else {
      await platformStore.disableNotificationRule(id)
    }
  }

  // ==================== Webhook 管理 ====================

  async function loadWebhooks(projectId?: number) {
    await platformStore.fetchWebhooks(projectId)
  }

  async function saveWebhook(data: Partial<WebhookSubscription>) {
    if (data.id) {
      await platformStore.updateWebhook(data.id, data)
    } else {
      await platformStore.createWebhook(data)
    }
    isEditing.value = false
    editingItem.value = null
  }

  async function deleteWebhook(id: number) {
    await platformStore.deleteWebhook(id)
  }

  async function toggleWebhook(id: number, enabled: boolean) {
    if (enabled) {
      await platformStore.enableWebhook(id)
    } else {
      await platformStore.disableWebhook(id)
    }
  }

  async function testWebhook(id: number) {
    await platformStore.testWebhook(id)
  }

  // ==================== 问题分类 ====================

  async function loadIssueCategories(projectId?: number) {
    await platformStore.fetchIssueCategories(projectId)
  }

  async function saveIssueCategory(data: Partial<IssueCategory>) {
    if (data.id) {
      await platformStore.updateIssueCategory(data.id, data)
    } else {
      await platformStore.createIssueCategory(data)
    }
  }

  async function deleteIssueCategory(id: number) {
    await platformStore.deleteIssueCategory(id)
  }

  // ==================== 编辑辅助 ====================

  function startEdit(item: NotificationRule | WebhookSubscription | IssueCategory) {
    editingItem.value = { ...item }
    isEditing.value = true
  }

  function cancelEdit() {
    editingItem.value = null
    isEditing.value = false
  }

  return {
    // 当前标签页
    activeTab,
    isEditing,
    editingItem,
    // 通知规则
    notificationRules: computed(() => platformStore.notificationRules),
    loadNotificationRules,
    saveNotificationRule,
    deleteNotificationRule,
    toggleNotificationRule,
    // Webhooks
    webhooks: computed(() => platformStore.webhooks),
    loadWebhooks,
    saveWebhook,
    deleteWebhook,
    toggleWebhook,
    testWebhook,
    // 问题分类
    issueCategories: computed(() => platformStore.issueCategories),
    loadIssueCategories,
    saveIssueCategory,
    deleteIssueCategory,
    // 审计日志
    auditEntries: computed(() => platformStore.auditEntries),
    fetchAuditEntries: platformStore.fetchAuditEntries,
    // 编辑辅助
    startEdit,
    cancelEdit,
    // 加载状态
    loading: computed(() => platformStore.loading),
  }
}
