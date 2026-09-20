<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchUserDetail, fetchUsers, updateUserStatus } from '@/api/admin'
import type { AdminUserDetail, AdminUserItem } from '@/types/admin'
import {
  BODY_TYPE_LABELS,
  GENDER_LABELS,
  GOAL_LABELS,
  INJURY_LABELS,
  LEVEL_LABELS,
  PLAN_STATUS_LABELS,
  TRAIN_LIMIT_LABELS,
  labelOf,
  labelsOf,
  weekDaysOf,
} from '@/utils/dict'

const loading = ref(false)
const rows = ref<AdminUserItem[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined as number | undefined,
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminUserDetail | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await fetchUsers({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status,
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
  query.keyword = ''
  query.status = undefined
  handleSearch()
}

async function handleStatusChange(row: AdminUserItem) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await updateUserStatus(row.id, nextStatus)
    row.status = nextStatus
    ElMessage.success(nextStatus === 1 ? '已启用该账号' : '已禁用该账号')
  } catch {
    // 失败提示由拦截器给出
  }
}

async function openDetail(row: AdminUserItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchUserDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          class="toolbar__input"
          placeholder="账号 / 昵称 / 手机号"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="query.status"
          class="toolbar__select"
          placeholder="账号状态"
          clearable
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button plain @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建档" width="90">
          <template #default="{ row }">
            <el-tag :type="row.profileCompleted ? 'success' : 'warning'" size="small">
              {{ row.profileCompleted ? '已完成' : '未完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="主目标" min-width="110">
          <template #default="{ row }">
            {{ labelOf(GOAL_LABELS, row.primaryGoal) }}
          </template>
        </el-table-column>
        <el-table-column label="打卡" width="80">
          <template #default="{ row }">{{ row.checkinCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="最近打卡" min-width="120">
          <template #default="{ row }">{{ row.latestCheckinDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>暂无用户</template>
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

    <el-drawer v-model="detailVisible" title="用户详情" size="46%">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions title="账号信息" :column="2" border size="small">
            <el-descriptions-item label="账号">
              {{ detail.user.username }}
            </el-descriptions-item>
            <el-descriptions-item label="昵称">
              {{ detail.user.nickname }}
            </el-descriptions-item>
            <el-descriptions-item label="手机号">
              {{ detail.user.phone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              {{ detail.user.createdAt || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-descriptions
            class="block"
            title="建档档案"
            :column="2"
            border
            size="small"
          >
            <el-descriptions-item label="性别">
              {{ labelOf(GENDER_LABELS, detail.profile?.gender) }}
            </el-descriptions-item>
            <el-descriptions-item label="年龄">
              {{ detail.profile?.age ?? '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="身高 / 体重">
              {{ detail.profile?.heightCm ?? '-' }} cm /
              {{ detail.profile?.weightKg ?? '-' }} kg
            </el-descriptions-item>
            <el-descriptions-item label="体型">
              {{ labelOf(BODY_TYPE_LABELS, detail.profile?.bodyType) }}
            </el-descriptions-item>
            <el-descriptions-item label="主目标">
              {{ labelOf(GOAL_LABELS, detail.profile?.primaryGoal) }}
            </el-descriptions-item>
            <el-descriptions-item label="每日时长">
              {{ detail.profile?.dailyMinutes ?? '-' }} 分钟
            </el-descriptions-item>
            <el-descriptions-item label="每周训练日">
              {{ weekDaysOf(detail.profile?.weeklyDays) }}
            </el-descriptions-item>
            <el-descriptions-item label="重点部位">
              {{ (detail.profile?.focusParts ?? []).join('、') || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="伤病">
              {{ labelsOf(INJURY_LABELS, detail.profile?.injuries) }}
            </el-descriptions-item>
            <el-descriptions-item label="训练限制">
              {{ labelsOf(TRAIN_LIMIT_LABELS, detail.profile?.trainLimits) }}
            </el-descriptions-item>
          </el-descriptions>

          <el-descriptions
            class="block"
            title="身体数据与体能"
            :column="2"
            border
            size="small"
          >
            <el-descriptions-item label="最近身体数据">
              {{ detail.latestBodyData?.recordedAt || '-' }}（BMI
              {{ detail.latestBodyData?.bmi ?? '-' }}，评分
              {{ detail.latestBodyData?.score ?? '-' }}）
            </el-descriptions-item>
            <el-descriptions-item label="体能等级">
              {{ labelOf(LEVEL_LABELS, detail.latestFitnessTest?.level) }}
            </el-descriptions-item>
            <el-descriptions-item label="打卡统计">
              共 {{ detail.checkinStats?.totalCount ?? 0 }} 次，连续
              {{ detail.checkinStats?.currentStreak ?? 0 }} 天
            </el-descriptions-item>
            <el-descriptions-item label="今日是否打卡">
              {{ detail.checkinStats?.todayChecked ? '已打卡' : '未打卡' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-descriptions
            class="block"
            title="当前训练计划"
            :column="1"
            border
            size="small"
          >
            <el-descriptions-item label="计划">
              <template v-if="detail.activePlan">
                {{ detail.activePlan.name }}（{{
                  labelOf(PLAN_STATUS_LABELS, detail.activePlan.status)
                }}，{{ detail.activePlan.daysPerWeek }} 天/周，{{
                  detail.activePlan.minutesPerSession
                }} 分钟/次）
              </template>
              <template v-else>暂无进行中计划</template>
            </el-descriptions-item>
            <el-descriptions-item label="推荐依据">
              {{ detail.activePlan?.reason || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="block">
            <p class="block__title">身体数据历史</p>
            <el-table :data="detail.bodyDataHistory ?? []" size="small" stripe>
              <el-table-column prop="recordedAt" label="记录时间" min-width="140" />
              <el-table-column prop="weightKg" label="体重(kg)" width="100" />
              <el-table-column prop="bmi" label="BMI" width="80" />
              <el-table-column prop="score" label="评分" width="80" />
              <el-table-column prop="remark" label="备注" min-width="120" />
              <template #empty>暂无记录</template>
            </el-table>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar__input {
  max-width: 240px;
}

.toolbar__select {
  width: 140px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.block {
  margin-top: 18px;
}

.block__title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}
</style>
