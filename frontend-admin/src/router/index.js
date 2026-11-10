
import { createRouter, createWebHistory } from 'vue-router'
import { safeAdminRedirect } from '../utils/navigation'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../components/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', meta: { requiresAdmin: true }, component: () => import('../views/Dashboard.vue') },
      { path: 'users', name: 'UserManage', meta: { requiresAdmin: true }, component: () => import('../views/UserManage.vue') },
      { path: 'pets', name: 'PetManage', meta: { requiresAdmin: true }, component: () => import('../views/PetManage.vue') },
      { path: 'adoptions', name: 'AdoptionManage', meta: { requiresAdmin: true }, component: () => import('../views/AdoptionManage.vue') },
      { path: 'health', name: 'HealthManage', meta: { requiresAdmin: true }, component: () => import('../views/HealthRecordManage.vue') },
      { path: 'vaccine', name: 'VaccineManage', meta: { requiresAdmin: true }, component: () => import('../views/VaccineRecordManage.vue') },
      { path: 'announcements', name: 'AnnouncementManage', meta: { requiresAdmin: true }, component: () => import('../views/AnnouncementManage.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const readCachedUser = () => {
  try {
    return JSON.parse(localStorage.getItem('adminUser') || 'null')
  } catch {
    localStorage.removeItem('adminUser')
    return null
  }
}

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const cachedUser = readCachedUser()
  if (to.path !== '/login' && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token && cachedUser?.role === 1) {
    next(safeAdminRedirect(to.query.redirect))
  } else if (to.meta.requiresAdmin && cachedUser?.role !== 1) {
    localStorage.removeItem('token')
    localStorage.removeItem('adminUser')
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

router.afterEach(to => {
  const titles = { Login: '管理员登录', Dashboard: '运营概览', UserManage: '用户管理', PetManage: '宠物档案', AdoptionManage: '领养审核', HealthManage: '健康记录', VaccineManage: '疫苗记录', AnnouncementManage: '公告资讯' }
  document.title = `${titles[to.name] || '管理工作台'} · 宠物领养中台`
})

router.onError((error, to) => {
  const message = String(error?.message || error || '')
  const isModuleLoadError = /dynamic|module|import/i.test(message)
  if (!isModuleLoadError) return

  const reloadKey = `admin-route-reload:${to?.fullPath || window.location.pathname}`
  if (sessionStorage.getItem(reloadKey) !== '1') {
    sessionStorage.setItem(reloadKey, '1')
    const separator = (to?.fullPath || window.location.pathname).includes('?') ? '&' : '?'
    window.location.replace(`${to?.fullPath || window.location.pathname}${separator}__admin_reload=${Date.now()}`)
  } else {
    sessionStorage.removeItem(reloadKey)
    window.location.replace('/login?__admin_route_error=1')
  }
})

export default router
