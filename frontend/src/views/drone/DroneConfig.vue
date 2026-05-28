<template>
  <div class="drone-config-container">
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

        <!-- 配置操作 -->
        <el-button type="primary" :icon="Download" @click="loadConfig">加载配置</el-button>
        <el-button type="success" :icon="Upload" @click="saveConfig">保存配置</el-button>
        <el-button :icon="Refresh" @click="resetConfig">重置</el-button>
      </div>

      <div class="right-controls">
        <!-- 无人机选择 -->
        <el-select v-model="selectedDroneModel" placeholder="选择无人机" style="width: 200px; margin-right: 16px;">
          <el-option
            v-for="model in droneModels"
            :key="model.id"
            :label="model.name"
            :value="model.id"
          />
        </el-select>

        <el-button :icon="Setting" @click="showAdvancedConfig = !showAdvancedConfig">
          {{ showAdvancedConfig ? '隐藏高级设置' : '显示高级设置' }}
        </el-button>
      </div>
    </div>

    <div class="config-content">
      <!-- 左侧基础配置 -->
      <div class="basic-config">
        <div class="config-section">
          <div class="section-header">
            <h3><el-icon><Setting /></el-icon> 基本飞行参数</h3>
            <el-tag type="info" size="small">当前型号: {{ currentModel?.name || '未选择' }}</el-tag>
          </div>

          <div class="config-form">
            <el-form :model="droneConfig" label-width="120px">
              <!-- 飞行速度 -->
              <el-form-item label="飞行速度">
                <el-slider
                  v-model="droneConfig.speed"
                  :min="currentModel?.minSpeed || 1"
                  :max="currentModel?.maxSpeed || 15"
                  :step="0.5"
                  show-stops
                  :marks="speedMarks"
                />
                <span class="slider-value">{{ droneConfig.speed }} m/s</span>
                <div class="form-tip">建议速度: 5-8 m/s</div>
              </el-form-item>

              <!-- 飞行高度 -->
              <el-form-item label="飞行高度">
                <el-slider
                  v-model="droneConfig.altitude"
                  :min="currentModel?.minAltitude || 5"
                  :max="currentModel?.maxAltitude || 30"
                  :step="0.5"
                  show-stops
                />
                <span class="slider-value">{{ droneConfig.altitude }} 米</span>
                <div class="form-tip">作物高度: {{ droneConfig.cropHeight || 1.5 }} 米</div>
              </el-form-item>

              <!-- 喷洒参数 -->
              <el-form-item label="喷洒流量">
                <el-slider
                  v-model="droneConfig.sprayRate"
                  :min="0.5"
                  :max="5"
                  :step="0.1"
                  show-stops
                />
                <span class="slider-value">{{ droneConfig.sprayRate }} L/min</span>
                <div class="form-tip">覆盖宽度: {{ droneConfig.sprayWidth || 5 }} 米</div>
              </el-form-item>

              <!-- 行间距 -->
              <el-form-item label="行间距">
                <el-slider
                  v-model="droneConfig.rowSpacing"
                  :min="3"
                  :max="10"
                  :step="0.1"
                  show-stops
                />
                <span class="slider-value">{{ droneConfig.rowSpacing }} 米</span>
                <div class="form-tip">覆盖率: {{ calculateCoverage().toFixed(1) }}%</div>
              </el-form-item>

              <!-- 电池设置 -->
              <el-form-item label="电池阈值">
                <el-slider
                  v-model="droneConfig.batteryThreshold"
                  :min="10"
                  :max="30"
                  :step="1"
                  show-stops
                />
                <span class="slider-value">{{ droneConfig.batteryThreshold }}%</span>
                <div class="form-tip">低于此值将自动返航</div>
              </el-form-item>

              <!-- 安全距离 -->
              <el-form-item label="安全距离">
                <el-slider
                  v-model="droneConfig.safetyDistance"
                  :min="1"
                  :max="10"
                  :step="0.5"
                  show-stops
                />
                <span class="slider-value">{{ droneConfig.safetyDistance }} 米</span>
                <div class="form-tip">与障碍物的最小距离</div>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 高级配置 -->
        <div class="config-section" v-if="showAdvancedConfig">
          <div class="section-header">
            <h3><el-icon><MagicStick /></el-icon> 高级作业参数</h3>
            <el-tag type="warning" size="small">专家模式</el-tag>
          </div>

          <div class="config-form">
            <el-form :model="droneConfig.advanced" label-width="140px">
              <!-- 转弯半径 -->
              <el-form-item label="最小转弯半径">
                <el-input-number
                  v-model="droneConfig.advanced.minTurnRadius"
                  :min="2"
                  :max="10"
                  :step="0.5"
                />
                <span class="unit">米</span>
              </el-form-item>

              <!-- 加速度 -->
              <el-form-item label="最大加速度">
                <el-input-number
                  v-model="droneConfig.advanced.maxAcceleration"
                  :min="1"
                  :max="5"
                  :step="0.1"
                />
                <span class="unit">m/s²</span>
              </el-form-item>

              <!-- 减速度 -->
              <el-form-item label="最大减速度">
                <el-input-number
                  v-model="droneConfig.advanced.maxDeceleration"
                  :min="1"
                  :max="5"
                  :step="0.1"
                />
                <span class="unit">m/s²</span>
              </el-form-item>

              <!-- 喷雾参数 -->
              <el-form-item label="雾滴大小">
                <el-select v-model="droneConfig.advanced.dropletSize" style="width: 200px;">
                  <el-option label="细雾 (100-150μm)" value="fine" />
                  <el-option label="中雾 (150-250μm)" value="medium" />
                  <el-option label="粗雾 (250-350μm)" value="coarse" />
                </el-select>
              </el-form-item>

              <!-- 风速限制 -->
              <el-form-item label="最大风速">
                <el-input-number
                  v-model="droneConfig.advanced.maxWindSpeed"
                  :min="3"
                  :max="15"
                  :step="0.5"
                />
                <span class="unit">m/s</span>
              </el-form-item>

              <!-- 通信设置 -->
              <el-form-item label="通信频率">
                <el-select v-model="droneConfig.advanced.communicationFreq" style="width: 200px;">
                  <el-option label="2.4GHz" value="2.4" />
                  <el-option label="5.8GHz" value="5.8" />
                  <el-option label="900MHz" value="0.9" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>

      <!-- 右侧预览和信息 -->
      <div class="preview-panel">
        <!-- 无人机信息 -->
        <div class="drone-info-card">
          <div class="card-header">
            <h3>无人机信息</h3>
            <el-tag :type="getStatusType(currentModel?.status)" size="small">
              {{ currentModel?.status || '未知' }}
            </el-tag>
          </div>

          <div class="card-body">
            <div class="drone-image">
              <img :src="currentModel?.image || dronePlaceholder" alt="无人机" />
            </div>

            <div class="drone-specs">
              <div class="spec-item">
                <span class="spec-label">型号</span>
                <span class="spec-value">{{ currentModel?.name || '--' }}</span>
              </div>
              <div class="spec-item">
                <span class="spec-label">喷洒箱容量</span>
                <span class="spec-value">{{ currentModel?.tankCapacity || '--' }} L</span>
              </div>
              <div class="spec-item">
                <span class="spec-label">最大飞行时间</span>
                <span class="spec-value">{{ currentModel?.maxFlightTime || '--' }} 分钟</span>
              </div>
              <div class="spec-item">
                <span class="spec-label">最大载荷</span>
                <span class="spec-value">{{ currentModel?.payloadCapacity || '--' }} kg</span>
              </div>
              <div class="spec-item">
                <span class="spec-label">推荐作业面积</span>
                <span class="spec-value">{{ (currentModel?.recommendedArea || 0).toFixed(1) }} 亩</span>
              </div>
              <div class="spec-item">
                <span class="spec-label">价格</span>
                <span class="spec-value">¥{{ (currentModel?.price || 0).toLocaleString() }}</span>
              </div>
            </div>

            <div class="drone-description">
              <p>{{ currentModel?.description || '暂无描述' }}</p>
            </div>
          </div>
        </div>

        <!-- 配置预览 -->
        <div class="config-preview">
          <div class="card-header">
            <h3>配置预览</h3>
            <el-button :icon="DataLine" type="text" size="small" @click="calculateEfficiency">
              计算效率
            </el-button>
          </div>

          <div class="card-body">
            <div class="preview-metrics">
              <div class="metric-item">
                <div class="metric-icon" style="background: #10b981;">
                  <el-icon><Timer /></el-icon>
                </div>
                <div class="metric-info">
                  <div class="metric-label">作业效率</div>
                  <div class="metric-value">{{ efficiencyMetrics.efficiency.toFixed(1) }}</div>
                  <div class="metric-unit">亩/小时</div>
                </div>
              </div>

              <div class="metric-item">
                <div class="metric-icon" style="background: #3b82f6;">
                  <el-icon><Watermelon /></el-icon>
                </div>
                <div class="metric-info">
                  <div class="metric-label">农药用量</div>
                  <div class="metric-value">{{ efficiencyMetrics.pesticideUse.toFixed(1) }}</div>
                  <div class="metric-unit">L/亩</div>
                </div>
              </div>

              <div class="metric-item">
                <div class="metric-icon" style="background: #f59e0b;">
                  <el-icon><Lightning /></el-icon>
                </div>
                <div class="metric-info">
                  <div class="metric-label">能耗</div>
                  <div class="metric-value">{{ efficiencyMetrics.energyUse.toFixed(1) }}</div>
                  <div class="metric-unit">Wh/亩</div>
                </div>
              </div>

              <div class="metric-item">
                <div class="metric-icon" style="background: #ef4444;">
                  <el-icon><Coin /></el-icon>
                </div>
                <div class="metric-info">
                  <div class="metric-label">作业成本</div>
                  <div class="metric-value">{{ efficiencyMetrics.costPerAcre.toFixed(1) }}</div>
                  <div class="metric-unit">元/亩</div>
                </div>
              </div>
            </div>

            <!-- 效率图表 -->
            <div class="efficiency-chart">
              <h4>参数影响分析</h4>
              <div class="chart-placeholder">
                <el-empty description="图表区域" :image-size="60" />
              </div>
            </div>

            <!-- 配置建议 -->
            <div class="config-suggestions">
              <h4>配置建议</h4>
              <div class="suggestions-list">
                <div
                  v-for="(suggestion, index) in suggestions"
                  :key="index"
                  class="suggestion-item"
                  :class="suggestion.level"
                >
                  <el-icon>
                    <component :is="getSuggestionIcon(suggestion.level)" />
                  </el-icon>
                  <span>{{ suggestion.message }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 配置模板 -->
        <div class="config-templates">
          <div class="card-header">
            <h3>配置模板</h3>
            <el-button :icon="Plus" type="text" size="small" @click="saveAsTemplate">
              保存为模板
            </el-button>
          </div>

          <div class="card-body">
            <div class="templates-list">
              <el-table :data="templates" size="small" style="width: 100%" height="200">
                <el-table-column prop="name" label="模板名称" width="120" />
                <el-table-column prop="droneModel" label="无人机型号" width="100" />
                <el-table-column prop="description" label="描述" />
                <el-table-column label="操作" width="80">
                  <template #default="{ row }">
                    <el-button type="text" size="small" @click="loadTemplate(row)">加载</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Setting,
  Download,
  Upload,
  Refresh,
  MagicStick,
  Timer,
  Watermelon,
  Lightning,
  Coin,
  DataLine,
  Plus,
  Check,
  Warning,
  InfoFilled
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode, droneService } from '@/services/dataService'

// 模式控制
const demoMode = ref(getCurrentMode())

// 无人机型号选择
const selectedDroneModel = ref(1)
const droneModels = ref([])

// 显示高级配置
const showAdvancedConfig = ref(false)

// 无人机配置
const droneConfig = reactive({
  speed: 8.0,
  altitude: 15.0,
  sprayRate: 2.5,
  rowSpacing: 5.0,
  batteryThreshold: 20,
  safetyDistance: 3.0,
  cropHeight: 1.5,
  sprayWidth: 5.0,
  advanced: {
    minTurnRadius: 5.0,
    maxAcceleration: 2.0,
    maxDeceleration: 2.5,
    dropletSize: 'medium',
    maxWindSpeed: 8.0,
    communicationFreq: '2.4'
  }
})

// 效率指标
const efficiencyMetrics = reactive({
  efficiency: 12.5,
  pesticideUse: 1.2,
  energyUse: 45.8,
  costPerAcre: 8.5
})

// 配置模板
const templates = ref([])

// 建议列表
const suggestions = ref([
  { level: 'warning', message: '飞行速度偏高，建议降低至7 m/s以提高喷雾均匀性' },
  { level: 'success', message: '行间距设置合理，覆盖率可达95%以上' },
  { level: 'info', message: '考虑增加安全距离至4米以应对突发障碍' }
])

// 占位图
const dronePlaceholder = 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400&h=300&fit=crop'

// 计算属性
const currentModel = computed(() => {
  return droneModels.value.find(model => model.id === selectedDroneModel.value)
})

const speedMarks = computed(() => {
  const marks = {}
  for (let i = 1; i <= 15; i += 2) {
    marks[i] = `${i}m/s`
  }
  return marks
})

// 初始化
onMounted(() => {
  loadDroneModels()
  loadTemplates()
})

// 监听无人机型号变化
watch(selectedDroneModel, (newModelId) => {
  const model = droneModels.value.find(m => m.id === newModelId)
  if (model) {
    // 根据无人机型号调整配置范围
    droneConfig.speed = Math.min(droneConfig.speed, model.maxSpeed)
    droneConfig.altitude = Math.min(droneConfig.altitude, model.maxAltitude)
    calculateEfficiency()
  }
})

// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
}

// 加载无人机型号
async function loadDroneModels() {
  try {
    const response = await droneService.getModels()
    droneModels.value = response.data

    // 如果没有选中型号且列表不为空，选择第一个
    if (droneModels.value.length > 0 && !selectedDroneModel.value) {
      selectedDroneModel.value = droneModels.value[0].id
    }
  } catch (error) {
    console.error('加载无人机型号失败:', error)

    // 演示模式：使用模拟数据
    if (demoMode.value) {
      droneModels.value = [
        {
          id: 1,
          name: 'DJI Agras T40',
          description: '大疆农业无人机，40L喷洒箱，智能作业系统',
          minSpeed: 1,
          maxSpeed: 10,
          minAltitude: 5,
          maxAltitude: 30,
          tankCapacity: 40,
          maxFlightTime: 30,
          payloadCapacity: 40,
          recommendedArea: 50,
          price: 60000,
          status: '推荐',
          image: 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400&h=300&fit=crop'
        },
        {
          id: 2,
          name: 'XAIRCRAFT P30',
          description: '极飞农业无人机，30L喷洒箱，高精度导航',
          minSpeed: 1,
          maxSpeed: 12,
          minAltitude: 5,
          maxAltitude: 25,
          tankCapacity: 30,
          maxFlightTime: 25,
          payloadCapacity: 30,
          recommendedArea: 40,
          price: 45000,
          status: '可用',
          image: 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400&h=300&fit=crop'
        },
        {
          id: 3,
          name: 'Hanhe DJI T20',
          description: '汉和DJI合作款，20L喷洒箱，经济实用',
          minSpeed: 1,
          maxSpeed: 9,
          minAltitude: 5,
          maxAltitude: 20,
          tankCapacity: 20,
          maxFlightTime: 20,
          payloadCapacity: 20,
          recommendedArea: 30,
          price: 32000,
          status: '经济',
          image: 'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400&h=300&fit=crop'
        }
      ]
      selectedDroneModel.value = 1
    }
  }
}

// 计算覆盖率
function calculateCoverage() {
  const coverage = (droneConfig.sprayWidth / droneConfig.rowSpacing) * 100
  return Math.min(coverage, 100)
}

// 计算效率指标
function calculateEfficiency() {
  // 简化的效率计算模型
  const speed = droneConfig.speed
  const sprayWidth = droneConfig.sprayWidth
  const sprayRate = droneConfig.sprayRate
  const rowSpacing = droneConfig.rowSpacing

  // 作业效率 (亩/小时)
  efficiencyMetrics.efficiency = (speed * sprayWidth * 0.0015 * 60) / rowSpacing

  // 农药用量 (L/亩)
  efficiencyMetrics.pesticideUse = (sprayRate * 60) / (efficiencyMetrics.efficiency * 1000)

  // 能耗 (Wh/亩) - 简化的能耗模型
  efficiencyMetrics.energyUse = (speed * 15 + droneConfig.altitude * 8) / efficiencyMetrics.efficiency

  // 作业成本 (元/亩) - 包含农药、电费、折旧
  const pesticideCost = efficiencyMetrics.pesticideUse * 50 // 假设农药50元/L
  const energyCost = (efficiencyMetrics.energyUse / 1000) * 0.8 // 电费0.8元/度
  const depreciation = currentModel.value?.price ? (currentModel.value.price / 10000) : 1 // 折旧成本

  efficiencyMetrics.costPerAcre = pesticideCost + energyCost + depreciation

  // 更新建议
  updateSuggestions()

  ElMessage.success('效率计算完成')
}

// 更新建议
function updateSuggestions() {
  suggestions.value = []

  if (droneConfig.speed > 8) {
    suggestions.value.push({
      level: 'warning',
      message: '飞行速度偏高，建议降低至7-8 m/s以提高喷雾均匀性'
    })
  }

  if (droneConfig.altitude > 20) {
    suggestions.value.push({
      level: 'warning',
      message: '飞行高度偏高，建议降低至15-18米以提高喷雾精度'
    })
  }

  if (droneConfig.rowSpacing > 6) {
    suggestions.value.push({
      level: 'info',
      message: '行间距较大，覆盖率可能不足，建议减小至5-6米'
    })
  }

  if (droneConfig.batteryThreshold < 15) {
    suggestions.value.push({
      level: 'warning',
      message: '电池阈值过低，建议提高至20-25%以确保安全返航'
    })
  }

  // 添加积极反馈
  const coverage = calculateCoverage()
  if (coverage > 95) {
    suggestions.value.push({
      level: 'success',
      message: '覆盖率达到95%以上，配置优秀'
    })
  }

  if (efficiencyMetrics.efficiency > 10) {
    suggestions.value.push({
      level: 'success',
      message: '作业效率较高，配置合理'
    })
  }

  if (suggestions.value.length === 0) {
    suggestions.value.push({
      level: 'info',
      message: '配置合理，无显著优化建议'
    })
  }
}

// 加载配置
async function loadConfig() {
  try {
    const response = await droneService.getConfig()
    const config = response.data

    // 更新配置
    Object.assign(droneConfig, config)
    ElMessage.success('配置加载成功')
  } catch (error) {
    console.error('加载配置失败:', error)
    ElMessage.error('加载配置失败')
  }
}

// 保存配置
async function saveConfig() {
  try {
    const configToSave = {
      ...droneConfig,
      droneModelId: selectedDroneModel.value
    }

    const response = await droneService.saveConfig(configToSave)
    ElMessage.success(response.message || '配置保存成功')
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存配置失败')
  }
}

// 重置配置
function resetConfig() {
  // 重置为默认值
  Object.assign(droneConfig, {
    speed: 8.0,
    altitude: 15.0,
    sprayRate: 2.5,
    rowSpacing: 5.0,
    batteryThreshold: 20,
    safetyDistance: 3.0,
    cropHeight: 1.5,
    sprayWidth: 5.0,
    advanced: {
      minTurnRadius: 5.0,
      maxAcceleration: 2.0,
      maxDeceleration: 2.5,
      dropletSize: 'medium',
      maxWindSpeed: 8.0,
      communicationFreq: '2.4'
    }
  })
  ElMessage.info('配置已重置为默认值')
  calculateEfficiency()
}

// 获取状态标签类型
function getStatusType(status) {
  const typeMap = {
    '推荐': 'success',
    '可用': 'primary',
    '经济': 'warning',
    '停用': 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取建议图标
function getSuggestionIcon(level) {
  return {
    success: Check,
    warning: Warning,
    info: InfoFilled
  }[level] || InfoFilled
}

// 加载配置模板
function loadTemplates() {
  // 模拟模板数据
  templates.value = [
    { id: 1, name: '高效作业模板', droneModel: 'DJI Agras T40', description: '适用于大面积高效作业' },
    { id: 2, name: '精准喷洒模板', droneModel: 'XAIRCRAFT P30', description: '适用于需要精准控制的场景' },
    { id: 3, name: '经济型模板', droneModel: 'Hanhe DJI T20', description: '成本优先的作业配置' },
    { id: 4, name: '果树喷洒模板', droneModel: 'DJI Agras T40', description: '适用于果园等高杆作物' }
  ]
}

// 加载模板
function loadTemplate(template) {
  ElMessage.info(`正在加载模板: ${template.name}`)
  // 这里可以加载具体的模板配置
}

// 保存为模板
function saveAsTemplate() {
  const templateName = prompt('请输入模板名称:', `自定义模板_${new Date().toLocaleDateString()}`)
  if (templateName) {
    templates.value.unshift({
      id: Date.now(),
      name: templateName,
      droneModel: currentModel.value?.name || '未知型号',
      description: '用户自定义配置模板'
    })
    ElMessage.success('模板保存成功')
  }
}
</script>

<style scoped>
.drone-config-container {
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

.right-controls {
  display: flex;
  gap: 8px;
}

.config-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #f5f5f5;
}

.basic-config {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.preview-panel {
  width: 400px;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.config-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  margin-bottom: 16px;
  overflow: hidden;
}

.section-header {
  padding: 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.config-form {
  padding: 16px;
}

.slider-value {
  display: inline-block;
  min-width: 60px;
  margin-left: 16px;
  font-weight: 600;
  color: #1f2937;
}

.form-tip {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.unit {
  margin-left: 8px;
  color: #6b7280;
  font-size: 14px;
}

.drone-info-card,
.config-preview,
.config-templates {
  margin: 16px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.card-header {
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.card-body {
  padding: 16px;
}

.drone-image {
  text-align: center;
  margin-bottom: 16px;
}

.drone-image img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 6px;
}

.drone-specs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.spec-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.spec-item:last-child {
  border-bottom: none;
}

.spec-label {
  font-size: 13px;
  color: #6b7280;
}

.spec-value {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.drone-description {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  padding: 8px;
  background: #f9fafb;
  border-radius: 4px;
}

.preview-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.metric-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.metric-info {
  flex: 1;
}

.metric-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 2px;
}

.metric-value {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}

.metric-unit {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.efficiency-chart {
  margin-bottom: 16px;
}

.efficiency-chart h4 {
  margin: 0 0 8px 0;
  font-size: 13px;
  font-weight: 600;
  color: #4a5568;
}

.chart-placeholder {
  height: 120px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px dashed #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
}

.config-suggestions h4 {
  margin: 0 0 8px 0;
  font-size: 13px;
  font-weight: 600;
  color: #4a5568;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
}

.suggestion-item.success {
  background: #ecfdf5;
  color: #065f46;
}

.suggestion-item.warning {
  background: #fffbeb;
  color: #92400e;
}

.suggestion-item.info {
  background: #eff6ff;
  color: #1e40af;
}

.templates-list {
  margin-top: 8px;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .config-content {
    flex-direction: column;
  }

  .preview-panel {
    width: 100%;
    height: 40%;
  }

  .preview-metrics {
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

  .preview-metrics {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>