
<template>
  <div v-if="fatalError" class="fatal-error">
    <div class="fatal-panel">
      <div class="fatal-icon">!</div>
      <p class="fatal-kicker">ADMIN CONSOLE</p>
      <h1>页面暂时无法显示</h1>
      <p>前端组件运行异常。刷新页面通常可以恢复；如果仍然出现，请确认后端服务已启动。</p>
      <div class="fatal-actions">
        <el-button type="primary" @click="reloadPage">刷新页面</el-button>
        <el-button @click="goLogin">返回登录</el-button>
      </div>
    </div>
  </div>
  <router-view v-else v-slot="{ Component }">
    <component :is="Component" v-if="Component" />
    <div v-else class="route-loading" role="status">
      <div class="route-loading-mark">P</div>
      <strong>正在加载管理工作台</strong>
      <span>页面资源准备中，请稍候</span>
    </div>
  </router-view>
</template>

<script setup>
import { onBeforeUnmount, onErrorCaptured, onMounted, ref } from 'vue'

const fatalError = ref(Boolean(window.__adminFatalError))

onErrorCaptured((error) => {
  console.error('[admin-ui]', error)
  fatalError.value = true
  return false
})

const handleFatalError = () => {
  fatalError.value = true
}

onMounted(() => {
  window.addEventListener('admin-ui-fatal', handleFatalError)
})

onBeforeUnmount(() => {
  window.removeEventListener('admin-ui-fatal', handleFatalError)
})

const reloadPage = () => window.location.reload()
const goLogin = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('adminUser')
  window.location.href = '/login'
}
</script>
