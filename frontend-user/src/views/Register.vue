<template>
  <AuthShell mode="register" title="开启一段新的陪伴" description="创建领养账号，认识未来的小小家人。">
    <div v-if="submitError" class="auth-error" role="alert">
      <el-icon><Warning /></el-icon>
      <div><strong>{{ submitError }}</strong><p>填写的内容已保留，修改后可再次提交。</p></div>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form" @submit.prevent="register">
      <div class="auth-form-grid">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" name="username" placeholder="4–20位字母、数字或下划线" autocomplete="username" size="large" :disabled="loading" maxlength="20">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="你的昵称" prop="nickname">
          <el-input v-model.trim="form.nickname" name="nickname" placeholder="希望我们怎么称呼你" autocomplete="nickname" size="large" :disabled="loading" maxlength="30">
            <template #prefix><el-icon><UserFilled /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" name="phone" type="tel" inputmode="tel" placeholder="请输入11位手机号" autocomplete="tel" size="large" :disabled="loading" maxlength="11">
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model.trim="form.email" name="email" type="email" inputmode="email" placeholder="请输入常用邮箱" autocomplete="email" size="large" :disabled="loading">
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="设置密码" prop="password">
          <el-input v-model="form.password" name="password" type="password" placeholder="设置6–30位密码" autocomplete="new-password" size="large" :disabled="loading" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" name="confirmPassword" type="password" placeholder="再次输入密码" autocomplete="new-password" size="large" :disabled="loading" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
      </div>
      <p class="auth-requirements"><span>以上信息均需填写，手机号与邮箱用于完善个人资料。</span><span>密码为6–30位，开头和结尾请勿使用空格。</span></p>
      <el-button type="primary" native-type="submit" class="auth-submit" :loading="loading">
        {{ loading ? '正在创建账号' : '创建账号，开启陪伴' }}<el-icon v-if="!loading"><ArrowRight /></el-icon>
      </el-button>
    </el-form>
    <p class="auth-switch">已经有账号了？<router-link :to="loginTarget">直接登录</router-link></p>
    <p class="auth-small-note"><el-icon><CircleCheck /></el-icon><span>注册成功后将自动登录，继续探索你的领养之旅。</span></p>
  </AuthShell>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, CircleCheck, Iphone, Lock, Message, User, UserFilled, Warning } from '@element-plus/icons-vue'
import AuthShell from '../components/AuthShell.vue'
import request from '../utils/request'
import { safeRedirect, setSession } from '../utils/session'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitError = ref('')
const form = reactive({ username: '', nickname: '', phone: '', email: '', password: '', confirmPassword: '' })
const loginTarget = computed(() => ({
  path: '/login',
  query: typeof route.query.redirect === 'string' ? { redirect: safeRedirect(route.query.redirect) } : {}
}))

const confirmPasswordValidator = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}
const passwordWhitespaceValidator = (rule, value, callback) => {
  if (value && value !== value.trim()) callback(new Error('密码开头和结尾请勿使用空格'))
  else callback()
}
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '使用4–20位字母、数字或下划线', trigger: 'blur' }
  ],
  nickname: [
    { required: true, whitespace: true, message: '请输入昵称', trigger: 'blur' },
    { max: 30, message: '昵称不能超过30个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入电子邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, whitespace: true, message: '请设置登录密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度应为6–30位', trigger: 'blur' },
    { validator: passwordWhitespaceValidator, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: confirmPasswordValidator, trigger: 'blur' }
  ]
}

watch(() => form.password, () => {
  if (form.confirmPassword && formRef.value) formRef.value.validateField('confirmPassword').catch(() => {})
})

const register = async () => {
  if (!formRef.value || loading.value) return
  submitError.value = ''
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await request.post('/auth/register', {
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      phone: form.phone,
      email: form.email
    })
    if (res.code !== 200 || !res.data?.token) {
      submitError.value = res.message || '注册未成功，请检查填写的信息'
      return
    }
    setSession(res.data)
    ElMessage.success('账号已创建，欢迎加入')
    await router.replace(safeRedirect(route.query.redirect, '/profile'))
  } catch (error) {
    submitError.value = error.response?.data?.message || (error.code === 'ECONNABORTED' ? '连接超时，请稍后重试' : '暂时无法创建账号，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>
