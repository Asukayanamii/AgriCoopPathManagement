<template>
  <div class="map-display-container">
    <!-- 工作流状态栏 -->
    <div class="workflow-status" v-if="workflowStore.currentStep === workflowStore.STEPS.MAP_SELECTION">
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
          请先选择地图{{ mapStore.currentMap ? '并标记节点' : '' }}
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
        <!-- 地图选择 -->
        <el-select v-model="selectedMapId" placeholder="选择地图" @change="loadMap" style="width: 200px; margin-right: 16px;">
          <el-option
            v-for="map in mapList"
            :key="map.id"
            :label="map.name"
            :value="map.id"
          />
        </el-select>

        <!-- 模式切换 -->
        <el-switch
          v-model="demoMode"
          active-text="演示模式"
          inactive-text="真实模式"
          style="margin-right: 16px;"
          @change="toggleMode"
        />

        <!-- 缩放控制 -->
        <div class="zoom-controls">
          <el-button :icon="ZoomIn" @click="zoomIn" circle />
          <el-button :icon="ZoomOut" @click="zoomOut" circle />
          <el-button :icon="Refresh" @click="resetView" circle />
        </div>
      </div>

      <div class="right-controls">
        <!-- 节点操作按钮 -->
        <el-button type="primary" :icon="Delete" @click="clearAllNodes">
          清空节点
        </el-button>
        <el-button type="success" :icon="Download" @click="saveNodes">
          保存节点
        </el-button>
      </div>
    </div>

    <div class="map-content">
      <!-- 左侧地图区域 -->
      <div class="map-area" ref="mapContainer">
        <div
          class="map-image-container"
          ref="mapImageContainer"
          :style="{
            transform: `scale(${scale}) translate(${translateX}px, ${translateY}px)`,
            cursor: isDragging ? 'grabbing' : 'grab'
          }"
          @mousedown="startDrag"
          @click="handleMapClick"
        >
          <img
            :src="currentMap?.url || ''"
            :alt="currentMap?.name || '地图'"
            class="map-image"
            ref="mapImage"
            @load="centerMap"
          />

          <!-- 节点标记 -->
          <div
            v-for="node in nodes"
            :key="node.id"
            class="node-marker"
            :style="{
              left: `${node.x * scale}px`,
              top: `${node.y * scale}px`,
              backgroundColor: node.color || '#f5222d',
              transform: `translate(-50%, -50%) scale(${1/scale})`
            }"
            @click.stop="selectNode(node)"
            @contextmenu.prevent="showNodeContextMenu($event, node)"
          >
            <div class="node-label">{{ node.label || node.id }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧节点信息面板 -->
      <div class="node-panel">
        <div class="panel-header">
          <h3>节点管理 ({{ nodes.length }})</h3>
          <el-button :icon="Plus" type="primary" text @click="addRandomNode">
            添加测试节点
          </el-button>
        </div>

        <!-- 节点列表 -->
        <div class="node-list">
          <el-table :data="nodes" style="width: 100%" size="small" height="calc(100% - 100px)">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="label" label="标签" width="120">
              <template #default="{ row }">
                <el-input
                  v-if="row.editing"
                  v-model="row.label"
                  size="small"
                  @blur="row.editing = false"
                  @keyup.enter="row.editing = false"
                />
                <span v-else @click="row.editing = true">{{ row.label || `节点${row.id}` }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="x" label="X坐标" width="80" />
            <el-table-column prop="y" label="Y坐标" width="80" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-select
                  v-model="row.type"
                  size="small"
                  v-if="row.editingType"
                  @change="row.editingType = false"
                >
                  <el-option label="关注点" value="关注点" />
                  <el-option label="障碍物" value="障碍物" />
                  <el-option label="起点" value="起点" />
                  <el-option label="终点" value="终点" />
                </el-select>
                <span v-else @click="row.editingType = true">{{ row.type }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ $index }">
                <el-button type="danger" :icon="Delete" size="small" @click="removeNode($index)" />
                <el-button type="warning" :icon="Edit" size="small" @click="editNode($index)" />
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 当前选中节点信息 -->
        <div class="selected-node-info" v-if="selectedNode">
          <h4>选中节点信息</h4>
          <el-form label-width="80px" size="small">
            <el-form-item label="ID">
              <el-input v-model="selectedNode.id" disabled />
            </el-form-item>
            <el-form-item label="坐标">
              <el-input-number v-model="selectedNode.x" :min="0" :max="1000" />
              <el-input-number v-model="selectedNode.y" :min="0" :max="1000" style="margin-left: 8px;" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="selectedNode.type">
                <el-option label="关注点" value="关注点" />
                <el-option label="障碍物" value="障碍物" />
                <el-option label="起点" value="起点" />
                <el-option label="终点" value="终点" />
              </el-select>
            </el-form-item>
            <el-form-item label="颜色">
              <el-color-picker v-model="selectedNode.color" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updateSelectedNode">更新</el-button>
              <el-button @click="selectedNode = null">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: `${contextMenu.x}px`, top: `${contextMenu.y}px` }"
      @mouseleave="contextMenu.visible = false"
    >
      <div class="context-menu-item" @click="editContextNode">编辑节点</div>
      <div class="context-menu-item" @click="deleteContextNode">删除节点</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ZoomIn,
  ZoomOut,
  Refresh,
  Delete,
  Download,
  Plus,
  Edit
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode, mapService } from '@/services/dataService'
import { useMapStore } from '@/stores/mapStore'
import { useWorkflowStore } from '@/stores/workflowStore'

// Store初始化
const mapStore = useMapStore()
const workflowStore = useWorkflowStore()

// 工作流状态
const workflowStatus = computed(() => workflowStore.getStepStatus(workflowStore.STEPS.MAP_SELECTION))
const canProceed = computed(() => workflowStore.canProceed)
const validation = computed(() => workflowStore.getCurrentValidation())

// 地图数据 - 从store获取
const mapList = computed(() => mapStore.mapList)
const selectedMapId = computed({
  get: () => mapStore.selectedMapId,
  set: (value) => { mapStore.selectedMapId = value }
})
const currentMap = computed(() => mapStore.currentMap)

// 节点数据 - 从store获取
const nodes = computed(() => mapStore.nodes)
const selectedNode = computed({
  get: () => mapStore.selectedNode,
  set: (value) => { if (value === null) mapStore.clearSelectedNode(); else mapStore.selectNode(value) }
})
const nextNodeId = computed(() => mapStore.nextNodeId)

// 视图控制
const scale = ref(1)
const translateX = ref(0)
const translateY = ref(0)
const isDragging = ref(false)
const dragStart = reactive({ x: 0, y: 0 })

// 引用
const mapContainer = ref(null)
const mapImageContainer = ref(null)
const mapImage = ref(null)

// 模式控制
const demoMode = ref(getCurrentMode())

// 右键菜单
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  node: null
})

// 预生成的测试地图数据
const mockMaps = [
  { id: 1, name: '农田区域A', url: 'https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=1000&h=800&fit=crop', width: 1000, height: 800 },
  { id: 2, name: '农田区域B', url: 'https://images.unsplash.com/photo-1505253668822-42074d58a7c6?w=1200&h=900&fit=crop', width: 1200, height: 900 },
  { id: 3, name: '果园区域C', url: 'https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&h=600&fit=crop', width: 800, height: 600 }
]

// 预生成的测试节点数据
const mockNodes = [
  { id: 1, x: 150, y: 200, label: '水源点', type: '关注点', color: '#1890ff' },
  { id: 2, x: 400, y: 300, label: '障碍物1', type: '障碍物', color: '#ff4d4f' },
  { id: 3, x: 600, y: 150, label: '起点', type: '起点', color: '#52c41a' },
  { id: 4, x: 800, y: 450, label: '终点', type: '终点', color: '#722ed1' },
  { id: 5, x: 300, y: 500, label: '施肥点', type: '关注点', color: '#1890ff' }
]

// 初始化
onMounted(() => {
  loadMapList()
  loadMap()

  // 添加键盘事件
  document.addEventListener('keydown', handleKeyDown)
})

// 图片加载完成后居中显示
function centerMap() {
  if (!mapContainer.value || !mapImage.value) return

  const containerWidth = mapContainer.value.clientWidth
  const containerHeight = mapContainer.value.clientHeight
  const imageWidth = mapImage.value.naturalWidth || mapImage.value.width
  const imageHeight = mapImage.value.naturalHeight || mapImage.value.height

  // 如果图片比容器小，居中显示
  if (imageWidth < containerWidth || imageHeight < containerHeight) {
    translateX.value = (containerWidth - imageWidth) / 2
    translateY.value = (containerHeight - imageHeight) / 2
    scale.value = 1
  } else {
    // 图片比容器大，缩放以适应
    const scaleX = containerWidth / imageWidth
    const scaleY = containerHeight / imageHeight
    scale.value = Math.min(scaleX, scaleY)
    // 缩放后居中
    const scaledWidth = imageWidth * scale.value
    const scaledHeight = imageHeight * scale.value
    translateX.value = (containerWidth - scaledWidth) / 2
    translateY.value = (containerHeight - scaledHeight) / 2
  }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})

// 加载地图列表
async function loadMapList() {
  try {
    // 调用store的loadMapList，它会处理演示/真实模式切换
    const maps = await mapStore.loadMapList()

    // 如果有地图但当前没有选中地图，加载第一个地图
    if (maps.length > 0 && !mapStore.currentMap) {
      await mapStore.loadMap(maps[0].id)
    }

    return maps
  } catch (error) {
    ElMessage.error('加载地图列表失败')
    console.error(error)
    return []
  }
}

// 加载地图
async function loadMap() {
  if (!selectedMapId.value) return

  try {
    const map = await mapStore.loadMap(selectedMapId.value)
    if (map) {
      scale.value = 1
      translateX.value = 0
      translateY.value = 0
      // 图片加载后会触发@load事件调用centerMap
    }
    return map
  } catch (error) {
    console.error('加载地图失败:', error)
    return null
  }
}


// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  loadMapList()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
}

// 地图点击事件
function handleMapClick(event) {
  if (!mapImageContainer.value || !mapImage.value) return

  const rect = mapImageContainer.value.getBoundingClientRect()
  // 计算相对于容器左上角的坐标
  const containerX = event.clientX - rect.left
  const containerY = event.clientY - rect.top

  // 考虑容器的变换：先缩放，后平移
  // 逆变换：先减去平移，再除以缩放
  const x = (containerX - translateX.value) / scale.value
  const y = (containerY - translateY.value) / scale.value

  // 检查是否在图像范围内
  const imageWidth = mapImage.value.naturalWidth || mapImage.value.width
  const imageHeight = mapImage.value.naturalHeight || mapImage.value.height
  if (x >= 0 && x <= imageWidth && y >= 0 && y <= imageHeight) {
    addNode(x, y)
  }
}

// 添加节点
function addNode(x, y) {
  const nodeData = {
    x: Math.round(x),
    y: Math.round(y),
    label: `节点${mapStore.nextNodeId}`,
    type: '关注点',
    color: '#f5222d' // 默认红色
  }
  const newNode = mapStore.addNode(nodeData)
  ElMessage.success(`已添加节点 (${newNode.x}, ${newNode.y})`)
}

// 添加随机测试节点
function addRandomNode() {
  if (!currentMap.value) return

  const x = Math.floor(Math.random() * currentMap.value.width)
  const y = Math.floor(Math.random() * currentMap.value.height)
  addNode(x, y)
}

// 选择节点
function selectNode(node) {
  mapStore.selectNode(node)
}

// 更新选中节点
function updateSelectedNode() {
  if (mapStore.selectedNode) {
    const nodeId = mapStore.selectedNode.id
    const updates = { ...mapStore.selectedNode }
    // 移除id属性，因为updateNode不应该更新id
    delete updates.id
    mapStore.updateNode(nodeId, updates)
    ElMessage.success('节点已更新')
  }
  mapStore.clearSelectedNode()
}

// 删除节点
function removeNode(index) {
  if (index >= 0 && index < mapStore.nodes.length) {
    const node = mapStore.nodes[index]
    mapStore.deleteNode(node.id)
    ElMessage.success('节点已删除')
  }
}

// 编辑节点
function editNode(index) {
  if (index >= 0 && index < mapStore.nodes.length) {
    mapStore.selectNode(mapStore.nodes[index])
  }
}

// 清空所有节点
function clearAllNodes() {
  ElMessageBox.confirm(
    '确定要清空所有节点吗？此操作不可撤销。',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    mapStore.clearAllNodes()
    ElMessage.success('所有节点已清空')
  })
}

// 保存节点
async function saveNodes() {
  try {
    const response = await mapService.saveMapNodes(selectedMapId.value, mapStore.nodes)
    ElMessage.success('节点保存成功')
    return response.data || true
  } catch (error) {
    ElMessage.error('保存节点失败')
    console.error(error)
    return false
  }
}

// 缩放控制
function zoomIn() {
  scale.value = Math.min(scale.value + 0.1, 3)
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.1, 0.5)
}

function resetView() {
  scale.value = 1
  translateX.value = 0
  translateY.value = 0
  centerMap()
}

// 拖拽控制
function startDrag(event) {
  isDragging.value = true
  dragStart.x = event.clientX - translateX.value
  dragStart.y = event.clientY - translateY.value

  const moveHandler = (moveEvent) => {
    if (!isDragging.value) return
    translateX.value = moveEvent.clientX - dragStart.x
    translateY.value = moveEvent.clientY - dragStart.y
  }

  const stopHandler = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', moveHandler)
    document.removeEventListener('mouseup', stopHandler)
  }

  document.addEventListener('mousemove', moveHandler)
  document.addEventListener('mouseup', stopHandler)
}

// 右键菜单
function showNodeContextMenu(event, node) {
  event.preventDefault()
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.node = node
}

function editContextNode() {
  if (contextMenu.node) {
    mapStore.selectNode(contextMenu.node)
  }
  contextMenu.visible = false
}

function deleteContextNode() {
  if (contextMenu.node) {
    mapStore.deleteNode(contextMenu.node.id)
    ElMessage.success('节点已删除')
  }
  contextMenu.visible = false
}

// 键盘快捷键
function handleKeyDown(event) {
  if (event.key === 'Escape') {
    mapStore.clearSelectedNode()
    contextMenu.visible = false
  }
  if (event.key === '+' || event.key === '=') {
    event.preventDefault()
    zoomIn()
  }
  if (event.key === '-' || event.key === '_') {
    event.preventDefault()
    zoomOut()
  }
  if (event.key === '0') {
    event.preventDefault()
    resetView()
  }
}
</script>

<style scoped>
.map-display-container {
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

.zoom-controls {
  display: flex;
  gap: 8px;
  margin-left: 16px;
}

.right-controls {
  display: flex;
  gap: 8px;
}

.map-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #f5f5f5;
}

.map-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #1a202c;
}

.map-image-container {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: 0 0;
  transition: transform 0.1s ease-out;
}

.map-image {
  display: block;
  max-width: none; /* 允许放大超过原始尺寸 */
  user-select: none;
  -webkit-user-drag: none;
}

.node-marker {
  position: absolute;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 10;
}

.node-marker:hover {
  transform: translate(-50%, -50%) scale(1.2) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  z-index: 20;
}

.node-label {
  position: absolute;
  top: -30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.node-marker:hover .node-label {
  opacity: 1;
}

.node-panel {
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

.node-list {
  flex: 1;
  padding: 0 16px;
  overflow: hidden;
}

.selected-node-info {
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
  max-height: 300px;
  overflow-y: auto;
}

.selected-node-info h4 {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  min-width: 120px;
  overflow: hidden;
}

.context-menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #4a5568;
  transition: background 0.2s;
}

.context-menu-item:hover {
  background: #f7fafc;
}

/* 工作流状态栏 */
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

/* 响应式适配 */
@media (max-width: 768px) {
  .map-content {
    flex-direction: column;
  }

  .node-panel {
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
}
</style>