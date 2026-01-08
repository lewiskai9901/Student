<template>
  <div class="p-6 bg-gray-50 min-h-full">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">流程模板</h1>
      <p class="mt-1 text-sm text-gray-500">管理任务审批流程模板</p>
    </div>

    <!-- 操作栏 -->
    <div class="mb-4 flex justify-between">
      <div class="flex gap-3">
        <input
          v-model="queryParams.keyword"
          type="text"
          placeholder="搜索模板名称"
          class="h-9 w-48 rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          @keyup.enter="handleSearch"
        />
        <button
          class="h-9 rounded-md bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          @click="handleSearch"
        >
          搜索
        </button>
      </div>
      <button
        class="h-9 rounded-md bg-green-600 px-4 text-sm font-medium text-white hover:bg-green-700"
        @click="openCreateDialog"
      >
        + 新建模板
      </button>
    </div>

    <!-- 模板列表 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">模板名称</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">模板编码</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">模板类型</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">状态</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">默认</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">版本</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500">更新时间</th>
            <th class="px-4 py-3 text-center text-xs font-medium text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200 bg-white">
          <tr v-for="tpl in templateList" :key="tpl.id" class="hover:bg-gray-50">
            <td class="px-4 py-3 text-sm text-gray-900">{{ tpl.templateName }}</td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ tpl.templateCode }}</td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ getTemplateTypeName(tpl.templateType) }}</td>
            <td class="px-4 py-3">
              <span
                :class="tpl.status === 1 ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'"
                class="rounded px-2 py-0.5 text-xs"
              >
                {{ tpl.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span v-if="tpl.isDefault === 1" class="text-yellow-500">
                <StarIcon class="h-4 w-4" />
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">v{{ tpl.version }}</td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ formatDate(tpl.updatedAt) }}</td>
            <td class="px-4 py-3 text-center">
              <div class="flex justify-center gap-2">
                <button
                  class="text-sm text-purple-600 hover:text-purple-800"
                  @click="openDesigner(tpl)"
                >
                  设计
                </button>
                <button
                  class="text-sm text-orange-600 hover:text-orange-800"
                  @click="openApproverConfig(tpl)"
                >
                  配置审批人
                </button>
                <button
                  class="text-sm text-blue-600 hover:text-blue-800"
                  @click="editTemplate(tpl)"
                >
                  编辑
                </button>
                <button
                  v-if="tpl.isDefault !== 1"
                  class="text-sm text-yellow-600 hover:text-yellow-800"
                  @click="setDefault(tpl)"
                >
                  设为默认
                </button>
                <button
                  v-if="!tpl.processDefinitionId"
                  class="text-sm text-green-600 hover:text-green-800"
                  @click="deployTemplate(tpl)"
                >
                  部署
                </button>
                <button
                  v-if="tpl.isDefault !== 1"
                  class="text-sm text-red-600 hover:text-red-800"
                  @click="deleteTemplateConfirm(tpl)"
                >
                  删除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="templateList.length === 0">
            <td colspan="8" class="px-4 py-8 text-center text-sm text-gray-500">暂无数据</td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <p class="text-sm text-gray-500">共 {{ total }} 条记录</p>
        <div class="flex gap-2">
          <button
            :disabled="queryParams.pageNum === 1"
            class="rounded border px-3 py-1 text-sm disabled:opacity-50"
            @click="queryParams.pageNum--; loadTemplateList()"
          >
            上一页
          </button>
          <button
            :disabled="queryParams.pageNum * queryParams.pageSize >= total"
            class="rounded border px-3 py-1 text-sm disabled:opacity-50"
            @click="queryParams.pageNum++; loadTemplateList()"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 创建/编辑对话框 -->
    <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/50" @click="dialogVisible = false"></div>
      <div class="relative w-full max-w-lg rounded-lg bg-white shadow-xl">
        <div class="flex items-center justify-between border-b px-6 py-4">
          <h3 class="text-lg font-semibold">{{ editingTemplate ? '编辑模板' : '新建模板' }}</h3>
          <button class="text-gray-400 hover:text-gray-600" @click="dialogVisible = false">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <div class="px-6 py-4">
          <div class="space-y-4">
            <div>
              <label class="mb-1 block text-sm font-medium text-gray-700">模板名称 *</label>
              <input
                v-model="form.templateName"
                type="text"
                placeholder="请输入模板名称"
                class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
              />
            </div>
            <div>
              <label class="mb-1 block text-sm font-medium text-gray-700">模板编码 *</label>
              <input
                v-model="form.templateCode"
                type="text"
                placeholder="请输入模板编码（如：TASK_DEFAULT）"
                :disabled="!!editingTemplate"
                class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none disabled:bg-gray-100"
              />
            </div>
            <div>
              <label class="mb-1 block text-sm font-medium text-gray-700">模板类型</label>
              <select
                v-model="form.templateType"
                class="w-full rounded-md border border-gray-300 px-3 py-2"
              >
                <option value="TASK">任务审批</option>
                <option value="LEAVE">请假审批</option>
                <option value="EXPENSE">报销审批</option>
              </select>
            </div>
            <div>
              <label class="mb-1 block text-sm font-medium text-gray-700">描述</label>
              <textarea
                v-model="form.description"
                rows="3"
                placeholder="请输入模板描述"
                class="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-blue-500 focus:outline-none"
              ></textarea>
            </div>
            <div class="flex items-center gap-4">
              <label class="flex items-center">
                <input
                  v-model="form.status"
                  type="checkbox"
                  :true-value="1"
                  :false-value="0"
                  class="mr-2 h-4 w-4 rounded border-gray-300"
                />
                <span class="text-sm">启用</span>
              </label>
              <label class="flex items-center">
                <input
                  v-model="form.isDefault"
                  type="checkbox"
                  :true-value="1"
                  :false-value="0"
                  class="mr-2 h-4 w-4 rounded border-gray-300"
                />
                <span class="text-sm">设为默认</span>
              </label>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 border-t px-6 py-4">
          <button
            class="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="dialogVisible = false"
          >
            取消
          </button>
          <button
            class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            :disabled="submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 审批人配置对话框 -->
    <div v-if="approverDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/50" @click="approverDialogVisible = false"></div>
      <div class="relative w-full max-w-3xl rounded-lg bg-white shadow-xl">
        <div class="flex items-center justify-between border-b px-6 py-4">
          <h3 class="text-lg font-semibold">配置审批人 - {{ configTemplate?.templateName }}</h3>
          <button class="text-gray-400 hover:text-gray-600" @click="approverDialogVisible = false">
            <XMarkIcon class="h-5 w-5" />
          </button>
        </div>

        <div class="max-h-[60vh] overflow-y-auto px-6 py-4">
          <div class="mb-4 rounded-lg border border-blue-200 bg-blue-50 p-3">
            <p class="text-sm text-blue-800">
              <InformationCircleIcon class="mr-1 inline h-4 w-4" />
              以下节点从流程设计器中自动读取。为每个节点配置审批人，创建任务时可修改标记为"允许修改"的节点。
            </p>
          </div>

          <!-- 空状态 -->
          <div v-if="approverNodes.length === 0" class="py-8 text-center">
            <p class="text-gray-500">未找到审批节点，请先在流程设计器中添加用户任务节点</p>
            <button
              class="mt-3 text-sm text-blue-600 hover:text-blue-800"
              @click="openDesigner(configTemplate!)"
            >
              前往流程设计 →
            </button>
          </div>

          <!-- 审批节点列表 -->
          <div v-else class="space-y-4">
            <div
              v-for="(node, index) in approverNodes"
              :key="node.nodeId"
              class="rounded-lg border border-gray-200 p-4"
            >
              <div class="mb-3 flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-xs text-white">
                    {{ index + 1 }}
                  </span>
                  <span class="px-2 py-1 text-sm font-medium text-gray-900">
                    {{ node.nodeName }}
                  </span>
                  <span class="text-xs text-gray-400">({{ node.nodeId }})</span>
                </div>
                <label class="flex items-center text-sm text-gray-600">
                  <input
                    v-model="node.allowModify"
                    type="checkbox"
                    class="mr-1.5 h-4 w-4 rounded border-gray-300"
                  />
                  允许修改
                </label>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="mb-1 block text-xs text-gray-500">审批人类型</label>
                  <select
                    v-model="node.approverType"
                    class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                  >
                    <option value="USER">指定用户</option>
                    <option value="ROLE">指定角色</option>
                    <option value="DEPARTMENT_LEADER">部门负责人</option>
                    <option value="TASK_CREATOR">任务创建人</option>
                  </select>
                </div>

                <!-- 指定用户时显示用户选择 -->
                <div v-if="node.approverType === 'USER'">
                  <label class="mb-1 block text-xs text-gray-500">选择审批人</label>
                  <select
                    v-model="node.approverUserId"
                    class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                  >
                    <option :value="undefined">请选择</option>
                    <option v-for="user in simpleUserList" :key="user.id" :value="user.id">
                      {{ user.realName }}{{ user.departmentName ? ` (${user.departmentName})` : '' }}
                    </option>
                  </select>
                </div>

                <!-- 指定角色时显示角色选择 -->
                <div v-if="node.approverType === 'ROLE'">
                  <label class="mb-1 block text-xs text-gray-500">选择角色</label>
                  <select
                    v-model="node.approverRoleCode"
                    class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                  >
                    <option :value="undefined">请选择</option>
                    <option v-for="role in roleList" :key="role.roleCode" :value="role.roleCode">
                      {{ role.roleName }}
                    </option>
                  </select>
                </div>

                <!-- 部门负责人层级 -->
                <div v-if="node.approverType === 'DEPARTMENT_LEADER'">
                  <label class="mb-1 block text-xs text-gray-500">部门层级</label>
                  <select
                    v-model="node.departmentLevel"
                    class="w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                  >
                    <option :value="1">直属部门负责人</option>
                    <option :value="2">上级部门负责人</option>
                    <option :value="3">顶级部门负责人</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 border-t px-6 py-4">
          <button
            class="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="approverDialogVisible = false"
          >
            取消
          </button>
          <button
            class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
            :disabled="savingApprover"
            @click="saveApproverConfig"
          >
            {{ savingApprover ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="deleteDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/50" @click="deleteDialogVisible = false"></div>
      <div class="relative w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
        <h4 class="mb-4 text-lg font-semibold">确认删除</h4>
        <p class="mb-6 text-gray-600">确定要删除模板"{{ deletingTemplate?.templateName }}"吗？此操作不可恢复。</p>
        <div class="flex justify-end gap-3">
          <button
            class="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="deleteDialogVisible = false"
          >
            取消
          </button>
          <button
            class="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
            :disabled="submitting"
            @click="handleDelete"
          >
            {{ submitting ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { XMarkIcon, StarIcon, InformationCircleIcon, TrashIcon, PlusIcon } from '@heroicons/vue/24/outline'
import {
  getWorkflowTemplateList,
  getWorkflowTemplateDetail,
  createWorkflowTemplate,
  updateWorkflowTemplate,
  deleteWorkflowTemplate,
  deployWorkflowTemplate,
  setDefaultTemplate,
  getUserTaskNodes,
  type WorkflowTemplateDTO,
  type WorkflowTemplateRequest,
  type UserTaskNode
} from '@/api/task/workflow'
import { getSimpleUserList, type SimpleUser } from '@/api/user'
import { getAllRoles, type Role } from '@/api/role'

// 审批节点接口
interface ApproverNode {
  nodeId: string
  nodeName: string
  nodeOrder: number
  approverType: 'USER' | 'ROLE' | 'DEPARTMENT_LEADER' | 'TASK_CREATOR'
  approverUserId?: number
  approverRoleCode?: string
  departmentLevel?: number
  allowModify: boolean
}

const router = useRouter()

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

// 数据
const templateList = ref<WorkflowTemplateDTO[]>([])
const total = ref(0)

// 对话框
const dialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const approverDialogVisible = ref(false)
const editingTemplate = ref<WorkflowTemplateDTO | null>(null)
const deletingTemplate = ref<WorkflowTemplateDTO | null>(null)
const configTemplate = ref<WorkflowTemplateDTO | null>(null)
const submitting = ref(false)
const savingApprover = ref(false)

// 审批人配置相关
const approverNodes = ref<ApproverNode[]>([])
const bpmnNodes = ref<UserTaskNode[]>([])  // BPMN中的原始节点
const simpleUserList = ref<SimpleUser[]>([])
const roleList = ref<Role[]>([])
const loadingNodes = ref(false)

// 加载用户和角色列表
const loadUserAndRoleList = async () => {
  try {
    const [users, roles] = await Promise.all([
      getSimpleUserList(),
      getAllRoles()
    ])
    simpleUserList.value = users
    roleList.value = roles || []
  } catch (error) {
    console.error('加载用户/角色列表失败', error)
  }
}

// 打开审批人配置对话框
const openApproverConfig = async (tpl: WorkflowTemplateDTO) => {
  configTemplate.value = tpl
  loadingNodes.value = true

  try {
    // 并行加载：用户/角色列表、BPMN节点、已保存的配置
    const [, nodes, detail] = await Promise.all([
      loadUserAndRoleList(),
      getUserTaskNodes(tpl.id),
      getWorkflowTemplateDetail(tpl.id)
    ])

    bpmnNodes.value = nodes || []

    // 如果BPMN中没有用户任务节点，提示用户
    if (bpmnNodes.value.length === 0) {
      alert('请先在流程设计器中添加审批节点后再配置审批人')
      return
    }

    // 加载已配置的审批人信息
    let savedConfig: Record<string, ApproverNode> = {}
    if (detail.nodeConfig) {
      const config = typeof detail.nodeConfig === 'string'
        ? JSON.parse(detail.nodeConfig)
        : detail.nodeConfig
      // 将保存的配置转为map方便查找
      if (config.nodes) {
        for (const node of config.nodes) {
          savedConfig[node.nodeId] = node
        }
      }
    }

    // 根据BPMN中的节点构建审批人配置列表
    approverNodes.value = bpmnNodes.value.map(bpmnNode => {
      const saved = savedConfig[bpmnNode.nodeId]
      return {
        nodeId: bpmnNode.nodeId,
        nodeName: bpmnNode.nodeName,
        nodeOrder: bpmnNode.nodeOrder,
        approverType: saved?.approverType || 'USER',
        approverUserId: saved?.approverUserId,
        approverRoleCode: saved?.approverRoleCode,
        departmentLevel: saved?.departmentLevel || 1,
        allowModify: saved?.allowModify ?? true
      }
    })

    approverDialogVisible.value = true
  } catch (error) {
    console.error('加载配置失败', error)
    alert('加载节点配置失败，请确保流程已设计')
  } finally {
    loadingNodes.value = false
  }
}

// 保存审批人配置
const saveApproverConfig = async () => {
  if (!configTemplate.value) return

  // 验证
  for (const node of approverNodes.value) {
    if (!node.nodeName) {
      alert('请填写节点名称')
      return
    }
    if (node.approverType === 'USER' && !node.approverUserId) {
      alert(`请为"${node.nodeName}"选择审批人`)
      return
    }
    if (node.approverType === 'ROLE' && !node.approverRoleCode) {
      alert(`请为"${node.nodeName}"选择角色`)
      return
    }
  }

  savingApprover.value = true
  try {
    const nodeConfig = { nodes: approverNodes.value }
    await updateWorkflowTemplate(configTemplate.value.id, {
      templateName: configTemplate.value.templateName,
      templateCode: configTemplate.value.templateCode,
      templateType: configTemplate.value.templateType,
      description: configTemplate.value.description,
      status: configTemplate.value.status,
      isDefault: configTemplate.value.isDefault,
      nodeConfig
    })
    approverDialogVisible.value = false
    alert('审批人配置保存成功')
  } catch (error) {
    console.error('保存审批人配置失败', error)
    alert('保存失败')
  } finally {
    savingApprover.value = false
  }
}

// 表单
const form = reactive<WorkflowTemplateRequest>({
  templateName: '',
  templateCode: '',
  templateType: 'TASK',
  description: '',
  status: 1,
  isDefault: 0
})

// 加载模板列表
const loadTemplateList = async () => {
  try {
    const res = await getWorkflowTemplateList(queryParams)
    templateList.value = res.records
    total.value = res.total
  } catch (error) {
    console.error('加载模板列表失败', error)
  }
}

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  loadTemplateList()
}

// 打开创建对话框
const openCreateDialog = () => {
  editingTemplate.value = null
  form.templateName = ''
  form.templateCode = ''
  form.templateType = 'TASK'
  form.description = ''
  form.status = 1
  form.isDefault = 0
  dialogVisible.value = true
}

// 打开设计器
const openDesigner = (tpl: WorkflowTemplateDTO) => {
  router.push(`/task/workflow/${tpl.id}/design`)
}

// 编辑模板
const editTemplate = (tpl: WorkflowTemplateDTO) => {
  editingTemplate.value = tpl
  form.templateName = tpl.templateName
  form.templateCode = tpl.templateCode
  form.templateType = tpl.templateType
  form.description = tpl.description || ''
  form.status = tpl.status
  form.isDefault = tpl.isDefault
  dialogVisible.value = true
}

// 保存模板
const handleSubmit = async () => {
  if (!form.templateName || !form.templateCode) {
    alert('请填写模板名称和编码')
    return
  }

  submitting.value = true
  try {
    if (editingTemplate.value) {
      await updateWorkflowTemplate(editingTemplate.value.id, form)
    } else {
      await createWorkflowTemplate(form)
    }
    dialogVisible.value = false
    loadTemplateList()
  } catch (error) {
    console.error('保存模板失败', error)
    alert('保存失败')
  } finally {
    submitting.value = false
  }
}

// 设为默认
const setDefault = async (tpl: WorkflowTemplateDTO) => {
  try {
    await setDefaultTemplate(tpl.id, tpl.templateType)
    loadTemplateList()
  } catch (error) {
    console.error('设置默认模板失败', error)
    alert('操作失败')
  }
}

// 部署模板
const deployTemplate = async (tpl: WorkflowTemplateDTO) => {
  try {
    await deployWorkflowTemplate(tpl.id)
    loadTemplateList()
    alert('部署成功')
  } catch (error) {
    console.error('部署模板失败', error)
    alert('部署失败')
  }
}

// 确认删除
const deleteTemplateConfirm = (tpl: WorkflowTemplateDTO) => {
  deletingTemplate.value = tpl
  deleteDialogVisible.value = true
}

// 删除模板
const handleDelete = async () => {
  if (!deletingTemplate.value) return

  submitting.value = true
  try {
    await deleteWorkflowTemplate(deletingTemplate.value.id)
    deleteDialogVisible.value = false
    loadTemplateList()
  } catch (error) {
    console.error('删除模板失败', error)
    alert('删除失败')
  } finally {
    submitting.value = false
  }
}

// 获取模板类型名称
const getTemplateTypeName = (type: string) => {
  const names: Record<string, string> = {
    TASK: '任务审批',
    LEAVE: '请假审批',
    EXPENSE: '报销审批'
  }
  return names[type] || type
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

onMounted(() => {
  loadTemplateList()
})
</script>
