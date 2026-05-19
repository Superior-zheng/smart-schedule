<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { http } from '../api/http'
import { TaskType } from '../utils/constants'

const morningMessage = ref('正在连接 AI 秘书...')
const stats = ref({ total: 0, today: 0 })
const userInput = ref('')
const allTasks = ref([])
const sortMode = ref('deadline')

const toDate = (value) => {
  if (!value) return null
  const d = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? null : d
}

const isSameDay = (a, b) => {
  if (!a || !b) return false
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

const fetchStats = async () => {
  try {
    const res = await http.get('/api/tasks/all')
    allTasks.value = (res.data || []).map((task) => ({ ...task, taskType: task.taskType || 1 }))
  } catch (e) {
    console.error(e)
    morningMessage.value = '获取数据失败，请先登录或检查服务状态'
    stats.value = { total: 0, today: 0 }
    allTasks.value = []
    return
  }

  const today = new Date()
  stats.value.total = allTasks.value.length
  stats.value.today = allTasks.value.filter((t) => t.status === 0 && isSameDay(toDate(t.deadline), today)).length
  getAiReport(stats.value.today)
}

const getAiReport = async (count) => {
  try {
    const res = await http.get(`/api/tasks/morning-report?taskCount=${count}`)
    morningMessage.value = res.data
  } catch {
    morningMessage.value = '今天也要稳稳推进，别让计划只停在想法里。'
  }
}

const handleAnalyze = async () => {
  if (!userInput.value.trim()) return ElMessage.warning('请输入点内容吧')
  const loading = ElLoading.service({ text: 'AI 正分析日程...' })

  try {
    const response = await http.post('/api/tasks/ai-parse', { content: userInput.value })
    if (!response.data?.id) return ElMessage.error('AI 解析结果无效，请重试')
    ElMessage.success('解析成功并存入数据库！')
    userInput.value = ''
    await fetchStats()
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.response?.data?.message || 'AI 解析失败，请稍后重试')
  } finally {
    loading.close()
  }
}

const handleCompleteTask = async (task) => {
  try {
    const payload = {
      ...task,
      status: 1 // 标记为完成
    }
    await http.put('/api/tasks', payload)
    ElMessage.success('任务已完成！')
    await fetchStats()
  } catch (e) {
    console.error(e)
    ElMessage.error('操作失败')
  }
}

const todayTasks = computed(() => {
  const today = new Date()
  const list = allTasks.value.filter((task) => task.status === 0 && isSameDay(toDate(task.deadline), today))

  if (sortMode.value === 'priority') {
    return [...list].sort((a, b) => (b.priority || 0) - (a.priority || 0))
  }

  return [...list].sort((a, b) => {
    const ta = toDate(a.deadline)?.getTime() ?? Number.MAX_SAFE_INTEGER
    const tb = toDate(b.deadline)?.getTime() ?? Number.MAX_SAFE_INTEGER
    return ta - tb
  })
})

const getPriorityTag = (p) => (p === 3 ? 'danger' : p === 2 ? 'warning' : 'info')
const getPriorityText = (p) => (p === 3 ? '重要' : p === 2 ? '普通' : '附带')
const getTypeTag = (t) => (t === TaskType.TIMED ? 'success' : 'info')
const getTypeText = (t) => (t === TaskType.TIMED ? '计时事项' : '提醒事项')

onMounted(fetchStats)
</script>

<template>
  <div class="home-container">
    <el-card shadow="hover" class="morning-card">
      <template #header>
        <div class="morning-head">
          <div>
            <div class="morning-title">每日晨报</div>
            <div class="morning-subtitle">今天也值得认真安排</div>
          </div>
          <div class="summary-badges">
            <span class="summary-pill">总任务 {{ stats.total }}</span>
            <span class="summary-pill">今日待办 {{ stats.today }}</span>
          </div>
        </div>
      </template>
      <div class="morning-text">“ {{ morningMessage }} ”</div>
    </el-card>

    <el-card shadow="hover" class="parse-card">
      <template #header>
        <div class="parse-header">
          <div>
            <div class="parse-title">智能解析并保存</div>
            <div class="parse-subtitle">一句话输入，自动识别优先级、任务类型和时间安排</div>
          </div>
          <el-select v-model="sortMode" style="width: 220px;">
            <el-option label="今日任务按截止时间排序" value="deadline" />
            <el-option label="今日任务按优先级排序" value="priority" />
          </el-select>
        </div>
      </template>

      <el-input
        v-model="userInput"
        type="textarea"
        :rows="4"
        placeholder="例如：今晚19点听网课40分钟，睡前记得吃药，明早去超市买菜"
        class="parse-input"
      />

      <div class="parse-actions">
        <el-button type="primary" size="large" @click="handleAnalyze">智能解析并保存</el-button>
      </div>

      <el-divider />

      <div class="todo-title">今日待办任务</div>
      <div v-if="todayTasks.length === 0" class="empty-state">今天暂无待办，继续保持！</div>
      <div v-else class="today-list">
        <div v-for="task in todayTasks" :key="task.id" class="today-item">
          <div class="today-main">
            <div class="today-tags">
              <el-tag :type="getPriorityTag(task.priority)" size="small">{{ getPriorityText(task.priority) }}</el-tag>
              <el-tag :type="getTypeTag(task.taskType)" size="small" effect="plain">{{ getTypeText(task.taskType) }}</el-tag>
            </div>
            <div class="today-task-title">{{ task.title }}</div>
          </div>
          <div class="today-actions">
            <span class="deadline">{{ task.deadline ? String(task.deadline).replace('T', ' ') : '未设置截止时间' }}</span>
            <el-button type="success" size="small" @click="handleCompleteTask(task)">完成</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.home-container { 
  padding: 24px; 
  max-width: 1200px; 
  margin: 0 auto;
}
.morning-card {
  background: linear-gradient(135deg, #20324f 0%, #35588d 55%, #4b7ed1 100%);
  color: #fff;
  border: none;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(32, 50, 79, 0.25);
}
.morning-head { display: flex; justify-content: space-between; align-items: center; gap: 20px; flex-wrap: wrap; }
.morning-title { font-size: 20px; font-weight: 700; }
.morning-subtitle { margin-top: 4px; color: rgba(255,255,255,0.85); font-size: 13px; }
.summary-badges { display: flex; gap: 10px; flex-wrap: wrap; }
.summary-pill {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(6px);
  font-size: 13px;
  font-weight: 500;
}
.morning-text {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.6;
  text-align: center;
  padding: 12px 8px 6px;
}
.parse-card { 
  margin-top: 18px; 
  border-radius: 18px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.parse-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; }
.parse-title { font-size: 18px; font-weight: 700; }
.parse-subtitle { margin-top: 4px; color: var(--text-secondary, #909399); font-size: 13px; }
.parse-input { margin-top: 8px; }
.parse-actions { display: flex; justify-content: flex-end; margin-top: 14px; }
.todo-title { 
  font-weight: 700; 
  margin-bottom: 12px;
  font-size: 16px;
  color: var(--text-primary, #303133);
}
.empty-state { 
  color: var(--text-secondary, #909399); 
  padding: 12px 0;
  text-align: center;
}
.today-list { display: grid; gap: 10px; }
.today-item {
  border: 1px solid var(--border-color, #e4e7ed);
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
  transition: all 0.3s ease;
}
.today-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.today-main { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; flex: 1; }
.today-tags { display: flex; gap: 8px; }
.today-task-title { font-weight: 600; color: var(--text-primary, #303133); }
.today-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.deadline { 
  color: var(--text-secondary, #909399); 
  font-size: 13px; 
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* 暗色模式适配 */
:root.dark .today-item {
  background: linear-gradient(180deg, #2a2f3e 0%, #242938 100%);
  border-color: var(--border-color, #3a3f4b);
}
:root.dark .today-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
:root.dark .today-task-title {
  color: var(--text-primary, #e5eaf3);
}
:root.dark .deadline {
  color: var(--text-secondary, #a3a6ad);
}
</style>
