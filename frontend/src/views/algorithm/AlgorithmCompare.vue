<template>
  <div class="compare-container">
    <!-- 工作流状态栏 -->
    <div class="workflow-status" v-if="workflowStore.currentStep === workflowStore.STEPS.ALGORITHM_SELECTION ||
                                      workflowStore.currentStep === workflowStore.STEPS.PARAMETER_CONFIG">
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
          请先{{ validation.message || '完成当前步骤' }}
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
        <!-- 模式切换 -->
        <el-switch
          v-model="demoMode"
          active-text="演示模式"
          inactive-text="真实模式"
          style="margin-right: 16px;"
          @change="toggleMode"
        />

        <!-- 场景选择 -->
        <el-select v-model="selectedScenario" placeholder="选择测试场景" style="width: 180px; margin-right: 16px;">
          <el-option label="标准农田" value="standard-farm" />
          <el-option label="复杂地形" value="complex-terrain" />
          <el-option label="多障碍物" value="multi-obstacle" />
          <el-option label="大型农田" value="large-farm" />
          <el-option label="自定义场景" value="custom" />
        </el-select>

        <!-- 操作按钮 -->
        <el-button type="primary" :icon="VideoPlay" @click="runComparison" :loading="isCalculating">
          执行对比
        </el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>

      <div class="right-controls">
        <el-button :icon="Download" @click="exportComparison">导出结果</el-button>
        <el-button :icon="Setting" @click="showConfigDialog = true">
          对比配置
        </el-button>
      </div>
    </div>

    <div class="compare-content">
      <!-- 顶部概览 -->
      <div class="overview-section">
        <div class="overview-card">
          <div class="overview-icon" style="background-color: #e6f7ff;">
            <el-icon size="24" color="#1890ff"><Trophy /></el-icon>
          </div>
          <div class="overview-content">
            <div class="overview-label">最佳算法</div>
            <div class="overview-value">{{ bestAlgorithm.name || '未计算' }}</div>
            <div class="overview-desc">{{ bestAlgorithm.reason || '请执行算法对比' }}</div>
          </div>
        </div>

        <div class="overview-card">
          <div class="overview-icon" style="background-color: #f6ffed;">
            <el-icon size="24" color="#52c41a"><TrendCharts /></el-icon>
          </div>
          <div class="overview-content">
            <div class="overview-label">综合得分</div>
            <div class="overview-value">{{ overallScore.toFixed(2) }}</div>
            <div class="overview-desc">最高分: {{ maxScore.toFixed(2) }}</div>
          </div>
        </div>

        <div class="overview-card">
          <div class="overview-icon" style="background-color: #fff7e6;">
            <el-icon size="24" color="#fa8c16"><Clock /></el-icon>
          </div>
          <div class="overview-content">
            <div class="overview-label">最快算法</div>
            <div class="overview-value">{{ fastestAlgorithm.name || '未计算' }}</div>
            <div class="overview-desc">{{ fastestAlgorithm.time || '0' }}ms</div>
          </div>
        </div>

        <div class="overview-card">
          <div class="overview-icon" style="background-color: #f9f0ff;">
            <el-icon size="24" color="#722ed1"><Connection /></el-icon>
          </div>
          <div class="overview-content">
            <div class="overview-label">最短路径</div>
            <div class="overview-value">{{ shortestAlgorithm.name || '未计算' }}</div>
            <div class="overview-desc">{{ shortestAlgorithm.distance || '0' }}米</div>
          </div>
        </div>
      </div>

      <!-- 对比图表和表格 -->
      <div class="main-content">
        <!-- 左侧图表 -->
        <div class="chart-section">
          <div class="chart-header">
            <h3>算法性能对比</h3>
            <div class="chart-tabs">
              <el-radio-group v-model="activeChart" size="small">
                <el-radio-button label="radar">雷达图</el-radio-button>
                <el-radio-button label="bar">柱状图</el-radio-button>
                <el-radio-button label="score">得分图</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div class="chart-container">
            <canvas ref="chartCanvas" :width="chartWidth" :height="chartHeight"></canvas>
          </div>

          <div class="chart-legend">
            <div class="legend-item" v-for="algorithm in algorithms" :key="algorithm.id">
              <div class="legend-color" :style="{ backgroundColor: algorithm.color }"></div>
              <span>{{ algorithm.name }}</span>
            </div>
          </div>
        </div>

        <!-- 右侧详细数据 -->
        <div class="detail-section">
          <div class="detail-header">
            <h3>详细对比数据</h3>
            <el-button :icon="RefreshRight" @click="refreshData">刷新</el-button>
          </div>

          <el-table :data="algorithms" style="width: 100%;" height="calc(100vh - 480px)">
            <el-table-column prop="name" label="算法" width="120" fixed>
              <template #default="{ row }">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <div class="algorithm-color" :style="{ backgroundColor: row.color }"></div>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="路径长度" width="100" sortable>
              <template #default="{ row }">
                <div class="metric-cell">
                  <span class="metric-value">{{ row.distance.toFixed(1) }}</span>
                  <span class="metric-unit">米</span>
                  <el-progress
                    :percentage="(row.distance / maxDistance * 100)"
                    :show-text="false"
                    status="exception"
                    style="margin-top: 4px;"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="计算时间" width="100" sortable>
              <template #default="{ row }">
                <div class="metric-cell">
                  <span class="metric-value">{{ row.timeMs }}</span>
                  <span class="metric-unit">ms</span>
                  <el-progress
                    :percentage="(row.timeMs / maxTime * 100)"
                    :show-text="false"
                    status="warning"
                    style="margin-top: 4px;"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="覆盖率" width="100" sortable>
              <template #default="{ row }">
                <div class="metric-cell">
                  <span class="metric-value">{{ (row.coverage * 100).toFixed(1) }}%</span>
                  <el-progress
                    :percentage="row.coverage * 100"
                    :show-text="false"
                    status="success"
                    style="margin-top: 4px;"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="效率" width="100" sortable>
              <template #default="{ row }">
                <div class="metric-cell">
                  <span class="metric-value">{{ row.efficiency.toFixed(2) }}</span>
                  <el-progress
                    :percentage="row.efficiency * 100"
                    :show-text="false"
                    style="margin-top: 4px;"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="能耗" width="100" sortable>
              <template #default="{ row }">
                <div class="metric-cell">
                  <span class="metric-value">{{ row.energy.toFixed(1) }}</span>
                  <span class="metric-unit">Wh</span>
                  <el-progress
                    :percentage="(row.energy / maxEnergy * 100)"
                    :show-text="false"
                    status="exception"
                    style="margin-top: 4px;"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="综合得分" width="100" sortable>
              <template #default="{ row }">
                <div class="score-cell">
                  <el-rate
                    v-model="row.score"
                    disabled
                    show-score
                    text-color="#ff9900"
                    score-template="{value}"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="推荐度" width="120">
              <template #default="{ row }">
                <el-tag :type="getRecommendationType(row.recommendation)" size="small">
                  {{ row.recommendation }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="viewAlgorithmDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 权重配置 -->
          <div class="weight-config">
            <h4>指标权重配置</h4>
            <div class="weight-sliders">
              <div class="weight-item">
                <span>路径长度权重</span>
                <el-slider v-model="weights.distance" :min="0" :max="10" :step="1" style="width: 150px;" />
                <span>{{ weights.distance }}</span>
              </div>
              <div class="weight-item">
                <span>计算时间权重</span>
                <el-slider v-model="weights.time" :min="0" :max="10" :step="1" style="width: 150px;" />
                <span>{{ weights.time }}</span>
              </div>
              <div class="weight-item">
                <span>覆盖率权重</span>
                <el-slider v-model="weights.coverage" :min="0" :max="10" :step="1" style="width: 150px;" />
                <span>{{ weights.coverage }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部推荐 -->
      <div class="recommendation-section">
        <div class="recommendation-header">
          <el-icon size="20" color="#1890ff"><Star /></el-icon>
          <h3>算法推荐</h3>
        </div>
        <div class="recommendation-content">
          <div class="recommendation-card" v-for="rec in recommendations" :key="rec.type">
            <div class="rec-icon" :style="{ backgroundColor: rec.color }">
              <el-icon size="20"><component :is="rec.icon" /></el-icon>
            </div>
            <div class="rec-content">
              <div class="rec-title">{{ rec.title }}</div>
              <div class="rec-algorithm">{{ rec.algorithm }}</div>
              <div class="rec-reason">{{ rec.reason }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 配置对话框 -->
    <el-dialog v-model="showConfigDialog" title="对比配置" width="500">
      <el-form :model="config" label-width="100px">
        <el-form-item label="对比算法">
          <el-checkbox-group v-model="config.selectedAlgorithms">
            <el-checkbox label="a-star" disabled>A*算法</el-checkbox>
            <el-checkbox label="dijkstra" disabled>Dijkstra算法</el-checkbox>
            <el-checkbox label="boustrophedon" disabled>Boustrophedon算法</el-checkbox>
            <el-checkbox label="genetic" disabled>遗传算法</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="评估指标">
          <el-checkbox-group v-model="config.metrics">
            <el-checkbox label="distance">路径长度</el-checkbox>
            <el-checkbox label="time">计算时间</el-checkbox>
            <el-checkbox label="coverage">覆盖率</el-checkbox>
            <el-checkbox label="energy">能耗</el-checkbox>
            <el-checkbox label="efficiency">效率</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="测试次数">
          <el-input-number v-model="config.testCount" :min="1" :max="10" />
        </el-form-item>

        <el-form-item label="场景参数">
          <el-input v-model="config.scenarioParams" type="textarea" :rows="3" placeholder='JSON格式，如：{"obstacleCount": 10}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showConfigDialog = false">取消</el-button>
          <el-button type="primary" @click="saveConfig">保存</el-button>
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
  Download,
  Setting,
  RefreshRight,
  Trophy,
  TrendCharts,
  Clock,
  Connection,
  Star,
  ScaleToOriginal,
  Lightning,
  DataLine
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode, algorithmService } from '@/services/dataService'
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

// 数据依赖检查
const hasValidComparisonData = computed(() => {
  return algorithmStore.historyResults.length >= 2
})

const dataValidation = computed(() => {
  if (algorithmStore.historyResults.length < 2) {
    return {
      valid: false,
      message: `需要至少2个算法结果进行对比，当前只有${algorithmStore.historyResults.length}个`,
      suggestion: '请先在不同的算法页面（基础算法或农业算法）执行计算'
    }
  }

  // 检查是否包含不同算法的结果
  const uniqueAlgorithms = new Set(algorithmStore.historyResults.map(r => r.algorithm))
  if (uniqueAlgorithms.size < 2) {
    return {
      valid: false,
      message: `需要至少2种不同算法的结果进行对比，当前只有${uniqueAlgorithms.size}种`,
      suggestion: '请尝试计算不同类型的算法（如A*、Dijkstra、农业算法）'
    }
  }

  return {
    valid: true,
    message: '有足够的算法结果进行对比分析',
    suggestion: null
  }
})

// 模式控制
const demoMode = ref(getCurrentMode())

// 场景选择
const selectedScenario = ref('standard-farm')

// 计算状态
const isCalculating = ref(false)

// 图表配置
const activeChart = ref('radar')
const chartCanvas = ref(null)
const chartWidth = ref(500)
const chartHeight = ref(300)
const chartCtx = ref(null)

// 配置
const showConfigDialog = ref(false)
const config = reactive({
  selectedAlgorithms: ['a-star', 'dijkstra', 'boustrophedon', 'genetic'],
  metrics: ['distance', 'time', 'coverage', 'energy', 'efficiency'],
  testCount: 3,
  scenarioParams: '{"obstacleCount": 15, "mapSize": 1000}'
})

// 权重配置
const weights = reactive({
  distance: 8,
  time: 6,
  coverage: 9,
  energy: 7,
  efficiency: 8
})

// 算法数据
const algorithms = ref([])

// 预生成的测试对比数据
const mockComparison = [
  {
    id: 'a-star',
    name: 'A*算法',
    color: '#ff6b6b',
    distance: 450.75,
    timeMs: 120,
    coverage: 0.92,
    efficiency: 0.85,
    energy: 4200,
    score: 4.2,
    recommendation: '强烈推荐'
  },
  {
    id: 'dijkstra',
    name: 'Dijkstra算法',
    color: '#4ecdc4',
    distance: 480.20,
    timeMs: 180,
    coverage: 0.88,
    efficiency: 0.78,
    energy: 4500,
    score: 3.8,
    recommendation: '推荐'
  },
  {
    id: 'boustrophedon',
    name: 'Boustrophedon算法',
    color: '#45b7d1',
    distance: 520.10,
    timeMs: 95,
    coverage: 0.95,
    efficiency: 0.91,
    energy: 3800,
    score: 4.5,
    recommendation: '最优选择'
  },
  {
    id: 'genetic',
    name: '遗传算法',
    color: '#9d65c9',
    distance: 460.30,
    timeMs: 320,
    coverage: 0.90,
    efficiency: 0.82,
    energy: 4800,
    score: 3.9,
    recommendation: '一般'
  }
]

// 推荐信息
const recommendations = computed(() => {
  if (algorithms.value.length === 0) {
    // 如果没有数据，返回默认推荐
    return [
      {
        type: 'balanced',
        title: '平衡性能',
        algorithm: 'A*算法',
        reason: '路径长度、计算时间和覆盖率均衡表现',
        color: '#e6f7ff',
        icon: 'ScaleToOriginal'
      },
      {
        type: 'fastest',
        title: '最快速度',
        algorithm: 'Boustrophedon算法',
        reason: '计算时间最短，适合实时应用',
        color: '#f6ffed',
        icon: 'Lightning'
      },
      {
        type: 'shortest',
        title: '最短路径',
        algorithm: 'A*算法',
        reason: '路径长度最优，节省能源',
        color: '#fff7e6',
        icon: 'Connection'
      },
      {
        type: 'coverage',
        title: '最高覆盖率',
        algorithm: 'Boustrophedon算法',
        reason: '覆盖最全面，适合农田作业',
        color: '#f9f0ff',
        icon: 'DataLine'
      }
    ]
  }

  // 基于实际算法结果生成推荐
  const recs = []

  // 1. 平衡性能推荐（综合得分最高）
  const balanced = algorithms.value.reduce((prev, current) =>
    (prev.score > current.score) ? prev : current
  )
  if (balanced) {
    recs.push({
      type: 'balanced',
      title: '平衡性能',
      algorithm: balanced.name,
      reason: `综合得分最高（${balanced.score.toFixed(1)}分），各方面表现均衡`,
      color: '#e6f7ff',
      icon: 'Scale'
    })
  }

  // 2. 最快速度推荐
  const fastest = algorithms.value.reduce((prev, current) =>
    (prev.timeMs < current.timeMs) ? prev : current
  )
  if (fastest && fastest !== balanced) {
    recs.push({
      type: 'fastest',
      title: '最快速度',
      algorithm: fastest.name,
      reason: `计算时间最短（${fastest.timeMs}ms），适合实时应用`,
      color: '#f6ffed',
      icon: 'Lightning'
    })
  }

  // 3. 最短路径推荐
  const shortest = algorithms.value.reduce((prev, current) =>
    (prev.distance < current.distance) ? prev : current
  )
  if (shortest && shortest !== balanced && shortest !== fastest) {
    recs.push({
      type: 'shortest',
      title: '最短路径',
      algorithm: shortest.name,
      reason: `路径长度最短（${shortest.distance.toFixed(1)}米），节省能源`,
      color: '#fff7e6',
      icon: 'Connection'
    })
  }

  // 4. 最高覆盖率推荐
  const bestCoverage = algorithms.value.reduce((prev, current) =>
    (prev.coverage > current.coverage) ? prev : current
  )
  if (bestCoverage && bestCoverage !== balanced && bestCoverage !== fastest && bestCoverage !== shortest) {
    recs.push({
      type: 'coverage',
      title: '最高覆盖率',
      algorithm: bestCoverage.name,
      reason: `覆盖率最高（${(bestCoverage.coverage * 100).toFixed(1)}%），适合农田作业`,
      color: '#f9f0ff',
      icon: 'DataLine'
    })
  }

  // 如果推荐不足4个，用默认值填充
  while (recs.length < 4) {
    const defaultRecs = [
      {
        type: 'balanced',
        title: '平衡性能',
        algorithm: 'A*算法',
        reason: '路径长度、计算时间和覆盖率均衡表现',
        color: '#e6f7ff',
        icon: 'ScaleToOriginal'
      },
      {
        type: 'fastest',
        title: '最快速度',
        algorithm: 'Boustrophedon算法',
        reason: '计算时间最短，适合实时应用',
        color: '#f6ffed',
        icon: 'Lightning'
      },
      {
        type: 'shortest',
        title: '最短路径',
        algorithm: 'A*算法',
        reason: '路径长度最优，节省能源',
        color: '#fff7e6',
        icon: 'Connection'
      },
      {
        type: 'coverage',
        title: '最高覆盖率',
        algorithm: 'Boustrophedon算法',
        reason: '覆盖最全面，适合农田作业',
        color: '#f9f0ff',
        icon: 'DataLine'
      }
    ]
    const defaultRec = defaultRecs[recs.length]
    if (!recs.some(r => r.type === defaultRec.type)) {
      recs.push(defaultRec)
    } else {
      break
    }
  }

  return recs.slice(0, 4) // 最多返回4个推荐
})

// 计算属性
const bestAlgorithm = computed(() => {
  if (algorithms.value.length === 0) return {}
  const best = algorithms.value.reduce((prev, current) =>
    (prev.score > current.score) ? prev : current
  )
  return {
    name: best.name,
    reason: '综合得分最高'
  }
})

const fastestAlgorithm = computed(() => {
  if (algorithms.value.length === 0) return {}
  const fastest = algorithms.value.reduce((prev, current) =>
    (prev.timeMs < current.timeMs) ? prev : current
  )
  return {
    name: fastest.name,
    time: fastest.timeMs
  }
})

const shortestAlgorithm = computed(() => {
  if (algorithms.value.length === 0) return {}
  const shortest = algorithms.value.reduce((prev, current) =>
    (prev.distance < current.distance) ? prev : current
  )
  return {
    name: shortest.name,
    distance: shortest.distance.toFixed(1)
  }
})

const overallScore = computed(() => {
  if (algorithms.value.length === 0) return 0
  const scores = algorithms.value.map(a => a.score)
  return scores.reduce((sum, score) => sum + score, 0) / scores.length
})

const maxScore = computed(() => {
  if (algorithms.value.length === 0) return 0
  return Math.max(...algorithms.value.map(a => a.score))
})

const maxDistance = computed(() => {
  if (algorithms.value.length === 0) return 1
  return Math.max(...algorithms.value.map(a => a.distance))
})

const maxTime = computed(() => {
  if (algorithms.value.length === 0) return 1
  return Math.max(...algorithms.value.map(a => a.timeMs))
})

const maxEnergy = computed(() => {
  if (algorithms.value.length === 0) return 1
  return Math.max(...algorithms.value.map(a => a.energy))
})

// 将算法历史结果转换为对比格式
const algorithmResultsForComparison = computed(() => {
  if (demoMode.value) {
    // 演示模式：使用模拟数据
    return [...mockComparison]
  }

  // 真实模式：基于algorithmStore的历史结果
  if (algorithmStore.historyResults.length === 0) {
    return []
  }

  // 为每种算法只取最新的结果
  const algorithmMap = new Map()
  algorithmStore.historyResults.forEach(result => {
    if (!algorithmMap.has(result.algorithm) || result.id > algorithmMap.get(result.algorithm).id) {
      algorithmMap.set(result.algorithm, result)
    }
  })

  const algorithmColors = {
    'a-star': '#ff6b6b',
    'dijkstra': '#4ecdc4',
    'genetic': '#9d65c9',
    'boustrophedon': '#9d65c9' // 为兼容性添加
  }

  const algorithmNames = {
    'a-star': 'A*算法',
    'dijkstra': 'Dijkstra算法',
    'genetic': '遗传算法',
    'boustrophedon': 'Boustrophedon算法'
  }

  const comparisonData = []

  algorithmMap.forEach((result, algorithmType) => {
    const resultData = result.result || {}
    const algorithmData = {
      id: algorithmType,
      name: algorithmNames[algorithmType] || algorithmType,
      color: algorithmColors[algorithmType] || '#999999',
      distance: resultData.distance || 0,
      timeMs: resultData.timeMs || 0,
      coverage: resultData.coverage || 0,
      efficiency: resultData.efficiency || 0.5,
      energy: calculateEnergy(resultData.distance, algorithmType),
      score: 0, // 将由calculateScores计算
      recommendation: '待评估',
      rawResult: result // 保存原始结果引用
    }

    // 如果缺少效率值，基于覆盖率和时间估算
    if (algorithmData.efficiency === 0.5 && resultData.coverage && resultData.timeMs) {
      algorithmData.efficiency = Math.min(0.95, resultData.coverage * (1 - Math.min(resultData.timeMs / 1000, 0.5)))
    }

    comparisonData.push(algorithmData)
  })

  return comparisonData
})

// 计算能耗（基于距离和算法类型）
function calculateEnergy(distance, algorithmType) {
  const baseEnergy = distance * 0.1 // 基础能耗：每米0.1Wh

  // 不同算法的能耗系数
  const energyFactors = {
    'a-star': 1.0,
    'dijkstra': 1.2,
    'genetic': 1.5,
    'boustrophedon': 0.8
  }

  return baseEnergy * (energyFactors[algorithmType] || 1.0)
}

// 初始化
onMounted(() => {
  console.log('AlgorithmCompare组件已挂载')
  console.log('工作流状态:', workflowStore.currentStep)
  console.log('算法历史结果:', algorithmStore.historyResults)
  console.log('数据验证:', dataValidation.value)
  initChart()
  loadComparisonData()
})

// 初始化图表
function initChart() {
  if (!chartCanvas.value) return
  chartCtx.value = chartCanvas.value.getContext('2d')
  drawChart()
}

// 绘制图表
function drawChart() {
  if (!chartCtx.value || algorithms.value.length === 0) return

  const ctx = chartCtx.value
  const width = chartWidth.value
  const height = chartHeight.value
  const padding = 40

  // 清除画布
  ctx.clearRect(0, 0, width, height)

  // 绘制背景网格
  ctx.strokeStyle = '#f0f0f0'
  ctx.lineWidth = 1

  // 垂直网格线
  for (let x = padding; x <= width - padding; x += (width - 2 * padding) / 5) {
    ctx.beginPath()
    ctx.moveTo(x, padding)
    ctx.lineTo(x, height - padding)
    ctx.stroke()
  }

  // 水平网格线
  for (let y = padding; y <= height - padding; y += (height - 2 * padding) / 5) {
    ctx.beginPath()
    ctx.moveTo(padding, y)
    ctx.lineTo(width - padding, y)
    ctx.stroke()
  }

  // 绘制坐标轴
  ctx.strokeStyle = '#333'
  ctx.lineWidth = 2

  // X轴
  ctx.beginPath()
  ctx.moveTo(padding, height - padding)
  ctx.lineTo(width - padding, height - padding)
  ctx.stroke()

  // Y轴
  ctx.beginPath()
  ctx.moveTo(padding, padding)
  ctx.lineTo(padding, height - padding)
  ctx.stroke()

  // 根据图表类型绘制数据
  if (activeChart.value === 'radar') {
    drawRadarChart()
  } else if (activeChart.value === 'bar') {
    drawBarChart()
  } else {
    drawScoreChart()
  }
}

// 绘制雷达图
function drawRadarChart() {
  // 简化实现，实际项目中可以使用ECharts等图表库
  const ctx = chartCtx.value
  const width = chartWidth.value
  const height = chartHeight.value
  const centerX = width / 2
  const centerY = height / 2
  const radius = Math.min(width, height) / 2 - 60

  // 绘制雷达图框架
  ctx.strokeStyle = '#ccc'
  ctx.lineWidth = 1

  // 同心圆
  for (let r = radius / 3; r <= radius; r += radius / 3) {
    ctx.beginPath()
    for (let i = 0; i < 5; i++) {
      const angle = (i * Math.PI * 2) / 5 - Math.PI / 2
      const x = centerX + r * Math.cos(angle)
      const y = centerY + r * Math.sin(angle)
      if (i === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    }
    ctx.closePath()
    ctx.stroke()
  }

  // 轴线
  for (let i = 0; i < 5; i++) {
    const angle = (i * Math.PI * 2) / 5 - Math.PI / 2
    const x = centerX + radius * Math.cos(angle)
    const y = centerY + radius * Math.sin(angle)
    ctx.beginPath()
    ctx.moveTo(centerX, centerY)
    ctx.lineTo(x, y)
    ctx.stroke()
  }

  // 绘制算法数据（简化）
  algorithms.value.forEach((algorithm, index) => {
    const points = []
    const metrics = ['distance', 'timeMs', 'coverage', 'efficiency', 'energy']

    metrics.forEach((metric, i) => {
      const angle = (i * Math.PI * 2) / 5 - Math.PI / 2
      const maxVal = Math.max(...algorithms.value.map(a => a[metric]))
      const normalized = algorithm[metric] / maxVal
      const r = radius * normalized
      const x = centerX + r * Math.cos(angle)
      const y = centerY + r * Math.sin(angle)
      points.push({ x, y })
    })

    // 绘制多边形
    ctx.fillStyle = algorithm.color + '40' // 半透明
    ctx.strokeStyle = algorithm.color
    ctx.lineWidth = 2

    ctx.beginPath()
    points.forEach((point, i) => {
      if (i === 0) ctx.moveTo(point.x, point.y)
      else ctx.lineTo(point.x, point.y)
    })
    ctx.closePath()
    ctx.fill()
    ctx.stroke()
  })
}

// 绘制柱状图
function drawBarChart() {
  const ctx = chartCtx.value
  const width = chartWidth.value
  const height = chartHeight.value
  const padding = 60
  const chartWidthInner = width - 2 * padding
  const chartHeightInner = height - 2 * padding

  const barWidth = chartWidthInner / (algorithms.value.length * 2)
  const maxDistanceVal = Math.max(...algorithms.value.map(a => a.distance))

  // 绘制柱状图
  algorithms.value.forEach((algorithm, index) => {
    const x = padding + index * barWidth * 2 + barWidth / 2
    const barHeight = (algorithm.distance / maxDistanceVal) * chartHeightInner

    // 绘制柱子
    ctx.fillStyle = algorithm.color
    ctx.fillRect(x, height - padding - barHeight, barWidth, barHeight)

    // 绘制标签
    ctx.fillStyle = '#333'
    ctx.font = '12px Arial'
    ctx.textAlign = 'center'
    ctx.fillText(algorithm.name, x + barWidth / 2, height - padding + 20)
    ctx.fillText(algorithm.distance.toFixed(1), x + barWidth / 2, height - padding - barHeight - 10)
  })
}

// 绘制得分图
function drawScoreChart() {
  const ctx = chartCtx.value
  const width = chartWidth.value
  const height = chartHeight.value
  const padding = 60
  const chartWidthInner = width - 2 * padding
  const chartHeightInner = height - 2 * padding

  const barWidth = chartWidthInner / (algorithms.value.length * 1.5)
  const maxScoreVal = Math.max(...algorithms.value.map(a => a.score))

  // 绘制得分柱状图
  algorithms.value.forEach((algorithm, index) => {
    const x = padding + index * barWidth * 1.5 + barWidth / 2
    const barHeight = (algorithm.score / maxScoreVal) * chartHeightInner

    // 绘制柱子
    const gradient = ctx.createLinearGradient(x, height - padding - barHeight, x, height - padding)
    gradient.addColorStop(0, algorithm.color)
    gradient.addColorStop(1, '#fff')
    ctx.fillStyle = gradient
    ctx.fillRect(x, height - padding - barHeight, barWidth, barHeight)

    // 绘制边框
    ctx.strokeStyle = algorithm.color
    ctx.lineWidth = 2
    ctx.strokeRect(x, height - padding - barHeight, barWidth, barHeight)

    // 绘制标签
    ctx.fillStyle = '#333'
    ctx.font = '12px Arial'
    ctx.textAlign = 'center'
    ctx.fillText(algorithm.name, x + barWidth / 2, height - padding + 20)
    ctx.fillText(algorithm.score.toFixed(1), x + barWidth / 2, height - padding - barHeight - 10)
  })
}

// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
}

// 加载对比数据
async function loadComparisonData() {
  try {
    // 检查数据依赖
    if (!dataValidation.value.valid) {
      ElMessage.warning(dataValidation.value.message)
      if (dataValidation.value.suggestion) {
        ElMessage.info(dataValidation.value.suggestion)
      }

      // 如果没有足够的数据，使用演示数据或空数组
      if (demoMode.value) {
        algorithms.value = [...mockComparison]
        calculateScores()
        ElMessage.success('加载演示对比数据成功（真实数据不足）')
      } else {
        algorithms.value = []
        ElMessage.warning('无法加载对比数据：' + dataValidation.value.message)
      }
      return
    }

    // 使用转换后的数据
    algorithms.value = algorithmResultsForComparison.value

    if (algorithms.value.length === 0) {
      // 如果转换后没有数据，使用演示数据
      if (demoMode.value) {
        algorithms.value = [...mockComparison]
        calculateScores()
        ElMessage.success('加载演示对比数据成功')
      } else {
        ElMessage.warning('没有可对比的算法结果')
      }
    } else {
      calculateScores()
      ElMessage.success(`已加载${algorithms.value.length}个算法结果进行对比`)
    }
  } catch (error) {
    console.error('加载对比数据失败:', error)
    algorithms.value = [...mockComparison]
    calculateScores()
    ElMessage.error('加载对比数据失败，使用演示数据')
  }
}

// 计算算法得分
function calculateScores() {
  algorithms.value.forEach(algorithm => {
    // 标准化各项指标（0-1范围，值越大越好）
    const maxDist = Math.max(...algorithms.value.map(a => a.distance))
    const maxTime = Math.max(...algorithms.value.map(a => a.timeMs))
    const maxEnergy = Math.max(...algorithms.value.map(a => a.energy))

    const normDistance = 1 - (algorithm.distance / maxDist)  // 距离越小越好
    const normTime = 1 - (algorithm.timeMs / maxTime)       // 时间越短越好
    const normCoverage = algorithm.coverage                 // 覆盖率越大越好
    const normEfficiency = algorithm.efficiency             // 效率越大越好
    const normEnergy = 1 - (algorithm.energy / maxEnergy)   // 能耗越小越好

    // 加权得分
    const totalWeight = weights.distance + weights.time + weights.coverage + weights.energy + weights.efficiency
    const score = (
      normDistance * weights.distance +
      normTime * weights.time +
      normCoverage * weights.coverage +
      normEfficiency * weights.efficiency +
      normEnergy * weights.energy
    ) / totalWeight * 5  // 转换为5分制

    algorithm.score = Math.min(5, Math.max(0, score))

    // 设置推荐度
    if (algorithm.score >= 4.0) {
      algorithm.recommendation = '强烈推荐'
    } else if (algorithm.score >= 3.0) {
      algorithm.recommendation = '推荐'
    } else if (algorithm.score >= 2.0) {
      algorithm.recommendation = '一般'
    } else {
      algorithm.recommendation = '不推荐'
    }
  })

  // 重绘图表
  drawChart()
}

// 执行对比
async function runComparison() {
  if (isCalculating.value) return

  isCalculating.value = true

  try {
    if (demoMode.value) {
      // 演示模式：模拟计算延迟
      await new Promise(resolve => setTimeout(resolve, 1500))

      // 为模拟数据添加一些随机变化
      algorithms.value = mockComparison.map(algorithm => ({
        ...algorithm,
        distance: algorithm.distance * (0.9 + Math.random() * 0.2),
        timeMs: algorithm.timeMs * (0.8 + Math.random() * 0.4),
        coverage: Math.min(1, algorithm.coverage * (0.95 + Math.random() * 0.1)),
        efficiency: Math.min(1, algorithm.efficiency * (0.9 + Math.random() * 0.2)),
        energy: algorithm.energy * (0.85 + Math.random() * 0.3)
      }))
    } else {
      // 真实模式：重新从Store加载数据
      // 检查数据依赖
      if (!dataValidation.value.valid) {
        ElMessage.warning(dataValidation.value.message)
        if (dataValidation.value.suggestion) {
          ElMessage.info(dataValidation.value.suggestion)
        }
        return
      }

      // 使用最新的算法结果
      algorithms.value = algorithmResultsForComparison.value

      if (algorithms.value.length === 0) {
        ElMessage.warning('没有可对比的算法结果')
        return
      }
    }

    // 计算得分
    calculateScores()

    ElMessage.success(`算法对比完成，共对比${algorithms.value.length}个算法`)
  } catch (error) {
    console.error('算法对比失败:', error)
    ElMessage.error('算法对比失败，请重试')
  } finally {
    isCalculating.value = false
  }
}

// 重置
function reset() {
  if (demoMode.value) {
    algorithms.value = [...mockComparison]
  } else {
    // 真实模式：重新加载Store中的数据
    algorithms.value = algorithmResultsForComparison.value
    if (algorithms.value.length === 0) {
      ElMessage.warning('没有算法结果可重置，请先执行算法计算')
      return
    }
  }
  calculateScores()
  ElMessage.info('已重置对比数据')
}

// 导出对比结果
function exportComparison() {
  if (algorithms.value.length === 0) {
    ElMessage.warning('没有可导出的对比数据')
    return
  }

  const exportData = {
    scenario: selectedScenario.value,
    mode: demoMode.value ? '演示模式' : '真实模式',
    config: config,
    weights: weights,
    algorithms: algorithms.value.map(alg => {
      // 移除原始结果引用，避免导出过大的对象
      const { rawResult, ...exportAlg } = alg
      return exportAlg
    }),
    summary: {
      bestAlgorithm: bestAlgorithm.value,
      fastestAlgorithm: fastestAlgorithm.value,
      shortestAlgorithm: shortestAlgorithm.value,
      overallScore: overallScore.value,
      totalAlgorithms: algorithms.value.length,
      dataSource: demoMode.value ? '演示数据' : '真实算法计算结果',
      timestamp: new Date().toISOString()
    },
    dataSourceInfo: {
      mapId: mapStore.selectedMapId,
      nodeCount: mapStore.nodes.length,
      hasStartNode: !!mapStore.startNode,
      hasEndNode: !!mapStore.endNode,
      algorithmResultsCount: algorithmStore.historyResults.length
    }
  }

  const dataStr = JSON.stringify(exportData, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr)
  const exportFileName = `算法对比_${selectedScenario.value}_${demoMode.value ? 'demo' : 'real'}_${new Date().getTime()}.json`

  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', exportFileName)
  linkElement.click()

  ElMessage.success('对比结果已导出')
}

// 刷新数据
function refreshData() {
  loadComparisonData()
}

// 查看算法详情
function viewAlgorithmDetail(algorithm) {
  if (algorithm.rawResult) {
    // 如果有原始结果，显示详细信息
    ElMessage.info(`查看 ${algorithm.name} 的详细分析`)
    console.log('算法原始结果:', algorithm.rawResult)

    // 在实际项目中，这里可以打开详情对话框显示完整信息
    // 例如：算法参数、输入节点、完整路径等
  } else {
    ElMessage.info(`查看 ${algorithm.name} 的详细分析（演示数据）`)
  }
}

// 保存配置
function saveConfig() {
  showConfigDialog.value = false
  calculateScores() // 重新计算得分
  ElMessage.success('配置已保存')
}

// 获取推荐度标签类型
function getRecommendationType(recommendation) {
  switch (recommendation) {
    case '强烈推荐': return 'success'
    case '推荐': return 'primary'
    case '一般': return 'warning'
    case '不推荐': return 'danger'
    default: return 'info'
  }
}

// 监听权重变化
watch(weights, () => {
  calculateScores()
}, { deep: true })

// 监听图表类型变化
watch(activeChart, () => {
  drawChart()
})

// 监听算法Store的历史结果变化（真实模式下）
watch(() => algorithmStore.historyResults, () => {
  if (!demoMode.value && algorithmStore.historyResults.length > 0) {
    // 只有在真实模式且有结果时才更新
    algorithms.value = algorithmResultsForComparison.value
    calculateScores()
  }
}, { deep: true })
</script>

<style scoped>
.compare-container {
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

.compare-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: auto;
  padding: 20px;
  background: #f5f5f5;
}

.overview-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.overview-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;
}

.overview-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.overview-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overview-content {
  flex: 1;
}

.overview-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.overview-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}

.overview-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.main-content {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  height: calc(100vh - 400px);
}

.chart-section {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
}

.chart-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.chart-legend {
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4b5563;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.detail-section {
  width: 600px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.detail-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
}

.metric-cell {
  display: flex;
  flex-direction: column;
}

.metric-value {
  font-weight: 600;
  color: #1f2937;
}

.metric-unit {
  font-size: 11px;
  color: #9ca3af;
}

.score-cell {
  display: flex;
  justify-content: center;
}

.algorithm-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.weight-config {
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  margin-top: auto;
}

.weight-config h4 {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.weight-sliders {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.weight-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.weight-item span:first-child {
  width: 120px;
  font-size: 13px;
  color: #4b5563;
}

.weight-item span:last-child {
  width: 30px;
  text-align: center;
  font-weight: 600;
  color: #1f2937;
}

.recommendation-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.recommendation-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;
}

.recommendation-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
}

.recommendation-content {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: #e5e7eb;
}

.recommendation-card {
  background: #fff;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.rec-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rec-icon .el-icon {
  color: #fff;
}

.rec-content {
  flex: 1;
}

.rec-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.rec-algorithm {
  font-size: 16px;
  font-weight: 700;
  color: #1890ff;
  margin-bottom: 4px;
}

.rec-reason {
  font-size: 12px;
  color: #6b7280;
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
    height: auto;
  }

  .detail-section {
    width: 100%;
  }

  .overview-section {
    grid-template-columns: repeat(2, 1fr);
  }

  .recommendation-content {
    grid-template-columns: repeat(2, 1fr);
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
  }

  .overview-section {
    grid-template-columns: 1fr;
  }

  .recommendation-content {
    grid-template-columns: 1fr;
  }
}

/* 工作流状态栏样式 */
.workflow-status {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  margin: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.workflow-step-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.workflow-message {
  font-size: 14px;
  opacity: 0.9;
}

.workflow-progress {
  flex: 1;
  max-width: 300px;
}

.progress-text {
  font-size: 12px;
  text-align: center;
  margin-top: 4px;
  opacity: 0.8;
}
</style>