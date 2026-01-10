<template>
  <div class="search-bar">
    <!-- 基础搜索 -->
    <div class="search-bar__basic">
      <slot name="basic">
        <el-input
          v-model="keyword"
          :placeholder="placeholder"
          clearable
          class="search-bar__input"
          @keyup.enter="handleSearch"
          @clear="handleClear"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </slot>

      <div class="search-bar__actions">
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          重置
        </el-button>
        <el-button
          v-if="showAdvanced && $slots.advanced"
          text
          @click="toggleAdvanced"
        >
          {{ isExpanded ? '收起' : '展开' }}
          <el-icon :class="{ 'is-rotate': isExpanded }">
            <ArrowDown />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- 高级搜索 -->
    <transition name="slide-down">
      <div v-if="showAdvanced && isExpanded && $slots.advanced" class="search-bar__advanced">
        <slot name="advanced" />
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Search, RefreshRight, ArrowDown } from '@element-plus/icons-vue'

interface Props {
  modelValue?: string
  placeholder?: string
  showAdvanced?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请输入搜索关键字',
  showAdvanced: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  search: []
  reset: []
}>()

const keyword = ref(props.modelValue)
const isExpanded = ref(false)

watch(
  () => props.modelValue,
  (val) => {
    keyword.value = val
  }
)

watch(keyword, (val) => {
  emit('update:modelValue', val)
})

const handleSearch = () => {
  emit('search')
}

const handleReset = () => {
  keyword.value = ''
  emit('reset')
}

const handleClear = () => {
  emit('search')
}

const toggleAdvanced = () => {
  isExpanded.value = !isExpanded.value
}
</script>

<style scoped>
.search-bar {
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-gray-200);
}

.search-bar__basic {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
}

.search-bar__input {
  flex: 1;
  max-width: 400px;
}

.search-bar__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-bar__advanced {
  padding: 1rem;
  border-top: 1px solid var(--color-gray-200);
  background: var(--color-gray-50);
}

.is-rotate {
  transform: rotate(180deg);
}

/* 过渡动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.slide-down-enter-from,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.slide-down-enter-to,
.slide-down-leave-from {
  max-height: 500px;
  opacity: 1;
}

@media (max-width: 768px) {
  .search-bar__basic {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar__input {
    max-width: none;
  }

  .search-bar__actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
