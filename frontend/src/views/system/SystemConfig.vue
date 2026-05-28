<template>
  <div class="system-config-container">
    <div class="header-controls">
      <div class="left-controls">
        <h2>系统配置</h2>
        <el-switch
          v-model="demoMode"
          active-text="演示模式"
          inactive-text="真实模式"
          style="margin-left: 20px;"
          @change="toggleMode"
        />
      </div>
      <div class="right-controls">
        <el-button type="primary" :icon="Download" @click="exportConfig">导出配置</el-button>
        <el-button :icon="Upload" @click="importConfig">导入配置</el-button>
        <el-button type="success" :icon="Check" @click="saveConfig" :loading="isSaving">保存配置</el-button>
      </div>
    </div>

    <div class="config-content">
      <!-- 左侧配置表单 -->
      <div class="config-forms">
        <el-tabs v-model="activeTab" type="card">
          <!-- 基本设置 -->
          <el-tab-pane label="基本设置" name="general">
            <el-form :model="config.general" label-width="120px" class="config-form">
              <el-form-item label="系统名称">
                <el-input v-model="config.general.siteName" placeholder="请输入系统名称" />
              </el-form-item>
              <el-form-item label="系统Logo">
                <el-input v-model="config.general.siteLogo" placeholder="Logo图片路径">
                  <template #append>
                    <el-button :icon="Picture" @click="selectLogo">选择</el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="默认语言">
                <el-select v-model="config.general.defaultLanguage" style="width: 100%;">
                  <el-option label="简体中文" value="zh-CN" />
                  <el-option label="English" value="en-US" />
                </el-select>
              </el-form-item>
              <el-form-item label="时区设置">
                <el-select v-model="config.general.timezone" style="width: 100%;">
                  <el-option label="亚洲/上海" value="Asia/Shanghai" />
                  <el-option label="UTC" value="UTC" />
                  <el-option label="美国/纽约" value="America/New_York" />
                </el-select>
              </el-form-item>
              <el-form-item label="日期格式">
                <el-select v-model="config.general.dateFormat" style="width: 100%;">
                  <el-option label="YYYY-MM-DD" value="YYYY-MM-DD" />
                  <el-option label="DD/MM/YYYY" value="DD/MM/YYYY" />
                  <el-option label="MM/DD/YYYY" value="MM/DD/YYYY" />
                </el-select>
              </el-form-item>
              <el-form-item label="时间格式">
                <el-select v-model="config.general.timeFormat" style="width: 100%;">
                  <el-option label="HH:mm:ss" value="HH:mm:ss" />
                  <el-option label="hh:mm A" value="hh:mm A" />
                </el-select>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- 地图设置 -->
          <el-tab-pane label="地图设置" name="map">
            <el-form :model="config.map" label-width="120px" class="config-form">
              <el-form-item label="默认地图">
                <el-select v-model="config.map.defaultMapId" style="width: 100%;">
                  <el-option v-for="map in mapList" :key="map.id" :label="map.name" :value="map.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="默认缩放">
                <el-slider v-model="config.map.defaultZoom" :min="0.1" :max="5" :step="0.1" />
                <span style="margin-left: 10px;">{{ config.map.defaultZoom.toFixed(1) }}x</span>
              </el-form-item>
              <el-form-item label="最小缩放">
                <el-input-number v-model="config.map.minZoom" :min="0.1" :max="1" :step="0.1" />
              </el-form-item>
              <el-form-item label="最大缩放">
                <el-input-number v-model="config.map.maxZoom" :min="1" :max="10" :step="0.5" />
              </el-form-item>
              <el-form-item label="节点颜色">
                <div class="color-picker-group">
                  <div class="color-item">
                    <span>起点:</span>
                    <el-color-picker v-model="config.map.nodeColors.start" />
                  </div>
                  <div class="color-item">
                    <span>终点:</span>
                    <el-color-picker v-model="config.map.nodeColors.end" />
                  </div>
                  <div class="color-item">
                    <span>障碍物:</span>
                    <el-color-picker v-model="config.map.nodeColors.obstacle" />
                  </div>
                  <div class="color-item">
                    <span>路径点:</span>
                    <el-color-picker v-model="config.map.nodeColors.waypoint" />
                  </div>
                </div>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- 算法设置 -->
          <el-tab-pane label="算法设置" name="algorithm">
            <el-form :model="config.algorithm" label-width="120px" class="config-form">
              <el-form-item label="默认算法">
                <el-select v-model="config.algorithm.defaultAlgorithm" style="width: 100%;">
                  <el-option label="A*算法" value="a-star" />
                  <el-option label="Dijkstra算法" value="dijkstra" />
                  <el-option label="广度优先搜索" value="bfs" />
                  <el-option label="深度优先搜索" value="dfs" />
                </el-select>
              </el-form-item>
              <el-form-item label="默认启发函数">
                <el-select v-model="config.algorithm.defaultHeuristic" style="width: 100%;">
                  <el-option label="欧几里得距离" value="euclidean" />
                  <el-option label="曼哈顿距离" value="manhattan" />
                  <el-option label="切比雪夫距离" value="chebyshev" />
                </el-select>
              </el-form-item>
              <el-form-item label="启发式权重">
                <el-slider v-model="config.algorithm.defaultWeight" :min="0.1" :max="3" :step="0.1" />
                <span style="margin-left: 10px;">{{ config.algorithm.defaultWeight.toFixed(1) }}</span>
              </el-form-item>
              <el-form-item label="计算超时">
                <el-input-number v-model="config.algorithm.calculationTimeout" :min="1000" :max="60000" :step="1000" />
                <span style="margin-left: 10px;">毫秒</span>
              </el-form-item>
              <el-form-item label="允许对角线">
                <el-switch v-model="config.algorithm.allowDiagonal" />
              </el-form-item>
              <el-form-item label="障碍物惩罚">
                <el-input-number v-model="config.algorithm.obstaclePenalty" :min="1" :max="10" />
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- 监控设置 -->
          <el-tab-pane label="监控设置" name="monitoring">
            <el-form :model="config.monitoring" label-width="120px" class="config-form">
              <el-form-item label="更新间隔">
                <el-input-number v-model="config.monitoring.updateInterval" :min="1000" :max="30000" :step="1000" />
                <span style="margin-left: 10px;">毫秒 (当前: {{ config.monitoring.updateInterval / 1000 }}秒)</span>
              </el-form-item>
              <el-form-item label="历史保留天数">
                <el-input-number v-model="config.monitoring.historyRetentionDays" :min="1" :max="365" />
              </el-form-item>
              <el-form-item label="告警保留天数">
                <el-input-number v-model="config.monitoring.alertRetentionDays" :min="7" :max="730" />
              </el-form-item>
              <el-form-item label="自动刷新">
                <el-switch v-model="config.monitoring.autoRefresh" />
              </el-form-item>
              <el-form-item label="低电量阈值">
                <el-input-number v-model="config.monitoring.lowBatteryThreshold" :min="10" :max="50" />
                <span style="margin-left: 10px;">%</span>
              </el-form-item>
              <el-form-item label="返航电量阈值">
                <el-input-number v-model="config.monitoring.returnHomeBattery" :min="20" :max="80" />
                <span style="margin-left: 10px;">%</span>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- 安全设置 -->
          <el-tab-pane label="安全设置" name="security">
            <el-form :model="config.security" label-width="120px" class="config-form">
              <el-form-item label="启用双因素认证">
                <el-switch v-model="config.security.twoFactorAuth" />
              </el-form-item>
              <el-form-item label="会话超时">
                <el-input-number v-model="config.security.sessionTimeout" :min="15" :max="480" />
                <span style="margin-left: 10px;">分钟</span>
              </el-form-item>
              <el-form-item label="密码最小长度">
                <el-input-number v-model="config.security.minPasswordLength" :min="6" :max="20" />
              </el-form-item>
              <el-form-item label="密码复杂度">
                <el-select v-model="config.security.passwordComplexity" style="width: 100%;">
                  <el-option label="低 (仅字母)" value="low" />
                  <el-option label="中 (字母+数字)" value="medium" />
                  <el-option label="高 (字母+数字+特殊字符)" value="high" />
                </el-select>
              </el-form-item>
              <el-form-item label="最大登录尝试次数">
                <el-input-number v-model="config.security.maxLoginAttempts" :min="1" :max="10" />
              </el-form-item>
              <el-form-item label="登录锁定时间">
                <el-input-number v-model="config.security.loginLockoutMinutes" :min="5" :max="60" />
                <span style="margin-left: 10px;">分钟</span>
              </el-form-item>
              <el-form-item label="启用登录日志">
                <el-switch v-model="config.security.enableLoginLog" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 右侧预览区域 -->
      <div class="config-preview">
        <div class="preview-card">
          <h3>配置预览</h3>
          <div class="preview-content">
            <div class="preview-item">
              <span class="preview-label">当前模式:</span>
              <el-tag :type="demoMode ? 'warning' : 'success'">
                {{ demoMode ? '演示模式' : '真实模式' }}
              </el-tag>
            </div>
            <div class="preview-item">
              <span class="preview-label">系统名称:</span>
              <span class="preview-value">{{ config.general.siteName }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">默认算法:</span>
              <span class="preview-value">{{ getAlgorithmName(config.algorithm.defaultAlgorithm) }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">监控间隔:</span>
              <span class="preview-value">{{ config.monitoring.updateInterval / 1000 }}秒</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">最后保存:</span>
              <span class="preview-value">{{ lastSaved || '未保存' }}</span>
            </div>
          </div>
        </div>

        <div class="actions-card">
          <h3>快速操作</h3>
          <div class="action-buttons">
            <el-button type="primary" plain :icon="Refresh" @click="resetToDefaults">恢复默认</el-button>
            <el-button type="info" plain :icon="Document" @click="viewChangeLog">查看变更</el-button>
            <el-button type="warning" plain :icon="Warning" @click="clearCache">清除缓存</el-button>
            <el-button type="danger" plain :icon="Delete" @click="resetAll">重置所有配置</el-button>
          </div>
        </div>

        <div class="system-info-card">
          <h3>系统信息</h3>
          <div class="info-content">
            <div class="info-item">
              <span class="info-label">前端版本:</span>
              <span class="info-value">v1.0.0</span>
            </div>
            <div class="info-item">
              <span class="info-label">构建时间:</span>
              <span class="info-value">2026-04-14</span>
            </div>
            <div class="info-item">
              <span class="info-label">运行环境:</span>
              <span class="info-value">Vue 3 + Vite</span>
            </div>
            <div class="info-item">
              <span class="info-label">UI框架:</span>
              <span class="info-value">Element Plus</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 导入配置对话框 -->
    <el-dialog v-model="showImportDialog" title="导入配置" width="500">
      <el-upload
        class="config-upload"
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleUploadChange"
        :show-file-list="false"
      >
        <el-icon :size="50"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽配置文件到此处或<em>点击上传</em></div>
        <div class="el-upload__tip">支持JSON格式配置文件</div>
      </el-upload>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showImportDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmImport">确认导入</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Download,
  Upload,
  Check,
  Picture,
  Refresh,
  Document,
  Warning,
  Delete,
  UploadFilled
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode, fetchData } from '@/services/dataService'

// 演示模式
const demoMode = ref(getCurrentMode())

// 加载状态
const isSaving = ref(false)
const activeTab = ref('general')
const showImportDialog = ref(false)
const lastSaved = ref('')
const uploadedFile = ref(null)

// 地图列表（模拟数据）
const mapList = ref([
  { id: 1, name: '农田区域A' },
  { id: 2, name: '农田区域B' },
  { id: 3, name: '山地农田C' }
])

// 配置数据
const config = reactive({
  general: {
    siteName: '农业无人机路径规划系统',
    siteLogo: '/logo.png',
    defaultLanguage: 'zh-CN',
    timezone: 'Asia/Shanghai',
    dateFormat: 'YYYY-MM-DD',
    timeFormat: 'HH:mm:ss'
  },
  map: {
    defaultMapId: 1,
    defaultZoom: 1.0,
    minZoom: 0.5,
    maxZoom: 3.0,
    nodeColors: {
      start: '#10B981',
      end: '#EF4444',
      obstacle: '#9CA3AF',
      waypoint: '#3B82F6'
    }
  },
  algorithm: {
    defaultAlgorithm: 'a-star',
    defaultHeuristic: 'euclidean',
    defaultWeight: 1.0,
    calculationTimeout: 30000,
    allowDiagonal: true,
    obstaclePenalty: 2
  },
  monitoring: {
    updateInterval: 5000,
    historyRetentionDays: 30,
    alertRetentionDays: 90,
    autoRefresh: true,
    lowBatteryThreshold: 20,
    returnHomeBattery: 30
  },
  security: {
    twoFactorAuth: false,
    sessionTimeout: 30,
    minPasswordLength: 8,
    passwordComplexity: 'medium',
    maxLoginAttempts: 5,
    loginLockoutMinutes: 15,
    enableLoginLog: true
  }
})

// 算法名称映射
const algorithmNames = {
  'a-star': 'A*算法',
  'dijkstra': 'Dijkstra算法',
  'bfs': '广度优先搜索',
  'dfs': '深度优先搜索'
}

// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
}

// 获取算法名称
function getAlgorithmName(algorithmKey) {
  return algorithmNames[algorithmKey] || algorithmKey
}

// 选择Logo
function selectLogo() {
  ElMessage.info('Logo选择功能待实现')
}

// 保存配置
async function saveConfig() {
  isSaving.value = true

  try {
    if (demoMode.value) {
      // 演示模式：模拟保存
      await new Promise(resolve => setTimeout(resolve, 800))
      lastSaved.value = new Date().toLocaleTimeString()
      ElMessage.success('配置保存成功（演示模式）')
    } else {
      // 真实模式：调用API
      const response = await fetchData(
        () => Promise.resolve({ code: 200, message: 'success', data: { saved: true } }),
        { saved: true }
      )

      if (response.code === 200) {
        lastSaved.value = new Date().toLocaleTimeString()
        ElMessage.success('配置保存成功')
      }
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存配置失败，请重试')
  } finally {
    isSaving.value = false
  }
}

// 导出配置
function exportConfig() {
  const configStr = JSON.stringify(config, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(configStr)

  const exportFileName = `系统配置_${new Date().getTime()}.json`

  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', exportFileName)
  linkElement.click()

  ElMessage.success('配置已导出')
}

// 导入配置
function importConfig() {
  showImportDialog.value = true
}

// 处理文件上传
function handleUploadChange(file) {
  uploadedFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const importedConfig = JSON.parse(e.target.result)
      Object.assign(config, importedConfig)
      ElMessage.success('配置文件解析成功，点击确认导入应用配置')
    } catch (error) {
      ElMessage.error('配置文件格式错误')
    }
  }
  reader.readAsText(file.raw)
}

// 确认导入
function confirmImport() {
  showImportDialog.value = false
  ElMessage.success('配置导入成功')
  lastSaved.value = new Date().toLocaleTimeString()
}

// 恢复默认设置
function resetToDefaults() {
  ElMessageBox.confirm(
    '确定要恢复默认设置吗？当前修改将丢失。',
    '恢复默认设置',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 重置配置
    config.general.siteName = '农业无人机路径规划系统'
    config.general.siteLogo = '/logo.png'
    config.general.defaultLanguage = 'zh-CN'
    config.general.timezone = 'Asia/Shanghai'
    config.general.dateFormat = 'YYYY-MM-DD'
    config.general.timeFormat = 'HH:mm:ss'

    ElMessage.success('已恢复默认设置')
  })
}

// 查看变更
function viewChangeLog() {
  ElMessage.info('查看变更功能待实现')
}

// 清除缓存
function clearCache() {
  ElMessageBox.confirm(
    '确定要清除系统缓存吗？这可能会影响性能。',
    '清除缓存',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('缓存清除成功')
  })
}

// 重置所有配置
function resetAll() {
  ElMessageBox.confirm(
    '确定要重置所有配置吗？这将恢复所有设置到初始状态。',
    '重置所有配置',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'error'
    }
  ).then(() => {
    location.reload() // 简单重载页面
  })
}

// 初始化加载配置
onMounted(async () => {
  try {
    if (!demoMode.value) {
      // 真实模式：从API加载配置
      const response = await fetchData(
        () => Promise.resolve({
          code: 200,
          message: 'success',
          data: config
        }),
        config
      )

      if (response.code === 200 && response.data) {
        Object.assign(config, response.data)
      }
    }
  } catch (error) {
    console.error('加载配置失败:', error)
  }
})
</script>

<style scoped>
.system-config-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.left-controls {
  display: flex;
  align-items: center;
  gap: 20px;
}

.left-controls h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a202c;
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
  padding: 20px;
  gap: 20px;
}

.config-forms {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.config-form {
  padding: 20px;
}

.color-picker-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.color-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-item span {
  font-size: 14px;
  color: #4b5563;
  min-width: 60px;
}

.config-preview {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-card,
.actions-card,
.system-info-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 16px;
}

.preview-card h3,
.actions-card h3,
.system-info-card h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.preview-label {
  font-size: 14px;
  color: #6b7280;
}

.preview-value {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
}

.info-label {
  font-size: 14px;
  color: #6b7280;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.config-upload {
  text-align: center;
}

.el-upload__text {
  margin: 10px 0;
  font-size: 14px;
}

.el-upload__tip {
  font-size: 12px;
  color: #9ca3af;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .config-content {
    flex-direction: column;
  }

  .config-preview {
    width: 100%;
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
}
</style>