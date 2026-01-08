<template>
  <div class="bpmn-designer">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <button class="btn" @click="handleUndo" title="撤销">
          <ArrowUturnLeftIcon class="icon" />
        </button>
        <button class="btn" @click="handleRedo" title="重做">
          <ArrowUturnRightIcon class="icon" />
        </button>
        <span class="divider"></span>
        <button class="btn" @click="handleZoomIn" title="放大">
          <MagnifyingGlassPlusIcon class="icon" />
        </button>
        <button class="btn" @click="handleZoomOut" title="缩小">
          <MagnifyingGlassMinusIcon class="icon" />
        </button>
        <button class="btn" @click="handleResetZoom" title="重置缩放">
          <ArrowsPointingOutIcon class="icon" />
        </button>
        <span class="divider"></span>
        <button class="btn" @click="handleAlignLeft" title="左对齐">
          <Bars3BottomLeftIcon class="icon" />
        </button>
        <button class="btn" @click="handleAlignCenter" title="居中">
          <Bars3Icon class="icon" />
        </button>
      </div>
      <div class="toolbar-right">
        <button class="btn btn-primary" @click="handleSave">
          <CheckIcon class="icon" />
          <span>保存</span>
        </button>
        <button class="btn" @click="handleDownloadXml" title="导出XML">
          <ArrowDownTrayIcon class="icon" />
        </button>
        <button class="btn" @click="handleDownloadSvg" title="导出SVG">
          <PhotoIcon class="icon" />
        </button>
      </div>
    </div>

    <!-- 设计区域 -->
    <div class="designer-container">
      <!-- 画布 -->
      <div ref="canvasRef" class="canvas"></div>

      <!-- 属性面板 -->
      <div ref="propertiesPanelRef" class="properties-panel"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import customTranslateModule from './translations'
import {
  ArrowUturnLeftIcon,
  ArrowUturnRightIcon,
  MagnifyingGlassPlusIcon,
  MagnifyingGlassMinusIcon,
  ArrowsPointingOutIcon,
  Bars3BottomLeftIcon,
  Bars3Icon,
  CheckIcon,
  ArrowDownTrayIcon,
  PhotoIcon
} from '@heroicons/vue/24/outline'

// 导入样式
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

const props = defineProps<{
  xml?: string
  processId?: string
  processName?: string
}>()

const emit = defineEmits<{
  save: [xml: string]
  change: [xml: string]
}>()

// 默认空流程XML
const getDefaultXml = () => {
  const id = props.processId || 'Process_' + Date.now()
  const name = props.processName || '新流程'
  return `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:flowable="http://flowable.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="${id}" name="${name}" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:userTask id="Task_1" name="审批节点" flowable:assignee="\${assignee}">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:endEvent id="EndEvent_1" name="结束">
      <bpmn:incoming>Flow_2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1" />
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="EndEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${id}">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="187" y="203" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Task_1_di" bpmnElement="Task_1">
        <dc:Bounds x="280" y="138" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="450" y="160" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="457" y="203" width="22" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="216" y="178" />
        <di:waypoint x="280" y="178" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="380" y="178" />
        <di:waypoint x="450" y="178" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`
}

const canvasRef = ref<HTMLDivElement | null>(null)
const propertiesPanelRef = ref<HTMLDivElement | null>(null)
let bpmnModeler: BpmnModeler | null = null
let currentZoom = 1

// 初始化建模器
const initModeler = async () => {
  if (!canvasRef.value) return

  bpmnModeler = new BpmnModeler({
    container: canvasRef.value,
    keyboard: {
      bindTo: document
    },
    // 添加中文翻译模块
    additionalModules: [
      customTranslateModule
    ],
    // 添加 Flowable 命名空间支持
    moddleExtensions: {
      flowable: {
        name: 'Flowable',
        uri: 'http://flowable.org/bpmn',
        prefix: 'flowable',
        xml: {
          tagAlias: 'lowerCase'
        },
        types: [
          {
            name: 'Assignable',
            extends: ['bpmn:UserTask'],
            properties: [
              { name: 'assignee', isAttr: true, type: 'String' },
              { name: 'candidateUsers', isAttr: true, type: 'String' },
              { name: 'candidateGroups', isAttr: true, type: 'String' }
            ]
          }
        ]
      }
    }
  })

  // 监听变化
  bpmnModeler.on('commandStack.changed', async () => {
    const { xml } = await bpmnModeler!.saveXML({ format: true })
    if (xml) {
      emit('change', xml)
    }
  })

  // 导入流程
  try {
    const xmlToImport = props.xml || getDefaultXml()
    await bpmnModeler.importXML(xmlToImport)

    // 居中显示
    const canvas = bpmnModeler.get('canvas') as any
    canvas.zoom('fit-viewport')
  } catch (error) {
    console.error('导入流程失败:', error)
  }
}

// 撤销
const handleUndo = () => {
  const commandStack = bpmnModeler?.get('commandStack') as any
  if (commandStack?.canUndo()) {
    commandStack.undo()
  }
}

// 重做
const handleRedo = () => {
  const commandStack = bpmnModeler?.get('commandStack') as any
  if (commandStack?.canRedo()) {
    commandStack.redo()
  }
}

// 放大
const handleZoomIn = () => {
  currentZoom = Math.min(currentZoom + 0.1, 4)
  const canvas = bpmnModeler?.get('canvas') as any
  canvas?.zoom(currentZoom)
}

// 缩小
const handleZoomOut = () => {
  currentZoom = Math.max(currentZoom - 0.1, 0.2)
  const canvas = bpmnModeler?.get('canvas') as any
  canvas?.zoom(currentZoom)
}

// 重置缩放
const handleResetZoom = () => {
  currentZoom = 1
  const canvas = bpmnModeler?.get('canvas') as any
  canvas?.zoom('fit-viewport')
}

// 左对齐
const handleAlignLeft = () => {
  const alignElements = bpmnModeler?.get('alignElements') as any
  const selection = bpmnModeler?.get('selection') as any
  const selected = selection?.get()
  if (selected?.length > 1) {
    alignElements?.trigger(selected, 'left')
  }
}

// 居中对齐
const handleAlignCenter = () => {
  const alignElements = bpmnModeler?.get('alignElements') as any
  const selection = bpmnModeler?.get('selection') as any
  const selected = selection?.get()
  if (selected?.length > 1) {
    alignElements?.trigger(selected, 'center')
  }
}

// 保存
const handleSave = async () => {
  if (!bpmnModeler) return
  try {
    const { xml } = await bpmnModeler.saveXML({ format: true })
    if (xml) {
      emit('save', xml)
    }
  } catch (error) {
    console.error('保存失败:', error)
  }
}

// 导出XML
const handleDownloadXml = async () => {
  if (!bpmnModeler) return
  try {
    const { xml } = await bpmnModeler.saveXML({ format: true })
    if (xml) {
      downloadFile(xml, 'process.bpmn', 'application/xml')
    }
  } catch (error) {
    console.error('导出XML失败:', error)
  }
}

// 导出SVG
const handleDownloadSvg = async () => {
  if (!bpmnModeler) return
  try {
    const { svg } = await bpmnModeler.saveSVG()
    if (svg) {
      downloadFile(svg, 'process.svg', 'image/svg+xml')
    }
  } catch (error) {
    console.error('导出SVG失败:', error)
  }
}

// 下载文件
const downloadFile = (data: string, filename: string, type: string) => {
  const blob = new Blob([data], { type })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

// 获取当前XML
const getXml = async (): Promise<string | null> => {
  if (!bpmnModeler) return null
  try {
    const { xml } = await bpmnModeler.saveXML({ format: true })
    return xml || null
  } catch (error) {
    console.error('获取XML失败:', error)
    return null
  }
}

// 导入XML
const importXml = async (xml: string) => {
  if (!bpmnModeler) return
  try {
    await bpmnModeler.importXML(xml)
    const canvas = bpmnModeler.get('canvas') as any
    canvas.zoom('fit-viewport')
  } catch (error) {
    console.error('导入XML失败:', error)
  }
}

// 监听props.xml变化
watch(() => props.xml, (newXml) => {
  if (newXml && bpmnModeler) {
    importXml(newXml)
  }
})

// 暴露方法
defineExpose({
  getXml,
  importXml
})

onMounted(() => {
  initModeler()
})

onBeforeUnmount(() => {
  bpmnModeler?.destroy()
})
</script>

<style scoped>
.bpmn-designer {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.btn:active {
  background: #e5e7eb;
}

.btn-primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.btn-primary:hover {
  background: #1d4ed8;
  border-color: #1d4ed8;
}

.icon {
  width: 16px;
  height: 16px;
}

.divider {
  display: inline-block;
  width: 1px;
  height: 20px;
  background: #d1d5db;
  margin: 0 8px;
}

.designer-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.canvas {
  flex: 1;
  height: 100%;
  overflow: hidden;
}

.properties-panel {
  width: 300px;
  border-left: 1px solid #e5e7eb;
  background: #f9fafb;
  overflow-y: auto;
}

/* bpmn-js 样式覆盖 */
:deep(.djs-palette) {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.djs-palette-entries) {
  padding: 8px;
}

:deep(.entry) {
  margin: 4px;
}

:deep(.bjs-powered-by) {
  display: none;
}
</style>
