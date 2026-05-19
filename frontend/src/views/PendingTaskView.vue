<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
import { formatYmd } from '../utils/date'
import { Search, Warning } from '@element-plus/icons-vue'
import { TaskStatus, TaskType, DeadlineStatus } from '../utils/constants'
import { getDeadlineStatus, getDeadlineStyle } from '../utils/task'

const taskList = ref([])
const editDialogVisible = ref(false)
const addDialogVisible = ref(false)
const editingTask = ref({})
const newTask = ref({
  title: '',
  content: '',
  deadline: '',
  durationMinutes: 25,
  taskType: TaskType.TIMED
})
const searchQuery = ref('')
const loading = ref(false)
const isAdmin = computed(() => String(sessionStorage.getItem('role') || '0') === '1')

const activeTimerTaskId = ref(null)
const remainingSeconds = ref(0)
const timerRunning = ref(false)
let timerHandle = null

const todayKey = () => `smart_schedule_timer_${formatYmd(new Date())}`

const loadTodayTimers = () => {
  try {
    const raw = localStorage.getItem(todayKey())
    const parsed = raw ? JSON.parse(raw) : {}
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

const saveTodayTimers = (map) => {
  try {
    localStorage.setItem(todayKey(), JSON.stringify(map || {}))
  } catch {
    // ignore storage failures
  }
}

const addSpentSeconds = (taskId, deltaSeconds) => {
  if (!taskId) return
  const delta = Math.max(0, Number(deltaSeconds) || 0)
  if (!delta) return
  const map = loadTodayTimers()
  const key = String(taskId)
  map[key] = (Number(map[key]) || 0) + delta
  saveTodayTimers(map)
}

const formatMmSs = (seconds) => {
  const s = Math.max(0, Number(seconds) || 0)
  const m = Math.floor(s / 60).toString().padStart(2, '0')
  const ss = Math.floor(s % 60).toString().padStart(2, '0')
  return `${m}:${ss}`
}

const formatHhMm = (seconds) => {
  const s = Math.max(0, Number(seconds) || 0)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  if (h <= 0) return `${m} 分钟`
  return `${h} 小时 ${m} 分钟`
}

const stopTimer = () => {
  timerRunning.value = false
  if (timerHandle) clearInterval(timerHandle)
  timerHandle = null
}

const resetTimer = () => {
  stopTimer()
  activeTimerTaskId.value = null
  remainingSeconds.value = 0
}

const startOrToggleTimerForTask = (task) => {
  if ((task?.taskType || TaskType.REMINDER) !== TaskType.TIMED) {
    return ElMessage.warning('这里只展示计时事项')
  }

  const duration = Number(task?.durationMinutes)
  if (!duration || Number.isNaN(duration) || duration <= 0) {
    return ElMessage.warning('该计时事项未设置计划用时')
  }

  if (activeTimerTaskId.value !== task.id) {
    stopTimer()
    activeTimerTaskId.value = task.id
    remainingSeconds.value = Math.round(duration * 60)
    timerRunning.value = true
  } else {
    timerRunning.value = !timerRunning.value
  }

  if (timerRunning.value && !timerHandle) {
    timerHandle = setInterval(() => {
      if (!timerRunning.value) return
      if (remainingSeconds.value > 0) {
        remainingSeconds.value -= 1
        addSpentSeconds(activeTimerTaskId.value, 1)
      } else {
        stopTimer()
        ElMessage.success('计时完成！')
      }
    }, 1000)
  }

  if (!timerRunning.value) stopTimer()
}

const todayTimedTotalSeconds = computed(() => {
  const map = loadTodayTimers()
  return Object.keys(map).reduce((sum, key) => sum + (Number(map[key]) || 0), 0)
})

const fetchTasks = async () => {
  loading.value = true
  try {
    const response = await http.get('/api/tasks/all')
    taskList.value = (response.data || [])
      .filter((t) => t.status === TaskStatus.TODO && (t.taskType || TaskType.REMINDER) === TaskType.TIMED && Number(t.durationMinutes) > 0)
      .map((t) => ({ ...t, taskType: t.taskType || TaskType.TIMED }))
  } catch (e) {
    console.error(e)
    ElMessage.error('获取事项失败（请检查是否已登录/后端是否启动）')
    taskList.value = []
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await http.delete(`/api/tasks/${id}`)
    ElMessage.success('事项已删除')
    await fetchTasks()
  } catch {
    ElMessage.error('删除失败')
  }
}

const openAddDialog = () => {
  newTask.value = {
    title: '',
    content: '',
    deadline: '',
    durationMinutes: 25,
    taskType: TaskType.TIMED
  }
  addDialogVisible.value = true
}

const submitAdd = async () => {
  if (!newTask.value.title.trim()) {
    return ElMessage.warning('请输入事件标题')
  }
  if (!newTask.value.durationMinutes || newTask.value.durationMinutes <= 0) {
    return ElMessage.warning('请输入有效的计划用时')
  }
  try {
    const payload = {
      ...newTask.value,
      status: TaskStatus.TODO,
      durationMinutes: normalizeDurationMinutes(newTask.value.durationMinutes)
    }
    await http.post('/api/tasks', payload)
    ElMessage.success('添加成功')
    addDialogVisible.value = false
    await fetchTasks()
  } catch {
    ElMessage.error('添加失败')
  }
}

const openEditDialog = (row) => {
  editingTask.value = { ...row, taskType: TaskType.TIMED }
  editDialogVisible.value = true
}

const normalizeDurationMinutes = (val) => {
  if (val === '' || val === undefined || val === null) return null
  const n = Number(val)
  if (!Number.isFinite(n)) return null
  const rounded = Math.round(n)
  return rounded > 0 ? rounded : null
}

const submitEdit = async () => {
  try {
    const payload = {
      ...editingTask.value,
      taskType: TaskType.TIMED,
      durationMinutes: normalizeDurationMinutes(editingTask.value.durationMinutes)
    }
    await http.put('/api/tasks', payload)
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    await fetchTasks()
  } catch {
    ElMessage.error('修改失败')
  }
}

const handleStatusChange = async (row, val) => {
  row.status = val ? TaskStatus.DONE : TaskStatus.TODO
  try {
    await http.put('/api/tasks', {
      ...row,
      taskType: TaskType.TIMED,
      durationMinutes: normalizeDurationMinutes(row.durationMinutes)
    })
    ElMessage.success(row.status === TaskStatus.DONE ? '已标记为完成' : '已设为待办')
    await fetchTasks()
  } catch {
    ElMessage.error('状态更新失败')
  }
}

const filteredTaskList = computed(() => {
  const list = taskList.value
  if (!searchQuery.value) return list
  const query = searchQuery.value.toLowerCase()
  return list.filter((task) =>
    (task.title && task.title.toLowerCase().includes(query)) ||
    (task.content && task.content.toLowerCase().includes(query))
  )
})

onMounted(fetchTasks)
onUnmounted(resetTimer)
</script>

<template>
  <div style="padding: 28px; max-width: 1200px; margin: 0 auto;">
    <div style="display:flex; justify-content:flex-end; align-items:center; gap:12px; flex-wrap: wrap; margin-bottom: 18px;">
      <el-button type="primary" @click="openAddDialog">新增计时事项</el-button>
      <el-input
        v-model="searchQuery"
        placeholder="搜索计时事项标题或内容..."
        :prefix-icon="Search"
        clearable
        style="width: 320px;"
      />
    </div>

    <el-table v-loading="loading" :data="filteredTaskList" style="width: 100%" border stripe table-layout="auto">
      <el-table-column label="状态" width="60">
        <template #default="scope">
          <el-checkbox :model-value="scope.row.status === TaskStatus.DONE" @change="(val) => handleStatusChange(scope.row, val)" />
        </template>
      </el-table-column>

      <el-table-column v-if="isAdmin" label="归属用户" width="110">
        <template #default="scope">
          <span style="font-variant-numeric: tabular-nums;">{{ scope.row.owner }}</span>
        </template>
      </el-table-column>

      <el-table-column label="计时事项" min-width="220">
        <template #default="scope">
          <span style="font-weight: 600;">{{ scope.row.title }}</span>
        </template>
      </el-table-column>

      <el-table-column label="截止时间" min-width="200">
        <template #default="scope">
          <div :style="getDeadlineStyle(scope.row)">
            <el-icon v-if="getDeadlineStatus(scope.row.deadline, scope.row.status) === DeadlineStatus.OVERDUE" style="vertical-align: middle; margin-right: 4px;">
              <Warning />
            </el-icon>
            <span>{{ scope.row.deadline ? scope.row.deadline.replace('T', ' ') : '未设置' }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="计划用时" width="110">
        <template #default="scope">
          <span>{{ scope.row.durationMinutes }} 分钟</span>
        </template>
      </el-table-column>

      <el-table-column label="计时器" width="220">
        <template #default="scope">
          <div style="display:flex; align-items:center; gap:10px; justify-content:flex-start;">
            <el-button size="small" type="primary" @click="startOrToggleTimerForTask(scope.row)">
              <span v-if="activeTimerTaskId !== scope.row.id">开始</span>
              <span v-else>{{ timerRunning ? '暂停' : '继续' }}</span>
            </el-button>
            <el-button size="small" :disabled="activeTimerTaskId !== scope.row.id" @click="resetTimer">重置</el-button>
            <span v-if="activeTimerTaskId === scope.row.id" style="font-variant-numeric: tabular-nums;">
              {{ formatMmSs(remainingSeconds) }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="content" label="详细内容" min-width="220" />

      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="addDialogVisible" title="新增计时事项" width="500px">
      <el-form :model="newTask" label-width="80px">
        <el-form-item label="事件标题" required>
          <el-input v-model="newTask.title" placeholder="请输入事件标题" />
        </el-form-item>
        <el-form-item label="详细内容">
          <el-input v-model="newTask.content" type="textarea" placeholder="可选" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-input v-model="newTask.deadline" placeholder="yyyy-MM-dd HH:mm:ss（可选）" />
        </el-form-item>
        <el-form-item label="计划用时" required>
          <el-input v-model="newTask.durationMinutes" placeholder="分钟" style="width: 220px;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑计时事项" width="500px">
      <el-form :model="editingTask" label-width="80px">
        <el-form-item label="事件标题">
          <el-input v-model="editingTask.title" />
        </el-form-item>
        <el-form-item label="详细内容">
          <el-input v-model="editingTask.content" type="textarea" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-input v-model="editingTask.deadline" placeholder="yyyy-MM-dd HH:mm:ss" />
        </el-form-item>
        <el-form-item label="计划用时">
          <el-input v-model="editingTask.durationMinutes" placeholder="分钟" style="width: 220px;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>
