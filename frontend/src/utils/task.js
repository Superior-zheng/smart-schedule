import { TaskStatus, DeadlineStatus } from './constants'

/**
 * 获取截止日期状态
 * @param {string} deadline 截止日期字符串
 * @param {number} status 任务状态
 * @returns {number} DeadlineStatus
 */
export const getDeadlineStatus = (deadline, status) => {
  if (!deadline || status === TaskStatus.DONE) return DeadlineStatus.NORMAL
  const now = new Date()
  const target = new Date(String(deadline).replace(' ', 'T'))
  if (target < now) {
    return now.toLocaleDateString() === target.toLocaleDateString()
      ? DeadlineStatus.TODAY_EXPIRED
      : DeadlineStatus.OVERDUE
  }
  return DeadlineStatus.NORMAL
}

/**
 * 获取截止日期样式
 * @param {object} row 任务行数据
 * @returns {object} CSS样式对象
 */
export const getDeadlineStyle = (row) => {
  const status = getDeadlineStatus(row.deadline, row.status)
  if (row.status === TaskStatus.DONE) {
    return { color: '#999', textDecoration: 'line-through' }
  }
  if (status === DeadlineStatus.OVERDUE) {
    return { color: '#F56C6C', fontWeight: 'bold' }
  }
  if (status === DeadlineStatus.TODAY_EXPIRED) {
    return { color: '#E6A23C' }
  }
  return {}
}
