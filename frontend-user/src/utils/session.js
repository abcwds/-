import { ref } from 'vue'

function readUser() {
  try {
    const cached = JSON.parse(localStorage.getItem('user') || 'null')
    return localStorage.getItem('token') && cached && typeof cached === 'object' && !Array.isArray(cached) ? cached : null
  } catch {
    localStorage.removeItem('user')
    return null
  }
}

export const currentUser = ref(readUser())

export function setUser(value) {
  if (!value || typeof value !== 'object') return
  const { token, password, ...user } = value
  localStorage.setItem('user', JSON.stringify(user))
  currentUser.value = user
}

export function setSession(value) {
  if (!value?.token) throw new Error('登录信息不完整，请重试')
  localStorage.setItem('token', value.token)
  setUser(value)
}

export function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  currentUser.value = null
}

export function safeRedirect(value, fallback = '/profile') {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//') || value.includes('\\')) return fallback
  try {
    const url = new URL(value, window.location.origin)
    const routes = ['/', '/profile', '/adoptions', '/pets', '/health-radar', '/care-plan', '/announcements']
    if (url.origin !== window.location.origin || (!routes.includes(url.pathname) && !/^\/pet\/\d+$/.test(url.pathname))) return fallback
    return url.pathname + url.search + url.hash
  } catch { return fallback }
}

window.addEventListener('storage', event => {
  if (event.key === 'user' || event.key === 'token' || event.key === null) currentUser.value = readUser()
})
