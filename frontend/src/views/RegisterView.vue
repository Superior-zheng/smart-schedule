<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'
import { useRouter } from 'vue-router'

const router = useRouter()

const form = ref({
  username: '',
  password: '',
  confirm: '',
  nickname: ''
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
  if (form.value.password !== form.value.confirm) {
    return ElMessage.warning('两次密码不一致')
  }
  if ((form.value.nickname || '').trim().length > 50) {
    return ElMessage.warning('昵称不能超过50个字符')
  }

  loading.value = true
  try {
    const res = await http.post('/api/auth/register', {
      username: form.value.username,
      password: form.value.password,
      nickname: form.value.nickname || ''
    })
    if (!res.data?.ok) {
      return ElMessage.error(res.data?.message || '注册失败')
    }
    ElMessage.success('注册成功，请登录')
    await router.push('/login')
  } catch (e) {
    ElMessage.error('注册失败')
  } finally {
    loading.value = false
  }
}

const goLogin = () => router.push('/login')
</script>

<template>
  <div style="min-height: 100vh; display:flex; align-items:center; justify-content:center; background:#f6f7fb; padding: 24px;">
    <el-card style="width: 420px; border-radius: 16px;">
      <template #header>
        <div style="font-weight: 600; font-size: 18px;">注册</div>
      </template>

      <el-form label-width="90px">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="6-12位纯数字" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="6-12位，含字母+数字" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirm" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="可不填" maxlength="50" show-word-limit />
        </el-form-item>

        <div style="display:flex; gap: 12px; justify-content: space-between; margin-top: 10px;">
          <el-button type="primary" :loading="loading" @click="submit" style="flex: 1;">注册</el-button>
          <el-button @click="goLogin" style="flex: 1;">去登录</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>
