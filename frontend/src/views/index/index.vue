<template>
  <div class="dashboard">
    <div class="hero">
      <h1>农业无人机与小车协同算法展示系统</h1>
      <p>基于 K-means 聚类 · A* 路径规划 · 二分搜索资源调度的协同算法演示平台</p>
    </div>
    <div class="cards">
      <div class="card" @click="$router.push('/algorithm/cluster')">
        <div class="icon" style="background:#e74c3c"><el-icon size="28"><PieChart /></el-icon></div>
        <h3>聚类算法</h3>
        <p>K-means 空间聚类，将农田作业请求点按位置划分为若干作业区</p>
        <el-tag size="small">点击进入 →</el-tag>
      </div>
      <div class="card" @click="$router.push('/algorithm/path-planning')">
        <div class="icon" style="background:#3498db"><el-icon size="28"><Connection /></el-icon></div>
        <h3>路径规划</h3>
        <p>A* 启发式搜索算法，融合 Dijkstra 预处理与差分启发式实现高效寻路</p>
        <el-tag size="small">点击进入 →</el-tag>
      </div>
      <div class="card" @click="$router.push('/algorithm/resource-search')">
        <div class="icon" style="background:#2ecc71"><el-icon size="28"><Search /></el-icon></div>
        <h3>资源搜索</h3>
        <p>二分搜索半径内可用小车资源，按距离排序分配至各作业请求点</p>
        <el-tag size="small">点击进入 →</el-tag>
      </div>
      <div class="card" @click="$router.push('/algorithm/pipeline')">
        <div class="icon" style="background:#9b59b6"><el-icon size="28"><VideoPlay /></el-icon></div>
        <h3>协同流水线</h3>
        <p>三步串联：聚类 → 资源搜索 → 路径规划，完整展示协同调度全流程</p>
        <el-tag size="small">点击进入 →</el-tag>
      </div>
    </div>
    <div class="flow">
      <h2>算法流程</h2>
      <div class="flow-steps">
        <div class="flow-step">1. 任务聚类<br><small>K-means 划分作业区</small></div>
        <div class="arrow">→</div>
        <div class="flow-step">2. 资源搜索<br><small>二分搜索最近小车</small></div>
        <div class="arrow">→</div>
        <div class="flow-step">3. 路径规划<br><small>A* 搜索最优路径</small></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard { max-width: 900px; margin: 0 auto; }
.hero { text-align: center; padding: 30px 0; }
.hero h1 { font-size: 26px; color: #1a202c; margin: 0 0 8px; }
.hero p { color: #666; font-size: 14px; margin: 0; }
.cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.card { background: #fff; border-radius: 12px; padding: 24px; cursor: pointer; transition: all 0.2s; border: 1px solid #e5e7eb; }
.card:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(0,0,0,0.1); }
.icon { width: 52px; height: 52px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: white; margin-bottom: 12px; }
.card h3 { margin: 0 0 8px; font-size: 16px; color: #1a202c; }
.card p { margin: 0 0 12px; font-size: 13px; color: #666; line-height: 1.5; }
.flow { margin-top: 24px; background: #fff; border-radius: 12px; padding: 24px; border: 1px solid #e5e7eb; }
.flow h2 { margin: 0 0 16px; font-size: 18px; color: #1a202c; }
.flow-steps { display: flex; align-items: center; justify-content: center; gap: 24px; }
.flow-step { text-align: center; background: #f0fdf4; padding: 20px 30px; border-radius: 12px; font-size: 15px; font-weight: 600; color: #166534; min-width: 140px; border: 1px solid #bbf7d0; }
.flow-step small { font-weight: 400; color: #666; font-size: 12px; display: block; margin-top: 4px; }
.arrow { font-size: 24px; color: #22c55e; font-weight: bold; }
</style>
