<template>
  <div class="page">
    <!-- Top Toolbar -->
    <div class="toolbar">
      <el-select v-model="selectedMapId" placeholder="选择地图" style="width:200px" @change="onMapSelect" clearable>
        <el-option v-for="m in mapList" :key="m.id" :label="m.originName" :value="m.id" />
      </el-select>
      <el-upload :http-request="handleUpload" accept=".png" :show-file-list="false" :disabled="uploading">
        <el-button type="primary" :icon="Upload" :loading="uploading">上传</el-button>
      </el-upload>
      <span class="sep" />
      <el-button v-if="selectedMapId" size="small" type="danger" plain @click="handleDeleteMap">删除</el-button>
      <span v-if="mapInfo" class="map-info">{{ mapInfo }}</span>
      <span v-if="progress && progress.total > 0" class="progress-info">已完成 {{ progress.done }}/{{ progress.total }}</span>
    </div>

    <div class="content">
      <!-- SVG Canvas -->
      <div class="canvas-wrap">
        <svg v-if="mapImageUrl"
          :viewBox="`0 0 ${vw} ${vh}`" class="svg" ref="svgRef"
          @mousedown="onSvgMouseDown" @mouseup="onSvgMouseUp"
          @mousemove="onSvgMousemove" @mouseleave="onSvgMouseleave">
          <image :href="mapImageUrl" x="0" y="0" :width="vw" :height="vh" preserveAspectRatio="none" />

          <!-- Road nodes -->
          <circle v-for="(n, i) in roadNodes" :key="'rn'+i"
            :cx="n.x" :cy="n.y" r="6" fill="#409eff" stroke="#fff" stroke-width="2"
            @mouseenter="hoverNodeIdx = i" @mouseleave="hoverNodeIdx = -1" style="cursor:pointer" />
          <text v-if="hoverNodeIdx >= 0" :x="roadNodes[hoverNodeIdx].x" :y="roadNodes[hoverNodeIdx].y - 12"
            text-anchor="middle" font-size="11" fill="#409eff" font-weight="bold">
            ID:{{ roadNodes[hoverNodeIdx].id }}
          </text>
          <text v-for="(n, i) in roadNodes" :key="'rnl'+i"
            :x="n.x" :y="n.y + 18" text-anchor="middle" font-size="9" fill="#666">{{ n.id }}</text>

          <!-- Edges -->
          <line v-for="(e, i) in roadEdges" :key="'re'+i"
            :x1="e.fx" :y1="e.fy" :x2="e.tx" :y2="e.ty" stroke="#409eff" stroke-width="2" opacity="0.6" />

          <!-- Edge preview (connecting nodes) -->
          <line v-if="edgeFrom" :x1="edgeFrom.x" :y1="edgeFrom.y"
            :x2="mouseSvgX" :y2="mouseSvgY" stroke="#409eff" stroke-width="2" stroke-dasharray="4,2" opacity="0.5" />

          <!-- Task points -->
          <circle v-for="(t, i) in taskPoints" :key="'tp'+i"
            :cx="t.x" :cy="t.y" r="5" :fill="getColor(t.clusterId)" stroke="#fff" stroke-width="1.5" />
          <text v-for="(t, i) in taskPoints" :key="'tpl'+i"
            :x="t.x" :y="t.y + 16" text-anchor="middle" font-size="8" fill="#666">{{ i }}</text>

          <!-- Cluster centers -->
          <polygon v-for="(c, i) in clusterCenters" :key="'cc'+i"
            :points="starPoints(c.x, c.y, 10, 5)" :fill="colors[i % colors.length]" stroke="#333" stroke-width="1.5" />

          <!-- Car markers -->
          <g v-for="(c, i) in cars" :key="'car'+i">
            <rect :x="c.x - 8" :y="c.y - 6" width="16" height="12" rx="2"
              fill="#f39c12" stroke="#e67e22" stroke-width="2" />
            <text :x="c.x" :y="c.y + 4" text-anchor="middle" font-size="8" fill="#fff" font-weight="bold">🚗</text>
          </g>

          <!-- Car paths -->
          <polyline v-for="(cp, i) in carPaths" :key="'cp'+i"
            :points="cp.svgPoints" fill="none" stroke="#e74c3c" stroke-width="3"
            stroke-linejoin="round" stroke-linecap="round" opacity="0.85" />
          <text v-for="(cp, i) in carPaths" :key="'cpl'+i"
            :x="cp.midX" :y="cp.midY - 8" text-anchor="middle" font-size="11"
            fill="#e74c3c" font-weight="bold">小车{{ cp.carId }}</text>

          <!-- Drone TSP route -->
          <polyline v-if="droneRoutePoints.length"
            :points="droneRoutePoints.map(p => `${p.x},${p.y}`).join(' ')"
            fill="none" stroke="#2ecc71" stroke-width="2.5" stroke-dasharray="6,3"
            stroke-linejoin="round" stroke-linecap="round" opacity="0.8" />
        </svg>
        <div v-else class="empty-state">
          <el-icon :size="48"><Picture /></el-icon>
          <p>请上传地图开始算法演示</p>
        </div>
      </div>

      <!-- Step Panel -->
      <div class="step-panel">
        <div class="step" :class="{ active: currentStep >= 1, done: stage1Done }">
          <div class="step-header" @click="currentStep = 1"><span class="step-num">1</span> 标记路网
            <el-tag v-if="stage1Done" size="small" type="success">{{ roadNodes.length }}点/{{ roadEdges.length }}边</el-tag>
          </div>
          <div v-if="currentStep === 1" class="step-body">
            <el-radio-group v-model="mode" size="small">
              <el-radio-button value="road-node">加路点</el-radio-button>
              <el-radio-button value="road-edge">加边</el-radio-button>
              <el-radio-button value="delete">删除</el-radio-button>
              <el-radio-button value="view">查看</el-radio-button>
            </el-radio-group>
            <div class="step-actions">
              <el-button size="small" @click="undoLast">撤销</el-button>
              <el-button size="small" type="danger" @click="clearRoadNetwork">清空</el-button>
              <el-button v-if="roadNodes.length >= 2" size="small" type="success" @click="confirmStage1">确认路网</el-button>
            </div>
            <div class="hint" v-if="mode==='road-node'">点击地图放置路点</div>
            <div class="hint" v-else-if="mode==='road-edge'">依次点击两个路点添加路段</div>
            <div class="hint" v-else-if="mode==='delete'">点击路点/边删除</div>
          </div>
        </div>

        <div class="step" :class="{ active: currentStep >= 2, done: stage2Done }">
          <div class="step-header" @click="currentStep = 2"><span class="step-num">2</span> 建图
            <el-tag v-if="stage2Done" size="small" type="success">已建图</el-tag>
          </div>
          <div v-if="currentStep === 2" class="step-body">
            <el-button size="small" type="primary" @click="doBuild" :loading="building">建图 + 编码 + 标准地图</el-button>
          </div>
        </div>

        <div class="step" :class="{ active: currentStep >= 3, done: stage3Done }">
          <div class="step-header" @click="currentStep = 3"><span class="step-num">3</span> 任务点 & 聚类
            <el-tag v-if="stage3Done" size="small" type="success">{{ clusterCount }}簇</el-tag>
          </div>
          <div v-if="currentStep === 3" class="step-body">
            <el-radio-group v-model="mode" size="small">
              <el-radio-button value="task-point">加任务点</el-radio-button>
              <el-radio-button value="view">查看</el-radio-button>
            </el-radio-group>
            <el-button size="small" @click="clearTasks">清空</el-button>
            <el-button v-if="taskPoints.length >= 2" size="small" type="primary" @click="doCluster" :loading="clustering">执行聚类</el-button>
            <div class="hint">点击地图放置任务点，至少 2 个才能聚类</div>
          </div>
        </div>

        <div class="step" :class="{ active: currentStep >= 4, done: cars.length > 0 }">
          <div class="step-header" @click="currentStep = 4"><span class="step-num">4</span> 车辆资源
            <el-tag v-if="cars.length" size="small" type="success">{{ cars.length }}辆</el-tag>
          </div>
          <div v-if="currentStep === 4" class="step-body">
            <el-radio-group v-model="mode" size="small">
              <el-radio-button value="car">加小车</el-radio-button>
              <el-radio-button value="view">查看</el-radio-button>
            </el-radio-group>
            <div v-for="(c, i) in cars" :key="i" class="car-item">
              <span>小车{{ i }}: ({{ c.x }},{{ c.y }}) → 路点{{ c.belongNode }}</span>
              <el-button size="small" text type="danger" @click="cars.splice(i,1)">✕</el-button>
            </div>
            <el-button v-if="cars.length" size="small" type="primary" @click="doSaveCars" :loading="savingCars">保存到数据库</el-button>
            <div class="hint">切换"加小车"模式，点击路点登记车辆</div>
          </div>
        </div>

        <div class="step" :class="{ active: currentStep >= 5, done: stage5Done }">
          <div class="step-header" @click="currentStep = 5"><span class="step-num">5</span> 优先级
            <el-tag v-if="stage5Done" size="small" type="success">已确认</el-tag>
          </div>
          <div v-if="currentStep === 5" class="step-body">
            <div v-if="priorities.length === 0" class="hint">请先执行聚类</div>
            <draggable v-else v-model="priorities" item-key="clusterId" class="drag-list" @end="onPriorityChange">
              <template #item="{ element, index }">
                <div class="drag-item">
                  <span class="drag-handle">☰</span>
                  <span class="drag-color" :style="{ background: colors[index % colors.length] }" />
                  <span>优先级 {{ index + 1 }} — 簇 {{ element.clusterId }}</span>
                </div>
              </template>
            </draggable>
            <el-button size="small" type="success" @click="confirmStage5">确认并保存</el-button>
          </div>
        </div>

        <div class="step" :class="{ active: currentStep >= 6 }">
          <div class="step-header" @click="currentStep = 6"><span class="step-num">6</span> 执行</div>
          <div v-if="currentStep === 6" class="step-body">
            <el-button v-if="!execDone" type="success" size="small" @click="doExecuteNext" :loading="executing">执行下一簇</el-button>
            <div v-if="carPaths.length" class="result-box">
              <div v-for="(cp, i) in carPaths" :key="i" class="result-line">
                小车 {{ cp.carId }} → 路径 {{ cp.pathCode }}
              </div>
              <el-button size="small" @click="doCarArrived" :loading="tspLoading">小车到达，规划无人机</el-button>
            </div>
            <div v-if="droneRoute" class="result-box">
              无人机路线: {{ droneRoute }}
              <el-button size="small" type="warning" @click="doDroneDone">无人机完成</el-button>
            </div>
            <div v-if="progress && progress.total > 0" class="result-box">进度: {{ progress.done }}/{{ progress.total }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Picture } from '@element-plus/icons-vue'
import { uploadMapImage, getMapImageList, getMapImageUrl, deleteMapImage } from '@/api/map'
import { saveNodes, saveEdges, buildGraph, createTasks, runCluster, savePriority, executeNext, carArrived, droneDone, getProgress, saveResources, getNodes, getEdges, getTasks, getResources } from '@/api/algorithm'
import draggable from 'vuedraggable'

// --- Map ---
const mapList = ref([])
const selectedMapId = ref(null)
const mapImageUrl = ref(null)
const mapInfo = ref('')
const uploading = ref(false)

// --- Image dimensions for viewBox ---
const imgW = ref(1000)
const imgH = ref(1000)
const vw = computed(() => imgW.value >= imgH.value ? 1000 : Math.round(1000 * imgW.value / imgH.value))
const vh = computed(() => imgW.value >= imgH.value ? Math.round(1000 * imgH.value / imgW.value) : 1000)

// --- SVG interaction ---
const svgRef = ref(null)
const mode = ref('view')
const mouseSvgPos = ref({ x: 0, y: 0 })
const mouseSvgX = computed(() => mouseSvgPos.value.x)
const mouseSvgY = computed(() => mouseSvgPos.value.y)

function getSvgPoint(clientX, clientY) {
  const svg = svgRef.value
  if (!svg) return null
  const rect = svg.getBoundingClientRect()
  const sc = Math.min(rect.width / vw.value, rect.height / vh.value)
  const rw = vw.value * sc, rh = vh.value * sc
  const ox = (rect.width - rw) / 2, oy = (rect.height - rh) / 2
  return { x: (clientX - rect.left - ox) / sc, y: (clientY - rect.top - oy) / sc }
}

// --- Stage 1: Road Network ---
const roadNodes = ref([])
const roadEdges = ref([])
const nextNodeId = ref(1)
const edgeFrom = ref(null)
const hoverNodeIdx = ref(-1)
const stage1Done = ref(false)

function confirmStage1() { stage1Done.value = true; if (currentStep.value < 2) currentStep.value = 2 }

function undoLast() {
  if (edgeFrom.value) { edgeFrom.value = null; return }
  if (roadEdges.value.length) roadEdges.value.pop()
  else if (roadNodes.value.length) roadNodes.value.pop()
}

function clearRoadNetwork() { roadNodes.value = []; roadEdges.value = []; edgeFrom.value = null; nextNodeId.value = 1; stage1Done.value = false }

function onSvgMouseDown(e) {
  const pt = getSvgPoint(e.clientX, e.clientY)
  if (!pt) return

  if (mode.value === 'road-node') {
    roadNodes.value.push({ id: nextNodeId.value++, x: Math.round(pt.x), y: Math.round(pt.y) })
  } else if (mode.value === 'road-edge') {
    const clicked = roadNodes.value.findIndex(n => Math.abs(n.x - pt.x) < 12 && Math.abs(n.y - pt.y) < 12)
    if (clicked >= 0) {
      const n = roadNodes.value[clicked]
      if (!edgeFrom.value) { edgeFrom.value = n; return }
      if (edgeFrom.value.id !== n.id) {
        roadEdges.value.push({ fromNode: edgeFrom.value.id, toNode: n.id, fx: edgeFrom.value.x, fy: edgeFrom.value.y, tx: n.x, ty: n.y })
      }
      edgeFrom.value = null
    }
  } else if (mode.value === 'delete') {
    const ni = roadNodes.value.findIndex(n => Math.abs(n.x - pt.x) < 12 && Math.abs(n.y - pt.y) < 12)
    if (ni >= 0) {
      const id = roadNodes.value[ni].id
      roadEdges.value = roadEdges.value.filter(e => e.fromNode !== id && e.toNode !== id)
      roadNodes.value.splice(ni, 1)
      return
    }
    const ei = roadEdges.value.findIndex(e => distToSegment(pt.x, pt.y, e.fx, e.fy, e.tx, e.ty) < 10)
    if (ei >= 0) roadEdges.value.splice(ei, 1)
  } else if (mode.value === 'car') {
    // Find nearest road node within 20px
    const nearest = findNearestNode(pt.x, pt.y, 20)
    if (nearest) {
      // Check if car already at this node
      if (!cars.value.some(c => c.belongNode === nearest.id))
        cars.value.push({ x: nearest.x, y: nearest.y, belongNode: nearest.id })
    }
  } else if (mode.value === 'task-point') {
    taskPoints.value.push({ x: Math.round(pt.x), y: Math.round(pt.y), clusterId: -1 })
  }
}
function onSvgMouseUp() { /* placeholder for potential drag operations */ }
function onSvgMousemove(e) {
  const pt = getSvgPoint(e.clientX, e.clientY)
  if (pt) mouseSvgPos.value = pt
}
function onSvgMouseleave() { mouseSvgPos.value = { x: 0, y: 0 } }

function findNearestNode(x, y, threshold) {
  let best = null, bestDist = threshold
  for (const n of roadNodes.value) {
    const d = Math.hypot(n.x - x, n.y - y)
    if (d < bestDist) { bestDist = d; best = n }
  }
  return best
}

function distToSegment(px, py, x1, y1, x2, y2) {
  const dx = x2 - x1, dy = y2 - y1
  const dot = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy || 1)
  const t = Math.max(0, Math.min(1, dot))
  return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy))
}

// --- Stage 2: Build ---
const building = ref(false)
const stage2Done = ref(false)

async function doBuild() {
  if (roadNodes.value.length < 2) { ElMessage.warning('至少需要 2 个路点'); return }
  if (!selectedMapId.value) { ElMessage.warning('请先选择地图'); return }
  building.value = true
  try {
    const r1 = await saveNodes(selectedMapId.value, roadNodes.value.map(n => ({ x: n.x, y: n.y })))
    if (r1.code !== 1) { ElMessage.error(r1.msg || '保存路点失败'); return }
    const ids = r1.data.ids
    const edges = roadEdges.value.map(e => {
      const fromN = roadNodes.value.find(n => n.id === e.fromNode)
      const toN = roadNodes.value.find(n => n.id === e.toNode)
      if (!fromN || !toN) return null
      return {
        fromNode: ids[roadNodes.value.indexOf(fromN)],
        toNode: ids[roadNodes.value.indexOf(toN)],
        weight: Math.round(Math.hypot(fromN.x - toN.x, fromN.y - toN.y))
      }
    }).filter(Boolean)
    const r2 = await saveEdges(selectedMapId.value, edges)
    if (r2.code !== 1) { ElMessage.error(r2.msg || '保存路段失败'); return }
    const r3 = await buildGraph(selectedMapId.value)
    if (r3.code === 1) {
      ElMessage.success('建图成功')
      stage2Done.value = true
      await loadMapData(selectedMapId.value) // reload persisted data
      if (currentStep.value < 3) currentStep.value = 3
    } else {
      ElMessage.error(r3.msg || '建图失败')
    }
  } catch (e) { ElMessage.error('建图失败: ' + (e.message || ''))
  } finally { building.value = false }
}

// --- Stage 3: Tasks & Cluster ---
const taskPoints = ref([])
const clustering = ref(false)
const stage3Done = ref(false)
const clusterCount = ref(0)
const clusterCenters = ref([])
const clusters = ref([])
const colors = ['#e74c3c','#3498db','#2ecc71','#f39c12','#9b59b6','#1abc9c','#e67e22','#34495e']

async function doCluster() {
  clustering.value = true
  try {
    const r1 = await createTasks(selectedMapId.value, taskPoints.value.map(t => ({ x: t.x, y: t.y })))
    if (r1.code !== 1) return
    // Store task codes for TSP result matching
    r1.data.ids.forEach((id, i) => { if (taskPoints.value[i]) taskPoints.value[i]._dbCode = r1.data.codes[i] })
    // Map DB task IDs to frontend indices for later lookup
    const taskIdMap = new Map()
    r1.data.ids.forEach((id, i) => taskIdMap.set(id, i))

    const r2 = await runCluster(selectedMapId.value, { space: Math.max(2, Math.floor(taskPoints.value.length / 3)), deviation: 2, maxIter: 50 })
    if (r2.code === 1) {
      clusterCount.value = r2.data.clusterCount
      clusters.value = r2.data.clusters
      // Apply cluster assignments to task points
      for (const a of r2.data.assignments) {
        const idx = taskIdMap.get(a.taskId)
        if (idx !== undefined) taskPoints.value[idx].clusterId = a.clusterId
      }
      // Compute cluster centers from assigned points
      const centerMap = new Map()
      for (const tp of taskPoints.value) {
        if (tp.clusterId < 0) continue
        if (!centerMap.has(tp.clusterId)) centerMap.set(tp.clusterId, { sumX: 0, sumY: 0, count: 0 })
        const c = centerMap.get(tp.clusterId)
        c.sumX += tp.x; c.sumY += tp.y; c.count++
      }
      clusterCenters.value = []
      for (const [cid, c] of centerMap) {
        clusterCenters.value.push({ x: Math.round(c.sumX / c.count), y: Math.round(c.sumY / c.count) })
      }
      stage3Done.value = true
      buildPriorities()
      await updateProgress()
      if (currentStep.value < 4) currentStep.value = 4
      ElMessage.success(`聚类完成，共 ${clusterCount.value} 个簇`)
    }
  } catch (e) { ElMessage.error('聚类失败') }
  finally { clustering.value = false }
}

function getColor(clusterId) { return clusterId >= 0 ? colors[clusterId % colors.length] : '#bbb' }

function clearTasks() {
  taskPoints.value = []; clusterCenters.value = []; clusterCount.value = 0
  clusters.value = []; priorities.value = []; stage3Done.value = false; stage5Done.value = false
}

// --- Stage 4: Cars ---
const cars = ref([])
const savingCars = ref(false)

async function doSaveCars() {
  if (!cars.value.length) return
  savingCars.value = true
  try {
    const r = await saveResources(selectedMapId.value, cars.value.map(c => ({ x: c.x, y: c.y, belongNode: c.belongNode })))
    if (r.code === 1) ElMessage.success(`已保存 ${r.data.count} 辆车`)
    else ElMessage.error(r.msg || '保存失败')
  } catch (e) { ElMessage.error('保存车辆失败') }
  finally { savingCars.value = false }
}

// --- Stage 5: Priority ---
const priorities = ref([])
const stage5Done = ref(false)

function buildPriorities() {
  priorities.value = []
  for (let i = 0; i < clusterCount.value; i++) priorities.value.push({ clusterId: i, priority: i })
}
function onPriorityChange() {
  priorities.value.forEach((p, i) => p.priority = i)
}
async function confirmStage5() {
  try {
    await savePriority(selectedMapId.value, priorities.value.map(p => ({ clusterId: p.clusterId, priority: p.priority })))
    stage5Done.value = true
    await updateProgress()
    if (currentStep.value < 6) currentStep.value = 6
    ElMessage.success('优先级已保存')
  } catch (e) { ElMessage.error('保存失败') }
}

// --- Stage 5: Execute ---
const executing = ref(false)
const execDone = ref(false)
const carPaths = ref([])
const droneRoute = ref('')
const droneRoutePoints = ref([])
const tspLoading = ref(false)
const progress = ref(null)
const currentStep = ref(1)

async function doExecuteNext() {
  if (!stage3Done.value || !stage5Done.value) {
    ElMessage.warning('请先完成聚类和优先级设置')
    return
  }
  executing.value = true
  try {
    const r = await executeNext(selectedMapId.value)
    if (r.code === 1) {
      currentClusterId.value = r.data.clusterId
      carPaths.value = r.data.carPaths.map(cp => {
        const codes = cp.pathCode.match(/.{4}/g) || []
        const graphIndices = codes.map(c => parseInt(c, 16)).filter(id => !isNaN(id))
        const svgPts = graphIndices.map(gi => {
          const n = roadNodes.value.find(n => n.id === gi + 1)
          return n ? { x: n.x, y: n.y } : null
        }).filter(Boolean)
        return {
          carId: cp.carId,
          pathCode: cp.pathCode,
          svgPoints: svgPts.map(p => `${p.x},${p.y}`).join(' '),
          midX: svgPts.length > 0 ? svgPts[Math.floor(svgPts.length / 2)].x : 0,
          midY: svgPts.length > 0 ? svgPts[Math.floor(svgPts.length / 2)].y : 0
        }
      })
      await updateProgress()
      ElMessage.success('小车路径已规划')
    } else ElMessage.error(r.msg || '执行失败')
  } catch (e) { ElMessage.error('执行失败') }
  finally { executing.value = false }
}

async function doCarArrived() {
  tspLoading.value = true
  try {
    const r = await carArrived(selectedMapId.value, currentClusterId.value)
    if (r.code === 1) {
      droneRoute.value = r.data.droneRoute
      // Parse TSP result: codes are task point codes (or "0000" for cluster center)
      const codes = r.data.droneRoute.match(/.{4}/g) || []
      droneRoutePoints.value = codes.map(c => {
        const rawId = parseInt(c, 16)
        if (isNaN(rawId)) return null
        // rawId 0 = cluster center, positive rawId = task point
        if (rawId === 0 && clusterCenters.value.length > 0) return clusterCenters.value[0]
        // rawId corresponds to the DB task ID; find matching task point by its code
        const tp = taskPoints.value.find(t => t._dbCode === c)
        return tp || null
      }).filter(Boolean)
      ElMessage.success('无人机路线已规划')
    }
  } catch (e) { ElMessage.error('TSP 规划失败') }
  finally { tspLoading.value = false }
}

async function doDroneDone() {
  try {
    const r = await droneDone(selectedMapId.value, currentClusterId.value)
    if (r.code === 1) {
      progress.value = r.data
      carPaths.value = []
      droneRoute.value = ''
      droneRoutePoints.value = []
      if (r.data.done >= r.data.total) execDone.value = true
      await updateProgress()
      ElMessage.success('簇已完成')
    }
  } catch (e) { ElMessage.error('操作失败') }
}

async function updateProgress() {
  try {
    const r = await getProgress(selectedMapId.value)
    if (r.code === 1) progress.value = r.data
  } catch (_) {}
}

function starPoints(cx, cy, r, n) {
  const pts = []
  for (let i = 0; i < n * 2; i++) {
    const a = (i * Math.PI) / n - Math.PI / 2
    const rad = i % 2 === 0 ? r : r / 2
    pts.push(`${cx + Math.cos(a) * rad},${cy + Math.sin(a) * rad}`)
  }
  return pts.join(' ')
}

// --- Lifecycle ---
onMounted(() => loadMapList())
async function loadMapList() {
  try { const r = await getMapImageList(); if (r.code === 1) mapList.value = r.data || [] } catch (_) {}
}
async function onMapSelect(id) {
  clearRoadNetwork(); clearTasks(); carPaths.value = []; droneRoute.value = ''; stage2Done.value = false; stage5Done.value = false; cars.value = []; execDone.value = false; progress.value = null; currentStep.value = 1
  if (!id) { mapImageUrl.value = null; mapInfo.value = ''; return }
  const m = mapList.value.find(m => m.id === id)
  if (m) { mapImageUrl.value = getMapImageUrl(id); imgW.value = m.width; imgH.value = m.height; mapInfo.value = `${m.width}×${m.height}px` }
  await loadMapData(id)
  currentStep.value = 1
}

async function loadMapData(mapId) {
  try {
    // Load road nodes
    const nr = await getNodes(mapId)
    if (nr.code === 1 && nr.data.length) {
      roadNodes.value = nr.data.map(n => ({ id: n.id, x: n.x, y: n.y }))
      nextNodeId.value = Math.max(...nr.data.map(n => n.id), 0) + 1
      // Load edges
      const er = await getEdges(mapId)
      if (er.code === 1) {
        const nodeMap = Object.fromEntries(nr.data.map(n => [n.id, n]))
        roadEdges.value = er.data.map(e => ({
          fromNode: e.fromNode, toNode: e.toNode,
          fx: nodeMap[e.fromNode]?.x || 0, fy: nodeMap[e.fromNode]?.y || 0,
          tx: nodeMap[e.toNode]?.x || 0, ty: nodeMap[e.toNode]?.y || 0
        }))
      }
      stage1Done.value = true; stage2Done.value = true
    }
    // Load task points
    const tr = await getTasks(mapId)
    if (tr.code === 1 && tr.data.length) {
      taskPoints.value = tr.data.map(t => ({ x: t.x, y: t.y, clusterId: -1, _dbCode: t.code }))
      stage3Done.value = true
    }
    // Load resources
    const rr = await getResources(mapId)
    if (rr.code === 1) cars.value = rr.data.map(c => ({ x: c.x, y: c.y, belongNode: c.belongNode }))
    // Load progress & clusters
    const pr = await getProgress(mapId)
    if (pr.code === 1) progress.value = pr.data
    if (progress.value && progress.value.total > 0) {
      stage3Done.value = true; stage5Done.value = true
    } else {
      // For fresh maps, check if there are persisted task points with codes
      if (taskPoints.value.length > 0) stage3Done.value = true
    }
  } catch (_) { /* ignore load errors */ }
}
async function handleUpload({ file }) {
  uploading.value = true
  try {
    const r = await uploadMapImage(file)
    if (r.code === 1) { ElMessage.success('上传成功'); await loadMapList(); selectedMapId.value = r.data.id; onMapSelect(r.data.id) }
    else ElMessage.error(r.msg || '上传失败')
  } catch (e) { ElMessage.error('上传失败') }
  finally { uploading.value = false }
}
async function handleDeleteMap() {
  if (!selectedMapId.value) return
  try {
    const r = await deleteMapImage(selectedMapId.value)
    if (r.code === 1) {
      ElMessage.success('已删除')
      selectedMapId.value = null
      onMapSelect(null)
      await loadMapList()
    } else ElMessage.error(r.msg || '删除失败')
  } catch (e) { ElMessage.error('删除失败') }
}
</script>

<style scoped>
.page { height: 100%; display: flex; flex-direction: column; gap: 8px; }
.toolbar { display: flex; align-items: center; gap: 10px; background: #fff; border-radius: 8px; padding: 8px 16px; }
.sep { width: 1px; height: 24px; background: #e5e7eb; }
.map-info { font-size: 12px; color: #999; }
.progress-info { margin-left: auto; font-size: 12px; color: #27ae60; font-weight: 600; }

.content { flex: 1; display: flex; gap: 8px; overflow: hidden; }
.canvas-wrap { flex: 1; background: #fff; border-radius: 8px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.svg { width: 100%; height: 100%; cursor: crosshair; display: block; }

.step-panel { width: 280px; background: #fff; border-radius: 8px; padding: 12px; overflow-y: auto; flex-shrink: 0; display: flex; flex-direction: column; gap: 6px; }
.step { border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; }
.step.active { border-color: #409eff; }
.step.done { border-color: #27ae60; }
.step-header { display: flex; align-items: center; gap: 8px; padding: 8px 10px; cursor: pointer; font-size: 13px; font-weight: 600; color: #333; background: #f9fafb; }
.step.done .step-header { background: #f0fdf4; }
.step.active .step-header { background: #eff6ff; }
.step-num { display: inline-flex; width: 20px; height: 20px; border-radius: 50%; background: #d1d5db; color: #fff; align-items: center; justify-content: center; font-size: 11px; }
.step.active .step-num { background: #409eff; }
.step.done .step-num { background: #27ae60; }
.step-body { padding: 8px 10px; display: flex; flex-direction: column; gap: 6px; }
.step-actions { display: flex; gap: 4px; flex-wrap: wrap; }
.hint { font-size: 11px; color: #999; }

.drag-list { display: flex; flex-direction: column; gap: 4px; }
.drag-item { display: flex; align-items: center; gap: 6px; padding: 4px 8px; background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 4px; cursor: move; font-size: 12px; }
.drag-handle { cursor: grab; color: #bbb; }
.drag-color { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }

.car-item { display: flex; align-items: center; gap: 4px; font-size: 12px; padding: 3px 6px; background: #fffbeb; border: 1px solid #fde68a; border-radius: 4px; }
.result-box { font-size: 12px; padding: 6px; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 4px; display: flex; flex-direction: column; gap: 4px; }
.result-line { padding: 2px 0; }

.empty-state { display: flex; flex-direction: column; align-items: center; color: #bbb; }
.empty-state p { margin: 8px 0 0; font-size: 14px; }
</style>
