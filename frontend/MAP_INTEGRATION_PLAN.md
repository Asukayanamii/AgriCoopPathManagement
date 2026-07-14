# 真实地图 PNG 接入方案

## 1. 核心问题

当前系统的坐标流：

```
前端内部坐标 (0~200)
  → ×3 映射到 SVG (0~600)
    → 传给后端 mapWidth=600
      → DLL 栅格化 600/10=60×60
        → 路径输出网格中心点
```

引入真实地图 PNG 后，图片有自己的像素尺寸（如 1920×1080, 800×600），需要一套与分辨率无关的坐标体系衔接前后端。

---

## 2. 整体架构

```
┌───────────────────────────────────────────────────────────────┐
│                   前端渲染层 (SVG 0~1000)                       │
│  <image> + 宽高比自适应 + 居中偏移                                │
│  用户点击 → 逻辑坐标 (归一化 0~1000)                             │
│  路径/标注 → 逻辑坐标 → SVG 坐标渲染                             │
└──────────────────────┬────────────────────────────────────────┘
                       │ coordMapper.js
                       ▼
┌───────────────────────────────────────────────────────────────┐
│                   逻辑坐标空间 (0~1000 归一化)                    │
│                                                                  │
│  像素 → 逻辑: x_logical = x_px / imgW * 1000                    │
│  逻辑 → 像素: x_px = x_logical / 1000 * imgW                    │
│                                                                  │
│  所有标注点、障碍物、路径点、聚类中心均以逻辑坐标存储和传输              │
└──────────────────────┬────────────────────────────────────────┘
                       │ API (mapWidth=1000, mapHeight=1000)
                       ▼
┌───────────────────────────────────────────────────────────────┐
│                   后端算法层                                     │
│                                                                  │
│  gridResolution 参数控制栅格粒度 (默认 10)                        │
│  网格数 = 1000 / gridResolution                                 │
│  DLL 不再硬编码 /10, 改用传入的 gridResolution                     │
└───────────────────────────────────────────────────────────────┘
```

---

## 3. 坐标映射流程

### 3.1 三层坐标体系

| 层级 | 范围 | 说明 |
|------|------|------|
| **图片像素坐标** | `[0, imgW) × [0, imgH)` | PNG 原始像素位置 |
| **逻辑坐标** | `[0, 1000] × [0, 1000]` | 与分辨率无关，前后端传输和存储的统一坐标 |
| **SVG 渲染坐标** | `[0, svgViewBoxW] × [0, svgViewBoxH]` | 实际屏幕渲染位置（含宽高比偏移） |

### 3.2 坐标转换公式

```javascript
// coordMapper.js

// 图片像素 → 逻辑坐标（用于标注点转换）
function pxToLogical(px, imgW, imgH) {
  return {
    x: (px.x / imgW) * 1000,
    y: (px.y / imgH) * 1000
  }
}

// 逻辑坐标 → 图片像素（用于在图上定位）
function logicalToPx(logical, imgW, imgH) {
  return {
    x: (logical.x / 1000) * imgW,
    y: (logical.y / 1000) * imgH
  }
}

// 逻辑坐标 → SVG 渲染坐标（含宽高比自适应）
function logicalToSvg(logical, viewBox, imgW, imgH) {
  const scale = Math.min(viewBox.w / imgW, viewBox.h / imgH)
  const dispW = imgW * scale
  const dispH = imgH * scale
  const offsetX = (viewBox.w - dispW) / 2
  const offsetY = (viewBox.h - dispH) / 2
  return {
    x: (logical.x / 1000) * dispW + offsetX,
    y: (logical.y / 1000) * dispH + offsetY
  }
}

// SVG 点击 → 逻辑坐标（用户点击地图反向映射）
function svgClickToLogical(svgPos, viewBox, imgW, imgH) {
  const scale = Math.min(viewBox.w / imgW, viewBox.h / imgH)
  const dispW = imgW * scale
  const dispH = imgH * scale
  const offsetX = (viewBox.w - dispW) / 2
  const offsetY = (viewBox.h - dispH) / 2
  return {
    x: ((svgPos.x - offsetX) / dispW) * 1000,
    y: ((svgPos.y - offsetY) / dispH) * 1000
  }
}
```

### 3.3 传输约定

所有前端→后端的 API 请求、后端→前端的响应，一律使用**逻辑坐标 (0~1000)**。

```javascript
// 前端发送
const res = await pathPlanning({
  mapWidth: 1000, mapHeight: 1000,
  startX: startLogical.x, startY: startLogical.y,
  endX: endLogical.x, endY: endLogical.y,
  obstacles: obstaclesInLogical,
  gridResolution: 10   // ← 新增，控制 DLL 栅格粒度
})
```

---

## 4. SVG 渲染方案

### 4.1 地图背景 + 标注叠层

```html
<svg viewBox="0 0 1000 1000">
  <!-- 地图图片背景（保持宽高比居中） -->
  <image
    :x="imgOffsetX" :y="imgOffsetY"
    :width="imgDisplayW" :height="imgDisplayH"
    :href="mapImageUrl"
    preserveAspectRatio="xMidYMid meet"
  />

  <!-- 半透明栅格覆层 -->
  <line v-for="gridLine in gridLines" ... stroke="rgba(0,0,0,0.08)"/>

  <!-- 标注点 -->
  <g v-for="pt in logicalPoints">
    <circle
      :cx="logicalToSvg(pt, viewBox, imgW, imgH).x"
      :cy="logicalToSvg(pt, viewBox, imgW, imgH).y"
      r="5" fill="#e74c3c"
    />
  </g>

  <!-- 路径 -->
  <polyline
    :points="pathInLogical.map(p => {
      const svg = logicalToSvg(p, viewBox, imgW, imgH)
      return `${svg.x},${svg.y}`
    }).join(' ')"
    fill="none" stroke="#3498db" stroke-width="3"
  />
</svg>
```

### 4.2 宽高比自适应

```javascript
const viewBox = { w: 1000, h: 1000 }

// 根据图片实际宽高比计算显示区域
const imgAspect = imgNaturalW / imgNaturalH
const vbAspect = viewBox.w / viewBox.h

if (imgAspect > vbAspect) {
  // 图片更宽 → 水平撑满，上下留白
  imgDisplayW = viewBox.w
  imgDisplayH = viewBox.w / imgAspect
  imgOffsetX = 0
  imgOffsetY = (viewBox.h - imgDisplayH) / 2
} else {
  // 图片更高 → 垂直撑满，左右留白
  imgDisplayH = viewBox.h
  imgDisplayW = viewBox.h * imgAspect
  imgOffsetX = (viewBox.w - imgDisplayW) / 2
  imgOffsetY = 0
}
```

---

## 5. 后端改造

### 5.1 DTO 新增字段

```java
// PathPlanRequestDTO.java
public class PathPlanRequestDTO {
    private int mapWidth = 1000;       // 固定 1000
    private int mapHeight = 1000;      // 固定 1000
    private int gridResolution = 10;   // ← 新增，默认 10（即 100×100 网格）
    private int startX, startY;
    private int endX, endY;
    private List<int[]> obstacles;
}
```

同样在 `ClusterRequestDTO`、`ResourceSearchRequestDTO`、`PipelineRequestDTO` 中增加 `gridResolution` 字段。

### 5.2 DLL 改造

```cpp
// AStarJNI.cpp 改造对照
// 当前:  int gw = mapW / 10;          // 硬编码 10
// 改造:  int gw = mapW / gridRes;     // 从 Java 传入

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_agripath_acpcommon_utils_AStarNative_findPath(
    JNIEnv* env, jclass clazz,
    jint mapW, jint mapH,
    jint startX, jint startY, jint endX, jint endY,
    jint obstacleCount, jintArray jox, jintArray joy,
    jint gridRes)               // ← 新增参数
{
    int gw = mapW / gridRes;
    int gh = mapH / gridRes;
    // ...
    // 输出: (gridCol * gridRes + gridRes/2, gridRow * gridRes + gridRes/2)
}
```

### 5.3 Java JNI Wrapper 同步修改

```java
// AStarNative.java
public static native int[][] findPath(
    int mapW, int mapH,
    int startX, int startY, int endX, int endY,
    int obstacleCount, int[] ox, int[] oy,
    int gridResolution   // ← 新增
);
```

### 5.4 Service 层传递参数

```java
// AlgorithmPipelineService.java
public Map<String, Object> pathPlanning(PathPlanRequestDTO dto) {
    int[][] path = AStarNative.findPath(
        dto.getMapWidth(), dto.getMapHeight(),
        dto.getStartX(), dto.getStartY(),
        dto.getEndX(), dto.getEndY(),
        obstacles.size(), ox, oy,
        dto.getGridResolution()  // ← 传入
    );
    // ...
}
```

---

## 6. 前端改造清单

### 6.1 新增文件

| 文件 | 职责 |
|------|------|
| `src/utils/coordMapper.js` | 像素/逻辑/SVG 三层坐标互转 |
| `src/components/MapBackground.vue` | 封装地图背景渲染组件，含 `<image>`、宽高比计算、栅格绘制 |

### 6.2 修改文件

| 文件 | 改动 |
|------|------|
| `src/views/algorithm/ClusterView.vue` | SVG 添加 `<MapBackground>` 组件；坐标渲染改用 `logicalToSvg()` |
| `src/views/algorithm/PathPlanView.vue` | 同上；逻辑坐标发送（0~1000）；移除硬编码 ×3 缩放 |
| `src/views/algorithm/ResourceSearchView.vue` | 同上 |
| `src/views/algorithm/PipelineView.vue` | 同上 |
| `src/stores/algorithmStore.js` | 接口参数使用逻辑坐标 |
| `src/api/map.js`（恢复） | 新增地图上传/列表/节点存储接口 |
| `vite.config.js` | 若需代理图片资源服务 |

### 6.3 主要 View 改造模式

以 PathPlanView.vue 为例，坐标系改造前后的变化：

```javascript
// 改造前
const scale = 3
const start = ref({ x: 30, y: 30 })  // 内部 0~200

// 发送时手动 ×3
const res = await pathPlanning({
  mapWidth: 600, mapHeight: 600,
  startX: start.value.x * 3, startY: start.value.y * 3,
  ...
})
// SVG 渲染时再 ×3
const startS = computed(() => [start.value.x * scale, start.value.y * scale])

// ─── 改造后 ───

// 逻辑坐标 (0~1000)
const start = ref({ x: 150, y: 150 })

// 直接发送逻辑坐标
const res = await pathPlanning({
  mapWidth: 1000, mapHeight: 1000,
  startX: start.value.x, startY: start.value.y,
  gridResolution: 10,
  ...
})

// SVG 渲染通过 coordMapper 自动转换
import { logicalToSvg } from '@/utils/coordMapper'
const startSvg = computed(() => {
  const svg = logicalToSvg(
    { x: start.value.x, y: start.value.y },
    { w: 1000, h: 1000 },
    imgNaturalW.value, imgNaturalH.value
  )
  return [svg.x, svg.y]
})
```

---

## 7. 网格分辨率对照

`gridResolution` 参数直接影响 DLL 内部网格数和路径精度：

| gridResolution | 网格数 (mapW=1000) | 单网格代表距离 | 精度 | 性能 |
|:---:|:---:|:---:|:---:|:---:|
| 5 | 200×200 | 5 单位 | 高 | 较慢 |
| 10 | 100×100 | 10 单位 | 中 | 快 |
| 20 | 50×50 | 20 单位 | 低 | 很快 |

默认 `gridResolution=10` 提供 100×100 网格，平衡精度和性能。可在界面提供滑块让用户调节。

---

## 8. 向后兼容

当前前端生成的是 0~200 的内部坐标，乘以 3 后发给 mapWidth=600 的后端。迁移到 0~1000 逻辑坐标后：

| 项 | 当前 | 迁移后 |
|----|------|--------|
| 前端随机点范围 | 20~180 | 100~900 |
| 后端 mapWidth | 600 | 1000 |
| DLL 网格 (gridRes=10) | 60×60 | 100×100 |
| 路径点密度 | ~10 单位步长 | ~10 单位步长 |
| SVG viewBox | 600×600 | 1000×1000 |

迁移后随机点数据的空间分布不变（均匀分布在 80% 的坐标范围内），路径步长一致，算法行为无变化。

---

## 9. 实施步骤建议

```
Phase 1 — 基础设施
  ├─ 创建 coordMapper.js（坐标转换工具函数）
  ├─ 修改 DTO，新增 gridResolution 字段
  ├─ 修改 AStarJNI.cpp，gridResolution 替代硬编码 /10
  └─ 修改 Java JNI Wrapper 和 Service 层

Phase 2 — 前端迁移
  ├─ 创建 MapBackground.vue 组件
  ├─ 逐个改造 4 个 View 至 0~1000 逻辑坐标
  │  ├─ ClusterView.vue
  │  ├─ PathPlanView.vue
  │  ├─ ResourceSearchView.vue
  │  └─ PipelineView.vue
  └─ 验证各页面的坐标对齐

Phase 3 — 地图接入
  ├─ 实现地图上传接口（后端存储图片 + 元数据）
  ├─ 恢复 map.js API 文件，对接地图列表/节点存储
  ├─ 在各 View 中接入 MapBackground 显示真实地图
  └─ 测试不同分辨率 PNG 的渲染和交互
```
