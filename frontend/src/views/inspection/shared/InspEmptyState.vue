<script setup lang="ts">
/**
 * InspEmptyState — 空状态组件 (token 化, A 级)
 *
 * 用法:
 *   <InspEmptyState title="暂无项目" description="点击下方按钮新建">
 *     <template #icon><Inbox :size="32" /></template>
 *     <template #action>
 *       <InspButton variant="accent" @click="goCreate">新建</InspButton>
 *     </template>
 *   </InspEmptyState>
 */
import { Inbox } from 'lucide-vue-next'
import InspButton from './InspButton.vue'

defineProps<{
  title?: string
  description?: string
  actionLabel?: string
}>()

const emit = defineEmits<{ action: [] }>()
</script>

<template>
  <div class="ies">
    <div class="ies-icon">
      <slot name="icon">
        <Inbox :size="28" />
      </slot>
    </div>
    <p class="ies-title">{{ title || '暂无数据' }}</p>
    <p v-if="description" class="ies-sub">{{ description }}</p>
    <div v-if="$slots.action || actionLabel" class="ies-action">
      <slot name="action">
        <InspButton v-if="actionLabel" variant="accent" @click="emit('action')">
          {{ actionLabel }}
        </InspButton>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.ies {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--insp-sp-7) var(--insp-sp-4);
  text-align: center;
  color: var(--insp-ink-quaternary);
}
.ies-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: var(--insp-sp-3);
  border-radius: var(--insp-radius-pill);
  background: var(--insp-bg-subtle);
  color: var(--insp-ink-quaternary);
}
.ies-title {
  margin: 0;
  font-size: var(--insp-text-md);
  font-weight: var(--insp-fw-semibold);
  color: var(--insp-ink-secondary);
}
.ies-sub {
  margin: var(--insp-sp-1) 0 0;
  font-size: var(--insp-text-sm);
  color: var(--insp-ink-tertiary);
  max-width: 320px;
  line-height: var(--insp-leading-normal);
}
.ies-action {
  margin-top: var(--insp-sp-4);
}
</style>
