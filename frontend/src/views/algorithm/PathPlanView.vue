<template>
  <div class="page">
    <div class="card">
      <div class="card-header">
        <h2>A* 路径规划算法</h2>
        <div class="controls">
          <el-button type="primary" @click="generateObstacles" :icon="Plus">生成障碍物</el-button>
          <el-button type="success" @click="runPathPlan" :icon="VideoPlay" :loading="loading">执行规划</el-button>
          <el-button @click="reset" :icon="Refresh">重置</el-button>
        </div>
      </div>
      <div class="params">
        <span>障碍物: {{obstacleCount}}</span>
        <span>起点: ({{start.x}}, {{start.y}})</span>
        <span>终点: ({{end.x}}, {{end.y}})</span>
      </div>
    </div>
    <div class="content">
      <div class="vis-panel">
        <svg :viewBox="'0 0 600 600'" class="svg">
          <rect x="0" y="0" width="600" height="600" fill="#f8f9fa" stroke="#dee2e6"/>
          <line v-for="(_, i) in 12" :key="'gv'+i" :x1="i*50" y1="0" :x2="i*50" y2="600" stroke="#eee"/>
          <line v-for="(_, i) in 12" :key="'gh'+i" x1="0" :y1="i*50" :x2="600" :y2="i*50" stroke="#eee"/>
          <rect v-for="(o, i) in scaledObstacles" :key="'o'+i" :x="o[0]-8" :y="o[1]-8" width="16" height="16" rx="3" fill="#495057" stroke="#212529"/>
          <circle :cx="startS[0]" :cy="startS[1]" r="10" fill="#2ecc71" stroke="#fff" stroke-width="2"/>
          <text :x="startS[0]" :y="startS[1]+4" text-anchor="middle" font-size="10" fill="#fff" font-weight="bold">起</text>
          <polygon v-if="!loading" :points="endPoly" fill="#e74c3c" stroke="#fff" stroke-width="2"/>
          <polyline v-if="path.length" :points="pathStr" fill="none" stroke="#3498db" stroke-width="3" stroke-linejoin="round" stroke-linecap="round"/>
          <circle v-for="(pt, i) in pathNodes" :key="'pn'+i" :cx="pt[0]" :cy="pt[1]" r="3.5" fill="#3498db" stroke="#fff" stroke-width="1"/>
        </svg>
      </div>
      <div class="info-panel">
        <h3>规划结果</h3>
        <div class="metric"><span class="label">路径长度</span><span class="val">{{distance}}</span></div>
        <div class="metric"><span class="label">路径点数</span><span class="val">{{path.length}}</span></div>
        <div class="metric"><span class="label">障碍物数</span><span class="val">{{obstacles.length}}</span></div>
        <div class="metric"><span class="label">状态</span><el-tag :type="status === 'done' ? 'success' : 'info'" size="small">{{statusText}}</el-tag></div>
        <el-divider/>
        <h4>操作说明</h4>
        <p style="font-size:12px;color:#666;line-height:1.8">
          随机生成障碍物后，A*算法从起点(绿色)到终点(红色)规划最短路径（蓝色折线）。
          算法采用曼哈顿距离启发式，支持4方向移动。
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, VideoPlay, Refresh } from '@element-plus/icons-vue'
import { pathPlanning } from '@/api/algorithm'

const loading = ref(false)
const status = ref('idle')
const obstacles = ref([])
const path = ref([])
const distance = ref(0)
const start = ref({ x: 30, y: 30 })
const end = ref({ x: 170, y: 170 })
const obstacleCount = ref(20)

const scale = 3
const startS = computed(() => [start.value.x * scale, start.value.y * scale])
const endS = computed(() => [end.value.x * scale, end.value.y * scale])
const scaledObstacles = computed(() => obstacles.value.map(o => [o[0] * scale, o[1] * scale]))
const pathStr = computed(() => path.value.map(p => `${p[0]},${p[1]}`).join(' '))
const pathNodes = computed(() => path.value.filter((_, i) => i > 0 && i < path.value.length - 1))
const endPoly = computed(() => {
  const [ex, ey] = endS.value
  return `${ex},${ey-12} ${ex-8},${ey+6} ${ex+8},${ey+6}`
})
const statusText = computed(() => ({ idle: '就绪', running: '计算中...', done: '完成' })[status.value])

function generateObstacles() {
  const obs = []
  for (let i = 0; i < obstacleCount.value; i++) {
    let x = Math.floor(Math.random() * 180) + 10
    let y = Math.floor(Math.random() * 180) + 10
    if (Math.abs(x - start.value.x) < 10 && Math.abs(y - start.value.y) < 10) continue
    if (Math.abs(x - end.value.x) < 10 && Math.abs(y - end.value.y) < 10) continue
    obs.push([x, y])
  }
  obstacles.value = obs
  path.value = []
  distance.value = 0
  status.value = 'idle'
}

function reset() { generateObstacles() }

async function runPathPlan() {
  loading.value = true
  status.value = 'running'
  try {
    const res = await pathPlanning({
      mapWidth: 600, mapHeight: 600,
      startX: start.value.x * 3, startY: start.value.y * 3,
      endX: end.value.x * 3, endY: end.value.y * 3,
      obstacles: obstacles.value.map(o => [o[0] * 3, o[1] * 3])
    })
    const data = res.data
    path.value = (data.path || []).map(p => [p[0], p[1]])
    distance.value = data.distance || 0
    status.value = 'done'
    ElMessage.success(`路径规划完成，长度=${distance.value}`)
  } catch (e) { ElMessage.error('规划失败: ' + (e.message || '')) }
  finally { loading.value = false }
}

generateObstacles()
</script>

<style scoped>
.page { height: 100%; display: flex; flex-direction: column; gap: 16px; }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-header h2 { margin: 0; font-size: 18px; color: #1a202c; }
.controls { display: flex; gap: 8px; }
.params { display: flex; gap: 24px; font-size: 13px; color: #555; }
.content { flex: 1; display: flex; gap: 16px; overflow: hidden; }
.vis-panel { flex: 1; background: #fff; border-radius: 8px; padding: 16px; display: flex; align-items: center; justify-content: center; }
.svg { width: 100%; max-width: 600px; height: auto; }
.info-panel { width: 280px; background: #fff; border-radius: 8px; padding: 16px; overflow-y: auto; }
.info-panel h3 { margin: 0 0 12px; font-size: 16px; }
.metric { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; }
.metric .label { color: #666; }
.metric .val { font-weight: 600; color: #333; }
.info-panel h4 { margin: 8px 0; }
</style>
