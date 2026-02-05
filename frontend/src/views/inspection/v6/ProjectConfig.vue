<template>
  <div class="v6-project-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
          <span>项目配置</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本配置" name="basic">
          <el-form :model="formData" label-width="120px" style="max-width: 600px">
            <el-form-item label="项目名称">
              <el-input v-model="formData.projectName" />
            </el-form-item>
            <el-form-item label="基础分">
              <el-input-number v-model="formData.baseScore" :min="0" :max="200" />
            </el-form-item>
            <el-form-item label="周期类型">
              <el-select v-model="formData.cycleType">
                <el-option label="每日" value="DAILY" />
                <el-option label="每周" value="WEEKLY" />
                <el-option label="每月" value="MONTHLY" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker v-model="formData.startDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker v-model="formData.endDate" type="date" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="检查员配置" name="inspectors">
          <el-button type="primary" @click="addInspector" style="margin-bottom: 16px">添加检查员</el-button>
          <el-table :data="inspectors">
            <el-table-column prop="inspectorName" label="检查员" />
            <el-table-column prop="scopeType" label="范围类型" />
            <el-table-column prop="isDefault" label="默认分配">
              <template #default="{ row }">
                <el-tag :type="row.isDefault ? 'success' : 'info'">{{ row.isDefault ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="danger" @click="removeInspector(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="高级设置" name="advanced">
          <el-form :model="formData" label-width="160px" style="max-width: 600px">
            <el-form-item label="检查员分配模式">
              <el-radio-group v-model="formData.inspectorAssignmentMode">
                <el-radio value="FREE">自由领取</el-radio>
                <el-radio value="ASSIGNED">指定分配</el-radio>
                <el-radio value="HYBRID">混合模式</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="共享场所策略">
              <el-radio-group v-model="formData.sharedSpaceStrategy">
                <el-radio value="RATIO">按比例</el-radio>
                <el-radio value="AVERAGE">平均</el-radio>
                <el-radio value="FULL">全部</el-radio>
                <el-radio value="MAIN_ONLY">仅主要</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="检查项加权">
              <el-switch v-model="formData.enableItemWeight" />
            </el-form-item>
            <el-form-item label="启用双周期">
              <el-switch v-model="formData.enableDualCycle" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { v6ProjectApi, v6AssignmentApi } from '@/api/v6Inspection'

const router = useRouter()
const route = useRoute()
const projectId = Number(route.params.id)

const activeTab = ref('basic')
const saving = ref(false)
const inspectors = ref<any[]>([])

const formData = reactive({
  projectName: '',
  baseScore: 100,
  cycleType: 'DAILY',
  startDate: '',
  endDate: '',
  inspectorAssignmentMode: 'FREE',
  sharedSpaceStrategy: 'RATIO',
  enableItemWeight: false,
  enableDualCycle: false
})

const goBack = () => router.push(`/inspection/v6/projects/${projectId}`)

const handleSave = async () => {
  saving.value = true
  try {
    await v6ProjectApi.updateConfig(projectId, formData)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const loadData = async () => {
  try {
    const project = await v6ProjectApi.getById(projectId)
    Object.assign(formData, project)
    inspectors.value = await v6AssignmentApi.getProjectInspectors(projectId)
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

const addInspector = () => {
  ElMessage.info('添加检查员功能开发中')
}

const removeInspector = async (row: any) => {
  try {
    await v6AssignmentApi.removeProjectInspector(projectId, row.inspectorId)
    ElMessage.success('移除成功')
    inspectors.value = await v6AssignmentApi.getProjectInspectors(projectId)
  } catch (error) {
    ElMessage.error('移除失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.v6-project-config { padding: 20px; }
.card-header { display: flex; align-items: center; gap: 16px; }
</style>
