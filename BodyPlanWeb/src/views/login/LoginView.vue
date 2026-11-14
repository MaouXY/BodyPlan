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
      <p class="login-hero__eyebrow">BodyPlan</p>
      <h1>律动健身</h1>
      <p class="login-hero__text">管理与教练工作台</p>
    </section>

    <section class="login-card">
      <div class="login-card__header">
        <h2>账号登录</h2>
        <p>管理员 / 教练</p>
      </div>

      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="账号" />
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
