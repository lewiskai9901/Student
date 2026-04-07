<template>
  <div v-if="schema?.fields?.length > 0">
    <div v-for="group in fieldGroups" :key="group.name" class="tm-section">
      <h4 class="tm-section-title">
        {{ group.name }}
      </h4>
      <div class="tm-fields" :class="group.fields.length > 1 ? 'tm-cols-2' : ''">
        <div v-for="field in group.fields" :key="field.key" class="tm-field">
          <label class="tm-label">
            {{ field.label }}
            <span v-if="field.required" class="req">*</span>
            <span v-if="field.system && showSystemBadge" style="font-size: 10px; color: #9ca3af; font-weight: 400; margin-left: 4px;">(系统)</span>
          </label>

          <!-- text -->
          <input
            v-if="field.type === 'text'"
            :value="modelValue[field.key]"
            class="tm-input"
            :placeholder="field.label"
            :disabled="disabled"
            @input="update(field.key, ($event.target as HTMLInputElement).value)"
          />

          <!-- textarea -->
          <textarea
            v-else-if="field.type === 'textarea'"
            :value="modelValue[field.key]"
            class="tm-textarea"
            rows="2"
            :disabled="disabled"
            @input="update(field.key, ($event.target as HTMLTextAreaElement).value)"
          />

          <!-- number -->
          <input
            v-else-if="field.type === 'number'"
            :value="modelValue[field.key]"
            type="number"
            class="tm-input"
            :min="field.config?.min"
            :max="field.config?.max"
            :disabled="disabled"
            @input="update(field.key, Number(($event.target as HTMLInputElement).value))"
          />

          <!-- date -->
          <input
            v-else-if="field.type === 'date'"
            :value="modelValue[field.key]"
            type="date"
            class="tm-input"
            :disabled="disabled"
            @input="update(field.key, ($event.target as HTMLInputElement).value)"
          />

          <!-- boolean -->
          <label
            v-else-if="field.type === 'boolean'"
            style="display: flex; align-items: center; gap: 6px; font-size: 13px; cursor: pointer;"
          >
            <input
              type="checkbox"
              :checked="modelValue[field.key]"
              :disabled="disabled"
              @change="update(field.key, ($event.target as HTMLInputElement).checked)"
            />
            {{ modelValue[field.key] ? '是' : '否' }}
          </label>

          <!-- select -->
          <select
            v-else-if="field.type === 'select'"
            :value="modelValue[field.key]"
            class="tm-field-select"
            :disabled="disabled"
            @change="update(field.key, parseSelectValue(($event.target as HTMLSelectElement).value, field))"
          >
            <option :value="undefined">请选择</option>
            <option
              v-for="opt in field.config?.options || []"
              :key="opt.value"
              :value="opt.value"
            >{{ opt.label }}</option>
          </select>

          <!-- relation (简化版: 远程选择器) -->
          <select
            v-else-if="field.type === 'relation'"
            :value="modelValue[field.key]"
            class="tm-field-select"
            :disabled="disabled"
            @change="update(field.key, Number(($event.target as HTMLSelectElement).value) || undefined)"
          >
            <option :value="undefined">请选择</option>
            <option
              v-for="opt in getRelationOptions(field)"
              :key="opt.id"
              :value="opt.id"
            >{{ opt.label }}</option>
          </select>

          <!-- user (用户选择器) -->
          <select
            v-else-if="field.type === 'user'"
            :value="modelValue[field.key]"
            class="tm-field-select"
            :disabled="disabled"
            @change="update(field.key, Number(($event.target as HTMLSelectElement).value) || undefined)"
          >
            <option :value="undefined">请选择</option>
            <option v-for="u in userOptions" :key="u.id" :value="u.id">{{ u.name }}</option>
          </select>

          <!-- tags (多选) -->
          <select
            v-else-if="field.type === 'tags'"
            :value="modelValue[field.key]"
            class="tm-field-select"
            multiple
            style="min-height: 60px;"
            :disabled="disabled"
            @change="updateMulti(field.key, $event)"
          >
            <option
              v-for="opt in field.config?.options || []"
              :key="opt.value"
              :value="opt.value"
            >{{ opt.label }}</option>
          </select>

          <!-- fallback: unknown type -->
          <input
            v-else
            :value="modelValue[field.key]"
            class="tm-input"
            :placeholder="field.type + ' 类型暂不支持'"
            :disabled="disabled"
            @input="update(field.key, ($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { http } from '@/utils/request'

interface FieldSchema {
  key: string
  label: string
  type: string
  group?: string
  required?: boolean
  system?: boolean
  config?: Record<string, any>
}

interface Schema {
  fields: FieldSchema[]
}

const props = withDefaults(defineProps<{
  schema: Schema
  modelValue: Record<string, any>
  disabled?: boolean
  showSystemBadge?: boolean
}>(), {
  disabled: false,
  showSystemBadge: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
}>()

// Group fields by group name
const fieldGroups = computed(() => {
  const groups = new Map<string, FieldSchema[]>()
  for (const f of props.schema?.fields || []) {
    const g = f.group || '其他'
    if (!groups.has(g)) groups.set(g, [])
    groups.get(g)!.push(f)
  }
  return Array.from(groups.entries()).map(([name, fields]) => ({ name, fields }))
})

// Update a single field
function update(key: string, value: any) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}

function updateMulti(key: string, event: Event) {
  const select = event.target as HTMLSelectElement
  const values = Array.from(select.selectedOptions).map(o => o.value)
  emit('update:modelValue', { ...props.modelValue, [key]: values })
}

function parseSelectValue(val: string, field: FieldSchema) {
  if (val === 'undefined' || val === '') return undefined
  // If options have numeric values, parse as number
  const numVal = Number(val)
  if (!isNaN(numVal) && field.config?.options?.some((o: any) => typeof o.value === 'number')) {
    return numVal
  }
  return val
}

// Relation options cache
const relationCache = ref<Record<string, { id: number; label: string }[]>>({})
const userOptions = ref<{ id: number; name: string }[]>([])

function getRelationOptions(field: FieldSchema) {
  const target = field.config?.target
  if (!target) return []
  return relationCache.value[target] || []
}

async function loadRelationOptions() {
  const fields = props.schema?.fields || []
  const targets = new Set<string>()
  let needUsers = false

  for (const f of fields) {
    if (f.type === 'relation' && f.config?.target) targets.add(f.config.target)
    if (f.type === 'user') needUsers = true
  }

  // Load each relation target
  for (const target of targets) {
    try {
      let url = ''
      let labelField = 'name'

      if (target === 'org_units') {
        url = '/org-units/list'
        labelField = 'unitName'
      } else if (target === 'majors') {
        url = '/academic/majors'
        labelField = 'majorName'
      } else if (target === 'grades') {
        url = '/students/grades'
        labelField = 'gradeName'
      } else {
        url = `/${target}`
      }

      const res = await http.get(url)
      const data = (res as any).data || res
      const items = Array.isArray(data) ? data : data.records || []
      relationCache.value[target] = items.map((i: any) => ({
        id: i.id,
        label: i[labelField] || i.name || i.unitName || `#${i.id}`,
      }))
    } catch {
      relationCache.value[target] = []
    }
  }

  // Load users if needed
  if (needUsers) {
    try {
      const res = await http.get('/users', { params: { pageSize: 500 } })
      const data = (res as any).data || res
      const items = Array.isArray(data) ? data : data.records || []
      userOptions.value = items.map((u: any) => ({ id: u.id, name: u.realName || u.username }))
    } catch {
      userOptions.value = []
    }
  }
}

onMounted(loadRelationOptions)
</script>
