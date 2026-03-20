import { ref, computed } from 'vue'
import type { FloorPlanElement, FloorPlanElementType, FloorPlanLayout, FloorStyleType } from '@/types/universalPlace'

export type ActiveTool = 'select' | 'hand' | FloorPlanElementType

const GRID_SIZE = 10
const MAX_HISTORY = 50

function genId(): string {
  return crypto.randomUUID().slice(0, 8)
}

function snap(value: number, enabled = true): number {
  if (!enabled) return Math.round(value)
  return Math.round(value / GRID_SIZE) * GRID_SIZE
}

function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj))
}

const DEFAULT_SIZE: Record<FloorPlanElementType, { w: number; h: number }> = {
  seat: { w: 30, h: 30 },
  desk: { w: 120, h: 44 },
  'seat-desk': { w: 90, h: 54 },
  wall: { w: 200, h: 10 },
  door: { w: 60, h: 10 },
  partition: { w: 150, h: 6 },
  podium: { w: 180, h: 56 },
  blackboard: { w: 200, h: 36 },
  window: { w: 80, h: 10 },
  pillar: { w: 30, h: 30 },
  'round-table': { w: 80, h: 80 },
  text: { w: 100, h: 30 },
  rectangle: { w: 120, h: 80 },
  'circle-shape': { w: 60, h: 60 },
  'line-shape': { w: 150, h: 2 },
  area: { w: 200, h: 150 },
}

export interface FloorPlanTemplate {
  id: string
  name: string
  icon: string
  seats: number
  description: string
  generate: () => FloorPlanElement[]
}

function makeEl(
  type: FloorPlanElementType, x: number, y: number,
  overrides: Partial<FloorPlanElement> = {}
): FloorPlanElement {
  const size = DEFAULT_SIZE[type]
  return { id: genId(), type, x, y, width: size.w, height: size.h, rotation: 0, zIndex: 0, locked: false, opacity: 1, ...overrides }
}

// ==================== Templates ====================

function generateClassroomTemplate(): FloorPlanElement[] {
  const els: FloorPlanElement[] = []
  els.push(makeEl('wall', 20, 20, { width: 860, height: 10 }))
  els.push(makeEl('wall', 20, 570, { width: 860, height: 10 }))
  els.push(makeEl('wall', 20, 20, { width: 10, height: 560 }))
  els.push(makeEl('wall', 870, 20, { width: 10, height: 560 }))
  els.push(makeEl('window', 870, 100, { width: 10, height: 80 }))
  els.push(makeEl('window', 870, 250, { width: 10, height: 80 }))
  els.push(makeEl('window', 870, 400, { width: 10, height: 80 }))
  els.push(makeEl('door', 20, 480, { width: 10, height: 60 }))
  els.push(makeEl('rectangle', 200, 30, { width: 500, height: 36, fillColor: '#2D4A3E', label: '展示区' }))
  els.push(makeEl('rectangle', 360, 75, { width: 180, height: 50, fillColor: '#D4B896', label: '平台' }))
  const rowLabels = 'ABCDEFGH'
  let seatNo = 1
  for (let row = 0; row < 8; row++) {
    for (let col = 0; col < 8; col++) {
      const gap = col < 4 ? 0 : 30
      els.push(makeEl('seat', 160 + col * 38 + gap, 145 + row * 50, {
        positionNo: String(seatNo).padStart(2, '0'), rowLabel: rowLabels[row],
      }))
      seatNo++
    }
  }
  return els
}

function generateConferenceTemplate(): FloorPlanElement[] {
  const els: FloorPlanElement[] = []
  els.push(makeEl('wall', 100, 50, { width: 700, height: 10 }))
  els.push(makeEl('wall', 100, 500, { width: 700, height: 10 }))
  els.push(makeEl('wall', 100, 50, { width: 10, height: 460 }))
  els.push(makeEl('wall', 790, 50, { width: 10, height: 460 }))
  els.push(makeEl('door', 400, 500, { width: 80, height: 10 }))
  els.push(makeEl('desk', 250, 160, { width: 400, height: 44, label: '' }))
  els.push(makeEl('desk', 250, 204, { width: 44, height: 200, label: '' }))
  els.push(makeEl('desk', 606, 204, { width: 44, height: 200, label: '' }))
  let seatNo = 1
  for (let i = 0; i < 5; i++)
    els.push(makeEl('seat', 280 + i * 70, 120, { positionNo: String(seatNo++).padStart(2, '0'), rowLabel: 'A' }))
  for (let i = 0; i < 4; i++)
    els.push(makeEl('seat', 200, 210 + i * 48, { positionNo: String(seatNo++).padStart(2, '0'), rowLabel: 'L' }))
  for (let i = 0; i < 4; i++)
    els.push(makeEl('seat', 670, 210 + i * 48, { positionNo: String(seatNo++).padStart(2, '0'), rowLabel: 'R' }))
  return els
}

function generateOpenOfficeTemplate(): FloorPlanElement[] {
  const els: FloorPlanElement[] = []
  els.push(makeEl('wall', 20, 20, { width: 860, height: 10 }))
  els.push(makeEl('wall', 20, 570, { width: 860, height: 10 }))
  els.push(makeEl('wall', 20, 20, { width: 10, height: 560 }))
  els.push(makeEl('wall', 870, 20, { width: 10, height: 560 }))
  els.push(makeEl('door', 20, 480, { width: 10, height: 60 }))
  els.push(makeEl('window', 870, 200, { width: 10, height: 120 }))
  // 4 rows of workstations
  let seatNo = 1
  for (let row = 0; row < 4; row++) {
    const y = 60 + row * 120
    els.push(makeEl('desk', 100, y + 20, { width: 280, height: 44, label: '' }))
    els.push(makeEl('desk', 500, y + 20, { width: 280, height: 44, label: '' }))
    for (let col = 0; col < 6; col++) {
      const x = col < 3 ? 120 + col * 80 : 520 + (col - 3) * 80
      els.push(makeEl('seat', x, y + 70, { positionNo: String(seatNo++).padStart(2, '0'), rowLabel: String.fromCharCode(65 + row) }))
    }
  }
  // Meeting area
  els.push(makeEl('area', 350, 500, { width: 200, height: 60, label: '会议区' }))
  els.push(makeEl('round-table', 410, 510, { width: 60, height: 60, radius: 30 }))
  return els
}

function generateEventTemplate(): FloorPlanElement[] {
  const els: FloorPlanElement[] = []
  els.push(makeEl('rectangle', 250, 30, { width: 400, height: 50, fillColor: '#1E40AF', label: '舞台' }))
  const rowSeats = [10, 12, 12, 14, 14, 16, 16]
  const rowLabels = 'ABCDEFG'
  let seatNo = 1
  for (let row = 0; row < 7; row++) {
    const count = rowSeats[row]
    const totalWidth = count * 38
    const startX = (900 - totalWidth) / 2
    for (let col = 0; col < count; col++) {
      els.push(makeEl('seat', startX + col * 38, 110 + row * 52, {
        positionNo: String(seatNo++).padStart(2, '0'), rowLabel: rowLabels[row],
      }))
    }
  }
  els.push(makeEl('area', 50, 500, { width: 350, height: 70, label: 'VIP 区域' }))
  els.push(makeEl('area', 500, 500, { width: 350, height: 70, label: '无障碍区' }))
  return els
}

export const TEMPLATES: FloorPlanTemplate[] = [
  { id: 'classroom', name: '教室/培训室', icon: '🏫', seats: 64, description: '8×8 座位 + 展示区', generate: generateClassroomTemplate },
  { id: 'conference', name: '会议室', icon: '🤝', seats: 13, description: 'U型桌 + 13座', generate: generateConferenceTemplate },
  { id: 'open-office', name: '开放办公', icon: '🏢', seats: 24, description: '4排工位 + 会议区', generate: generateOpenOfficeTemplate },
  { id: 'event', name: '活动场地', icon: '🎭', seats: 94, description: '舞台 + 阶梯座位 + VIP区', generate: generateEventTemplate },
]

// ==================== Main Composable ====================

export function useFloorPlan() {
  const elements = ref<FloorPlanElement[]>([])
  const stageWidth = ref(900)
  const stageHeight = ref(600)
  const backgroundImage = ref<string | null>(null)
  const selectedIds = ref<Set<string>>(new Set())
  const activeTool = ref<ActiveTool>('select')
  const snapEnabled = ref(true)
  const floorStyle = ref<FloorStyleType>('grid')
  const clipboard = ref<FloorPlanElement[]>([])

  // Smart guide lines (shown during drag)
  const guideLineX = ref<number | null>(null)
  const guideLineY = ref<number | null>(null)

  // Rubber band selection
  const rubberBand = ref<{ x: number; y: number; w: number; h: number } | null>(null)

  const sortedElements = computed(() =>
    [...elements.value].sort((a, b) => (a.zIndex || 0) - (b.zIndex || 0))
  )
  const selectedElements = computed(() =>
    elements.value.filter(e => selectedIds.value.has(e.id))
  )
  const selectedElement = computed(() => {
    if (selectedIds.value.size !== 1) return null
    const id = [...selectedIds.value][0]
    return elements.value.find(e => e.id === id) || null
  })

  // ===== History =====
  const history = ref<FloorPlanElement[][]>([])
  const historyIndex = ref(-1)
  function pushHistory() {
    history.value = history.value.slice(0, historyIndex.value + 1)
    history.value.push(deepClone(elements.value))
    if (history.value.length > MAX_HISTORY) history.value.shift()
    historyIndex.value = history.value.length - 1
  }
  const canUndo = computed(() => historyIndex.value > 0)
  const canRedo = computed(() => historyIndex.value < history.value.length - 1)
  function undo() { if (!canUndo.value) return; historyIndex.value--; elements.value = deepClone(history.value[historyIndex.value]); selectedIds.value = new Set() }
  function redo() { if (!canRedo.value) return; historyIndex.value++; elements.value = deepClone(history.value[historyIndex.value]); selectedIds.value = new Set() }

  // ===== Z-order =====
  function bringForward(id: string) {
    const el = elements.value.find(e => e.id === id); if (!el) return
    const maxZ = Math.max(...elements.value.map(e => e.zIndex || 0))
    el.zIndex = (el.zIndex || 0) >= maxZ ? maxZ : (el.zIndex || 0) + 1; pushHistory()
  }
  function sendBackward(id: string) {
    const el = elements.value.find(e => e.id === id); if (!el) return
    const minZ = Math.min(...elements.value.map(e => e.zIndex || 0))
    el.zIndex = (el.zIndex || 0) <= minZ ? minZ : (el.zIndex || 0) - 1; pushHistory()
  }
  function toggleLock(id: string) {
    const el = elements.value.find(e => e.id === id); if (!el) return
    el.locked = !el.locked; pushHistory()
  }

  // ===== Seat numbering =====
  function nextSeatNumber(): string {
    const existing = new Set(
      elements.value.filter(e => (e.type === 'seat' || e.type === 'seat-desk') && e.positionNo).map(e => e.positionNo!)
    )
    for (let i = 1; i <= 999; i++) { const no = String(i).padStart(2, '0'); if (!existing.has(no)) return no }
    return ''
  }

  // ===== Element CRUD =====
  function addElement(type: FloorPlanElementType, x: number, y: number): FloorPlanElement {
    const size = DEFAULT_SIZE[type]
    const maxZ = elements.value.length > 0 ? Math.max(...elements.value.map(e => e.zIndex || 0)) : 0
    const el: FloorPlanElement = {
      id: genId(), type,
      x: snap(x, snapEnabled.value), y: snap(y, snapEnabled.value),
      width: size.w, height: size.h, rotation: 0, zIndex: maxZ + 1, locked: false, opacity: 1,
    }
    if (type === 'seat' || type === 'seat-desk') el.positionNo = nextSeatNumber()
    if (type === 'desk') el.label = ''
    if (type === 'podium') el.label = '平台'
    if (type === 'blackboard') el.label = '展示区'
    if (type === 'area') el.label = '区域'
    if (type === 'rectangle') el.label = ''
    if (type === 'text') { el.text = '文字'; el.fontSize = 14 }
    if (type === 'pillar' || type === 'round-table') el.radius = size.w / 2
    elements.value.push(el)
    pushHistory()
    return el
  }
  function updateElement(id: string, updates: Partial<FloorPlanElement>) {
    const idx = elements.value.findIndex(e => e.id === id); if (idx === -1) return
    Object.assign(elements.value[idx], updates)
  }
  function commitUpdate() { pushHistory() }
  function deleteSelected() {
    if (selectedIds.value.size === 0) return
    elements.value = elements.value.filter(e => !selectedIds.value.has(e.id))
    selectedIds.value = new Set(); pushHistory()
  }
  function deleteElement(id: string) {
    elements.value = elements.value.filter(e => e.id !== id)
    selectedIds.value.delete(id); pushHistory()
  }

  // ===== Selection =====
  function selectElement(id: string, multi = false) {
    if (multi) {
      const next = new Set(selectedIds.value)
      if (next.has(id)) next.delete(id); else next.add(id)
      selectedIds.value = next
    } else { selectedIds.value = new Set([id]) }
  }
  function clearSelection() { selectedIds.value = new Set() }
  function selectAll() { selectedIds.value = new Set(elements.value.map(e => e.id)) }

  // ===== Batch =====
  function addSeatRow(count: number, startX: number, startY: number, opts: { rowLabel?: string; spacing?: number } = {}) {
    const spacing = opts.spacing ?? 38
    const maxZ = elements.value.length > 0 ? Math.max(...elements.value.map(e => e.zIndex || 0)) : 0
    const newSeats: FloorPlanElement[] = []
    for (let i = 0; i < count; i++) {
      const el: FloorPlanElement = {
        id: genId(), type: 'seat',
        x: snap(startX + i * spacing, snapEnabled.value), y: snap(startY, snapEnabled.value),
        width: 30, height: 30, rotation: 0, zIndex: maxZ + 1, locked: false, opacity: 1,
        positionNo: nextSeatNumber(), rowLabel: opts.rowLabel,
      }
      elements.value.push(el); newSeats.push(el)
    }
    pushHistory(); return newSeats
  }
  interface SeatGridOptions {
    hSpacing?: number        // gap between seats horizontally (default 10)
    vSpacing?: number        // gap between seats vertically (default 10)
    seatWidth?: number       // seat width (default 30)
    seatHeight?: number      // seat height (default 30)
    startLabel?: string
    startNumber?: number
    numberingOrder?: 'ltr' | 'rtl' | 'snake' | 'column' | 'row-ltr' | 'row-rtl' | 'row-center'
    seatShape?: 'circle' | 'square' | 'rounded'
    aisleAfterCol?: number
    aisleWidth?: number
    staggered?: boolean
  }

  function centerOutOrder(cols: number): number[] {
    const order: number[] = []
    const center = Math.floor((cols - 1) / 2)
    order.push(center)
    for (let d = 1; order.length < cols; d++) {
      if (center + d < cols) order.push(center + d)
      if (center - d >= 0) order.push(center - d)
    }
    return order
  }

  function buildSeatNumbering(rows: number, cols: number, order: string, startNum: number): number[] {
    const seatNumbers: number[] = new Array(rows * cols)
    const isPerRow = order.startsWith('row-')
    if (isPerRow) {
      for (let r = 0; r < rows; r++) {
        let colOrder: number[]
        if (order === 'row-center') colOrder = centerOutOrder(cols)
        else {
          colOrder = Array.from({ length: cols }, (_, i) => i)
          if (order === 'row-rtl') colOrder.reverse()
        }
        for (let seq = 0; seq < cols; seq++)
          seatNumbers[r * cols + colOrder[seq]] = startNum + seq
      }
    } else if (order === 'column') {
      let seq = startNum
      for (let c = 0; c < cols; c++)
        for (let r = 0; r < rows; r++)
          seatNumbers[r * cols + c] = seq++
    } else {
      let seq = startNum
      for (let r = 0; r < rows; r++) {
        const indices: number[] = []
        for (let c = 0; c < cols; c++) indices.push(r * cols + c)
        let isReversed = false
        if (order === 'rtl') isReversed = true
        else if (order === 'snake') isReversed = r % 2 === 1
        if (isReversed) indices.reverse()
        for (const idx of indices) seatNumbers[idx] = seq++
      }
    }
    return seatNumbers
  }

  function generateSeatGridPreview(rows: number, cols: number, opts: SeatGridOptions = {}): { x: number; y: number; w: number; h: number; no: string }[] {
    const sw = Math.max(opts.seatWidth ?? 30, 16)
    const sh = Math.max(opts.seatHeight ?? 30, 16)
    const hGap = Math.max(opts.hSpacing ?? 10, 0)
    const vGap = Math.max(opts.vSpacing ?? 10, 0)
    const hStep = sw + hGap
    const vStep = sh + vGap
    const staggered = opts.staggered ?? false
    const aisleAfterCol = opts.aisleAfterCol ?? 0
    const aisleWidth = opts.aisleWidth ?? 30
    const order = opts.numberingOrder ?? 'ltr'
    const startNum = opts.startNumber ?? 1
    const seatNumbers = buildSeatNumbering(rows, cols, order, startNum)
    const result: { x: number; y: number; w: number; h: number; no: string }[] = []
    for (let r = 0; r < rows; r++) {
      const staggerOffset = (staggered && r % 2 === 1) ? hStep / 2 : 0
      for (let c = 0; c < cols; c++) {
        let aisleOffset = 0
        if (aisleAfterCol > 0) aisleOffset = Math.floor(c / aisleAfterCol) * aisleWidth
        const num = seatNumbers[r * cols + c]
        result.push({
          x: c * hStep + aisleOffset + staggerOffset,
          y: r * vStep,
          w: sw, h: sh,
          no: String(num).padStart(2, '0'),
        })
      }
    }
    return result
  }

  function addSeatGrid(rows: number, cols: number, startX: number, startY: number, opts: SeatGridOptions = {}) {
    const sw = Math.max(opts.seatWidth ?? 30, 16)
    const sh = Math.max(opts.seatHeight ?? 30, 16)
    const hGap = Math.max(opts.hSpacing ?? 10, 0)
    const vGap = Math.max(opts.vSpacing ?? 10, 0)
    const hStep = sw + hGap
    const vStep = sh + vGap
    const staggered = opts.staggered ?? false
    const startCharCode = (opts.startLabel || 'A').charCodeAt(0)
    const startNum = opts.startNumber ?? 1
    const order = opts.numberingOrder ?? 'ltr'
    const isPerRow = order.startsWith('row-')
    const shape = opts.seatShape
    const aisleAfterCol = opts.aisleAfterCol ?? 0
    const aisleWidth = opts.aisleWidth ?? 30
    const maxZ = elements.value.length > 0 ? Math.max(...elements.value.map(e => e.zIndex || 0)) : 0
    const seatNumbers = buildSeatNumbering(rows, cols, order, startNum)
    // Snap only the starting point, keep step exact so preview matches actual
    const sx = snap(startX, snapEnabled.value)
    const sy = snap(startY, snapEnabled.value)

    const allSeats: FloorPlanElement[] = []
    for (let r = 0; r < rows; r++) {
      const rowLabel = String.fromCharCode(startCharCode + r)
      const staggerOffset = (staggered && r % 2 === 1) ? hStep / 2 : 0
      for (let c = 0; c < cols; c++) {
        let aisleOffset = 0
        if (aisleAfterCol > 0) aisleOffset = Math.floor(c / aisleAfterCol) * aisleWidth
        const num = seatNumbers[r * cols + c]
        const posNo = isPerRow
          ? `${rowLabel}-${String(num).padStart(2, '0')}`
          : String(num).padStart(2, '0')
        const el: FloorPlanElement = {
          id: genId(), type: 'seat',
          x: Math.round(sx + c * hStep + aisleOffset + staggerOffset),
          y: Math.round(sy + r * vStep),
          width: sw, height: sh, rotation: 0, zIndex: maxZ + 1, locked: false, opacity: 1,
          positionNo: posNo, rowLabel,
        }
        if (shape && shape !== 'circle') el.seatShape = shape
        elements.value.push(el); allSeats.push(el)
      }
    }
    pushHistory(); return allSeats
  }

  // ===== Clipboard =====
  function copySelected() {
    const ids = [...selectedIds.value]; if (ids.length === 0) return
    clipboard.value = deepClone(elements.value.filter(e => ids.includes(e.id)))
  }
  function cutSelected() { copySelected(); deleteSelected() }
  function pasteClipboard() {
    if (clipboard.value.length === 0) return
    const newEls = clipboard.value.map(el => ({ ...deepClone(el), id: genId(), x: el.x + 20, y: el.y + 20 }))
    elements.value.push(...newEls)
    selectedIds.value = new Set(newEls.map(e => e.id)); pushHistory()
  }
  function duplicateSelected() {
    const ids = [...selectedIds.value]; if (ids.length === 0) return
    const originals = elements.value.filter(e => ids.includes(e.id))
    const newEls = originals.map(el => ({ ...deepClone(el), id: genId(), x: el.x + 20, y: el.y + 20 }))
    elements.value.push(...newEls)
    selectedIds.value = new Set(newEls.map(e => e.id)); pushHistory()
  }

  // ===== Alignment =====
  function getElBounds(el: FloorPlanElement) {
    return { left: el.x, top: el.y, right: el.x + (el.width || 0), bottom: el.y + (el.height || 0), cx: el.x + (el.width || 0) / 2, cy: el.y + (el.height || 0) / 2 }
  }
  function alignSelected(dir: 'left' | 'right' | 'top' | 'bottom' | 'centerH' | 'centerV') {
    const sel = selectedElements.value; if (sel.length < 2) return
    const bounds = sel.map(getElBounds)
    let target: number
    switch (dir) {
      case 'left': target = Math.min(...bounds.map(b => b.left)); break
      case 'right': target = Math.max(...bounds.map(b => b.right)); break
      case 'top': target = Math.min(...bounds.map(b => b.top)); break
      case 'bottom': target = Math.max(...bounds.map(b => b.bottom)); break
      case 'centerH': { const minX = Math.min(...bounds.map(b => b.left)); const maxX = Math.max(...bounds.map(b => b.right)); target = (minX + maxX) / 2; break }
      case 'centerV': { const minY = Math.min(...bounds.map(b => b.top)); const maxY = Math.max(...bounds.map(b => b.bottom)); target = (minY + maxY) / 2; break }
    }
    for (const el of sel) {
      switch (dir) {
        case 'left': el.x = target; break; case 'right': el.x = target - (el.width || 0); break
        case 'top': el.y = target; break; case 'bottom': el.y = target - (el.height || 0); break
        case 'centerH': el.x = target - (el.width || 0) / 2; break; case 'centerV': el.y = target - (el.height || 0) / 2; break
      }
    }
    pushHistory()
  }
  function distributeSelected(dir: 'horizontal' | 'vertical') {
    const sel = [...selectedElements.value]; if (sel.length < 3) return
    if (dir === 'horizontal') {
      sel.sort((a, b) => (a.x + (a.width || 0) / 2) - (b.x + (b.width || 0) / 2))
      const firstC = sel[0].x + (sel[0].width || 0) / 2
      const lastC = sel[sel.length - 1].x + (sel[sel.length - 1].width || 0) / 2
      if (lastC === firstC) return
      const step = (lastC - firstC) / (sel.length - 1)
      for (let i = 1; i < sel.length - 1; i++)
        sel[i].x = Math.round(firstC + step * i - (sel[i].width || 0) / 2)
    } else {
      sel.sort((a, b) => (a.y + (a.height || 0) / 2) - (b.y + (b.height || 0) / 2))
      const firstC = sel[0].y + (sel[0].height || 0) / 2
      const lastC = sel[sel.length - 1].y + (sel[sel.length - 1].height || 0) / 2
      if (lastC === firstC) return
      const step = (lastC - firstC) / (sel.length - 1)
      for (let i = 1; i < sel.length - 1; i++)
        sel[i].y = Math.round(firstC + step * i - (sel[i].height || 0) / 2)
    }
    elements.value = [...elements.value]
    pushHistory()
  }

  // ===== Smart Guides =====
  const GUIDE_SNAP = 5
  function computeGuides(draggingId: string, nx: number, ny: number, nw: number, nh: number): { snapX: number | null; snapY: number | null; gx: number | null; gy: number | null } {
    const cx = nx + nw / 2, cy = ny + nh / 2
    let snapX: number | null = null, snapY: number | null = null
    let gx: number | null = null, gy: number | null = null
    // Canvas center
    const canvasCX = stageWidth.value / 2, canvasCY = stageHeight.value / 2
    if (Math.abs(cx - canvasCX) < GUIDE_SNAP) { gx = canvasCX; snapX = canvasCX - nw / 2 }
    if (Math.abs(cy - canvasCY) < GUIDE_SNAP) { gy = canvasCY; snapY = canvasCY - nh / 2 }
    // Other elements — skip those far away (>300px) for performance
    const PROXIMITY = 300
    for (const other of elements.value) {
      if (gx !== null && gy !== null) break // both guides found, stop early
      if (other.id === draggingId) continue
      const ow = other.width || 0, oh = other.height || 0
      if (Math.abs(nx - other.x) > PROXIMITY && Math.abs(ny - other.y) > PROXIMITY) continue
      const ocx = other.x + ow / 2, ocy = other.y + oh / 2
      // Center-center
      if (gx === null && Math.abs(cx - ocx) < GUIDE_SNAP) { gx = ocx; snapX = ocx - nw / 2 }
      if (gy === null && Math.abs(cy - ocy) < GUIDE_SNAP) { gy = ocy; snapY = ocy - nh / 2 }
      // Edge-edge
      if (gx === null && Math.abs(nx - other.x) < GUIDE_SNAP) { gx = other.x; snapX = other.x }
      if (gx === null && Math.abs(nx + nw - other.x - ow) < GUIDE_SNAP) { gx = other.x + ow; snapX = other.x + ow - nw }
      if (gy === null && Math.abs(ny - other.y) < GUIDE_SNAP) { gy = other.y; snapY = other.y }
      if (gy === null && Math.abs(ny + nh - other.y - oh) < GUIDE_SNAP) { gy = other.y + oh; snapY = other.y + oh - nh }
    }
    return { snapX, snapY, gx, gy }
  }
  function updateGuides(gx: number | null, gy: number | null) { guideLineX.value = gx; guideLineY.value = gy }
  function clearGuides() { guideLineX.value = null; guideLineY.value = null }

  // ===== Stats =====
  const seatCount = computed(() => elements.value.filter(e => e.type === 'seat' || e.type === 'seat-desk').length)
  const deskCount = computed(() => elements.value.filter(e => e.type === 'desk' || e.type === 'seat-desk').length)
  const elementCount = computed(() => elements.value.length)

  function applyTemplate(template: FloorPlanTemplate) {
    elements.value = template.generate(); selectedIds.value = new Set(); pushHistory()
  }
  function toLayout(): FloorPlanLayout {
    return { version: 2, stageWidth: stageWidth.value, stageHeight: stageHeight.value, elements: deepClone(elements.value), backgroundImage: backgroundImage.value, floorStyle: floorStyle.value }
  }
  function loadLayout(layout: FloorPlanLayout | null | undefined) {
    if (!layout) {
      elements.value = []; stageWidth.value = 900; stageHeight.value = 600; backgroundImage.value = null; floorStyle.value = 'grid'
    } else {
      const els = deepClone(layout.elements || [])
      if (!layout.version || layout.version === 1) { for (const el of els) { if (el.zIndex === undefined) el.zIndex = 0; if (el.locked === undefined) el.locked = false; if (el.opacity === undefined) el.opacity = 1 } }
      elements.value = els; stageWidth.value = layout.stageWidth || 900; stageHeight.value = layout.stageHeight || 600
      backgroundImage.value = layout.backgroundImage || null; floorStyle.value = layout.floorStyle || 'grid'
    }
    selectedIds.value = new Set(); history.value = [deepClone(elements.value)]; historyIndex.value = 0
  }

  return {
    elements, sortedElements, stageWidth, stageHeight, backgroundImage,
    selectedIds, activeTool, selectedElement, selectedElements, snapEnabled, floorStyle, clipboard,
    guideLineX, guideLineY, rubberBand,
    seatCount, deskCount, elementCount,
    canUndo, canRedo, undo, redo,
    bringForward, sendBackward, toggleLock,
    addElement, updateElement, commitUpdate, deleteSelected, deleteElement,
    selectElement, clearSelection, selectAll,
    addSeatRow, addSeatGrid, generateSeatGridPreview,
    copySelected, cutSelected, pasteClipboard, duplicateSelected,
    alignSelected, distributeSelected,
    computeGuides, updateGuides, clearGuides,
    applyTemplate, toLayout, loadLayout,
    snap: (v: number) => snap(v, snapEnabled.value),
  }
}
