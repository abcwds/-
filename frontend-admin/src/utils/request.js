
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { safeAdminRedirect } from './navigation'

export const expireSession = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('adminUser')
  if (window.location.pathname === '/login') return
  const redirect = safeAdminRedirect(window.location.pathname + window.location.search)
  window.location.replace(`/login?redirect=${encodeURIComponent(redirect)}`)
}

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  }
)

const handleRequestError = error => {
  const status = error.response?.status
  const message = error.response?.data?.message
  if (status === 401 || error.response?.data?.code === 401) {
    if (!/\/auth\/login$/.test(error.config?.url || '')) {
      ElMessage.error(message || '登录已过期，请重新登录')
      expireSession()
    }
  } else if (status === 403 || error.response?.data?.code === 403) {
    ElMessage.error(message || '当前账号没有管理员权限')
  } else if (message) {
    ElMessage.error(message)
  } else if (error.code === 'ECONNABORTED' || !error.response) {
    ElMessage.error('服务暂时无法连接，请检查后端是否已启动')
  }
  return Promise.reject(error)
}

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      if (!/\/auth\/login$/.test(response.config?.url || '')) expireSession()
    }
    return res
  },
  handleRequestError
)

export default request
