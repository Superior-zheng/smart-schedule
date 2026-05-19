<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '../api/http'
import { useRouter } from 'vue-router'

const router = useRouter()

const isAdmin = computed(() => String(sessionStorage.getItem('role') || '0') === '1')

const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirm: ''
})
const adminPwdForm = ref({
  username: '',
  newPassword: '',
  confirm: ''
})
const profileForm = ref({
  nickname: sessionStorage.getItem('nickname') || ''
})

const THEME_KEY = 'smart_schedule_theme'
const themeMode = ref(localStorage.getItem(THEME_KEY) || 'light')

const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,12}$/
const usernameRegex = /^[0-9]{6,12}$/

const applyTheme = (mode) => {
  const isDark = mode === 'dark'
  document.documentElement.classList.toggle('dark', isDark)
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light')
  localStorage.setItem(THEME_KEY, mode)
  window.dispatchEvent(new Event('theme-changed'))
}

const onThemeModeChange = (mode) => {
  applyTheme(mode)
  ElMessage.success(mode === 'dark' ? '已切换为夜间模式' : '已切换为日间模式')
}

const submitChangeNickname = async () => {
  try {
    const res = await http.post('/api/auth/change-nickname', {
      nickname: profileForm.value.nickname || ''
    })
    if (!res.data?.ok) return ElMessage.error(res.data?.message || '修改失败')

    const nickname = res.data?.data?.nickname || ''
    sessionStorage.setItem('nickname', nickname)
    window.dispatchEvent(new Event('auth-changed'))
    ElMessage.success('昵称已更新')
  } catch (e) {
    console.error(e)
    ElMessage.error('修改失败')
  }
}

const submitChangePassword = async () => {
  if (!pwdForm.value.oldPassword) return ElMessage.warning('请输入旧密码')
  if (!passwordRegex.test(pwdForm.value.newPassword)) return ElMessage.warning('新密码必须包含字母和数字且6-12位')
  if (pwdForm.value.newPassword !== pwdForm.value.confirm) return ElMessage.warning('两次新密码不一致')
  try {
    const res = await http.post('/api/auth/change-password', {
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    if (!res.data?.ok) return ElMessage.error(res.data?.message || '修改失败')
    ElMessage.success('密码修改成功')
    pwdForm.value.oldPassword = ''
    pwdForm.value.newPassword = ''
    pwdForm.value.confirm = ''
  } catch (e) {
    console.error(e)
    ElMessage.error('修改失败')
  }
}

const submitAdminChangePassword = async () => {
  if (!usernameRegex.test(adminPwdForm.value.username)) return ElMessage.warning('目标账号必须是6-12位纯数字')
  if (!passwordRegex.test(adminPwdForm.value.newPassword)) return ElMessage.warning('新密码必须包含字母和数字且6-12位')
  if (adminPwdForm.value.newPassword !== adminPwdForm.value.confirm) return ElMessage.warning('两次新密码不一致')
  try {
    const res = await http.post('/api/auth/admin/change-password', {
      username: adminPwdForm.value.username,
      newPassword: adminPwdForm.value.newPassword
    })
    if (!res.data?.ok) return ElMessage.error(res.data?.message || '修改失败')
    ElMessage.success('密码修改成功')
    adminPwdForm.value.username = ''
    adminPwdForm.value.newPassword = ''
    adminPwdForm.value.confirm = ''
  } catch (e) {
    console.error(e)
    ElMessage.error('修改失败')
  }
}

const deleteAccount = async () => {
  try {
    await ElMessageBox.confirm(
      '注销后账号和任务数据将不可恢复，确定继续吗？',
      '确认注销账号',
      {
        type: 'warning',
        confirmButtonText: '确认注销',
        cancelButtonText: '取消',
        confirmButtonClass: 'is-danger'
      }
    )

    const res = await http.post('/api/auth/delete-account')
    if (!res.data?.ok) return ElMessage.error(res.data?.message || '注销失败')

    sessionStorage.removeItem('token')
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('nickname')
    window.dispatchEvent(new Event('auth-changed'))
    ElMessage.success('账号已注销')
    await router.push('/login')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    console.error(e)
    ElMessage.error('注销失败')
  }
}
</script>

<template>
  <div style="padding: 40px; max-width: 900px; margin: 0 auto;">
    <h2 style="margin: 0 0 20px; color: #303133;">设置</h2>

    <el-card style="margin-bottom: 16px;">
      <template #header>
        <div style="font-weight: 600;">更改昵称</div>
      </template>
      <el-form label-width="110px" style="max-width: 520px;">
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称（可留空）" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitChangeNickname">保存昵称</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-bottom: 16px;">
      <template #header>
        <div style="font-weight: 600;">主题模式</div>
      </template>
      <el-form label-width="110px" style="max-width: 520px;">
        <el-form-item label="页面主题">
          <el-radio-group v-model="themeMode" @change="onThemeModeChange">
            <el-radio-button label="light">日间模式</el-radio-button>
            <el-radio-button label="dark">夜间模式</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-bottom: 16px;">
      <template #header>
        <div style="font-weight: 600;">更改密码</div>
      </template>

      <div v-if="!isAdmin">
        <el-form label-width="110px" style="max-width: 520px;">
          <el-form-item label="旧密码">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-12位，含字母+数字" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="pwdForm.confirm" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitChangePassword">保存</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-else>
        <el-form label-width="140px" style="max-width: 620px;">
          <el-form-item label="目标账号">
            <el-input v-model="adminPwdForm.username" placeholder="6-12位纯数字" style="width: 260px;" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="adminPwdForm.newPassword" type="password" show-password placeholder="6-12位，含字母+数字" style="width: 260px;" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="adminPwdForm.confirm" type="password" show-password style="width: 260px;" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitAdminChangePassword">保存</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card>
      <template #header>
        <div style="font-weight: 600;">账号</div>
      </template>
      <p style="margin-top: 0; color: #909399;">点击后将进行确认，再注销账号。</p>
      <el-button @click="deleteAccount">注销账号</el-button>
    </el-card>
  </div>
</template>
