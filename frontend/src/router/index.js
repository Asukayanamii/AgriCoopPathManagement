import { createRouter, createWebHistory } from 'vue-router'

import IndexView from '@/views/index/index.vue'
import LayoutView from '@/views/layout/index.vue'
import LoginView from '@/views/login/index.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '',
      component: LayoutView,
      redirect: '/index',
      children: [
        { path: 'index', name: 'index', component: IndexView },
        // 地图与节点管理
        { path: 'map/display', name: 'map-display', component: () => import('@/views/map/MapDisplay.vue') },
        { path: 'map/node-edit', name: 'node-edit', component: () => import('@/views/map/NodeEdit.vue') },
        { path: 'map/clustering', name: 'clustering', component: () => import('@/views/map/ClusteringDemo.vue') },
        // 路径规划算法
        { path: 'algorithm/display', name: 'algorithm-display', component: () => import('@/views/algorithm/AlgorithmDisplay.vue') },
        { path: 'algorithm/compare', name: 'algorithm-compare', component: () => import('@/views/algorithm/AlgorithmCompare.vue') },
        // 无人机参数与仿真
        { path: 'drone/config', name: 'drone-config', component: () => import('@/views/drone/DroneConfig.vue') },
        { path: 'simulation/preview', name: 'simulation-preview', component: () => import('@/views/simulation/SimulationPreview.vue') },
        { path: 'simulation/realtime', name: 'realtime-monitor', component: () => import('@/views/simulation/RealtimeMonitor.vue') },
        // 系统管理
        { path: 'system/user', name: 'user-management', component: () => import('@/views/system/UserManagement.vue') },
        { path: 'system/config', name: 'system-config', component: () => import('@/views/system/SystemConfig.vue') }
      ]
    },
    { path: '/login', name: 'login', component: LoginView }
  ]
})

export default router