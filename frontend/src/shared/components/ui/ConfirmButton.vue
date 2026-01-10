<template>
  <el-popconfirm
    v-if="usePopconfirm"
    :title="confirmTitle"
    :confirm-button-text="confirmText"
    :cancel-button-text="cancelText"
    :confirm-button-type="confirmButtonType"
    :icon="icon"
    :icon-color="iconColor"
    :width="width"
    @confirm="handleConfirm"
  >
    <template #reference>
      <el-button
        v-bind="buttonProps"
        :loading="loading"
        :disabled="disabled"
      >
        <slot />
      </el-button>
    </template>
  </el-popconfirm>
  <el-button
    v-else
    v-bind="buttonProps"
    :loading="loading"
    :disabled="disabled"
    @click="handleClick"
  >
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import type { Component } from 'vue'

type ButtonType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default' | ''

interface Props {
  confirmTitle?: string
  confirmText?: string
  cancelText?: string
  confirmButtonType?: ButtonType
  icon?: Component
  iconColor?: string
  width?: number
  usePopconfirm?: boolean
  useMessageBox?: boolean
  messageBoxTitle?: string
  messageBoxType?: 'warning' | 'info' | 'error' | 'success'
  type?: ButtonType
  size?: 'large' | 'default' | 'small'
  plain?: boolean
  text?: boolean
  link?: boolean
  round?: boolean
  circle?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  confirmTitle: '确定执行此操作吗？',
  confirmText: '确定',
  cancelText: '取消',
  confirmButtonType: 'primary',
  icon: Warning,
  iconColor: '#f56c6c',
  width: 200,
  usePopconfirm: true,
  useMessageBox: false,
  messageBoxTitle: '确认',
  messageBoxType: 'warning',
  type: 'default',
  size: 'default',
  plain: false,
  text: false,
  link: false,
  round: false,
  circle: false,
  disabled: false
})

const emit = defineEmits<{
  confirm: []
}>()

const loading = ref(false)

const buttonProps = computed(() => ({
  type: props.type,
  size: props.size,
  plain: props.plain,
  text: props.text,
  link: props.link,
  round: props.round,
  circle: props.circle
}))

const handleConfirm = () => {
  emit('confirm')
}

const handleClick = async () => {
  if (props.useMessageBox) {
    try {
      await ElMessageBox.confirm(props.confirmTitle, props.messageBoxTitle, {
        confirmButtonText: props.confirmText,
        cancelButtonText: props.cancelText,
        type: props.messageBoxType
      })
      emit('confirm')
    } catch {
      // 用户取消
    }
  } else {
    emit('confirm')
  }
}
</script>
