<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Edit } from '@element-plus/icons-vue'
import { foodsApi, type Food } from '@/api/wellness'
const rows = ref<Food[]>([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const busy = ref(false)
const failed = ref(false)
const editing = ref(false)
const saving = ref(false)
const id = ref<string>()
const blank = () => ({ name: '', category: '主食', kcal: 0, protein: 0, fat: 0, carbohydrate: 0, fiber: null as number | null, source: '', enabled: true })
const form = reactive(blank())
async function load() {
  busy.value = true
  failed.value = false
  try { const result = await foodsApi.list(keyword.value, page.value); rows.value = result.items; total.value = result.total }
  catch (error) { failed.value = true; console.error('scenario=food-admin-load', { page: page.value, keyword: keyword.value }, error) }
  finally { busy.value = false }
}
function open(row?: Food) { id.value = row?.id; Object.assign(form, row || blank()); editing.value = true }
async function save() {
  if (!form.name.trim() || !form.source.trim() || !form.category.trim()) { ElMessage.warning('请填写名称、分类与数据来源'); return }
  if (saving.value) return
  saving.value = true
  try { await foodsApi.save(form, id.value); editing.value = false; ElMessage.success('食物已保存'); await load() }
  catch (error) { console.error('scenario=food-admin-save', { foodId: id.value }, error) }
  finally { saving.value = false }
}
onMounted(load)
</script>
<template>
  <section class="wellness-page">
    <div class="wellness-toolbar">
      <el-input v-model="keyword" placeholder="食物名称" clearable @keyup.enter="page = 1; load()" />
      <el-button :icon="Search" @click="page = 1; load()">查询</el-button>
      <el-button type="primary" :icon="Plus" @click="open()">新增食物</el-button>
      <span class="wellness-muted">每 100 克可食部 · 营养估算</span>
    </div>
    <el-alert v-if="failed" title="加载失败，请重试" type="error" :closable="false" />
    <el-table v-loading="busy" :data="rows" stripe>
      <el-table-column prop="name" label="食物" min-width="180" />
      <el-table-column prop="category" label="分类" width="90" />
      <el-table-column prop="kcal" label="热量 / 千卡" width="120" />
      <el-table-column prop="protein" label="蛋白质 / 克" width="115" />
      <el-table-column prop="fat" label="脂肪 / 克" width="100" />
      <el-table-column prop="carbohydrate" label="碳水 / 克" width="100" />
      <el-table-column label="纤维 / 克" width="105"><template #default="{ row }">{{ row.fiber ?? '暂无数据' }}</template></el-table-column>
      <el-table-column label="状态" width="85"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '上架' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button :icon="Edit" link type="primary" @click="open(row)">编辑</el-button></template></el-table-column>
      <template #empty>暂无食物</template>
    </el-table>
    <el-pagination class="wellness-pagination" v-model:current-page="page" :page-size="30" :total="total" layout="total, prev, pager, next" @current-change="load" />
    <el-dialog v-model="editing" :title="id ? '编辑食物' : '新增食物'" width="min(540px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="食物名称（标注生 / 熟）"><el-input v-model="form.name" maxlength="80" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" maxlength="30" /></el-form-item>
        <div class="wellness-form-grid">
          <el-form-item label="热量 / 千卡"><el-input-number v-model="form.kcal" :min="0" :max="1000" :precision="2" /></el-form-item>
          <el-form-item label="蛋白质 / 克"><el-input-number v-model="form.protein" :min="0" :max="100" :precision="2" /></el-form-item>
          <el-form-item label="脂肪 / 克"><el-input-number v-model="form.fat" :min="0" :max="100" :precision="2" /></el-form-item>
          <el-form-item label="碳水 / 克"><el-input-number v-model="form.carbohydrate" :min="0" :max="100" :precision="2" /></el-form-item>
          <el-form-item label="膳食纤维 / 克"><el-input-number v-model="form.fiber" :min="0" :max="100" :precision="2" /></el-form-item>
        </div>
        <el-form-item label="数据来源"><el-input v-model="form.source" maxlength="200" /></el-form-item>
        <el-form-item label="上架"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editing = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>
