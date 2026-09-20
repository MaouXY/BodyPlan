<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CollectionTag,
  DataAnalysis,
  DataLine,
  User,
} from '@element-plus/icons-vue'

import AppSidebar from '@/components/AppSidebar.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const menus = computed(() => [
  {
    title: '数据与用户',
    items: [
      {
        label: '打卡数据统计',
        path: '/admin/stats',
        icon: DataAnalysis,
      },
      {
        label: '用户管理',
        path: '/admin/users',
        icon: User,
      },
    ],
  },
  {
    title: '内容与档案',
    items: [
      {
        label: '身体数据查看',
        path: '/admin/body-data',
        icon: DataLine,
      },
      {
        label: '动作标签管理',
        path: '/admin/tags',
        icon: CollectionTag,
      },
    ],
  },
])

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <AppSidebar
      :collapsed="appStore.sidebarCollapsed"
      :menus="menus"
      :current-path="route.path"
    />
    <div class="shell__main">
      <header class="shell__header">
        <div>
          <p class="shell__eyebrow">个性化健身管理系统</p>
          <h1>{{ route.meta.title }}</h1>
        </div>
        <div class="shell__actions">
          <el-button plain @click="appStore.toggleSidebar()">
            {{ appStore.sidebarCollapsed ? '展开导航' : '收起导航' }}
          </el-button>
          <el-dropdown>
            <span class="shell__user">
              {{ authStore.displayName }}
              <small>管理员</small>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="shell__content">
        <router-view />
      </main>
    </div>
  </div>
</template>
