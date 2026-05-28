/**
 * 数据服务 - 处理演示模式和真实模式的数据切换
 */

// 当前模式：true为演示模式（使用模拟数据），false为真实模式（调用API）
let demoMode = true

// 切换模式
export function toggleDemoMode() {
  demoMode = !demoMode
  return demoMode
}

// 获取当前模式
export function getCurrentMode() {
  return demoMode
}

// 设置模式
export function setDemoMode(isDemo) {
  demoMode = isDemo
}

/**
 * 通用数据获取函数
 * @param {Function} apiCall - API调用函数
 * @param {*} mockData - 模拟数据
 * @param {...any} args - API参数
 * @returns {Promise} 数据Promise
 */
export async function fetchData(apiCall, mockData, ...args) {
  if (demoMode) {
    // 演示模式：返回模拟数据，模拟网络延迟
    return new Promise(resolve => {
      setTimeout(() => {
        resolve({
          code: 200,
          message: 'success',
          data: typeof mockData === 'function' ? mockData(...args) : mockData
        })
      }, 300) // 300ms延迟模拟网络请求
    })
  } else {
    // 真实模式：调用API
    try {
      return await apiCall(...args)
    } catch (error) {
      console.error('API调用失败:', error)
      throw error
    }
  }
}

/**
 * 地图数据服务
 */
export const mapService = {
  async getMapList() {
    const mockData = [
      { id: 1, name: '农田区域A', url: 'https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=1000&h=800&fit=crop', width: 1000, height: 800 },
      { id: 2, name: '农田区域B', url: 'https://images.unsplash.com/photo-1505253668822-42074d58a7c6?w=1200&h=900&fit=crop', width: 1200, height: 900 },
      { id: 3, name: '果园区域C', url: 'https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800&h=600&fit=crop', width: 800, height: 600 }
    ]
    return fetchData(() => import('@/api/map').then(m => m.getMapList()), mockData)
  },

  async getMapNodes(mapId) {
    const mockData = [
      { id: 1, x: 150, y: 200, label: '水源点', type: '关注点', color: '#1890ff' },
      { id: 2, x: 400, y: 300, label: '障碍物1', type: '障碍物', color: '#ff4d4f' },
      { id: 3, x: 600, y: 150, label: '起点', type: '起点', color: '#52c41a' },
      { id: 4, x: 800, y: 450, label: '终点', type: '终点', color: '#722ed1' },
      { id: 5, x: 300, y: 500, label: '施肥点', type: '关注点', color: '#1890ff' }
    ]
    return fetchData(() => import('@/api/map').then(m => m.getMapNodes(mapId)), mockData)
  },

  async saveNodes(data) {
    const mockData = { success: true, message: '节点保存成功' }
    return fetchData(() => import('@/api/map').then(m => m.saveNodes(data)), mockData)
  }
}

/**
 * 算法数据服务
 */
export const algorithmService = {
  async calculate(algorithmType, parameters, nodes) {
    const mockData = {
      path: [[100, 100], [200, 150], [300, 200], [400, 250]],
      distance: 450.75,
      timeMs: 120,
      coverage: 0.92,
      nodesVisited: 15
    }
    return fetchData(() => import('@/api/algorithm').then(m => m.calculateAlgorithm({ algorithmType, parameters, nodes })), mockData)
  },

  async getComparison() {
    const mockData = {
      algorithms: [
        { name: 'A*算法', distance: 450.75, timeMs: 120, coverage: 0.92, efficiency: 0.85 },
        { name: 'Dijkstra算法', distance: 480.20, timeMs: 180, coverage: 0.88, efficiency: 0.78 },
        { name: 'Boustrophedon算法', distance: 520.10, timeMs: 95, coverage: 0.95, efficiency: 0.91 },
        { name: '遗传算法', distance: 460.30, timeMs: 320, coverage: 0.90, efficiency: 0.82 }
      ]
    }
    return fetchData(() => import('@/api/algorithm').then(m => m.getAlgorithmComparison()), mockData)
  },

  async calculateClustering(nodes, parameters) {
    const mockData = {
      clusters: [
        { id: 1, nodes: [1, 2, 3], centroid: [150, 250], color: '#ff6b6b' },
        { id: 2, nodes: [4, 5, 6], centroid: [450, 350], color: '#4ecdc4' },
        { id: 3, nodes: [7, 8, 9], centroid: [750, 150], color: '#45b7d1' }
      ],
      stats: {
        totalClusters: 3,
        averageClusterSize: 3.0,
        silhouetteScore: 0.78
      }
    }
    return fetchData(() => import('@/api/algorithm').then(m => m.calculateClustering({ nodes, parameters })), mockData)
  },

  async getHistory() {
    const mockData = [
      { id: 1, algorithm: 'A*算法', timestamp: '2026-04-14 10:30:00', result: '成功', distance: 450.75 },
      { id: 2, algorithm: 'Dijkstra算法', timestamp: '2026-04-14 09:15:00', result: '成功', distance: 480.20 },
      { id: 3, algorithm: 'Boustrophedon算法', timestamp: '2026-04-14 08:45:00', result: '成功', distance: 520.10 }
    ]
    return fetchData(() => import('@/api/algorithm').then(m => m.getAlgorithmHistory()), mockData)
  },

  async getTemplates() {
    const mockData = [
      { id: 1, name: '基础农田作业', algorithm: 'Boustrophedon算法', parameters: { rowSpacing: 5, turningRadius: 10 } },
      { id: 2, name: '障碍物避让', algorithm: 'A*算法', parameters: { heuristic: 'euclidean', weight: 1.2 } },
      { id: 3, name: '快速覆盖', algorithm: '遗传算法', parameters: { populationSize: 50, generations: 100 } }
    ]
    return fetchData(() => import('@/api/algorithm').then(m => m.getAlgorithmTemplates()), mockData)
  }
}

/**
 * 无人机数据服务
 */
export const droneService = {
  async getConfig() {
    const mockData = {
      droneModel: 'DJI Agras T40',
      speed: 8, // m/s
      altitude: 15, // meters
      sprayRate: 2.5, // L/min
      batteryCapacity: 30000, // mAh
      maxFlightTime: 30, // minutes
      payloadCapacity: 40 // kg
    }
    return fetchData(() => import('@/api/drone').then(m => m.getDroneConfig()), mockData)
  },

  async saveConfig(data) {
    const mockData = { success: true, message: '配置保存成功' }
    return fetchData(() => import('@/api/drone').then(m => m.saveDroneConfig(data)), mockData)
  },

  async getModels() {
    const mockData = [
      { id: 1, name: 'DJI Agras T40', description: '大疆农业无人机，40L喷洒箱', maxSpeed: 10, maxAltitude: 30 },
      { id: 2, name: 'XAIRCRAFT P30', description: '极飞农业无人机，30L喷洒箱', maxSpeed: 12, maxAltitude: 25 },
      { id: 3, name: 'Hanhe DJI T20', description: '汉和DJI合作款，20L喷洒箱', maxSpeed: 9, maxAltitude: 20 }
    ]
    return fetchData(() => import('@/api/drone').then(m => m.getDroneModels()), mockData)
  }
}

/**
 * 仿真数据服务
 */
export const simulationService = {
  async start(data) {
    const mockData = {
      simulationId: 'sim_' + Date.now(),
      status: 'running',
      startTime: new Date().toISOString(),
      estimatedCompletion: new Date(Date.now() + 30000).toISOString() // 30秒后
    }
    return fetchData(() => import('@/api/simulation').then(m => m.startSimulation(data)), mockData)
  },

  async getStatus() {
    const mockData = {
      status: 'running',
      progress: 0.65,
      elapsedTime: 120, // seconds
      remainingTime: 65 // seconds
    }
    return fetchData(() => import('@/api/simulation').then(m => m.getSimulationStatus()), mockData)
  },

  async stop() {
    const mockData = {
      success: true,
      message: '仿真已停止',
      finalProgress: 0.72
    }
    return fetchData(() => import('@/api/simulation').then(m => m.stopSimulation()), mockData)
  },

  async getResult() {
    const mockData = {
      path: [[100, 100], [200, 150], [300, 200], [400, 250], [500, 300]],
      distance: 550.25,
      flightTime: 180, // seconds
      coverage: 0.88,
      energyConsumption: 4500 // Wh
    }
    return fetchData(() => import('@/api/simulation').then(m => m.getSimulationResult()), mockData)
  },

  async getRealtimeMonitor() {
    const mockData = {
      timestamp: new Date().toISOString(),
      dronePosition: { x: 250, y: 350 },
      batteryLevel: 78, // percentage
      progress: 0.65,
      alerts: [],
      currentSpeed: 7.5,
      currentAltitude: 14.2
    }
    return fetchData(() => import('@/api/simulation').then(m => m.getRealtimeMonitorData()), mockData)
  }
}

/**
 * 系统数据服务
 */
export const systemService = {
  async getUsers() {
    const mockData = [
      { id: 1, username: 'admin', role: '管理员', email: 'admin@example.com', status: 'active' },
      { id: 2, username: 'operator1', role: '操作员', email: 'operator1@example.com', status: 'active' },
      { id: 3, username: 'viewer1', role: '查看者', email: 'viewer1@example.com', status: 'inactive' }
    ]
    return fetchData(() => import('@/api/system').then(m => m.getUsers()), mockData)
  },

  async createUser(data) {
    const mockData = { success: true, message: '用户创建成功', userId: Date.now() }
    return fetchData(() => import('@/api/system').then(m => m.createUser(data)), mockData)
  },

  async updateUser(userId, data) {
    const mockData = { success: true, message: '用户更新成功' }
    return fetchData(() => import('@/api/system').then(m => m.updateUser(userId, data)), mockData)
  },

  async deleteUser(userId) {
    const mockData = { success: true, message: '用户删除成功' }
    return fetchData(() => import('@/api/system').then(m => m.deleteUser(userId)), mockData)
  },

  async getSystemConfig() {
    const mockData = {
      systemName: '农业无人机路径规划系统',
      apiEndpoint: 'http://localhost:8080/api',
      mapTileServer: 'https://tile.example.com',
      simulationInterval: 5, // seconds
      maxNodesPerMap: 100,
      defaultAlgorithm: 'a-star'
    }
    return fetchData(() => import('@/api/system').then(m => m.getSystemConfig()), mockData)
  },

  async updateSystemConfig(data) {
    const mockData = { success: true, message: '系统配置更新成功' }
    return fetchData(() => import('@/api/system').then(m => m.updateSystemConfig(data)), mockData)
  }
}