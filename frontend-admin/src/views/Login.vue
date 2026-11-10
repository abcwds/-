<template>
  <main class="login-page">
    <header class="login-topbar">
      <span class="portal-caption"><el-icon><Lock /></el-icon>宠物领养及健康管理<span>管理中心</span></span>
      <a :href="userSiteUrl" class="visit-site" target="_blank" rel="noopener noreferrer">访问用户端<el-icon><TopRight /></el-icon></a>
    </header>
    <div class="login-shell">
      <section class="login-intro" aria-labelledby="brand-title">
        <div class="login-brand">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="currentColor">
              <ellipse cx="8" cy="11" rx="3.1" ry="4" transform="rotate(-28 8 11)" />
              <ellipse cx="14" cy="7.7" rx="2.9" ry="4" transform="rotate(-8 14 7.7)" />
              <ellipse cx="21" cy="8.5" rx="2.9" ry="4" transform="rotate(17 21 8.5)" />
              <ellipse cx="26" cy="14" rx="2.7" ry="3.7" transform="rotate(32 26 14)" />
              <path d="M8.3 23.2c0-3 3-4.3 5.2-7.3 1.9-2.6 5.3-2.6 7.2.2 1.8 2.7 4.4 4.2 4.4 7.1 0 3-2.6 4.5-5.1 3.6-2.6-.9-3.7-1-6.5-.1-2.7.8-5.2-.7-5.2-3.5Z" />
            </svg>
          </span>
          <div><strong id="brand-title">宠物领养中台</strong><span>PET ADOPTION · ADMINISTRATION</span></div>
        </div>

        <div class="intro-copy">
          <div class="intro-eyebrow"><span></span> 专注管理，用心守护</div>
          <h2>把每一份关爱，<br />落在细致的日常里。</h2>
          <p>从领养审核到健康档案，<br />让每一次相遇都有记录，让每一份照护有迹可循。</p>
        </div>

        <div class="intro-art" aria-hidden="true">
          <svg viewBox="0 0 380 230" fill="none">
            <circle cx="205" cy="115" r="104" stroke="#ffffff" stroke-opacity=".09" />
            <circle cx="205" cy="115" r="79" stroke="#ffffff" stroke-opacity=".09" />
            <path d="m95 102 77-62a17 17 0 0 1 21 0l77 62" stroke="#c5d9b8" stroke-width="6" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M112 94v99a10 10 0 0 0 10 10h123a10 10 0 0 0 10-10V94" fill="#386751" stroke="#c5d9b8" stroke-width="4" stroke-linecap="round" />
            <rect x="138" y="107" width="31" height="31" rx="6" fill="#e1bc86" />
            <path d="M153.5 108v30M139 122.5h29" stroke="#386751" stroke-width="3" />
            <path d="M194 203v-43a23 23 0 0 1 46 0v43" fill="#244f3d" />
            <path d="M82 200c-13-7-21-20-16-31 3-8 12-8 16-1" stroke="#e6d5b9" stroke-width="6" stroke-linecap="round" />
            <path d="M78 203c-3-28 0-42 16-45 18-3 29 17 27 45" fill="#e6d5b9" />
            <path d="m82 146-3-25 18 10 14-9 6 25c3 13-8 24-18 23-12 0-20-10-17-24Z" fill="#eee2cd" />
            <path d="m84 129 2 11 7-7m16-2-7 4 9 5" fill="#bd9879" />
            <path d="M91 147h1m14-1h1" stroke="#315541" stroke-width="3" stroke-linecap="round" />
            <path d="m98 152 2 2 2-2" stroke="#a98069" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M286 202v-34" stroke="#b4c9a1" stroke-width="3" stroke-linecap="round" />
            <path d="M286 188c-18 0-22-12-20-22 16 0 22 10 20 22Zm0-12c18 0 23-12 21-22-16 0-23 10-21 22Z" fill="#8fa984" />
            <path d="M277 203h18l4-17h-26l4 17Z" fill="#ddb383" />
            <path d="M57 205h263" stroke="#ffffff" stroke-opacity=".2" stroke-width="2" stroke-linecap="round" />
            <path d="M279 65c-9-11-22 3 0 16 22-13 9-27 0-16Z" fill="#d7b785" />
            <path d="M69 98v10m-5-5h10M307 114v8m-4-4h8" stroke="#c5d9b8" stroke-width="2" stroke-linecap="round" />
          </svg>
        </div>
        <div class="operations-strip" aria-label="工作台管理功能">
          <div><el-icon><DocumentChecked /></el-icon><strong>领养审核</strong><span>认真回应每份申请</span></div>
          <div><el-icon><FirstAidKit /></el-icon><strong>健康档案</strong><span>妥善记录每次照护</span></div>
          <div><el-icon><Setting /></el-icon><strong>平台运营</strong><span>让日常管理更从容</span></div>
        </div>
      </section>

      <section class="login-container" aria-labelledby="login-title">
        <div class="access-caption"><span class="console-label"><el-icon><Lock /></el-icon> 管理员入口</span><span>ADMIN CONSOLE</span></div>
        <div class="login-header">
          <h1 id="login-title">欢迎回到工作台</h1>
          <p>使用已授权账号登录，开始今天的管理工作。</p>
        </div>

        <div v-if="submitError" class="login-feedback error" role="alert"><el-icon><Warning /></el-icon><div><strong>{{ submitError }}</strong><span>已保留填写内容，你可以检查后再次登录。</span></div></div>
        <div v-else-if="route.query.__admin_route_error === '1'" class="login-feedback" role="status"><el-icon><InfoFilled /></el-icon><div><strong>页面资源暂时未能加载</strong><span>请重新登录；若仍有问题，可刷新页面后重试。</span></div></div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form" :aria-busy="loading" @submit.prevent="login">
          <el-form-item prop="username">
            <template #label><span class="field-label">管理员账号<small>必填</small></span></template>
            <el-input v-model.trim="form.username" name="username" aria-label="管理员账号" aria-required="true" placeholder="请输入管理员账号" autocomplete="username" :disabled="loading" size="large" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <template #label><span class="field-label">登录密码<small>必填</small></span></template>
            <el-input v-model="form.password" name="password" aria-label="登录密码" aria-required="true" type="password" placeholder="请输入登录密码" autocomplete="current-password" show-password :disabled="loading" size="large" @keydown="checkCapsLock" @keyup="checkCapsLock" @blur="capsLock = false">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <p v-if="capsLock" class="caps-lock-note" role="status"><el-icon><Warning /></el-icon>大写锁定已开启，请注意密码大小写。</p>
          <div class="login-options"><el-checkbox v-model="rememberUsername" :disabled="loading">记住管理员账号</el-checkbox><span>不保存密码</span></div>
          <el-button native-type="submit" type="primary" class="login-btn" :loading="loading">
            {{ loading ? '正在验证身份' : '登录管理工作台' }}<el-icon v-if="!loading"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="login-note"><el-icon><CircleCheck /></el-icon><div><strong>专属管理员的工作空间</strong><span>账号需要管理员权限。无法登录时，请联系平台负责人确认账号状态。</span></div></div>
      </section>
    </div>
    <footer class="page-footer"><span>宠物领养及健康管理平台</span><span class="footer-dot">·</span><span>用心照顾，让爱延续</span></footer>
  </main>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, ArrowRight, CircleCheck, DocumentChecked, FirstAidKit, InfoFilled, Setting, TopRight, Warning } from '@element-plus/icons-vue'
import request from '../utils/request'
import { safeAdminRedirect } from '../utils/navigation'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitError = ref('')
const capsLock = ref(false)
const rememberUsername = ref(false)
const userSiteUrl = `${window.location.protocol}//${window.location.hostname}:3000`
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, whitespace: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, whitespace: true, message: '请输入登录密码', trigger: 'blur' }]
}

onMounted(() => {
  try {
    const remembered = localStorage.getItem('rememberedAdminUsername') || ''
    form.username = remembered
    rememberUsername.value = Boolean(remembered)
  } catch {
    // 记住账号为可选能力，不影响填写与提交。
  }
})

const checkCapsLock = event => {
  capsLock.value = Boolean(event.getModifierState?.('CapsLock'))
}

const login = async () => {
  if (!formRef.value || loading.value) return
  loading.value = true
  submitError.value = ''
  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    const res = await request.post('/auth/login', { username: form.username, password: form.password })
    if (res?.code !== 200) {
      submitError.value = res?.message || '登录失败，请检查账号和密码'
      return
    }
    if (res.data?.role !== 1) {
      submitError.value = '当前账号没有管理员权限，请使用管理员账号登录'
      return
    }
    if (!res.data.token) {
      submitError.value = '登录信息不完整，请稍后重试'
      return
    }
    const { token, password, ...adminUser } = res.data
    try {
      localStorage.setItem('token', token)
      localStorage.setItem('adminUser', JSON.stringify(adminUser))
    } catch {
      try { localStorage.removeItem('token'); localStorage.removeItem('adminUser') } catch {}
      submitError.value = '浏览器无法保存登录状态，请检查网站存储设置后重试'
      return
    }
    try {
      if (rememberUsername.value) localStorage.setItem('rememberedAdminUsername', form.username)
      else localStorage.removeItem('rememberedAdminUsername')
    } catch {
      // 可选记忆操作失败时，仍继续正常登录。
    }
    ElMessage.success('登录成功')
    await router.replace(safeAdminRedirect(route.query.redirect))
  } catch (error) {
    submitError.value = error.response?.data?.message || (error.code === 'ECONNABORTED' ? '连接超时，请稍后重试' : '暂时无法登录，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 30px 24px 24px;
  background: #f7f8f4;
}
.login-topbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; width: min(1080px, 100%); margin-bottom: 19px; }
.portal-caption { display: flex; align-items: center; gap: 7px; color: #7d8c78; font-size: 11px; }
.portal-caption > .el-icon { color: #6b8a6e; font-size: 13px; }
.portal-caption > span { padding-left: 11px; margin-left: 4px; border-left: 1px solid #dfe5d8; font-size: 10px; color: #9ba491; }
.visit-site { display: inline-flex; align-items: center; gap: 6px; color: #61795c; font-size: 11px; text-decoration: none; white-space: nowrap; }
.visit-site:hover { color: #28745b; }
.login-shell {
  width: min(1080px, 100%);
  display: grid;
  grid-template-columns: 1.06fr 1fr;
  background: #fff;
  border: 1px solid #e1e7d9;
  border-radius: 22px;
  overflow: hidden;
  box-shadow: 0 20px 65px -30px rgba(38, 59, 51, .2);
}
.login-intro {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 36px 42px 28px;
  color: #fff;
  background: #234d3d;
}
.login-brand { display: flex; align-items: center; gap: 12px; }
.brand-mark { display: grid; place-items: center; width: 42px; height: 42px; color: #214e3d; background: #edf1df; border-radius: 13px; }
.brand-mark svg { width: 28px; height: 28px; }
.login-brand strong { display: block; font-size: 17px; letter-spacing: .04em; font-weight: 600; }
.login-brand > div > span { display: block; margin-top: 5px; color: #a8c1b1; font-size: 8px; letter-spacing: .16em; }
.intro-copy { margin-top: 36px; }
.intro-eyebrow { display: flex; align-items: center; gap: 8px; color: #bed0bd; font-size: 11px; letter-spacing: .02em; }
.intro-eyebrow > span { height: 5px; width: 5px; background: #d7b785; border-radius: 50%; }
.intro-copy h2 { margin: 16px 0 14px; color: #fbfcf6; font-size: 32px; line-height: 1.55; font-weight: 600; letter-spacing: .035em; }
.intro-copy p { color: #b9ccbd; font-size: 12px; line-height: 1.95; }
.intro-art { flex: 1; display: grid; align-items: center; margin: 6px -4px 0; }
.intro-art svg { display: block; width: 100%; max-height: 205px; }
.operations-strip { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; padding-top: 21px; margin-top: 8px; border-top: 1px solid #ffffff19; }
.operations-strip > div { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.operations-strip .el-icon { margin-bottom: 2px; color: #c6d8b6; font-size: 18px; }
.operations-strip strong { color: #e4ecdc; font-size: 11px; font-weight: 500; }
.operations-strip span { color: #9bb5a2; font-size: 9px; line-height: 1.6; }
.login-container { display: flex; flex-direction: column; justify-content: center; min-width: 0; padding: 44px 48px; }
.access-caption { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.access-caption > span:last-child { color: #a5ae9d; font-size: 8px; letter-spacing: 1.4px; }
.console-label { display: flex; align-items: center; align-self: flex-start; gap: 6px; padding: 7px 10px; color: #5c7866; background: #f0f5ef; border: 1px solid #e7eee3; border-radius: 6px; font-size: 11px; }
.console-label .el-icon { font-size: 13px; }
.login-header { margin: 24px 0 30px; }
.login-header h1 { margin: 0 0 11px; color: #263b33; font-size: 28px; font-weight: 600; letter-spacing: .02em; }
.login-header p { color: #859086; font-size: 12px; line-height: 1.8; }
.login-feedback { display: flex; gap: 9px; align-items: flex-start; padding: 12px 14px; margin: -9px 0 22px; border: 1px solid #dfe8d9; border-radius: 9px; background: #f4f7f0; color: #69815c; }
.login-feedback.error { border-color: #eedfd4; background: #fcf5ef; color: #a46b53; }
.login-feedback > .el-icon { flex-shrink: 0; margin-top: 3px; font-size: 15px; }
.login-feedback strong { display: block; font-weight: 500; font-size: 12px; line-height: 1.7; }
.login-feedback span { display: block; color: #999683; font-size: 10px; line-height: 1.7; margin-top: 3px; }
.login-form { width: 100%; }
.login-form :deep(.el-form-item) { margin-bottom: 22px; }
.login-form :deep(.el-form-item__label) { display: block; width: 100%; margin-bottom: 9px; color: #405348; font-size: 12px; font-weight: 500; line-height: 20px; }
.login-form :deep(.el-form-item__label::before) { display: none; }
.field-label { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.field-label small { color: #9aa592; font-size: 9px; font-weight: 400; }
.login-form :deep(.el-input__wrapper) { height: 46px; padding: 0 14px; background: #fcfdfb; border-radius: 9px; box-shadow: 0 0 0 1px #e0e6dd inset; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #28745b inset, 0 0 0 3px rgba(40, 116, 91, .08); }
.login-form :deep(.is-error .el-input__wrapper) { box-shadow: 0 0 0 1px #c7816c inset; }
.login-form :deep(.el-form-item__error) { font-size: 11px; padding-top: 5px; }
.login-form :deep(.el-input__inner) { font-size: 13px; }
.login-form :deep(.el-input__prefix) { color: #93a091; }
.login-form :deep(.el-input__prefix-inner > :last-child) { margin-right: 10px; }
.login-options { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin: -3px 0 23px; }
.login-options :deep(.el-checkbox) { height: 24px; --el-checkbox-font-size: 12px; --el-checkbox-checked-text-color: #53704e; }
.login-options > span { color: #9da692; font-size: 10px; }
.caps-lock-note { display: flex; gap: 5px; align-items: center; margin: -6px 0 14px; color: #ad8a51; font-size: 10px; line-height: 1.6; }
.login-btn { width: 100%; height: 46px; border-radius: 9px; font-size: 14px; letter-spacing: .03em; --el-button-bg-color: #28745b; --el-button-border-color: #28745b; --el-button-hover-bg-color: #215f4b; --el-button-hover-border-color: #215f4b; box-shadow: 0 5px 12px rgba(40, 116, 91, .1); }
.login-btn .el-icon { margin-left: 12px; }
.login-note { display: flex; align-items: flex-start; gap: 9px; margin-top: 25px; padding: 15px 16px; border: 1px solid #ecf0e5; border-radius: 10px; background: #f8faf5; color: #8b9681; font-size: 10px; line-height: 1.8; }
.login-note .el-icon { color: #839b71; margin-top: 2px; font-size: 15px; flex-shrink: 0; }
.login-note strong { display: block; margin-bottom: 3px; color: #73836a; font-size: 11px; font-weight: 500; }
.page-footer { display: flex; align-items: center; gap: 9px; margin-top: 24px; color: #929c89; font-size: 10px; letter-spacing: .03em; }
@media (max-width: 900px) {
  .login-intro { padding: 32px; }
  .intro-copy h2 { font-size: 27px; }
  .login-container { padding: 38px 30px; }
  .login-header h1 { font-size: 25px; }
  .access-caption > span:last-child { display: none; }
  .operations-strip { gap: 7px; }
  .operations-strip span { font-size: 8px; }
}
@media (max-width: 680px) {
  .login-page { padding: 22px 16px; }
  .login-topbar { max-width: 450px; margin-bottom: 15px; }
  .portal-caption { font-size: 9px; }
  .portal-caption > span { display: none; }
  .visit-site { font-size: 10px; }
  .login-shell { max-width: 450px; grid-template-columns: 1fr; border-radius: 20px; }
  .login-intro { padding: 25px 28px 27px; }
  .login-brand strong { font-size: 16px; }
  .intro-copy { margin-top: 20px; }
  .intro-eyebrow { display: none; }
  .intro-copy h2 { margin: 0; font-size: 21px; line-height: 1.6; }
  .intro-copy h2 br { display: none; }
  .intro-copy p, .intro-art, .operations-strip { display: none; }
  .login-container { padding: 30px 28px; }
  .access-caption > span:last-child { display: block; }
  .console-label { font-size: 10px; padding: 5px 8px; }
  .login-header { margin: 20px 0 25px; }
  .login-header h1 { font-size: 25px; }
  .login-note { margin-top: 21px; }
  .page-footer { flex-wrap: wrap; justify-content: center; gap: 7px; margin-top: 18px; font-size: 9px; }
}
@media (max-width: 380px) {
  .login-page { padding: 18px 12px; }
  .login-intro, .login-container { padding-left: 23px; padding-right: 23px; }
  .intro-copy h2 { font-size: 19px; }
  .login-header h1 { font-size: 23px; }
  .access-caption > span:last-child { font-size: 7px; }
}
@media (prefers-reduced-motion: reduce) {
  .login-form :deep(*), .login-btn { transition: none; }
}
</style>
