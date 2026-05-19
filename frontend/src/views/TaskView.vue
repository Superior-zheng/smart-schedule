<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
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
  priority: 2,
  taskType: TaskType.REMINDER
})
const searchQuery = ref('')
const loading = ref(false)
const isAdmin = computed(() => String(sessionStorage.getItem('role') || '0') === '1')

const fetchTasks = async () => {
  loading.value = true
  try {
    const response = await http.get('/api/tasks/all')
    taskList.value = (response.data || [])
      .filter((t) => t.status === TaskStatus.TODO && (t.taskType || TaskType.REMINDER) === TaskType.REMINDER)
      .map((t) => ({ ...t, taskType: t.taskType || TaskType.REMINDER }))
  } catch (e) {
    console.error(e)
    ElMessage.error('获取清单失败（请检查是否已登录/后端是否启动）')
    taskList.value = []
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await http.delete(`/api/tasks/${id}`)
    ElMessage.success('任务已删除')
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
    priority: 2,
    taskType: TaskType.REMINDER
  }
  addDialogVisible.value = true
}

const submitAdd = async () => {
  if (!newTask.value.title.trim()) {
    return ElMessage.warning('请输入事件标题')
  }
  try {
    const payload = {
      ...newTask.value,
      status: TaskStatus.TODO,
      durationMinutes: null
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
  editingTask.value = {
    ...row,
    taskType: row.taskType || TaskType.REMINDER
  }
  editDialogVisible.value = true
}

const submitEdit = async () => {
  try {
    const payload = {
      ...editingTask.value,
      taskType: TaskType.REMINDER,
      durationMinutes: null
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
      taskType: TaskType.REMINDER,
      durationMinutes: null
    })
    ElMessage.success(row.status === TaskStatus.DONE ? '已标记为完成' : '已设为待办')
    await fetchTasks()
  } catch {
    ElMessage.error('状态更新失败')
  }
}

const filteredTaskList = computed(() => {
  if (!searchQuery.value) return taskList.value
  const query = searchQuery.value.toLowerCase()
  return taskList.value.filter((task) =>
    (task.title && task.title.toLowerCase().includes(query)) ||
    (task.content && task.content.toLowerCase().includes(query))
  )
})

onMounted(fetchTasks)
</script>

<template>
  <div style="padding: 28px; max-width: 1200px; margin: 0 auto;">
    <div style="display:flex; justify-content:flex-end; margin-bottom: 18px; gap: 12px;">
      <el-button type="primary" @click="openAddDialog">新增提醒事项</el-button>
      <el-input
        v-model="searchQuery"
        placeholder="搜索提醒事项标题或内容..."
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

      <el-table-column label="代办事项" min-width="220">
        <template #default="scope">
          <span style="font-weight: 600;">{{ scope.row.title }}</span>
        </template>
      </el-table-column>

      <el-table-column label="优先级" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.priority === 3 ? 'danger' : scope.row.priority === 2 ? 'warning' : 'info'" size="small">
            {{ scope.row.priority === 3 ? '高' : scope.row.priority === 2 ? '中' : '低' }}
          </el-tag>
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

      <el-table-column prop="content" label="详细内容" min-width="260" />

      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="addDialogVisible" title="新增提醒事项" width="500px">
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
        <el-form-item label="优先级">
          <el-select v-model="newTask.priority" style="width: 100%;">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑提醒事项" width="500px">
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
        <el-form-item label="优先级">
          <el-select v-model="editingTask.priority" style="width: 100%;">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>
