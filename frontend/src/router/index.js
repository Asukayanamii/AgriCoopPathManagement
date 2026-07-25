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
        { path: 'map-edit', name: 'map-edit', component: () => import('@/views/map/MapPreprocess.vue') },
      ]
    },
    { path: '/login', name: 'login', component: LoginView }
  ]
})

export default router
