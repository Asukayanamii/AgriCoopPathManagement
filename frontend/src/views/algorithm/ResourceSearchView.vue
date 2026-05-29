<template>
  <div class="page">
    <div class="card">
      <div class="card-header">
        <h2>资源搜索算法</h2>
        <div class="controls">
          <el-button type="primary" @click="generateScene" :icon="Plus">生成场景</el-button>
          <el-button type="success" @click="runSearch" :icon="VideoPlay" :loading="loading">执行搜索</el-button>
          <el-button @click="reset" :icon="Refresh">重置</el-button>
        </div>
      </div>
      <div class="params">
        <span>资源(小车): {{resources.length}}</span>
        <span>目标(请求): {{targets.length}}</span>
      </div>
    </div>
    <div class="content">
      <div class="vis-panel">
        <svg :viewBox="'0 0 600 600'" class="svg">
          <rect x="0" y="0" width="600" height="600" fill="#f8f9fa" stroke="#dee2e6"/>
          <line v-for="(_, i) in 12" :key="'gv'+i" :x1="i*50" y1="0" :x2="i*50" y2="600" stroke="#eee"/>
          <line v-for="(_, i) in 12" :key="'gh'+i" x1="0" :y1="i*50" :x2="600" :y2="i*50" stroke="#eee"/>
          <template v-for="(r, i) in scaledResources" :key="'r'+i">
            <rect :x="r[0]-8" :y="r[1]-6" width="16" height="12" rx="2" :fill="r[2] ? '#22c55e' : '#d1d5db'" stroke="#333" stroke-width="1.5"/>
            <text :x="r[0]" :y="r[1]+4" text-anchor="middle" font-size="8" fill="#fff" font-weight="bold">T{{r[3]}}</text>
          </template>
          <template v-for="(t, i) in scaledTargets" :key="'t'+i">
            <polygon :points="t.poly" :fill="t.matched ? '#f59e0b' : '#ef4444'" stroke="#fff" stroke-width="2"/>
            <text :x="t.x+15" :y="t.y+4" font-size="11" :fill="t.matched ? '#f59e0b' : '#ef4444'" font-weight="bold">P{{i+1}}(需{{t.req}})</text>
          </template>
          <line v-for="(line, i) in matchLines" :key="'ln'+i" :x1="line[0]" :y1="line[1]" :x2="line[2]" :y2="line[3]" stroke="#f59e0b" stroke-width="1.5" stroke-dasharray="4,3" opacity="0.6"/>
        </svg>
      </div>
      <div class="info-panel">
        <h3>搜索结果</h3>
        <div class="metric"><span class="label">可用资源</span><span class="val">{{availableCount}}</span></div>
        <div class="metric"><span class="label">目标数</span><span class="val">{{targets.length}}</span></div>
        <div class="metric"><span class="label">成功匹配</span><span class="val" style="color:#22c55e">{{successCount}}</span></div>
        <div class="metric"><span class="label">状态</span><el-tag :type="status==='done'?'success':'info'" size="small">{{statusText}}</el-tag></div>
        <el-divider/>
        <h4>调度详情</h4>
        <div v-for="(r, i) in results" :key="'r'+i" class="result-item">
          <span style="font-weight:500">P{{i+1}}</span>
          <span v-if="r.status==='success'" style="color:#22c55e">→ 分配 {{r.resourceIds?.length || 0}} 辆</span>
          <span v-else style="color:#ef4444">→ 无可用资源</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, VideoPlay, Refresh } from '@element-plus/icons-vue'
import { resourceSearch } from '@/api/algorithm'

const loading = ref(false)
const status = ref('idle')
const resources = ref([])
const targets = ref([])
const results = ref([])
const scale = 3

const scaledResources = computed(() =>
  resources.value.map(r => [r.x * scale, r.y * scale, r.available, r.id]))
const scaledTargets = computed(() =>
  targets.value.map((t, i) => ({
    x: t.x * scale, y: t.y * scale,
    req: t.required,
    matched: results.value[i]?.status === 'success',
    poly: `${t.x*scale},${t.y*scale-10} ${t.x*scale-7},${t.y*scale+5} ${t.x*scale+7},${t.y*scale+5}`
  })))
const matchLines = computed(() => {
  const lines = []
  targets.value.forEach((t, ti) => {
    if (results.value[ti]?.status !== 'success') return
    const ids = results.value[ti]?.resourceIds || []
    ids.forEach(id => {
      const r = resources.value.find(r2 => r2.id === id)
      if (r) lines.push([r.x * scale, r.y * scale, t.x * scale, t.y * scale])
    })
  })
  return lines
})
const availableCount = computed(() => resources.value.filter(r => r.available).length)
const successCount = computed(() => results.value.filter(r => r.status === 'success').length)
const statusText = computed(() => ({ idle: '就绪', running: '计算中...', done: '完成' })[status.value])

function generateScene() {
  const res = []
  for (let i = 0; i < 15; i++) {
    res.push({ x: Math.floor(Math.random() * 180) + 10, y: Math.floor(Math.random() * 180) + 10, id: i + 1, available: true })
  }
  const tgt = []
  const demand = [2, 3, 2, 1]
  const tcenters = [[40,40], [140,50], [50,150], [150,150]]
  for (let i = 0; i < 4; i++) {
    tgt.push({ x: tcenters[i][0] + Math.floor(Math.random() * 15 - 7), y: tcenters[i][1] + Math.floor(Math.random() * 15 - 7), required: demand[i] })
  }
  resources.value = res
  targets.value = tgt
  results.value = []
  status.value = 'idle'
}

function reset() { generateScene() }

async function runSearch() {
  loading.value = true
  status.value = 'running'
  try {
    const res = await resourceSearch({
      mapWidth: 600, mapHeight: 600,
      resources: resources.value.map(r => ({ x: r.x * 3, y: r.y * 3, id: r.id, available: r.available })),
      targets: targets.value.map(t => ({ x: t.x * 3, y: t.y * 3, required: t.required }))
    })
    results.value = res.data?.searchResults || []
    status.value = 'done'
    const ok = results.value.filter(r => r.status === 'success').length
    ElMessage.success(`搜索完成，${ok}/${targets.value.length} 个目标成功匹配`)
  } catch (e) { ElMessage.error('搜索失败: ' + (e.message || '')) }
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
.params { display: flex; gap: 24px; font-size: 13px; color: #555; }
.content { flex: 1; display: flex; gap: 16px; overflow: hidden; }
.vis-panel { flex: 1; background: #fff; border-radius: 8px; padding: 16px; display: flex; align-items: center; justify-content: center; }
.svg { width: 100%; max-width: 600px; height: auto; }
.info-panel { width: 280px; background: #fff; border-radius: 8px; padding: 16px; overflow-y: auto; }
.info-panel h3 { margin: 0 0 12px; font-size: 16px; }
.metric { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; }
.metric .label { color: #666; }
.metric .val { font-weight: 600; color: #333; }
.result-item { padding: 4px 0; font-size: 13px; display: flex; gap: 6px; }
</style>
