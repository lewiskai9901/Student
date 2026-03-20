<script setup lang="ts">
/**
 * PreventiveActionEditor - Preventive action editor
 *
 * Rich textarea for describing preventive measures to avoid recurrence.
 * Includes guidance text about what preventive actions should cover.
 */
import { computed } from 'vue'
import { ShieldCheck } from 'lucide-vue-next'

const props = withDefaults(
  defineProps<{
    modelValue: string
    readonly?: boolean
  }>(),
  {
    readonly: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const content = computed({
  get: () => props.modelValue || '',
  set: (val: string) => emit('update:modelValue', val),
})

const guidanceItems = [
  '制度或流程改进：是否需要更新操作规范、检查标准或工作流程？',
  '培训与教育：相关人员是否需要补充培训或安全教育？',
  '技术手段：是否可以通过技术改造或自动化手段防止问题再次发生？',
  '监督机制：是否需要增加检查频率或调整监控措施？',
  '资源保障：是否需要增加人员、设备或预算保障？',
]
</script>

<template>
  <div class="preventive-action-editor">
    <!-- Guidance -->
    <el-collapse v-if="!readonly" class="guidance-collapse mb-3">
      <el-collapse-item>
        <template #title>
          <div class="flex items-center gap-1 text-sm text-gray-500">
            <ShieldCheck class="w-4 h-4" />
            <span>预防措施编写指引</span>
          </div>
        </template>
        <div class="guidance-content">
          <p class="text-sm text-gray-500 mb-2">
            预防措施旨在消除问题根因，防止类似问题再次发生。请从以下角度思考：
          </p>
          <ul class="guidance-list">
            <li
              v-for="(item, index) in guidanceItems"
              :key="index"
              class="text-sm text-gray-600"
            >
              {{ item }}
            </li>
          </ul>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- Editor -->
    <template v-if="!readonly">
      <el-input
        v-model="content"
        type="textarea"
        :rows="6"
        placeholder="请描述预防措施，包括具体行动计划、责任人、完成期限等..."
        maxlength="3000"
        show-word-limit
      />
      <div class="text-gray-400 text-xs mt-1">
        建议包含：措施描述、负责人、预计完成时间、验证方式
      </div>
    </template>

    <!-- Readonly view -->
    <template v-else>
      <div v-if="content" class="readonly-content">
        <div class="whitespace-pre-wrap text-sm leading-relaxed">{{ content }}</div>
      </div>
      <el-empty v-else description="暂无预防措施" :image-size="48" />
    </template>
  </div>
</template>

<style scoped>
.preventive-action-editor {
  padding: 4px 0;
}

.guidance-collapse {
  border: none;
}

.guidance-collapse :deep(.el-collapse-item__header) {
  height: 36px;
  line-height: 36px;
  border-bottom: none;
  background: transparent;
}

.guidance-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.guidance-content {
  padding: 4px 0;
}

.guidance-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.guidance-list li {
  position: relative;
  padding: 4px 0 4px 16px;
  line-height: 1.6;
}

.guidance-list li::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 12px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: #c0c4cc;
}

.readonly-content {
  padding: 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
</style>
