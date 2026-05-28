<template>
  <div class="clustering-container">
    <div class="header-controls">
      <div class="left-controls">
        <!-- 模式切换 -->
        <el-switch
          v-model="demoMode"
          active-text="演示模式"
          inactive-text="真实模式"
          style="margin-right: 16px;"
          @change="toggleMode"
        />

        <!-- 聚类算法选择 -->
        <el-select v-model="selectedAlgorithm" placeholder="选择聚类算法" style="width: 180px; margin-right: 16px;">
          <el-option label="K-Means聚类" value="kmeans" />
          <el-option label="DBSCAN聚类" value="dbscan" />
          <el-option label="层次聚类" value="hierarchical" />
          <el-option label="均值漂移聚类" value="meanshift" />
        </el-select>

        <!-- 操作按钮 -->
        <el-button type="primary" :icon="VideoPlay" @click="runClustering" :loading="isCalculating">
          执行聚类
        </el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
        <el-button :icon="Download" @click="exportResult">导出结果</el-button>
      </div>

      <div class="right-controls">
        <!-- 参数配置 -->
        <el-button :icon="Setting" @click="showParameterDialog = true">
          算法参数
        </el-button>
      </div>
    </div>

    <div class="clustering-content">
      <!-- 左侧可视化区域 -->
      <div class="visualization-area">
        <div class="canvas-container" ref="canvasContainer">
          <canvas ref="canvas" :width="canvasWidth" :height="canvasHeight"></canvas>

          <!-- 图例 -->
          <div class="legend">
            <div class="legend-item" v-for="cluster in clusters" :key="cluster.id">
              <div class="legend-color" :style="{ backgroundColor: cluster.color }"></div>
              <span>聚类 {{ cluster.id }} ({{ cluster.nodes.length }}个点)</span>
            </div>
            <div class="legend-item" v-if="clusters.length === 0">
              <div class="legend-color noise-color"></div>
              <span>噪声点</span>
            </div>
          </div>

          <!-- 统计信息 -->
          <div class="stats-info">
            <div>总点数: {{ totalPoints }}</div>
            <div>聚类数: {{ clusters.length }}</div>
            <div>轮廓系数: {{ silhouetteScore.toFixed(3) }}</div>
          </div>
        </div>

        <!-- 控制面板 -->
        <div class="control-panel">
          <div class="parameter-controls">
            <div class="slider-control">
              <span>点数:</span>
              <el-slider v-model="pointCount" :min="10" :max="200" :step="10" style="width: 200px; margin: 0 16px;" />
              <span>{{ pointCount }}</span>
            </div>

            <div class="slider-control" v-if="selectedAlgorithm === 'kmeans'">
              <span>聚类数 (K):</span>
              <el-slider v-model="parameters.k" :min="2" :max="10" :step="1" style="width: 200px; margin: 0 16px;" />
              <span>{{ parameters.k }}</span>
            </div>

            <div class="slider-control" v-if="selectedAlgorithm === 'dbscan'">
              <span>邻域半径 (ε):</span>
              <el-slider v-model="parameters.epsilon" :min="0.1" :max="5" :step="0.1" style="width: 200px; margin: 0 16px;" />
              <span>{{ parameters.epsilon.toFixed(1) }}</span>
            </div>

            <div class="slider-control" v-if="selectedAlgorithm === 'dbscan'">
              <span>最小点数 (MinPts):</span>
              <el-slider v-model="parameters.minPts" :min="2" :max="10" :step="1" style="width: 200px; margin: 0 16px;" />
              <span>{{ parameters.minPts }}</span>
            </div>

            <el-button :icon="Plus" @click="generateRandomPoints">生成随机点</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧结果面板 -->
      <div class="result-panel">
        <div class="panel-header">
          <h3>聚类结果</h3>
          <el-button :icon="RefreshRight" @click="refreshResult" :disabled="clusters.length === 0">重新计算</el-button>
        </div>

        <!-- 聚类统计 -->
        <div class="cluster-stats" v-if="clusters.length > 0">
          <div class="metric-card">
            <div class="metric-label">聚类数量</div>
            <div class="metric-value">{{ clusters.length }}</div>
            <div class="metric-unit">个</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">平均大小</div>
            <div class="metric-value">{{ avgClusterSize.toFixed(1) }}</div>
            <div class="metric-unit">点/聚类</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">轮廓系数</div>
            <div class="metric-value">{{ silhouetteScore.toFixed(3) }}</div>
            <div class="metric-progress">
              <el-progress :percentage="silhouetteScore * 100" :show-text="false" />
            </div>
          </div>
          <div class="metric-card">
            <div class="metric-label">计算时间</div>
            <div class="metric-value">{{ calculationTime }}</div>
            <div class="metric-unit">毫秒</div>
          </div>
        </div>

        <!-- 聚类详情 -->
        <div class="cluster-details">
          <h4>聚类详情</h4>
          <el-table :data="clusters" size="small" height="200">
            <el-table-column label="聚类" width="80">
              <template #default="{ row }">
                <div class="cluster-color" :style="{ backgroundColor: row.color }"></div>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="nodes.length" label="点数" width="80" />
            <el-table-column label="中心点" width="120">
              <template #default="{ row }">
                ({{ row.centroid[0].toFixed(1) }}, {{ row.centroid[1].toFixed(1) }})
              </template>
            </el-table-column>
            <el-table-column label="直径" width="80">
              <template #default="{ row }">{{ row.diameter.toFixed(1) }}</template>
            </el-table-column>
            <el-table-column label="密度" width="80">
              <template #default="{ row }">{{ row.density.toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 算法详情 -->
        <div class="algorithm-details">
          <h4>算法详情</h4>
          <div class="detail-item">
            <span class="detail-label">当前算法:</span>
            <span class="detail-value">{{ algorithmNames[selectedAlgorithm] }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">状态:</span>
            <el-tag :type="isCalculating ? 'warning' : 'success'" size="small">
              {{ isCalculating ? '计算中...' : '就绪' }}
            </el-tag>
          </div>
          <div class="detail-item">
            <span class="detail-label">当前参数:</span>
            <span class="detail-value">{{ getParameterSummary }}</span>
          </div>
        </div>

        <!-- 历史记录 -->
        <div class="history-section">
          <h4>计算历史</h4>
          <el-table :data="history" size="small" height="150">
            <el-table-column prop="algorithm" label="算法" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ algorithmNames[row.algorithm] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="clusters" label="聚类数" width="80" />
            <el-table-column prop="silhouetteScore" label="轮廓系数" width="100">
              <template #default="{ row }">{{ row.silhouetteScore.toFixed(3) }}</template>
            </el-table-column>
            <el-table-column prop="timestamp" label="时间" width="120" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="loadHistoryResult(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 参数配置对话框 -->
    <el-dialog v-model="showParameterDialog" title="聚类算法参数配置" width="500">
      <el-form :model="parameters" label-width="120px">
        <el-form-item label="算法" v-if="selectedAlgorithm === 'kmeans'">
          <el-select v-model="parameters.kmeansMethod">
            <el-option label="标准K-Means" value="standard" />
            <el-option label="K-Means++" value="plusplus" />
            <el-option label="Mini Batch K-Means" value="minibatch" />
          </el-select>
        </el-form-item>

        <el-form-item label="最大迭代次数" v-if="selectedAlgorithm === 'kmeans'">
          <el-input-number v-model="parameters.maxIterations" :min="10" :max="1000" />
        </el-form-item>

        <el-form-item label="容忍度" v-if="selectedAlgorithm === 'kmeans'">
          <el-input-number v-model="parameters.tolerance" :min="0.0001" :max="0.1" :step="0.0001" />
        </el-form-item>

        <el-form-item label="距离度量" v-if="selectedAlgorithm === 'kmeans' || selectedAlgorithm === 'hierarchical'">
          <el-select v-model="parameters.distanceMetric">
            <el-option label="欧几里得距离" value="euclidean" />
            <el-option label="曼哈顿距离" value="manhattan" />
            <el-option label="余弦相似度" value="cosine" />
          </el-select>
        </el-form-item>

        <el-form-item label="链接方法" v-if="selectedAlgorithm === 'hierarchical'">
          <el-select v-model="parameters.linkage">
            <el-option label="单链接" value="single" />
            <el-option label="全链接" value="complete" />
            <el-option label="平均链接" value="average" />
          </el-select>
        </el-form-item>

        <el-form-item label="带宽" v-if="selectedAlgorithm === 'meanshift'">
          <el-input-number v-model="parameters.bandwidth" :min="0.1" :max="10" :step="0.1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showParameterDialog = false">取消</el-button>
          <el-button type="primary" @click="saveParameters">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  VideoPlay,
  Refresh,
  Setting,
  Download,
  Plus,
  RefreshRight
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode, algorithmService } from '@/services/dataService'

// 模式控制
const demoMode = ref(getCurrentMode())

// 算法选择
const selectedAlgorithm = ref('kmeans')
const algorithmNames = {
  'kmeans': 'K-Means聚类',
  'dbscan': 'DBSCAN聚类',
  'hierarchical': '层次聚类',
  'meanshift': '均值漂移聚类'
}

// 计算状态
const isCalculating = ref(false)
const clusters = ref([])
const history = ref([])
const calculationTime = ref(0)

// 参数配置
const showParameterDialog = ref(false)
const parameters = reactive({
  // K-Means参数
  k: 3,
  kmeansMethod: 'plusplus',
  maxIterations: 100,
  tolerance: 0.0001,

  // DBSCAN参数
  epsilon: 1.5,
  minPts: 3,

  // 通用参数
  distanceMetric: 'euclidean',

  // 层次聚类参数
  linkage: 'average',

  // 均值漂移参数
  bandwidth: 2.0
})

// 点数据
const pointCount = ref(50)
const points = ref([])

// 画布
const canvas = ref(null)
const canvasContainer = ref(null)
const canvasWidth = ref(600)
const canvasHeight = ref(600)
const ctx = ref(null)

// 预生成的测试聚类结果
const mockClusters = {
  'kmeans': [
    { id: 1, nodes: [0, 1, 2, 3, 4], centroid: [150, 250], color: '#ff6b6b', diameter: 85.3, density: 0.45 },
    { id: 2, nodes: [5, 6, 7, 8, 9], centroid: [450, 350], color: '#4ecdc4', diameter: 92.1, density: 0.38 },
    { id: 3, nodes: [10, 11, 12, 13, 14], centroid: [750, 150], color: '#45b7d1', diameter: 78.6, density: 0.52 }
  ],
  'dbscan': [
    { id: 1, nodes: [0, 1, 2, 3, 4, 5], centroid: [200, 300], color: '#ff6b6b', diameter: 120.5, density: 0.32 },
    { id: 2, nodes: [6, 7, 8, 9, 10], centroid: [600, 200], color: '#4ecdc4', diameter: 95.7, density: 0.41 },
    { id: 3, nodes: [11, 12, 13], centroid: [400, 500], color: '#45b7d1', diameter: 65.2, density: 0.28 }
  ],
  'hierarchical': [
    { id: 1, nodes: [0, 1, 2], centroid: [180, 280], color: '#ff6b6b', diameter: 72.4, density: 0.55 },
    { id: 2, nodes: [3, 4, 5, 6], centroid: [420, 380], color: '#4ecdc4', diameter: 88.9, density: 0.42 },
    { id: 3, nodes: [7, 8, 9], centroid: [680, 180], color: '#45b7d1', diameter: 69.3, density: 0.51 },
    { id: 4, nodes: [10, 11, 12], centroid: [300, 520], color: '#9d65c9', diameter: 76.8, density: 0.47 }
  ],
  'meanshift': [
    { id: 1, nodes: [0, 1, 2, 3, 4, 5], centroid: [250, 320], color: '#ff6b6b', diameter: 135.2, density: 0.29 },
    { id: 2, nodes: [6, 7, 8, 9], centroid: [550, 250], color: '#4ecdc4', diameter: 102.4, density: 0.36 },
    { id: 3, nodes: [10, 11, 12], centroid: [380, 480], color: '#45b7d1', diameter: 81.6, density: 0.44 }
  ]
}

// 计算属性
const totalPoints = computed(() => points.value.length)
const silhouetteScore = computed(() => {
  if (clusters.value.length <= 1) return 0
  // 简单计算轮廓系数（演示用）
  const avgScores = clusters.value.map(cluster => {
    const size = cluster.nodes.length
    if (size <= 1) return 0
    // 模拟轮廓系数计算
    return 0.7 + (Math.random() * 0.2) // 0.7-0.9之间
  })
  return avgScores.reduce((sum, score) => sum + score, 0) / avgScores.length
})
const avgClusterSize = computed(() => {
  if (clusters.value.length === 0) return 0
  return totalPoints.value / clusters.value.length
})
const getParameterSummary = computed(() => {
  switch (selectedAlgorithm.value) {
    case 'kmeans':
      return `K=${parameters.k}, 方法=${parameters.kmeansMethod}`
    case 'dbscan':
      return `ε=${parameters.epsilon.toFixed(1)}, MinPts=${parameters.minPts}`
    case 'hierarchical':
      return `链接=${parameters.linkage}, 度量=${parameters.distanceMetric}`
    case 'meanshift':
      return `带宽=${parameters.bandwidth.toFixed(1)}`
    default:
      return '无参数'
  }
})

// 初始化
onMounted(() => {
  initCanvas()
  generateRandomPoints()
})

// 初始化画布
function initCanvas() {
  if (!canvas.value) return
  ctx.value = canvas.value.getContext('2d')
  redraw()
}

// 生成随机点
function generateRandomPoints() {
  points.value = []
  const margin = 50

  for (let i = 0; i < pointCount.value; i++) {
    points.value.push({
      id: i,
      x: margin + Math.random() * (canvasWidth.value - 2 * margin),
      y: margin + Math.random() * (canvasHeight.value - 2 * margin)
    })
  }

  clusters.value = []
  redraw()
}

// 重绘画布
function redraw() {
  if (!ctx.value) return

  ctx.value.clearRect(0, 0, canvasWidth.value, canvasHeight.value)

  // 绘制网格背景
  drawGrid()

  // 绘制点
  points.value.forEach(point => {
    // 确定点的颜色：如果在聚类中，使用聚类颜色，否则用灰色
    let color = '#9ca3af'
    let radius = 4

    for (const cluster of clusters.value) {
      if (cluster.nodes.includes(point.id)) {
        color = cluster.color
        radius = 5
        break
      }
    }

    // 绘制点
    ctx.value.fillStyle = color
    ctx.value.beginPath()
    ctx.value.arc(point.x, point.y, radius, 0, Math.PI * 2)
    ctx.value.fill()

    // 绘制点轮廓
    ctx.value.strokeStyle = '#fff'
    ctx.value.lineWidth = 1
    ctx.value.stroke()
  })

  // 绘制聚类中心
  clusters.value.forEach(cluster => {
    const [cx, cy] = cluster.centroid

    // 绘制中心点
    ctx.value.fillStyle = cluster.color
    ctx.value.beginPath()
    ctx.value.arc(cx, cy, 8, 0, Math.PI * 2)
    ctx.value.fill()

    // 绘制中心点轮廓
    ctx.value.strokeStyle = '#fff'
    ctx.value.lineWidth = 2
    ctx.value.stroke()

    // 绘制聚类边界（圆形）
    ctx.value.strokeStyle = cluster.color
    ctx.value.lineWidth = 1
    ctx.value.setLineDash([5, 5])
    ctx.value.beginPath()
    ctx.value.arc(cx, cy, cluster.diameter / 2, 0, Math.PI * 2)
    ctx.value.stroke()
    ctx.value.setLineDash([])
  })
}

// 绘制网格
function drawGrid() {
  ctx.value.strokeStyle = '#f0f0f0'
  ctx.value.lineWidth = 1

  // 垂直网格线
  for (let x = 0; x <= canvasWidth.value; x += 50) {
    ctx.value.beginPath()
    ctx.value.moveTo(x, 0)
    ctx.value.lineTo(x, canvasHeight.value)
    ctx.value.stroke()
  }

  // 水平网格线
  for (let y = 0; y <= canvasHeight.value; y += 50) {
    ctx.value.beginPath()
    ctx.value.moveTo(0, y)
    ctx.value.lineTo(canvasWidth.value, y)
    ctx.value.stroke()
  }
}

// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
}

// 执行聚类
async function runClustering() {
  if (isCalculating.value) return

  isCalculating.value = true
  const startTime = Date.now()

  try {
    let result

    if (demoMode.value) {
      // 演示模式：使用模拟结果
      await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟计算延迟

      // 生成模拟聚类结果
      const mockResult = mockClusters[selectedAlgorithm.value] || mockClusters.kmeans
      result = {
        clusters: mockResult,
        stats: {
          totalClusters: mockResult.length,
          averageClusterSize: avgClusterSize.value,
          silhouetteScore: silhouetteScore.value
        }
      }
    } else {
      // 真实模式：调用API
      const response = await algorithmService.calculateClustering(
        points.value.map(p => ({ x: p.x, y: p.y })),
        {
          algorithm: selectedAlgorithm.value,
          ...parameters
        }
      )
      result = response.data
    }

    // 更新聚类结果
    clusters.value = result.clusters.map((cluster, index) => ({
      ...cluster,
      id: index + 1,
      color: cluster.color || getClusterColor(index)
    }))

    calculationTime.value = Date.now() - startTime

    // 添加到历史记录
    const historyItem = {
      id: Date.now(),
      algorithm: selectedAlgorithm.value,
      clusters: clusters.value.length,
      silhouetteScore: silhouetteScore.value,
      timestamp: new Date().toLocaleTimeString(),
      parameters: { ...parameters }
    }

    history.value.unshift(historyItem)
    if (history.value.length > 10) {
      history.value.pop()
    }

    // 重绘画布
    redraw()

    ElMessage.success('聚类计算完成')
  } catch (error) {
    console.error('聚类计算失败:', error)
    ElMessage.error('聚类计算失败，请重试')
  } finally {
    isCalculating.value = false
  }
}

// 获取聚类颜色
function getClusterColor(index) {
  const colors = ['#ff6b6b', '#4ecdc4', '#45b7d1', '#9d65c9', '#ffd166', '#06d6a0', '#118ab2', '#ef476f']
  return colors[index % colors.length]
}

// 重置
function reset() {
  clusters.value = []
  redraw()
  ElMessage.info('已重置聚类结果')
}

// 保存参数
function saveParameters() {
  showParameterDialog.value = false
  ElMessage.success('参数已保存')
}

// 导出结果
function exportResult() {
  if (clusters.value.length === 0) {
    ElMessage.warning('没有可导出的聚类结果')
    return
  }

  const exportData = {
    algorithm: selectedAlgorithm.value,
    parameters: parameters,
    points: points.value,
    clusters: clusters.value,
    stats: {
      totalClusters: clusters.value.length,
      averageClusterSize: avgClusterSize.value,
      silhouetteScore: silhouetteScore.value,
      calculationTime: calculationTime.value
    },
    timestamp: new Date().toISOString()
  }

  const dataStr = JSON.stringify(exportData, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr)
  const exportFileName = `聚类结果_${selectedAlgorithm.value}_${new Date().getTime()}.json`

  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', exportFileName)
  linkElement.click()

  ElMessage.success('聚类结果已导出')
}

// 刷新结果
function refreshResult() {
  if (clusters.value.length === 0) return
  runClustering()
}

// 加载历史结果
function loadHistoryResult(historyItem) {
  // 更新参数
  Object.assign(parameters, historyItem.parameters)
  selectedAlgorithm.value = historyItem.algorithm

  // 重新计算聚类（简化的演示）
  ElMessage.info('已加载历史参数，请重新计算聚类')
}

// 监听点数变化
watch(pointCount, () => {
  generateRandomPoints()
})

// 监听算法变化，重置聚类
watch(selectedAlgorithm, () => {
  clusters.value = []
})
</script>

<style scoped>
.clustering-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.left-controls {
  display: flex;
  align-items: center;
}

.right-controls {
  display: flex;
  gap: 8px;
}

.clustering-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #f5f5f5;
}

.visualization-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px;
}

.canvas-container {
  position: relative;
  flex: 1;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

canvas {
  display: block;
  background: #fff;
}

.legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border: 1px solid #e5e7eb;
  max-width: 200px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #4b5563;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 2px;
}

.noise-color {
  background-color: #9ca3af;
}

.stats-info {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  color: #4b5563;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.control-panel {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-top: 16px;
  border: 1px solid #e5e7eb;
}

.parameter-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 24px;
}

.slider-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-panel {
  width: 400px;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
}

.cluster-stats {
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.metric-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  text-align: center;
}

.metric-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}

.metric-unit {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.metric-progress {
  margin-top: 8px;
}

.cluster-details {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.cluster-details h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.cluster-color {
  width: 16px;
  height: 16px;
  border-radius: 2px;
  display: inline-block;
}

.algorithm-details {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.algorithm-details h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.detail-label {
  font-size: 13px;
  color: #6b7280;
}

.detail-value {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
}

.history-section {
  flex: 1;
  padding: 16px;
  overflow: hidden;
}

.history-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .clustering-content {
    flex-direction: column;
  }

  .result-panel {
    width: 100%;
    height: 40%;
  }

  .header-controls {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .left-controls, .right-controls {
    width: 100%;
    justify-content: center;
  }

  .parameter-controls {
    flex-direction: column;
    gap: 16px;
  }

  .slider-control {
    width: 100%;
  }
}
</style>