
import { createRouter, createWebHistory } from 'vue-router'
import { safeRedirect } from '../utils/session'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/pets',
    name: 'PetList',
    component: () => import('../views/PetList.vue')
  },
  {
    path: '/pet/:id',
    name: 'PetDetail',
    component: () => import('../views/PetDetail.vue')
  },
  {
    path: '/health-radar',
    name: 'HealthRadar',
    component: () => import('../views/HealthRadar.vue')
  },
  {
    path: '/care-plan',
    name: 'CarePlan',
    component: () => import('../views/CarePlan.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/adoptions',
    name: 'AdoptionList',
    component: () => import('../views/AdoptionList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/announcements',
    name: 'AnnouncementList',
    component: () => import('../views/AnnouncementList.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.path !== from.path) return { top: 0 }
  },
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next(safeRedirect(to.query.redirect, '/profile'))
  } else {
    next()
  }
})

router.afterEach(to => {
  const titles = { Home: '首页', PetList: '寻找伙伴', PetDetail: '宠物档案', HealthRadar: '健康雷达', CarePlan: '照护计划', Login: '欢迎回来', Register: '加入我们', Profile: '个人中心', AdoptionList: '我的领养', AnnouncementList: '公告资讯' }
  document.title = `${titles[to.name] || '温暖相伴'} · 宠物领养`
})

export default router
