<template>
  <AuthShell title="欢迎回来" description="登录账号，继续你和它的温暖故事。">
    <div v-if="submitError" class="auth-error" role="alert">
      <el-icon><Warning /></el-icon>
      <div><strong>{{ submitError }}</strong><p>填写的内容已保留，你可以检查后再次登录。</p></div>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form" @submit.prevent="login">
      <el-form-item label="用户名" prop="username">
        <el-input v-model.trim="form.username" name="username" placeholder="请输入用户名" autocomplete="username" size="large" :disabled="loading" clearable>
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-form-item label="登录密码" prop="password">
        <el-input v-model="form.password" name="password" type="password" placeholder="请输入登录密码" autocomplete="current-password" size="large" :disabled="loading" show-password>
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
      </el-form-item>
      <div class="auth-form-options">
        <el-checkbox v-model="rememberUsername" :disabled="loading">记住用户名</el-checkbox>
        <span>仅记住用户名，不保存密码</span>
      </div>
      <el-button type="primary" native-type="submit" class="auth-submit" :loading="loading">
        {{ loading ? '正在登录' : '登 录' }}<el-icon v-if="!loading"><ArrowRight /></el-icon>
      </el-button>
    </el-form>
    <p class="auth-switch">还没有账号？<router-link :to="registerTarget">创建一个新账号</router-link></p>
    <p class="auth-small-note"><el-icon><CircleCheck /></el-icon><span>每一份认真，都会让它离温暖的新家更近。</span></p>
  </AuthShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, CircleCheck, Lock, User, Warning } from '@element-plus/icons-vue'
import AuthShell from '../components/AuthShell.vue'
import request from '../utils/request'
import { safeRedirect, setSession } from '../utils/session'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitError = ref('')
const rememberUsername = ref(true)
const form = reactive({ username: '', password: '' })

const registerTarget = computed(() => ({
  path: '/register',
  query: typeof route.query.redirect === 'string' ? { redirect: safeRedirect(route.query.redirect) } : {}
}))

const rules = {
  username: [{ required: true, whitespace: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, whitespace: true, message: '请输入登录密码', trigger: 'blur' }]
}

onMounted(() => {
  try {
    form.username = localStorage.getItem('rememberedUsername') || ''
  } catch {
    // 浏览器禁止本地存储时仍可正常填写表单。
  }
})

const login = async () => {
  if (!formRef.value || loading.value) return
  submitError.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await request.post('/auth/login', { username: form.username, password: form.password })
    if (res.code !== 200 || !res.data?.token) {
      submitError.value = res.message || '登录未成功，请检查用户名和密码'
      return
    }
    setSession(res.data)
    try {
      if (rememberUsername.value) localStorage.setItem('rememberedUsername', form.username)
      else localStorage.removeItem('rememberedUsername')
    } catch {
      // 记住用户名是可选操作，不影响登录结果。
    }
    ElMessage.success('欢迎回来')
    await router.replace(safeRedirect(route.query.redirect, '/profile'))
  } catch (error) {
    submitError.value = error.response?.data?.message || (error.code === 'ECONNABORTED' ? '连接超时，请稍后再试' : '暂时无法登录，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>
