// 任务状态常量
export const TaskStatus = {
  TODO: 0,
  DONE: 1,
  OVERDUE: 2
}

// 任务类型常量
export const TaskType = {
  REMINDER: 1,   // 提醒事项
  TIMED: 2       // 计时事项
}

// 截止日期状态
export const DeadlineStatus = {
  NORMAL: 0,
  TODAY_EXPIRED: 1,  // 今天过期
  OVERDUE: 2         // 已过期
}
