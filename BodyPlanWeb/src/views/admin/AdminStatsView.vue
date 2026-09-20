<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'

import {
  fetchCheckinRanking,
  fetchCheckinTrend,
  fetchOverview,
} from '@/api/admin'
import type {
  AdminCheckinRankingItem,
  AdminCheckinTrendPoint,
  AdminOverview,
} from '@/types/admin'

const loading = ref(false)
const overview = ref<AdminOverview | null>(null)
const trend = ref<AdminCheckinTrendPoint[]>([])
const ranking = ref<AdminCheckinRankingItem[]>([])
const trendDays = ref(14)
const chartRef = ref<HTMLDivElement | null>(null)
let chart: ReturnType<typeof echarts.init> | null = null

const cards = computed(() => {
  const data = overview.value
  if (!data) {
    return []
  }
  return [
    { label: '用户总数', value: data.userTotal, unit: '人' },
    { label: '完成建档', value: data.profileCompletedTotal, unit: '人' },
    { label: '打卡总数', value: data.checkinTotal, unit: '次' },
    { label: '今日打卡', value: data.todayCheckinTotal, unit: '次' },
    { label: '进行中计划', value: data.activePlanTotal, unit: '个' },
    { label: '动作总数', value: data.exerciseTotal, unit: '个' },
  ]
})

const taskSummary = computed(() => {
  const data = overview.value
  if (!data) {
    return '-'
  }
  return `计划任务已完成 ${data.planItemDoneTotal} 项，待完成 ${data.planItemPendingTotal} 项`
})

async function load() {
  loading.value = true
  try {
    const [overviewData, trendData, rankingData] = await Promise.all([
      fetchOverview(),
      fetchCheckinTrend(trendDays.value),
      fetchCheckinRanking(10),
    ])
    overview.value = overviewData
    trend.value = trendData
    ranking.value = rankingData
    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value) {
    return
  }
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['打卡次数', '活跃用户'], bottom: 0 },
    grid: { left: 48, right: 24, top: 32, bottom: 56 },
    xAxis: {
      type: 'category',
      data: trend.value.map((point) => point.date.slice(5)),
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '打卡次数',
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: trend.value.map((point) => point.checkinCount),
      },
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        data: trend.value.map((point) => point.activeUserCount),
      },
    ],
  })
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div v-loading="loading" class="stats">
    <div class="stats__bar">
      <el-radio-group v-model="trendDays" @change="load">
        <el-radio-button :value="7">近 7 天</el-radio-button>
        <el-radio-button :value="14">近 14 天</el-radio-button>
        <el-radio-button :value="30">近 30 天</el-radio-button>
      </el-radio-group>
      <el-button type="primary" plain @click="load">刷新</el-button>
    </div>

    <div class="stats__cards">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <p class="stat-card__label">{{ card.label }}</p>
        <p class="stat-card__value">
          {{ card.value }}
          <small>{{ card.unit }}</small>
        </p>
      </div>
    </div>

    <el-card shadow="never" class="stats__panel">
      <template #header>
        <div class="panel__header">
          <span>打卡趋势</span>
          <span class="panel__hint">{{ taskSummary }}</span>
        </div>
      </template>
      <div ref="chartRef" class="stats__chart"></div>
    </el-card>

    <el-card shadow="never" class="stats__panel">
      <template #header>
        <div class="panel__header">
          <span>打卡排行榜（前 10）</span>
          <span class="panel__hint">按打卡总数倒序</span>
        </div>
      </template>
      <el-table :data="ranking" stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="checkinCount" label="打卡次数" width="110" />
        <el-table-column label="连续打卡" width="110">
          <template #default="{ row }">
            {{ row.currentStreak ?? 0 }} 天
          </template>
        </el-table-column>
        <el-table-column prop="lastCheckinDate" label="最近打卡" min-width="130" />
        <template #empty>暂无打卡数据</template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stats__cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.stat-card__label {
  margin: 0;
  font-size: 13px;
  color: #8a8f99;
}

.stat-card__value {
  margin: 8px 0 0;
  font-size: 26px;
  font-weight: 600;
  color: #1f2d3d;
}

.stat-card__value small {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  color: #8a8f99;
}

.stats__chart {
  width: 100%;
  height: 320px;
}

.panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel__hint {
  font-size: 12px;
  color: #8a8f99;
}
</style>
