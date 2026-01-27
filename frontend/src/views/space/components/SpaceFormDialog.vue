<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="480px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="space-y-4">
      <!-- 校区表单 -->
      <template v-if="spaceType === 'CAMPUS'">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs text-gray-500 mb-1">校区名称 <span class="text-red-500">*</span></label>
            <el-input v-model="form.spaceName" placeholder="如：主校区" />
          </div>
          <div>
            <label class="block text-xs text-gray-500 mb-1">校区编码</label>
            <el-input v-model="form.spaceCode" placeholder="留空自动生成" />
          </div>
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">详细地址</label>
          <el-input v-model="form.description" placeholder="校区的详细地址" />
        </div>
      </template>

      <!-- 楼栋表单 -->
      <template v-if="spaceType === 'BUILDING'">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs text-gray-500 mb-1">楼栋名称 <span class="text-red-500">*</span></label>
            <el-input v-model="form.spaceName" placeholder="如：1号宿舍楼" />
          </div>
          <div>
            <label class="block text-xs text-gray-500 mb-1">楼栋号 <span class="text-red-500">*</span></label>
            <el-input v-model="form.buildingNo" placeholder="如：1、A">
              <template #append>号楼</template>
            </el-input>
          </div>
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">
            楼栋类型 <span class="text-red-500">*</span>
            <span v-if="mode === 'edit'" class="text-gray-400 font-normal ml-1">(不可修改)</span>
          </label>
          <div class="grid grid-cols-4 gap-2">
            <div
              v-for="type in buildingTypes"
              :key="type.value"
              class="border rounded-lg p-3 text-center transition-all"
              :class="[
                form.buildingType === type.value
                  ? 'border-blue-500 bg-blue-50 text-blue-700'
                  : 'border-gray-200 text-gray-600',
                mode === 'edit' ? 'cursor-not-allowed opacity-60' : 'cursor-pointer hover:border-gray-300'
              ]"
              @click="mode === 'create' && (form.buildingType = type.value)"
            >
              <div class="text-sm font-medium">{{ type.label }}</div>
            </div>
          </div>
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">备注</label>
          <el-input v-model="form.description" placeholder="可选" />
        </div>
      </template>

      <!-- 楼层表单 -->
      <template v-if="spaceType === 'FLOOR'">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs text-gray-500 mb-1">楼层名称 <span class="text-red-500">*</span></label>
            <el-input v-model="form.spaceName" placeholder="如：一楼" />
          </div>
          <div>
            <label class="block text-xs text-gray-500 mb-1">
              楼层号 <span class="text-red-500">*</span>
              <span v-if="mode === 'edit'" class="text-gray-400 font-normal ml-1">(不可修改)</span>
            </label>
            <el-input-number
              v-model="form.floorNumber"
              :min="-5"
              :max="100"
              class="w-full"
              controls-position="right"
              :disabled="mode === 'edit'"
            />
          </div>
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">备注</label>
          <el-input v-model="form.description" placeholder="可选" />
        </div>
      </template>

      <!-- 房间表单 -->
      <template v-if="spaceType === 'ROOM'">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs text-gray-500 mb-1">房间名称 <span class="text-red-500">*</span></label>
            <el-input v-model="form.spaceName" placeholder="如：301宿舍" />
          </div>
          <div>
            <label class="block text-xs text-gray-500 mb-1">房间号 <span class="text-red-500">*</span></label>
            <el-input v-model="form.roomNo" placeholder="如：301">
              <template #append>室</template>
            </el-input>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs text-gray-500 mb-1">
              房间类型 <span class="text-red-500">*</span>
              <span v-if="mode === 'edit'" class="text-gray-400 font-normal ml-1">(不可修改)</span>
            </label>
            <el-select v-model="form.roomType" placeholder="选择类型" class="w-full" :disabled="mode === 'edit'">
              <el-option v-for="t in roomTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </div>
          <div>
            <label class="block text-xs text-gray-500 mb-1">容量</label>
            <el-input-number
              v-model="form.capacity"
              :min="0"
              :max="1000"
              class="w-full"
              controls-position="right"
              placeholder="人数"
            />
          </div>
        </div>

        <!-- 宿舍特有 -->
        <template v-if="form.roomType === 'DORMITORY'">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-xs text-gray-500 mb-1">性别限制</label>
              <div class="grid grid-cols-3 gap-2">
                <div
                  v-for="g in genderTypes"
                  :key="g.value"
                  class="border rounded-lg py-2 text-center cursor-pointer transition-all text-sm"
                  :class="form.genderType === g.value
                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                    : 'border-gray-200 hover:border-gray-300 text-gray-600'"
                  @click="form.genderType = g.value"
                >
                  {{ g.label }}
                </div>
              </div>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">归属班级</label>
              <el-select
                v-model="form.classId"
                placeholder="可选"
                class="w-full"
                clearable
                filterable
                :loading="loadingClasses"
              >
                <el-option v-for="cls in classList" :key="cls.id" :label="cls.name" :value="cls.id" />
              </el-select>
            </div>
          </div>
        </template>

        <!-- 教室特有 -->
        <template v-if="form.roomType === 'CLASSROOM'">
          <div>
            <label class="block text-xs text-gray-500 mb-1">固定班级</label>
            <el-select
              v-model="form.classId"
              placeholder="指定为某班级的固定教室（可选）"
              class="w-full"
              clearable
              filterable
              :loading="loadingClasses"
            >
              <el-option v-for="cls in classList" :key="cls.id" :label="cls.name" :value="cls.id" />
            </el-select>
          </div>
        </template>

        <div>
          <label class="block text-xs text-gray-500 mb-1">备注</label>
          <el-input v-model="form.description" placeholder="可选" />
        </div>
      </template>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ mode === 'create' ? '创建' : '保存' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createSpace, updateSpace } from '@/api/space'
import type { SpaceDTO, SpaceType, RoomType, BuildingType, CreateSpaceRequest, GenderType } from '@/types/space'
import { getClassList } from '@/api/organization'

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  spaceType: SpaceType
  parentId?: number
  editData?: SpaceDTO | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const dialogTitle = computed(() => {
  const typeNames: Record<SpaceType, string> = {
    CAMPUS: '校区',
    BUILDING: '楼栋',
    FLOOR: '楼层',
    ROOM: '房间'
  }
  return props.mode === 'create' ? `新增${typeNames[props.spaceType]}` : `编辑${typeNames[props.spaceType]}`
})

// 选项数据
const buildingTypes = [
  { value: 'DORMITORY', label: '宿舍楼' },
  { value: 'TEACHING', label: '教学楼' },
  { value: 'OFFICE', label: '办公楼' },
  { value: 'MIXED', label: '综合楼' }
]

const roomTypes = [
  { value: 'DORMITORY', label: '宿舍' },
  { value: 'CLASSROOM', label: '教室' },
  { value: 'LAB', label: '实验室' },
  { value: 'COMPUTER_ROOM', label: '机房' },
  { value: 'TRAINING', label: '实训室' },
  { value: 'MEETING', label: '会议室' },
  { value: 'OFFICE', label: '办公室' },
  { value: 'STORAGE', label: '仓库' },
  { value: 'OTHER', label: '其他' }
]

const genderTypes = [
  { value: 0, label: '不限' },
  { value: 1, label: '男' },
  { value: 2, label: '女' }
]

// 表单
const form = ref({
  spaceName: '',
  spaceCode: '',
  buildingType: '' as BuildingType | '',
  buildingNo: '',
  roomType: '' as RoomType | '',
  roomNo: '',
  floorNumber: undefined as number | undefined,
  capacity: undefined as number | undefined,
  genderType: 0 as GenderType,
  classId: undefined as number | undefined,
  description: ''
})

// 班级列表
const classList = ref<{ id: number; name: string }[]>([])
const loadingClasses = ref(false)

async function loadClasses() {
  loadingClasses.value = true
  try {
    const res = await getClassList()
    classList.value = res?.map((c: any) => ({ id: c.id, name: c.className || c.name })) || []
  } catch (error) {
    console.error('加载班级列表失败:', error)
  } finally {
    loadingClasses.value = false
  }
}

onMounted(() => {
  loadClasses()
})

// 监听编辑数据
watch(
  () => props.editData,
  (data) => {
    if (data && props.mode === 'edit') {
      form.value = {
        spaceName: data.spaceName,
        spaceCode: data.spaceCode || '',
        buildingType: (data.buildingType as BuildingType) || '',
        buildingNo: data.buildingNo || '',
        roomType: (data.roomType as RoomType) || '',
        roomNo: data.roomNo || '',
        floorNumber: data.floorNumber,
        capacity: data.capacity,
        genderType: data.genderType ?? 0,
        classId: data.classId,
        description: data.description || ''
      }
    } else {
      resetForm()
    }
  },
  { immediate: true }
)

function resetForm() {
  form.value = {
    spaceName: '',
    spaceCode: '',
    buildingType: '',
    buildingNo: '',
    roomType: '',
    roomNo: '',
    floorNumber: undefined,
    capacity: undefined,
    genderType: 0,
    classId: undefined,
    description: ''
  }
}

function handleClose() {
  resetForm()
}

// 提交
const submitting = ref(false)

async function handleSubmit() {
  // 基本校验
  if (!form.value.spaceName) {
    ElMessage.warning('请输入名称')
    return
  }
  if (props.spaceType === 'BUILDING' && !form.value.buildingNo) {
    ElMessage.warning('请输入楼栋号')
    return
  }
  if (props.spaceType === 'BUILDING' && !form.value.buildingType) {
    ElMessage.warning('请选择楼栋类型')
    return
  }
  if (props.spaceType === 'FLOOR' && form.value.floorNumber === undefined) {
    ElMessage.warning('请输入楼层号')
    return
  }
  if (props.spaceType === 'ROOM' && !form.value.roomNo) {
    ElMessage.warning('请输入房间号')
    return
  }
  if (props.spaceType === 'ROOM' && !form.value.roomType) {
    ElMessage.warning('请选择房间类型')
    return
  }

  submitting.value = true
  try {
    if (props.mode === 'create') {
      const request: CreateSpaceRequest = {
        spaceType: props.spaceType,
        spaceName: form.value.spaceName,
        spaceCode: form.value.spaceCode || undefined,
        parentId: props.parentId,
        buildingType: form.value.buildingType || undefined,
        buildingNo: form.value.buildingNo || undefined,
        roomType: form.value.roomType || undefined,
        roomNo: form.value.roomNo || undefined,
        floorNumber: form.value.floorNumber,
        capacity: form.value.capacity,
        genderType: form.value.roomType === 'DORMITORY' ? form.value.genderType : undefined,
        classId: form.value.classId,
        description: form.value.description || undefined
      }
      await createSpace(request)
      ElMessage.success('创建成功')
    } else if (props.editData) {
      await updateSpace(props.editData.id, {
        spaceName: form.value.spaceName,
        buildingNo: form.value.buildingNo || undefined,
        roomNo: form.value.roomNo || undefined,
        capacity: form.value.capacity,
        genderType: form.value.roomType === 'DORMITORY' ? form.value.genderType : undefined,
        classId: form.value.classId,
        description: form.value.description || undefined
      })
      ElMessage.success('保存成功')
    }

    visible.value = false
    emit('success')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.w-full {
  width: 100%;
}
</style>
