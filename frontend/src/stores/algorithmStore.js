import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { cluster, resourceSearch, pathPlanning, pipeline } from '@/api/algorithm'
import { ElMessage } from 'element-plus'

export const useAlgorithmStore = defineStore('algorithm', () => {
  const currentResult = ref(null)
  const historyResults = ref([])
  const isLoading = ref(false)

  const hasResult = computed(() => currentResult.value !== null)

  async function runCluster(params) {
    isLoading.value = true
    try {
      const res = await cluster(params)
      if (res.code === 1) {
        currentResult.value = { type: 'cluster', data: res.data, timestamp: new Date().toISOString() }
        historyResults.value.unshift({ ...currentResult.value })
        if (historyResults.value.length > 10) historyResults.value = historyResults.value.slice(0, 10)
        ElMessage.success('聚类计算完成')
        return res.data
      }
      ElMessage.error(res.msg || '聚类计算失败')
      return null
    } catch (e) {
      ElMessage.error('聚类计算失败')
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function runResourceSearch(params) {
    isLoading.value = true
    try {
      const res = await resourceSearch(params)
      if (res.code === 1) {
        currentResult.value = { type: 'resource-search', data: res.data, timestamp: new Date().toISOString() }
        historyResults.value.unshift({ ...currentResult.value })
        if (historyResults.value.length > 10) historyResults.value = historyResults.value.slice(0, 10)
        ElMessage.success('资源搜索完成')
        return res.data
      }
      ElMessage.error(res.msg || '资源搜索失败')
      return null
    } catch (e) {
      ElMessage.error('资源搜索失败')
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function runPathPlanning(params) {
    isLoading.value = true
    try {
      const res = await pathPlanning(params)
      if (res.code === 1) {
        currentResult.value = { type: 'path-planning', data: res.data, timestamp: new Date().toISOString() }
        historyResults.value.unshift({ ...currentResult.value })
        if (historyResults.value.length > 10) historyResults.value = historyResults.value.slice(0, 10)
        ElMessage.success('路径规划完成')
        return res.data
      }
      ElMessage.error(res.msg || '路径规划失败')
      return null
    } catch (e) {
      ElMessage.error('路径规划失败')
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function runPipeline(params) {
    isLoading.value = true
    try {
      const res = await pipeline(params)
      if (res.code === 1) {
        currentResult.value = { type: 'pipeline', data: res.data, timestamp: new Date().toISOString() }
        historyResults.value.unshift({ ...currentResult.value })
        if (historyResults.value.length > 10) historyResults.value = historyResults.value.slice(0, 10)
        ElMessage.success('协同流水线执行完成')
        return res.data
      }
      ElMessage.error(res.msg || '流水线执行失败')
      return null
    } catch (e) {
      ElMessage.error('流水线执行失败')
      return null
    } finally {
      isLoading.value = false
    }
  }

  function clearResult() { currentResult.value = null }
  function clearHistory() { historyResults.value = [] }

  return {
    currentResult, historyResults, isLoading, hasResult,
    runCluster, runResourceSearch, runPathPlanning, runPipeline,
    clearResult, clearHistory
  }
})
