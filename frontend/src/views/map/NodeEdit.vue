<template>
  <div class="node-edit-container">
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

        <!-- 操作按钮 -->
        <el-button type="primary" :icon="Plus" @click="addNode">新增节点</el-button>
        <el-button :icon="Delete" @click="batchDelete" :disabled="selectedNodes.length === 0">
          批量删除
        </el-button>
        <el-button :icon="Download" @click="exportNodes">导出数据</el-button>
        <el-button :icon="Upload" @click="importDialogVisible = true">导入数据</el-button>
      </div>

      <div class="right-controls">
        <!-- 搜索和筛选 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索节点标签或类型"
          style="width: 200px; margin-right: 16px;"
          :prefix-icon="Search"
          clearable
        />
        <el-button :icon="Refresh" @click="refreshNodes">刷新</el-button>
      </div>
    </div>

    <div class="content-area">
      <!-- 左侧节点列表 -->
      <div class="node-list">
        <div class="list-header">
          <h3>节点列表 ({{ filteredNodes.length }})</h3>
          <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
        </div>

        <el-table
          :data="filteredNodes"
          style="width: 100%;"
          height="calc(100vh - 280px)"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="80" sortable />
          <el-table-column prop="label" label="标签" width="120">
            <template #default="{ row }">
              <el-tag size="small" :color="row.color" style="color: white;">
                {{ row.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="x" label="X坐标" width="80" sortable />
          <el-table-column prop="y" label="Y坐标" width="80" sortable />
          <el-table-column prop="weight" label="权重" width="80" sortable />
          <el-table-column prop="description" label="描述" min-width="150" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="text" size="small" @click="editNode(row)">编辑</el-button>
              <el-button type="text" size="small" @click="deleteNode(row)" style="color: #f56c6c;">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 右侧编辑表单 -->
      <div class="edit-form">
        <div class="form-header">
          <h3>{{ editingNode.id ? '编辑节点' : '新增节点' }}</h3>
          <el-button :icon="Close" @click="cancelEdit" v-if="editingNode.id">取消</el-button>
        </div>

        <el-form
          ref="nodeFormRef"
          :model="editingNode"
          :rules="nodeRules"
          label-width="100px"
          style="padding: 20px;"
        >
          <el-form-item label="节点标签" prop="label">
            <el-input v-model="editingNode.label" placeholder="请输入节点标签" />
          </el-form-item>

          <el-form-item label="节点类型" prop="type">
            <el-select v-model="editingNode.type" placeholder="选择节点类型" style="width: 100%;">
              <el-option label="起点" value="起点" />
              <el-option label="终点" value="终点" />
              <el-option label="关注点" value="关注点" />
              <el-option label="障碍物" value="障碍物" />
              <el-option label="水源点" value="水源点" />
              <el-option label="施肥点" value="施肥点" />
              <el-option label="农药点" value="农药点" />
            </el-select>
          </el-form-item>

          <el-form-item label="节点颜色" prop="color">
            <el-color-picker v-model="editingNode.color" show-alpha />
            <span style="margin-left: 10px; font-size: 12px; color: #666;">
              类型推荐色：起点(#52c41a) 终点(#722ed1) 障碍物(#ff4d4f) 关注点(#1890ff)
            </span>
          </el-form-item>

          <el-form-item label="坐标位置" required>
            <div style="display: flex; gap: 16px;">
              <el-form-item prop="x" style="margin-bottom: 0; flex: 1;">
                <el-input-number
                  v-model="editingNode.x"
                  :min="0"
                  :max="1000"
                  :step="1"
                  placeholder="X坐标"
                  style="width: 100%;"
                />
              </el-form-item>
              <el-form-item prop="y" style="margin-bottom: 0; flex: 1;">
                <el-input-number
                  v-model="editingNode.y"
                  :min="0"
                  :max="1000"
                  :step="1"
                  placeholder="Y坐标"
                  style="width: 100%;"
                />
              </el-form-item>
            </div>
          </el-form-item>

          <el-form-item label="节点权重" prop="weight">
            <el-slider
              v-model="editingNode.weight"
              :min="0"
              :max="10"
              :step="0.1"
              show-input
              style="width: 100%;"
            />
            <span style="margin-left: 10px; font-size: 12px; color: #666;">
              权重越高，算法越倾向于选择该节点
            </span>
          </el-form-item>

          <el-form-item label="描述信息" prop="description">
            <el-input
              v-model="editingNode.description"
              type="textarea"
              :rows="3"
              placeholder="请输入节点描述信息"
            />
          </el-form-item>

          <el-form-item label="附加属性">
            <el-input
              v-model="editingNode.propertiesJson"
              type="textarea"
              :rows="2"
              placeholder='JSON格式，如：{"radius": 5, "height": 2}'
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="saveNode" :loading="isSaving">
              {{ editingNode.id ? '更新节点' : '创建节点' }}
            </el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 批量操作面板 -->
        <div class="batch-panel" v-if="selectedNodes.length > 0">
          <h4>批量操作 ({{ selectedNodes.length }}个节点)</h4>
          <div style="display: flex; gap: 12px; margin-top: 12px;">
            <el-button size="small" @click="batchUpdateType">
              批量设置类型
            </el-button>
            <el-button size="small" @click="batchUpdateWeight">
              批量设置权重
            </el-button>
            <el-button size="small" @click="batchUpdateColor">
              批量设置颜色
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入节点数据" width="500">
      <el-alert
        title="支持JSON格式导入"
        type="info"
        description="JSON数组格式：[{label: '节点1', x: 100, y: 200, type: '起点', ...}]"
        show-icon
        style="margin-bottom: 20px;"
      />
      <el-input
        v-model="importJson"
        type="textarea"
        :rows="10"
        placeholder="粘贴JSON数据"
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleImport">导入</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量设置类型对话框 -->
    <el-dialog v-model="batchTypeDialogVisible" title="批量设置类型" width="400">
      <el-select v-model="batchType" placeholder="选择节点类型" style="width: 100%;">
        <el-option label="起点" value="起点" />
        <el-option label="终点" value="终点" />
        <el-option label="关注点" value="关注点" />
        <el-option label="障碍物" value="障碍物" />
        <el-option label="水源点" value="水源点" />
        <el-option label="施肥点" value="施肥点" />
        <el-option label="农药点" value="农药点" />
      </el-select>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchTypeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmBatchType">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Delete,
  Download,
  Upload,
  Search,
  Refresh,
  Close
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode, mapService } from '@/services/dataService'

// 模式控制
const demoMode = ref(getCurrentMode())

// 搜索关键词
const searchKeyword = ref('')

// 节点列表数据
const nodes = ref([])
const selectedNodes = ref([])
const selectAll = ref(false)

// 编辑节点数据
const editingNode = reactive({
  id: '',
  label: '',
  type: '',
  color: '#1890ff',
  x: 0,
  y: 0,
  weight: 1.0,
  description: '',
  propertiesJson: '{}'
})

// 表单引用和验证规则
const nodeFormRef = ref(null)
const nodeRules = {
  label: [{ required: true, message: '请输入节点标签', trigger: 'blur' }],
  type: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
  x: [{ required: true, message: '请输入X坐标', trigger: 'blur' }],
  y: [{ required: true, message: '请输入Y坐标', trigger: 'blur' }]
}

// 对话框状态
const importDialogVisible = ref(false)
const batchTypeDialogVisible = ref(false)
const importJson = ref('')
const batchType = ref('')

// 操作状态
const isSaving = ref(false)

// 预生成的测试节点数据
const mockNodes = [
  { id: 1, label: '水源点', type: '水源点', color: '#1890ff', x: 150, y: 200, weight: 1.5, description: '主要灌溉水源', propertiesJson: '{"flowRate": 5}' },
  { id: 2, label: '起点', type: '起点', color: '#52c41a', x: 100, y: 100, weight: 1.0, description: '作业起点位置', propertiesJson: '{}' },
  { id: 3, label: '障碍物1', type: '障碍物', color: '#ff4d4f', x: 400, y: 300, weight: 10.0, description: '大型障碍物，需要绕行', propertiesJson: '{"radius": 15}' },
  { id: 4, label: '施肥点', type: '施肥点', color: '#1890ff', x: 300, y: 500, weight: 2.0, description: '需要施肥的区域', propertiesJson: '{"fertilizerType": "NPK"}' },
  { id: 5, label: '终点', type: '终点', color: '#722ed1', x: 800, y: 450, weight: 1.0, description: '作业终点位置', propertiesJson: '{}' },
  { id: 6, label: '关注点', type: '关注点', color: '#1890ff', x: 600, y: 150, weight: 1.2, description: '需要特别关注的区域', propertiesJson: '{"importance": "high"}' },
  { id: 7, label: '农药点', type: '农药点', color: '#faad14', x: 500, y: 350, weight: 1.8, description: '需要喷洒农药的区域', propertiesJson: '{"pesticideType": "herbicide"}' },
  { id: 8, label: '障碍物2', type: '障碍物', color: '#ff4d4f', x: 700, y: 250, weight: 8.0, description: '小型障碍物', propertiesJson: '{"radius": 8}' }
]

// 计算属性：过滤后的节点
const filteredNodes = computed(() => {
  if (!searchKeyword.value.trim()) {
    return nodes.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return nodes.value.filter(node =>
    node.label.toLowerCase().includes(keyword) ||
    node.type.toLowerCase().includes(keyword) ||
    node.description.toLowerCase().includes(keyword)
  )
})

// 初始化加载节点数据
onMounted(() => {
  loadNodes()
})

// 加载节点数据
async function loadNodes() {
  try {
    if (demoMode.value) {
      // 演示模式：使用模拟数据
      nodes.value = [...mockNodes]
      ElMessage.success('加载演示数据成功')
    } else {
      // 真实模式：调用API
      // 这里需要传递地图ID，暂时使用默认值1
      const response = await mapService.getMapNodes(1)
      nodes.value = response.data || []
      ElMessage.success('加载节点数据成功')
    }
  } catch (error) {
    console.error('加载节点数据失败:', error)
    ElMessage.error('加载节点数据失败')
    // 失败时使用演示数据
    nodes.value = [...mockNodes]
  }
}

// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
  loadNodes()
}

// 刷新节点
function refreshNodes() {
  loadNodes()
}

// 新增节点
function addNode() {
  Object.assign(editingNode, {
    id: '',
    label: '',
    type: '',
    color: '#1890ff',
    x: 0,
    y: 0,
    weight: 1.0,
    description: '',
    propertiesJson: '{}'
  })
}

// 编辑节点
function editNode(node) {
  Object.assign(editingNode, {
    id: node.id,
    label: node.label,
    type: node.type,
    color: node.color,
    x: node.x,
    y: node.y,
    weight: node.weight,
    description: node.description,
    propertiesJson: node.propertiesJson || '{}'
  })
}

// 保存节点
async function saveNode() {
  if (!nodeFormRef.value) return

  try {
    await nodeFormRef.value.validate()
    isSaving.value = true

    // 准备节点数据
    const nodeData = {
      id: editingNode.id,
      label: editingNode.label,
      type: editingNode.type,
      color: editingNode.color,
      x: editingNode.x,
      y: editingNode.y,
      weight: editingNode.weight,
      description: editingNode.description,
      properties: JSON.parse(editingNode.propertiesJson || '{}')
    }

    if (demoMode.value) {
      // 演示模式：本地更新
      if (nodeData.id) {
        // 更新现有节点
        const index = nodes.value.findIndex(n => n.id === nodeData.id)
        if (index !== -1) {
          nodes.value[index] = { ...nodeData, propertiesJson: editingNode.propertiesJson }
        }
      } else {
        // 新增节点
        const newId = Math.max(...nodes.value.map(n => n.id), 0) + 1
        nodeData.id = newId
        nodes.value.unshift({ ...nodeData, propertiesJson: editingNode.propertiesJson })
      }
      ElMessage.success(editingNode.id ? '节点更新成功' : '节点创建成功')
    } else {
      // 真实模式：调用API
      const response = await mapService.saveNodes({
        mapId: 1,
        nodes: [nodeData]
      })
      if (response.success) {
        ElMessage.success('节点保存成功')
        loadNodes() // 重新加载数据
      }
    }

    // 重置编辑表单
    addNode()
  } catch (error) {
    console.error('保存节点失败:', error)
    if (error && error.message !== 'validate') {
      ElMessage.error('保存节点失败')
    }
  } finally {
    isSaving.value = false
  }
}

// 删除节点
async function deleteNode(node) {
  try {
    await ElMessageBox.confirm(`确定删除节点 "${node.label}" 吗？`, '确认删除', {
      type: 'warning'
    })

    if (demoMode.value) {
      // 演示模式：本地删除
      nodes.value = nodes.value.filter(n => n.id !== node.id)
      ElMessage.success('节点删除成功')
    } else {
      // 真实模式：调用API删除
      // 这里需要实现删除API调用
      ElMessage.success('节点删除成功（演示模式）')
      loadNodes()
    }
  } catch (error) {
    // 用户取消删除
  }
}

// 批量删除
async function batchDelete() {
  if (selectedNodes.value.length === 0) return

  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedNodes.value.length} 个节点吗？`, '确认批量删除', {
      type: 'warning'
    })

    if (demoMode.value) {
      // 演示模式：本地删除
      const selectedIds = selectedNodes.value.map(node => node.id)
      nodes.value = nodes.value.filter(n => !selectedIds.includes(n.id))
      selectedNodes.value = []
      selectAll.value = false
      ElMessage.success(`成功删除 ${selectedIds.length} 个节点`)
    } else {
      // 真实模式：调用API批量删除
      ElMessage.success('批量删除成功（演示模式）')
      loadNodes()
    }
  } catch (error) {
    // 用户取消删除
  }
}

// 选择节点变化
function handleSelectionChange(selection) {
  selectedNodes.value = selection
  selectAll.value = selection.length === filteredNodes.value.length && filteredNodes.value.length > 0
}

// 全选/取消全选
function handleSelectAll(val) {
  if (val) {
    selectedNodes.value = [...filteredNodes.value]
  } else {
    selectedNodes.value = []
  }
}

// 导出节点数据
function exportNodes() {
  if (nodes.value.length === 0) {
    ElMessage.warning('没有可导出的节点数据')
    return
  }

  const exportData = nodes.value.map(node => ({
    label: node.label,
    type: node.type,
    color: node.color,
    x: node.x,
    y: node.y,
    weight: node.weight,
    description: node.description,
    properties: JSON.parse(node.propertiesJson || '{}')
  }))

  const dataStr = JSON.stringify(exportData, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr)
  const exportFileName = `节点数据_${new Date().getTime()}.json`

  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', exportFileName)
  linkElement.click()

  ElMessage.success('节点数据已导出')
}

// 导入节点数据
function handleImport() {
  try {
    if (!importJson.value.trim()) {
      ElMessage.warning('请输入要导入的JSON数据')
      return
    }

    const importedNodes = JSON.parse(importJson.value)
    if (!Array.isArray(importedNodes)) {
      ElMessage.warning('导入的数据必须是JSON数组格式')
      return
    }

    // 验证导入数据的基本结构
    const validNodes = importedNodes.filter(node =>
      node.label && typeof node.x === 'number' && typeof node.y === 'number'
    )

    if (validNodes.length === 0) {
      ElMessage.warning('没有有效的节点数据可以导入')
      return
    }

    // 为导入的节点添加ID并转换格式
    const maxId = Math.max(...nodes.value.map(n => n.id), 0)
    const newNodes = validNodes.map((node, index) => ({
      id: maxId + index + 1,
      label: node.label,
      type: node.type || '关注点',
      color: node.color || '#1890ff',
      x: node.x,
      y: node.y,
      weight: node.weight || 1.0,
      description: node.description || '',
      propertiesJson: JSON.stringify(node.properties || {})
    }))

    // 添加到现有节点列表
    nodes.value = [...newNodes, ...nodes.value]

    importDialogVisible.value = false
    importJson.value = ''
    ElMessage.success(`成功导入 ${newNodes.length} 个节点`)
  } catch (error) {
    console.error('导入数据失败:', error)
    ElMessage.error('导入数据失败，请检查JSON格式是否正确')
  }
}

// 批量设置类型
function batchUpdateType() {
  if (selectedNodes.value.length === 0) {
    ElMessage.warning('请先选择要操作的节点')
    return
  }
  batchType.value = ''
  batchTypeDialogVisible.value = true
}

// 确认批量设置类型
function confirmBatchType() {
  if (!batchType.value) {
    ElMessage.warning('请选择节点类型')
    return
  }

  selectedNodes.value.forEach(node => {
    const index = nodes.value.findIndex(n => n.id === node.id)
    if (index !== -1) {
      nodes.value[index].type = batchType.value
      // 根据类型自动设置推荐颜色
      switch(batchType.value) {
        case '起点': nodes.value[index].color = '#52c41a'; break
        case '终点': nodes.value[index].color = '#722ed1'; break
        case '障碍物': nodes.value[index].color = '#ff4d4f'; break
        default: nodes.value[index].color = '#1890ff'; break
      }
    }
  })

  batchTypeDialogVisible.value = false
  ElMessage.success(`已为 ${selectedNodes.value.length} 个节点设置类型为 "${batchType.value}"`)
}

// 批量设置权重
function batchUpdateWeight() {
  if (selectedNodes.value.length === 0) {
    ElMessage.warning('请先选择要操作的节点')
    return
  }

  ElMessageBox.prompt('请输入权重值（0-10）', '批量设置权重', {
    inputValue: '1.0',
    inputValidator: (value) => {
      const num = parseFloat(value)
      if (isNaN(num) || num < 0 || num > 10) {
        return '请输入0-10之间的数字'
      }
      return true
    }
  }).then(({ value }) => {
    const weight = parseFloat(value)
    selectedNodes.value.forEach(node => {
      const index = nodes.value.findIndex(n => n.id === node.id)
      if (index !== -1) {
        nodes.value[index].weight = weight
      }
    })
    ElMessage.success(`已为 ${selectedNodes.value.length} 个节点设置权重为 ${weight}`)
  }).catch(() => {
    // 用户取消
  })
}

// 批量设置颜色
function batchUpdateColor() {
  if (selectedNodes.value.length === 0) {
    ElMessage.warning('请先选择要操作的节点')
    return
  }

  ElMessageBox.prompt('请输入颜色值（十六进制，如#1890ff）', '批量设置颜色', {
    inputValue: '#1890ff'
  }).then(({ value }) => {
    // 简单的颜色格式验证
    if (!/^#[0-9A-Fa-f]{6}$/.test(value) && !/^#[0-9A-Fa-f]{8}$/.test(value)) {
      ElMessage.warning('颜色格式不正确，请输入十六进制颜色值（如#1890ff）')
      return
    }

    selectedNodes.value.forEach(node => {
      const index = nodes.value.findIndex(n => n.id === node.id)
      if (index !== -1) {
        nodes.value[index].color = value
      }
    })
    ElMessage.success(`已为 ${selectedNodes.value.length} 个节点设置颜色为 ${value}`)
  }).catch(() => {
    // 用户取消
  })
}

// 重置表单
function resetForm() {
  if (nodeFormRef.value) {
    nodeFormRef.value.resetFields()
  }
}

// 取消编辑
function cancelEdit() {
  addNode()
}
</script>

<style scoped>
.node-edit-container {
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
  align-items: center;
  gap: 8px;
}

.content-area {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #f5f5f5;
}

.node-list {
  flex: 1;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid #e5e7eb;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
}

.edit-form {
  width: 400px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.form-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
}

.batch-panel {
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  margin-top: auto;
  background: #f8fafc;
}

.batch-panel h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a5568;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .content-area {
    flex-direction: column;
  }

  .edit-form {
    width: 100%;
    height: 50%;
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