<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'

const isAdmin = computed(() => String(sessionStorage.getItem('role') || '0') === '1')
const allTasks = ref([])
const loading = ref(false)
const statsPage = ref(0)
const chartRef = ref(null)
let chart = null
const timeRange = ref('all') // 'week', 'month', 'all'

const fetchTasks = async () => {
  loading.value = true
  try {
    const res = await http.get('/api/tasks/all')
    const tasks = (res.data || []).map((task) => ({ ...task, taskType: task.taskType || 1 }))
    
    // 根据时间范围过滤
    const now = new Date()
    let filtered = tasks
    if (timeRange.value === 'week') {
      const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
      filtered = tasks.filter(t => {
        const createTime = t.createTime ? new Date(t.createTime.replace(' ', 'T')) : null
        return createTime && createTime >= weekAgo
      })
    } else if (timeRange.value === 'month') {
      const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
      filtered = tasks.filter(t => {
        const createTime = t.createTime ? new Date(t.createTime.replace(' ', 'T')) : null
        return createTime && createTime >= monthAgo
      })
    }
    
    allTasks.value = filtered
    renderChart()
  } catch (e) {
    console.error(e)
    allTasks.value = []
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const completedTasks = computed(() => allTasks.value.filter((t) => t.status === 1))
const overdueTasks = computed(() => allTasks.value.filter((t) => t.status === 2))
const todoTasks = computed(() => allTasks.value.filter((t) => t.status === 0))
const learningTasks = computed(() => allTasks.value.filter((t) => t.taskType === 2))
const reminderTasks = computed(() => allTasks.value.filter((t) => t.taskType !== 2))

const totalCount = computed(() => allTasks.value.length)
const completionRate = computed(() => totalCount.value ? Math.round((completedTasks.value.length / totalCount.value) * 100) : 0)
const overdueRate = computed(() => totalCount.value ? Math.round((overdueTasks.value.length / totalCount.value) * 100) : 0)
const learningRate = computed(() => totalCount.value ? Math.round((learningTasks.value.length / totalCount.value) * 100) : 0)

const statPages = computed(() => ([
  [
    { label: '任务总数', value: totalCount.value, suffix: '项' },
    { label: '已完成', value: completedTasks.value.length, suffix: '项' },
    { label: '待处理', value: todoTasks.value.length, suffix: '项' },
    { label: '超时未完成', value: overdueTasks.value.length, suffix: '项' }
  ],
  [
    { label: '完成率', value: completionRate.value, suffix: '%' },
    { label: '超时率', value: overdueRate.value, suffix: '%' },
    { label: '计时事项占比', value: learningRate.value, suffix: '%' },
    { label: '提醒型任务', value: reminderTasks.value.length, suffix: '项' }
  ]
]))

const visibleStats = computed(() => statPages.value[statsPage.value] || [])
const canNextPage = computed(() => statsPage.value < statPages.value.length - 1)
const canPrevPage = computed(() => statsPage.value > 0)

const nextPage = () => {
  if (canNextPage.value) statsPage.value += 1
}
const prevPage = () => {
  if (canPrevPage.value) statsPage.value -= 1
}

const getResultText = (status) => status === 1 ? '已完成' : status === 2 ? '超时未完成' : '未知'
const getResultTag = (status) => status === 1 ? 'success' : status === 2 ? 'danger' : 'info'
const getTypeText = (taskType) => taskType === 2 ? '计时事项' : '提醒事项'

const restoreTask = async (row) => {
  try {
    const payload = { ...row, status: 0 }
    await http.put('/api/tasks', payload)
    ElMessage.success('已恢复到任务管理')
    await fetchTasks()
  } catch (e) {
    console.error(e)
    ElMessage.error('恢复失败')
  }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        label: { show: true, formatter: '{b}\n{d}%' },
        data: [
          { value: completedTasks.value.length, name: '已完成' },
          { value: todoTasks.value.length, name: '待处理' },
          { value: overdueTasks.value.length, name: '超时未完成' }
        ]
      }
    ]
  })
}

const handleResize = () => {
  chart?.resize()
}

onMounted(() => {
  fetchTasks()
  window.addEventListener('resize', handleResize)
})

watch(timeRange, () => {
  fetchTasks()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div style="padding: 28px; max-width: 1200px; margin: 0 auto;">
    <div style="display:flex; justify-content: space-between; align-items:center; margin-bottom: 18px; gap: 12px; flex-wrap: wrap;">
      <h2 style="margin: 0; color: #303133;">数据统计</h2>
      <div style="display:flex; gap: 10px; align-items: center;">
        <el-select v-model="timeRange" placeholder="选择时间范围" style="width: 150px;">
          <el-option label="近一周" value="week" />
          <el-option label="近一月" value="month" />
          <el-option label="从开始到现在" value="all" />
        </el-select>
        <el-button :disabled="!canPrevPage" @click="prevPage">上一页</el-button>
        <el-button :disabled="!canNextPage" @click="nextPage">下一页</el-button>
      </div>
    </div>

    <el-card style="margin-bottom: 16px;">
      <div ref="chartRef" style="height: 320px;"></div>
    </el-card>

    <el-card style="margin-bottom: 16px;">
      <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px;">
        <el-statistic v-for="item in visibleStats" :key="item.label" :title="item.label" :value="item.value">
          <template #suffix>{{ item.suffix }}</template>
        </el-statistic>
      </div>
    </el-card>

    <el-table v-loading="loading" :data="allTasks.filter(t => t.status === 1 || t.status === 2)" style="width: 100%;" border stripe table-layout="auto">
      <el-table-column v-if="isAdmin" label="归属用户" width="110">
        <template #default="scope">
          <span style="font-variant-numeric: tabular-nums;">{{ scope.row.owner }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="title" label="任务标题" min-width="200" />
      <el-table-column label="类型" width="100">
        <template #default="scope">{{ getTypeText(scope.row.taskType) }}</template>
      </el-table-column>
      <el-table-column prop="content" label="详细内容" min-width="260" />
      <el-table-column label="截止时间" min-width="200">
        <template #default="scope">
          <span>{{ scope.row.deadline ? String(scope.row.deadline).replace('T', ' ') : '未设置' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="130">
        <template #default="scope">
          <el-tag :type="getResultTag(scope.row.status)">{{ getResultText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button size="small" @click="restoreTask(scope.row)">恢复到任务管理</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
