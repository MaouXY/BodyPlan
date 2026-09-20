<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { fetchBodyData } from '@/api/admin'
import type { AdminBodyDataItem } from '@/types/admin'
import { BODY_TYPE_LABELS, labelOf } from '@/utils/dict'

const loading = ref(false)
const rows = ref<AdminBodyDataItem[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  userId: undefined as number | undefined,
})

async function load() {
  loading.value = true
  try {
    const result = await fetchBodyData({
      page: query.page,
      size: query.size,
      userId: query.userId,
    })
    rows.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  load()
}

function handlePageChange(page: number) {
  query.page = page
  load()
}

function handleSizeChange(size: number) {
  query.size = size
  query.page = 1
  load()
}

function handleReset() {
  query.userId = undefined
  handleSearch()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input-number
          v-model="query.userId"
          class="toolbar__input"
          :controls="false"
          placeholder="用户ID"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button plain @click="handleReset">重置</el-button>
        <span class="toolbar__hint">
          身体数据包含建档时的初始记录与后续每一次复测，用于观察体重、BMI 与评分变化
        </span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="userId" label="用户ID" width="170" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="heightCm" label="身高(cm)" width="100" />
        <el-table-column prop="weightKg" label="体重(kg)" width="100" />
        <el-table-column prop="bmi" label="BMI" width="90" />
        <el-table-column label="体型" width="100">
          <template #default="{ row }">
            {{ labelOf(BODY_TYPE_LABELS, row.bodyType) }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="身体评分" width="100" />
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column prop="recordedAt" label="记录时间" min-width="160" />
        <template #empty>暂无身体数据</template>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :current-page="query.page"
        :page-size="query.size"
        :page-sizes="[10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar__input {
  width: 200px;
}

.toolbar__hint {
  font-size: 12px;
  color: #8a8f99;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
