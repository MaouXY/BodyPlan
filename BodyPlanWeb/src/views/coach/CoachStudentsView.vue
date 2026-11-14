<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Bell, Search } from '@element-plus/icons-vue'
import { coachesApi, localDate, type Student, type StudentDay } from '@/api/wellness'
import { GOAL_LABELS, labelOf } from '@/utils/dict'

const date = ref(localDate())
const keyword = ref('')
const rows = ref<Student[]>([])
const busy = ref(false)
const failed = ref(false)
const detail = ref<StudentDay | null>(null)
const selected = ref<Student | null>(null)
const detailOpen = ref(false)
const detailBusy = ref(false)
const remindOpen = ref(false)
const sending = ref(false)
const content = ref('')
const filtered = computed(() => rows.value.filter(s => `${s.nickname} ${s.username}`.includes(keyword.value)))
const pending = computed(() => rows.value.filter(s => s.totalCount > s.doneCount))
async function load() {
  busy.value = true
  failed.value = false
  try { rows.value = await coachesApi.students(date.value) }
  catch (error) { failed.value = true; console.error('scenario=coach-students-load', { date: date.value }, error) }
  finally { busy.value = false }
}
async function open(row: Student) {
  selected.value = row
  detail.value = null
  detailOpen.value = true
  detailBusy.value = true
  try { detail.value = await coachesApi.day(row.studentId, date.value) }
  catch (error) { console.error('scenario=coach-student-detail', { studentId: row.studentId, date: date.value }, error) }
  finally { detailBusy.value = false }
}
function prepareReminder(row: Student) {
  selected.value = row
  content.value = `今天已完成 ${row.doneCount}/${row.totalCount} 项训练，有空记得完成剩余任务。`
  remindOpen.value = true
}
async function send() {
  if (!selected.value || sending.value) return
  sending.value = true
  try {
    await coachesApi.remind(selected.value.studentId, content.value)
    ElMessage.success('站内提醒已发送')
    remindOpen.value = false
  } catch (error) { console.error('scenario=coach-remind', { studentId: selected.value?.studentId }, error) }
  finally { sending.value = false }
}
const sportName: Record<string, string> = { RUN: '跑步', WALK: '步行', RIDE: '骑行', STRENGTH: '力量训练', OTHER: '其他运动' }
onMounted(load)
</script>

<template>
  <section class="wellness-page">
    <div class="wellness-toolbar">
      <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="load" />
      <el-input v-model="keyword" placeholder="学员姓名 / 账号" :prefix-icon="Search" clearable />
      <el-button :icon="Refresh" :loading="busy" @click="load">刷新</el-button>
    </div>
    <el-alert v-if="failed" title="加载失败，请重试" type="error" :closable="false" />
    <div class="wellness-summary">
      <div><strong>{{ rows.length }}</strong><span>我的学员</span></div>
      <div><strong>{{ pending.length }}</strong><span>任务待完成</span></div>
      <div><strong>{{ rows.filter(s => s.totalCount > 0 && s.doneCount >= s.totalCount).length }}</strong><span>已完成</span></div>
    </div>
    <el-table :data="filtered" v-loading="busy" stripe>
      <el-table-column label="学员" min-width="150"><template #default="{ row }"><strong>{{ row.nickname }}</strong><div class="wellness-muted">{{ row.username }}</div></template></el-table-column>
      <el-table-column prop="coachName" label="教练" min-width="100" />
      <el-table-column label="训练目标" min-width="110"><template #default="{ row }">{{ labelOf(GOAL_LABELS, row.goal) }}</template></el-table-column>
      <el-table-column label="当日进度" min-width="210"><template #default="{ row }">
        <span v-if="!row.totalCount" class="wellness-muted">无计划任务</span>
        <template v-else><el-progress :percentage="Math.round(row.doneCount / row.totalCount * 100)" :stroke-width="6" /><span class="wellness-muted">{{ row.doneCount }} / {{ row.totalCount }} 项</span></template>
      </template></el-table-column>
      <el-table-column label="操作" width="200"><template #default="{ row }">
        <el-button link type="primary" @click="open(row)">查看记录</el-button>
        <el-button :icon="Bell" link type="primary" :disabled="date !== localDate() || row.totalCount <= row.doneCount" @click="prepareReminder(row)">提醒</el-button>
      </template></el-table-column>
      <template #empty>暂无学员</template>
    </el-table>
    <el-drawer v-model="detailOpen" :title="`${selected?.nickname || ''} · ${date}`" size="min(560px, 96vw)">
      <div v-loading="detailBusy">
        <template v-if="detail">
          <h2 class="wellness-heading">计划任务</h2>
          <ul class="wellness-task-list"><li v-for="task in detail.tasks" :key="task.id"><span>{{ task.name }} <small>{{ task.sets }} 组</small></span><el-tag :type="task.done ? 'success' : 'warning'">{{ task.done ? '已完成' : '待完成' }}</el-tag></li></ul>
          <el-empty v-if="!detail.tasks.length" description="当日无计划任务" :image-size="60" />
          <h2 class="wellness-heading">自主运动</h2>
          <ul class="wellness-task-list"><li v-for="(sport, i) in detail.sports" :key="i"><span>{{ sportName[sport.sportType] }}</span><span>{{ (sport.distanceM / 1000).toFixed(2) }} 公里 · {{ Math.round(sport.durationSec / 60) }} 分钟</span></li></ul>
          <p v-if="!detail.sports.length" class="wellness-muted">暂无运动记录</p>
          <h2 class="wellness-heading">饮食摘要</h2>
          <p>{{ detail.diet.entryCount }} 条记录 · {{ Number(detail.diet.kcal).toFixed(0) }} 千卡 · 蛋白质 {{ Number(detail.diet.protein).toFixed(1) }} 克</p>
        </template>
        <el-empty v-else-if="!detailBusy" description="记录加载失败" />
      </div>
    </el-drawer>
    <el-dialog v-model="remindOpen" :title="`提醒 ${selected?.nickname || ''}`" width="min(480px, 94vw)">
      <el-input v-model="content" type="textarea" :rows="4" maxlength="300" show-word-limit />
      <template #footer><el-button @click="remindOpen = false">取消</el-button><el-button type="primary" :icon="Bell" :loading="sending" @click="send">发送提醒</el-button></template>
    </el-dialog>
  </section>
</template>
