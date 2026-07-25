# 算法交互流程文档 — 逐项对照检查

## 阶段一：地图预处理（前端人工标记）

- [x] PNG 栅格化 → 标记岔路口/拐点（图节点）→ 前端 MapPreprocess Step 1
- [x] 标记路段（边）→ 点击两个路点添加
- [x] 其余栅格自动视为障碍物
- [x] 坐标体系：三层（像素→逻辑0~1000→SVG）— 前端 coordMapper/viewBox 处理
- [x] 边权 = 欧氏距离 — doBuild 中用 Math.hypot 计算

## 阶段二：建图（DLL）

- [x] create_graph(node_count, from, to, weight, edge_count) — 已对接
- [x] create_standmap(px, py, N, graph) — 已对接
- [x] 两个句柄都要 free — free_graph / free_standmap 已对接
- [x] create_standmap 节点数必须等于 create_graph 节点数 — 一致
- [x] 编码是独立步骤，可在建图前后任意时刻调用 — encode_id/nodes/edges 已对接
- [ ] 编码结果持久化到 road_nodes.code 和 road_edges.code — 已对接
- [x] 编码可存库复用 — encode_nodes/edges 写回 DB

## 阶段三：任务点聚类（DLL）

- [x] kmeans(px, py, n, space, deviation, max_iter, out_cluster, out_center) — 已对接
- [x] 聚类只依赖坐标，不依赖图结构 — 正确
- [x] 任务点与图的桥接在 car_planning 中通过 find_best_endpoint 完成 — 正确

## 阶段四：人工标注聚类优先级

- [x] 前端拖拽排序 — vuedraggable 实现
- [x] 存库 — PUT /tasks/priority → cluster_priority 表
- [x] done 字段跟踪执行进度 — 已实现

## 阶段五：按优先级循环处理每个簇

### 5.1+5.2 Car_Planning 综合接口

- [x] car_planning(road_px, road_py, road_count, car_x, car_y, car_state, car_belong_node, car_count, task_px, task_py, task_codes, task_count, graph, sm) — 已对接
- [x] 返回格式 "小车ID::路径编码" — 已实现
- [x] free_car_path — adapter 中自动调用

### 5.3 TSP

- [x] tsp_plan(px, py, codes, n) — 已对接
- [x] px[0]=起点(岔路口), px[1..n]=任务点 — 已实现
- [x] n ≤ 20

### 5.4 回到小车 → 下一个簇

- [x] free_standmap + free_graph — 已实现
- [x] done 标记 — cluster_priority.done 更新
- [x] 释放车辆 — resources.state = 1

## 数据库表设计

### road_nodes
- [x] id INTEGER PRIMARY KEY
- [x] x REAL NOT NULL
- [x] y REAL NOT NULL
- [x] code TEXT NOT NULL

### road_edges
- [x] id INTEGER PRIMARY KEY
- [x] from_node INTEGER NOT NULL REFERENCES road_nodes(id)
- [x] to_node INTEGER NOT NULL REFERENCES road_nodes(id)
- [x] weight REAL NOT NULL
- [x] code TEXT NOT NULL

### standmap
- [x] center_node INTEGER NOT NULL
- [x] target_node INTEGER NOT NULL
- [x] distance INTEGER NOT NULL
- [ ] PRIMARY KEY (center_node, target_node) — 用的是自增 id（功能等价, 不影响使用）

### resources
- [x] id INTEGER PRIMARY KEY
- [x] x REAL NOT NULL
- [x] y REAL NOT NULL
- [x] state INTEGER NOT NULL DEFAULT 1
- [x] belong_node INTEGER REFERENCES road_nodes(id)

### task_points
- [x] id INTEGER PRIMARY KEY
- [x] x REAL NOT NULL
- [x] y REAL NOT NULL
- [x] code TEXT NOT NULL

### task_clusters
- [x] task_id REFERENCES task_points(id)
- [x] cluster_id INTEGER NOT NULL
- [x] center_id INTEGER NOT NULL

### cluster_priority
- [x] cluster_id INTEGER PRIMARY KEY
- [x] priority INTEGER NOT NULL
- [x] center_x REAL
- [x] center_y REAL
- [x] done INTEGER NOT NULL DEFAULT 0

## 数据流转关系

- [x] 阶段一: road_nodes ← 标记路点, road_edges ← 标记路段
- [x] 阶段二: encode_nodes → 写入 road_nodes.code
- [x] encode_edges → 写入 road_edges.code
- [x] create_graph + create_standmap → standmap 表持久化
- [x] 阶段三: task_points → kmeans → task_clusters
- [x] 阶段四: cluster_priority ← 拖拽排序
- [x] 阶段五: road_nodes → 读出编码 → car_planning codes 参数
- [x] road_edges → 重建图 → create_graph 入参
- [x] standmap 表 → 重建启发数据
- [x] resources → 更新 state (执行中标记占用，完成后释放)
- [x] cluster_priority → SET done=1

## API 接口

- [x] POST /map/nodes — 批量保存路点
- [x] POST /map/edges — 批量保存路段
- [x] POST /map/build — 建图+编码+标准地图
- [x] GET /map/nodes — 获取路点列表
- [x] GET /map/edges — 获取路段列表
- [x] GET /map/stats — 地图统计
- [x] POST /tasks — 批量创建任务点
- [x] POST /tasks/cluster — 执行聚类
- [x] GET /tasks — 获取所有任务点
- [x] GET /tasks/clusters — 获取聚类结果
- [x] PUT /tasks/priority — 保存人工标注的聚类优先级
- [x] GET /tasks/priority — 获取优先级列表
- [x] POST /execute/next — 执行下一个优先级最高的未处理簇
- [x] POST /execute/car-arrived — 小车到达 → TSP
- [x] POST /execute/drone-done — 无人机完成 → 标记 done+释放车辆
- [x] GET /execute/progress — 获取执行进度
- [x] POST /resources — 批量注册/更新车辆
- [x] GET /resources — 获取车辆列表
- [x] PUT /resources/{id}/state — 更新车辆状态

## 前后端交互时序

- [x] 加载 PNG → coordMapper
- [x] 点击画布标记路点/路段 → POST /map/nodes, /map/edges
- [x] 点"建图" → POST /map/build → DLL: create_graph + encode + create_standmap → 回写编码到 DB
- [x] 布任务点 → POST /tasks
- [x] 点"聚类" → POST /tasks/cluster → DLL: kmeans → 存 DB
- [x] 拖拽排优先级 → PUT /tasks/priority
- [x] 点"执行下一簇" → POST /execute/next → DLL: car_planning → 返回小车路径
- [x] 小车到达 → POST /execute/car-arrived → DLL: tsp_plan
- [x] 无人机完成 → POST /execute/drone-done → UPDATE done=1, resources.state=1

## DLL 导出函数

- [x] create_graph — 已对接
- [x] create_standmap — 已对接
- [x] free_graph — 已对接
- [x] free_standmap — 已对接
- [x] encode_id — 已对接
- [x] encode_nodes — 已对接
- [x] encode_edges — 已对接
- [x] kmeans — 已对接
- [x] car_planning — 已对接
- [x] free_car_path — 已对接
- [x] tsp_plan — 已对接
- [x] free_array — 已对接
- [x] export_standmap — 新增（文档之外，用于持久化）
- [x] create_standmap_from_data — 新增（文档之外，用于持久化）

## 差异项

1. **API 路径前缀**: 文档写 `/api/map/...` 但项目实际用 `/map/...`（Vite 代理处理 /api 转发）
2. **standmap 主键**: 文档是复合主键 (center_node, target_node)，实现用自增 id（功能等价）
3. **export_standmap/create_standmap_from_data**: 为实现 standmap 持久化额外新增的 2 个函数，不违反文档
