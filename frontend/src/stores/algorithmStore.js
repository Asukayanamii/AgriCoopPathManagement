import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { algorithmService } from '@/services/dataService'
import { useMapStore } from './mapStore'
import { ElMessage } from 'element-plus'

export const useAlgorithmStore = defineStore('algorithm', () => {
  // 状态
  const algorithmType = ref('a-star') // 'a-star', 'dijkstra'
  const algorithmParams = ref({
    heuristic: 'euclidean', // 'euclidean', 'manhattan', 'diagonal'
    allowDiagonal: true,
    weight: 1.0,
    coverageThreshold: 0.8,
    efficiencyWeight: 0.7
  })

  const currentResult = ref(null)
  const historyResults = ref([])
  const isLoading = ref(false)

  // 计算属性
  const hasResult = computed(() => currentResult.value !== null)
  const resultPath = computed(() => currentResult.value?.result?.path || [])
  const resultDistance = computed(() => currentResult.value?.result?.distance || 0)
  const resultTime = computed(() => currentResult.value?.result?.timeMs || 0)
  const resultCoverage = computed(() => currentResult.value?.result?.coverage || 0)

  const canCalculate = computed(() => {
    const mapStore = useMapStore()
    return mapStore.hasValidPathData
  })

  const calculationStatus = computed(() => {
    if (!canCalculate.value) {
      return {
        valid: false,
        message: '需要至少一个起点和一个终点',
        missing: []
      }
    }

    const mapStore = useMapStore()
    const missing = []

    if (!mapStore.startNode) missing.push('起点')
    if (!mapStore.endNode) missing.push('终点')

    return {
      valid: missing.length === 0,
      message: missing.length > 0 ? `缺少: ${missing.join(', ')}` : '可以计算',
      missing
    }
  })

  // 动作
  async function calculateAlgorithm() {
    if (!canCalculate.value) {
      ElMessage.warning('无法计算：' + calculationStatus.value.message)
      return null
    }

    isLoading.value = true

    try {
      const mapStore = useMapStore()

      const requestData = {
        algorithm: algorithmType.value,
        parameters: { ...algorithmParams.value },
        nodes: mapStore.nodes.map(node => ({
          id: node.id,
          x: node.x,
          y: node.y,
          type: node.type,
          weight: node.weight || 1,
          properties: node.properties || {}
        }))
      }

      const response = await algorithmService.calculate(
        algorithmType.value,
        requestData.parameters,
        requestData.nodes
      )

      const result = {
        id: Date.now(),
        algorithm: algorithmType.value,
        parameters: { ...algorithmParams.value },
        inputNodes: [...requestData.nodes],
        timestamp: new Date().toISOString(),
        result: response.data || {}
      }

      currentResult.value = result
      historyResults.value.unshift(result)

      // 只保留最近10条历史记录
      if (historyResults.value.length > 10) {
        historyResults.value = historyResults.value.slice(0, 10)
      }

      ElMessage.success('算法计算完成')
      return result
    } catch (error) {
      ElMessage.error('算法计算失败')
      console.error(error)
      return null
    } finally {
      isLoading.value = false
    }
  }

  function setAlgorithmType(type) {
    algorithmType.value = type

    // 根据算法类型设置默认参数
    switch (type) {
      case 'a-star':
        algorithmParams.value = {
          heuristic: 'euclidean',
          allowDiagonal: true,
          weight: 1.0
        }
        break
      case 'dijkstra':
        algorithmParams.value = {
          allowDiagonal: true,
          weight: 1.0
        }
        break
    }
  }

  function updateAlgorithmParams(params) {
    algorithmParams.value = { ...algorithmParams.value, ...params }
  }

  function clearResult() {
    currentResult.value = null
  }

  function clearHistory() {
    historyResults.value = []
  }

  function selectResult(resultId) {
    const result = historyResults.value.find(r => r.id === resultId)
    if (result) {
      currentResult.value = result
      algorithmType.value = result.algorithm
      algorithmParams.value = result.parameters
    }
  }

  // 导出状态和动作
  return {
    // 状态
    algorithmType,
    algorithmParams,
    currentResult,
    historyResults,
    isLoading,

    // 计算属性
    hasResult,
    resultPath,
    resultDistance,
    resultTime,
    resultCoverage,
    canCalculate,
    calculationStatus,

    // 动作
    calculateAlgorithm,
    setAlgorithmType,
    updateAlgorithmParams,
    clearResult,
    clearHistory,
    selectResult
  }
})