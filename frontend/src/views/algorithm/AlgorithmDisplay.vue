<template>
  <div class="algorithm-container">
    <!-- 工作流状态栏 -->
    <div class="workflow-status" v-if="workflowStore.currentStep === workflowStore.STEPS.ALGORITHM_SELECTION">
      <div class="workflow-step-info">
        <el-tag :type="workflowStatus === 'current' ? 'primary' : workflowStatus === 'completed' ? 'success' : 'info'">
          {{ workflowStore.currentStepLabel }}
        </el-tag>
        <span class="workflow-message">{{ validation.message }}</span>
        <el-button
          v-if="canProceed"
          type="primary"
          size="small"
          @click="workflowStore.next()"
        >
          进入下一步：{{ workflowStore.nextStep ? workflowStore.STEP_LABELS[workflowStore.nextStep] : '完成' }}
        </el-button>
        <el-button v-else type="info" size="small" disabled>
          请先{{ validation.missing.join('、') || '完成数据准备' }}
        </el-button>
      </div>
      <div class="workflow-progress">
        <el-progress
          :percentage="workflowStore.progressPercentage"
          :stroke-width="8"
          :show-text="false"
        />
        <div class="progress-text">进度: {{ workflowStore.progressPercentage }}%</div>
      </div>
    </div>

    <div class="header-controls">
      <div class="left-controls">
        <!-- 算法选择 -->
        <el-select v-model="selectedAlgorithm" placeholder="选择算法" style="width: 180px; margin-right: 16px;">
          <el-option label="A*算法" value="a-star" />
          <el-option label="Dijkstra算法" value="dijkstra" />
          <el-option label="广度优先搜索" value="bfs" />
          <el-option label="深度优先搜索" value="dfs" />
        </el-select>

        <!-- 模式切换 -->
        <el-switch
          v-model="demoMode"
          active-text="演示模式"
          inactive-text="真实模式"
          style="margin-right: 16px;"
          @change="toggleMode"
        />

        <!-- 操作按钮 -->
        <el-button type="primary" :icon="VideoPlay" @click="runAlgorithm" :loading="isCalculating">
          运行算法
        </el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>

      <div class="right-controls">
        <!-- 参数配置 -->
        <el-button :icon="Setting" @click="showParameterDialog = true">
          算法参数
        </el-button>
        <el-button :icon="Download" @click="exportResult">导出结果</el-button>
      </div>
    </div>

    <div class="algorithm-content">
      <!-- 左侧可视化区域 - 地图图片展示 -->
      <div class="visualization-area">
        <div class="map-container" ref="mapContainer">
          <!-- 地图图片 -->
          <img
            v-if="mapStore.currentMap?.url"
            :src="mapStore.currentMap.url"
            :alt="mapStore.currentMap.name"
            class="map-image"
            ref="mapImage"
            @load="onMapImageLoad"
          />

          <!-- 覆盖层画布，用于绘制节点和路径 -->
          <canvas
            ref="overlayCanvas"
            class="overlay-canvas"
            :width="canvasWidth"
            :height="canvasHeight"
          ></canvas>

          <!-- 地图信息 -->
          <div class="map-info">
            地图: {{ mapStore.currentMap?.name || '未选择地图' }} |
            尺寸: {{ mapStore.currentMap?.width || 0 }}×{{ mapStore.currentMap?.height || 0 }} |
            节点: {{ nodes.length }} |
            障碍物: {{ obstacles.length }}
          </div>

          <!-- 图例 -->
          <div class="legend">
            <div class="legend-item">
              <div class="legend-color start-color"></div>
              <span>起点</span>
            </div>
            <div class="legend-item">
              <div class="legend-color end-color"></div>
              <span>终点</span>
            </div>
            <div class="legend-item">
              <div class="legend-color obstacle-color"></div>
              <span>障碍物</span>
            </div>
            <div class="legend-item">
              <div class="legend-color path-color"></div>
              <span>路径</span>
            </div>
            <div class="legend-item">
              <div class="legend-color waypoint-color"></div>
              <span>路径点</span>
            </div>
          </div>
        </div>

        <!-- 控制按钮 -->
        <div class="visualization-controls">
          <div class="slider-control">
            <span>缩放:</span>
            <el-slider v-model="zoomLevel" :min="0.5" :max="2" :step="0.1" style="width: 200px; margin: 0 16px;" />
            <span>{{ (zoomLevel * 100).toFixed(0) }}%</span>
          </div>
          <div class="data-source-info">
            <span>数据来源: {{ mapStore.currentMap?.name || '未选择地图' }}</span>
            <span>节点数量: {{ rawNodes.length }}</span>
            <span>起点: {{ startNode ? '已设置' : '未设置' }}</span>
            <span>终点: {{ endNode ? '已设置' : '未设置' }}</span>
            <span>当前算法: {{ algorithmNames[selectedAlgorithm] }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧结果面板 -->
      <div class="result-panel">
        <div class="panel-header">
          <h3>算法结果</h3>
          <el-button :icon="RefreshRight" @click="refreshResult" :disabled="!lastResult">重新计算</el-button>
        </div>

        <!-- 结果指标 -->
        <div class="result-metrics" v-if="lastResult">
          <div class="metric-card">
            <div class="metric-label">路径长度</div>
            <div class="metric-value">{{ lastResult.distance.toFixed(2) }}</div>
            <div class="metric-unit">米</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">计算时间</div>
            <div class="metric-value">{{ lastResult.timeMs }}</div>
            <div class="metric-unit">毫秒</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">访问节点</div>
            <div class="metric-value">{{ lastResult.nodesVisited }}</div>
            <div class="metric-unit">个</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">覆盖率</div>
            <div class="metric-value">{{ (lastResult.coverage * 100).toFixed(1) }}%</div>
            <div class="metric-progress">
              <el-progress :percentage="lastResult.coverage * 100" :show-text="false" />
            </div>
          </div>
        </div>

        <!-- 算法详情 -->
        <div class="algorithm-details">
          <h4>算法详情</h4>
          <div class="detail-item">
            <span class="detail-label">当前算法:</span>
            <span class="detail-value">{{ algorithmNames[selectedAlgorithm] }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">启发函数:</span>
            <span class="detail-value">{{ parameters.heuristic === 'euclidean' ? '欧几里得距离' : '曼哈顿距离' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">权重:</span>
            <span class="detail-value">{{ parameters.weight }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">状态:</span>
            <el-tag :type="isCalculating ? 'warning' : 'success'" size="small">
              {{ isCalculating ? '计算中...' : '就绪' }}
            </el-tag>
          </div>
        </div>

        <!-- 历史记录 -->
        <div class="history-section">
          <h4>计算历史</h4>
          <el-table :data="history" size="small" height="200">
            <el-table-column prop="algorithm" label="算法" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ algorithmNames[row.algorithm] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="distance" label="距离" width="80">
              <template #default="{ row }">{{ row.distance.toFixed(1) }}</template>
            </el-table-column>
            <el-table-column prop="timeMs" label="时间" width="80">
              <template #default="{ row }">{{ row.timeMs }}ms</template>
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
    <el-dialog v-model="showParameterDialog" title="算法参数配置" width="500">
      <el-form :model="parameters" label-width="120px">
        <el-form-item label="启发函数">
          <el-select v-model="parameters.heuristic">
            <el-option label="欧几里得距离" value="euclidean" />
            <el-option label="曼哈顿距离" value="manhattan" />
            <el-option label="切比雪夫距离" value="chebyshev" />
          </el-select>
        </el-form-item>
        <el-form-item label="启发式权重">
          <el-slider v-model="parameters.weight" :min="0.1" :max="2" :step="0.1" />
          <span style="margin-left: 10px;">{{ parameters.weight }}</span>
        </el-form-item>
        <el-form-item label="允许对角线">
          <el-switch v-model="parameters.allowDiagonal" />
        </el-form-item>
        <el-form-item label="障碍物惩罚">
          <el-input-number v-model="parameters.obstaclePenalty" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="最大迭代次数">
          <el-input-number v-model="parameters.maxIterations" :min="100" :max="10000" />
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
import { ref, reactive, onMounted, watch, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  VideoPlay,
  Refresh,
  Setting,
  Download,
  Plus,
  RefreshRight
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode } from '@/services/dataService'
import { useMapStore } from '@/stores/mapStore'
import { useAlgorithmStore } from '@/stores/algorithmStore'
import { useWorkflowStore } from '@/stores/workflowStore'

// Store初始化
const mapStore = useMapStore()
const algorithmStore = useAlgorithmStore()
const workflowStore = useWorkflowStore()

// 工作流状态
const workflowStatus = computed(() => workflowStore.getStepStatus(workflowStore.STEPS.ALGORITHM_SELECTION))
const canProceed = computed(() => workflowStore.canProceed)
const validation = computed(() => workflowStore.getCurrentValidation())

// 算法配置 - 与store同步
const selectedAlgorithm = computed({
  get: () => algorithmStore.algorithmType,
  set: (value) => algorithmStore.setAlgorithmType(value)
})

const algorithmNames = {
  'a-star': 'A*算法',
  'dijkstra': 'Dijkstra算法',
  'bfs': '广度优先搜索',
  'dfs': '深度优先搜索'
}

// 模式控制
const demoMode = ref(getCurrentMode())

// 计算状态
const isCalculating = computed(() => algorithmStore.isLoading)
const lastResult = computed(() => algorithmStore.currentResult)
const history = computed(() => algorithmStore.historyResults)

// 参数配置 - 与store同步
const showParameterDialog = ref(false)
const parameters = computed({
  get: () => algorithmStore.algorithmParams,
  set: (value) => algorithmStore.updateAlgorithmParams(value)
})

// 地图数据 - 从store获取
const rawNodes = computed(() => mapStore.nodes)
const mapWidth = computed(() => mapStore.currentMap?.width || 1000)
const mapHeight = computed(() => mapStore.currentMap?.height || 800)
const mapImageUrl = computed(() => mapStore.currentMap?.url)

// 缩放控制
const zoomLevel = ref(1.0)
const canvasWidth = computed(() => mapWidth.value * zoomLevel.value)
const canvasHeight = computed(() => mapHeight.value * zoomLevel.value)

// 起点和终点
const startNode = computed(() => rawNodes.value.find(node => node.type === '起点'))
const endNode = computed(() => rawNodes.value.find(node => node.type === '终点'))
const obstacles = computed(() => rawNodes.value.filter(node => node.type === '障碍点'))
const waypoints = computed(() => rawNodes.value.filter(node => node.type === '路径点'))
const nodes = computed(() => rawNodes.value) // 所有节点

// 路径数据（直接使用原始坐标）
const pathPoints = computed(() => {
  if (!lastResult.value?.path) return []
  return lastResult.value.path.map(point => {
    // 点可能是[x, y]数组或{x, y}对象
    const x = Array.isArray(point) ? point[0] : point.x
    const y = Array.isArray(point) ? point[1] : point.y
    return { x, y }
  })
})

// 数据依赖检查
const hasValidData = computed(() => {
  return rawNodes.value.length >= 2 && startNode.value && endNode.value
})

// 画布和容器
const overlayCanvas = ref(null)
const mapContainer = ref(null)
const mapImage = ref(null)
const ctx = ref(null)

// 预生成的测试结果
const mockResults = {
  'a-star': {
    path: [[1,1],[2,2],[3,3],[4,4],[5,5],[6,6],[7,7],[8,8],[9,9],[10,10],[11,11],[12,12],[13,13]],
    distance: 16.97,
    timeMs: 45,
    coverage: 0.95,
    nodesVisited: 35
  },
  'dijkstra': {
    path: [[1,1],[1,2],[2,3],[3,4],[4,5],[5,6],[6,7],[7,8],[8,9],[9,10],[10,11],[11,12],[12,13],[13,13]],
    distance: 18.49,
    timeMs: 68,
    coverage: 0.92,
    nodesVisited: 52
  },
  'bfs': {
    path: [[1,1],[2,1],[3,2],[4,3],[5,4],[6,5],[7,6],[8,7],[9,8],[10,9],[11,10],[12,11],[13,12],[13,13]],
    distance: 19.31,
    timeMs: 92,
    coverage: 0.88,
    nodesVisited: 78
  },
  'dfs': {
    path: [[1,1],[2,2],[3,1],[4,2],[5,3],[6,2],[7,3],[8,4],[9,5],[10,6],[11,7],[12,8],[13,9],[13,13]],
    distance: 22.15,
    timeMs: 120,
    coverage: 0.82,
    nodesVisited: 95
  }
}

// 初始化
onMounted(() => {
  initCanvas()
  // 监听地图图片加载
  if (mapImage.value?.complete) {
    onMapImageLoad()
  }
})

// 地图图片加载完成回调
function onMapImageLoad() {
  redraw()
}

// 初始化画布
function initCanvas() {
  if (!overlayCanvas.value) return
  ctx.value = overlayCanvas.value.getContext('2d')
  redraw()
}


// 重绘画布 - 在地图图片上绘制节点和路径
function redraw() {
  if (!ctx.value || !overlayCanvas.value) return

  // 清除画布
  ctx.value.clearRect(0, 0, canvasWidth.value, canvasHeight.value)

  // 绘制路径
  if (pathPoints.value.length > 0) {
    ctx.value.strokeStyle = '#3b82f6'
    ctx.value.lineWidth = 4 * zoomLevel.value
    ctx.value.lineJoin = 'round'
    ctx.value.lineCap = 'round'

    ctx.value.beginPath()
    pathPoints.value.forEach((point, index) => {
      const x = point.x * zoomLevel.value
      const y = point.y * zoomLevel.value
      if (index === 0) {
        ctx.value.moveTo(x, y)
      } else {
        ctx.value.lineTo(x, y)
      }
    })
    ctx.value.stroke()

    // 绘制路径点标记
    const pointRadius = 6 * zoomLevel.value
    pathPoints.value.forEach((point, index) => {
      const x = point.x * zoomLevel.value
      const y = point.y * zoomLevel.value

      // 起点和终点特殊标记
      if (index === 0) {
        ctx.value.fillStyle = '#10b981' // 起点颜色
      } else if (index === pathPoints.value.length - 1) {
        ctx.value.fillStyle = '#ef4444' // 终点颜色
      } else {
        ctx.value.fillStyle = '#3b82f6' // 路径点颜色
      }

      ctx.value.beginPath()
      ctx.value.arc(x, y, pointRadius, 0, Math.PI * 2)
      ctx.value.fill()

      // 添加白色边框
      ctx.value.strokeStyle = '#ffffff'
      ctx.value.lineWidth = 2 * zoomLevel.value
      ctx.value.stroke()
    })
  }

  // 绘制地图节点（起点、终点、障碍点、路径点）
  const nodeRadius = 8 * zoomLevel.value

  // 绘制起点
  if (startNode.value) {
    const x = startNode.value.x * zoomLevel.value
    const y = startNode.value.y * zoomLevel.value
    ctx.value.fillStyle = '#10b981'
    ctx.value.beginPath()
    ctx.value.arc(x, y, nodeRadius, 0, Math.PI * 2)
    ctx.value.fill()

    // 添加白色边框和标签
    ctx.value.strokeStyle = '#ffffff'
    ctx.value.lineWidth = 2 * zoomLevel.value
    ctx.value.stroke()

    ctx.value.fillStyle = '#ffffff'
    ctx.value.font = `${12 * zoomLevel.value}px Arial`
    ctx.value.textAlign = 'center'
    ctx.value.textBaseline = 'middle'
    ctx.value.fillText('起点', x, y - nodeRadius * 2)
  }

  // 绘制终点
  if (endNode.value) {
    const x = endNode.value.x * zoomLevel.value
    const y = endNode.value.y * zoomLevel.value
    ctx.value.fillStyle = '#ef4444'
    ctx.value.beginPath()
    ctx.value.arc(x, y, nodeRadius, 0, Math.PI * 2)
    ctx.value.fill()

    ctx.value.strokeStyle = '#ffffff'
    ctx.value.lineWidth = 2 * zoomLevel.value
    ctx.value.stroke()

    ctx.value.fillStyle = '#ffffff'
    ctx.value.font = `${12 * zoomLevel.value}px Arial`
    ctx.value.textAlign = 'center'
    ctx.value.textBaseline = 'middle'
    ctx.value.fillText('终点', x, y - nodeRadius * 2)
  }

  // 绘制障碍点
  obstacles.value.forEach(obs => {
    const x = obs.x * zoomLevel.value
    const y = obs.y * zoomLevel.value
    ctx.value.fillStyle = '#9ca3af'
    ctx.value.beginPath()
    ctx.value.arc(x, y, nodeRadius * 0.8, 0, Math.PI * 2)
    ctx.value.fill()

    ctx.value.strokeStyle = '#ffffff'
    ctx.value.lineWidth = 1 * zoomLevel.value
    ctx.value.stroke()
  })

  // 绘制路径点（关注点）
  waypoints.value.forEach(wp => {
    const x = wp.x * zoomLevel.value
    const y = wp.y * zoomLevel.value
    ctx.value.fillStyle = '#1890ff'
    ctx.value.beginPath()
    ctx.value.arc(x, y, nodeRadius * 0.7, 0, Math.PI * 2)
    ctx.value.fill()

    ctx.value.strokeStyle = '#ffffff'
    ctx.value.lineWidth = 1 * zoomLevel.value
    ctx.value.stroke()
  })
}

// 监听缩放变化、节点数据变化和算法结果变化
watch([zoomLevel, rawNodes, lastResult], () => {
  redraw()
}, { immediate: true, deep: true })

// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
}

// 运行算法
async function runAlgorithm() {
  if (isCalculating.value) return

  // 数据依赖检查：确保有有效的节点数据
  if (!hasValidData.value) {
    ElMessage.warning('无法计算：需要至少一个起点和一个终点节点')
    return
  }

  try {
    const result = await algorithmStore.calculateAlgorithm()

    if (result) {
      // 结果已自动保存到algorithmStore，lastResult computed会自动更新
      // 重绘画布显示路径
      redraw()
      ElMessage.success('算法计算完成')
    } else {
      ElMessage.error('算法计算失败')
    }
  } catch (error) {
    console.error('算法计算失败:', error)
    ElMessage.error('算法计算失败，请重试')
  }
}

// 重置
function reset() {
  algorithmStore.clearResult()
  redraw()
  ElMessage.info('已重置')
}

// 保存参数
function saveParameters() {
  showParameterDialog.value = false
  ElMessage.success('参数已保存')
}

// 导出结果
function exportResult() {
  if (!lastResult.value) {
    ElMessage.warning('没有可导出的结果')
    return
  }

  const dataStr = JSON.stringify(lastResult.value, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr)

  const exportFileDefaultName = `算法结果_${selectedAlgorithm.value}_${new Date().getTime()}.json`

  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', exportFileDefaultName)
  linkElement.click()

  ElMessage.success('结果已导出')
}

// 刷新结果
function refreshResult() {
  if (!lastResult.value) return
  runAlgorithm()
}

// 加载历史结果
function loadHistoryResult(historyItem) {
  algorithmStore.selectResult(historyItem.id)
  redraw()
  ElMessage.info('已加载历史结果')
}
</script>

<style scoped>
.algorithm-container {
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

.algorithm-content {
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

.map-container {
  position: relative;
  flex: 1;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

.map-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.overlay-canvas {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
}

.map-info {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
  color: #4b5563;
  border: 1px solid #e5e7eb;
  z-index: 10;
}

.legend {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  gap: 16px;
  border: 1px solid #e5e7eb;
  z-index: 10;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4b5563;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 2px;
}

.start-color {
  background-color: #10b981;
}

.end-color {
  background-color: #ef4444;
}

.obstacle-color {
  background-color: #9ca3af;
}

.path-color {
  background-color: #3b82f6;
}

.waypoint-color {
  background-color: #1890ff;
}

.visualization-controls {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-top: 16px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
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

.result-metrics {
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
  font-size: 24px;
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
  .algorithm-content {
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

  .visualization-controls {
    flex-direction: column;
    gap: 16px;
  }

  .slider-control {
    width: 100%;
  }
}
</style>