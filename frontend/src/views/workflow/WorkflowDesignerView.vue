<template>
  <div class="workflow-designer">
    <div class="toolbar">
      <h2>BPMN 流程设计器</h2>
      <div class="toolbar-actions">
        <el-button @click="loadEmpty">新建</el-button>
        <el-upload :before-upload="loadFile" :show-file-list="false" accept=".bpmn,.xml">
          <el-button>导入文件</el-button>
        </el-upload>
        <el-button type="primary" @click="exportXml">导出 XML</el-button>
        <el-button type="success" @click="deployToServer">部署到服务器</el-button>
      </div>
    </div>
    <div ref="canvas" class="designer-canvas" />
    <el-dialog v-model="deployDialogVisible" title="部署流程" width="400px">
      <el-form>
        <el-form-item label="流程名称">
          <el-input v-model="deployName" placeholder="如:学生请假审批" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deployDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDeploy">确认部署</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
// @ts-ignore - bpmn-js 没有官方 d.ts
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import { ElMessage } from 'element-plus'
import { workflowApi } from '@/api/workflow'

const canvas = ref<HTMLDivElement>()
let modeler: any = null

const deployDialogVisible = ref(false)
const deployName = ref('')

const EMPTY_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  id="Definitions_1" targetNamespace="http://flowable.org/bpmn">
  <bpmn:process id="Process_1" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="100" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

onMounted(() => {
  modeler = new BpmnModeler({ container: canvas.value!, keyboard: { bindTo: window } })
  modeler.importXML(EMPTY_BPMN)
})

onBeforeUnmount(() => {
  modeler?.destroy()
})

function loadEmpty() {
  modeler?.importXML(EMPTY_BPMN)
}

function loadFile(file: File): boolean {
  const reader = new FileReader()
  reader.onload = (e) => {
    const xml = e.target?.result as string
    modeler?.importXML(xml).catch((err: Error) => ElMessage.error(`加载失败: ${err.message}`))
  }
  reader.readAsText(file)
  return false  // 阻止 el-upload 默认上传
}

async function exportXml() {
  if (!modeler) return
  const { xml } = await modeler.saveXML({ format: true })
  const blob = new Blob([xml || ''], { type: 'application/xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'process.bpmn20.xml'
  a.click()
  URL.revokeObjectURL(url)
}

function deployToServer() {
  deployDialogVisible.value = true
}

async function confirmDeploy() {
  if (!modeler) return
  if (!deployName.value.trim()) {
    ElMessage.warning('请输入流程名称')
    return
  }
  try {
    const { xml } = await modeler.saveXML({ format: true })
    const blob = new Blob([xml || ''], { type: 'application/xml' })
    const file = new File([blob], `${deployName.value}.bpmn20.xml`, { type: 'application/xml' })
    await workflowApi.deploy(file, deployName.value)
    ElMessage.success('部署成功')
    deployDialogVisible.value = false
  } catch (err) {
    ElMessage.error(`部署失败: ${(err as Error).message}`)
  }
}
</script>

<style scoped>
.workflow-designer {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;
}
.toolbar h2 { margin: 0; font-size: 18px; }
.toolbar-actions { display: flex; gap: 8px; align-items: center; }
.designer-canvas { flex: 1; background: #fafafa; }
</style>
