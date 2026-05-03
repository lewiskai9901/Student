<script setup lang="ts">
/**
 * InspButton — 检查平台标准按钮 (A 级 token 化)
 *
 * 4 variants: default / accent / ghost / danger
 * 3 sizes:   sm / md (默认) / lg
 * 状态: loading / disabled
 *
 * 用法:
 *   <InspButton variant="accent" size="sm" @click="...">发布</InspButton>
 *   <InspButton variant="ghost" :loading="busy">保存</InspButton>
 */
import { computed } from 'vue'
import { Loader2 } from 'lucide-vue-next'

interface Props {
  variant?: 'default' | 'accent' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  block?: boolean
  loading?: boolean
  disabled?: boolean
  type?: 'button' | 'submit' | 'reset'
  /** 仅图标按钮 (无文字, 自动方形) */
  iconOnly?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'md',
  type: 'button',
})

const cls = computed(() => [
  'insp-btn',
  props.variant !== 'default' && `insp-btn--${props.variant}`,
  props.size !== 'md' && `insp-btn--${props.size}`,
  props.block && 'insp-btn--block',
  props.iconOnly && 'insp-btn--icon-only',
])
</script>

<template>
  <button
    :class="cls"
    :type="type"
    :disabled="disabled || loading"
  >
    <Loader2 v-if="loading" :size="size === 'sm' ? 11 : 13" class="ib-spin" />
    <slot />
  </button>
</template>

<style scoped>
.insp-btn--icon-only {
  width: var(--insp-h-md);
  padding: 0;
}
.insp-btn--icon-only.insp-btn--sm { width: var(--insp-h-sm); }
.insp-btn--icon-only.insp-btn--lg { width: var(--insp-h-lg); }

.ib-spin {
  animation: ib-spin 800ms linear infinite;
}
@keyframes ib-spin {
  to { transform: rotate(360deg); }
}
</style>
