<template>
  <div class="page">
    <div class="card">
      <div class="card-header">
        <h2>K-means 聚类算法</h2>
        <div class="controls">
          <el-button type="primary" @click="generatePoints" :icon="Plus">生成随机点</el-button>
          <el-button type="success" @click="runCluster" :icon="VideoPlay" :loading="loading">执行聚类</el-button>
          <el-button @click="reset" :icon="Refresh">重置</el-button>
        </div>
      </div>
      <div class="params">
        <span>点数: <el-slider v-model="pointCount" :min="20" :max="150" :step="10" style="width:160px;display:inline-block;margin:0 10px"/>{{ pointCount }}</span>
        <span>最小聚类规模: <el-slider v-model="spaceCluster" :min="3" :max="20" :step="1" style="width:120px;display:inline-block;margin:0 10px"/>{{ spaceCluster }}</span>
      </div>
    </div>
    <div class="content">
      <div class="vis-panel">
        <svg :viewBox="'0 0 600 600'" class="svg" ref="svgRef">
          <rect x="0" y="0" width="600" height="600" fill="#f8f9fa" stroke="#dee2e6"/>
          <line v-for="(_, i) in 12" :key="'gv'+i" :x1="i*50" y1="0" :x2="i*50" y2="600" stroke="#eee"/>
          <line v-for="(_, i) in 12" :key="'gh'+i" x1="0" :y1="i*50" :x2="600" :y2="i*50" stroke="#eee"/>
          <g v-for="(pt, i) in scaledPoints" :key="'pt'+i">
            <circle :cx="pt[0]" :cy="pt[1]" r="5" :fill="getColor(pt[2])" stroke="#fff" stroke-width="1.5"/>
          </g>
          <g v-for="(c, i) in centers" :key="'c'+i">
            <circle :cx="c[0]" :cy="c[1]" r="10" :fill="colors[i%colors.length]" stroke="#333" stroke-width="2.5"/>
            <text :x="c[0]" :y="c[1]+20" text-anchor="middle" font-size="12" font-weight="bold" fill="#333">C{{i+1}}</text>
          </g>
        </svg>
      </div>
      <div class="info-panel">
        <h3>聚类结果</h3>
        <div class="metric"><span class="label">聚类数</span><span class="val">{{clusterCount}}</span></div>
        <div class="metric"><span class="label">总点数</span><span class="val">{{points.length}}</span></div>
        <div class="metric"><span class="label">状态</span><el-tag :type="status === 'done' ? 'success' : 'info'" size="small">{{statusText}}</el-tag></div>
        <el-divider/>
        <h4>聚类详情</h4>
        <div v-for="(g, i) in groups" :key="'g'+i" class="cluster-item">
          <span class="dot" :style="{background: colors[i%colors.length]}"></span>
          <span>聚类{{i+1}}: {{g.length}}个点</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, VideoPlay, Refresh } from '@element-plus/icons-vue'
import { cluster } from '@/api/algorithm'

const colors = ['#e74c3c','#3498db','#2ecc71','#f39c12','#9b59b6','#1abc9c','#e67e22','#2c3e50']
const pointCount = ref(60)
const spaceCluster = ref(8)
const points = ref([])
const groups = ref([])
const centers = ref([])
const clusterCount = ref(0)
const status = ref('idle')
const loading = ref(false)

const scaledPoints = computed(() => points.value.map(p => [p[0]*3, p[1]*3, p[2]]))
const statusText = computed(() => ({ idle: '就绪', running: '计算中...', done: '完成' })[status.value])

const getColor = (meanid) => meanid > 0 ? colors[(meanid-1) % colors.length] : '#adb5bd'

function generatePoints() {
  const pts = []
  for (let i = 0; i < pointCount.value; i++) {
    pts.push([Math.floor(Math.random() * 180) + 10, Math.floor(Math.random() * 180) + 10, 0])
  }
  points.value = pts
  groups.value = []
  centers.value = []
  clusterCount.value = 0
  status.value = 'idle'
}

function reset() { generatePoints() }

async function runCluster() {
  if (points.value.length < 5) { ElMessage.warning('点太少'); return }
  loading.value = true
  status.value = 'running'
  try {
    const res = await cluster({
      points: points.value.map(p => [p[0], p[1]]),
      spaceCluster: spaceCluster.value,
      deviation: 2, iterationCount: 20
    })
    const data = res.data
    groups.value = data.groupedClusters || []
    centers.value = (data.clusterCenters || []).map((cid, i) => {
      const g = groups.value[i] || []
      const xs = g.map(p => p[0]), ys = g.map(p => p[1])
      return [Math.round(xs.reduce((a,b)=>a+b,0)/xs.length * 3), Math.round(ys.reduce((a,b)=>a+b,0)/ys.length * 3)]
    })
    clusterCount.value = data.clusterCount || 0
    points.value = (data.clusterResult || []).map(p => [p[0], p[1], p[2]])
    status.value = 'done'
    ElMessage.success(`聚类完成，共${clusterCount.value}个聚类`)
  } catch (e) { ElMessage.error('聚类失败: ' + (e.message || '')) }
  finally { loading.value = false }
}

generatePoints()
</script>

<style scoped>
.page { height: 100%; display: flex; flex-direction: column; gap: 16px; }
.card { background: #fff; border-radius: 8px; padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-header h2 { margin: 0; font-size: 18px; color: #1a202c; }
.controls { display: flex; gap: 8px; }
.params { display: flex; gap: 24px; align-items: center; font-size: 13px; color: #555; }
.content { flex: 1; display: flex; gap: 16px; overflow: hidden; }
.vis-panel { flex: 1; background: #fff; border-radius: 8px; padding: 16px; display: flex; align-items: center; justify-content: center; }
.svg { width: 100%; max-width: 600px; height: auto; }
.info-panel { width: 280px; background: #fff; border-radius: 8px; padding: 16px; overflow-y: auto; }
.info-panel h3 { margin: 0 0 12px; font-size: 16px; }
.metric { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; }
.metric .label { color: #666; }
.metric .val { font-weight: 600; color: #333; }
.cluster-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; }
.dot { width: 12px; height: 12px; border-radius: 50%; display: inline-block; flex-shrink: 0; }
</style>
