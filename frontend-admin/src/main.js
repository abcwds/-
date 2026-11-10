
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import './styles/admin.css'

const app = createApp(App)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

const reportFatalError = (error, info = '') => {
  console.error('[admin-ui]', error, info)
  window.__adminFatalError = error
  window.dispatchEvent(new CustomEvent('admin-ui-fatal', {
    detail: { error, info }
  }))
}

app.config.errorHandler = reportFatalError
window.addEventListener('error', event => {
  if (event.error) reportFatalError(event.error, 'window error')
})
window.addEventListener('unhandledrejection', event => {
  reportFatalError(event.reason, 'unhandled promise rejection')
})

app.mount('#app')
