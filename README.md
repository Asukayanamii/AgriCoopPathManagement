# AgriCoopPathManagement — 农业无人机与小车协同算法展示系统

基于 Vue 3 + Spring Boot 的全栈算法可视化平台，集成空间聚类、A\* 路径规划、资源搜索三种核心算法，支持单算法独立运行与多算法协同流水线。算法核心通过 C++ 实现并经 JNI 集成，兼顾开发效率与计算性能。

## 功能模块

- **聚类算法** — 在地图上生成随机任务点，通过迭代分裂聚类识别高密度作业区域
- **路径规划** — 在障碍物环境中使用 A\* 算法搜索最短避障路径，支持调节搜索粒度
- **资源搜索** — 为每个目标点匹配最近的可用资源，使用二分搜索优化分配效率
- **协同流水线** — 将聚类 → 资源搜索 → 路径规划串联为端到端自动化决策流程
- **地图预处理** — 上传 PNG 地图，栅格化后交互式标记障碍物、起点和终点，执行路径规划并可视化结果

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 (Composition API), Vite, Element Plus, Pinia, Vue Router, Axios, SVG |
| 后端 | Spring Boot (多模块 Maven), MyBatis, MySQL, Druid, JWT |
| 算法集成 | JNI (Java Native Interface), C++ DLL, 运行时动态加载 |
| 存储 | 阿里云 OSS / 本地文件存储, 自动缩略图生成 |

## 快速开始

### 环境要求

- Node.js 18+、npm
- JDK 17+、Maven 3.8+
- MySQL 8.0+、Redis
- g++ (如需重新编译 DLL)

### 启动

**1. 初始化数据库**

```sql
CREATE DATABASE agricooppath;
USE agricooppath;
SOURCE backend/db/schema.sql;
```

**2. 启动后端**

配置 `backend/ACP-server/src/main/resources/application-dev.yml` 中的数据库和 Redis 连接信息，然后：

```bash
cd backend
mvn clean package -DskipTests
java -jar ACP-server/target/ACP-server-0.0.1-SNAPSHOT.jar
```

**3. 启动前端**

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，通过 Vite 代理将 `/api` 请求转发到后端 `localhost:8080`。

### 构建 DLL（可选）

如需修改 A\* 搜索参数或自定义算法：

```bash
cd backend/ACP-common/native
g++ -shared -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/win32" -O2 -o AStarJNI.dll AStarJNI.cpp
```

将生成的 DLL 复制到 `ACP-common/src/main/resources/native/`。

## 坐标系统

系统采用三层坐标体系以适配不同分辨率的地图：

| 层级 | 范围 | 说明 |
|------|------|------|
| 图片像素坐标 | `[0, imgW) × [0, imgH)` | PNG 原始像素 |
| 逻辑坐标 | `[0, 1000] × [0, 1000]` | API 传输与存储的统一坐标 |
| SVG 渲染坐标 | viewBox 动态适配 | 屏幕渲染位置 |

所有前后端 API 交互均使用逻辑坐标，与图片分辨率无关。栅格分辨率参数 `gridResolution` 控制 A\* 搜索粒度（值越小网格越密，路径精度越高）。

## 项目结构

```
frontend/
  src/
    api/          — 接口封装 (algorithm, map, login)
    stores/       — Pinia 状态管理
    utils/        — 工具 (Axios 实例)
    views/
      algorithm/  — 四个算法页面
      map/        — 地图预处理页面
      layout/     — 布局组件
backend/
  ACP-common/     — JNI 桥接、原生 DLL、通用工具
  ACP-pojo/       — DTO、实体、VO
  ACP-server/     — 控制器、服务、Mapper、配置
  ACP-common/native/ — C++ 源码 (A\*、聚类、资源搜索)
```
