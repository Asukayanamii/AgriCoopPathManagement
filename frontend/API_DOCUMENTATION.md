# 农业无人机路径规划系统 API 接口文档

## 概述

本文档描述了农业无人机路径规划系统前端与后端之间的 RESTful API 接口规范。所有 API 使用统一的请求/响应格式，并基于 token 进行身份认证。

### 基础信息
- **基础URL**: `http://localhost:8080` (开发环境)
- **API前缀**: `/api` (前端通过 Vite 代理转发时会自动移除 `/api` 前缀)
- **内容类型**: `application/json`
- **字符编码**: `UTF-8`

### 统一响应格式

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 实际返回的数据
  }
}
```

#### 错误响应
```json
{
  "code": 错误代码,
  "message": "错误描述信息",
  "data": null
}
```

### 状态码说明
| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权或 token 无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 认证与授权

所有需要认证的 API 必须在请求头中携带 `token` 字段。

### 请求头示例
```
Authorization: Bearer <token>
```

或者使用项目现有的格式：
```
token: <token>
```

前端已通过 axios 拦截器自动在所有请求中添加 `token` 头。

## 用户认证 API

### 1. 用户登录
**POST** `/user/login`

#### 请求参数
```json
{
  "username": "admin",
  "password": "123456"
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "avatar": "/avatars/default.png",
      "role": "admin",
      "permissions": ["*:*:*"]
    }
  }
}
```

### 2. 检查登录状态
**GET** `/user/loginornot`

#### 请求头
```
token: <token>
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isLogin": true,
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员"
    }
  }
}
```

#### 未登录响应
```json
{
  "code": 401,
  "message": "未登录",
  "data": null
}
```

## 地图管理 API

### 1. 获取地图列表
**GET** `/api/map/list`

#### 请求参数
无

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "农田区域A",
      "description": "主要作业区域A，面积约50亩",
      "url": "/maps/farm_a.jpg",
      "thumbnail": "/maps/thumbs/farm_a.jpg",
      "width": 1000,
      "height": 800,
      "scale": 2.5, // 米/像素
      "createdAt": "2026-04-01T10:00:00Z",
      "updatedAt": "2026-04-01T10:00:00Z"
    },
    {
      "id": 2,
      "name": "农田区域B",
      "description": "山地农田区域B",
      "url": "/maps/farm_b.jpg",
      "width": 1200,
      "height": 900,
      "scale": 3.0,
      "createdAt": "2026-04-02T14:30:00Z",
      "updatedAt": "2026-04-02T14:30:00Z"
    }
  ]
}
```

### 2. 获取地图详情
**GET** `/api/map/detail/:mapId`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| mapId | integer | 是 | 地图ID |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "农田区域A",
    "description": "主要作业区域A，面积约50亩",
    "url": "/maps/farm_a.jpg",
    "width": 1000,
    "height": 800,
    "scale": 2.5,
    "boundary": [
      {"x": 0, "y": 0},
      {"x": 1000, "y": 0},
      {"x": 1000, "y": 800},
      {"x": 0, "y": 800}
    ],
    "createdAt": "2026-04-01T10:00:00Z",
    "updatedAt": "2026-04-01T10:00:00Z"
  }
}
```

### 3. 获取地图节点数据
**GET** `/api/map/nodes/:mapId`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| mapId | integer | 是 | 地图ID |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "mapId": 1,
    "nodes": [
      {
        "id": 1,
        "mapId": 1,
        "x": 150,
        "y": 200,
        "type": "起点",
        "color": "red",
        "properties": {
          "name": "作业起点",
          "altitude": 120.5,
          "priority": "high"
        },
        "createdAt": "2026-04-10T09:30:00Z"
      },
      {
        "id": 2,
        "mapId": 1,
        "x": 850,
        "y": 650,
        "type": "终点",
        "color": "blue",
        "properties": {
          "name": "作业终点",
          "altitude": 118.2,
          "priority": "high"
        },
        "createdAt": "2026-04-10T09:32:00Z"
      },
      {
        "id": 3,
        "mapId": 1,
        "x": 450,
        "y": 350,
        "type": "障碍物",
        "color": "gray",
        "properties": {
          "name": "大树",
          "radius": 15,
          "height": 25
        },
        "createdAt": "2026-04-10T09:35:00Z"
      }
    ]
  }
}
```

### 4. 保存节点数据
**POST** `/api/map/nodes`

#### 请求参数
```json
{
  "mapId": 1,
  "nodes": [
    {
      "id": null, // 新增节点时为null，更新时传节点ID
      "x": 200,
      "y": 300,
      "type": "关注点",
      "color": "red",
      "properties": {
        "name": "水质监测点",
        "altitude": 115.8,
        "description": "农田水质监测点"
      }
    }
  ]
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "savedCount": 1,
    "nodes": [
      {
        "id": 4,
        "mapId": 1,
        "x": 200,
        "y": 300,
        "type": "关注点",
        "color": "red",
        "properties": {
          "name": "水质监测点",
          "altitude": 115.8,
          "description": "农田水质监测点"
        },
        "createdAt": "2026-04-14T10:45:00Z",
        "updatedAt": "2026-04-14T10:45:00Z"
      }
    ]
  }
}
```

### 5. 删除节点
**DELETE** `/api/map/nodes/:nodeId`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nodeId | integer | 是 | 节点ID |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "deleted": true
  }
}
```

## 路径规划算法 API

### 1. 执行算法计算
**POST** `/api/algorithm/calculate`

#### 请求参数
```json
{
  "algorithm": "a-star", // "a-star", "dijkstra", "bfs", "dfs", "agricultural"
  "parameters": {
    "heuristic": "euclidean", // "euclidean", "manhattan", "chebyshev"
    "weight": 1.0,
    "allowDiagonal": true,
    "obstaclePenalty": 2,
    "maxIterations": 1000,
    "coverageThreshold": 0.8,
    "turnPenalty": 1.5
  },
  "mapData": {
    "mapId": 1,
    "nodes": [
      {"x": 150, "y": 200, "type": "start"},
      {"x": 850, "y": 650, "type": "end"},
      {"x": 450, "y": 350, "type": "obstacle"}
    ],
    "gridSize": 20,
    "start": {"x": 150, "y": 200},
    "end": {"x": 850, "y": 650}
  }
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "algorithm": "a-star",
    "path": [
      [150, 200],
      [180, 220],
      [210, 240],
      // ... 更多路径点
      [850, 650]
    ],
    "distance": 1250.75, // 路径总长度（米）
    "timeMs": 45, // 计算耗时（毫秒）
    "coverage": 0.92, // 覆盖率（0-1）
    "nodesVisited": 156, // 访问节点数
    "energyCost": 2850, // 能量消耗估算
    "turns": 8, // 转弯次数
    "detailedMetrics": {
      "pathSmoothness": 0.87,
      "safetyScore": 0.95,
      "efficiencyScore": 0.88
    },
    "visualization": {
      "visitedNodes": [[x1,y1], [x2,y2], ...],
      "frontierNodes": [[x1,y1], [x2,y2], ...],
      "obstacles": [[x1,y1], [x2,y2], ...]
    }
  }
}
```

### 2. 获取算法对比数据
**GET** `/api/algorithm/comparison`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| mapId | integer | 是 | 地图ID |
| algorithms | string | 否 | 算法列表，逗号分隔，默认"a-star,dijkstra,agricultural" |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "mapId": 1,
    "comparisons": [
      {
        "algorithm": "a-star",
        "distance": 1250.75,
        "timeMs": 45,
        "coverage": 0.92,
        "nodesVisited": 156,
        "energyCost": 2850,
        "score": 0.88
      },
      {
        "algorithm": "dijkstra",
        "distance": 1280.20,
        "timeMs": 68,
        "coverage": 0.95,
        "nodesVisited": 210,
        "energyCost": 2950,
        "score": 0.82
      },
      {
        "algorithm": "agricultural",
        "distance": 1350.40,
        "timeMs": 120,
        "coverage": 0.98,
        "nodesVisited": 85,
        "energyCost": 2650,
        "score": 0.91
      }
    ],
    "recommendation": {
      "bestAlgorithm": "agricultural",
      "reason": "覆盖率最高且能量消耗最低",
      "scenario": "农田作业场景"
    }
  }
}
```

### 3. 执行聚类算法
**POST** `/api/algorithm/clustering`

#### 请求参数
```json
{
  "algorithm": "kmeans", // "kmeans", "dbscan", "hierarchical"
  "parameters": {
    "k": 5, // 聚类数量（kmeans用）
    "epsilon": 2.5, // 邻域半径（dbscan用）
    "minPoints": 3, // 最小点数（dbscan用）
    "distanceMetric": "euclidean"
  },
  "points": [
    {"x": 100, "y": 150, "weight": 1.0},
    {"x": 120, "y": 180, "weight": 1.2},
    {"x": 300, "y": 250, "weight": 0.8},
    // ... 更多点
  ]
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "algorithm": "kmeans",
    "clusters": [
      {
        "id": 0,
        "centroid": {"x": 110.5, "y": 165.2},
        "points": [
          {"x": 100, "y": 150, "pointId": 1},
          {"x": 120, "y": 180, "pointId": 2}
        ],
        "color": "#FF6B6B",
        "size": 45.8,
        "density": 0.85
      },
      {
        "id": 1,
        "centroid": {"x": 305.2, "y": 248.7},
        "points": [
          {"x": 300, "y": 250, "pointId": 3}
        ],
        "color": "#4ECDC4",
        "size": 32.1,
        "density": 0.45
      }
    ],
    "metrics": {
      "silhouetteScore": 0.75,
      "inertia": 1250.5,
      "clusterCount": 5
    }
  }
}
```

## 无人机参数配置 API

### 1. 获取无人机机型列表
**GET** `/api/drone/models`

#### 请求参数
无

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "DJI Agras T40",
      "type": "喷洒无人机",
      "image": "/drones/dji_agras_t40.png",
      "specs": {
        "maxSpeed": 10.0,
        "cruiseSpeed": 7.5,
        "maxAltitude": 200,
        "minAltitude": 5,
        "batteryCapacity": 30000,
        "sprayTankCapacity": 40,
        "weight": 26.3,
        "windResistance": 8
      },
      "description": "大疆农业无人机，适用于大面积农田作业"
    },
    {
      "id": 2,
      "name": "极飞 P100",
      "type": "植保无人机",
      "specs": {
        "maxSpeed": 12.0,
        "cruiseSpeed": 8.0,
        "maxAltitude": 250,
        "minAltitude": 3,
        "batteryCapacity": 28000,
        "sprayTankCapacity": 35,
        "weight": 24.8,
        "windResistance": 7
      }
    }
  ]
}
```

### 2. 获取当前配置
**GET** `/api/drone/config`

#### 请求参数
无

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "droneModelId": 1,
    "flightParams": {
      "cruiseSpeed": 7.5,
      "operatingAltitude": 30,
      "minAltitude": 10,
      "maxAltitude": 50,
      "takeoffAltitude": 20,
      "landingAltitude": 5
    },
    "operationParams": {
      "sprayRate": 1.2, // 升/亩
      "sprayWidth": 5.0, // 米
      "overlapRate": 0.15, // 重叠率
      "turnRadius": 3.0, // 转弯半径
      "sprayOnTurn": false // 转弯时是否喷洒
    },
    "safetyParams": {
      "lowBatteryThreshold": 20,
      "returnHomeBattery": 30,
      "maxWindSpeed": 6,
      "rainProtection": true,
      "obstacleAvoidance": true
    },
    "updatedAt": "2026-04-14T09:30:00Z"
  }
}
```

### 3. 保存配置
**POST** `/api/drone/config`

#### 请求参数
```json
{
  "droneModelId": 1,
  "flightParams": {
    "cruiseSpeed": 7.5,
    "operatingAltitude": 30,
    "minAltitude": 10,
    "maxAltitude": 50,
    "takeoffAltitude": 20,
    "landingAltitude": 5
  },
  "operationParams": {
    "sprayRate": 1.2,
    "sprayWidth": 5.0,
    "overlapRate": 0.15,
    "turnRadius": 3.0,
    "sprayOnTurn": false
  },
  "safetyParams": {
    "lowBatteryThreshold": 20,
    "returnHomeBattery": 30,
    "maxWindSpeed": 6,
    "rainProtection": true,
    "obstacleAvoidance": true
  }
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "saved": true,
    "updatedAt": "2026-04-14T10:50:00Z"
  }
}
```

### 4. 获取配置模板
**GET** `/api/drone/config/templates`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| cropType | string | 否 | 作物类型，如"水稻","小麦","玉米" |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "水稻标准作业模板",
      "cropType": "水稻",
      "description": "适用于水稻田的标准喷洒作业参数",
      "config": {
        "flightParams": { /* 具体参数 */ },
        "operationParams": { /* 具体参数 */ }
      }
    },
    {
      "id": 2,
      "name": "果树精细作业模板",
      "cropType": "果树",
      "description": "适用于果园的精细喷洒作业",
      "config": {
        "flightParams": { /* 具体参数 */ },
        "operationParams": { /* 具体参数 */ }
      }
    }
  ]
}
```

## 工作流管理 API

工作流管理API用于支持前端渐进式数据流架构，管理8个工作流步骤的状态、验证和数据依赖关系。

### 1. 获取工作流状态
**GET** `/api/workflow/status`

#### 请求参数
无

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "currentStep": "map-selection",
    "completedSteps": ["map-selection", "node-marking"],
    "skippedSteps": [],
    "stepOrder": [
      "map-selection",
      "node-marking", 
      "drone-config",
      "algorithm-selection",
      "parameter-config",
      "calculation",
      "simulation-preview",
      "realtime-monitor"
    ],
    "stepLabels": {
      "map-selection": "地图选择",
      "node-marking": "节点标记",
      "drone-config": "无人机配置",
      "algorithm-selection": "算法选择",
      "parameter-config": "参数配置",
      "calculation": "算法计算",
      "simulation-preview": "仿真预览",
      "realtime-monitor": "实时监控"
    },
    "progressPercentage": 25,
    "canProceed": true
  }
}
```

### 2. 验证步骤数据依赖
**GET** `/api/workflow/validate/:step`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| step | string | 是 | 步骤标识符 |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "step": "algorithm-selection",
    "valid": true,
    "missing": [],
    "message": "步骤验证通过",
    "dependencies": {
      "requiredData": ["地图数据", "节点数据"],
      "availableData": ["地图ID: 1", "节点数: 5"],
      "ready": true
    }
  }
}
```

### 3. 切换到指定步骤
**POST** `/api/workflow/goto-step`

#### 请求参数
```json
{
  "step": "simulation-preview"
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "previousStep": "calculation",
    "currentStep": "simulation-preview",
    "completedSteps": ["map-selection", "node-marking", "algorithm-selection", "parameter-config", "calculation"],
    "canProceed": true
  }
}
```

### 4. 标记步骤为完成
**POST** `/api/workflow/complete-step`

#### 请求参数
```json
{
  "step": "calculation",
  "resultData": {
    "algorithm": "a-star",
    "pathLength": 1250.75,
    "completionTime": "2026-04-14T10:45:00Z"
  }
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "step": "calculation",
    "completed": true,
    "completedSteps": ["map-selection", "node-marking", "algorithm-selection", "parameter-config", "calculation"],
    "progressPercentage": 63
  }
}
```

### 5. 跳过步骤
**POST** `/api/workflow/skip-step`

#### 请求参数
```json
{
  "step": "drone-config",
  "reason": "使用默认配置"
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "step": "drone-config",
    "skipped": true,
    "skippedSteps": ["drone-config"],
    "nextStep": "algorithm-selection"
  }
}
```

### 6. 重置工作流
**POST** `/api/workflow/reset`

#### 请求参数
无

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reset": true,
    "currentStep": "map-selection",
    "completedSteps": [],
    "skippedSteps": [],
    "progressPercentage": 0
  }
}
```

### 7. 获取步骤数据依赖图
**GET** `/api/workflow/dependencies`

#### 请求参数
无

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dependencies": [
      {
        "step": "map-selection",
        "requires": [],
        "produces": ["地图数据"],
        "optional": false
      },
      {
        "step": "node-marking",
        "requires": ["地图数据"],
        "produces": ["节点数据"],
        "optional": false
      },
      {
        "step": "drone-config",
        "requires": [],
        "produces": ["无人机配置"],
        "optional": true
      },
      {
        "step": "algorithm-selection",
        "requires": ["节点数据"],
        "produces": ["算法类型"],
        "optional": false
      },
      {
        "step": "parameter-config",
        "requires": ["算法类型"],
        "produces": ["算法参数"],
        "optional": false
      },
      {
        "step": "calculation",
        "requires": ["节点数据", "算法类型", "算法参数"],
        "produces": ["算法结果"],
        "optional": false
      },
      {
        "step": "simulation-preview",
        "requires": ["算法结果"],
        "produces": ["仿真结果"],
        "optional": false
      },
      {
        "step": "realtime-monitor",
        "requires": ["算法结果"],
        "produces": ["监控数据"],
        "optional": false
      }
    ]
  }
}
```

### 8. 检查数据完整性
**GET** `/api/workflow/integrity`

#### 请求参数
无

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "integrity": {
      "mapData": {
        "available": true,
        "mapId": 1,
        "nodeCount": 5,
        "hasStartEnd": true
      },
      "algorithmData": {
        "available": true,
        "algorithm": "a-star",
        "hasResult": true,
        "resultTimestamp": "2026-04-14T10:45:00Z"
      },
      "droneConfig": {
        "available": false,
        "usingDefault": true
      },
      "simulationData": {
        "available": true,
        "simulationId": "sim_001",
        "status": "completed"
      }
    },
    "readyForSteps": {
      "map-selection": true,
      "node-marking": true,
      "drone-config": true,
      "algorithm-selection": true,
      "parameter-config": true,
      "calculation": true,
      "simulation-preview": true,
      "realtime-monitor": true
    }
  }
}
```

## 仿真与监控 API

### 1. 开始仿真
**POST** `/api/simulation/start`

#### 请求参数
```json
{
  "mapId": 1,
  "algorithm": "agricultural",
  "droneConfigId": 1,
  "simulationParams": {
    "speedFactor": 1.0, // 仿真速度倍率
    "includeWeather": true,
    "randomEvents": false,
    "maxDuration": 3600 // 最大仿真时长（秒）
  }
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "simulationId": "sim_202604141050_001",
    "status": "running",
    "estimatedDuration": 2850, // 预计耗时（秒）
    "startTime": "2026-04-14T10:50:00Z"
  }
}
```

### 2. 获取仿真状态
**GET** `/api/simulation/status/:simulationId`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| simulationId | string | 是 | 仿真ID |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "simulationId": "sim_202604141050_001",
    "status": "running", // "pending", "running", "paused", "completed", "failed"
    "progress": 0.45, // 进度 0-1
    "currentState": {
      "position": {"x": 450, "y": 320},
      "altitude": 30.5,
      "speed": 7.2,
      "heading": 125.8,
      "battery": 78,
      "sprayRemaining": 65
    },
    "elapsedTime": 1280, // 已运行时间（秒）
    "estimatedRemaining": 1570, // 预计剩余时间（秒）
    "metrics": {
      "areaCovered": 25.8, // 已覆盖面积（亩）
      "sprayUsed": 18.5, // 已使用药液（升）
      "energyUsed": 12500, // 已消耗能量
      "turnsCompleted": 12
    }
  }
}
```

### 3. 控制仿真
**POST** `/api/simulation/control`

#### 请求参数
```json
{
  "simulationId": "sim_202604141050_001",
  "action": "pause" // "pause", "resume", "stop", "restart"
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "simulationId": "sim_202604141050_001",
    "status": "paused",
    "message": "仿真已暂停"
  }
}
```

### 4. 获取仿真结果
**GET** `/api/simulation/result/:simulationId`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| simulationId | string | 是 | 仿真ID |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "simulationId": "sim_202604141050_001",
    "status": "completed",
    "startTime": "2026-04-14T10:50:00Z",
    "endTime": "2026-04-14T11:35:00Z",
    "totalDuration": 2700,
    "summary": {
      "totalArea": 58.5, // 总面积（亩）
      "coveredArea": 56.2, // 覆盖面积（亩）
      "coverageRate": 0.96,
      "totalSpray": 67.4, // 总用药量（升）
      "energyConsumed": 24500,
      "averageSpeed": 6.8,
      "turns": 24
    },
    "detailedMetrics": {
      "efficiency": 0.88,
      "uniformity": 0.92,
      "safetyScore": 0.95,
      "costPerAcre": 12.5
    },
    "pathData": [
      {"time": 0, "x": 150, "y": 200, "altitude": 20},
      {"time": 10, "x": 165, "y": 215, "altitude": 25},
      // ... 更多轨迹点
    ],
    "events": [
      {
        "time": 1250,
        "type": "turn",
        "description": "第8次转弯",
        "position": {"x": 520, "y": 380}
      },
      {
        "time": 1850,
        "type": "refill",
        "description": "药液补充",
        "position": {"x": 320, "y": 280}
      }
    ]
  }
}
```

### 5. 获取实时监控数据
**GET** `/api/monitor/realtime`

#### 请求参数
无

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "timestamp": "2026-04-14T10:55:30Z",
    "droneStatus": {
      "position": {"x": 520, "y": 380},
      "altitude": 28.5,
      "speed": 6.8,
      "heading": 142.5,
      "batteryLevel": 72,
      "signalStrength": 95,
      "gpsSatellites": 12
    },
    "operationStatus": {
      "progress": 0.48,
      "areaCovered": 28.1,
      "sprayRemaining": 58,
      "currentTask": "喷洒作业",
      "nextWaypoint": {"x": 580, "y": 420, "distance": 85.2}
    },
    "environment": {
      "temperature": 25.8,
      "humidity": 65,
      "windSpeed": 3.2,
      "windDirection": 120,
      "visibility": "good"
    },
    "alerts": [
      {
        "level": "warning",
        "type": "battery",
        "message": "电量低于30%，建议返航",
        "time": "2026-04-14T10:55:00Z"
      },
      {
        "level": "info",
        "type": "boundary",
        "message": "接近作业边界",
        "time": "2026-04-14T10:54:30Z"
      }
    ],
    "systemHealth": {
      "cpuUsage": 45,
      "memoryUsage": 62,
      "diskUsage": 38,
      "networkStatus": "connected"
    }
  }
}
```

### 6. 获取历史监控数据
**GET** `/api/monitor/history`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| startTime | string | 是 | 开始时间，ISO格式 |
| endTime | string | 是 | 结束时间，ISO格式 |
| interval | integer | 否 | 数据间隔（秒），默认60 |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "timeRange": {
      "start": "2026-04-14T09:00:00Z",
      "end": "2026-04-14T10:00:00Z"
    },
    "dataPoints": [
      {
        "timestamp": "2026-04-14T09:00:00Z",
        "position": {"x": 150, "y": 200},
        "altitude": 20.0,
        "speed": 0.0,
        "battery": 100,
        "progress": 0.0
      },
      {
        "timestamp": "2026-04-14T09:05:00Z",
        "position": {"x": 210, "y": 240},
        "altitude": 25.0,
        "speed": 6.5,
        "battery": 98,
        "progress": 0.08
      },
      // ... 更多数据点
    ],
    "summary": {
      "totalDistance": 2850.5,
      "averageSpeed": 6.2,
      "energyUsed": 12500,
      "maxAltitude": 32.5,
      "minBattery": 95
    }
  }
}
```

## 系统管理 API

### 1. 获取用户列表
**GET** `/api/system/users`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | integer | 否 | 页码，默认1 |
| pageSize | integer | 否 | 每页条数，默认10 |
| search | string | 否 | 搜索关键词（用户名/昵称） |
| status | string | 否 | 状态筛选，"active"或"inactive" |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 25,
    "page": 1,
    "pageSize": 10,
    "users": [
      {
        "id": 1,
        "username": "admin",
        "nickname": "管理员",
        "email": "admin@example.com",
        "phone": "13800138000",
        "role": "admin",
        "avatar": "/avatars/admin.jpg",
        "status": "active",
        "lastLogin": "2026-04-14T09:30:00Z",
        "createdAt": "2026-01-01T00:00:00Z"
      },
      {
        "id": 2,
        "username": "operator1",
        "nickname": "操作员1",
        "email": "operator1@example.com",
        "role": "operator",
        "status": "active",
        "lastLogin": "2026-04-14T08:45:00Z",
        "createdAt": "2026-02-15T10:30:00Z"
      }
    ]
  }
}
```

### 2. 创建用户
**POST** `/api/system/users`

#### 请求参数
```json
{
  "username": "newuser",
  "password": "password123",
  "nickname": "新用户",
  "email": "newuser@example.com",
  "phone": "13800138001",
  "role": "operator"
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 26,
    "username": "newuser",
    "nickname": "新用户",
    "email": "newuser@example.com",
    "role": "operator",
    "status": "active",
    "createdAt": "2026-04-14T11:00:00Z"
  }
}
```

### 3. 更新用户
**PUT** `/api/system/users/:userId`

#### 请求参数
```json
{
  "nickname": "更新后的昵称",
  "email": "updated@example.com",
  "phone": "13800138002",
  "role": "operator",
  "status": "active"
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "username": "operator1",
    "nickname": "更新后的昵称",
    "email": "updated@example.com",
    "phone": "13800138002",
    "role": "operator",
    "status": "active",
    "updatedAt": "2026-04-14T11:05:00Z"
  }
}
```

### 4. 删除用户
**DELETE** `/api/system/users/:userId`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | integer | 是 | 用户ID |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "deleted": true,
    "userId": 3
  }
}
```

### 5. 重置用户密码
**POST** `/api/system/users/:userId/reset-password`

#### 请求参数
```json
{
  "newPassword": "newpassword123"
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reset": true,
    "userId": 2
  }
}
```

### 6. 获取系统配置
**GET** `/api/system/config`

#### 请求参数
无

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "general": {
      "siteName": "农业无人机路径规划系统",
      "siteLogo": "/logo.png",
      "favicon": "/favicon.ico",
      "defaultLanguage": "zh-CN",
      "timezone": "Asia/Shanghai",
      "dateFormat": "YYYY-MM-DD",
      "timeFormat": "HH:mm:ss"
    },
    "map": {
      "defaultMapId": 1,
      "defaultZoom": 1.0,
      "minZoom": 0.5,
      "maxZoom": 3.0,
      "nodeColors": {
        "start": "#10B981",
        "end": "#EF4444",
        "obstacle": "#9CA3AF",
        "waypoint": "#3B82F6"
      }
    },
    "algorithm": {
      "defaultAlgorithm": "agricultural",
      "defaultHeuristic": "euclidean",
      "defaultWeight": 1.0,
      "calculationTimeout": 30000
    },
    "monitoring": {
      "updateInterval": 5000,
      "historyRetentionDays": 30,
      "alertRetentionDays": 90,
      "autoRefresh": true
    },
    "updatedAt": "2026-04-14T09:00:00Z"
  }
}
```

### 7. 更新系统配置
**POST** `/api/system/config`

#### 请求参数
```json
{
  "general": {
    "siteName": "农业无人机路径规划系统",
    "defaultLanguage": "zh-CN"
  },
  "map": {
    "defaultMapId": 1,
    "defaultZoom": 1.0
  },
  "algorithm": {
    "defaultAlgorithm": "agricultural",
    "calculationTimeout": 30000
  }
}
```

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "updated": true,
    "updatedAt": "2026-04-14T11:10:00Z"
  }
}
```

## 数据导入导出 API

### 1. 导出节点数据
**GET** `/api/export/nodes`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| mapId | integer | 是 | 地图ID |
| format | string | 否 | 导出格式，"json"或"csv"，默认"json" |

#### 成功响应（JSON格式）
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "filename": "nodes_map1_20260414.json",
    "content": "base64编码的文件内容",
    "size": 2048,
    "downloadUrl": "/api/download/nodes_map1_20260414.json"
  }
}
```

### 2. 导入节点数据
**POST** `/api/import/nodes`

#### 请求参数（multipart/form-data）
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| mapId | integer | 是 | 地图ID |
| file | file | 是 | 上传的文件（JSON或CSV格式） |
| overwrite | boolean | 否 | 是否覆盖现有节点，默认false |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "importedCount": 15,
    "skippedCount": 2,
    "errors": [],
    "summary": "成功导入15个节点，跳过2个无效节点"
  }
}
```

### 3. 导出仿真结果
**GET** `/api/export/simulation/:simulationId`

#### 请求参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| simulationId | string | 是 | 仿真ID |
| format | string | 否 | 导出格式，"json"或"csv"，默认"json" |

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "filename": "simulation_result_sim001_20260414.json",
    "content": "base64编码的文件内容",
    "size": 4096,
    "downloadUrl": "/api/download/simulation_result_sim001_20260414.json"
  }
}
```

## WebSocket 实时通信

### 连接地址
```
ws://localhost:8080/ws/monitor
```

### 连接参数
连接时需要携带 token：
```
ws://localhost:8080/ws/monitor?token=<token>
```

### 消息格式

#### 客户端发送
```json
{
  "type": "subscribe",
  "channels": ["realtime", "alerts"]
}
```

#### 服务器推送
1. **实时监控数据**
```json
{
  "type": "realtime",
  "data": {
    "timestamp": "2026-04-14T11:00:00Z",
    "position": {"x": 520, "y": 380},
    "battery": 72,
    "progress": 0.48
  }
}
```

2. **告警通知**
```json
{
  "type": "alert",
  "data": {
    "level": "warning",
    "message": "电量低于30%，建议返航",
    "time": "2026-04-14T11:00:00Z"
  }
}
```

3. **系统状态**
```json
{
  "type": "system",
  "data": {
    "cpuUsage": 45,
    "memoryUsage": 62,
    "connectedClients": 3
  }
}
```

## 错误处理

### 常见错误码
| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 1001 | 参数验证失败 | 检查请求参数格式和必填项 |
| 1002 | 资源不存在 | 检查资源ID是否正确 |
| 1003 | 权限不足 | 检查用户角色和权限 |
| 1004 | 操作冲突 | 资源可能被其他操作锁定 |
| 1005 | 数据格式错误 | 检查上传文件的格式 |
| 2001 | 算法计算超时 | 增加超时时间或简化参数 |
| 2002 | 地图加载失败 | 检查地图文件是否存在 |
| 2003 | 无人机通信失败 | 检查无人机连接状态 |
| 3001 | 数据库连接失败 | 检查数据库服务状态 |
| 3002 | 文件系统错误 | 检查磁盘空间和权限 |

### 错误响应示例
```json
{
  "code": 1001,
  "message": "参数验证失败: 用户名不能为空",
  "data": null,
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为空"
    }
  ]
}
```

## 附录

### 1. 数据结构说明

#### 坐标点
```typescript
interface Point {
  x: number;  // 横坐标（像素）
  y: number;  // 纵坐标（像素）
}
```

#### 地理坐标
```typescript
interface GeoPoint {
  latitude: number;  // 纬度
  longitude: number; // 经度
  altitude: number;  // 海拔高度（米）
}
```

#### 节点属性
```typescript
interface Node {
  id?: number;
  mapId: number;
  x: number;
  y: number;
  type: 'start' | 'end' | 'obstacle' | 'waypoint' | 'interest';
  color: string;
  properties: Record<string, any>;
  createdAt?: string;
  updatedAt?: string;
}
```

### 2. 单位说明
- 距离：米（m）
- 速度：米/秒（m/s）
- 面积：亩（1亩≈666.67平方米）
- 药液量：升（L）
- 能量：焦耳（J）
- 时间：秒（s）或毫秒（ms）

### 3. 坐标系
- 地图坐标系：以像素为单位，原点在左上角
- 地理坐标系：WGS84标准（纬度、经度、海拔）
- 转换公式：`实际距离 = 像素距离 × 比例尺`

### 4. 更新日志
| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-04-14 | 初始版本，包含所有基础API |

---

**文档维护**: 前端开发团队  
**最后更新**: 2026-04-14 (新增工作流管理API)  
**联系方式**: dev@agricoop.example.com