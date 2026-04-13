<template>
  <div class="fp-editor" :class="{ 'fp-view-mode': mode === 'view' }">
    <!-- ===== Top Toolbar ===== -->
    <div v-if="mode === 'edit'" class="fp-topbar">
      <button class="fp-btn" :class="{ active: activeTool === 'select' }" title="选择 (V)" @click="activeTool = 'select'">
        <MousePointer2 :size="16" />
      </button>
      <button class="fp-btn" :class="{ active: activeTool === 'hand' }" title="手型 (H)" @click="activeTool = 'hand'">
        <Hand :size="16" />
      </button>
      <span class="fp-sep" />
      <!-- Furniture dropdown -->
      <div class="fp-tool-group">
        <button class="fp-btn fp-btn-dropdown" :class="{ active: isFurnitureActive }" @click="toggleDropdown('furniture')">
          <component :is="activeFurnitureTool.icon" :size="16" />
          <ChevronDown :size="10" class="fp-chevron" />
        </button>
        <div v-if="openDropdown === 'furniture'" class="fp-dropdown">
          <button v-for="t in furnitureTools" :key="t.key" class="fp-dropdown-item" :class="{ active: activeTool === t.key }" @click="activeTool = t.key; openDropdown = null">
            <component :is="t.icon" :size="16" />
            <span>{{ t.label }}</span>
          </button>
        </div>
      </div>
      <!-- Structure dropdown -->
      <div class="fp-tool-group">
        <button class="fp-btn fp-btn-dropdown" :class="{ active: isStructActive }" @click="toggleDropdown('struct')">
          <component :is="activeStructTool.icon" :size="16" />
          <ChevronDown :size="10" class="fp-chevron" />
        </button>
        <div v-if="openDropdown === 'struct'" class="fp-dropdown">
          <button v-for="t in structTools" :key="t.key" class="fp-dropdown-item" :class="{ active: activeTool === t.key }" @click="activeTool = t.key; openDropdown = null">
            <component :is="t.icon" :size="16" />
            <span>{{ t.label }}</span>
          </button>
        </div>
      </div>
      <!-- Facility dropdown -->
      <div class="fp-tool-group">
        <button class="fp-btn fp-btn-dropdown" :class="{ active: isFacilityActive }" @click="toggleDropdown('facility')">
          <component :is="activeFacilityTool.icon" :size="16" />
          <ChevronDown :size="10" class="fp-chevron" />
        </button>
        <div v-if="openDropdown === 'facility'" class="fp-dropdown">
          <button v-for="t in facilityTools" :key="t.key" class="fp-dropdown-item" :class="{ active: activeTool === t.key }" @click="activeTool = t.key; openDropdown = null">
            <component :is="t.icon" :size="16" />
            <span>{{ t.label }}</span>
          </button>
        </div>
      </div>
      <span class="fp-sep" />
      <!-- Generic shapes -->
      <button v-for="t in shapeTools" :key="t.key" class="fp-btn" :class="{ active: activeTool === t.key }" :title="t.label" @click="activeTool = t.key">
        <component :is="t.icon" :size="16" />
      </button>
      <button class="fp-btn" :class="{ active: activeTool === 'text' }" title="文字" @click="activeTool = 'text'">
        <TypeIcon :size="16" />
      </button>
      <span class="fp-sep" />
      <button class="fp-btn" title="批量添加座位" @click="showBatchDialog = true"><Plus :size="16" /></button>
      <button class="fp-btn" title="模板" @click="showTemplateDialog = true"><LayoutTemplate :size="16" /></button>
      <label class="fp-btn" title="上传底图">
        <ImageIcon :size="16" />
        <input type="file" accept="image/*" class="sr-only" @change="handleBgUpload" />
      </label>
      <button v-if="backgroundImage" class="fp-btn fp-btn-danger" title="移除底图" @click="backgroundImage = null"><X :size="14" /></button>
      <div class="fp-spacer" />
      <!-- Alignment (multi-select) -->
      <template v-if="selectedIds.size >= 2">
        <button class="fp-btn" title="左对齐" @click="alignSelected('left')"><AlignLeft :size="14" /></button>
        <button class="fp-btn" title="水平居中" @click="alignSelected('centerH')"><AlignCenter :size="14" /></button>
        <button class="fp-btn" title="右对齐" @click="alignSelected('right')"><AlignRight :size="14" /></button>
        <button class="fp-btn" title="顶对齐" @click="alignSelected('top')"><ArrowUpToLine :size="14" /></button>
        <button class="fp-btn" title="垂直居中" @click="alignSelected('centerV')"><AlignVerticalSpaceAround :size="14" /></button>
        <button class="fp-btn" title="底对齐" @click="alignSelected('bottom')"><ArrowDownToLine :size="14" /></button>
        <template v-if="selectedIds.size >= 3">
          <button class="fp-btn" title="水平等距" @click="distributeSelected('horizontal')"><MoveHorizontal :size="14" /></button>
          <button class="fp-btn" title="垂直等距" @click="distributeSelected('vertical')"><MoveVertical :size="14" /></button>
        </template>
        <span class="fp-sep" />
      </template>
      <button class="fp-btn" :disabled="!canUndo" title="撤销 Ctrl+Z" @click="undo"><Undo2 :size="16" /></button>
      <button class="fp-btn" :disabled="!canRedo" title="重做 Ctrl+Y" @click="redo"><Redo2 :size="16" /></button>
      <span class="fp-sep" />
      <button class="fp-btn" title="缩小" @click="zoomOut"><ZoomOut :size="16" /></button>
      <span class="fp-zoom-label">{{ Math.round(scale * 100) }}%</span>
      <button class="fp-btn" title="放大" @click="zoomIn"><ZoomIn :size="16" /></button>
      <button class="fp-btn" title="重置视图" @click="resetZoom"><Maximize2 :size="14" /></button>
      <span class="fp-sep" />
      <button class="fp-btn" :class="{ active: snapEnabled }" title="对齐网格" @click="snapEnabled = !snapEnabled"><Grid3x3 :size="16" /></button>
    </div>

    <!-- ===== Content ===== -->
    <div class="fp-content">
      <div class="fp-canvas-wrap">
        <div ref="canvasContainerRef" class="fp-canvas" @wheel.prevent="handleWheel">
          <v-stage ref="stageRef" :config="stageConfig" @mousedown="handleStageMouseDown" @mousemove="handleStageMouseMove" @mouseup="handleStageMouseUp" @touchstart="handleStageMouseDown">
            <!-- Grid layer: single sceneFunc for performance -->
            <v-layer :config="{ listening: false }">
              <v-rect :config="{ x: 0, y: 0, width: stageWidth, height: stageHeight, fill: '#FAFBFC' }" />
              <v-shape v-if="floorStyle !== 'plain'" :config="gridShapeConfig" />
            </v-layer>
            <!-- BG image -->
            <v-layer v-if="bgImageObj" :config="{ listening: false }">
              <v-image :config="{ image: bgImageObj, opacity: 0.25, width: stageWidth, height: stageHeight }" />
            </v-layer>
            <!-- Elements -->
            <v-layer ref="mainLayerRef">
              <!-- Column labels (top) -->
              <v-text v-for="cl in colLabelPositions" :key="'cl-'+cl.label" :config="{ x: cl.x - cl.fontSize / 2, y: cl.y, text: cl.label, fontSize: cl.fontSize, fontStyle: 'bold', fill: '#B0B8C4', listening: false }" />
              <!-- Row labels (left) -->
              <v-text v-for="rl in rowLabelPositions" :key="'rl-'+rl.label" :config="{ x: rl.x, y: rl.y, text: rl.label, fontSize: 12, fontStyle: 'bold', fill: '#B0B8C4', listening: false }" />

              <template v-for="el in sortedElements" :key="el.id">
                <!-- SEAT (circle / square / rounded) -->
                <v-group v-if="el.type === 'seat'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-circle v-if="!el.seatShape || el.seatShape === 'circle'" :config="seatCircleCfg(el)" />
                  <v-rect v-else :config="seatRectCfg(el)" />
                  <v-text :config="seatTextCfg(el)" />
                </v-group>
                <!-- DESK -->
                <v-group v-if="el.type === 'desk'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-rect :config="{ width: el.width || 120, height: el.height || 44, fill: el.fillColor || '#FAECD8', stroke: el.strokeColor || '#D4B896', strokeWidth: 1, cornerRadius: 3, opacity: el.opacity ?? 1, ...shadow }" />
                  <v-text :config="{ text: el.label || '', width: el.width || 120, height: el.height || 44, align: 'center', verticalAlign: 'middle', fontSize: el.fontSize || 11, fontFamily: el.fontFamily || undefined, fill: el.textColor || '#8B6914', listening: false }" />
                </v-group>
                <!-- SEAT-DESK -->
                <v-group v-if="el.type === 'seat-desk'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-rect :config="{ width: el.width || 90, height: 28, fill: '#FAECD8', stroke: el.strokeColor || '#D4B896', strokeWidth: 1, cornerRadius: [3,3,0,0], opacity: el.opacity ?? 1, ...shadow }" />
                  <v-circle :config="{ x: (el.width || 90) / 2, y: 40, radius: 12, fill: seatFill(el, isOccupied(el)), stroke: seatStroke(el, isOccupied(el)), strokeWidth: seatStrokeWidth(el, isOccupied(el)), opacity: el.opacity ?? 1, listening: false, perfectDrawEnabled: false }" />
                  <v-text :config="{ text: seatDisplayText(el), x: (el.width || 90) / 2 - 12, y: 28, width: 24, height: 24, align: 'center', verticalAlign: 'middle', fontSize: el.fontSize || 9, fontFamily: el.fontFamily || undefined, fill: seatTextFill(el, isOccupied(el)), listening: false }" />
                </v-group>
                <!-- WALL -->
                <v-rect v-if="el.type === 'wall'" :config="{ ...baseConfig(el), fill: el.fillColor || '#4B5563', cornerRadius: 1, ...shadow }" v-bind="evtBinds(el)" />
                <!-- PARTITION -->
                <v-rect v-if="el.type === 'partition'" :config="{ ...baseConfig(el), fill: el.fillColor || '#CBD5E1', stroke: el.strokeColor || '#94A3B8', strokeWidth: 1, dash: [6,4], cornerRadius: 1 }" v-bind="evtBinds(el)" />
                <!-- PILLAR -->
                <v-circle v-if="el.type === 'pillar'" :config="{ x: el.x + (el.radius || 15), y: el.y + (el.radius || 15), radius: el.radius || 15, fill: el.fillColor || '#9CA3AF', opacity: el.opacity ?? 1, draggable: isDraggable && !el.locked, id: el.id, name: 'element', ...shadow }" v-bind="evtBinds(el)" />
                <!-- DOOR -->
                <v-group v-if="el.type === 'door'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-rect :config="{ width: el.width || 60, height: el.height || 10, fill: el.fillColor || '#C4956A', cornerRadius: 2, opacity: el.opacity ?? 1, ...shadow }" />
                  <v-circle :config="{ x: (el.width || 60) - 7, y: (el.height || 10) / 2, radius: 2, fill: '#DAA520' }" />
                </v-group>
                <!-- WINDOW -->
                <v-group v-if="el.type === 'window'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-rect :config="{ width: el.width || 80, height: el.height || 10, fill: el.fillColor || '#E8F4FD', stroke: el.strokeColor || '#B4D5E8', strokeWidth: 1, cornerRadius: 1, opacity: el.opacity ?? 1 }" />
                  <v-line :config="{ points: [(el.width || 80)/2, 0, (el.width || 80)/2, el.height || 10], stroke: '#FFFFFF', strokeWidth: 2 }" />
                </v-group>
                <!-- PODIUM -->
                <v-group v-if="el.type === 'podium'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-rect :config="{ width: el.width || 180, height: el.height || 56, fill: el.fillColor || '#F5EBD9', stroke: el.strokeColor || '#C4A87C', strokeWidth: 1.5, cornerRadius: 4, opacity: el.opacity ?? 1, ...shadow }" />
                  <v-text :config="{ text: el.label || '平台', width: el.width || 180, height: el.height || 56, align: 'center', verticalAlign: 'middle', fontSize: el.fontSize || 13, fontFamily: el.fontFamily || undefined, fontStyle: 'bold', fill: el.textColor || '#8B7355', listening: false }" />
                </v-group>
                <!-- BLACKBOARD -->
                <v-group v-if="el.type === 'blackboard'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-rect :config="{ width: el.width || 200, height: el.height || 36, fill: el.fillColor || '#2D4A3E', stroke: el.strokeColor || '#6B5B3E', strokeWidth: 3, cornerRadius: 2, opacity: el.opacity ?? 1, ...shadow }" />
                  <v-text :config="{ text: el.label || '展示区', x: 3, y: 2, width: (el.width || 200) - 6, height: (el.height || 36) - 4, align: 'center', verticalAlign: 'middle', fontSize: el.fontSize || 12, fontFamily: el.fontFamily || undefined, fill: el.textColor || 'rgba(255,255,255,0.5)', listening: false }" />
                </v-group>
                <!-- ROUND TABLE -->
                <v-group v-if="el.type === 'round-table'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-ellipse :config="{ x: el.radius || 40, y: el.radius || 40, radiusX: el.radius || 40, radiusY: (el.radius || 40) * 0.88, fill: el.fillColor || '#F5EBD9', stroke: el.strokeColor || '#C4A87C', strokeWidth: 1.5, opacity: el.opacity ?? 1, ...shadow }" />
                </v-group>
                <!-- RECTANGLE -->
                <v-group v-if="el.type === 'rectangle'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-rect :config="{ width: el.width || 120, height: el.height || 80, fill: el.fillColor || '#E5E7EB', stroke: el.strokeColor || '#9CA3AF', strokeWidth: 1, cornerRadius: 4, opacity: el.opacity ?? 1, ...shadow }" />
                  <v-text v-if="el.label" :config="{ text: el.label, width: el.width || 120, height: el.height || 80, align: 'center', verticalAlign: 'middle', fontSize: el.fontSize || 12, fontFamily: el.fontFamily || undefined, fill: el.textColor || '#374151', listening: false }" />
                </v-group>
                <!-- CIRCLE-SHAPE -->
                <v-circle v-if="el.type === 'circle-shape'" :config="{ x: el.x + (el.width || 60) / 2, y: el.y + (el.height || 60) / 2, radius: (el.width || 60) / 2, fill: el.fillColor || '#DBEAFE', stroke: el.strokeColor || '#93C5FD', strokeWidth: 1, opacity: el.opacity ?? 1, draggable: isDraggable && !el.locked, id: el.id, name: 'element', ...shadow }" v-bind="evtBinds(el)" />
                <!-- LINE-SHAPE -->
                <v-line v-if="el.type === 'line-shape'" :config="{ points: [el.x, el.y, el.x + (el.width || 150), el.y + (el.height || 0)], stroke: el.fillColor || '#6B7280', strokeWidth: 2, opacity: el.opacity ?? 1, draggable: isDraggable && !el.locked, id: el.id, name: 'element', hitStrokeWidth: 12 }" v-bind="evtBinds(el)" />
                <!-- AREA -->
                <v-group v-if="el.type === 'area'" :config="groupCfg(el)" v-bind="evtBinds(el)">
                  <v-rect :config="{ width: el.width || 200, height: el.height || 150, fill: el.fillColor || 'rgba(59,130,246,0.06)', stroke: el.strokeColor || '#93C5FD', strokeWidth: 1, dash: [8,4], cornerRadius: 6, opacity: el.opacity ?? 1 }" />
                  <v-text v-if="el.label" :config="{ text: el.label, width: el.width || 200, y: 4, align: 'center', fontSize: el.fontSize || 11, fontFamily: el.fontFamily || undefined, fill: el.textColor || '#6B7280', listening: false }" />
                </v-group>
                <!-- TEXT -->
                <v-text v-if="el.type === 'text'" :config="{ x: el.x, y: el.y, text: el.text || '', fontSize: el.fontSize || 14, fontFamily: el.fontFamily || undefined, fill: el.textColor || el.fillColor || '#374151', rotation: el.rotation || 0, opacity: el.opacity ?? 1, draggable: isDraggable && !el.locked, id: el.id, name: 'element' }" v-bind="evtBinds(el)" />
              </template>
              <v-transformer v-if="mode === 'edit'" ref="transformerRef" :config="transformerCfg" />
            </v-layer>
            <!-- Guide lines & rubber band overlay -->
            <v-layer :config="{ listening: false }">
              <v-line v-if="guideLineX !== null" :config="{ points: [guideLineX, 0, guideLineX, stageHeight], stroke: '#3B82F6', strokeWidth: 1, dash: [4,4] }" />
              <v-line v-if="guideLineY !== null" :config="{ points: [0, guideLineY, stageWidth, guideLineY], stroke: '#3B82F6', strokeWidth: 1, dash: [4,4] }" />
              <v-rect v-if="rubberBand" :config="{ x: rubberBand.x, y: rubberBand.y, width: rubberBand.w, height: rubberBand.h, fill: 'rgba(59,130,246,0.08)', stroke: '#3B82F6', strokeWidth: 1, dash: [4,4] }" />
              <v-rect v-if="drawPreview" :config="{ x: drawPreview.x, y: drawPreview.y, width: drawPreview.w, height: drawPreview.h, fill: 'rgba(59,130,246,0.06)', stroke: '#3B82F6', strokeWidth: 1, dash: [6,3], cornerRadius: 4 }" />
            </v-layer>
          </v-stage>
        </div>
        <!-- Status -->
        <div class="fp-status">
          <span>座位 {{ seatCount }}</span><span class="fp-status-sep">|</span>
          <span>空位 {{ emptySeatCount }}</span><span class="fp-status-sep">|</span>
          <span>元素 {{ elementCount }}</span>
          <template v-if="selectedIds.size > 0"><span class="fp-status-sep">|</span><span>选中 {{ selectedIds.size }}</span></template>
          <div class="fp-spacer" />
          <span>{{ Math.round(scale * 100) }}%</span>
          <template v-if="snapEnabled"><span class="fp-status-sep">|</span><span>网格吸附</span></template>
        </div>
      </div>

      <!-- ===== Right Panel ===== -->
      <div v-if="mode === 'edit'" class="fp-panel">
        <template v-if="selectedElement">
          <div class="fp-panel-header">
            <span>{{ typeLabels[selectedElement.type] || selectedElement.type }}</span>
            <button class="fp-icon-btn" title="删除" @click="deleteSelected"><Trash2 :size="16" /></button>
          </div>
          <!-- Position -->
          <div class="fp-section">
            <div class="fp-section-title">位置与尺寸</div>
            <div class="fp-grid-2">
              <div class="fp-field"><label>X</label><input type="number" :value="selectedElement.x" @change="onProp('x', $event)" /></div>
              <div class="fp-field"><label>Y</label><input type="number" :value="selectedElement.y" @change="onProp('y', $event)" /></div>
              <template v-if="selectedElement.type === 'seat' && (!selectedElement.seatShape || selectedElement.seatShape === 'circle')">
                <div class="fp-field"><label>直径</label><input type="number" :value="selectedElement.width" @change="onDiameter($event)" /></div>
              </template>
              <template v-else-if="selectedElement.type === 'seat'">
                <div class="fp-field"><label>W</label><input type="number" :value="selectedElement.width" @change="onProp('width', $event)" /></div>
                <div class="fp-field"><label>H</label><input type="number" :value="selectedElement.height" @change="onProp('height', $event)" /></div>
              </template>
              <template v-else-if="selectedElement.type === 'circle-shape'">
                <div class="fp-field"><label>直径</label><input type="number" :value="selectedElement.width" @change="onDiameter($event)" /></div>
              </template>
              <template v-else>
                <div class="fp-field"><label>W</label><input type="number" :value="selectedElement.width" @change="onProp('width', $event)" /></div>
                <div class="fp-field"><label>H</label><input type="number" :value="selectedElement.height" @change="onProp('height', $event)" /></div>
              </template>
            </div>
            <div class="fp-grid-2" style="margin-top:4px">
              <div class="fp-field"><label>角度</label><input type="number" :value="selectedElement.rotation || 0" step="15" @change="onProp('rotation', $event)" /></div>
              <div v-if="selectedElement.type === 'pillar' || selectedElement.type === 'round-table'" class="fp-field">
                <label>半径</label><input type="number" :value="selectedElement.radius || 15" min="5" @change="onProp('radius', $event)" />
              </div>
            </div>
          </div>
          <!-- Seat props -->
          <div v-if="selectedElement.type === 'seat' || selectedElement.type === 'seat-desk'" class="fp-section">
            <div class="fp-section-title">座位</div>
            <div class="fp-field full"><label>位置号</label><input type="text" :value="selectedElement.positionNo" @change="onProp('positionNo', $event)" /></div>
            <div class="fp-field full" style="margin-top:4px"><label>行标签</label><input type="text" :value="selectedElement.rowLabel" maxlength="2" @change="onProp('rowLabel', $event)" /></div>
            <div class="fp-field full" style="margin-top:4px"><label>入座人</label><span class="fp-readonly">{{ getOccupantName(selectedElement) || '空位' }}</span></div>
            <div class="fp-field full" style="margin-top:4px">
              <label>形状</label>
              <div class="fp-radio-group fp-radio-sm">
                <label :class="{ active: !selectedElement!.seatShape || selectedElement!.seatShape === 'circle' }"><input type="radio" value="circle" :checked="!selectedElement!.seatShape || selectedElement!.seatShape === 'circle'" @change="onSeatShape('circle')" />圆形</label>
                <label :class="{ active: selectedElement!.seatShape === 'square' }"><input type="radio" value="square" :checked="selectedElement!.seatShape === 'square'" @change="onSeatShape('square')" />方形</label>
                <label :class="{ active: selectedElement!.seatShape === 'rounded' }"><input type="radio" value="rounded" :checked="selectedElement!.seatShape === 'rounded'" @change="onSeatShape('rounded')" />圆角</label>
              </div>
            </div>
          </div>
          <!-- Label (for elements that have labels) -->
          <div v-if="hasLabel(selectedElement.type)" class="fp-section">
            <div class="fp-field full"><label>标签</label><input type="text" :value="selectedElement.label" @change="onProp('label', $event)" /></div>
            <div class="fp-field full" style="margin-top:4px">
              <label>字体</label>
              <select :value="selectedElement.fontFamily || ''" @change="onFontFamily($event)" class="fp-select">
                <option v-for="opt in fontFamilyOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
            <div class="fp-field full" style="margin-top:4px"><label>字号</label><input type="number" :value="selectedElement.fontSize || 12" min="8" max="72" @change="onProp('fontSize', $event)" /></div>
          </div>
          <!-- Text -->
          <div v-if="selectedElement.type === 'text'" class="fp-section">
            <div class="fp-field full"><label>文字</label><input type="text" :value="selectedElement.text" @change="onProp('text', $event)" /></div>
            <div class="fp-field full" style="margin-top:4px">
              <label>字体</label>
              <select :value="selectedElement.fontFamily || ''" @change="onFontFamily($event)" class="fp-select">
                <option v-for="opt in fontFamilyOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
            <div class="fp-field full" style="margin-top:4px"><label>字号</label><input type="number" :value="selectedElement.fontSize || 14" min="8" max="72" @change="onProp('fontSize', $event)" /></div>
          </div>
          <!-- Appearance: Seat-specific -->
          <div v-if="isSeatType(selectedElement.type)" class="fp-section">
            <div class="fp-section-title">外观</div>
            <div class="fp-field full">
              <label>字体</label>
              <select :value="selectedElement.fontFamily || ''" @change="onFontFamily($event)" class="fp-select">
                <option v-for="opt in fontFamilyOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>字号</label>
              <input type="number" :value="selectedElement.fontSize || 9" min="6" max="36" @change="onProp('fontSize', $event)" />
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>文字色</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: selectedElement.textColor || '#9CA3AF' }">
                  <input type="color" :value="selectedElement.textColor || '#9CA3AF'" @input="onTextColor($event)" />
                </div>
                <span class="fp-color-hex">{{ selectedElement.textColor || '默认' }}</span>
                <button v-if="selectedElement.textColor" class="fp-icon-btn-sm" title="重置" @click="resetTextColor"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>入座文字</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: selectedElement.occupiedTextColor || '#FFFFFF' }">
                  <input type="color" :value="selectedElement.occupiedTextColor || '#FFFFFF'" @input="onOccupiedTextColor($event)" />
                </div>
                <span class="fp-color-hex">{{ selectedElement.occupiedTextColor || '默认' }}</span>
                <button v-if="selectedElement.occupiedTextColor" class="fp-icon-btn-sm" title="重置" @click="resetOccupiedTextColor"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>空位背景</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: selectedElement.fillColor || '#FFFFFF' }">
                  <input type="color" :value="selectedElement.fillColor || '#FFFFFF'" @input="onColor($event)" />
                </div>
                <span class="fp-color-hex">{{ selectedElement.fillColor || '#FFFFFF' }}</span>
                <button v-if="selectedElement.fillColor" class="fp-icon-btn-sm" title="重置" @click="resetColor"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>入座背景</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: selectedElement.occupiedColor || '#5B8DEF' }">
                  <input type="color" :value="selectedElement.occupiedColor || '#5B8DEF'" @input="onOccupiedColor($event)" />
                </div>
                <span class="fp-color-hex">{{ selectedElement.occupiedColor || '#5B8DEF' }}</span>
                <button v-if="selectedElement.occupiedColor" class="fp-icon-btn-sm" title="重置" @click="resetOccupiedColor"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>边框色</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: selectedElement.strokeColor || '#D4D7DC' }">
                  <input type="color" :value="selectedElement.strokeColor || '#D4D7DC'" @input="onStrokeColor($event)" />
                </div>
                <span class="fp-color-hex">{{ selectedElement.strokeColor || '默认' }}</span>
                <button v-if="selectedElement.strokeColor" class="fp-icon-btn-sm" title="重置" @click="resetStrokeColor"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>入座边框</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: selectedElement.occupiedStrokeColor || 'transparent' }">
                  <input type="color" :value="selectedElement.occupiedStrokeColor || '#D4D7DC'" @input="onOccupiedStrokeColor($event)" />
                </div>
                <span class="fp-color-hex">{{ selectedElement.occupiedStrokeColor || '默认' }}</span>
                <button v-if="selectedElement.occupiedStrokeColor" class="fp-icon-btn-sm" title="重置" @click="resetOccupiedStrokeColor"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div class="fp-field full" style="margin-top:4px"><label>透明度</label><input type="range" min="0.1" max="1" step="0.05" :value="selectedElement.opacity ?? 1" @input="onOpacity($event)" /></div>
          </div>
          <!-- Appearance: Non-seat -->
          <div v-else class="fp-section">
            <div class="fp-section-title">外观</div>
            <div class="fp-field full">
              <label>填充</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: selectedElement.fillColor || defaultColor(selectedElement.type) }">
                  <input type="color" :value="selectedElement.fillColor || defaultColor(selectedElement.type)" @input="onColor($event)" />
                </div>
                <span class="fp-color-hex">{{ selectedElement.fillColor || defaultColor(selectedElement.type) }}</span>
                <button v-if="selectedElement.fillColor" class="fp-icon-btn-sm" title="重置" @click="resetColor"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div v-if="hasStroke(selectedElement.type)" class="fp-field full" style="margin-top:4px">
              <label>边框</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: selectedElement.strokeColor || defaultStroke(selectedElement.type) }">
                  <input type="color" :value="selectedElement.strokeColor || defaultStroke(selectedElement.type)" @input="onStrokeColor($event)" />
                </div>
                <span class="fp-color-hex">{{ selectedElement.strokeColor || defaultStroke(selectedElement.type) }}</span>
                <button v-if="selectedElement.strokeColor" class="fp-icon-btn-sm" title="重置" @click="resetStrokeColor"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div v-if="hasTextColor(selectedElement)" class="fp-field full" style="margin-top:4px">
              <label>文字</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: selectedElement.textColor || defaultTextColor(selectedElement) }">
                  <input type="color" :value="selectedElement.textColor || defaultTextColor(selectedElement)" @input="onTextColor($event)" />
                </div>
                <span class="fp-color-hex">{{ selectedElement.textColor || defaultTextColor(selectedElement) }}</span>
                <button v-if="selectedElement.textColor" class="fp-icon-btn-sm" title="重置" @click="resetTextColor"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div class="fp-field full" style="margin-top:4px"><label>透明度</label><input type="range" min="0.1" max="1" step="0.05" :value="selectedElement.opacity ?? 1" @input="onOpacity($event)" /></div>
          </div>
          <!-- Layer -->
          <div class="fp-section">
            <div class="fp-section-title">图层</div>
            <div class="fp-layer-row">
              <button class="fp-icon-btn" title="上移一层" @click="bringForward(selectedElement!.id)"><ArrowUp :size="16" /></button>
              <button class="fp-icon-btn" title="下移一层" @click="sendBackward(selectedElement!.id)"><ArrowDown :size="16" /></button>
              <div class="fp-spacer" />
              <button class="fp-icon-btn" :class="{ active: selectedElement!.locked }" :title="selectedElement!.locked ? '解锁' : '锁定'" @click="toggleLock(selectedElement!.id)">
                <Lock v-if="selectedElement!.locked" :size="16" /><Unlock v-else :size="16" />
              </button>
            </div>
          </div>
        </template>
        <!-- Multi-selection panel -->
        <template v-else-if="selectedIds.size >= 2">
          <div class="fp-panel-header">
            <span>多选 ({{ selectedIds.size }})</span>
            <button class="fp-icon-btn" title="删除" @click="deleteSelected"><Trash2 :size="16" /></button>
          </div>
          <!-- Batch appearance -->
          <div class="fp-section">
            <div class="fp-section-title">批量外观</div>
            <div class="fp-field full">
              <label>字体</label>
              <select :value="multiFontFamily" @change="onMultiFontFamily($event)" class="fp-select">
                <option v-for="opt in fontFamilyOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>字号</label>
              <input type="number" :value="multiFontSize" min="6" max="72" @change="onMultiFontSize($event)" />
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>文字色</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: multiTextColor || '#374151' }">
                  <input type="color" :value="multiTextColor || '#374151'" @input="onMultiColor('textColor', $event)" />
                </div>
                <span class="fp-color-hex">{{ multiTextColor || '混合' }}</span>
                <button v-if="multiTextColor" class="fp-icon-btn-sm" title="重置全部" @click="onMultiResetColor('textColor')"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>填充</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: multiFillColor || '#E5E7EB' }">
                  <input type="color" :value="multiFillColor || '#E5E7EB'" @input="onMultiColor('fillColor', $event)" />
                </div>
                <span class="fp-color-hex">{{ multiFillColor || '混合' }}</span>
                <button v-if="multiFillColor" class="fp-icon-btn-sm" title="重置全部" @click="onMultiResetColor('fillColor')"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div v-if="multiHasSeats" class="fp-field full" style="margin-top:4px">
              <label>入座文字</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: multiOccupiedTextColor || '#FFFFFF' }">
                  <input type="color" :value="multiOccupiedTextColor || '#FFFFFF'" @input="onMultiColor('occupiedTextColor', $event)" />
                </div>
                <span class="fp-color-hex">{{ multiOccupiedTextColor || '混合' }}</span>
                <button v-if="multiOccupiedTextColor" class="fp-icon-btn-sm" title="重置全部" @click="onMultiResetColor('occupiedTextColor')"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div v-if="multiHasSeats" class="fp-field full" style="margin-top:4px">
              <label>入座背景</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: multiOccupiedColor || '#5B8DEF' }">
                  <input type="color" :value="multiOccupiedColor || '#5B8DEF'" @input="onMultiColor('occupiedColor', $event)" />
                </div>
                <span class="fp-color-hex">{{ multiOccupiedColor || '混合' }}</span>
                <button v-if="multiOccupiedColor" class="fp-icon-btn-sm" title="重置全部" @click="onMultiResetColor('occupiedColor')"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div v-if="multiHasStroke" class="fp-field full" style="margin-top:4px">
              <label>边框色</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: multiStrokeColor || '#9CA3AF' }">
                  <input type="color" :value="multiStrokeColor || '#9CA3AF'" @input="onMultiColor('strokeColor', $event)" />
                </div>
                <span class="fp-color-hex">{{ multiStrokeColor || '混合' }}</span>
                <button v-if="multiStrokeColor" class="fp-icon-btn-sm" title="重置全部" @click="onMultiResetColor('strokeColor')"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div v-if="multiHasSeats" class="fp-field full" style="margin-top:4px">
              <label>入座边框</label>
              <div class="fp-color-row">
                <div class="fp-color-swatch" :style="{ background: multiOccupiedStrokeColor || 'transparent' }">
                  <input type="color" :value="multiOccupiedStrokeColor || '#D4D7DC'" @input="onMultiColor('occupiedStrokeColor', $event)" />
                </div>
                <span class="fp-color-hex">{{ multiOccupiedStrokeColor || '混合' }}</span>
                <button v-if="multiOccupiedStrokeColor" class="fp-icon-btn-sm" title="重置全部" @click="onMultiResetColor('occupiedStrokeColor')"><RotateCcw :size="12" /></button>
              </div>
            </div>
            <div class="fp-field full" style="margin-top:4px">
              <label>透明度</label>
              <input type="range" min="0.1" max="1" step="0.05" :value="multiOpacity" @input="onMultiOpacity($event)" />
            </div>
          </div>
        </template>
        <template v-else>
          <div class="fp-panel-header"><span>画布</span></div>
          <div class="fp-section">
            <div class="fp-section-title">网格样式</div>
            <div class="fp-radio-group">
              <label :class="{ active: floorStyle === 'grid' }"><input type="radio" value="grid" :checked="floorStyle === 'grid'" @change="floorStyle = 'grid'" />网格</label>
              <label :class="{ active: floorStyle === 'dots' }"><input type="radio" value="dots" :checked="floorStyle === 'dots'" @change="floorStyle = 'dots'" />点阵</label>
              <label :class="{ active: floorStyle === 'plain' }"><input type="radio" value="plain" :checked="floorStyle === 'plain'" @change="floorStyle = 'plain'" />无</label>
            </div>
          </div>
          <p class="fp-hint">点击元素查看属性<br/>Shift+点击 多选<br/>拖拽空白区域 框选</p>
        </template>
      </div>
    </div>

    <!-- ===== Batch Dialog ===== -->
    <Teleport to="body">
      <Transition name="fp-fade">
        <div v-if="showBatchDialog" class="fp-overlay" @click.self="showBatchDialog = false">
          <div class="fp-dialog fp-dialog-wide">
            <div class="fp-dialog-head">
              <h3>批量添加座位</h3>
              <button class="fp-icon-btn" @click="showBatchDialog = false"><X :size="18" /></button>
            </div>
            <div class="fp-dialog-body fp-batch-body">
              <!-- Left: form -->
              <div class="fp-batch-form">
                <div class="fp-batch-group-label">基本设置</div>
                <div class="fp-form-grid">
                  <div class="fp-form-item"><label>行数</label><input type="number" v-model.number="batchForm.rows" min="1" max="50" /></div>
                  <div class="fp-form-item"><label>每行</label><input type="number" v-model.number="batchForm.cols" min="1" max="50" /></div>
                  <div class="fp-form-item"><label>起始行标</label><input type="text" v-model="batchForm.startLabel" maxlength="1" /></div>
                  <div class="fp-form-item"><label>起始编号</label><input type="number" v-model.number="batchForm.startNumber" min="1" max="999" /></div>
                </div>
                <div class="fp-batch-group-label">座位样式</div>
                <div class="fp-form-item full">
                  <label>形状</label>
                  <div class="fp-radio-group fp-radio-sm" style="margin-top:4px">
                    <label :class="{ active: batchForm.seatShape === 'circle' }"><input type="radio" value="circle" v-model="batchForm.seatShape" />圆形</label>
                    <label :class="{ active: batchForm.seatShape === 'square' }"><input type="radio" value="square" v-model="batchForm.seatShape" />方形</label>
                    <label :class="{ active: batchForm.seatShape === 'rounded' }"><input type="radio" value="rounded" v-model="batchForm.seatShape" />圆角</label>
                  </div>
                </div>
                <div class="fp-form-grid" style="margin-top:6px">
                  <template v-if="batchForm.seatShape === 'circle'">
                    <div class="fp-form-item"><label>直径</label><input type="number" v-model.number="batchForm.circleDiameter" min="16" max="80" /></div>
                  </template>
                  <template v-else>
                    <div class="fp-form-item"><label>宽(字符)</label><input type="number" v-model.number="batchForm.charCols" min="2" max="10" title="每个字符约10px" /></div>
                    <div class="fp-form-item"><label>高(行)</label><input type="number" v-model.number="batchForm.charRows" min="1" max="4" title="1=仅编号 2=编号+姓名" /></div>
                  </template>
                </div>
                <div class="fp-batch-group-label">间距</div>
                <div class="fp-form-grid">
                  <div class="fp-form-item"><label>列间距</label><input type="number" v-model.number="batchForm.columnGap" min="0" max="100" /></div>
                  <div class="fp-form-item"><label>行间距</label><input type="number" v-model.number="batchForm.rowGap" min="0" max="100" /></div>
                  <div class="fp-form-item"><label>过道列</label><input type="number" v-model.number="batchForm.aisleAfterCol" min="0" :max="batchForm.cols" title="每N列后插入过道，0=无" /></div>
                  <div class="fp-form-item"><label>过道宽</label><input type="number" v-model.number="batchForm.aisleWidth" min="10" max="100" :disabled="batchForm.aisleAfterCol === 0" /></div>
                </div>
                <div class="fp-batch-group-label">排列</div>
                <div class="fp-form-item full">
                  <label>编号范围</label>
                  <div class="fp-radio-group fp-radio-sm" style="margin-top:4px">
                    <label :class="{ active: batchForm.numberingScope === 'global' }"><input type="radio" value="global" v-model="batchForm.numberingScope" />连续</label>
                    <label :class="{ active: batchForm.numberingScope === 'per-row' }"><input type="radio" value="per-row" v-model="batchForm.numberingScope" />每排独立</label>
                  </div>
                </div>
                <div class="fp-form-item full" style="margin-top:4px">
                  <label>编号方向</label>
                  <div v-if="batchForm.numberingScope === 'global'" class="fp-radio-group fp-radio-sm" style="margin-top:4px">
                    <label :class="{ active: batchForm.numberingDir === 'ltr' }"><input type="radio" value="ltr" v-model="batchForm.numberingDir" />左→右</label>
                    <label :class="{ active: batchForm.numberingDir === 'rtl' }"><input type="radio" value="rtl" v-model="batchForm.numberingDir" />右→左</label>
                    <label :class="{ active: batchForm.numberingDir === 'snake' }"><input type="radio" value="snake" v-model="batchForm.numberingDir" />蛇形</label>
                    <label :class="{ active: batchForm.numberingDir === 'column' }"><input type="radio" value="column" v-model="batchForm.numberingDir" />列优先</label>
                  </div>
                  <div v-else class="fp-radio-group fp-radio-sm" style="margin-top:4px">
                    <label :class="{ active: batchForm.numberingDir === 'ltr' }"><input type="radio" value="ltr" v-model="batchForm.numberingDir" />左→右</label>
                    <label :class="{ active: batchForm.numberingDir === 'rtl' }"><input type="radio" value="rtl" v-model="batchForm.numberingDir" />右→左</label>
                    <label :class="{ active: batchForm.numberingDir === 'center' }"><input type="radio" value="center" v-model="batchForm.numberingDir" />中间→两边</label>
                  </div>
                </div>
                <label class="fp-checkbox" style="margin-top:8px">
                  <input type="checkbox" v-model="batchForm.staggered" />
                  <span>错位排列（偶数行偏移半个间距）</span>
                </label>
              </div>
              <!-- Right: preview -->
              <div class="fp-batch-preview-area">
                <div class="fp-batch-preview-label">预览</div>
                <div class="fp-batch-preview-canvas">
                  <div :style="previewContainerStyle" class="fp-preview-container">
                    <div v-for="seat in previewSeats" :key="seat.no"
                      class="fp-preview-seat"
                      :class="batchForm.seatShape"
                      :style="{ left: seat.x + 'px', top: seat.y + 'px', width: seat.w + 'px', height: seat.h + 'px' }">
                      {{ seat.no }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="fp-dialog-foot">
              <span class="fp-batch-summary">
                预计 <strong>{{ batchTotalSeats }}</strong> 个座位
                <span class="fp-batch-dim">{{ computedSeatWidth }}×{{ computedSeatHeight }}px</span>
                <span v-if="previewRawSize.w" class="fp-batch-dim">总 {{ previewRawSize.w }}×{{ previewRawSize.h }}px</span>
              </span>
              <div class="fp-spacer" />
              <button class="fp-btn-secondary" @click="showBatchDialog = false">取消</button>
              <button class="fp-btn-primary" @click="doBatchAdd">添加</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ===== Template Dialog ===== -->
    <Teleport to="body">
      <Transition name="fp-fade">
        <div v-if="showTemplateDialog" class="fp-overlay" @click.self="showTemplateDialog = false">
          <div class="fp-dialog">
            <div class="fp-dialog-head">
              <h3>选择模板</h3>
              <button class="fp-icon-btn" @click="showTemplateDialog = false"><X :size="18" /></button>
            </div>
            <div class="fp-dialog-body">
              <div v-for="tpl in templates" :key="tpl.id" class="fp-tpl-card" :class="{ selected: selectedTplId === tpl.id }" @click="selectedTplId = tpl.id">
                <span class="fp-tpl-icon">{{ tpl.icon }}</span>
                <div class="fp-tpl-info">
                  <div class="fp-tpl-name">{{ tpl.name }}</div>
                  <div class="fp-tpl-meta">{{ tpl.seats }} 座位 · {{ tpl.description }}</div>
                </div>
              </div>
            </div>
            <div class="fp-dialog-foot">
              <button class="fp-btn-secondary" @click="showTemplateDialog = false">取消</button>
              <button class="fp-btn-primary" :disabled="!selectedTplId" @click="doApplyTemplate">应用</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import {
  MousePointer2, Hand, CircleDot, Table2, Armchair, Circle,
  Presentation, RectangleHorizontal, SeparatorHorizontal,
  DoorOpen, AppWindow, PanelTop, Type as TypeIcon,
  Plus, LayoutTemplate, Image as ImageIcon, Undo2, Redo2,
  ZoomIn, ZoomOut, Maximize2, Grid3x3, X, ChevronDown,
  Trash2, ArrowUp, ArrowDown, Lock, Unlock, RotateCcw,
  AlignLeft, AlignCenter, AlignRight,
  ArrowUpToLine, ArrowDownToLine, AlignVerticalSpaceAround,
  MoveHorizontal, MoveVertical, Crosshair,
  Square, CircleDashed, Minus, BoxSelect,
} from 'lucide-vue-next'
import type { FloorPlanElement, FloorPlanLayout, FloorPlanElementType, PlaceOccupant } from '@/types/universalPlace'
import { useFloorPlan, TEMPLATES, type ActiveTool } from '../composables/useFloorPlan'

// ===== Props & Emits =====
interface Props {
  placeId: number | string
  initialLayout?: FloorPlanLayout | null
  occupants?: PlaceOccupant[]
  mode?: 'edit' | 'view'
  showSeatLabels?: boolean
}
const props = withDefaults(defineProps<Props>(), { initialLayout: null, occupants: () => [], mode: 'edit', showSeatLabels: false })
const emit = defineEmits<{ 'seat-click': [positionNo: string] }>()

// ===== Composable =====
const {
  elements, sortedElements, stageWidth, stageHeight, backgroundImage,
  selectedIds, activeTool, selectedElement, selectedElements, snapEnabled, floorStyle, clipboard,
  guideLineX, guideLineY, rubberBand,
  seatCount, deskCount, elementCount,
  canUndo, canRedo, undo, redo,
  bringForward, sendBackward, toggleLock,
  addElement, updateElement, commitUpdate, deleteSelected,
  selectElement, clearSelection, selectAll,
  addSeatGrid, generateSeatGridPreview, copySelected, cutSelected, pasteClipboard, duplicateSelected,
  alignSelected, distributeSelected,
  computeGuides, updateGuides, clearGuides,
  applyTemplate, toLayout, loadLayout, snap,
} = useFloorPlan()

// ===== Tool definitions =====
const furnitureTools: { key: ActiveTool; icon: any; label: string }[] = [
  { key: 'seat', icon: CircleDot, label: '座位' },
  { key: 'desk', icon: Table2, label: '桌子' },
  { key: 'seat-desk', icon: Armchair, label: '桌椅' },
  { key: 'round-table', icon: Circle, label: '圆桌' },
  { key: 'podium', icon: Presentation, label: '平台' },
]
const structTools: { key: ActiveTool; icon: any; label: string }[] = [
  { key: 'wall', icon: RectangleHorizontal, label: '墙体' },
  { key: 'partition', icon: SeparatorHorizontal, label: '隔断' },
  { key: 'pillar', icon: Crosshair, label: '柱子' },
]
const facilityTools: { key: ActiveTool; icon: any; label: string }[] = [
  { key: 'door', icon: DoorOpen, label: '门' },
  { key: 'window', icon: AppWindow, label: '窗户' },
  { key: 'blackboard', icon: PanelTop, label: '展示板' },
]
const shapeTools: { key: ActiveTool; icon: any; label: string }[] = [
  { key: 'rectangle', icon: Square, label: '矩形' },
  { key: 'circle-shape', icon: CircleDashed, label: '圆形' },
  { key: 'line-shape', icon: Minus, label: '线条' },
  { key: 'area', icon: BoxSelect, label: '区域' },
]
const typeLabels: Record<string, string> = {
  seat: '座位', desk: '桌子', 'seat-desk': '桌椅', 'round-table': '圆桌',
  podium: '平台', blackboard: '展示板', wall: '墙体', partition: '隔断',
  pillar: '柱子', door: '门', window: '窗户', text: '文字',
  rectangle: '矩形', 'circle-shape': '圆形', 'line-shape': '线条', area: '区域',
}

function hasLabel(type: string): boolean {
  return ['desk', 'podium', 'blackboard', 'rectangle', 'area'].includes(type)
}

// ===== Dropdown groups =====
const openDropdown = ref<string | null>(null)
function toggleDropdown(group: string) {
  openDropdown.value = openDropdown.value === group ? null : group
}
const isFurnitureActive = computed(() => furnitureTools.some(t => t.key === activeTool.value))
const isStructActive = computed(() => structTools.some(t => t.key === activeTool.value))
const isFacilityActive = computed(() => facilityTools.some(t => t.key === activeTool.value))
const activeFurnitureTool = computed(() => furnitureTools.find(t => t.key === activeTool.value) || furnitureTools[0])
const activeStructTool = computed(() => structTools.find(t => t.key === activeTool.value) || structTools[0])
const activeFacilityTool = computed(() => facilityTools.find(t => t.key === activeTool.value) || facilityTools[0])

// Close dropdown on outside click
function handleDocClick(e: MouseEvent) {
  if (openDropdown.value && !(e.target as HTMLElement)?.closest('.fp-tool-group')) {
    openDropdown.value = null
  }
}

// ===== Rendering helpers =====
const shadow = { shadowColor: 'rgba(0,0,0,0.08)', shadowBlur: 4, shadowOffsetX: 0, shadowOffsetY: 1 }

function defaultColor(type: string): string {
  const map: Record<string, string> = {
    seat: '#5B8DEF', desk: '#FAECD8', 'seat-desk': '#FAECD8', wall: '#4B5563',
    door: '#C4956A', window: '#E8F4FD', podium: '#F5EBD9', blackboard: '#2D4A3E',
    pillar: '#9CA3AF', partition: '#CBD5E1', 'round-table': '#F5EBD9', text: '#374151',
    rectangle: '#E5E7EB', 'circle-shape': '#DBEAFE', 'line-shape': '#6B7280', area: 'rgba(59,130,246,0.06)',
  }
  return map[type] || '#9CA3AF'
}

function groupCfg(el: FloorPlanElement) {
  return { x: el.x, y: el.y, rotation: el.rotation || 0, draggable: isDraggable.value && !el.locked, id: el.id, name: 'element' }
}
function baseConfig(el: FloorPlanElement) {
  return { x: el.x, y: el.y, width: el.width, height: el.height, rotation: el.rotation || 0, opacity: el.opacity ?? 1, draggable: isDraggable.value && !el.locked, id: el.id, name: 'element' }
}

// ===== Occupants =====
const occupantMap = computed(() => {
  const m = new Map<string, PlaceOccupant>()
  for (const o of props.occupants ?? []) if (o.positionNo) m.set(o.positionNo, o)
  return m
})
function getOccupantName(el: FloorPlanElement): string {
  if (el.type !== 'seat' && el.type !== 'seat-desk') return ''
  if (!el.positionNo) return ''
  return occupantMap.value.get(el.positionNo)?.occupantName || ''
}
function isOccupied(el: FloorPlanElement): boolean { return !!getOccupantName(el) }
const emptySeatCount = computed(() =>
  elements.value.filter(e => (e.type === 'seat' || e.type === 'seat-desk') && e.positionNo && !occupantMap.value.has(e.positionNo)).length
)

// ===== Seat circle rendering (no shadow for perf — seats are small) =====
function seatFill(el: FloorPlanElement, occ: boolean): string {
  return occ ? (el.occupiedColor || el.categoryColor || '#5B8DEF') : (el.fillColor || '#FFFFFF')
}
function seatStroke(el: FloorPlanElement, occ: boolean): string {
  if (occ && el.occupiedStrokeColor) return el.occupiedStrokeColor
  if (el.strokeColor) return el.strokeColor
  return occ ? 'transparent' : '#D4D7DC'
}
function seatStrokeWidth(el: FloorPlanElement, occ: boolean): number {
  if (occ && el.occupiedStrokeColor) return 1.5
  if (el.strokeColor) return 1.5
  return occ ? 0 : 1.5
}
function seatCircleCfg(el: FloorPlanElement) {
  const r = Math.min(el.width || 30, el.height || 30) / 2
  const occ = isOccupied(el)
  return {
    x: (el.width || 30) / 2, y: (el.height || 30) / 2, radius: r,
    fill: seatFill(el, occ),
    stroke: seatStroke(el, occ),
    strokeWidth: seatStrokeWidth(el, occ),
    opacity: el.opacity ?? 1, listening: true, perfectDrawEnabled: false,
  }
}
function seatRectCfg(el: FloorPlanElement) {
  const w = el.width || 30, h = el.height || 30
  const occ = isOccupied(el)
  return {
    width: w, height: h,
    cornerRadius: el.seatShape === 'rounded' ? 6 : 0,
    fill: seatFill(el, occ),
    stroke: seatStroke(el, occ),
    strokeWidth: seatStrokeWidth(el, occ),
    opacity: el.opacity ?? 1, listening: true, perfectDrawEnabled: false,
  }
}
function seatTextFill(el: FloorPlanElement, occ: boolean): string {
  if (occ && el.occupiedTextColor) return el.occupiedTextColor
  if (el.textColor) return el.textColor
  return occ ? '#FFFFFF' : '#9CA3AF'
}
function seatFontSize(el: FloorPlanElement, occ: boolean, mode: 'label' | 'default' = 'default'): number {
  if (el.fontSize) return el.fontSize
  if (mode === 'label') return occ ? 8 : 9
  return occ ? 10 : 9
}
function seatTextCfg(el: FloorPlanElement) {
  const occ = isOccupied(el)
  const showLabel = props.showSeatLabels
  const name = getOccupantName(el)
  const w = el.width || 30, h = el.height || 30

  const ff = el.fontFamily || undefined
  // When showSeatLabels is on and seat is occupied: two lines (name + positionNo)
  if (showLabel && occ && el.positionNo) {
    return {
      text: `${name}\n${el.positionNo}`,
      x: 0, y: 0, width: w, height: h,
      align: 'center', verticalAlign: 'middle',
      fontSize: seatFontSize(el, occ, 'label'), lineHeight: 1.3, fontStyle: 'bold',
      fontFamily: ff, fill: seatTextFill(el, occ), listening: false,
    }
  }
  // When showSeatLabels is on and seat is empty: full positionNo
  if (showLabel && !occ && el.positionNo) {
    return {
      text: el.positionNo,
      x: 0, y: 0, width: w, height: h,
      align: 'center', verticalAlign: 'middle',
      fontSize: seatFontSize(el, occ, 'label'), fontStyle: 'normal',
      fontFamily: ff, fill: seatTextFill(el, occ), listening: false,
    }
  }
  // Default: name or short column number
  return {
    text: seatDisplayText(el), x: 0, y: 0,
    width: w, height: h,
    align: 'center', verticalAlign: 'middle',
    fontSize: seatFontSize(el, occ),
    fontStyle: occ ? 'bold' : 'normal',
    fontFamily: ff, fill: seatTextFill(el, occ), listening: false,
  }
}
function seatDisplayText(el: FloorPlanElement): string {
  const name = getOccupantName(el)
  if (name) return name
  const rawNo = el.positionNo || ''
  return rawNo.includes('-') ? rawNo.split('-')[1]! : rawNo
}

// ===== Row & column label positions =====
const colLabelPositions = computed(() => {
  const seats = elements.value.filter(e => e.type === 'seat' || e.type === 'seat-desk')
  if (seats.length < 2) return []
  const groups: { cx: number; minY: number; avgW: number; count: number }[] = []
  const sorted = [...seats].sort((a, b) => a.x - b.x)
  for (const s of sorted) {
    const sw = s.width || 30
    const cx = s.x + sw / 2
    const match = groups.find(g => Math.abs(g.cx - cx) < 15)
    if (match) { match.minY = Math.min(match.minY, s.y); match.avgW = (match.avgW * match.count + sw) / (match.count + 1); match.count++ }
    else groups.push({ cx, minY: s.y, avgW: sw, count: 1 })
  }
  if (groups.length < 2) return []
  // Auto-size: scale font relative to average seat width, clamp 8-14
  const globalAvgW = groups.reduce((s, g) => s + g.avgW, 0) / groups.length
  const fontSize = Math.max(8, Math.min(14, Math.round(globalAvgW * 0.32)))
  const topY = Math.min(...groups.map(g => g.minY))
  return groups.map((g, i) => ({ label: String(i + 1), x: g.cx, y: topY - fontSize - 4, fontSize }))
})
const rowLabelPositions = computed(() => {
  const groups = new Map<string, { minX: number; minY: number; maxY: number; h: number }>()
  for (const el of elements.value) {
    if (el.type !== 'seat' || !el.rowLabel) continue
    const h = el.height || 30
    const ex = groups.get(el.rowLabel)
    if (!ex) {
      groups.set(el.rowLabel, { minX: el.x, minY: el.y, maxY: el.y, h })
    } else {
      if (el.x < ex.minX) ex.minX = el.x
      if (el.y < ex.minY) ex.minY = el.y
      if (el.y > ex.maxY) { ex.maxY = el.y; ex.h = h }
    }
  }
  // Use global minimum x so all row labels align vertically
  const globalMinX = groups.size > 0 ? Math.min(...[...groups.values()].map(g => g.minX)) : 0
  return [...groups.entries()].map(([label, g]) => {
    const centerY = (g.minY + g.maxY + g.h) / 2
    return { label, x: globalMinX - 22, y: centerY - 6 }
  })
})

// ===== Refs =====
const canvasContainerRef = ref<HTMLElement | null>(null)
const stageRef = ref<any>(null)
const mainLayerRef = ref<any>(null)
const transformerRef = ref<any>(null)

// ===== Zoom & Pan =====
const scale = ref(1)
const stageX = ref(0)
const stageY = ref(0)
const containerWidth = ref(900)
const containerHeight = ref(600)
let resizeObs: ResizeObserver | null = null

const stageConfig = computed(() => ({
  width: containerWidth.value, height: containerHeight.value,
  scaleX: scale.value, scaleY: scale.value,
  x: stageX.value, y: stageY.value,
  draggable: false, // We handle pan via middle-mouse or when select+drag on empty
}))
const transformerCfg = {
  rotateEnabled: true,
  enabledAnchors: ['top-left','top-right','bottom-left','bottom-right','middle-left','middle-right','top-center','bottom-center'],
  boundBoxFunc: (oldBox: any, newBox: any) => (newBox.width < 10 || newBox.height < 5 ? oldBox : newBox),
}
const isDraggable = computed(() => props.mode === 'edit')

// ===== Grid via sceneFunc (performance: single shape instead of thousands of nodes) =====
const gridShapeConfig = computed(() => {
  const w = stageWidth.value
  const h = stageHeight.value
  const style = floorStyle.value
  return {
    sceneFunc: (ctx: any, shape: any) => {
      if (style === 'dots') {
        ctx.fillStyle = '#D1D5DB'
        for (let x = 0; x <= w; x += 20) {
          for (let y = 0; y <= h; y += 20) {
            ctx.beginPath()
            ctx.arc(x, y, 1, 0, Math.PI * 2)
            ctx.fill()
          }
        }
      } else if (style === 'grid') {
        ctx.beginPath()
        ctx.strokeStyle = '#ECEEF1'
        ctx.lineWidth = 0.5
        for (let x = 0; x <= w; x += 20) {
          ctx.moveTo(x, 0)
          ctx.lineTo(x, h)
        }
        for (let y = 0; y <= h; y += 20) {
          ctx.moveTo(0, y)
          ctx.lineTo(w, y)
        }
        ctx.stroke()
      }
      ctx.fillStrokeShape(shape)
    },
    listening: false,
  }
})

// ===== BG Image =====
const bgImageObj = ref<HTMLImageElement | null>(null)
watch(backgroundImage, (src) => {
  if (!src) { bgImageObj.value = null; return }
  const img = new Image()
  img.onload = () => { bgImageObj.value = img }
  img.src = src
}, { immediate: true })
function handleBgUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => { backgroundImage.value = reader.result as string }
  reader.readAsDataURL(file)
  ;(e.target as HTMLInputElement).value = ''
}

// ===== Events =====
function evtBinds(el: FloorPlanElement) {
  return {
    onClick: (e: any) => handleElementClick(e, el),
    onTap: (e: any) => handleElementClick(e, el),
    onDragmove: (e: any) => handleDragMove(e, el),
    onDragend: (e: any) => handleDragEnd(e, el),
    onTransformend: (e: any) => handleTransformEnd(e, el),
    onMouseenter: () => setCursor(el),
    onMouseleave: () => resetCursor(),
  }
}
function setCursor(el: FloorPlanElement) {
  const stage = stageRef.value?.getStage()
  if (!stage) return
  if (props.mode === 'view' && (el.type === 'seat' || el.type === 'seat-desk')) {
    stage.container().style.cursor = 'pointer'
  } else if (props.mode === 'edit') {
    stage.container().style.cursor = el.locked ? 'not-allowed' : 'move'
  }
}
function resetCursor() {
  const stage = stageRef.value?.getStage()
  if (!stage) return
  if (isSpacePressed.value || activeTool.value === 'hand') {
    stage.container().style.cursor = 'grab'
  } else if (activeTool.value === 'select') {
    stage.container().style.cursor = 'default'
  } else {
    stage.container().style.cursor = 'crosshair'
  }
}

// ===== Rubber band & pan state =====
let rbStartX = 0
let rbStartY = 0
let isRubberBanding = false
let isPanning = false
let panStartX = 0
let panStartY = 0
let panStartStageX = 0
let panStartStageY = 0
const isSpacePressed = ref(false)

// ===== Drag-to-draw state =====
let isDrawingShape = false
let drawStartX = 0
let drawStartY = 0
const drawPreview = ref<{ x: number; y: number; w: number; h: number } | null>(null)
let drawShapeType: FloorPlanElementType | null = null

function getCanvasPoint(e: any): { x: number; y: number } | null {
  const stage = stageRef.value?.getStage()
  if (!stage) return null
  const pointer = stage.getPointerPosition()
  if (!pointer) return null
  return stage.getAbsoluteTransform().copy().invert().point(pointer)
}

function handleStageMouseDown(e: any) {
  const evt = e.evt || e
  if (!evt) return
  const target = e.target
  const stage = stageRef.value?.getStage()
  if (!stage) return

  // Middle mouse or Space+click = pan
  if (evt.button === 1 || isSpacePressed.value) {
    isPanning = true
    panStartX = evt.clientX
    panStartY = evt.clientY
    panStartStageX = stageX.value
    panStartStageY = stageY.value
    stage.container().style.cursor = 'grabbing'
    return
  }

  // Hand tool = left-click pan
  if (activeTool.value === 'hand') {
    isPanning = true
    panStartX = evt.clientX
    panStartY = evt.clientY
    panStartStageX = stageX.value
    panStartStageY = stageY.value
    stage.container().style.cursor = 'grabbing'
    return
  }

  if (target === stage) {
    if (props.mode === 'view') {
      // In view mode, left-click on empty area starts panning
      isPanning = true
      panStartX = evt.clientX
      panStartY = evt.clientY
      panStartStageX = stageX.value
      panStartStageY = stageY.value
      stage.container().style.cursor = 'grabbing'
      return
    }
    clearSelection(); updateTransformer()

    // Drag-to-draw for area/rectangle
    if (['area', 'rectangle'].includes(activeTool.value)) {
      const pos = getCanvasPoint(e)
      if (!pos) return
      isDrawingShape = true
      drawShapeType = activeTool.value as FloorPlanElementType
      drawStartX = pos.x
      drawStartY = pos.y
      drawPreview.value = { x: pos.x, y: pos.y, w: 0, h: 0 }
      return
    }

    if (activeTool.value !== 'select' && activeTool.value !== 'hand') {
      // Place new element
      const pos = getCanvasPoint(e)
      if (!pos) return
      const el = addElement(activeTool.value as FloorPlanElementType, pos.x, pos.y)
      selectElement(el.id)
      nextTick(() => updateTransformer())
    } else if (activeTool.value === 'select') {
      // Start rubber band selection
      const pos = getCanvasPoint(e)
      if (!pos) return
      isRubberBanding = true
      rbStartX = pos.x
      rbStartY = pos.y
      rubberBand.value = { x: pos.x, y: pos.y, w: 0, h: 0 }
    }
    return
  }
  const clickedId = target.id?.() || target.parent?.id?.()
  if (!clickedId) return
  const foundEl = elements.value.find(el => el.id === clickedId)
  if (!foundEl) return
  if (props.mode === 'view') {
    if ((foundEl.type === 'seat' || foundEl.type === 'seat-desk') && foundEl.positionNo)
      emit('seat-click', foundEl.positionNo)
    return
  }
  const shiftKey = evt.shiftKey
  selectElement(clickedId, shiftKey)
  nextTick(() => updateTransformer())
}

function handleStageMouseMove(e: any) {
  const evt = e.evt || e
  if (!evt) return

  // Pan with middle mouse
  if (isPanning) {
    stageX.value = panStartStageX + (evt.clientX - panStartX)
    stageY.value = panStartStageY + (evt.clientY - panStartY)
    return
  }

  // Drag-to-draw
  if (isDrawingShape && drawPreview.value) {
    const pos = getCanvasPoint(e)
    if (!pos) return
    drawPreview.value = {
      x: Math.min(drawStartX, pos.x),
      y: Math.min(drawStartY, pos.y),
      w: Math.abs(pos.x - drawStartX),
      h: Math.abs(pos.y - drawStartY),
    }
    return
  }

  // Rubber band
  if (isRubberBanding) {
    const pos = getCanvasPoint(e)
    if (!pos) return
    const x = Math.min(rbStartX, pos.x)
    const y = Math.min(rbStartY, pos.y)
    const w = Math.abs(pos.x - rbStartX)
    const h = Math.abs(pos.y - rbStartY)
    rubberBand.value = { x, y, w, h }
  }
}

function handleStageMouseUp(e: any) {
  const evt = e.evt || e
  if (isPanning) { isPanning = false; resetCursor(); return }

  // Finish drag-to-draw
  if (isDrawingShape) {
    const p = drawPreview.value
    if (p && p.w > 20 && p.h > 20) {
      const el = addElement(drawShapeType!, p.x, p.y)
      updateElement(el.id, { width: Math.round(p.w), height: Math.round(p.h) })
      commitUpdate()
      selectElement(el.id)
      nextTick(() => updateTransformer())
    } else {
      // Too small, fall back to click-place
      const pos = getCanvasPoint(e)
      if (pos) {
        const el = addElement(drawShapeType!, pos.x, pos.y)
        selectElement(el.id)
        nextTick(() => updateTransformer())
      }
    }
    isDrawingShape = false
    drawPreview.value = null
    drawShapeType = null
    return
  }

  if (isRubberBanding && rubberBand.value) {
    const rb = rubberBand.value
    if (rb.w > 5 || rb.h > 5) {
      // Select elements intersecting the rubber band
      const newSel = new Set<string>()
      for (const el of elements.value) {
        const ew = el.width || 0, eh = el.height || 0
        const elRight = el.x + ew, elBottom = el.y + eh
        const rbRight = rb.x + rb.w, rbBottom = rb.y + rb.h
        if (el.x < rbRight && elRight > rb.x && el.y < rbBottom && elBottom > rb.y) {
          newSel.add(el.id)
        }
      }
      selectedIds.value = newSel
      nextTick(() => updateTransformer())
    }
    rubberBand.value = null
    isRubberBanding = false
  }
}

function handleElementClick(e: any, el: FloorPlanElement) {
  e.cancelBubble = true
  if (props.mode === 'view') {
    if ((el.type === 'seat' || el.type === 'seat-desk') && el.positionNo)
      emit('seat-click', el.positionNo)
    return
  }
  const shiftKey = (e.evt || e)?.shiftKey
  selectElement(el.id, shiftKey)
  nextTick(() => updateTransformer())
}

let dragRAF: number | null = null
function handleDragMove(e: any, el: FloorPlanElement) {
  const n = e.target
  // Capture values synchronously (Konva may reuse event objects)
  const nx = n.x(), ny = n.y()
  const nw = el.width || 0, nh = el.height || 0
  // Throttle guide computation to animation frames
  if (dragRAF) cancelAnimationFrame(dragRAF)
  dragRAF = requestAnimationFrame(() => {
    dragRAF = null
    const { snapX, snapY, gx, gy } = computeGuides(el.id, nx, ny, nw, nh)
    if (snapX !== null) n.x(snapX)
    if (snapY !== null) n.y(snapY)
    if (gx !== guideLineX.value || gy !== guideLineY.value) updateGuides(gx, gy)
  })
}

function handleDragEnd(e: any, el: FloorPlanElement) {
  clearGuides()
  const n = e.target
  updateElement(el.id, { x: snap(n.x()), y: snap(n.y()) })
  n.x(snap(n.x())); n.y(snap(n.y()))
  commitUpdate()
}

function handleTransformEnd(e: any, el: FloorPlanElement) {
  const n = e.target
  const sx = n.scaleX(), sy = n.scaleY()
  n.scaleX(1); n.scaleY(1)
  const newW = Math.max(10, Math.round((el.width || 50) * sx))
  const newH = Math.max(5, Math.round((el.height || 40) * sy))
  const updates: Partial<FloorPlanElement> = {
    x: snap(n.x()), y: snap(n.y()), rotation: Math.round(n.rotation()),
  }
  if (el.type === 'circle-shape' || (el.type === 'seat' && (!el.seatShape || el.seatShape === 'circle'))) {
    const d = Math.max(newW, newH)
    updates.width = d; updates.height = d
  } else {
    updates.width = newW; updates.height = newH
  }
  updateElement(el.id, updates)
  commitUpdate()
}

function handleWheel(e: WheelEvent) {
  const stage = stageRef.value?.getStage()
  if (!stage) return
  const pointer = stage.getPointerPosition()
  if (!pointer) return
  const old = scale.value
  const by = 1.08
  const ns = e.deltaY < 0 ? Math.min(old * by, 5) : Math.max(old / by, 0.2)
  const mp = { x: (pointer.x - stageX.value) / old, y: (pointer.y - stageY.value) / old }
  scale.value = ns
  stageX.value = pointer.x - mp.x * ns
  stageY.value = pointer.y - mp.y * ns
}

// ===== Zoom =====
function zoomIn() { scale.value = Math.min(scale.value * 1.2, 5) }
function zoomOut() { scale.value = Math.max(scale.value / 1.2, 0.2) }
function resetZoom() { scale.value = 1; stageX.value = 0; stageY.value = 0 }

// ===== Transformer =====
function updateTransformer() {
  const tr = transformerRef.value?.getNode()
  if (!tr) return
  const stage = stageRef.value?.getStage()
  if (!stage) return
  if (selectedIds.value.size === 0) { tr.nodes([]); return }
  tr.nodes([...selectedIds.value].map(id => stage.findOne('#' + id)).filter(Boolean))
}

// ===== Font family options =====
const fontFamilyOptions = [
  { value: '', label: '默认' },
  { value: 'Microsoft YaHei', label: '微软雅黑' },
  { value: 'SimSun', label: '宋体' },
  { value: 'SimHei', label: '黑体' },
  { value: 'KaiTi', label: '楷体' },
  { value: 'FangSong', label: '仿宋' },
  { value: 'Arial', label: 'Arial' },
  { value: 'Helvetica', label: 'Helvetica' },
  { value: 'monospace', label: '等宽' },
]
function onFontFamily(e: Event) {
  if (!selectedElement.value) return
  const val = (e.target as HTMLSelectElement).value
  updateElement(selectedElement.value.id, { fontFamily: val || undefined })
  commitUpdate()
}
function onMultiFontFamily(e: Event) {
  const val = (e.target as HTMLSelectElement).value
  for (const el of selectedElements.value) {
    updateElement(el.id, { fontFamily: val || undefined })
  }
  commitUpdate()
}

// ===== Property panel =====
function onProp(prop: string, e: Event) {
  if (!selectedElement.value) return
  let value: any = (e.target as HTMLInputElement).value
  if (['x','y','width','height','rotation','fontSize','radius'].includes(prop)) value = Number(value)
  updateElement(selectedElement.value.id, { [prop]: value })
  commitUpdate()
  nextTick(() => updateTransformer())
}
function onDiameter(e: Event) {
  if (!selectedElement.value) return
  const d = Number((e.target as HTMLInputElement).value)
  updateElement(selectedElement.value.id, { width: d, height: d })
  commitUpdate()
  nextTick(() => updateTransformer())
}
function onColor(e: Event) {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { fillColor: (e.target as HTMLInputElement).value })
  commitUpdate()
}
function resetColor() {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { fillColor: undefined })
  commitUpdate()
}
function onOpacity(e: Event) {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { opacity: parseFloat((e.target as HTMLInputElement).value) })
  commitUpdate()
}
function onSeatShape(shape: 'circle' | 'square' | 'rounded') {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { seatShape: shape === 'circle' ? undefined : shape })
  commitUpdate()
}

// ===== Seat type helper =====
function isSeatType(type: string): boolean {
  return type === 'seat' || type === 'seat-desk'
}

// ===== Occupied color (seat only) =====
function onOccupiedColor(e: Event) {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { occupiedColor: (e.target as HTMLInputElement).value })
  commitUpdate()
}
function resetOccupiedColor() {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { occupiedColor: undefined })
  commitUpdate()
}
function onOccupiedTextColor(e: Event) {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { occupiedTextColor: (e.target as HTMLInputElement).value })
  commitUpdate()
}
function resetOccupiedTextColor() {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { occupiedTextColor: undefined })
  commitUpdate()
}
function onOccupiedStrokeColor(e: Event) {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { occupiedStrokeColor: (e.target as HTMLInputElement).value })
  commitUpdate()
}
function resetOccupiedStrokeColor() {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { occupiedStrokeColor: undefined })
  commitUpdate()
}

// ===== Stroke & Text color =====
function hasStroke(type: string): boolean {
  return ['desk', 'partition', 'podium', 'blackboard', 'window', 'rectangle', 'circle-shape', 'round-table', 'area'].includes(type)
}
function defaultStroke(type: string): string {
  const map: Record<string, string> = {
    desk: '#D4B896', partition: '#94A3B8', podium: '#C4A87C', blackboard: '#6B5B3E',
    window: '#B4D5E8', rectangle: '#9CA3AF', 'circle-shape': '#93C5FD', 'round-table': '#C4A87C', area: '#93C5FD',
  }
  return map[type] || '#9CA3AF'
}
function hasTextColor(el: FloorPlanElement): boolean {
  if (el.type === 'text') return true
  return ['desk', 'podium', 'blackboard', 'rectangle', 'area'].includes(el.type) // elements with labels
}
function defaultTextColor(el: FloorPlanElement): string {
  const map: Record<string, string> = {
    text: '#374151', desk: '#8B6914', podium: '#8B7355', blackboard: '#808080',
    rectangle: '#374151', area: '#6B7280',
  }
  return map[el.type] || '#374151'
}
function onStrokeColor(e: Event) {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { strokeColor: (e.target as HTMLInputElement).value })
  commitUpdate()
}
function resetStrokeColor() {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { strokeColor: undefined })
  commitUpdate()
}
function onTextColor(e: Event) {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { textColor: (e.target as HTMLInputElement).value })
  commitUpdate()
}
function resetTextColor() {
  if (!selectedElement.value) return
  updateElement(selectedElement.value.id, { textColor: undefined })
  commitUpdate()
}

// ===== Multi-selection property helpers =====
const multiFillColor = computed(() => {
  const sel = selectedElements.value
  if (sel.length < 2) return null
  const colors = sel.map(e => e.fillColor).filter(Boolean)
  if (colors.length === 0) return null
  return colors.every(c => c === colors[0]) ? colors[0]! : null
})
const multiStrokeColor = computed(() => {
  const sel = selectedElements.value.filter(e => hasStroke(e.type))
  if (sel.length === 0) return null
  const colors = sel.map(e => e.strokeColor).filter(Boolean)
  if (colors.length === 0) return null
  return colors.every(c => c === colors[0]) ? colors[0]! : null
})
const multiTextColor = computed(() => {
  const sel = selectedElements.value.filter(e => hasTextColor(e))
  if (sel.length === 0) return null
  const colors = sel.map(e => e.textColor).filter(Boolean)
  if (colors.length === 0) return null
  return colors.every(c => c === colors[0]) ? colors[0]! : null
})
const multiOpacity = computed(() => {
  const sel = selectedElements.value
  if (sel.length === 0) return 1
  const first = sel[0].opacity ?? 1
  return sel.every(e => (e.opacity ?? 1) === first) ? first : 1
})
const multiHasStroke = computed(() => selectedElements.value.some(e => hasStroke(e.type)))
const multiHasText = computed(() => selectedElements.value.some(e => hasTextColor(e)))
const multiHasSeats = computed(() => selectedElements.value.some(e => isSeatType(e.type)))
const multiOccupiedColor = computed(() => {
  const sel = selectedElements.value.filter(e => isSeatType(e.type))
  if (sel.length === 0) return null
  const colors = sel.map(e => e.occupiedColor).filter(Boolean)
  if (colors.length === 0) return null
  return colors.every(c => c === colors[0]) ? colors[0]! : null
})
const multiOccupiedTextColor = computed(() => {
  const sel = selectedElements.value.filter(e => isSeatType(e.type))
  if (sel.length === 0) return null
  const colors = sel.map(e => e.occupiedTextColor).filter(Boolean)
  if (colors.length === 0) return null
  return colors.every(c => c === colors[0]) ? colors[0]! : null
})
const multiOccupiedStrokeColor = computed(() => {
  const sel = selectedElements.value.filter(e => isSeatType(e.type))
  if (sel.length === 0) return null
  const colors = sel.map(e => e.occupiedStrokeColor).filter(Boolean)
  if (colors.length === 0) return null
  return colors.every(c => c === colors[0]) ? colors[0]! : null
})
const multiFontSize = computed(() => {
  const sel = selectedElements.value
  if (sel.length === 0) return 10
  const sizes = sel.map(e => e.fontSize || 0).filter(Boolean)
  if (sizes.length === 0) return 10
  return sizes.every(s => s === sizes[0]) ? sizes[0] : 10
})
const multiFontFamily = computed(() => {
  const sel = selectedElements.value
  if (sel.length === 0) return ''
  const families = sel.map(e => e.fontFamily || '')
  return families.every(f => f === families[0]) ? families[0] : ''
})

function onMultiFontSize(e: Event) {
  const val = Number((e.target as HTMLInputElement).value)
  if (!val || val < 6) return
  for (const el of selectedElements.value) {
    updateElement(el.id, { fontSize: val })
  }
  commitUpdate()
}

type ColorProp = 'fillColor' | 'strokeColor' | 'textColor' | 'occupiedColor' | 'occupiedTextColor' | 'occupiedStrokeColor'
function onMultiColor(prop: ColorProp, e: Event) {
  const val = (e.target as HTMLInputElement).value
  for (const el of selectedElements.value) {
    if (prop === 'strokeColor' && !hasStroke(el.type)) continue
    if (prop === 'textColor' && !hasTextColor(el) && !isSeatType(el.type)) continue
    if (prop === 'occupiedColor' && !isSeatType(el.type)) continue
    if (prop === 'occupiedTextColor' && !isSeatType(el.type)) continue
    if (prop === 'occupiedStrokeColor' && !isSeatType(el.type)) continue
    updateElement(el.id, { [prop]: val })
  }
  commitUpdate()
}
function onMultiResetColor(prop: ColorProp) {
  for (const el of selectedElements.value) {
    updateElement(el.id, { [prop]: undefined })
  }
  commitUpdate()
}
function onMultiOpacity(e: Event) {
  const val = parseFloat((e.target as HTMLInputElement).value)
  for (const el of selectedElements.value) {
    updateElement(el.id, { opacity: val })
  }
  commitUpdate()
}

// ===== Batch dialog =====
const showBatchDialog = ref(false)
const batchForm = reactive({
  rows: 5,
  cols: 8,
  startLabel: 'A',
  startNumber: 1,
  seatShape: 'rounded' as 'circle' | 'square' | 'rounded',
  charCols: 3,
  charRows: 2,
  circleDiameter: 30,
  columnGap: 10,
  rowGap: 10,
  aisleAfterCol: 0,
  aisleWidth: 30,
  numberingScope: 'global' as 'global' | 'per-row',
  numberingDir: 'ltr' as 'ltr' | 'rtl' | 'snake' | 'column' | 'center',
  staggered: false,
})
watch(() => batchForm.numberingScope, (scope) => {
  const d = batchForm.numberingDir
  if (scope === 'per-row' && (d === 'snake' || d === 'column')) batchForm.numberingDir = 'ltr'
  if (scope === 'global' && d === 'center') batchForm.numberingDir = 'ltr'
})
const effectiveNumberingOrder = computed(() => {
  if (batchForm.numberingScope === 'per-row') {
    if (batchForm.numberingDir === 'center') return 'row-center'
    if (batchForm.numberingDir === 'rtl') return 'row-rtl'
    return 'row-ltr'
  }
  return batchForm.numberingDir as string
})
const CHAR_W = 10, LINE_H = 14, PAD = 8
const computedSeatWidth = computed(() =>
  batchForm.seatShape === 'circle' ? batchForm.circleDiameter
    : batchForm.charCols * CHAR_W + PAD
)
const computedSeatHeight = computed(() =>
  batchForm.seatShape === 'circle' ? batchForm.circleDiameter
    : batchForm.charRows * LINE_H + PAD
)
const batchTotalSeats = computed(() => batchForm.rows * batchForm.cols)
const previewSeats = computed(() =>
  generateSeatGridPreview(batchForm.rows, batchForm.cols, {
    hSpacing: batchForm.columnGap,
    vSpacing: batchForm.rowGap,
    seatWidth: computedSeatWidth.value,
    seatHeight: computedSeatHeight.value,
    startNumber: batchForm.startNumber,
    numberingOrder: effectiveNumberingOrder.value as any,
    seatShape: batchForm.seatShape,
    aisleAfterCol: batchForm.aisleAfterCol,
    aisleWidth: batchForm.aisleWidth,
    staggered: batchForm.staggered,
  })
)
const previewContainerStyle = computed(() => {
  const seats = previewSeats.value
  if (seats.length === 0) return {}
  const maxX = Math.max(...seats.map(s => s.x + s.w))
  const maxY = Math.max(...seats.map(s => s.y + s.h))
  const areaW = 340, areaH = 360
  const sc = Math.min(1, areaW / maxX, areaH / maxY)
  return {
    width: Math.round(maxX * sc) + 'px',
    height: Math.round(maxY * sc) + 'px',
    transform: `scale(${sc})`,
    transformOrigin: 'top left',
  }
})
const previewRawSize = computed(() => {
  const seats = previewSeats.value
  if (seats.length === 0) return { w: 0, h: 0 }
  return {
    w: Math.max(...seats.map(s => s.x + s.w)),
    h: Math.max(...seats.map(s => s.y + s.h)),
  }
})
function doBatchAdd() {
  // When both gaps are 0, force square shape for table-like appearance
  const tightPack = batchForm.columnGap <= 0 && batchForm.rowGap <= 0
  const shape = tightPack ? 'square' : batchForm.seatShape
  addSeatGrid(batchForm.rows, batchForm.cols, 100, 100, {
    hSpacing: batchForm.columnGap,
    vSpacing: batchForm.rowGap,
    seatWidth: computedSeatWidth.value,
    seatHeight: computedSeatHeight.value,
    startLabel: batchForm.startLabel,
    startNumber: batchForm.startNumber,
    numberingOrder: effectiveNumberingOrder.value as any,
    seatShape: shape,
    aisleAfterCol: batchForm.aisleAfterCol,
    aisleWidth: batchForm.aisleWidth,
    staggered: batchForm.staggered,
  })
  showBatchDialog.value = false
}

// ===== Template dialog =====
const templates = TEMPLATES
const showTemplateDialog = ref(false)
const selectedTplId = ref<string | null>(null)
function doApplyTemplate() {
  const tpl = templates.find(t => t.id === selectedTplId.value)
  if (!tpl) return
  applyTemplate(tpl)
  showTemplateDialog.value = false
  selectedTplId.value = null
}

// ===== Keyboard =====
function handleKeyDown(e: KeyboardEvent) {
  if (props.mode === 'view') return
  // Space = pan mode (works even in input fields)
  if (e.key === ' ' && !e.ctrlKey && !e.metaKey) {
    const tag = (e.target as HTMLElement).tagName
    if (tag !== 'INPUT' && tag !== 'TEXTAREA') {
      e.preventDefault()
      if (!isSpacePressed.value) { isSpacePressed.value = true; resetCursor() }
      return
    }
  }
  const tag = (e.target as HTMLElement).tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
  const ctrl = e.ctrlKey || e.metaKey
  if ((e.key === 'Delete' || e.key === 'Backspace') && selectedIds.value.size > 0) { e.preventDefault(); deleteSelected(); updateTransformer() }
  if (ctrl && e.key === 'z' && !e.shiftKey) { e.preventDefault(); undo(); updateTransformer() }
  if (ctrl && (e.key === 'y' || (e.shiftKey && e.key === 'Z'))) { e.preventDefault(); redo(); updateTransformer() }
  if (ctrl && e.key === 'c') { e.preventDefault(); copySelected() }
  if (ctrl && e.key === 'x') { e.preventDefault(); cutSelected(); updateTransformer() }
  if (ctrl && e.key === 'v') { e.preventDefault(); pasteClipboard(); nextTick(() => updateTransformer()) }
  if (ctrl && e.key === 'd') { e.preventDefault(); duplicateSelected(); nextTick(() => updateTransformer()) }
  if (ctrl && e.key === 'a') { e.preventDefault(); selectAll(); nextTick(() => updateTransformer()) }
  if (e.key === 'Escape') { e.preventDefault(); clearSelection(); activeTool.value = 'select'; updateTransformer() }
  if (e.key === 'v' && !ctrl) activeTool.value = 'select'
  if (e.key === 'h' && !ctrl) activeTool.value = 'hand'
}
function handleKeyUp(e: KeyboardEvent) {
  if (e.key === ' ') { isSpacePressed.value = false; resetCursor() }
}

// ===== Lifecycle =====
onMounted(() => {
  loadLayout(props.initialLayout)
  document.addEventListener('keydown', handleKeyDown)
  document.addEventListener('keyup', handleKeyUp)
  document.addEventListener('click', handleDocClick)
  if (canvasContainerRef.value) {
    containerWidth.value = canvasContainerRef.value.clientWidth
    containerHeight.value = canvasContainerRef.value.clientHeight
    resizeObs = new ResizeObserver(entries => {
      for (const entry of entries) {
        containerWidth.value = entry.contentRect.width
        containerHeight.value = entry.contentRect.height
      }
    })
    resizeObs.observe(canvasContainerRef.value)
  }
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeyDown)
  document.removeEventListener('keyup', handleKeyUp)
  document.removeEventListener('click', handleDocClick)
  resizeObs?.disconnect()
})
watch(selectedIds, () => nextTick(() => updateTransformer()), { deep: true })
defineExpose({
  getLayout: toLayout,
  getStageTransform: () => ({ scale: scale.value, x: stageX.value, y: stageY.value }),
  getCanvasContainer: () => canvasContainerRef.value,
})
</script>

<style scoped>
.fp-editor { display: flex; flex-direction: column; height: 100%; background: #F5F6F8; overflow: hidden; }
.fp-content { display: flex; flex: 1; min-height: 0; }
.fp-canvas-wrap { flex: 1; display: flex; flex-direction: column; min-width: 0; position: relative; }
.fp-canvas { flex: 1; overflow: hidden; background: #F5F6F8; }

/* Toolbar */
.fp-topbar {
  display: flex; align-items: center; gap: 4px; height: 42px; padding: 0 12px;
  background: rgba(255,255,255,0.92); backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0,0,0,0.06);
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  flex-shrink: 0; overflow: visible; position: relative; z-index: 10;
}
.fp-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border: none; border-radius: 8px;
  background: transparent; color: #4B5563; cursor: pointer; transition: all 0.12s; flex-shrink: 0; position: relative;
}
.fp-btn:hover { background: rgba(0,0,0,0.05); color: #1F2937; }
.fp-btn.active { background: #3B82F6; color: #FFFFFF; box-shadow: 0 1px 3px rgba(59,130,246,0.3); }
.fp-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.fp-btn.fp-btn-danger { color: #DC2626; }
.fp-btn.fp-btn-danger:hover { background: #FEF2F2; }
.fp-btn-dropdown { width: auto; padding: 0 6px; gap: 2px; }
.fp-chevron { opacity: 0.5; flex-shrink: 0; }
.fp-sep { width: 8px; height: 1px; background: transparent; flex-shrink: 0; }
.fp-spacer { flex: 1; }
.fp-zoom-label { font-size: 11px; color: #6B7280; min-width: 38px; text-align: center; user-select: none; font-variant-numeric: tabular-nums; }

/* Tool group dropdown */
.fp-tool-group { position: relative; }
.fp-dropdown {
  position: absolute; top: calc(100% + 4px); left: 0;
  background: #FFFFFF; border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12), 0 1px 3px rgba(0,0,0,0.06);
  padding: 4px; min-width: 140px; z-index: 100;
  animation: fp-dropdown-in 0.12s ease;
}
@keyframes fp-dropdown-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
.fp-dropdown-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; border-radius: 6px; font-size: 12px;
  color: #4B5563; cursor: pointer; transition: background 0.1s;
  border: none; background: transparent; width: 100%;
}
.fp-dropdown-item:hover { background: #F3F4F6; }
.fp-dropdown-item.active { background: #EFF6FF; color: #2563EB; }

/* Panel */
.fp-panel {
  width: 240px; flex-shrink: 0;
  background: rgba(255,255,255,0.95); backdrop-filter: blur(8px);
  border-left: 1px solid rgba(0,0,0,0.06);
  overflow-y: auto; font-size: 12px;
}
.fp-panel-header { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; font-size: 13px; font-weight: 600; color: #1F2937; border-bottom: 1px solid #F3F4F6; }
.fp-section { padding: 8px 14px; border-bottom: 1px solid #F3F4F6; }
.fp-section-title { font-size: 10px; font-weight: 600; color: #9CA3AF; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
.fp-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; }
.fp-field { display: flex; align-items: center; gap: 4px; }
.fp-field.full { width: 100%; }
.fp-field label { font-size: 11px; color: #6B7280; min-width: 24px; flex-shrink: 0; }
.fp-field input[type="number"], .fp-field input[type="text"] {
  flex: 1; min-width: 0; height: 28px; padding: 0 6px; font-size: 12px;
  border: 1px solid #E5E7EB; border-radius: 6px; outline: none; color: #1F2937; background: #FAFBFC;
}
.fp-field input:focus, .fp-field select:focus { border-color: #93C5FD; box-shadow: 0 0 0 2px rgba(59,130,246,0.08); background: #FFFFFF; }
.fp-field input[type="range"] { flex: 1; accent-color: #3B82F6; }
.fp-select {
  flex: 1; min-width: 0; height: 28px; padding: 0 4px; font-size: 12px;
  border: 1px solid #E5E7EB; border-radius: 6px; outline: none; color: #1F2937; background: #FAFBFC;
  cursor: pointer; appearance: auto;
}
.fp-readonly { font-size: 12px; color: #6B7280; }
.fp-color-row { display: flex; align-items: center; gap: 6px; flex: 1; }
.fp-color-swatch {
  width: 24px; height: 24px; border-radius: 50%;
  border: 2px solid #E5E7EB; cursor: pointer;
  position: relative; overflow: hidden; flex-shrink: 0;
  transition: border-color 0.15s;
}
.fp-color-swatch:hover { border-color: #93C5FD; }
.fp-color-swatch input[type="color"] {
  position: absolute; inset: -8px;
  width: 40px; height: 40px; border: none; cursor: pointer;
  opacity: 0;
}
.fp-color-hex { font-size: 10px; color: #9CA3AF; font-family: monospace; min-width: 0; }
.fp-icon-btn {
  display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px;
  border: 1px solid #E5E7EB; border-radius: 6px; background: #FFFFFF; color: #6B7280; cursor: pointer; transition: all 0.12s;
}
.fp-icon-btn:hover { background: #F3F4F6; color: #374151; border-color: #D1D5DB; }
.fp-icon-btn.active { background: #EFF6FF; border-color: #BFDBFE; color: #2563EB; }
.fp-icon-btn-sm {
  display: inline-flex; align-items: center; justify-content: center; width: 20px; height: 20px;
  border: none; border-radius: 4px; background: transparent; color: #9CA3AF; cursor: pointer;
}
.fp-icon-btn-sm:hover { background: #F3F4F6; color: #6B7280; }
.fp-layer-row { display: flex; align-items: center; gap: 4px; }
.fp-radio-group { display: flex; gap: 2px; background: #F3F4F6; border-radius: 8px; padding: 2px; }
.fp-radio-group label { flex: 1; text-align: center; padding: 5px 0; font-size: 12px; color: #6B7280; border-radius: 6px; cursor: pointer; transition: all 0.12s; }
.fp-radio-group label.active { background: #FFFFFF; color: #1F2937; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.fp-radio-group input { display: none; }
.fp-hint { text-align: center; color: #9CA3AF; font-size: 12px; padding: 24px 14px; margin: 0; line-height: 1.6; }

/* Status */
.fp-status {
  display: flex; align-items: center; gap: 10px; height: 24px; padding: 0 14px;
  font-size: 10px; color: #6B7280;
  background: rgba(255,255,255,0.9); border-top: 1px solid rgba(0,0,0,0.04); flex-shrink: 0;
}
.fp-status-sep { color: #D1D5DB; }

/* View mode */
.fp-view-mode .fp-content { flex-direction: column; }
.fp-view-mode .fp-canvas { cursor: default; }

/* Dialogs */
.fp-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 9999; }
.fp-dialog { background: #FFFFFF; border-radius: 14px; box-shadow: 0 24px 48px rgba(0,0,0,0.16); width: 500px; max-height: 80vh; display: flex; flex-direction: column; }
.fp-dialog-head { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid #F3F4F6; }
.fp-dialog-head h3 { font-size: 16px; font-weight: 600; color: #1F2937; margin: 0; }
.fp-dialog-body { padding: 16px 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.fp-dialog-foot { display: flex; align-items: center; justify-content: flex-end; gap: 8px; padding: 12px 20px; border-top: 1px solid #F3F4F6; }

/* Batch form */
.fp-dialog-wide { width: 720px; }
.fp-batch-body { flex-direction: row !important; gap: 0 !important; padding: 0 !important; }
.fp-batch-form { width: 320px; flex-shrink: 0; padding: 16px 20px; overflow-y: auto; max-height: 480px; display: flex; flex-direction: column; gap: 6px; border-right: 1px solid #F3F4F6; }
.fp-batch-preview-area { flex: 1; min-width: 0; padding: 12px 16px; display: flex; flex-direction: column; }
.fp-batch-preview-label { font-size: 11px; font-weight: 600; color: #9CA3AF; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }
.fp-batch-preview-canvas { flex: 1; background: #F9FAFB; border-radius: 8px; overflow: hidden; position: relative; display: flex; align-items: center; justify-content: center; min-height: 200px; padding: 16px; }
.fp-preview-container { position: relative; }
.fp-preview-seat {
  position: absolute; display: flex; align-items: center; justify-content: center;
  font-size: 7px; color: #9CA3AF; border: 1px solid #D4D7DC; background: #FFFFFF;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.fp-preview-seat.circle { border-radius: 50%; }
.fp-preview-seat.square { border-radius: 0; }
.fp-preview-seat.rounded { border-radius: 4px; }
.fp-batch-summary { font-size: 12px; color: #6B7280; display: flex; align-items: center; gap: 8px; }
.fp-batch-summary strong { color: #2563EB; font-size: 15px; }
.fp-batch-dim { color: #9CA3AF; font-size: 11px; }
.fp-checkbox { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #4B5563; cursor: pointer; }
.fp-checkbox input[type="checkbox"] { width: 14px; height: 14px; accent-color: #3B82F6; cursor: pointer; }
.fp-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.fp-form-item { display: flex; flex-direction: column; gap: 4px; }
.fp-form-item label { font-size: 12px; color: #6B7280; font-weight: 500; }
.fp-form-item input { height: 34px; padding: 0 10px; font-size: 13px; border: 1px solid #E5E7EB; border-radius: 8px; outline: none; color: #1F2937; }
.fp-form-item input:focus { border-color: #93C5FD; box-shadow: 0 0 0 2px rgba(59,130,246,0.08); }
.fp-batch-group-label { font-size: 11px; font-weight: 600; color: #9CA3AF; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; margin-top: 4px; }
.fp-radio-sm label { font-size: 11px !important; padding: 4px 0 !important; }

/* Template cards */
.fp-tpl-card { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 2px solid #F3F4F6; border-radius: 10px; cursor: pointer; transition: all 0.12s; }
.fp-tpl-card:hover { border-color: #BFDBFE; background: #FAFBFF; }
.fp-tpl-card.selected { border-color: #3B82F6; background: #EFF6FF; }
.fp-tpl-icon { font-size: 28px; flex-shrink: 0; }
.fp-tpl-info { flex: 1; min-width: 0; }
.fp-tpl-name { font-size: 14px; font-weight: 600; color: #1F2937; }
.fp-tpl-meta { font-size: 12px; color: #9CA3AF; margin-top: 2px; }

/* Buttons */
.fp-btn-secondary, .fp-btn-primary { height: 34px; padding: 0 18px; font-size: 13px; border-radius: 8px; cursor: pointer; transition: all 0.12s; font-weight: 500; }
.fp-btn-secondary { background: #FFFFFF; border: 1px solid #E5E7EB; color: #374151; }
.fp-btn-secondary:hover { background: #F9FAFB; border-color: #D1D5DB; }
.fp-btn-primary { background: #3B82F6; border: none; color: #FFFFFF; }
.fp-btn-primary:hover { background: #2563EB; }
.fp-btn-primary:disabled { background: #93C5FD; cursor: not-allowed; }

.fp-fade-enter-active, .fp-fade-leave-active { transition: opacity 0.15s; }
.fp-fade-enter-from, .fp-fade-leave-to { opacity: 0; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); border: 0; }
</style>
