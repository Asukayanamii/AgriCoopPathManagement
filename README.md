# AgriCoopPathManagement — 智慧农业无人机调度系统

面向农业场景的无人机与无人车协同调度可视化平台。用户上传农田地图后，通过渐进式操作流程完成从路网标记、K-means 聚类、资源分配到 A\* 路径规划和小车/无人机协同任务执行的完整闭环。算法核心通过 C++ 实现，经 JNI 集成到 Spring Boot 后端。

## 操作流程

```
① 标记路网 → ② 建图 → ③ 任务点 & 聚类 → ④ 注册车辆 → ⑤ 排优先级 → ⑥ 执行
```

| 步骤 | 说明 |
|------|------|
| ① 标记路网 | 在地图上点击标记岔路口/拐点（图节点）和可行走路段（边） |
| ② 建图 | DLL 建图 + 节点编码 + 标准距离地图，结果持久化到数据库 |
| ③ 任务点 & 聚类 | 标记作业任务点，执行 K-means 聚类，自动创建优先级列表 |
| ④ 车辆资源 | 在地图路点上登记空闲车辆（后续算法自动分配） |
| ⑤ 优先级 | 拖拽排序各簇的处理顺序 |
| ⑥ 执行 | 小车路径规划 → 到达后 TSP 无人机路径 → 完成标记 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 (Composition API), Vite, Element Plus, SVG |
| 后端 | Spring Boot (多模块 Maven), MyBatis, MySQL, JWT |
| 算法集成 | JNI, C++ DLL, 运行时动态加载 |
| 算法 | K-means 聚类, A\* 图搜索, TSP 路径规划 (Held-Karp DP), 二分搜索资源分配 |

## 核心算法（DLL 导出函数）

| 函数 | 用途 |
|:-----|:------|
| `create_graph` | 根据边表建无向图（前向星） |
| `create_standmap` | 建 A\* 启发数据（Dijkstra 全源最短路径） |
| `encode_id / encode_nodes / encode_edges` | 节点/边 ID ↔ 4 位十六进制编码 |
| `kmeans` | K-means 聚类（不依赖图结构） |
| `car_planning` | 综合接口：选目标岔路口 → 搜附近小车 → A\* 路径 |
| `tsp_plan` | TSP 路径规划（Held-Karp DP, n ≤ 20） |
| `export_standmap / create_standmap_from_data` | 标准距离地图的持久化与恢复 |

## 坐标系统

采用三层坐标体系以适配不同分辨率的地图：

| 层级 | 范围 | 说明 |
|------|------|------|
| 图片像素坐标 | `[0, imgW) × [0, imgH)` | PNG 原始像素 |
| 逻辑坐标 | `[0, 1000] × [0, 1000]` | 与分辨率无关，API 传输和存储用 |
| SVG 渲染坐标 | viewBox 动态适配 | 屏幕渲染位置，含宽高比自适应 |

所有前后端接口交互使用逻辑坐标。

## 快速开始

### 环境要求

- Node.js 18+, npm
- JDK 17+, Maven 3.8+
- MySQL 8.0+
- g++ (如需重新编译 DLL)

### 启动

**1. 初始化数据库**

```sql
CREATE DATABASE agricooppath;
USE agricooppath;
SOURCE backend/db/schema.sql;
SOURCE backend/db/migrate_v2.sql;
```

**2. 启动后端**

配置 `backend/ACP-server/src/main/resources/application-dev.yml` 中的数据库连接信息，在 IntelliJ 中打开项目，运行 `AcpServerApplication.main()`。

**3. 启动前端**

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，通过 Vite 代理将 `/api` 请求转发到后端 `localhost:8080`。

### 构建 DLL

```bash
g++ -std=c++17 -O2 -shared -o UAVLibrary.dll UAVLibrary.cpp -lws2_32
```

## 项目结构

```
frontend/
  src/
    api/          — 接口封装
    views/map/    — 算法演示页面（渐进式流程）
    views/layout/ — 布局组件
backend/
  ACP-common/     — JNI 桥接、原生 DLL、通用工具
    native/       — C++ 源码 + JNI 适配器
  ACP-pojo/       — DTO、实体、VO
  ACP-server/     — 控制器、服务、Mapper、配置
  db/             — SQL 脚本（建表、迁移、数据清理）
```

## 关键约定

- 节点编码 = 节点 ID 的 4 位十六进制
- 路径编码 = 节点编码的拼接，表示访问顺序
- 图是双向无向图，边权为两点间距
- 小车只停靠在图节点上，无人机无视障碍（飞行），TSP 直接用欧氏距离
- 所有的算法数据按 `map_id` 隔离，不同地图的数据互不干扰
