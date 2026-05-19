<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { http } from '../api/http'

const habits = ref([])
const checkinRecords = ref({})
const loading = ref(false)

const now = new Date()
const currentMonth = ref(new Date(now.getFullYear(), now.getMonth(), 1))
const selectedHabitId = ref(null)

const newHabitName = ref('')
const newHabitColor = ref('#E6A23C')

const periodHabitId = ref(null)
const periodType = ref('week')

const fetchHabits = async () => {
  loading.value = true
  try {
    const res = await http.get('/api/checkin/habits')
    habits.value = res.data || []
    if (habits.value.length > 0 && !selectedHabitId.value) {
      selectedHabitId.value = habits.value[0].id
      periodHabitId.value = habits.value[0].id
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取打卡项失败')
    habits.value = []
  } finally {
    loading.value = false
  }
}

const fetchRecords = async () => {
  try {
    const res = await http.get('/api/checkin/records')
    const data = res.data || {}
    checkinRecords.value = {}
    for (const [date, ids] of Object.entries(data)) {
      checkinRecords.value[date] = ids
    }
  } catch (e) {
    console.error(e)
    checkinRecords.value = {}
  }
}

const fetchData = async () => {
  await fetchHabits()
  await fetchRecords()
}

const formatDateKey = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const parseDateKey = (dateKey) => {
  const d = new Date(`${dateKey}T00:00:00`)
  return Number.isNaN(d.getTime()) ? null : d
}

const todayDateKey = computed(() => formatDateKey(new Date()))

const monthLabel = computed(() => {
  const y = currentMonth.value.getFullYear()
  const m = currentMonth.value.getMonth() + 1
  return `${y}年${m}月`
})

const startWeekday = computed(() => {
  const d = currentMonth.value.getDay()
  return d === 0 ? 7 : d
})

const daysInMonth = computed(() => {
  const y = currentMonth.value.getFullYear()
  const m = currentMonth.value.getMonth()
  return new Date(y, m + 1, 0).getDate()
})

const calendarCells = computed(() => {
  const cells = []
  const y = currentMonth.value.getFullYear()
  const m = currentMonth.value.getMonth()
  const total = 42
  const first = startWeekday.value

  for (let i = 1; i < first; i += 1) {
    cells.push({ inMonth: false, dateKey: '' })
  }

  for (let d = 1; d <= daysInMonth.value; d += 1) {
    const mm = String(m + 1).padStart(2, '0')
    const dd = String(d).padStart(2, '0')
    const dateKey = `${y}-${mm}-${dd}`
    const today = new Date()
    const isToday = y === today.getFullYear() && m === today.getMonth() && d === today.getDate()
    cells.push({ inMonth: true, day: d, dateKey, isToday })
  }

  while (cells.length < total) {
    cells.push({ inMonth: false, dateKey: '' })
  }

  return cells
})

const prevMonth = () => {
  const d = currentMonth.value
  currentMonth.value = new Date(d.getFullYear(), d.getMonth() - 1, 1)
}

const nextMonth = () => {
  const d = currentMonth.value
  currentMonth.value = new Date(d.getFullYear(), d.getMonth() + 1, 1)
}

const addHabit = async () => {
  const name = newHabitName.value.trim()
  if (!name) return ElMessage.warning('请输入打卡名称')

  try {
    const res = await http.post('/api/checkin/habits', { name, color: newHabitColor.value })
    if (!res.data?.ok) return ElMessage.error(res.data?.message || '添加失败')
    newHabitName.value = ''
    await fetchHabits()
    ElMessage.success('已新增打卡项')
  } catch (e) {
    console.error(e)
    ElMessage.error('添加失败')
  }
}

const deleteHabit = async () => {
  if (!selectedHabitId.value) return ElMessage.warning('请先选择一个打卡项')
  if (habits.value.length <= 1) return ElMessage.warning('至少保留一个打卡项')

  try {
    await ElMessageBox.confirm('删除后会移除该打卡项的历史记录，是否继续？', '删除打卡项', { type: 'warning' })
  } catch {
    return
  }

  try {
    await http.delete(`/api/checkin/habits/${selectedHabitId.value}`)
    const fallback = habits.value.find(h => h.id !== selectedHabitId.value)?.id || null
    selectedHabitId.value = fallback
    periodHabitId.value = fallback
    await fetchData()
    ElMessage.success('已删除打卡项')
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  }
}

const getCheckedHabitIdsByDate = (dateKey) => {
  const arr = checkinRecords.value[dateKey]
  return Array.isArray(arr) ? arr : []
}

const isHabitCheckedOnDate = (habitId, dateKey) => {
  return getCheckedHabitIdsByDate(dateKey).includes(habitId)
}

const dayStart = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate())
const dayDiff = (a, b) => Math.floor((dayStart(a).getTime() - dayStart(b).getTime()) / 86400000)

const allCheckedDatesByHabit = (habitId) => {
  return Object.keys(checkinRecords.value)
    .filter((dateKey) => isHabitCheckedOnDate(habitId, dateKey))
    .map(parseDateKey)
    .filter(Boolean)
    .sort((a, b) => a - b)
}

const currentStreakByHabit = (habitId) => {
  const dates = allCheckedDatesByHabit(habitId)
  if (!dates.length) return 0

  const checkedSet = new Set(dates.map(formatDateKey))
  let cursor = dayStart(new Date())
  let streak = 0

  while (checkedSet.has(formatDateKey(cursor))) {
    streak += 1
    cursor.setDate(cursor.getDate() - 1)
  }

  return streak
}

const bestStreakByHabit = (habitId) => {
  const dates = allCheckedDatesByHabit(habitId)
  if (!dates.length) return 0

  let best = 1
  let cur = 1
  for (let i = 1; i < dates.length; i += 1) {
    if (dayDiff(dates[i], dates[i - 1]) === 1) {
      cur += 1
      if (cur > best) best = cur
    } else {
      cur = 1
    }
  }
  return best
}

const doTodayCheckin = async () => {
  if (!selectedHabitId.value) return ElMessage.warning('请先选择一个打卡项')

  const todayKey = todayDateKey.value
  if (isHabitCheckedOnDate(selectedHabitId.value, todayKey)) {
    return ElMessage.success('今天已经打过卡啦，继续保持！')
  }

  try {
    await http.post('/api/checkin/records', { habitId: selectedHabitId.value, date: todayKey })
    await fetchRecords()
    const streak = currentStreakByHabit(selectedHabitId.value)
    ElMessage.success(`🎉 打卡成功！已连续 ${streak} 天`)
  } catch (e) {
    console.error(e)
    ElMessage.error('打卡失败')
  }
}

const undoTodayCheckin = async () => {
  if (!selectedHabitId.value) return ElMessage.warning('请先选择一个打卡项')
  const todayKey = todayDateKey.value
  if (!isHabitCheckedOnDate(selectedHabitId.value, todayKey)) {
    return ElMessage.info('今天还没有这项打卡记录')
  }

  try {
    await http.delete('/api/checkin/records', { data: { habitId: selectedHabitId.value, date: todayKey } })
    await fetchRecords()
    ElMessage.success('已取消今日打卡')
  } catch (e) {
    console.error(e)
    ElMessage.error('取消失败')
  }
}

const habitById = (id) => habits.value.find(h => h.id === id)

const dotsByDate = (dateKey) => {
  const ids = getCheckedHabitIdsByDate(dateKey)
  return ids.map(id => habitById(id)).filter(Boolean)
}

const habitStats = computed(() => {
  const counts = habits.value.map(h => ({ ...h, days: 0 }))
  const map = Object.fromEntries(counts.map(h => [h.id, h]))

  Object.keys(checkinRecords.value).forEach(dateKey => {
    const ids = getCheckedHabitIdsByDate(dateKey)
    ids.forEach(id => {
      if (map[id]) map[id].days += 1
    })
  })

  const max = Math.max(1, ...counts.map(c => c.days))
  return counts.map(c => ({ ...c, width: `${(c.days / max) * 100}%` }))
})

const periodCount = computed(() => {
  const habitId = periodHabitId.value
  if (!habitId) return 0

  const end = new Date()
  const start = new Date(end)

  if (periodType.value === 'week') start.setDate(end.getDate() - 6)
  if (periodType.value === 'month') start.setDate(end.getDate() - 29)
  if (periodType.value === 'year') start.setDate(end.getDate() - 364)

  let count = 0
  Object.keys(checkinRecords.value).forEach((dateKey) => {
    const d = parseDateKey(dateKey)
    if (!d) return
    if (d < new Date(start.getFullYear(), start.getMonth(), start.getDate())) return
    if (d > new Date(end.getFullYear(), end.getMonth(), end.getDate())) return

    if (isHabitCheckedOnDate(habitId, dateKey)) count += 1
  })

  return count
})

const periodLabel = computed(() => {
  if (periodType.value === 'week') return '近一周'
  if (periodType.value === 'month') return '近一月'
  return '近一年'
})

const selectedHabitName = computed(() => habitById(selectedHabitId.value)?.name || '未选择')
const todayChecked = computed(() => selectedHabitId.value && isHabitCheckedOnDate(selectedHabitId.value, todayDateKey.value))

const totalHabits = computed(() => habits.value.length)
const totalCheckinTimes = computed(() => {
  let sum = 0
  Object.keys(checkinRecords.value).forEach((dateKey) => {
    sum += getCheckedHabitIdsByDate(dateKey).length
  })
  return sum
})
const activeDaysCount = computed(() => {
  return Object.keys(checkinRecords.value).filter((dateKey) => getCheckedHabitIdsByDate(dateKey).length > 0).length
})
const todayDoneCount = computed(() => getCheckedHabitIdsByDate(todayDateKey.value).length)
const selectedHabitCurrentStreak = computed(() => periodHabitId.value ? currentStreakByHabit(periodHabitId.value) : 0)
const selectedHabitBestStreak = computed(() => periodHabitId.value ? bestStreakByHabit(periodHabitId.value) : 0)
const consistencyRate = computed(() => {
  const base = periodType.value === 'week' ? 7 : periodType.value === 'month' ? 30 : 365
  return Math.min(100, Math.round((periodCount.value / base) * 100))
})

onMounted(fetchData)
</script>

<template>
  <div class="checkin-page">
    <el-card style="margin-bottom: 16px;">
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px;">
        <el-statistic title="打卡项目数" :value="totalHabits" />
        <el-statistic title="累计打卡次数" :value="totalCheckinTimes" />
        <el-statistic title="活跃打卡天数" :value="activeDaysCount" />
        <el-statistic title="今日已完成" :value="todayDoneCount" />
      </div>
    </el-card>

    <el-card v-loading="loading">
      <div class="calendar-header">
        <div class="left-actions">
          <el-button circle :icon="ArrowLeft" @click="prevMonth" />
          <span class="month-label">{{ monthLabel }}</span>
          <el-button circle :icon="ArrowRight" @click="nextMonth" />
        </div>

        <div class="right-actions" style="display:flex; gap: 8px; align-items:center; flex-wrap: wrap;">
          <el-select v-model="selectedHabitId" placeholder="打卡项" style="width: 180px;">
            <el-option v-for="h in habits" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>

          <el-input v-model="newHabitName" placeholder="新增打卡项" style="width: 180px;" />
          <el-color-picker v-model="newHabitColor" />
          <el-button @click="addHabit">新增</el-button>
          <el-button type="danger" plain @click="deleteHabit">删除</el-button>
        </div>
      </div>

      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom: 12px; gap: 10px; flex-wrap: wrap;">
        <div>
          <el-tag type="info">当前打卡项：{{ selectedHabitName }}</el-tag>
          <el-tag v-if="todayChecked" type="success" style="margin-left: 8px;">今日已打卡</el-tag>
        </div>
        <div style="display:flex; gap: 8px; flex-wrap: wrap;">
          <el-button type="primary" @click="doTodayCheckin">今日打卡</el-button>
          <el-button @click="undoTodayCheckin">取消今日打卡</el-button>
        </div>
      </div>

      <div class="week-header">
        <span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span><span>日</span>
      </div>

      <div class="calendar-grid">
        <div
          v-for="(cell, idx) in calendarCells"
          :key="idx"
          class="day-cell"
          :class="{ 'out-month': !cell.inMonth, today: cell.isToday }"
        >
          <div class="day-number">{{ cell.day || '' }}</div>
          <div class="dots">
            <span
              v-for="dot in dotsByDate(cell.dateKey)"
              :key="dot.id"
              class="dot"
              :title="dot.name"
              :style="{ background: dot.color }"
            ></span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div style="font-weight: 600;">打卡统计（累计天数）</div>
      </template>

      <div v-if="habitStats.length === 0" style="color: #909399;">暂无打卡项</div>
      <div v-for="item in habitStats" :key="item.id" class="stat-row">
        <div class="stat-name">
          <span class="habit-dot" :style="{ background: item.color }"></span>
          <span>{{ item.name }}</span>
        </div>
        <div class="stat-bar-wrap">
          <div class="stat-bar" :style="{ width: item.width, background: item.color }"></div>
        </div>
        <div class="stat-days">{{ item.days }} 天</div>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div style="font-weight: 600;">时间范围统计</div>
      </template>
      <div style="display:flex; gap: 12px; flex-wrap: wrap; align-items: center; margin-bottom: 10px;">
        <el-select v-model="periodHabitId" placeholder="选择打卡项" style="width: 220px;">
          <el-option v-for="h in habits" :key="h.id" :label="h.name" :value="h.id" />
        </el-select>
        <el-radio-group v-model="periodType">
          <el-radio-button label="week">近一周</el-radio-button>
          <el-radio-button label="month">近一月</el-radio-button>
          <el-radio-button label="year">近一年</el-radio-button>
        </el-radio-group>
      </div>
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px;">
        <el-statistic :title="`${periodLabel}打卡数量`" :value="periodCount" />
        <el-statistic title="当前连续天数" :value="selectedHabitCurrentStreak" />
        <el-statistic title="历史最长连续" :value="selectedHabitBestStreak" />
        <el-statistic title="该周期完成率(%)" :value="consistencyRate" />
      </div>
    </el-card>

  </div>
</template>

<style scoped>
.checkin-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
.calendar-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; gap: 12px; }
.left-actions { display: flex; align-items: center; gap: 10px; }
.right-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.month-label { font-weight: 700; font-size: 18px; min-width: 110px; text-align: center; }
.week-header { display: grid; grid-template-columns: repeat(7, 1fr); font-weight: 600; color: var(--text-regular, #606266); margin-bottom: 8px; text-align: center; }
.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.day-cell { border: 1px solid var(--border-color, #ebeef5); min-height: 90px; border-radius: 10px; padding: 6px; background: var(--card-bg, #fff); }
.day-cell.out-month { background: #f7f8fa; }
.day-cell.today { background: #ecf5ff; border-color: #79bbff; }
.day-number { font-weight: 600; color: var(--text-primary, #303133); }
.dots { margin-top: 8px; display: flex; gap: 6px; flex-wrap: wrap; }
.dot, .habit-dot { width: 8px; height: 8px; border-radius: 999px; display: inline-block; }
.stat-row { display: grid; grid-template-columns: 220px 1fr 80px; align-items: center; gap: 10px; margin-bottom: 10px; }
.stat-name { display: flex; align-items: center; gap: 8px; color: var(--text-primary, #303133); }
.stat-bar-wrap { background: var(--table-stripe-bg, #f2f6fc); border-radius: 999px; height: 10px; overflow: hidden; }
.stat-bar { height: 100%; border-radius: 999px; }
.stat-days { text-align: right; font-variant-numeric: tabular-nums; color: var(--text-primary, #303133); }
</style>
