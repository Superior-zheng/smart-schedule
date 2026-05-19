<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const isAuthPage = computed(() => ['/login', '/register'].includes(route.path))
const username = ref('')
const nickname = ref('')

const THEME_KEY = 'smart_schedule_theme'

const applyTheme = (mode) => {
  const isDark = mode === 'dark'
  document.documentElement.classList.toggle('dark', isDark)
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light')
}

const syncTheme = () => {
  const mode = localStorage.getItem(THEME_KEY) || 'light'
  applyTheme(mode)
}

const syncAuth = () => {
  username.value = sessionStorage.getItem('username') || ''
  nickname.value = sessionStorage.getItem('nickname') || ''
}

const displayName = computed(() => {
  if (nickname.value) return `${nickname.value}（${username.value}）`
  return username.value
})

const logout = async () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('username')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('nickname')
  window.dispatchEvent(new Event('auth-changed'))
  ElMessage.success('已退出登录')
  await router.push('/login')
}

onMounted(() => {
  syncAuth()
  syncTheme()
  window.addEventListener('auth-changed', syncAuth)
  window.addEventListener('storage', syncTheme)
  window.addEventListener('theme-changed', syncTheme)
})

onUnmounted(() => {
  window.removeEventListener('auth-changed', syncAuth)
  window.removeEventListener('storage', syncTheme)
  window.removeEventListener('theme-changed', syncTheme)
})

watch(() => route.path, () => {
  syncAuth()
})
</script>

<template>
  <router-view v-if="isAuthPage" />

  <el-container v-else class="layout-shell">
    <el-aside width="220px" class="side-nav">
      <div class="brand-box">
        <h3 class="brand-title">智能日程系统</h3>
        <div v-if="username" class="account-line">
          当前账号：{{ displayName }}
        </div>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        background-color="transparent"
        text-color="var(--menu-text)"
        active-text-color="var(--menu-active)"
        style="flex: 1;"
      >
        <el-menu-item index="/">
          <span>首页</span>
        </el-menu-item>

        <el-sub-menu index="/tasks">
          <template #title>
            <span>任务管理</span>
          </template>
          <el-menu-item index="/tasks/pending">
            <span>计时事项</span>
          </el-menu-item>
          <el-menu-item index="/tasks/todo">
            <span>提醒事项</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/stats">
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/checkin">
          <span>签到打卡</span>
        </el-menu-item>
        <el-menu-item index="/vision">
          <span>愿景板/倒数日</span>
        </el-menu-item>
        <el-menu-item index="/library">
          <span>学习资料库</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <span>设置</span>
        </el-menu-item>
      </el-menu>

      <div class="logout-box">
        <el-button size="small" style="width:100%;" @click="logout">退出登录</el-button>
      </div>
    </el-aside>

    <el-main class="main-wrap">
      <router-view />
    </el-main>
  </el-container>
</template>

<style>
body { margin: 0; padding: 0; }

.layout-shell {
  height: 100vh;
  background: var(--app-bg);
}

.side-nav {
  background: var(--side-bg);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--side-border);
}

.brand-box {
  padding: 14px 0 10px;
}

.brand-title {
  color: var(--side-title);
  text-align: center;
  margin: 0;
}

.account-line {
  color: var(--side-subtitle);
  text-align: center;
  font-size: 12px;
  margin-top: 6px;
}

.logout-box {
  padding: 12px;
  border-top: 1px solid var(--side-border);
}

.main-wrap {
  background: var(--main-bg);
}
</style>
