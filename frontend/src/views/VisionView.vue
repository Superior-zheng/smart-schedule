<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'

const items = ref([])
const loading = ref(false)
const todayHistory = ref(null)

const form = ref({
  name: '',
  date: '',
  mode: 'countdown',
  repeat: 'none'
})

const showAdd = ref(false)
const editDialogVisible = ref(false)
const editingItem = ref(null)

const fetchItems = async () => {
  loading.value = true
  try {
    const res = await http.get('/api/vision/items')
    items.value = (res.data || []).map(i => ({
      ...i,
      date: i.targetDate,
      mode: i.mode || 'countdown',
      repeat: i.repeatMode || 'none'
    }))
  } catch (e) {
    console.error(e)
    items.value = []
  } finally {
    loading.value = false
  }
}

const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate())
const daysDiff = (a, b) => Math.floor((startOfDay(a).getTime() - startOfDay(b).getTime()) / (24 * 60 * 60 * 1000))

const parseDate = (dateText) => {
  const d = new Date(`${dateText}T00:00:00`)
  return Number.isNaN(d.getTime()) ? null : d
}

const addItem = async () => {
  const name = form.value.name.trim()
  const date = form.value.date
  if (!name) return ElMessage.warning('请输入名称')
  if (!date) return ElMessage.warning('请选择日期')

  try {
    const res = await http.post('/api/vision/items', {
      name,
      targetDate: date,
      mode: form.value.mode,
      repeatMode: form.value.mode === 'countup' ? form.value.repeat : 'none'
    })
    if (!res.data?.ok) return ElMessage.error(res.data?.message || '添加失败')

    form.value.name = ''
    form.value.date = ''
    form.value.mode = 'countdown'
    form.value.repeat = 'none'
    showAdd.value = false
    await fetchItems()
    ElMessage.success('已添加')
  } catch (e) {
    console.error(e)
    ElMessage.error('添加失败')
  }
}

const openEditDialog = (item) => {
  editingItem.value = {
    id: item.id,
    name: item.name,
    date: item.date,
    mode: item.mode || 'countdown',
    repeat: item.repeat || 'none'
  }
  editDialogVisible.value = true
}

const saveEdit = async () => {
  const item = editingItem.value
  if (!item) return
  const name = (item.name || '').trim()
  if (!name) return ElMessage.warning('请输入名称')
  if (!item.date) return ElMessage.warning('请选择日期')

  try {
    const res = await http.put('/api/vision/items', {
      id: item.id,
      name,
      targetDate: item.date,
      mode: item.mode,
      repeatMode: item.mode === 'countup' ? item.repeat : 'none'
    })
    if (!res.data?.ok) return ElMessage.error(res.data?.message || '更新失败')
    await fetchItems()
    editDialogVisible.value = false
    ElMessage.success('已更新')
  } catch (e) {
    console.error(e)
    ElMessage.error('更新失败')
  }
}

const removeCurrentItem = async () => {
  if (!editingItem.value) return
  try {
    await http.delete(`/api/vision/items/${editingItem.value.id}`)
    await fetchItems()
    editDialogVisible.value = false
    ElMessage.success('已删除')
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  }
}

const getMonthlyOccurrence = (year, monthIndex, dayOfMonth) => {
  const maxDay = new Date(year, monthIndex + 1, 0).getDate()
  const d = Math.min(dayOfMonth, maxDay)
  return new Date(year, monthIndex, d)
}

const getNextCycleDate = (item, today) => {
  const start = parseDate(item.date)
  if (!start) return null

  const repeat = item.repeat || 'none'
  if (repeat === 'weekly') {
    const targetWeekday = start.getDay()
    const currentWeekday = today.getDay()
    const delta = (targetWeekday - currentWeekday + 7) % 7
    const next = new Date(today)
    next.setDate(today.getDate() + delta)
    return next
  }

  if (repeat === 'monthly') {
    const targetDay = start.getDate()
    let next = getMonthlyOccurrence(today.getFullYear(), today.getMonth(), targetDay)
    if (startOfDay(next).getTime() < startOfDay(today).getTime()) {
      const nextMonth = new Date(today.getFullYear(), today.getMonth() + 1, 1)
      next = getMonthlyOccurrence(nextMonth.getFullYear(), nextMonth.getMonth(), targetDay)
    }
    return next
  }

  if (repeat === 'yearly') {
    const targetMonth = start.getMonth()
    const targetDay = start.getDate()
    let next = getMonthlyOccurrence(today.getFullYear(), targetMonth, targetDay)
    if (startOfDay(next).getTime() < startOfDay(today).getTime()) {
      next = getMonthlyOccurrence(today.getFullYear() + 1, targetMonth, targetDay)
    }
    return next
  }

  return null
}

const cards = computed(() => {
  const today = startOfDay(new Date())

  return items.value.map((i) => {
    const start = parseDate(i.date)
    if (!start) return { ...i, text: '日期无效', nextText: '' }

    const baseDiff = Math.max(0, daysDiff(today, start))

    if ((i.mode || 'countdown') === 'countdown') {
      const toTarget = daysDiff(start, today)
      const text = toTarget > 0 ? `还有 ${toTarget} 天` : toTarget === 0 ? '就是今天' : `已过去 ${Math.abs(toTarget)} 天`
      return { ...i, text, nextText: '' }
    }

    const repeat = i.repeat || 'none'
    let nextText = ''
    if (repeat !== 'none') {
      const next = getNextCycleDate(i, today)
      const nextDays = next ? Math.max(0, daysDiff(next, today)) : 0
      nextText = `距离下一次 ${nextDays} 天`
    }
    return { ...i, text: `距今 ${baseDiff} 天`, nextText }
  })
})

const loadHistoryToday = async () => {
  try {
    const res = await http.get('/api/tasks/history-today')
    todayHistory.value = res.data || null
  } catch (e) {
    todayHistory.value = null
  }
}

onMounted(() => {
  fetchItems()
  loadHistoryToday()
})
</script>

<template>
  <div class="vision-page">
    <el-card style="margin-bottom: 16px;">
      <template #header>
        <div style="font-weight: 700;">历史上的今日</div>
      </template>
      <div v-if="todayHistory">
        <div style="font-size: 18px; font-weight: 700; margin-bottom: 8px;">
          {{ todayHistory.year }}年{{ Number((todayHistory.monthDay || '01-01').split('-')[0]) }}月{{ Number((todayHistory.monthDay || '01-01').split('-')[1]) }}日 · {{ todayHistory.title }}
        </div>
        <div style="color: var(--text-regular, #606266);">{{ todayHistory.summary }}</div>
      </div>
      <div v-else style="color: var(--text-secondary, #909399);">今日事件加载中...</div>
    </el-card>

    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 16px;">
      <h2 style="margin:0;">愿景板 / 倒数日</h2>
      <el-button type="primary" @click="showAdd = !showAdd">{{ showAdd ? '收起' : '添加' }}</el-button>
    </div>

    <el-card v-if="showAdd" style="margin-bottom: 16px;">
      <el-form inline>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：考研" style="width: 180px;" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="模式">
          <el-select v-model="form.mode" style="width: 120px;">
            <el-option label="倒计时" value="countdown" />
            <el-option label="正计时" value="countup" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.mode === 'countup'" label="循环">
          <el-select v-model="form.repeat" style="width: 120px;">
            <el-option label="不循环" value="none" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
            <el-option label="每年" value="yearly" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addItem">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-if="cards.length === 0" class="empty-wrap">
      <el-button type="primary" size="large" @click="showAdd = true">添加倒数日</el-button>
    </div>

    <div v-else class="grid">
      <el-card
        v-for="item in cards"
        :key="item.id"
        class="vision-card"
        shadow="hover"
        @click="openEditDialog(item)"
      >
        <div>
          <div class="title">{{ item.name }}</div>
          <div class="date">{{ item.date }}</div>
        </div>

        <div class="days">{{ item.text }}</div>
        <div v-if="item.nextText" class="next-days">{{ item.nextText }}</div>
      </el-card>
    </div>

    <el-dialog v-model="editDialogVisible" title="编辑事项" width="540px">
      <el-form v-if="editingItem" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="editingItem.name" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="editingItem.date" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="模式">
          <el-select v-model="editingItem.mode" style="width: 100%;">
            <el-option label="倒计时" value="countdown" />
            <el-option label="正计时" value="countup" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingItem.mode === 'countup'" label="循环">
          <el-select v-model="editingItem.repeat" style="width: 100%;">
            <el-option label="不循环" value="none" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
            <el-option label="每年" value="yearly" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div style="display:flex; justify-content:space-between; width:100%;">
          <el-button type="danger" plain @click="removeCurrentItem">删除</el-button>
          <div style="display:flex; gap: 8px;">
            <el-button @click="editDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="saveEdit">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.vision-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
.empty-wrap { min-height: 280px; display:flex; justify-content:center; align-items:center; }
.grid { display:grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 14px; }
.vision-card { border-radius: 14px; cursor: pointer; }
.title { font-size: 18px; font-weight: 700; color: var(--text-primary, #303133); }
.date { color: var(--text-secondary, #909399); margin-top: 6px; }
.days { margin-top: 16px; font-size: 20px; font-weight: 700; color: #409EFF; }
.next-days { margin-top: 6px; color: var(--text-regular, #606266); font-size: 13px; }
</style>
