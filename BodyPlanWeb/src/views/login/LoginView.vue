<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import type { LoginForm } from '@/types/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const submitting = ref(false)

const form = reactive<LoginForm>({
  username: 'admin',
  password: '123456',
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  submitting.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    const redirect =
      typeof route.query.redirect === 'string'
        ? route.query.redirect
        : authStore.defaultRoute
    await router.push(redirect)
  } catch (error) {
    console.error('scenario=admin-login', { username: form.username }, error instanceof Error ? error.stack : error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-hero">
      <p class="login-hero__eyebrow">BodyPlan · 管理端</p>
      <h1>个性化健身管理系统</h1>
      <p class="login-hero__text">
        管理端覆盖用户与建档档案查看、身体数据趋势、动作标签维护，以及打卡数据统计与排行。
        训练计划由后端按“目标 + 体能等级 + 重点部位 + 伤病禁忌 + 场地器械”五要素规则生成，
        管理端负责数据查看与内容维护。
      </p>
      <div class="login-hero__chips">
        <span>用户管理</span>
        <span>身体数据</span>
        <span>动作标签</span>
        <span>打卡统计</span>
      </div>
    </section>

    <section class="login-card">
      <div class="login-card__header">
        <h2>管理员登录</h2>
        <p>仅管理员账号可登录管理端，普通用户请使用 Android App。</p>
      </div>

      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入管理员账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button
          class="login-card__submit"
          type="primary"
          :loading="submitting"
          @click="handleLogin"
        >
          登录管理端
        </el-button>
      </el-form>

      <p class="login-card__tip">演示账号：admin / 123456</p>
    </section>
  </div>
</template>

<style scoped>
.login-card__tip {
  margin-top: 12px;
  font-size: 12px;
  color: #8a8f99;
  text-align: center;
}
</style>
