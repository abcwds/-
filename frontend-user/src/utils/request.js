
import axios from 'axios'
import { clearSession, safeRedirect } from './session'

const expireSession = () => {
  clearSession()
  if (['/login', '/register'].includes(window.location.pathname)) return
  const redirect = safeRedirect(window.location.pathname + window.location.search)
  window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
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
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      expireSession()
    }
    return res
  },
  error => {
    if (error.response?.status === 401 && !/\/auth\/(login|register)$/.test(error.config?.url || '')) expireSession()
    return Promise.reject(error)
  }
)

export default request
