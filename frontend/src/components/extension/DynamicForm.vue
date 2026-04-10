<template>
  <div v-if="schema?.fields?.length > 0" class="df-root">
    <div v-for="group in fieldGroups" :key="group.name" class="df-group">
      <div v-if="!disabled || fieldGroups.length > 2" class="df-group-title">{{ group.name }}</div>
      <div class="df-grid">
        <div v-for="field in group.fields" :key="field.key" class="df-row">
          <span class="df-label">
            {{ field.label }}
            <span v-if="field.required && !disabled" class="df-req">*</span>
          </span>

          <!-- ========== READ-ONLY MODE ========== -->
          <template v-if="disabled">
            <a
              v-if="field.type === 'user' && modelValue[field.key]"
              class="df-value df-link"
              :href="`/system/users?id=${modelValue[field.key]}`"
              @click.prevent="router.push(`/system/users?id=${modelValue[field.key]}`)"
            >{{ displayValue(field) }}</a>
            <span v-else class="df-value" :class="{ 'df-empty': !displayValue(field) }">
              {{ displayValue(field) || '-' }}
            </span>
          </template>

          <!-- ========== EDIT MODE ========== -->
          <template v-else>
            <!-- text / number / date -->
            <input
              v-if="field.type === 'text' || field.type === 'number' || field.type === 'date'"
              :value="modelValue[field.key]"
              :type="field.type === 'text' ? 'text' : field.type"
              class="df-input"
              :placeholder="field.label"
              :min="field.config?.min"
              :max="field.config?.max"
              @input="update(field.key, field.type === 'number' ? Number(($event.target as HTMLInputElement).value) : ($event.target as HTMLInputElement).value)"
            />

            <!-- textarea -->
            <textarea
              v-else-if="field.type === 'textarea'"
              :value="modelValue[field.key]"
              class="df-input df-textarea"
              rows="2"
              @input="update(field.key, ($event.target as HTMLTextAreaElement).value)"
            />

            <!-- boolean -->
            <label v-else-if="field.type === 'boolean'" class="df-check">
              <input
                type="checkbox"
                :checked="modelValue[field.key]"
                @change="update(field.key, ($event.target as HTMLInputElement).checked)"
              />
              {{ modelValue[field.key] ? '是' : '否' }}
            </label>

            <!-- select -->
            <select
              v-else-if="field.type === 'select'"
              :value="modelValue[field.key]"
              class="df-input df-select"
              @change="update(field.key, parseSelectValue(($event.target as HTMLSelectElement).value, field))"
            >
              <option :value="undefined">请选择</option>
              <option v-for="opt in field.config?.options || []" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>

            <!-- relation -->
            <select
              v-else-if="field.type === 'relation'"
              :value="modelValue[field.key]"
              class="df-input df-select"
              @change="update(field.key, Number(($event.target as HTMLSelectElement).value) || undefined)"
            >
              <option :value="undefined">请选择</option>
              <option v-for="opt in getRelationOptions(field)" :key="opt.id" :value="opt.id">{{ opt.label }}</option>
            </select>

            <!-- user -->
            <select
              v-else-if="field.type === 'user'"
              :value="modelValue[field.key]"
              class="df-input df-select"
              @change="update(field.key, Number(($event.target as HTMLSelectElement).value) || undefined)"
            >
              <option :value="undefined">请选择</option>
              <option v-for="u in userOptions" :key="u.id" :value="u.id">{{ u.name }}</option>
            </select>

            <!-- tags -->
            <select
              v-else-if="field.type === 'tags'"
              :value="modelValue[field.key]"
              class="df-input df-select"
              multiple
              style="min-height: 52px;"
              @change="updateMulti(field.key, $event)"
            >
              <option v-for="opt in field.config?.options || []" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>

            <!-- fallback -->
            <input
              v-else
              :value="modelValue[field.key]"
              class="df-input"
              :placeholder="field.type + ' 暂不支持'"
              @input="update(field.key, ($event.target as HTMLInputElement).value)"
            />
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
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

const router = useRouter()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
}>()

const fieldGroups = computed(() => {
  const groups = new Map<string, FieldSchema[]>()
  for (const f of props.schema?.fields || []) {
    const g = f.group || '其他'
    if (!groups.has(g)) groups.set(g, [])
    groups.get(g)!.push(f)
  }
  return Array.from(groups.entries()).map(([name, fields]) => ({ name, fields }))
})

// Display value for read-only mode
function displayValue(field: FieldSchema): string {
  const val = props.modelValue[field.key]
  if (val == null || val === '') return ''

  if (field.type === 'boolean') return val ? '是' : '否'

  if (field.type === 'select') {
    const opt = field.config?.options?.find((o: any) => o.value === val || String(o.value) === String(val))
    return opt?.label || String(val)
  }

  if (field.type === 'relation') {
    const opts = relationCache.value[field.config?.target || ''] || []
    const found = opts.find(o => o.id === Number(val) || String(o.id) === String(val))
    return found?.label || String(val)
  }

  if (field.type === 'user') {
    const found = userOptions.value.find(u => u.id === Number(val) || String(u.id) === String(val))
    return found?.name || String(val)
  }

  if (field.type === 'tags' && Array.isArray(val)) {
    return val.map(v => {
      const opt = field.config?.options?.find((o: any) => o.value === v)
      return opt?.label || v
    }).join(', ')
  }

  return String(val)
}

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
  const numVal = Number(val)
  if (!isNaN(numVal) && field.config?.options?.some((o: any) => typeof o.value === 'number')) {
    return numVal
  }
  return val
}

const relationCache = ref<Record<string, { id: number; label: string }[]>>({})
const userOptions = ref<{ id: number; name: string }[]>([])

function getRelationOptions(field: FieldSchema) {
  return relationCache.value[field.config?.target || ''] || []
}

async function loadRelationOptions() {
  const fields = props.schema?.fields || []
  const targets = new Set<string>()
  let needUsers = false

  for (const f of fields) {
    if (f.type === 'relation' && f.config?.target) targets.add(f.config.target)
    if (f.type === 'user') needUsers = true
  }

  for (const target of targets) {
    try {
      let url = ''
      let labelField = 'name'
      if (target === 'org_units') { url = '/org-units/list'; labelField = 'unitName' }
      else if (target === 'majors') { url = '/academic/majors'; labelField = 'majorName' }
      else if (target === 'grades') { url = '/students/grades'; labelField = 'gradeName' }
      else { url = `/${target}` }

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

<style scoped>
/* ===== Layout ===== */
.df-group { margin-bottom: 6px; }
.df-group:last-child { margin-bottom: 0; }
.df-group-title {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #9ca3af;
  margin-bottom: 4px;
}
.df-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px 20px;
}
.df-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
}
.df-label {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 500;
  color: #9ca3af;
  white-space: nowrap;
  min-width: 48px;
  text-align: right;
}
.df-label .df-req { color: #ef4444; margin-left: 1px; }

/* ===== Read-only value ===== */
.df-value {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.df-value.df-empty {
  color: #d1d5db;
  font-weight: 400;
}
.df-value.df-link {
  color: #2563eb;
  cursor: pointer;
  text-decoration: none;
}
.df-value.df-link:hover {
  text-decoration: underline;
}

/* ===== Edit mode inputs ===== */
.df-input {
  flex: 1;
  min-width: 0;
  padding: 3px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 5px;
  font-size: 12px;
  font-family: inherit;
  height: 26px;
  background: #fff;
  color: #111827;
  outline: none;
  transition: border-color 0.15s;
}
.df-input:focus { border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,0.08); }
.df-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%239ca3af' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-position: right 8px center;
  background-repeat: no-repeat;
  padding-right: 24px;
}
.df-textarea { height: auto; resize: vertical; min-height: 48px; }
.df-check { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #374151; cursor: pointer; }
</style>
