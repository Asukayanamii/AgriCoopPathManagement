<template>
  <div class="page">
    <div class="card">
      <div class="card-header">
        <h2>协同流水线 · 聚类 → 资源搜索 → 路径规划</h2>
        <div class="controls">
          <el-button type="primary" @click="generateScene" :icon="Plus">生成场景</el-button>
          <el-button type="success" @click="runPipeline" :icon="VideoPlay" :loading="loading">执行完整流水线</el-button>
          <el-button @click="reset" :icon="Refresh">重置</el-button>
        </div>
      </div>
      <div class="steps-bar">
        <div v-for="(s, i) in stepStatus" :key="'s'+i" class="step" :class="s">
          <div class="step-num">{{i+1}}</div>
          <div class="step-label">{{s === 'done' ? '已完成' : s === 'running' ? '进行中' : '待执行'}}</div>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="vis-panel">
        <svg :viewBox="'0 0 600 600'" class="svg">
          <rect x="0" y="0" width="600" height="600" fill="#f8f9fa" stroke="#dee2e6"/>
          <line v-for="(_, i) in 12" :key="'gv'+i" :x1="i*50" y1="0" :x2="i*50" y2="600" stroke="#eee"/>
          <line v-for="(_, i) in 12" :key="'gh'+i" x1="0" :y1="i*50" :x2="600" :y2="i*50" stroke="#eee"/>
          <g v-for="(pt, i) in scaledTaskPoints" :key="'tp'+i">
            <circle :cx="pt[0]" :cy="pt[1]" r="4" :fill="getClusterColor(pt[2])" stroke="#fff" stroke-width="1" opacity="0.8"/>
          </g>
          <g v-for="(c, i) in scaledCenters" :key="'cc'+i">
            <circle :cx="c[0]" :cy="c[1]" r="9" :fill="colors[i%colors.length]" stroke="#333" stroke-width="2"/>
            <text :x="c[0]" :y="c[1]+4" text-anchor="middle" font-size="9" fill="#fff" font-weight="bold">Z{{i+1}}</text>
          </g>
          <template v-for="(r, i) in scaledResources" :key="'rr'+i">
            <rect :x="r[0]-6" :y="r[1]-5" width="12" height="10" rx="2" fill="#22c55e" stroke="#166534" stroke-width="1.5"/>
          </template>
          <polygon v-for="(t, i) in scaledTargets" :key="'tg'+i" :points="t.poly" fill="#ef4444" stroke="#fff" stroke-width="2"/>
          <line v-for="(l, i) in matchLines" :key="'ml'+i" :x1="l[0]" :y1="l[1]" :x2="l[2]" :y2="l[3]" stroke="#f59e0b" stroke-width="2" stroke-dasharray="4,3" opacity="0.7"/>
          <polyline v-for="(pl, i) in pathLines" :key="'pl'+i" :points="pl" fill="none" stroke="#3498db" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" opacity="0.8"/>
        </svg>
      </div>
      <div class="info-panel">
        <h3>流水线结果</h3>
        <div class="metric"><span class="label">任务点数</span><span class="val">{{taskPoints.length}}</span></div>
        <div class="metric"><span class="label">聚类数</span><span class="val">{{clusterCount}}</span></div>
        <div class="metric"><span class="label">资源(小车)</span><span class="val">{{resources.length}}</span></div>
        <div class="metric"><span class="label">调度目标</span><span class="val">{{targets.length}}</span></div>
        <el-divider/>
        <h4>聚类详情</h4>
        <div v-for="(g, i) in groups" :key="'g'+i" class="cluster-item">
          <span class="dot" :style="{background: colors[i%colors.length]}"></span>
          <span>聚类{{i+1}}: {{g.length}}个点</span>
        </div>
        <el-divider/>
        <h4>调度结果</h4>
        <div v-for="(r, i) in searchResults" :key="'sr'+i" class="result-item">
          <span>P{{i+1}}: </span>
          <span v-if="r.status==='success'" style="color:#22c55e">已分配</span>
          <span v-else style="color:#ef4444">失败</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, VideoPlay, Refresh } from '@element-plus/icons-vue'
import { pipeline } from '@/api/algorithm'

const colors = ['#e74c3c','#3498db','#2ecc71','#f39c12','#9b59b6']
const loading = ref(false)
const taskPoints = ref([])
const resources = ref([])
const targets = ref([])
const groups = ref([])
const centers = ref([])
const clusterCount = ref(0)
const searchResults = ref([])
const pathPlans = ref([])
const stepStatus = ref(['pending', 'pending', 'pending'])
const scale = 3

const scaledTaskPoints = computed(() => taskPoints.value.map(p => [p[0]*scale, p[1]*scale, p[2]]))
const scaledCenters = computed(() => centers.value.map(c => [c[0]*scale, c[1]*scale]))
const scaledResources = computed(() => resources.value.map(r => [r.x*scale, r.y*scale]))
const scaledTargets = computed(() => targets.value.map(t => ({
  x: t.x*scale, y: t.y*scale,
  poly: `${t.x*scale},${t.y*scale-9} ${t.x*scale-6},${t.y*scale+5} ${t.x*scale+6},${t.y*scale+5}`
})))
const matchLines = computed(() => {
  const lines = []
  targets.value.forEach((t, ti) => {
    if (searchResults.value[ti]?.status !== 'success') return
    ;(searchResults.value[ti]?.resourceIds || []).forEach(id => {
      const r = resources.value.find(r2 => r2.id === id)
      if (r) lines.push([r.x*scale, r.y*scale, t.x*scale, t.y*scale])
    })
  })
  return lines
})
const pathLines = computed(() => {
  return pathPlans.value.map(pl => {
    if (!pl.path || pl.path.length < 2) return ''
    return pl.path.map(p => `${p[0] * scale},${p[1] * scale}`).join(' ')
  }).filter(Boolean)
})

const getClusterColor = (meanid) => meanid > 0 ? colors[(meanid-1) % colors.length] : '#adb5bd'

function generateScene() {
  const pts = []
  const zoneCenters = [[30,30], [140,30], [30,150], [120,150], [70,80]]
  const zoneSizes = [12, 10, 8, 10, 8]
  zoneCenters.forEach((zc, zi) => {
    for (let j = 0; j < zoneSizes[zi]; j++) {
      pts.push([zc[0] + Math.floor(Math.random() * 25 - 12), zc[1] + Math.floor(Math.random() * 25 - 12), 0])
    }
  })
  taskPoints.value = pts
  const res = []
  const departCenters = [[15,15], [65,65], [150,160]]
  departCenters.forEach((dc, di) => {
    for (let j = 0; j < 3; j++) {
      res.push({ x: dc[0] + Math.floor(Math.random() * 10 - 5), y: dc[1] + Math.floor(Math.random() * 10 - 5), id: di * 3 + j + 1, available: true })
    }
  })
  resources.value = res
  const tgts = [{x:25,y:25,required:2},{x:135,y:30,required:2},{x:30,y:145,required:1},{x:115,y:150,required:3}]
  targets.value = tgts
  groups.value = []
  centers.value = []
  clusterCount.value = 0
  searchResults.value = []
  pathPlans.value = []
  stepStatus.value = ['pending', 'pending', 'pending']
}

function reset() { generateScene() }

async function runPipeline() {
  loading.value = true
  stepStatus.value = ['running', 'pending', 'pending']
  try {
    const res = await pipeline({
      mapWidth: 600, mapHeight: 600,
      taskPoints: taskPoints.value.map(p => [p[0], p[1]]),
      spaceCluster: 8, deviation: 2, iterationCount: 20,
      resources: resources.value.map(r => ({ x: r.x, y: r.y, id: r.id, available: r.available })),
      specificTargets: targets.value.map(t => ({ x: t.x, y: t.y, required: t.required }))
    })
    const steps = res.data?.pipelineSteps || {}
    const clusterInfo = steps.cluster || []
    groups.value = clusterInfo.map(c => c.points || [])
    centers.value = clusterInfo.map(c => [c.avgX || 0, c.avgY || 0])
    clusterCount.value = clusterInfo.length
    taskPoints.value = taskPoints.value.map((p, i) => {
      const assigned = clusterInfo.findIndex(c => (c.points || []).some(pt => pt[0] === p[0] && pt[1] === p[1]))
      return [p[0], p[1], assigned >= 0 ? assigned + 1 : 0]
    })
    stepStatus.value = ['done', 'done', 'done']
    searchResults.value = steps.resourceSearch || []
    pathPlans.value = steps.pathPlanning || []
    const matched = searchResults.value.filter(r => r.status === 'success').length
    ElMessage.success(`流水线完成：${clusterCount.value}个聚类，${matched}个目标匹配`)
  } catch (e) { ElMessage.error('流水线失败: ' + (e.message || '')) }
  finally { loading.value = false }
}

generateScene()
</script>

<style scoped>
.page { height: 100%; display: flex; flex-direction: column; gap: 16px; }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-header h2 { margin: 0; font-size: 18px; color: #1a202c; }
.controls { display: flex; gap: 8px; }
.steps-bar { display: flex; gap: 12px; margin-top: 8px; }
.step { flex: 1; text-align: center; padding: 10px; border-radius: 8px; background: #e9ecef; transition: all 0.3s; }
.step.done { background: #d4edda; }
.step.running { background: #cce5ff; animation: pulse 1.5s infinite; }
.step-num { font-size: 20px; font-weight: bold; color: #333; }
.step-label { font-size: 11px; color: #666; margin-top: 2px; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.6} }
.content { flex: 1; display: flex; gap: 16px; overflow: hidden; }
.vis-panel { flex: 1; background: #fff; border-radius: 8px; padding: 16px; display: flex; align-items: center; justify-content: center; }
.svg { width: 100%; max-width: 600px; height: auto; }
.info-panel { width: 280px; background: #fff; border-radius: 8px; padding: 16px; overflow-y: auto; }
.info-panel h3 { margin: 0 0 12px; font-size: 16px; }
.metric { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; }
.metric .label { color: #666; }
.metric .val { font-weight: 600; color: #333; }
.cluster-item, .result-item { padding: 4px 0; font-size: 13px; display: flex; align-items: center; gap: 6px; }
.dot { width: 12px; height: 12px; border-radius: 50%; display: inline-block; flex-shrink: 0; }
</style>
