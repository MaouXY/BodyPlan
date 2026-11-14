<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh, Delete } from '@element-plus/icons-vue'
import { coachesApi, type Coach, type Candidate } from '@/api/wellness'

const coaches = ref<Coach[]>([])
const students = ref<Candidate[]>([])
const keyword = ref('')
const busy = ref(false)
const failed = ref(false)
const adding = ref(false)
const form = reactive({ username: '', nickname: '', password: '' })
const assigning = ref('')
const selected = reactive<Record<string, string>>({})

async function load() {
  busy.value = true
  failed.value = false
  try {
    const [a, b] = await Promise.all([coachesApi.list(), coachesApi.candidates(keyword.value)])
    coaches.value = a
    students.value = b
    b.forEach(s => { selected[s.id] = s.coachId || '' })
  } catch (error) {
    failed.value = true
    console.error('scenario=coach-assignment-load', { keyword: keyword.value }, error)
  } finally { busy.value = false }
}
async function create() {
  if (!/^[a-zA-Z0-9_]{3,32}$/.test(form.username) || !form.nickname.trim() || form.password.length < 6) {
    ElMessage.warning('账号需为3~32位字母、数字或下划线，昵称必填，密码至少6位')
    return
  }
  if (busy.value) return
  busy.value = true
  try {
    await coachesApi.create(form)
    adding.value = false
    form.username = form.nickname = form.password = ''
    ElMessage.success('教练账号已创建')
    await load()
  } catch (error) { console.error('scenario=coach-create', { username: form.username }, error) }
  finally { busy.value = false }
}
async function assign(student: Candidate, remove = false) {
  if (assigning.value) return
  assigning.value = student.id
  try {
    if (remove) await coachesApi.unassign(student.id)
    else if (selected[student.id]) await coachesApi.assign(student.id, selected[student.id]!)
    else { ElMessage.warning('请选择教练'); return }
    ElMessage.success(remove ? '已解除分配' : '已保存分配')
    await load()
  } catch (error) { console.error('scenario=coach-assign', { studentId: student.id, coachId: selected[student.id] }, error) }
  finally { assigning.value = '' }
}
onMounted(load)
</script>

<template>
  <section class="wellness-page" v-loading="busy">
    <div class="wellness-toolbar">
      <el-input v-model="keyword" placeholder="学员账号 / 昵称" clearable @keyup.enter="load" />
      <el-button :icon="Search" @click="load">查询</el-button>
      <el-button :icon="Refresh" circle aria-label="刷新" @click="load" />
      <el-button type="primary" :icon="Plus" @click="adding = true">新建教练</el-button>
    </div>
    <el-alert v-if="failed" title="加载失败，请重试" type="error" :closable="false" />
    <div class="wellness-summary">
      <div><strong>{{ coaches.length }}</strong><span>教练</span></div>
      <div><strong>{{ students.filter(s => s.coachId).length }}</strong><span>当前列表已分配</span></div>
      <div><strong>{{ students.filter(s => !s.coachId).length }}</strong><span>当前列表待分配</span></div>
    </div>
    <h2 class="wellness-heading">教练名单</h2>
    <el-table :data="coaches" stripe>
      <el-table-column prop="nickname" label="教练" min-width="140" />
      <el-table-column prop="username" label="登录账号" min-width="160" />
      <el-table-column label="状态"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <template #empty>暂无教练</template>
    </el-table>
    <h2 class="wellness-heading">学员分配</h2>
    <el-table :data="students" stripe>
      <el-table-column prop="nickname" label="学员" min-width="130" />
      <el-table-column prop="username" label="账号" min-width="140" />
      <el-table-column prop="coachName" label="当前教练" min-width="130"><template #default="{ row }">{{ row.coachName || '未分配' }}</template></el-table-column>
      <el-table-column label="分配教练" min-width="200"><template #default="{ row }">
        <el-select v-model="selected[row.id]" placeholder="选择教练" filterable>
          <el-option v-for="coach in coaches.filter(c => c.status === 1)" :key="coach.id" :value="coach.id" :label="`${coach.nickname} · ${coach.username}`" />
        </el-select>
      </template></el-table-column>
      <el-table-column label="操作" width="160"><template #default="{ row }">
        <el-button link type="primary" :disabled="!!assigning" @click="assign(row)">保存</el-button>
        <el-popconfirm title="解除该学员的教练分配？" @confirm="assign(row, true)">
          <template #reference><el-button :icon="Delete" link :disabled="!row.coachId || !!assigning" aria-label="解除分配" /></template>
        </el-popconfirm>
      </template></el-table-column>
      <template #empty>暂无匹配学员</template>
    </el-table>
    <el-dialog v-model="adding" title="新建教练" width="min(480px, 94vw)">
      <el-form label-position="top" @submit.prevent="create">
        <el-form-item label="登录账号"><el-input v-model="form.username" maxlength="32" /></el-form-item>
        <el-form-item label="教练姓名"><el-input v-model="form.nickname" maxlength="64" /></el-form-item>
        <el-form-item label="初始密码"><el-input v-model="form.password" type="password" show-password maxlength="72" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="adding = false">取消</el-button><el-button type="primary" :loading="busy" @click="create">创建</el-button></template>
    </el-dialog>
  </section>
</template>
