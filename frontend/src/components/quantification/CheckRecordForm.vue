<template>
  <div class="p-6">
    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></div>
    </div>

    <form v-else @submit.prevent="handleSubmit" class="space-y-6">
      <!-- 基本信息 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">检查基本信息</h3>
        </div>
        <div class="space-y-4 p-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">检查类型 <span class="text-red-500">*</span></label>
              <select v-model="formData.typeId" @change="handleTypeChange" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                <option :value="null">请选择检查类型</option>
                <option v-for="item in checkTypeList" :key="item.id" :value="item.id">{{ item.typeName }}</option>
              </select>
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-gray-700">检查对象 <span class="text-red-500">*</span></label>
              <select v-model="formData.targetId" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none">
                <option :value="null">请选择检查对象</option>
                <option v-for="item in targetList" :key="item.id" :value="item.id">{{ item.name }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="mb-1.5 block text-sm font-medium text-gray-700">检查时间 <span class="text-red-500">*</span></label>
            <input type="datetime-local" v-model="formData.checkDate" class="h-9 w-full rounded-lg border border-gray-200 px-3 text-sm focus:border-blue-500 focus:outline-none" />
          </div>
          <div>
            <label class="mb-1.5 block text-sm font-medium text-gray-700">备注</label>
            <textarea v-model="formData.remark" rows="2" maxlength="500" placeholder="请输入检查备注" class="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"></textarea>
          </div>
        </div>
      </div>

      <!-- 加权配置提示 -->
      <div class="rounded-lg border border-blue-200 bg-blue-50 p-4">
        <div class="flex items-center gap-2 text-sm text-blue-700">
          <Info class="h-4 w-4 flex-shrink-0" />
          <span>加权方案已在检查模板中统一配置，创建检查时将自动应用模板中的加权设置</span>
        </div>
      </div>

      <!-- 检查明细 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">检查明细</h3>
          <span class="text-sm font-semibold text-blue-600">总分: {{ totalScore }}/{{ maxScore }}</span>
        </div>
        <div class="p-4">
          <div v-if="checkItems.length > 0" class="space-y-4">
            <div v-for="(item, index) in checkItems" :key="index" class="rounded-lg border border-gray-200 p-4">
              <div class="mb-3 flex items-center justify-between border-b border-gray-100 pb-3">
                <span class="text-sm font-semibold text-gray-900">{{ item.itemName }}</span>
                <span class="text-sm font-medium text-blue-600">{{ item.itemScore }}/{{ item.maxScore }}分</span>
              </div>
              <div class="space-y-3">
                <div>
                  <label class="mb-1.5 block text-xs text-gray-500">{{ item.itemName }}得分</label>
                  <div class="flex items-center gap-4">
                    <input type="range" v-model.number="item.itemScore" :max="item.maxScore" class="h-2 flex-1 cursor-pointer appearance-none rounded-full bg-gray-200" />
                    <input type="number" v-model.number="item.itemScore" :max="item.maxScore" min="0" class="h-8 w-16 rounded border border-gray-200 px-2 text-center text-sm" />
                  </div>
                </div>
                <div>
                  <label class="mb-1 block text-xs text-gray-500">扣分原因</label>
                  <input type="text" v-model="item.deductionReason" placeholder="如有扣分请填写原因" maxlength="200" class="h-8 w-full rounded border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none" />
                </div>
                <div>
                  <label class="mb-1 block text-xs text-gray-500">备注</label>
                  <input type="text" v-model="item.remark" placeholder="可填写具体问题或建议" maxlength="200" class="h-8 w-full rounded border border-gray-200 px-2 text-sm focus:border-blue-500 focus:outline-none" />
                </div>
              </div>
            </div>
          </div>
          <div v-else class="flex flex-col items-center justify-center py-12 text-gray-400">
            <ClipboardList class="mb-2 h-12 w-12" />
            <span class="text-sm">请先选择检查类型</span>
          </div>
        </div>
      </div>

      <!-- 照片上传 -->
      <div class="rounded-lg border border-gray-200 bg-white">
        <div class="border-b border-gray-200 px-4 py-3">
          <h3 class="text-sm font-semibold text-gray-900">检查照片</h3>
        </div>
        <div class="p-4">
          <div class="grid grid-cols-6 gap-4">
            <div v-for="(photo, index) in photoList" :key="index" class="group relative aspect-square overflow-hidden rounded-lg border border-gray-200">
              <img :src="photo.url" class="h-full w-full object-cover" />
              <button type="button" @click="removePhoto(index)" class="absolute right-1 top-1 hidden rounded-full bg-black/50 p-1 text-white group-hover:block hover:bg-black/70">
                <X class="h-3 w-3" />
              </button>
            </div>
            <label v-if="photoList.length < 6" class="flex aspect-square cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 text-gray-400 transition-colors hover:border-blue-400 hover:text-blue-500">
              <Plus class="h-8 w-8" />
              <span class="mt-1 text-xs">上传照片</span>
              <input type="file" accept="image/*" class="hidden" @change="handlePhotoUpload" />
            </label>
          </div>
          <p class="mt-2 text-xs text-gray-400">最多上传6张照片</p>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="flex justify-end gap-3 border-t border-gray-200 pt-4">
        <button type="button" class="h-9 rounded-lg border border-gray-300 bg-white px-4 text-sm text-gray-700 hover:bg-gray-50" @click="$emit('close')">取消</button>
        <button type="submit" :disabled="submitting" class="h-9 rounded-lg bg-blue-600 px-4 text-sm text-white hover:bg-blue-700 disabled:opacity-50">
          <span v-if="submitting" class="mr-1.5 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></span>
          {{ mode === 'add' ? '确定新增' : '确定修改' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, X, Info, ClipboardList } from 'lucide-vue-next'

interface Props { mode: 'add' | 'edit', recordId?: number | null }
const props = defineProps<Props>()
const emit = defineEmits<{ success: [], close: [] }>()

const loading = ref(false)
const submitting = ref(false)
const checkTypeList = ref<any[]>([])
const targetList = ref<any[]>([])
const checkItems = ref<any[]>([])
const photoList = ref<any[]>([])

const formData = reactive({
  typeId: null as number | null, targetId: null as number | null, checkDate: '', remark: ''
})

const totalScore = computed(() => checkItems.value.reduce((sum, item) => sum + (item.itemScore || 0), 0))
const maxScore = computed(() => checkItems.value.reduce((sum, item) => sum + (item.maxScore || 0), 0))

const loadCheckTypeList = async () => {
  checkTypeList.value = [{ id: 1, typeName: '宿舍卫生检查' }, { id: 2, typeName: '教室检查' }, { id: 3, typeName: '纪律检查' }]
}

const loadTargetList = async (targetType: string) => {
  if (targetType === 'dormitory') {
    targetList.value = [{ id: 1, name: '1号楼201' }, { id: 2, name: '1号楼202' }, { id: 3, name: '2号楼301' }]
  } else if (targetType === 'class') {
    targetList.value = [{ id: 1, name: '计算机1班' }, { id: 2, name: '计算机2班' }, { id: 3, name: '软件工程1班' }]
  }
}

const handleTypeChange = async (e: Event) => {
  const typeId = formData.typeId
  formData.targetId = null
  targetList.value = []
  try {
    if (typeId === 1) {
      checkItems.value = [
        { itemName: '地面清洁', maxScore: 20, itemScore: 20, deductionReason: '', remark: '' },
        { itemName: '床铺整理', maxScore: 20, itemScore: 20, deductionReason: '', remark: '' },
        { itemName: '物品摆放', maxScore: 20, itemScore: 20, deductionReason: '', remark: '' },
        { itemName: '垃圾处理', maxScore: 15, itemScore: 15, deductionReason: '', remark: '' },
        { itemName: '空气质量', maxScore: 15, itemScore: 15, deductionReason: '', remark: '' },
        { itemName: '安全检查', maxScore: 10, itemScore: 10, deductionReason: '', remark: '' }
      ]
      await loadTargetList('dormitory')
    } else if (typeId === 2) {
      checkItems.value = [
        { itemName: '黑板清洁', maxScore: 15, itemScore: 15, deductionReason: '', remark: '' },
        { itemName: '地面清洁', maxScore: 25, itemScore: 25, deductionReason: '', remark: '' },
        { itemName: '桌椅排列', maxScore: 20, itemScore: 20, deductionReason: '', remark: '' },
        { itemName: '垃圾清理', maxScore: 20, itemScore: 20, deductionReason: '', remark: '' },
        { itemName: '门窗清洁', maxScore: 20, itemScore: 20, deductionReason: '', remark: '' }
      ]
      await loadTargetList('class')
    }
  } catch (error) { ElMessage.error('加载检查项目失败') }
}

const handlePhotoUpload = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files && input.files[0]) {
    const file = input.files[0]
    const url = URL.createObjectURL(file)
    photoList.value.push({ url, file })
    input.value = ''
  }
}

const removePhoto = (index: number) => { photoList.value.splice(index, 1) }

const handleSubmit = async () => {
  if (!formData.typeId) { ElMessage.warning('请选择检查类型'); return }
  if (!formData.targetId) { ElMessage.warning('请选择检查对象'); return }
  if (!formData.checkDate) { ElMessage.warning('请选择检查时间'); return }
  submitting.value = true
  try {
    const submitData = {
      ...formData, details: checkItems.value, totalScore: totalScore.value, maxScore: maxScore.value,
      scoreRate: maxScore.value > 0 ? Math.round((totalScore.value / maxScore.value) * 100) : 0,
      photos: photoList.value.map(p => p.url).filter(Boolean)
    }
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success(`检查记录${props.mode === 'add' ? '创建' : '更新'}成功`)
    emit('success')
  } catch (error: any) { ElMessage.error(error.message || '操作失败') } finally { submitting.value = false }
}

onMounted(() => { loadCheckTypeList() })
</script>
