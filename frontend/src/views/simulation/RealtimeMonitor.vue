<template>
  <div class="monitor-container">
    <!-- 工作流状态栏 -->
    <div class="workflow-status-bar" v-if="workflowStore.currentStep === workflowStore.STEPS.REALTIME_MONITOR">
      <div class="workflow-steps">
        <div
          v-for="step in workflowStore.STEP_ORDER"
          :key="step"
          class="workflow-step"
          :class="getStepClass(step)"
          @click="goToStep(step)"
        >
          <el-icon class="step-icon">
            <component :is="getStepIcon(step)" />
          </el-icon>
          <div class="step-label">{{ workflowStore.STEP_LABELS[step] }}</div>
          <div class="step-validation" v-if="getStepValidation(step)?.missing?.length">
            <el-tooltip :content="`需要: ${getStepValidation(step).missing.join(', ')}`">
              <el-icon size="14" color="#ff4d4f"><Warning /></el-icon>
            </el-tooltip>
          </div>
        </div>
      </div>
      <div class="workflow-progress">
        <div class="progress-label">进度: {{ workflowStore.progressPercentage }}%</div>
        <el-progress :percentage="workflowStore.progressPercentage" :stroke-width="6" />
        <div class="current-validation" v-if="!workflowStore.canProceed">
          <el-icon size="14" color="#ff4d4f"><Warning /></el-icon>
          <span style="margin-left: 4px; font-size: 12px; color: #ff4d4f;">
            {{ currentValidation?.message }}
          </span>
        </div>
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

        <!-- 控制按钮 -->
        <el-button type="primary" :icon="VideoPlay" @click="startMonitoring" :disabled="!canStartMonitoring">
          开始监控
        </el-button>
        <el-button type="warning" :icon="VideoPause" @click="pauseMonitoring" :disabled="!isMonitoring">
          暂停监控
        </el-button>
        <el-button :icon="Refresh" @click="resetMonitoring">重置</el-button>

        <!-- 状态提示 -->
        <div class="status-hint">
          <el-tag :type="hasValidAlgorithmResult ? 'success' : 'warning'" size="small">
            <template v-if="hasValidAlgorithmResult">
              <el-icon><CircleCheck /></el-icon>
              <span style="margin-left: 4px;">算法结果就绪</span>
            </template>
            <template v-else>
              <el-icon><Warning /></el-icon>
              <span style="margin-left: 4px;">需要算法结果</span>
            </template>
          </el-tag>
        </div>

        <!-- 刷新间隔设置 -->
        <div class="interval-control">
          <span>刷新间隔:</span>
          <el-select v-model="refreshInterval" size="small" style="width: 100px; margin-left: 8px;">
            <el-option label="1秒" :value="1" />
            <el-option label="3秒" :value="3" />
            <el-option label="5秒" :value="5" />
            <el-option label="10秒" :value="10" />
          </el-select>
        </div>
      </div>

      <div class="right-controls">
        <!-- 导出按钮 -->
        <el-button :icon="Download" @click="exportData">导出数据</el-button>
        <el-button :icon="Setting" @click="showSettingsDialog = true">
          监控设置
        </el-button>
      </div>
    </div>

    <div class="monitor-content">
      <!-- 左侧监控区域 -->
      <div class="monitor-left">
        <!-- 地图显示区域 -->
        <div class="map-monitor">
          <div class="map-title">无人机实时位置</div>
          <div class="map-container" ref="mapContainer">
            <div class="map-background">
              <img
                :src="mapStore.currentMap?.url || mapImageUrl"
                alt="农田地图"
                class="map-image"
              />

              <!-- 无人机标记 -->
              <div
                v-if="dronePosition"
                class="drone-marker"
                :style="{
                  left: `${dronePosition.x * positionScale}px`,
                  top: `${dronePosition.y * positionScale}px`
                }"
              >
                <div class="drone-icon">
                  <el-icon size="24" color="#409EFF">
                    <Position />
                  </el-icon>
                </div>
                <div class="drone-info">
                  <div class="drone-label">无人机</div>
                  <div class="drone-coords">
                    ({{ dronePosition.x.toFixed(1) }}, {{ dronePosition.y.toFixed(1) }})
                  </div>
                </div>
              </div>

              <!-- 路径轨迹 -->
              <div class="path-container">
                <svg width="100%" height="100%" style="position: absolute; top: 0; left: 0;">
                  <path
                    v-if="pathPoints.length > 1"
                    :d="pathData"
                    stroke="#409EFF"
                    stroke-width="2"
                    fill="none"
                    stroke-dasharray="5,5"
                  />
                </svg>
              </div>
            </div>
          </div>

          <!-- 地图控制 -->
          <div class="map-controls">
            <el-button :icon="ZoomIn" @click="zoomIn" circle size="small" />
            <el-button :icon="ZoomOut" @click="zoomOut" circle size="small" />
            <el-button :icon="Refresh" @click="resetView" circle size="small" />
            <span class="zoom-level">缩放: {{ (positionScale * 100).toFixed(0) }}%</span>
          </div>
        </div>

        <!-- 进度和状态卡片 -->
        <div class="status-cards">
          <div class="card">
            <div class="card-header">
              <el-icon><Timer /></el-icon>
              <span>作业进度</span>
            </div>
            <div class="card-body">
              <div class="progress-info">
                <div class="progress-label">总体进度</div>
                <el-progress
                  :percentage="currentData.progress * 100"
                  :stroke-width="8"
                  :show-text="false"
                />
                <div class="progress-value">{{ (currentData.progress * 100).toFixed(1) }}%</div>
              </div>
              <div class="progress-details">
                <div class="detail-item">
                  <span class="detail-label">已飞距离:</span>
                  <span class="detail-value">{{ currentData.distanceTraveled?.toFixed(1) || '0.0' }} 米</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">剩余距离:</span>
                  <span class="detail-value">{{ currentData.distanceRemaining?.toFixed(1) || '0.0' }} 米</span>
                </div>
                <div class="detail-item">
                  <span class="detail-label">预计完成:</span>
                  <span class="detail-value">{{ currentData.estimatedCompletion || '--' }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-header">
              <el-icon><Bell /></el-icon>
              <span>系统状态</span>
            </div>
            <div class="card-body">
              <div class="status-indicators">
                <div class="status-item" :class="{ 'status-error': currentData.batteryLevel < 20 }">
                  <el-icon><Lightning /></el-icon>
                  <span class="status-label">电量</span>
                  <el-progress
                    :percentage="currentData.batteryLevel || 0"
                    :stroke-width="6"
                    :show-text="false"
                  />
                  <span class="status-value">{{ currentData.batteryLevel || 0 }}%</span>
                </div>
                <div class="status-item">
                  <el-icon><TrendCharts /></el-icon>
                  <span class="status-label">速度</span>
                  <span class="status-value">{{ currentData.currentSpeed?.toFixed(1) || '0.0' }} m/s</span>
                </div>
                <div class="status-item">
                  <el-icon><Top /></el-icon>
                  <span class="status-label">高度</span>
                  <span class="status-value">{{ currentData.currentAltitude?.toFixed(1) || '0.0' }} 米</span>
                </div>
                <div class="status-item">
                  <el-icon><Monitor /></el-icon>
                  <span class="status-label">信号强度</span>
                  <el-progress
                    :percentage="currentData.signalStrength || 100"
                    :stroke-width="6"
                    :show-text="false"
                  />
                  <span class="status-value">{{ currentData.signalStrength || 100 }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧信息面板 -->
      <div class="monitor-right">
        <!-- 告警信息 -->
        <div class="alerts-panel">
          <div class="panel-header">
            <h3>系统告警</h3>
            <el-button :icon="Bell" type="text" size="small" @click="clearAlerts">
              清空
            </el-button>
          </div>
          <div class="alerts-list">
            <el-empty v-if="alerts.length === 0" description="暂无告警信息" :image-size="80" />
            <div v-else class="alert-items">
              <div
                v-for="(alert, index) in alerts"
                :key="index"
                class="alert-item"
                :class="getAlertClass(alert.level)"
              >
                <el-icon class="alert-icon">
                  <component :is="getAlertIcon(alert.level)" />
                </el-icon>
                <div class="alert-content">
                  <div class="alert-title">{{ alert.title }}</div>
                  <div class="alert-message">{{ alert.message }}</div>
                  <div class="alert-time">{{ alert.time }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 历史数据图表 -->
        <div class="history-chart">
          <div class="panel-header">
            <h3>历史轨迹</h3>
            <el-select v-model="chartType" size="small" style="width: 120px;">
              <el-option label="路径轨迹" value="path" />
              <el-option label="速度变化" value="speed" />
              <el-option label="高度变化" value="altitude" />
            </el-select>
          </div>
          <div class="chart-container" ref="chartContainer">
            <!-- 这里可以集成ECharts或Chart.js -->
            <div class="chart-placeholder">
              <el-empty description="图表区域" :image-size="60" />
            </div>
          </div>
        </div>

        <!-- 实时数据流 -->
        <div class="data-stream">
          <div class="panel-header">
            <h3>实时数据流</h3>
            <el-tag :type="isMonitoring ? 'success' : 'warning'" size="small">
              {{ isMonitoring ? '监控中' : '已暂停' }}
            </el-tag>
          </div>
          <div class="stream-list">
            <div
              v-for="(log, index) in dataLogs"
              :key="index"
              class="stream-item"
            >
              <div class="stream-time">{{ log.time }}</div>
              <div class="stream-content">
                <span class="stream-type">{{ log.type }}:</span>
                <span class="stream-data">{{ log.data }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 设置对话框 -->
    <el-dialog v-model="showSettingsDialog" title="监控设置" width="500">
      <el-form :model="settings" label-width="120px">
        <el-form-item label="地图图片URL">
          <el-input v-model="settings.mapUrl" placeholder="输入地图图片URL" />
        </el-form-item>
        <el-form-item label="告警级别">
          <el-select v-model="settings.alertLevel" multiple>
            <el-option label="信息" value="info" />
            <el-option label="警告" value="warning" />
            <el-option label="错误" value="error" />
          </el-select>
        </el-form-item>
        <el-form-item label="保存历史数据">
          <el-switch v-model="settings.saveHistory" />
        </el-form-item>
        <el-form-item label="自动清空日志">
          <el-input-number v-model="settings.autoClearHours" :min="1" :max="24" />
          <span style="margin-left: 8px;">小时后</span>
        </el-form-item>
        <el-form-item label="声音提醒">
          <el-switch v-model="settings.soundAlert" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showSettingsDialog = false">取消</el-button>
          <el-button type="primary" @click="saveSettings">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  VideoPlay,
  VideoPause,
  Refresh,
  Download,
  Setting,
  ZoomIn,
  ZoomOut,
  Timer,
  Bell,
  Lightning,
  TrendCharts,
  Top,
  Monitor,
  Position,
  Warning,
  CircleCheck,
  InfoFilled,
  Location,
  MapLocation,
  DataAnalysis,
  Cpu,
  Setting as SettingIcon,
  VideoCamera,
  Monitor as MonitorIcon
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode, simulationService } from '@/services/dataService'
import { useAlgorithmStore } from '@/stores/algorithmStore'
import { useWorkflowStore } from '@/stores/workflowStore'
import { useMapStore } from '@/stores/mapStore'

// Store实例
const algorithmStore = useAlgorithmStore()
const workflowStore = useWorkflowStore()
const mapStore = useMapStore()

// 数据依赖检查
const hasValidAlgorithmResult = computed(() => {
  return algorithmStore.result && algorithmStore.result.path && algorithmStore.result.path.length >= 2
})

const canStartMonitoring = computed(() => {
  if (workflowStore.currentStep !== workflowStore.STEPS.REALTIME_MONITOR) return false
  return hasValidAlgorithmResult.value && !isMonitoring.value
})

const currentValidation = computed(() => workflowStore.getCurrentValidation())

// 工作流辅助函数
function getStepClass(step) {
  const status = workflowStore.getStepStatus(step)
  return `step-${status}`
}

function getStepIcon(step) {
  const icons = {
    [workflowStore.STEPS.MAP_SELECTION]: Location,
    [workflowStore.STEPS.NODE_MARKING]: MapLocation,
    [workflowStore.STEPS.DRONE_CONFIG]: SettingIcon,
    [workflowStore.STEPS.ALGORITHM_SELECTION]: DataAnalysis,
    [workflowStore.STEPS.PARAMETER_CONFIG]: Cpu,
    [workflowStore.STEPS.CALCULATION]: DataAnalysis,
    [workflowStore.STEPS.SIMULATION_PREVIEW]: VideoCamera,
    [workflowStore.STEPS.REALTIME_MONITOR]: MonitorIcon
  }
  return icons[step] || DataAnalysis
}

function getStepValidation(step) {
  return workflowStore.getStepValidation(step)
}

function goToStep(step) {
  // 只允许跳转到已完成步骤或当前步骤
  const stepIndex = workflowStore.STEP_ORDER.indexOf(step)
  const currentIndex = workflowStore.STEP_ORDER.indexOf(workflowStore.currentStep)
  if (stepIndex <= currentIndex) {
    workflowStore.goToStep(step)
  }
}

// 模式控制
const demoMode = ref(getCurrentMode())

// 监控状态
const isMonitoring = ref(false)
const refreshInterval = ref(5) // 默认5秒
let monitoringTimer = null

// 当前数据 - 基于算法结果初始化
const currentData = reactive({
  timestamp: new Date().toISOString(),
  dronePosition: { x: 0, y: 0 },
  batteryLevel: 100,
  progress: 0,
  currentSpeed: 0,
  currentAltitude: 0,
  distanceTraveled: 0,
  distanceRemaining: 0,
  estimatedCompletion: '--',
  signalStrength: 100
})

// 路径轨迹 - 基于算法结果
const pathPoints = ref([])

// 从算法结果初始化路径
function initPathFromAlgorithmResult() {
  if (!algorithmStore.result || !algorithmStore.result.path || algorithmStore.result.path.length < 2) {
    pathPoints.value = []
    return
  }

  // 将算法路径转换为路径点
  const algorithmPath = algorithmStore.result.path
  pathPoints.value = algorithmPath.map((point, index) => ({
    x: point[0] || point.x || 0,
    y: point[1] || point.y || 0,
    time: new Date()
  }))

  // 初始化无人机位置为路径起点
  if (pathPoints.value.length > 0) {
    currentData.dronePosition.x = pathPoints.value[0].x
    currentData.dronePosition.y = pathPoints.value[0].y
  }

  // 计算总距离和剩余距离
  if (algorithmStore.result.distance) {
    currentData.distanceRemaining = algorithmStore.result.distance
  } else {
    // 估算距离
    let totalDistance = 0
    for (let i = 1; i < pathPoints.value.length; i++) {
      const dx = pathPoints.value[i].x - pathPoints.value[i-1].x
      const dy = pathPoints.value[i].y - pathPoints.value[i-1].y
      totalDistance += Math.sqrt(dx*dx + dy*dy)
    }
    currentData.distanceRemaining = totalDistance
  }
}

// 告警信息
const alerts = ref([
  { level: 'warning', title: '低电量警告', message: '电池电量低于30%', time: '14:25:30' },
  { level: 'info', title: '边缘区域提醒', message: '接近作业区域边界', time: '14:25:15' },
  { level: 'info', title: '风速监测', message: '当前风速 2.5 m/s', time: '14:25:00' }
])

// 数据日志
const dataLogs = ref([])

// 地图相关
const mapImageUrl = ref('https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800&h=600&fit=crop')
const positionScale = ref(0.5)

// 设置
const showSettingsDialog = ref(false)
const settings = reactive({
  mapUrl: 'https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800&h=600&fit=crop',
  alertLevel: ['info', 'warning', 'error'],
  saveHistory: true,
  autoClearHours: 6,
  soundAlert: true
})

// 图表类型
const chartType = ref('path')

// 计算属性
const dronePosition = computed(() => currentData.dronePosition)

const pathData = computed(() => {
  if (pathPoints.value.length < 2) return ''

  const scaledPoints = pathPoints.value.map(p => ({
    x: p.x * positionScale.value,
    y: p.y * positionScale.value
  }))

  let d = `M ${scaledPoints[0].x} ${scaledPoints[0].y}`
  for (let i = 1; i < scaledPoints.length; i++) {
    d += ` L ${scaledPoints[i].x} ${scaledPoints[i].y}`
  }
  return d
})

// 初始化
onMounted(() => {
  // 从算法结果初始化路径
  initPathFromAlgorithmResult()

  // 监听算法结果变化
  watch(() => algorithmStore.result, () => {
    initPathFromAlgorithmResult()
  }, { deep: true })

  // 如果当前是实时监控步骤且算法结果有效，标记步骤为完成
  if (workflowStore.currentStep === workflowStore.STEPS.REALTIME_MONITOR && hasValidAlgorithmResult.value) {
    if (!workflowStore.completedSteps.includes(workflowStore.STEPS.REALTIME_MONITOR)) {
      workflowStore.completedSteps.push(workflowStore.STEPS.REALTIME_MONITOR)
    }
  }

  // 初始化数据日志
  updateDataLog()
})

onUnmounted(() => {
  stopMonitoring()
})

// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
}

// 开始监控
async function startMonitoring() {
  if (isMonitoring.value) return

  // 检查数据依赖
  if (!hasValidAlgorithmResult.value) {
    ElMessage.warning('请先完成算法计算才能开始监控')
    return
  }

  // 检查工作流步骤
  if (workflowStore.currentStep !== workflowStore.STEPS.REALTIME_MONITOR) {
    workflowStore.goToStep(workflowStore.STEPS.REALTIME_MONITOR)
  }

  isMonitoring.value = true
  ElMessage.success('开始实时监控')

  // 立即获取一次数据
  await fetchMonitorData()

  // 设置定时器
  monitoringTimer = setInterval(async () => {
    await fetchMonitorData()
  }, refreshInterval.value * 1000)
}

// 暂停监控
function pauseMonitoring() {
  if (!isMonitoring.value) return

  isMonitoring.value = false
  if (monitoringTimer) {
    clearInterval(monitoringTimer)
    monitoringTimer = null
  }
  ElMessage.info('已暂停监控')
}

// 停止监控
function stopMonitoring() {
  isMonitoring.value = false
  if (monitoringTimer) {
    clearInterval(monitoringTimer)
    monitoringTimer = null
  }
}

// 重置监控
function resetMonitoring() {
  stopMonitoring()
  currentData.progress = 0
  currentData.distanceTraveled = 0
  currentData.distanceRemaining = algorithmStore.result?.distance || 0
  currentData.batteryLevel = 100
  currentData.currentSpeed = 0
  currentData.currentAltitude = 0
  currentData.estimatedCompletion = '--'
  currentData.signalStrength = 100

  // 重置无人机位置到路径起点
  if (pathPoints.value.length > 0) {
    currentData.dronePosition.x = pathPoints.value[0].x
    currentData.dronePosition.y = pathPoints.value[0].y
  }

  dataLogs.value = []
  alerts.value = []
  ElMessage.info('监控已重置')
}

// 获取监控数据
async function fetchMonitorData() {
  try {
    if (demoMode.value) {
      // 演示模式：基于算法路径生成模拟数据
      await new Promise(resolve => setTimeout(resolve, 300))

      // 只有有有效算法结果才能监控
      if (!hasValidAlgorithmResult.value) {
        ElMessage.warning('请先完成算法计算才能开始监控')
        pauseMonitoring()
        return
      }

      // 模拟沿着算法路径飞行
      const totalPathPoints = pathPoints.value.length
      if (totalPathPoints === 0) return

      // 计算当前进度对应的路径索引
      const targetIndex = Math.floor(currentData.progress * totalPathPoints)
      const currentIndex = Math.min(targetIndex, totalPathPoints - 1)

      // 更新无人机位置
      if (currentIndex < totalPathPoints) {
        currentData.dronePosition.x = pathPoints.value[currentIndex].x
        currentData.dronePosition.y = pathPoints.value[currentIndex].y
      }

      // 更新进度和其他数据
      currentData.timestamp = new Date().toISOString()
      currentData.progress = Math.min(1, currentData.progress + 0.02)

      // 计算距离
      if (algorithmStore.result?.distance) {
        currentData.distanceTraveled = currentData.progress * algorithmStore.result.distance
        currentData.distanceRemaining = algorithmStore.result.distance - currentData.distanceTraveled
      }

      currentData.batteryLevel = Math.max(10, 100 - (currentData.progress * 90))
      currentData.currentSpeed = 6 + Math.random() * 3
      currentData.currentAltitude = 12 + Math.random() * 6
      currentData.signalStrength = 90 + Math.random() * 10

      // 更新预计完成时间
      if (currentData.progress < 1) {
        const remainingProgress = 1 - currentData.progress
        const estimatedSeconds = (remainingProgress / 0.02) * (refreshInterval.value) // 基于刷新间隔估算
        const completionTime = new Date(Date.now() + estimatedSeconds * 1000)
        currentData.estimatedCompletion = completionTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      } else {
        currentData.estimatedCompletion = '已完成'
      }

      // 模拟告警
      if (Math.random() < 0.05) {
        const alertTypes = [
          { level: 'info', title: '路径更新', message: '无人机已更新飞行路径' },
          { level: 'warning', title: '风速警告', message: '当前风速超过安全阈值' },
          { level: 'info', title: '喷洒状态', message: '喷洒系统工作正常' },
          { level: 'warning', title: '低电量警告', message: '电池电量低于30%' },
          { level: 'info', title: '边缘区域提醒', message: '接近作业区域边界' }
        ]
        const alert = alertTypes[Math.floor(Math.random() * alertTypes.length)]
        alerts.value.unshift({
          ...alert,
          time: new Date().toLocaleTimeString()
        })

        // 保持告警数量不超过10个
        if (alerts.value.length > 10) {
          alerts.value.pop()
        }
      }
    } else {
      // 真实模式：调用API
      const response = await simulationService.getRealtimeMonitor()
      const data = response.data

      // 更新数据
      Object.assign(currentData, data)

      // 如果有新的无人机位置，添加到路径轨迹
      if (data.dronePosition) {
        pathPoints.value.push({
          x: data.dronePosition.x,
          y: data.dronePosition.y,
          time: new Date()
        })

        // 保持路径点数不超过100个
        if (pathPoints.value.length > 100) {
          pathPoints.value.shift()
        }
      }
    }

    // 更新数据日志
    updateDataLog()

    // 如果进度完成，自动停止监控
    if (currentData.progress >= 1) {
      ElMessage.success('监控任务已完成')
      pauseMonitoring()

      // 标记步骤为完成
      if (!workflowStore.completedSteps.includes(workflowStore.STEPS.REALTIME_MONITOR)) {
        workflowStore.completedSteps.push(workflowStore.STEPS.REALTIME_MONITOR)
      }
    }

  } catch (error) {
    console.error('获取监控数据失败:', error)
    ElMessage.error('获取监控数据失败')
    pauseMonitoring()
  }
}

// 更新数据日志
function updateDataLog() {
  const now = new Date()
  const timeStr = now.toLocaleTimeString()

  dataLogs.value.unshift({
    time: timeStr,
    type: '数据更新',
    data: `位置: (${currentData.dronePosition.x.toFixed(1)}, ${currentData.dronePosition.y.toFixed(1)}), 进度: ${(currentData.progress * 100).toFixed(1)}%`
  })

  // 保持日志条数不超过20
  if (dataLogs.value.length > 20) {
    dataLogs.value.pop()
  }
}

// 地图控制
function zoomIn() {
  positionScale.value = Math.min(positionScale.value + 0.1, 2)
}

function zoomOut() {
  positionScale.value = Math.max(positionScale.value - 0.1, 0.1)
}

function resetView() {
  positionScale.value = 0.5
}

// 告警处理
function getAlertClass(level) {
  return {
    info: 'alert-info',
    warning: 'alert-warning',
    error: 'alert-error'
  }[level] || 'alert-info'
}

function getAlertIcon(level) {
  return {
    info: InfoFilled,
    warning: Warning,
    error: CircleCheck
  }[level] || InfoFilled
}

function clearAlerts() {
  alerts.value = []
  ElMessage.success('已清空所有告警')
}

// 导出数据
function exportData() {
  const exportData = {
    timestamp: new Date().toISOString(),
    currentData: { ...currentData },
    alerts: [...alerts.value],
    pathPoints: [...pathPoints.value]
  }

  const dataStr = JSON.stringify(exportData, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr)

  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', `监控数据_${new Date().getTime()}.json`)
  linkElement.click()

  ElMessage.success('数据已导出')
}

// 保存设置
function saveSettings() {
  mapImageUrl.value = settings.mapUrl
  showSettingsDialog.value = false
  ElMessage.success('设置已保存')
}
</script>

<style scoped>
/* 工作流状态栏样式 */
.workflow-status-bar {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 12px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  z-index: 20;
}

.workflow-steps {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.workflow-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  border-radius: 6px;
  cursor: pointer;
  min-width: 80px;
  transition: all 0.3s ease;
  position: relative;
}

.workflow-step:hover {
  background: #f8fafc;
}

.workflow-step.step-completed {
  color: #52c41a;
}

.workflow-step.step-current {
  color: #1890ff;
  background: #e6f7ff;
  font-weight: 600;
}

.workflow-step.step-upcoming {
  color: #8c8c8c;
  opacity: 0.7;
}

.workflow-step.step-pending {
  color: #ff4d4f;
}

.workflow-step.step-skipped {
  color: #faad14;
  opacity: 0.5;
}

.step-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.step-label {
  font-size: 12px;
  text-align: center;
  white-space: nowrap;
}

.step-validation {
  position: absolute;
  top: 0;
  right: 4px;
}

.workflow-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-label {
  font-size: 12px;
  color: #6b7280;
  min-width: 80px;
}

.current-validation {
  display: flex;
  align-items: center;
  font-size: 12px;
  margin-left: 12px;
}

.monitor-container {
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

.interval-control {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.status-hint {
  margin-left: 16px;
}

.right-controls {
  display: flex;
  gap: 8px;
}

.monitor-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #f5f5f5;
}

.monitor-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px;
}

.map-monitor {
  flex: 2;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.map-title {
  padding: 12px 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;
}

.map-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.map-background {
  position: relative;
  width: 100%;
  height: 100%;
  background: #1a202c;
  display: flex;
  justify-content: center;
  align-items: center;
}

.map-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.drone-marker {
  position: absolute;
  transform: translate(-50%, -50%);
  z-index: 20;
}

.drone-icon {
  position: relative;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.drone-info {
  position: absolute;
  top: -40px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0.9;
}

.drone-label {
  font-weight: bold;
}

.drone-coords {
  font-size: 10px;
  opacity: 0.8;
}

.map-controls {
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
  background: #f8fafc;
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-level {
  margin-left: auto;
  font-size: 14px;
  color: #6b7280;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.card-header {
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.card-body {
  padding: 16px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.progress-label {
  font-size: 14px;
  color: #6b7280;
  min-width: 80px;
}

.progress-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  min-width: 60px;
  text-align: right;
}

.progress-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.detail-label {
  color: #6b7280;
}

.detail-value {
  color: #1f2937;
  font-weight: 500;
}

.status-indicators {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-item.status-error {
  color: #ef4444;
}

.status-label {
  flex: 1;
  font-size: 13px;
  color: #6b7280;
  min-width: 60px;
}

.status-value {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  min-width: 60px;
  text-align: right;
}

.monitor-right {
  width: 400px;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.alerts-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-bottom: 1px solid #e5e7eb;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.alerts-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.alert-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  border-left: 4px solid #3b82f6;
}

.alert-item.alert-info {
  border-left-color: #3b82f6;
  background: #eff6ff;
}

.alert-item.alert-warning {
  border-left-color: #f59e0b;
  background: #fffbeb;
}

.alert-item.alert-error {
  border-left-color: #ef4444;
  background: #fef2f2;
}

.alert-icon {
  font-size: 20px;
  margin-top: 2px;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 2px;
}

.alert-message {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.alert-time {
  font-size: 11px;
  color: #9ca3af;
}

.history-chart {
  height: 200px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.chart-container {
  flex: 1;
  padding: 16px;
}

.chart-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px dashed #e5e7eb;
}

.data-stream {
  height: 200px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.stream-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.stream-item {
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.stream-time {
  font-size: 11px;
  color: #9ca3af;
  margin-bottom: 2px;
}

.stream-content {
  display: flex;
  gap: 4px;
  font-size: 13px;
}

.stream-type {
  color: #6b7280;
}

.stream-data {
  color: #1f2937;
  font-weight: 500;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .monitor-content {
    flex-direction: column;
  }

  .monitor-right {
    width: 100%;
    height: 40%;
  }

  .status-cards {
    grid-template-columns: 1fr;
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
}
</style>