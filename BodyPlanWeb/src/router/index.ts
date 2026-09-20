import { createRouter, createWebHistory } from 'vue-router'

import AppShell from '@/layouts/AppShell.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      redirect: '/admin/stats',
    },
    {
      path: '/',
      component: AppShell,
      children: [
        {
          path: 'admin/stats',
          name: 'admin-stats',
          component: () => import('@/views/admin/AdminStatsView.vue'),
          meta: { title: '打卡数据统计' },
        },
        {
          path: 'admin/users',
          name: 'admin-users',
          component: () => import('@/views/admin/AdminUsersView.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'admin/body-data',
          name: 'admin-body-data',
          component: () => import('@/views/admin/AdminBodyDataView.vue'),
          meta: { title: '身体数据查看' },
        },
        {
          path: 'admin/tags',
          name: 'admin-tags',
          component: () => import('@/views/admin/AdminTagsView.vue'),
          meta: { title: '动作标签管理' },
        },
      ],
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.title) {
    document.title = `${String(to.meta.title)} - ${import.meta.env.VITE_APP_TITLE}`
  }

  if (to.meta.public) {
    return true
  }

  if (!authStore.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
