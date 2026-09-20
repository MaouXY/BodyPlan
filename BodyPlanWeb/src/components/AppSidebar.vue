<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  collapsed: boolean
  currentPath: string
  menus: Array<{
    title: string
    items: Array<{
      label: string
      path: string
      icon: Component
    }>
  }>
}>()
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': collapsed }">
    <div class="sidebar__brand">
      <span class="sidebar__mark">BP</span>
      <div v-if="!collapsed">
        <strong>BodyPlan 管理端</strong>
        <p>个性化健身管理系统</p>
      </div>
    </div>

    <section v-for="group in menus" :key="group.title" class="sidebar__group">
      <p v-if="!collapsed" class="sidebar__title">{{ group.title }}</p>
      <router-link
        v-for="item in group.items"
        :key="item.path"
        :to="item.path"
        class="sidebar__link"
        :class="{ 'is-active': currentPath === item.path }"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span v-if="!collapsed">{{ item.label }}</span>
      </router-link>
    </section>
  </aside>
</template>
