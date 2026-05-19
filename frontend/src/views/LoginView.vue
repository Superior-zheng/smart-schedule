<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
import { useRouter } from 'vue-router'

const router = useRouter()

const form = ref({
  username: '',
  password: ''
})

const usernameRegex = /^[0-9]{6,12}$/
const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,12}$/

const loading = ref(false)

const submit = async () => {
  if (!usernameRegex.test(form.value.username)) {
    return ElMessage.warning('账号必须是6-12位纯数字')
  }
  if (!passwordRegex.test(form.value.password)) {
    return ElMessage.warning('密码必须包含字母和数字且6-12位')
  }
  loading.value = true
  try {
    const res = await http.post('/api/auth/login', {
      username: form.value.username,
      password: form.value.password
    })
    if (!res.data?.ok) {
      return ElMessage.error(res.data?.message || '登录失败')
    }
    sessionStorage.setItem('token', res.data.data.token)
    sessionStorage.setItem('username', res.data.data.username)
    sessionStorage.setItem('role', String(res.data.data.role ?? 0))
    sessionStorage.setItem('nickname', res.data.data.nickname || '')
    window.dispatchEvent(new Event('auth-changed'))
    ElMessage.success('登录成功')
    await router.push('/')
  } catch (e) {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}

const goRegister = () => router.push('/register')
</script>

<template>
  <div style="min-height: 100vh; display:flex; align-items:center; justify-content:center; background:#f6f7fb; padding: 24px;">
    <el-card style="width: 420px; border-radius: 16px;">
      <template #header>
        <div style="font-weight: 600; font-size: 18px;">登录</div>
      </template>

      <el-form label-width="90px">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="6-12位纯数字" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="6-12位，含字母+数字" />
        </el-form-item>

        <div style="display:flex; gap: 12px; justify-content: space-between; margin-top: 10px;">
          <el-button type="primary" :loading="loading" @click="submit" style="flex: 1;">登录</el-button>
          <el-button @click="goRegister" style="flex: 1;">去注册</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>
