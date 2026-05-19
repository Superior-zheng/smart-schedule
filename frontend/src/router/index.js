import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import TaskView from '../views/TaskView.vue'
import PendingTaskView from '../views/PendingTaskView.vue'
import StatsView from '../views/StatsView.vue'
import CheckinView from '../views/CheckinView.vue'
import VisionView from '../views/VisionView.vue'
import LibraryView from '../views/LibraryView.vue'
import SettingsView from '../views/SettingsView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'

const routes = [
  { path: '/', component: HomeView },
  { path: '/tasks', redirect: '/tasks/todo' },
  { path: '/tasks/pending', component: PendingTaskView },
  { path: '/tasks/todo', component: TaskView },
  { path: '/stats', component: StatsView },
  { path: '/checkin', component: CheckinView },
  { path: '/vision', component: VisionView },
  { path: '/library', component: LibraryView },
  { path: '/settings', component: SettingsView },
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const publicPaths = ['/login', '/register']
  if (publicPaths.includes(to.path)) return true
  const token = sessionStorage.getItem('token')
  if (!token) return '/login'
  return true
})

export default router
