import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { droneService } from '@/services/dataService'
import { ElMessage } from 'element-plus'

export const useDroneStore = defineStore('drone', () => {
  // 状态
  const droneModels = ref([])
  const currentConfig = ref({
    modelId: 1,
    modelName: 'DJI Agras T40',
    flightSpeed: 5.0, // m/s
    flightHeight: 3.0, // meters
    batteryCapacity: 10000, // mAh
    payloadCapacity: 40, // kg
    sprayRate: 2.0, // L/min
    turnRadius: 2.0, // meters
    maxFlightTime: 30 // minutes
  })

  const customConfigs = ref([])
  const isLoading = ref(false)

  // 计算属性
  const currentModel = computed(() => {
    return droneModels.value.find(model => model.id === currentConfig.value.modelId) || null
  })

  const flightEfficiency = computed(() => {
    // 简单的效率计算：速度 * 有效载荷 / 能耗
    const speed = currentConfig.value.flightSpeed
    const payload = currentConfig.value.payloadCapacity
    const battery = currentConfig.value.batteryCapacity

    if (battery > 0) {
      return (speed * payload) / (battery / 1000)
    }
    return 0
  })

  const estimatedCoverage = computed(() => {
    // 估计覆盖率：基于飞行时间和喷洒速率
    const flightTime = currentConfig.value.maxFlightTime
    const sprayRate = currentConfig.value.sprayRate
    const speed = currentConfig.value.flightSpeed

    // 简化计算：飞行距离 * 喷洒宽度
    const flightDistance = speed * flightTime * 60 // 转换为米
    const sprayWidth = 5 // 假设喷洒宽度5米

    return flightDistance * sprayWidth
  })

  // 动作
  async function loadDroneModels() {
    isLoading.value = true
    try {
      const response = await droneService.getDroneModels()
      droneModels.value = response.data || []

      // 如果有模型但当前配置没有模型ID，设置第一个模型
      if (droneModels.value.length > 0 && !currentConfig.value.modelId) {
        const firstModel = droneModels.value[0]
        currentConfig.value.modelId = firstModel.id
        currentConfig.value.modelName = firstModel.name
      }

      return droneModels.value
    } catch (error) {
      ElMessage.error('加载无人机型号失败')
      console.error(error)
      return []
    } finally {
      isLoading.value = false
    }
  }

  async function loadCurrentConfig() {
    isLoading.value = true
    try {
      const response = await droneService.getDroneConfig()
      if (response.data) {
        currentConfig.value = { ...currentConfig.value, ...response.data }
      }
      return currentConfig.value
    } catch (error) {
      ElMessage.error('加载无人机配置失败')
      console.error(error)
      return currentConfig.value
    } finally {
      isLoading.value = false
    }
  }

  async function saveCurrentConfig() {
    isLoading.value = true
    try {
      const response = await droneService.saveDroneConfig(currentConfig.value)
      ElMessage.success('无人机配置保存成功')
      return response.data || true
    } catch (error) {
      ElMessage.error('保存无人机配置失败')
      console.error(error)
      return false
    } finally {
      isLoading.value = false
    }
  }

  function updateConfig(updates) {
    currentConfig.value = { ...currentConfig.value, ...updates }

    // 如果更新了modelId，同时更新modelName
    if (updates.modelId && droneModels.value.length > 0) {
      const model = droneModels.value.find(m => m.id === updates.modelId)
      if (model) {
        currentConfig.value.modelName = model.name
      }
    }
  }

  function saveAsCustomConfig(name) {
    const configCopy = { ...currentConfig.value }
    const customConfig = {
      id: Date.now(),
      name: name || `配置_${new Date().toLocaleString()}`,
      config: configCopy,
      createdAt: new Date().toISOString()
    }

    customConfigs.value.unshift(customConfig)

    // 只保留最近10个自定义配置
    if (customConfigs.value.length > 10) {
      customConfigs.value = customConfigs.value.slice(0, 10)
    }

    ElMessage.success('配置已保存为模板')
    return customConfig
  }

  function loadCustomConfig(configId) {
    const config = customConfigs.value.find(c => c.id === configId)
    if (config) {
      currentConfig.value = { ...config.config }
      ElMessage.success('配置已加载')
      return true
    }
    return false
  }

  function deleteCustomConfig(configId) {
    const index = customConfigs.value.findIndex(c => c.id === configId)
    if (index !== -1) {
      customConfigs.value.splice(index, 1)
      ElMessage.success('配置模板已删除')
      return true
    }
    return false
  }

  function resetToDefaults() {
    currentConfig.value = {
      modelId: 1,
      modelName: 'DJI Agras T40',
      flightSpeed: 5.0,
      flightHeight: 3.0,
      batteryCapacity: 10000,
      payloadCapacity: 40,
      sprayRate: 2.0,
      turnRadius: 2.0,
      maxFlightTime: 30
    }
    ElMessage.success('已重置为默认配置')
  }

  // 导出状态和动作
  return {
    // 状态
    droneModels,
    currentConfig,
    customConfigs,
    isLoading,

    // 计算属性
    currentModel,
    flightEfficiency,
    estimatedCoverage,

    // 动作
    loadDroneModels,
    loadCurrentConfig,
    saveCurrentConfig,
    updateConfig,
    saveAsCustomConfig,
    loadCustomConfig,
    deleteCustomConfig,
    resetToDefaults
  }
})