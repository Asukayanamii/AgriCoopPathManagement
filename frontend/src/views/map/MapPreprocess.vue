<template>
  <div class="page">
    <!-- Top Toolbar -->
    <div class="toolbar">
      <el-select v-model="selectedMapId" placeholder="选择已上传的地图" style="width:240px" @change="onMapSelect" clearable>
        <el-option v-for="m in mapList" :key="m.id" :label="m.originName" :value="m.id" />
      </el-select>

      <el-upload :http-request="handleUpload" accept=".png" :show-file-list="false" :disabled="uploading">
        <el-button type="primary" :icon="Upload" :loading="uploading">上传地图</el-button>
      </el-upload>

      <span class="sep" />
      <span style="font-size:13px;color:#666">栅格:</span>
      <el-select v-model="gridCells" style="width:100px" @change="clearPath">
        <el-option :value="10" label="10×10" />
        <el-option :value="20" label="20×20" />
        <el-option :value="25" label="25×25" />
        <el-option :value="50" label="50×50" />
      </el-select>

      <span v-if="mapInfo" class="map-info">{{ mapInfo }}</span>
    </div>

    <div class="content">
      <!-- SVG Canvas -->
      <div class="canvas-wrap" ref="canvasWrapRef">
        <svg v-if="mapImageUrl"
          :viewBox="`0 0 ${viewBoxW} ${viewBoxH}`" class="svg"
          ref="svgRef"
          @mousedown="onSvgMouseDown"
          @mouseup="onSvgMouseUp"
          @mousemove="onSvgMousemove"
          @mouseleave="onSvgMouseleave">
          <!-- Map background image (fill viewBox, same aspect ratio so no distortion) -->
          <image :href="mapImageUrl" x="0" y="0" :width="viewBoxW" :height="viewBoxH"
            preserveAspectRatio="none" />

          <!-- Grid lines -->
          <template v-for="i in gridCells" :key="'gl' + i">
            <line :x1="i * cellW" y1="0" :x2="i * cellW" :y2="viewBoxH"
              stroke="rgba(0,0,0,0.1)" stroke-width="0.5" />
            <line x1="0" :y1="i * cellH" :x2="viewBoxW" :y2="i * cellH"
              stroke="rgba(0,0,0,0.1)" stroke-width="0.5" />
          </template>

          <!-- Hover highlight -->
          <rect v-if="hoverCell && mapImageUrl"
            :x="hoverCell.x - cellW / 2" :y="hoverCell.y - cellH / 2"
            :width="cellW" :height="cellH"
            fill="rgba(0,0,0,0.12)" stroke="rgba(0,0,0,0.25)" stroke-width="1" stroke-dasharray="3,2" />

          <!-- Drag selection preview -->
          <rect v-if="isDragging && dragStartCell && dragEndCell"
            :x="Math.min(dragStartCell.x, dragEndCell.x) - cellW / 2"
            :y="Math.min(dragStartCell.y, dragEndCell.y) - cellH / 2"
            :width="Math.abs(dragEndCell.x - dragStartCell.x) + cellW"
            :height="Math.abs(dragEndCell.y - dragStartCell.y) + cellH"
            :fill="tool === 'obstacle' ? 'rgba(231,76,60,0.18)' : 'rgba(0,0,0,0.10)'"
            :stroke="tool === 'obstacle' ? '#e74c3c' : '#666'"
            stroke-width="1.5" stroke-dasharray="4,2" />

          <!-- Obstacles -->
          <rect v-for="(o, i) in obstacles" :key="'ob' + i"
            :x="o.x - cellW / 2" :y="o.y - cellH / 2"
            :width="cellW" :height="cellH"
            fill="#e74c3c" opacity="0.75" stroke="#c0392b" stroke-width="1" rx="2" />

          <!-- Start point -->
          <g v-if="startPoint">
            <rect :x="startPoint.x - cellW / 2" :y="startPoint.y - cellH / 2"
              :width="cellW" :height="cellH"
              fill="#2ecc71" opacity="0.85" stroke="#27ae60" stroke-width="2" rx="3" />
            <text :x="startPoint.x" :y="startPoint.y + cellH * 0.3" text-anchor="middle"
              font-size="11" fill="#fff" font-weight="bold">起</text>
          </g>

          <!-- End point -->
          <g v-if="endPoint">
            <rect :x="endPoint.x - cellW / 2" :y="endPoint.y - cellH / 2"
              :width="cellW" :height="cellH"
              fill="#3498db" opacity="0.85" stroke="#2980b9" stroke-width="2" rx="3" />
            <text :x="endPoint.x" :y="endPoint.y + cellH * 0.3" text-anchor="middle"
              font-size="11" fill="#fff" font-weight="bold">终</text>
          </g>

          <!-- Path result -->
          <polyline v-if="pathPoints.length" :points="pathStr"
            fill="none" stroke="#f39c12" stroke-width="3.5"
            stroke-linejoin="round" stroke-linecap="round" opacity="0.9" />
          <circle v-for="(p, i) in pathPoints" :key="'pp' + i"
            :cx="p[0] / 1000 * viewBoxW" :cy="p[1] / 1000 * viewBoxH"
            r="2.5" fill="#f39c12" stroke="#fff" stroke-width="1" />
        </svg>

        <div v-else class="empty-state">
          <el-icon :size="48"><Picture /></el-icon>
          <p>请上传或选择一张 PNG 地图</p>
        </div>
      </div>

      <!-- Tool Panel -->
      <div class="tool-panel">
        <h3>标记工具</h3>
        <el-radio-group v-model="tool" class="tools">
          <el-radio-button value="start">
            <el-icon style="vertical-align:-2px"><Pointer /></el-icon> 起点
          </el-radio-button>
          <el-radio-button value="end">
            <el-icon style="vertical-align:-2px"><Pointer /></el-icon> 终点
          </el-radio-button>
          <el-radio-button value="obstacle">
            <el-icon style="vertical-align:-2px"><CircleCloseFilled /></el-icon> 障碍
          </el-radio-button>
          <el-radio-button value="eraser">
            <el-icon style="vertical-align:-2px"><Delete /></el-icon> 擦除
          </el-radio-button>
        </el-radio-group>

        <el-divider />
        <h4>统计</h4>
        <div class="stat"><span>起点</span><span :style="{color:'#27ae60',fontWeight:600}">{{ startPoint ? '已设置' : '—' }}</span></div>
        <div class="stat"><span>终点</span><span :style="{color:'#2980b9',fontWeight:600}">{{ endPoint ? '已设置' : '—' }}</span></div>
        <div class="stat"><span>障碍物</span><span style="fontWeight:600">{{ obstacles.length }}</span></div>

        <el-divider />
        <h4>当前网格</h4>
        <div class="coord-info" v-if="hoverCell">
          <span>网格: ({{ hoverCell.col }}, {{ hoverCell.row }})</span>
          <span>坐标: {{ hoverCell.x }}, {{ hoverCell.y }}</span>
        </div>
        <div class="coord-info dim" v-else>悬停在地图上查看</div>

        <el-divider />
        <div class="btn-group">
          <el-button type="danger" @click="clearAll" :icon="Delete" plain style="width:100%">清空</el-button>
          <el-button type="success" @click="runPathPlan" :icon="VideoPlay" :loading="loading"
            :disabled="!canPlan" style="width:100%;margin-top:6px">执行路径规划</el-button>
        </div>
      </div>
    </div>

    <!-- Result Bar -->
    <transition name="fade">
      <div v-if="result" class="result-bar">
        <el-icon color="#f39c12" :size="18"><Link /></el-icon>
        <span>路径长度: <b>{{ result.distance }}</b></span>
        <span>路径点数: <b>{{ result.pathPoints }}</b></span>
        <el-tag size="small" type="success">规划完成</el-tag>
        <el-button size="small" text @click="clearPath">关闭</el-button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Picture, Delete, VideoPlay, Pointer, CircleCloseFilled, Link } from '@element-plus/icons-vue'
import { uploadMapImage, getMapImageList, getMapImageUrl } from '@/api/map'
import { pathPlanning } from '@/api/algorithm'

// --- Map state ---
const mapList = ref([])
const selectedMapId = ref(null)
const mapImageUrl = ref(null)
const mapInfo = ref('')
const uploading = ref(false)

// --- Grid state ---
const gridCells = ref(20)
const imgNatural = ref({ w: 1000, h: 1000 })
const viewBoxW = computed(() => {
  const { w, h } = imgNatural.value
  return w >= h ? 1000 : Math.round(1000 * w / h)
})
const viewBoxH = computed(() => {
  const { w, h } = imgNatural.value
  return w >= h ? Math.round(1000 * h / w) : 1000
})
const cellW = computed(() => viewBoxW.value / gridCells.value)
const cellH = computed(() => viewBoxH.value / gridCells.value)

// --- Markers ---
const tool = ref('obstacle')
const startPoint = ref(null)
const endPoint = ref(null)
const obstacles = ref([])

// --- Interaction ---
const svgRef = ref(null)
const hoverCell = ref(null)
const isDragging = ref(false)
const dragMoved = ref(false)
const dragStartCell = ref(null)
const dragEndCell = ref(null)

// --- Path result ---
const loading = ref(false)
const pathPoints = ref([])
const result = ref(null)

const pathStr = computed(() => {
  const vw = viewBoxW.value, vh = viewBoxH.value
  return pathPoints.value.map(p => {
    const vx = p[0] / 1000 * vw
    const vy = p[1] / 1000 * vh
    return `${vx},${vy}`
  }).join(' ')
})

const canPlan = computed(() =>
  startPoint.value && endPoint.value && !loading.value
)

// --- Lifecycle ---
onMounted(() => loadMapList())

// --- Map operations ---
async function loadMapList() {
  try {
    const res = await getMapImageList()
    if (res.code === 1) mapList.value = res.data || []
  } catch (_) { /* ignore */ }
}

function onMapSelect(id) {
  clearAll()
  if (!id) { mapImageUrl.value = null; mapInfo.value = ''; return }
  const map = mapList.value.find(m => m.id === id)
  if (map) {
    mapImageUrl.value = getMapImageUrl(id)
    imgNatural.value = { w: map.width, h: map.height }
    mapInfo.value = `${map.width}×${map.height}px`
  }
}

async function handleUpload({ file }) {
  uploading.value = true
  try {
    const res = await uploadMapImage(file)
    if (res.code === 1) {
      ElMessage.success('上传成功')
      await loadMapList()
      selectedMapId.value = res.data.id
      onMapSelect(res.data.id)
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败: ' + (e.message || ''))
  } finally {
    uploading.value = false
  }
}

// --- SVG interaction ---
function getGridCell(clientX, clientY) {
  const svg = svgRef.value
  if (!svg) return null
  const rect = svg.getBoundingClientRect()
  const vw = viewBoxW.value, vh = viewBoxH.value
  const cw = cellW.value, ch = cellH.value
  const scale = Math.min(rect.width / vw, rect.height / vh)
  const renderW = vw * scale, renderH = vh * scale
  const offsetX = (rect.width - renderW) / 2
  const offsetY = (rect.height - renderH) / 2
  const svgX = (clientX - rect.left - offsetX) / scale
  const svgY = (clientY - rect.top - offsetY) / scale
  const col = Math.floor(svgX / cw)
  const row = Math.floor(svgY / ch)
  if (col < 0 || col >= gridCells.value || row < 0 || row >= gridCells.value) return null
  return {
    col, row,
    x: Math.round(col * cw + cw / 2),
    y: Math.round(row * ch + ch / 2)
  }
}

function onSvgMouseDown(event) {
  const cell = getGridCell(event.clientX, event.clientY)
  if (!cell) return
  if (tool.value !== 'obstacle' && tool.value !== 'eraser') return

  isDragging.value = true
  dragMoved.value = false
  dragStartCell.value = { x: cell.x, y: cell.y, col: cell.col, row: cell.row }
  dragEndCell.value = { x: cell.x, y: cell.y, col: cell.col, row: cell.row }
}

function onSvgMouseUp(event) {
  if (isDragging.value && dragMoved.value) {
    fillDragRect(tool.value)
    clearPath()
  } else {
    // Single click
    const cell = getGridCell(event.clientX, event.clientY)
    if (cell) handleCellClick(cell)
  }

  isDragging.value = false
  dragMoved.value = false
  dragStartCell.value = null
  dragEndCell.value = null
}

function handleCellClick(cell) {
  switch (tool.value) {
    case 'start': {
      obstacles.value = obstacles.value.filter(o => o.x !== cell.x || o.y !== cell.y)
      if (endPoint.value && cell.x === endPoint.value.x && cell.y === endPoint.value.y) endPoint.value = null
      startPoint.value = { x: cell.x, y: cell.y }
      break
    }
    case 'end': {
      obstacles.value = obstacles.value.filter(o => o.x !== cell.x || o.y !== cell.y)
      if (startPoint.value && cell.x === startPoint.value.x && cell.y === startPoint.value.y) startPoint.value = null
      endPoint.value = { x: cell.x, y: cell.y }
      break
    }
    case 'obstacle': {
      if (startPoint.value && cell.x === startPoint.value.x && cell.y === startPoint.value.y) return
      if (endPoint.value && cell.x === endPoint.value.x && cell.y === endPoint.value.y) return
      if (!obstacles.value.some(o => o.x === cell.x && o.y === cell.y)) {
        obstacles.value.push({ x: cell.x, y: cell.y })
      }
      break
    }
    case 'eraser': {
      obstacles.value = obstacles.value.filter(o => o.x !== cell.x || o.y !== cell.y)
      if (startPoint.value && cell.x === startPoint.value.x && cell.y === startPoint.value.y) startPoint.value = null
      if (endPoint.value && cell.x === endPoint.value.x && cell.y === endPoint.value.y) endPoint.value = null
      break
    }
  }
  clearPath()
}

function onSvgMousemove(event) {
  const cell = getGridCell(event.clientX, event.clientY)
  hoverCell.value = cell

  if (isDragging.value && cell && dragStartCell.value) {
    if (cell.col !== dragStartCell.value.col || cell.row !== dragStartCell.value.row) {
      dragMoved.value = true
    }
    dragEndCell.value = { x: cell.x, y: cell.y, col: cell.col, row: cell.row }
  }
}

function onSvgMouseleave() {
  hoverCell.value = null
  if (isDragging.value) {
    isDragging.value = false
    dragMoved.value = false
    dragStartCell.value = null
    dragEndCell.value = null
  }
}

// --- Path planning ---
async function runPathPlan() {
  if (!canPlan.value) return
  loading.value = true
  const vw = viewBoxW.value, vh = viewBoxH.value
  try {
    const res = await pathPlanning({
      mapWidth: 1000,
      mapHeight: 1000,
      startX: Math.round(startPoint.value.x / vw * 1000),
      startY: Math.round(startPoint.value.y / vh * 1000),
      endX: Math.round(endPoint.value.x / vw * 1000),
      endY: Math.round(endPoint.value.y / vh * 1000),
      obstacles: obstacles.value.map(o => [
        Math.round(o.x / vw * 1000),
        Math.round(o.y / vh * 1000)
      ]),
      gridResolution: Math.round(1000 / gridCells.value)
    })
    if (res.code === 1) {
      pathPoints.value = res.data.path || []
      result.value = {
        distance: res.data.distance,
        pathPoints: res.data.pathPoints
      }
      ElMessage.success('路径规划完成')
    } else {
      ElMessage.error(res.msg || '路径规划失败')
    }
  } catch (e) {
    ElMessage.error('路径规划失败: ' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

// --- Drag fill ---
function fillDragRect(mode) {
  if (!dragStartCell.value || !dragEndCell.value) return
  const minCol = Math.min(dragStartCell.value.col, dragEndCell.value.col)
  const maxCol = Math.max(dragStartCell.value.col, dragEndCell.value.col)
  const minRow = Math.min(dragStartCell.value.row, dragEndCell.value.row)
  const maxRow = Math.max(dragStartCell.value.row, dragEndCell.value.row)
  const cw = cellW.value, ch = cellH.value

  for (let r = minRow; r <= maxRow; r++) {
    for (let c = minCol; c <= maxCol; c++) {
      const x = Math.round(c * cw + cw / 2)
      const y = Math.round(r * ch + ch / 2)

      if (mode === 'obstacle') {
        if (startPoint.value && startPoint.value.x === x && startPoint.value.y === y) continue
        if (endPoint.value && endPoint.value.x === x && endPoint.value.y === y) continue
        if (!obstacles.value.some(o => o.x === x && o.y === y)) {
          obstacles.value.push({ x, y })
        }
      } else if (mode === 'eraser') {
        obstacles.value = obstacles.value.filter(o => o.x !== x || o.y !== y)
        if (startPoint.value && startPoint.value.x === x && startPoint.value.y === y) startPoint.value = null
        if (endPoint.value && endPoint.value.x === x && endPoint.value.y === y) endPoint.value = null
      }
    }
  }
}

// --- Clear ---
function clearAll() {
  startPoint.value = null
  endPoint.value = null
  obstacles.value = []
  clearPath()
}

function clearPath() {
  pathPoints.value = []
  result.value = null
}
</script>

<style scoped>
.page { height: 100%; display: flex; flex-direction: column; gap: 12px; }
.toolbar { display: flex; align-items: center; gap: 10px; background: #fff; border-radius: 8px; padding: 10px 16px; flex-wrap: wrap; }
.sep { width: 1px; height: 24px; background: #e5e7eb; }
.map-info { margin-left: auto; font-size: 12px; color: #999; }

.content { flex: 1; display: flex; gap: 12px; overflow: hidden; }
.canvas-wrap { flex: 1; background: #fff; border-radius: 8px; display: flex; align-items: center; justify-content: center; overflow: hidden; position: relative; }
.svg { width: 100%; height: 100%; cursor: crosshair; display: block; }

.tool-panel { width: 220px; background: #fff; border-radius: 8px; padding: 16px; overflow-y: auto; flex-shrink: 0; }
.tool-panel h3 { margin: 0 0 10px; font-size: 15px; color: #1a202c; }
.tool-panel h4 { margin: 0 0 6px; font-size: 13px; color: #555; }
.tools { display: flex; flex-direction: column; gap: 4px; }
.tools :deep(.el-radio-button) { width: 100%; }
.tools :deep(.el-radio-button__inner) { width: 100%; justify-content: flex-start; border-radius: 6px !important; border: 1px solid #e5e7eb; }
.tools :deep(.el-radio-button__original:checked + .el-radio-button__inner) { border-color: #409eff; }
.stat { display: flex; justify-content: space-between; font-size: 13px; padding: 3px 0; }
.coord-info { font-size: 12px; line-height: 1.8; color: #333; }
.coord-info.dim { color: #bbb; }

.result-bar { display: flex; align-items: center; gap: 16px; background: #fff; border-radius: 8px; padding: 8px 16px; font-size: 13px; border-left: 4px solid #f39c12; }

.empty-state { display: flex; flex-direction: column; align-items: center; color: #bbb; }
.empty-state p { margin: 8px 0 0; font-size: 14px; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
