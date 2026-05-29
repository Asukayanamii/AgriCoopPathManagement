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
        { path: 'algorithm/cluster', name: 'cluster', component: () => import('@/views/algorithm/ClusterView.vue') },
        { path: 'algorithm/path-planning', name: 'path-planning', component: () => import('@/views/algorithm/PathPlanView.vue') },
        { path: 'algorithm/resource-search', name: 'resource-search', component: () => import('@/views/algorithm/ResourceSearchView.vue') },
        { path: 'algorithm/pipeline', name: 'pipeline', component: () => import('@/views/algorithm/PipelineView.vue') },
      ]
    },
    { path: '/login', name: 'login', component: LoginView }
  ]
})

export default router
