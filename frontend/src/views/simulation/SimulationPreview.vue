<template>
  <div class="simulation-container">
    <!-- 工作流状态栏 -->
    <div v-if="workflowStore.currentStep === 'simulation-preview'" class="workflow-status-bar">
      <div class="workflow-header">
        <h3>工作流状态: 仿真预览 (第{{ workflowStore.currentStepIndex + 1 }}/{{ workflowStore.STEP_ORDER.length }}步)</h3>
        <div class="workflow-actions">
          <el-button size="small" @click="validateCurrentStep">验证步骤</el-button>
          <el-button size="small" type="primary" @click="goToNextStep">下一步: 实时监控</el-button>
        </div>
      </div>
      <el-steps :active="workflowStore.currentStepIndex" simple class="workflow-steps">
        <el-step
          v-for="(step, index) in workflowStore.STEP_ORDER"
          :key="step"
          :title="workflowStore.STEP_LABELS[step]"
          :class="{
            'completed': workflowStore.completedSteps.includes(step),
            'current': index === workflowStore.currentStepIndex,
            'pending': index > workflowStore.currentStepIndex
          }"
        />
      </el-steps>
      <div v-if="validationMessage" class="validation-message" :class="validationStatus">
        {{ validationMessage }}
      </div>
    </div>

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

        <!-- 仿真控制按钮 -->
        <el-button type="success" :icon="VideoPlay" @click="startSimulation" :disabled="!canStartSimulation">
          开始仿真
        </el-button>
        <el-button type="warning" :icon="VideoPause" @click="pauseSimulation" :disabled="simulationStatus !== 'running'">
          暂停仿真
        </el-button>
        <el-button type="danger" :icon="CircleClose" @click="stopSimulation" :disabled="simulationStatus === 'stopped'">
          停止仿真
        </el-button>
        <el-button :icon="Refresh" @click="resetSimulation">重置</el-button>

        <!-- 仿真速度控制 -->
        <div class="speed-control">
          <span>仿真速度:</span>
          <el-slider
            v-model="simulationSpeed"
            :min="0.1"
            :max="5"
            :step="0.1"
            style="width: 150px; margin: 0 16px;"
          />
          <span>{{ simulationSpeed }}x</span>
        </div>
      </div>

      <div class="right-controls">
        <!-- 算法选择 -->
        <el-select v-model="selectedAlgorithm" placeholder="选择算法" style="width: 180px; margin-right: 16px;">
          <el-option label="A*算法" value="a-star" />
          <el-option label="Dijkstra算法" value="dijkstra" />
          <el-option label="Boustrophedon算法" value="boustrophedon" />
          <el-option label="螺旋算法" value="spiral" />
        </el-select>

        <el-button :icon="Download" @click="exportSimulation">导出结果</el-button>
        <el-button :icon="Setting" @click="showConfigDialog = true">配置</el-button>
      </div>
    </div>

    <div class="simulation-content">
      <!-- 左侧仿真展示区域 -->
      <div class="simulation-display">
        <!-- 仿真地图 -->
        <div class="simulation-map">
          <div class="map-header">
            <h3>路径仿真展示</h3>
            <div class="simulation-status">
              <el-tag :type="getStatusTagType(simulationStatus)" size="small">
                {{ getStatusText(simulationStatus) }}
              </el-tag>
              <span class="time-elapsed">已运行: {{ formatTime(elapsedTime) }}</span>
            </div>
          </div>

          <div class="map-container" ref="mapContainer">
            <canvas ref="simulationCanvas" :width="canvasWidth" :height="canvasHeight"></canvas>

            <!-- 仿真进度 -->
            <div class="simulation-progress">
              <el-progress
                :percentage="simulationProgress * 100"
                :stroke-width="6"
                :show-text="false"
              />
              <div class="progress-text">{{ (simulationProgress * 100).toFixed(1) }}%</div>
            </div>
          </div>

          <!-- 仿真控制面板 -->
          <div class="simulation-controls">
            <div class="control-buttons">
              <el-button :icon="CaretLeft" @click="stepBackward" :disabled="simulationStatus !== 'paused'">
                上一步
              </el-button>
              <el-button :icon="CaretRight" @click="stepForward" :disabled="simulationStatus !== 'paused'">
                下一步
              </el-button>
              <el-button :icon="Scissor" @click="togglePathVisual">路径显示</el-button>
            </div>

            <div class="simulation-stats">
              <div class="stat-item">
                <span class="stat-label">当前位置:</span>
                <span class="stat-value">{{ currentPosition.x.toFixed(1) }}, {{ currentPosition.y.toFixed(1) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">当前速度:</span>
                <span class="stat-value">{{ currentSpeed.toFixed(1) }} m/s</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">已飞距离:</span>
                <span class="stat-value">{{ distanceTraveled.toFixed(1) }} 米</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 仿真日志 -->
        <div class="simulation-log">
          <div class="log-header">
            <h3>仿真日志</h3>
            <el-button :icon="Delete" type="text" size="small" @click="clearLog">清空</el-button>
          </div>
          <div class="log-content">
            <div
              v-for="(log, index) in simulationLogs"
              :key="index"
              class="log-item"
              :class="log.type"
            >
              <div class="log-time">{{ log.time }}</div>
              <div class="log-message">{{ log.message }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧结果面板 -->
      <div class="result-panel">
        <!-- 仿真结果统计 -->
        <div class="result-stats">
          <div class="panel-header">
            <h3>仿真结果统计</h3>
            <el-button :icon="RefreshRight" @click="refreshStats" size="small">刷新</el-button>
          </div>

          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-icon" style="background: #3b82f6;">
                <el-icon><Timer /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-label">总耗时</div>
                <div class="stat-value">{{ simulationResult.totalTime?.toFixed(1) || '--' }}</div>
                <div class="stat-unit">秒</div>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon" style="background: #10b981;">
                <el-icon><MapLocation /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-label">路径长度</div>
                <div class="stat-value">{{ simulationResult.pathLength?.toFixed(1) || '--' }}</div>
                <div class="stat-unit">米</div>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon" style="background: #f59e0b;">
                <el-icon><DataAnalysis /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-label">覆盖率</div>
                <div class="stat-value">{{ ((simulationResult.coverage || 0) * 100).toFixed(1) }}</div>
                <div class="stat-unit">%</div>
              </div>
            </div>

            <div class="stat-card">
              <div class="stat-icon" style="background: #ef4444;">
                <el-icon><Lightning /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-label">能耗</div>
                <div class="stat-value">{{ simulationResult.energyConsumption?.toFixed(0) || '--' }}</div>
                <div class="stat-unit">Wh</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 路径详情 -->
        <div class="path-details">
          <div class="panel-header">
            <h3>路径详情</h3>
            <el-switch v-model="showPathDetails" size="small" />
          </div>

          <div class="details-content" v-if="showPathDetails">
            <div class="detail-section">
              <h4>路径节点</h4>
              <div class="path-nodes">
                <div
                  v-for="(point, index) in simulationResult.path?.slice(0, 10) || []"
                  :key="index"
                  class="path-node"
                >
                  <span class="node-index">#{{ index + 1 }}</span>
                  <span class="node-coords">({{ point[0]?.toFixed(1) || 0 }}, {{ point[1]?.toFixed(1) || 0 }})</span>
                </div>
                <div v-if="simulationResult.path && simulationResult.path.length > 10" class="more-nodes">
                  ... 还有 {{ simulationResult.path.length - 10 }} 个节点
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h4>路径分段</h4>
              <el-table :data="pathSegments" size="small" style="width: 100%;" height="150">
                <el-table-column prop="segment" label="分段" width="60" />
                <el-table-column prop="start" label="起点" width="100">
                  <template #default="{ row }">
                    ({{ row.start[0]?.toFixed(0) }}, {{ row.start[1]?.toFixed(0) }})
                  </template>
                </el-table-column>
                <el-table-column prop="end" label="终点" width="100">
                  <template #default="{ row }">
                    ({{ row.end[0]?.toFixed(0) }}, {{ row.end[1]?.toFixed(0) }})
                  </template>
                </el-table-column>
                <el-table-column prop="length" label="长度" width="80">
                  <template #default="{ row }">{{ row.length?.toFixed(1) || '0.0' }}m</template>
                </el-table-column>
                <el-table-column prop="time" label="耗时" width="80">
                  <template #default="{ row }">{{ row.time?.toFixed(1) || '0.0' }}s</template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>

        <!-- 性能分析 -->
        <div class="performance-analysis">
          <div class="panel-header">
            <h3>性能分析</h3>
            <el-button :icon="DataLine" type="text" size="small" @click="toggleAnalysis">分析</el-button>
          </div>

          <div class="analysis-content" v-if="showAnalysis">
            <div class="metric-charts">
              <div class="metric-item">
                <div class="metric-header">
                  <span class="metric-title">速度变化</span>
                  <span class="metric-value">{{ averageSpeed.toFixed(1) }} m/s</span>
                </div>
                <div class="metric-chart">
                  <div class="chart-placeholder">速度图表</div>
                </div>
              </div>

              <div class="metric-item">
                <div class="metric-header">
                  <span class="metric-title">能耗分布</span>
                  <span class="metric-value">{{ (simulationResult.energyConsumption || 0).toFixed(0) }} Wh</span>
                </div>
                <div class="metric-chart">
                  <div class="chart-placeholder">能耗图表</div>
                </div>
              </div>
            </div>

            <div class="performance-metrics">
              <div class="perf-metric">
                <div class="perf-label">作业效率</div>
                <div class="perf-value">{{ (simulationResult.efficiency || 0).toFixed(2) }}</div>
                <el-progress
                  :percentage="(simulationResult.efficiency || 0) * 100"
                  :stroke-width="8"
                  :show-text="false"
                />
              </div>

              <div class="perf-metric">
                <div class="perf-label">重复率</div>
                <div class="perf-value">{{ ((simulationResult.overlapRate || 0) * 100).toFixed(1) }}%</div>
                <el-progress
                  :percentage="(simulationResult.overlapRate || 0) * 100"
                  :stroke-width="8"
                  :show-text="false"
                  :color="simulationResult.overlapRate > 0.1 ? '#ef4444' : '#10b981'"
                />
              </div>

              <div class="perf-metric">
                <div class="perf-label">转弯次数</div>
                <div class="perf-value">{{ simulationResult.turnCount || 0 }}</div>
                <div class="perf-description">路径平滑度指标</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 配置对话框 -->
    <el-dialog v-model="showConfigDialog" title="仿真配置" width="600">
      <el-tabs v-model="activeConfigTab">
        <el-tab-pane label="仿真参数" name="simulation">
          <el-form :model="simulationConfig" label-width="120px">
            <el-form-item label="无人机速度">
              <el-slider v-model="simulationConfig.droneSpeed" :min="1" :max="15" :step="0.5" />
              <span style="margin-left: 10px;">{{ simulationConfig.droneSpeed }} m/s</span>
            </el-form-item>
            <el-form-item label="初始位置">
              <el-input-number v-model="simulationConfig.startX" :min="0" :max="1000" />
              <el-input-number v-model="simulationConfig.startY" :min="0" :max="1000" style="margin-left: 8px;" />
              <span style="margin-left: 8px;">({{ simulationConfig.startX }}, {{ simulationConfig.startY }})</span>
            </el-form-item>
            <el-form-item label="终点位置">
              <el-input-number v-model="simulationConfig.endX" :min="0" :max="1000" />
              <el-input-number v-model="simulationConfig.endY" :min="0" :max="1000" style="margin-left: 8px;" />
              <span style="margin-left: 8px;">({{ simulationConfig.endX }}, {{ simulationConfig.endY }})</span>
            </el-form-item>
            <el-form-item label="障碍物数量">
              <el-slider v-model="simulationConfig.obstacleCount" :min="0" :max="50" :step="1" />
              <span style="margin-left: 10px;">{{ simulationConfig.obstacleCount }} 个</span>
            </el-form-item>
            <el-form-item label="仿真精度">
              <el-slider v-model="simulationConfig.precision" :min="0.1" :max="1" :step="0.05" />
              <span style="margin-left: 10px;">{{ simulationConfig.precision }}</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="显示设置" name="display">
          <el-form :model="displayConfig" label-width="120px">
            <el-form-item label="显示轨迹">
              <el-switch v-model="displayConfig.showTrajectory" />
            </el-form-item>
            <el-form-item label="显示网格">
              <el-switch v-model="displayConfig.showGrid" />
            </el-form-item>
            <el-form-item label="显示障碍物">
              <el-switch v-model="displayConfig.showObstacles" />
            </el-form-item>
            <el-form-item label="轨迹颜色">
              <el-color-picker v-model="displayConfig.trajectoryColor" />
            </el-form-item>
            <el-form-item label="无人机颜色">
              <el-color-picker v-model="displayConfig.droneColor" />
            </el-form-item>
            <el-form-item label="画布大小">
              <el-input-number v-model="displayConfig.canvasWidth" :min="400" :max="1200" />
              <el-input-number v-model="displayConfig.canvasHeight" :min="300" :max="800" style="margin-left: 8px;" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showConfigDialog = false">取消</el-button>
          <el-button type="primary" @click="applyConfig">应用配置</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { ElMessage, ElSteps, ElStep } from 'element-plus'
import {
  VideoPlay,
  VideoPause,
  CircleClose,
  Refresh,
  Download,
  Setting,
  CaretLeft,
  CaretRight,
  Scissor,
  Delete,
  Timer,
  MapLocation,
  DataAnalysis,
  Lightning,
  DataLine,
  RefreshRight
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode, simulationService } from '@/services/dataService'
import { useAlgorithmStore } from '@/stores/algorithmStore'
import { useWorkflowStore } from '@/stores/workflowStore'
import { useMapStore } from '@/stores/mapStore'

// Store实例化
const algorithmStore = useAlgorithmStore()
const workflowStore = useWorkflowStore()
const mapStore = useMapStore()

// 模式控制
const demoMode = ref(getCurrentMode())

// 仿真状态
const simulationStatus = ref('stopped') // 'stopped', 'running', 'paused'
const simulationSpeed = ref(1.0)
const simulationProgress = ref(0)
const elapsedTime = ref(0)
let simulationTimer = null

// 算法选择
const selectedAlgorithm = ref('a-star')

// 画布相关
const simulationCanvas = ref(null)
const canvasWidth = ref(800)
const canvasHeight = ref(500)
const ctx = ref(null)

// 当前状态
const currentPosition = ref({ x: 100, y: 100 })
const currentSpeed = ref(8.0)
const distanceTraveled = ref(0)
const showPathDetails = ref(true)
const showAnalysis = ref(true)

// 配置
const showConfigDialog = ref(false)
const activeConfigTab = ref('simulation')
const simulationConfig = reactive({
  droneSpeed: 8.0,
  startX: 100,
  startY: 100,
  endX: 700,
  endY: 400,
  obstacleCount: 15,
  precision: 0.5
})

const displayConfig = reactive({
  showTrajectory: true,
  showGrid: true,
  showObstacles: true,
  trajectoryColor: '#3b82f6',
  droneColor: '#ef4444',
  canvasWidth: 800,
  canvasHeight: 500
})

// 工作流验证状态
const validationMessage = ref('')
const validationStatus = ref('')

// 数据依赖检查
const hasValidAlgorithmResult = computed(() => {
  return algorithmStore.result && algorithmStore.result.path && algorithmStore.result.path.length >= 2
})

// 是否可以开始仿真
const canStartSimulation = computed(() => {
  // 检查算法结果
  if (!hasValidAlgorithmResult.value) return false

  // 检查仿真状态
  if (simulationStatus.value === 'running') return false

  return true
})

// 仿真结果 - 基于算法Store结果
const simulationResult = computed(() => {
  // 如果没有算法结果，返回默认空值
  if (!algorithmStore.result) {
    return {
      totalTime: 0,
      pathLength: 0,
      coverage: 0,
      energyConsumption: 0,
      efficiency: 0,
      overlapRate: 0,
      turnCount: 0,
      path: []
    }
  }

  const algoResult = algorithmStore.result
  return {
    totalTime: algoResult.timeMs ? algoResult.timeMs / 1000 : (algoResult.distance || 0) / 8.0, // 假设速度8m/s
    pathLength: algoResult.distance || 0,
    coverage: algoResult.coverage || 0,
    energyConsumption: (algoResult.distance || 0) * 8, // 假设每米8Wh
    efficiency: algoResult.efficiency || 0.85,
    overlapRate: algoResult.overlapRate || 0.05,
    turnCount: algoResult.turnCount || 12,
    path: algoResult.path || []
  }
})

// 仿真日志
const simulationLogs = ref([])

// 计算属性
const averageSpeed = computed(() => {
  const result = simulationResult.value
  if (result.totalTime && result.pathLength) {
    return result.pathLength / result.totalTime
  }
  return 8.0
})

// 障碍物节点（从地图Store获取）
const obstacleNodes = computed(() => {
  if (!mapStore.nodes || mapStore.nodes.length === 0) return []
  return mapStore.nodes.filter(node => node.type === '障碍物' || node.type === 'obstacle')
})

const pathSegments = computed(() => {
  const segments = []
  const result = simulationResult.value
  if (!result.path || result.path.length < 2) return segments

  for (let i = 0; i < result.path.length - 1; i++) {
    const start = result.path[i]
    const end = result.path[i + 1]
    const dx = end[0] - start[0]
    const dy = end[1] - start[1]
    const length = Math.sqrt(dx * dx + dy * dy)
    const time = length / currentSpeed.value

    segments.push({
      segment: i + 1,
      start,
      end,
      length,
      time
    })
  }

  return segments
})

// 初始化
onMounted(() => {
  initCanvas()
  initSimulationData()
  addLog('info', '仿真系统初始化完成')
})

onUnmounted(() => {
  stopSimulation()
})

// 初始化画布
function initCanvas() {
  if (!simulationCanvas.value) return
  ctx.value = simulationCanvas.value.getContext('2d')
  drawSimulation()
}

// 初始化仿真数据
function initSimulationData() {
  // 如果有算法结果，使用算法结果的起点
  const result = algorithmStore.result
  if (result && result.path && result.path.length > 0) {
    const startPoint = result.path[0]
    simulationConfig.startX = startPoint[0] || simulationConfig.startX
    simulationConfig.startY = startPoint[1] || simulationConfig.startY

    // 如果有终点，也更新
    if (result.path.length > 1) {
      const endPoint = result.path[result.path.length - 1]
      simulationConfig.endX = endPoint[0] || simulationConfig.endX
      simulationConfig.endY = endPoint[1] || simulationConfig.endY
    }
  }

  currentPosition.value = { x: simulationConfig.startX, y: simulationConfig.startY }
  currentSpeed.value = simulationConfig.droneSpeed
  simulationProgress.value = 0
  elapsedTime.value = 0
  distanceTraveled.value = 0
}

// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
}

// 开始仿真
async function startSimulation() {
  if (simulationStatus.value === 'running') return

  simulationStatus.value = 'running'
  addLog('success', '仿真开始')

  try {
    if (demoMode.value) {
      // 演示模式：模拟仿真
      simulationTimer = setInterval(() => {
        updateSimulation()
      }, 100 / simulationSpeed.value)
    } else {
      // 真实模式：调用API，传递算法结果和地图数据
      const simulationData = {
        algorithm: selectedAlgorithm.value,
        algorithmResult: algorithmStore.result, // 算法计算结果
        mapData: {
          mapId: mapStore.currentMap?.id,
          nodes: mapStore.nodes,
          width: mapStore.currentMap?.width || 800,
          height: mapStore.currentMap?.height || 500
        },
        config: simulationConfig,
        droneConfig: {} // 可以从droneStore获取，如果需要
      }
      const response = await simulationService.start(simulationData)
      const data = response.data
      addLog('info', `仿真ID: ${data.simulationId}`)
    }
  } catch (error) {
    console.error('开始仿真失败:', error)
    addLog('error', '开始仿真失败')
    simulationStatus.value = 'stopped'
  }
}

// 暂停仿真
function pauseSimulation() {
  if (simulationStatus.value !== 'running') return

  simulationStatus.value = 'paused'
  if (simulationTimer) {
    clearInterval(simulationTimer)
    simulationTimer = null
  }
  addLog('warning', '仿真暂停')
}

// 停止仿真
function stopSimulation() {
  simulationStatus.value = 'stopped'
  if (simulationTimer) {
    clearInterval(simulationTimer)
    simulationTimer = null
  }
  initSimulationData()
  addLog('info', '仿真停止')
  drawSimulation()
}

// 重置仿真
function resetSimulation() {
  stopSimulation()
  simulationLogs.value = []
  initSimulationData()
  addLog('info', '仿真重置')
}

// 更新仿真状态
function updateSimulation() {
  if (simulationProgress.value >= 1) {
    stopSimulation()
    addLog('success', '仿真完成')
    return
  }

  // 更新进度
  simulationProgress.value += 0.01 * simulationSpeed.value
  elapsedTime.value += 0.1 * simulationSpeed.value

  // 更新当前位置（模拟沿路径移动）
  const result = simulationResult.value
  if (result.path && result.path.length > 1) {
    const totalSegments = result.path.length - 1
    const progressPerSegment = 1 / totalSegments
    const currentSegment = Math.floor(simulationProgress.value / progressPerSegment)
    const segmentProgress = (simulationProgress.value % progressPerSegment) / progressPerSegment

    if (currentSegment < totalSegments) {
      const start = result.path[currentSegment]
      const end = result.path[currentSegment + 1]
      currentPosition.value = {
        x: start[0] + (end[0] - start[0]) * segmentProgress,
        y: start[1] + (end[1] - start[1]) * segmentProgress
      }
    }
  }

  // 更新已飞距离
  distanceTraveled.value = simulationProgress.value * (result.pathLength || 500)

  // 重绘画布
  drawSimulation()

  // 每10%进度记录一次日志
  if (Math.abs(simulationProgress.value * 100 % 10) < 0.1 * simulationSpeed.value) {
    addLog('info', `仿真进度: ${(simulationProgress.value * 100).toFixed(1)}%`)
  }
}

// 绘制仿真
function drawSimulation() {
  if (!ctx.value) return

  const width = canvasWidth.value
  const height = canvasHeight.value

  // 清空画布
  ctx.value.clearRect(0, 0, width, height)

  // 绘制背景
  ctx.value.fillStyle = '#f8fafc'
  ctx.value.fillRect(0, 0, width, height)

  // 绘制网格
  if (displayConfig.showGrid) {
    ctx.value.strokeStyle = '#e5e7eb'
    ctx.value.lineWidth = 1
    const gridSize = 50

    for (let x = 0; x <= width; x += gridSize) {
      ctx.value.beginPath()
      ctx.value.moveTo(x, 0)
      ctx.value.lineTo(x, height)
      ctx.value.stroke()
    }

    for (let y = 0; y <= height; y += gridSize) {
      ctx.value.beginPath()
      ctx.value.moveTo(0, y)
      ctx.value.lineTo(width, y)
      ctx.value.stroke()
    }
  }

  // 绘制障碍物
  if (displayConfig.showObstacles) {
    ctx.value.fillStyle = '#9ca3af'
    // 绘制地图中的障碍物节点
    const obstacles = obstacleNodes.value
    if (obstacles.length > 0) {
      // 使用真实的障碍物节点
      obstacles.forEach(node => {
        const x = node.x || 0
        const y = node.y || 0
        ctx.value.fillRect(x - 20, y - 20, 40, 40)
        // 绘制障碍物标签
        if (node.label) {
          ctx.value.fillStyle = '#1f2937'
          ctx.value.font = '12px Arial'
          ctx.value.textAlign = 'center'
          ctx.value.fillText(node.label, x, y - 25)
          ctx.value.fillStyle = '#9ca3af'
        }
      })
    } else {
      // 如果没有障碍物节点，生成一些随机障碍物
      for (let i = 0; i < simulationConfig.obstacleCount; i++) {
        const x = (i * 70) % width + 50
        const y = (i * 50) % height + 50
        ctx.value.fillRect(x - 20, y - 20, 40, 40)
      }
    }
  }

  // 绘制路径
  const result = simulationResult.value
  if (displayConfig.showTrajectory && result.path && result.path.length > 1) {
    ctx.value.strokeStyle = displayConfig.trajectoryColor
    ctx.value.lineWidth = 3
    ctx.value.lineJoin = 'round'
    ctx.value.lineCap = 'round'

    ctx.value.beginPath()
    ctx.value.moveTo(result.path[0][0], result.path[0][1])
    for (let i = 1; i < result.path.length; i++) {
      ctx.value.lineTo(result.path[i][0], result.path[i][1])
    }
    ctx.value.stroke()

    // 绘制路径点
    ctx.value.fillStyle = displayConfig.trajectoryColor
    result.path.forEach(point => {
      ctx.value.beginPath()
      ctx.value.arc(point[0], point[1], 5, 0, Math.PI * 2)
      ctx.value.fill()
    })
  }

  // 绘制无人机
  const droneSize = 20
  ctx.value.fillStyle = displayConfig.droneColor
  ctx.value.beginPath()
  ctx.value.arc(currentPosition.value.x, currentPosition.value.y, droneSize, 0, Math.PI * 2)
  ctx.value.fill()

  // 绘制无人机方向
  ctx.value.strokeStyle = '#ffffff'
  ctx.value.lineWidth = 3
  ctx.value.beginPath()
  ctx.value.moveTo(currentPosition.value.x, currentPosition.value.y)
  ctx.value.lineTo(
    currentPosition.value.x + Math.cos(elapsedTime.value) * droneSize * 1.5,
    currentPosition.value.y + Math.sin(elapsedTime.value) * droneSize * 1.5
  )
  ctx.value.stroke()
}

// 步进控制
function stepForward() {
  if (simulationProgress.value < 1) {
    simulationProgress.value = Math.min(1, simulationProgress.value + 0.05)
    updateSimulation()
  }
}

function stepBackward() {
  if (simulationProgress.value > 0) {
    simulationProgress.value = Math.max(0, simulationProgress.value - 0.05)
    updateSimulation()
  }
}

function togglePathVisual() {
  displayConfig.showTrajectory = !displayConfig.showTrajectory
  drawSimulation()
  addLog('info', `路径显示: ${displayConfig.showTrajectory ? '开启' : '关闭'}`)
}

// 添加日志
function addLog(type, message) {
  const now = new Date()
  const timeStr = now.toLocaleTimeString()

  simulationLogs.value.unshift({
    time: timeStr,
    type,
    message
  })

  // 保持日志条数不超过50
  if (simulationLogs.value.length > 50) {
    simulationLogs.value.pop()
  }
}

function clearLog() {
  simulationLogs.value = []
  addLog('info', '日志已清空')
}

// 状态处理
function getStatusTagType(status) {
  return {
    stopped: 'info',
    running: 'success',
    paused: 'warning'
  }[status] || 'info'
}

function getStatusText(status) {
  return {
    stopped: '已停止',
    running: '运行中',
    paused: '已暂停'
  }[status] || '未知'
}

function formatTime(seconds) {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 刷新统计
async function refreshStats() {
  try {
    if (demoMode.value) {
      // 演示模式：基于当前仿真进度更新日志
      addLog('info', `仿真进度: ${(simulationProgress.value * 100).toFixed(1)}%, 已运行: ${formatTime(elapsedTime.value)}, 已飞距离: ${distanceTraveled.value.toFixed(1)}米`)
    } else {
      // 真实模式：调用API获取仿真状态
      const response = await simulationService.getStatus()
      const data = response.data
      // 更新仿真状态变量
      if (data.progress !== undefined) simulationProgress.value = data.progress
      if (data.elapsedTime !== undefined) elapsedTime.value = data.elapsedTime
      // 注意：simulationResult是计算属性，基于算法结果，不从仿真状态API更新
      addLog('info', '仿真状态已更新')
    }
  } catch (error) {
    console.error('刷新统计数据失败:', error)
    addLog('error', '刷新统计数据失败')
  }
}

// 导出仿真结果
function exportSimulation() {
  const exportData = {
    timestamp: new Date().toISOString(),
    algorithm: selectedAlgorithm.value,
    config: { ...simulationConfig },
    result: { ...simulationResult.value },
    logs: simulationLogs.value.slice(0, 20)
  }

  const dataStr = JSON.stringify(exportData, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr)

  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', `仿真结果_${selectedAlgorithm.value}_${new Date().getTime()}.json`)
  linkElement.click()

  addLog('success', '仿真结果已导出')
}

// 应用配置
function applyConfig() {
  canvasWidth.value = displayConfig.canvasWidth
  canvasHeight.value = displayConfig.canvasHeight
  simulationConfig.droneSpeed = currentSpeed.value

  nextTick(() => {
    initCanvas()
    drawSimulation()
  })

  showConfigDialog.value = false
  addLog('info', '仿真配置已应用')
}

// 工作流验证方法
function validateCurrentStep() {
  validationMessage.value = ''
  validationStatus.value = ''

  // 检查数据依赖
  if (!hasValidAlgorithmResult.value) {
    validationMessage.value = '错误: 需要有效的算法计算结果才能进行仿真。请先在前面的步骤中运行算法计算。'
    validationStatus.value = 'error'
    ElMessage.error(validationMessage.value)
    return false
  }

  // 检查地图节点数据
  if (!mapStore.currentMap || !mapStore.nodes || mapStore.nodes.length < 2) {
    validationMessage.value = '警告: 地图节点数据不足。仿真可能无法准确反映实际农田情况。'
    validationStatus.value = 'warning'
    ElMessage.warning(validationMessage.value)
  } else {
    validationMessage.value = '验证通过: 数据依赖满足，可以开始仿真。'
    validationStatus.value = 'success'
    ElMessage.success(validationMessage.value)

    // 标记步骤为完成
    if (!workflowStore.completedSteps.includes('simulation-preview')) {
      workflowStore.completedSteps.push('simulation-preview')
    }
  }

  return validationStatus.value === 'success'
}

// 前往下一步
function goToNextStep() {
  if (!validateCurrentStep()) {
    ElMessage.error('请先完成当前步骤的验证')
    return
  }

  try {
    if (workflowStore.next()) {
      ElMessage.success('已进入下一步: 实时监控')
    } else {
      ElMessage.info('已经是最后一步')
    }
  } catch (error) {
    ElMessage.error(`无法切换到下一步: ${error.message}`)
  }
}

// 监听算法结果变化
watch(() => algorithmStore.result, (newResult) => {
  if (newResult) {
    addLog('info', '检测到新的算法结果，仿真数据已更新')

    // 重置仿真状态
    if (simulationStatus.value !== 'stopped') {
      stopSimulation()
    }

    // 更新配置中的起点和终点
    if (newResult.path && newResult.path.length >= 2) {
      const startPoint = newResult.path[0]
      const endPoint = newResult.path[newResult.path.length - 1]
      simulationConfig.startX = startPoint[0] || 100
      simulationConfig.startY = startPoint[1] || 100
      simulationConfig.endX = endPoint[0] || 700
      simulationConfig.endY = endPoint[1] || 400

      // 重置当前位置
      currentPosition.value = { x: simulationConfig.startX, y: simulationConfig.startY }
    }

    // 重绘画布
    nextTick(() => {
      drawSimulation()
    })
  }
}, { deep: true })

// 切换分析显示
function toggleAnalysis() {
  showAnalysis.value = !showAnalysis.value
}
</script>

<style scoped>
.simulation-container {
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
  gap: 16px;
}

.speed-control {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.right-controls {
  display: flex;
  gap: 8px;
}

.simulation-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #f5f5f5;
}

.simulation-display {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px;
}

.simulation-map {
  flex: 2;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.map-header {
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.map-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
}

.simulation-status {
  display: flex;
  align-items: center;
  gap: 16px;
}

.time-elapsed {
  font-size: 14px;
  color: #6b7280;
}

.map-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

canvas {
  display: block;
  background: #f8fafc;
}

.simulation-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 16px;
}

.progress-text {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  min-width: 60px;
}

.simulation-controls {
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
  background: #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.control-buttons {
  display: flex;
  gap: 8px;
}

.simulation-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.simulation-log {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  margin-top: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.log-header {
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.log-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.log-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.log-item {
  padding: 8px 12px;
  border-bottom: 1px solid #f3f4f6;
}

.log-item:last-child {
  border-bottom: none;
}

.log-item.info {
  border-left: 3px solid #3b82f6;
  background: #eff6ff;
}

.log-item.success {
  border-left: 3px solid #10b981;
  background: #ecfdf5;
}

.log-item.warning {
  border-left: 3px solid #f59e0b;
  background: #fffbeb;
}

.log-item.error {
  border-left: 3px solid #ef4444;
  background: #fef2f2;
}

.log-time {
  font-size: 11px;
  color: #9ca3af;
  margin-bottom: 2px;
}

.log-message {
  font-size: 13px;
  color: #1f2937;
}

.result-panel {
  width: 400px;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.result-stats {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.panel-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}

.stat-unit {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.path-details {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.details-content {
  margin-top: 12px;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section h4 {
  margin: 0 0 8px 0;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
}

.path-nodes {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 150px;
  overflow-y: auto;
  padding-right: 8px;
}

.path-node {
  display: flex;
  justify-content: space-between;
  padding: 4px 8px;
  background: #f9fafb;
  border-radius: 4px;
  font-size: 12px;
}

.node-index {
  color: #6b7280;
  font-weight: 500;
}

.node-coords {
  color: #1f2937;
  font-family: monospace;
}

.more-nodes {
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
  padding: 8px;
  background: #f3f4f6;
  border-radius: 4px;
}

.performance-analysis {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.analysis-content {
  flex: 1;
  overflow-y: auto;
  padding-top: 12px;
}

.metric-charts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.metric-item {
  background: #f9fafb;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e5e7eb;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.metric-title {
  font-size: 13px;
  font-weight: 600;
  color: #4a5568;
}

.metric-value {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.metric-chart {
  height: 80px;
  background: #ffffff;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 12px;
}

.performance-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.perf-metric {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e5e7eb;
}

.perf-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
}

.perf-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 8px;
}

.perf-description {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

/* 工作流状态栏样式 */
.workflow-status-bar {
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  padding: 16px;
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.workflow-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.workflow-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.workflow-actions {
  display: flex;
  gap: 8px;
}

.workflow-steps {
  margin-top: 16px;
}

.workflow-steps ::v-deep .el-step__head {
  font-size: 12px;
}

.workflow-steps ::v-deep .el-step__title {
  font-size: 12px;
}

.validation-message {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
}

.validation-message.success {
  background: #ecfdf5;
  color: #10b981;
  border-left: 4px solid #10b981;
}

.validation-message.error {
  background: #fef2f2;
  color: #ef4444;
  border-left: 4px solid #ef4444;
}

.validation-message.warning {
  background: #fffbeb;
  color: #f59e0b;
  border-left: 4px solid #f59e0b;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .simulation-content {
    flex-direction: column;
  }

  .result-panel {
    width: 100%;
    height: 40%;
  }

  .stats-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .header-controls {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .left-controls, .right-controls {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }

  .simulation-controls {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .simulation-stats {
    width: 100%;
    justify-content: space-between;
  }

  .metric-charts {
    grid-template-columns: 1fr;
  }
}
</style>