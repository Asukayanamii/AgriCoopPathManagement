import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { mapService } from '@/services/dataService'
import { ElMessage } from 'element-plus'

export const useMapStore = defineStore('map', () => {
  // 状态
  const mapList = ref([])
  const currentMap = ref(null)
  const selectedMapId = ref(1)
  const nodes = ref([])
  const selectedNode = ref(null)
  const nextNodeId = ref(1)

  // 计算属性
  const hasNodes = computed(() => nodes.value.length > 0)
  const nodeCount = computed(() => nodes.value.length)
  const startNode = computed(() => nodes.value.find(node => node.type === '起点'))
  const endNode = computed(() => nodes.value.find(node => node.type === '终点'))
  const obstacleNodes = computed(() => nodes.value.filter(node => node.type === '障碍点'))
  const hasValidPathData = computed(() => {
    return startNode.value && endNode.value && nodes.value.length >= 2
  })

  // 动作
  async function loadMapList() {
    try {
      const response = await mapService.getMapList()
      mapList.value = response.data || []
      if (mapList.value.length > 0 && !currentMap.value) {
        selectedMapId.value = mapList.value[0].id
        await loadMap(selectedMapId.value)
      }
      return mapList.value
    } catch (error) {
      ElMessage.error('加载地图列表失败')
      console.error(error)
      return []
    }
  }

  async function loadMap(mapId) {
    try {
      const map = mapList.value.find(m => m.id === mapId)
      if (map) {
        currentMap.value = map
        selectedMapId.value = mapId
        await loadNodes(mapId)
        return map
      }
      return null
    } catch (error) {
      ElMessage.error('加载地图失败')
      console.error(error)
      return null
    }
  }

  async function loadNodes(mapId) {
    try {
      const response = await mapService.getMapNodes(mapId)
      nodes.value = response.data || []
      // 计算下一个可用ID
      if (nodes.value.length > 0) {
        const maxId = Math.max(...nodes.value.map(n => n.id || 0))
        nextNodeId.value = maxId + 1
      } else {
        nextNodeId.value = 1
      }
      return nodes.value
    } catch (error) {
      ElMessage.error('加载节点数据失败')
      console.error(error)
      nodes.value = []
      nextNodeId.value = 1
      return []
    }
  }

  async function saveNodes() {
    if (!currentMap.value) {
      ElMessage.warning('请先选择地图')
      return false
    }

    try {
      const response = await mapService.saveMapNodes(currentMap.value.id, nodes.value)
      ElMessage.success('节点保存成功')
      return response.data || true
    } catch (error) {
      ElMessage.error('保存节点失败')
      console.error(error)
      return false
    }
  }

  function addNode(nodeData) {
    const newNode = {
      id: nextNodeId.value++,
      x: nodeData.x,
      y: nodeData.y,
      label: nodeData.label || `节点${nextNodeId.value - 1}`,
      type: nodeData.type || '关注点',
      color: nodeData.color || '#f5222d',
      weight: nodeData.weight || 1,
      properties: nodeData.properties || {},
      createdAt: new Date().toISOString()
    }

    nodes.value.push(newNode)
    return newNode
  }

  function updateNode(nodeId, updates) {
    const index = nodes.value.findIndex(n => n.id === nodeId)
    if (index !== -1) {
      nodes.value[index] = { ...nodes.value[index], ...updates }
      return nodes.value[index]
    }
    return null
  }

  function deleteNode(nodeId) {
    const index = nodes.value.findIndex(n => n.id === nodeId)
    if (index !== -1) {
      nodes.value.splice(index, 1)
      return true
    }
    return false
  }

  function clearAllNodes() {
    nodes.value = []
    nextNodeId.value = 1
    selectedNode.value = null
  }

  function selectNode(node) {
    selectedNode.value = node
  }

  function clearSelectedNode() {
    selectedNode.value = null
  }

  // 导出状态和动作
  return {
    // 状态
    mapList,
    currentMap,
    selectedMapId,
    nodes,
    selectedNode,
    nextNodeId,

    // 计算属性
    hasNodes,
    nodeCount,
    startNode,
    endNode,
    obstacleNodes,
    hasValidPathData,

    // 动作
    loadMapList,
    loadMap,
    loadNodes,
    saveNodes,
    addNode,
    updateNode,
    deleteNode,
    clearAllNodes,
    selectNode,
    clearSelectedNode
  }
})