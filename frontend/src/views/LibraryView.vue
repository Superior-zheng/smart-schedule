<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '../api/http'

const links = ref([])
const loading = ref(false)

const form = ref({
  url: '',
  note: ''
})

const fetchLinks = async () => {
  loading.value = true
  try {
    const res = await http.get('/api/library/links')
    links.value = res.data || []
  } catch (e) {
    console.error(e)
    links.value = []
  } finally {
    loading.value = false
  }
}

const normalizeUrl = (url) => {
  const value = url.trim()
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `https://${value}`
}

const host = (url) => {
  try {
    return new URL(url).hostname
  } catch {
    return url
  }
}

const addLink = async () => {
  const url = normalizeUrl(form.value.url)
  if (!url) return ElMessage.warning('请输入网址')
  try {
    new URL(url)
  } catch {
    return ElMessage.warning('网址格式不正确')
  }

  try {
    const res = await http.post('/api/library/links', {
      url,
      note: form.value.note.trim()
    })
    if (!res.data?.ok) return ElMessage.error(res.data?.message || '添加失败')

    form.value.url = ''
    form.value.note = ''
    await fetchLinks()
    ElMessage.success('已添加资料')
  } catch (e) {
    console.error(e)
    ElMessage.error('添加失败')
  }
}

const removeLink = async (id) => {
  try {
    await http.delete(`/api/library/links/${id}`)
    await fetchLinks()
    ElMessage.success('已删除')
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  }
}

const openWebsite = (url) => {
  window.open(url, '_blank', 'noopener,noreferrer')
}

const cards = computed(() => links.value.map(l => ({ ...l, host: host(l.url) })))

onMounted(fetchLinks)
</script>

<template>
  <div class="library-page">
    <h2 style="margin-top:0;">学习资料库</h2>

    <el-card style="margin-bottom: 16px;">
      <el-form inline>
        <el-form-item label="网站URL">
          <el-input v-model="form.url" placeholder="https://example.com" style="width: 340px;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" placeholder="写一点说明" style="width: 260px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addLink">添加</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-if="cards.length === 0" style="color: var(--text-secondary, #909399); padding: 12px 4px;">暂无资料，先添加一个网站吧。</div>

    <div v-else class="grid">
      <el-card v-for="item in cards" :key="item.id" class="link-card" shadow="hover">
        <div style="display:flex; justify-content:space-between; gap: 8px; align-items:flex-start;">
          <div>
            <div class="host">{{ item.host }}</div>
            <div class="url">{{ item.url }}</div>
          </div>
          <el-button type="danger" plain size="small" @click="removeLink(item.id)">删除</el-button>
        </div>

        <div class="note">{{ item.note || '（无备注）' }}</div>

        <div style="margin-top: 12px;">
          <el-button type="primary" plain size="small" @click="openWebsite(item.url)">打开网站</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.library-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
.grid { display:grid; grid-template-columns: repeat(auto-fill, minmax(270px, 1fr)); gap: 14px; }
.link-card { border-radius: 12px; }
.host { font-size: 16px; font-weight: 700; color: var(--text-primary, #303133); }
.url { color: var(--text-secondary, #909399); margin-top: 4px; word-break: break-all; font-size: 12px; }
.note { margin-top: 12px; color: var(--text-regular, #606266); min-height: 42px; }
</style>
