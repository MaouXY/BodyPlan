<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { createTag, deleteTag, fetchTags, updateTag } from '@/api/admin'
import type { AdminExerciseTag, TagForm } from '@/types/admin'
import { TAG_TYPE_LABELS, tagLabelOf } from '@/utils/dict'

const TAG_TYPES = ['PART', 'SPORT_TYPE', 'SCENE', 'EQUIPMENT', 'DIFFICULTY']

const loading = ref(false)
const rows = ref<AdminExerciseTag[]>([])
const tagType = ref<string | undefined>(undefined)

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<TagForm>({
  tagType: 'PART',
  tagCode: '',
  tagName: '',
  sortNo: 0,
  status: 1,
})

async function load() {
  loading.value = true
  try {
    rows.value = await fetchTags(tagType.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.tagType = tagType.value ?? 'PART'
  form.tagCode = ''
  form.tagName = ''
  form.sortNo = 0
  form.status = 1
  dialogVisible.value = true
}

function openEdit(row: AdminExerciseTag) {
  editingId.value = row.id
  form.tagType = row.tagType
  form.tagCode = row.tagCode
  form.tagName = row.tagName
  form.sortNo = row.sortNo ?? 0
  form.status = row.status ?? 1
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.tagCode || !form.tagName) {
    ElMessage.warning('标签编码与名称不能为空')
    return
  }
  saving.value = true
  try {
    if (editingId.value === null) {
      await createTag({ ...form })
      ElMessage.success('标签已新增')
    } else {
      await updateTag(editingId.value, { ...form })
      ElMessage.success('标签已更新')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 失败提示由拦截器给出
  } finally {
    saving.value = false
  }
}

async function handleStatusChange(row: AdminExerciseTag) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await updateTag(row.id, {
      tagType: row.tagType,
      tagCode: row.tagCode,
      tagName: row.tagName,
      sortNo: row.sortNo ?? 0,
      status: nextStatus,
    })
    row.status = nextStatus
    ElMessage.success(nextStatus === 1 ? '标签已启用' : '标签已停用')
  } catch {
    // 失败提示由拦截器给出
  }
}

async function handleDelete(row: AdminExerciseTag) {
  try {
    await ElMessageBox.confirm(
      `确定删除标签「${row.tagName}」吗？`,
      '删除确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteTag(row.id)
    ElMessage.success('标签已删除')
    load()
  } catch {
    // 失败提示由拦截器给出
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select
          v-model="tagType"
          class="toolbar__select"
          placeholder="标签类型"
          clearable
          @change="load"
        >
          <el-option
            v-for="type in TAG_TYPES"
            :key="type"
            :label="TAG_TYPE_LABELS[type]"
            :value="type"
          />
        </el-select>
        <el-button type="primary" @click="openCreate">新增标签</el-button>
        <el-button plain @click="load">刷新</el-button>
        <span class="toolbar__hint">
          标签字典用于动作筛选与新增动作时的可选项，编码需与动作数据保持一致
        </span>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            {{ TAG_TYPE_LABELS[row.tagType] ?? row.tagType }}
          </template>
        </el-table-column>
        <el-table-column prop="tagCode" label="编码" min-width="140" />
        <el-table-column label="名称" min-width="140">
          <template #default="{ row }">
            {{ tagLabelOf(row.tagType, row.tagCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>暂无标签</template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId === null ? '新增标签' : '编辑标签'"
      width="460px"
    >
      <el-form label-width="90px">
        <el-form-item label="标签类型">
          <el-select v-model="form.tagType" :disabled="editingId !== null">
            <el-option
              v-for="type in TAG_TYPES"
              :key="type"
              :label="TAG_TYPE_LABELS[type]"
              :value="type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标签编码">
          <el-input v-model="form.tagCode" placeholder="如 ABDOMEN" />
        </el-form-item>
        <el-form-item label="标签名称">
          <el-input v-model="form.tagName" placeholder="如 腹部" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar__select {
  width: 160px;
}

.toolbar__hint {
  font-size: 12px;
  color: #8a8f99;
}
</style>
