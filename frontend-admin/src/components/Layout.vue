<template>
  <div class="admin-layout" :class="{ 'nav-collapsed': collapsed }">
    <button v-if="mobileOpen" class="nav-backdrop" aria-label="关闭导航" @click="mobileOpen = false"></button>
    <aside id="admin-navigation" class="sidebar" :class="{ 'mobile-open': mobileOpen }" :inert="isMobile && !mobileOpen" aria-label="管理导航">
      <router-link to="/dashboard" class="sidebar-brand" aria-label="宠物领养中台首页" @click="mobileOpen = false">
        <span class="brand-mark"><PawMark /></span>
        <span class="brand-copy"><strong>宠物领养中台</strong><small>PET ADOPTION</small></span>
      </router-link>
      <nav class="sidebar-nav">
        <div v-for="group in menuGroups" :key="group.label" class="nav-group">
          <p class="nav-group-label">{{ group.label }}</p>
          <router-link v-for="item in group.items" :key="item.path" :to="item.path" class="nav-item" :title="collapsed ? item.label : undefined" @click="mobileOpen = false">
            <el-icon><component :is="item.icon" /></el-icon>
            <span class="nav-label">{{ item.label }}</span><span class="nav-active-dot" aria-hidden="true"></span>
          </router-link>
        </div>
      </nav>
      <div class="sidebar-bottom">
        <div class="care-note"><span class="care-note-icon"><PawMark /></span><strong>每一份用心，都有回响</strong><p>为它们，找到温暖的家。</p></div>
        <div class="service-status" :title="serviceText" role="status"><span class="status-dot" :class="serviceState"></span><span class="service-copy">{{ serviceText }}</span><span class="version">v1.0</span></div>
      </div>
    </aside>
    <main class="main-content">
      <header class="workspace-header">
        <div class="header-left">
          <button class="icon-button nav-toggle" :aria-label="navigationExpanded ? '收起导航' : '展开导航'" :aria-expanded="navigationExpanded" aria-controls="admin-navigation" @click="toggleNavigation"><el-icon><Fold v-if="navigationExpanded" /><Expand v-else /></el-icon></button>
          <div class="header-breadcrumb"><span>管理工作台</span><span class="breadcrumb-slash">/</span><strong>{{ pageTitle }}</strong></div>
        </div>
        <div class="header-right">
          <button class="icon-button guide-button" aria-label="工作台使用指南" title="工作台使用指南" @click="guideOpen = true"><el-icon><QuestionFilled /></el-icon></button>
          <a :href="userSiteUrl" target="_blank" rel="noopener noreferrer" class="visit-site"><el-icon><TopRight /></el-icon><span>访问用户端</span></a><span class="header-divider"></span>
          <el-dropdown trigger="click" @command="handleAccountCommand">
            <button class="profile-button" aria-label="管理员账号菜单"><el-avatar :size="34" :src="user?.avatar">{{ (user?.nickname || '管').slice(0, 1) }}</el-avatar><span class="profile-copy"><strong>{{ user?.nickname || '管理员' }}</strong><small>系统管理员</small></span><el-icon class="profile-chevron"><ArrowDown /></el-icon></button>
            <template #dropdown><el-dropdown-menu><el-dropdown-item command="profile" :icon="User">管理员资料</el-dropdown-item><el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
        </div>
      </header>
      <div ref="contentRef" class="workspace-content">
        <div class="content-inner"><router-view /></div>
        <footer class="workspace-footer"><span>宠物领养及健康管理平台</span><span>用心照顾，让爱延续</span></footer>
      </div>
      <div v-if="checking" class="session-status" role="status"><el-icon class="is-loading"><Loading /></el-icon>正在确认管理员身份</div>
    </main>
    <el-dialog v-model="guideOpen" title="工作台使用指南" width="760px"><p class="guide-intro">从档案维护到领养审核，在这里完成平台的日常管理。</p><div class="guide-grid"><router-link v-for="item in guideItems" :key="item.path" :to="item.path" @click="guideOpen = false"><span class="guide-icon"><el-icon><component :is="item.icon" /></el-icon></span><div><strong>{{ item.label }}</strong><p>{{ item.description }}</p></div><el-icon><TopRight /></el-icon></router-link></div><p class="guide-note">各模块均支持按条件查找与分页浏览，详细资料可通过列表中的“详情”或“预览”查看。</p></el-dialog>
    <el-dialog v-model="profileOpen" title="管理员资料" width="660px"><div class="detail-lead"><el-avatar :size="52" :src="user?.avatar">{{ (user?.nickname || '管').slice(0, 1) }}</el-avatar><div><h2>{{ user?.nickname || user?.username }}</h2><p>系统管理员 · @{{ user?.username }}</p></div></div><DetailSections :sections="accountSections" /><template #footer><el-button @click="profileOpen = false">关闭资料</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Bell, DocumentChecked, Expand, FirstAidKit, Fold, House, Loading, Notebook, QuestionFilled, SwitchButton, TopRight, User } from '@element-plus/icons-vue'
import PawMark from './PawMark.vue'
import DetailSections from './DetailSections.vue'
import request, { expireSession } from '../utils/request'
const route = useRoute()
const user = ref(readCachedUser())
const collapsed = ref(false)
const guideOpen = ref(false)
const profileOpen = ref(false)
const mobileOpen = ref(false)
const mobileQuery = window.matchMedia('(max-width: 640px)')
const isMobile = ref(mobileQuery.matches)
const checking = ref(false)
const contentRef = ref(null)
const serviceState = ref('checking')
const serviceText = computed(() => ({ checking: '正在连接服务', online: '服务运行正常', offline: '服务连接异常' }[serviceState.value]))
const navigationExpanded = computed(() => isMobile.value ? mobileOpen.value : !collapsed.value)
const userSiteUrl = `${window.location.protocol}//${window.location.hostname}:3000`
const menuGroups = [
  { label: '工作空间', items: [{ path: '/dashboard', label: '运营概览', icon: House }] },
  { label: '领养与照护', items: [
    { path: '/pets', label: '宠物档案', icon: PawMark },
    { path: '/adoptions', label: '领养审核', icon: DocumentChecked },
    { path: '/health', label: '健康记录', icon: FirstAidKit },
    { path: '/vaccine', label: '疫苗记录', icon: Notebook }
  ] },
  { label: '平台管理', items: [
    { path: '/users', label: '用户管理', icon: User },
    { path: '/announcements', label: '公告资讯', icon: Bell }
  ] }
]
const pageTitle = computed(() => menuGroups.flatMap(group => group.items).find(item => item.path === route.path)?.label || '管理后台')
const guideDescriptions = { '/dashboard': '查看运营统计、宠物状态与最新领养申请。', '/pets': '维护宠物基本资料、领养状态和可追溯来源。', '/adoptions': '核对申请资料，记录通过或拒绝结果。', '/health': '管理体检、诊疗、体征与来源记录。', '/vaccine': '维护接种项目、日期与下次接种计划。', '/users': '查看账号资料，管理用户的访问状态。', '/announcements': '编辑公告与养宠资讯，预览、发布或下架。' }
const guideItems = menuGroups.flatMap(group => group.items).map(item => ({ ...item, description: guideDescriptions[item.path] }))
const accountSections = computed(() => [{ title: '账号信息', fields: [{ label: '用户名', value: user.value?.username }, { label: '昵称', value: user.value?.nickname }, { label: '手机号', value: user.value?.phone }, { label: '电子邮箱', value: user.value?.email }, { label: '账号角色', value: '系统管理员' }, { label: '加入时间', value: user.value?.createdAt ? String(user.value.createdAt).replace('T', ' ').slice(0, 16) : '暂未记录' }] }])
function readCachedUser() {
  try { return JSON.parse(localStorage.getItem('adminUser') || 'null') }
  catch { localStorage.removeItem('adminUser'); return null }
}
function toggleNavigation() {
  if (isMobile.value) mobileOpen.value = !mobileOpen.value
  else collapsed.value = !collapsed.value
}
function handleViewportChange(event) { isMobile.value = event.matches; mobileOpen.value = false }
function closeOnEscape(event) { if (event.key === 'Escape') mobileOpen.value = false }
function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('adminUser')
  window.location.href = '/login'
}
function handleAccountCommand(command) { if (command === 'logout') logout(); else if (command === 'profile') profileOpen.value = true }
async function loadUser() {
  checking.value = true
  try {
    const res = await request.get('/auth/me')
    if (res.code === 401 || (res.code === 200 && res.data?.role !== 1)) { expireSession(); return }
    if (res.code !== 200 || !res.data) throw new Error('同步失败')
    const { token, password, ...account } = res.data
    user.value = account
    localStorage.setItem('adminUser', JSON.stringify(account))
  } catch (error) {
    if (error.response?.status === 401 || error.response?.data?.code === 401 || !user.value) expireSession()
    else ElMessage.warning('暂时无法同步管理员信息，当前显示缓存内容')
  } finally { checking.value = false }
}
let healthAbort
async function checkService() {
  healthAbort = new AbortController()
  const timeout = setTimeout(() => healthAbort.abort(), 8000)
  try {
    const response = await fetch('/api/system/health', { signal: healthAbort.signal })
    const result = await response.json()
    serviceState.value = response.ok && result.data?.status === 'UP' && result.data?.database === 'UP' ? 'online' : 'offline'
  } catch { serviceState.value = 'offline' }
  finally { clearTimeout(timeout) }
}
watch(() => route.path, () => { contentRef.value?.scrollTo({ top: 0 }); mobileOpen.value = false })
onMounted(() => {
  loadUser(); checkService()
  mobileQuery.addEventListener('change', handleViewportChange)
  window.addEventListener('keydown', closeOnEscape)
})
onBeforeUnmount(() => {
  healthAbort?.abort()
  mobileQuery.removeEventListener('change', handleViewportChange)
  window.removeEventListener('keydown', closeOnEscape)
})
</script>

<style scoped>
.guide-intro { color: #83917d; font-size: 12px; line-height: 1.8; margin-bottom: 20px; }
.guide-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.guide-grid a { display: flex; align-items: flex-start; gap: 12px; padding: 17px; border: 1px solid #e6ece0; border-radius: 11px; text-decoration: none; background: #fafbf8; }
.guide-grid a:hover { border-color: #b4c8a8; background: #f2f7ed; }
.guide-grid a > .el-icon { margin-left: auto; color: #a5b299; font-size: 12px; flex-shrink: 0; }
.guide-icon { display: grid; place-items: center; flex: 0 0 32px; height: 32px; background: #eaf1e3; color: #6d8b59; border-radius: 9px; font-size: 17px; }
.guide-grid strong { color: #47633e; font-size: 13px; font-weight: 600; }
.guide-grid p { color: #8b9c7e; font-size: 11px; margin-top: 6px; line-height: 1.8; }
.guide-note { margin-top: 20px; color: #a1ac97; font-size: 10px; line-height: 1.8; }
@media (max-width: 560px) { .guide-grid { grid-template-columns: 1fr; } }
.admin-layout { display: flex; height: 100vh; height: 100dvh; overflow: hidden; background: var(--admin-bg); }
.sidebar { width: 232px; flex: 0 0 232px; display: flex; flex-direction: column; background: #fff; border-right: 1px solid var(--admin-line); transition: width .22s ease, flex-basis .22s ease, transform .22s ease; z-index: 30; }
.sidebar-brand { height: 90px; flex-shrink: 0; display: flex; align-items: center; gap: 11px; padding: 0 24px; text-decoration: none; color: var(--admin-ink); }
.brand-mark { display: grid; place-items: center; width: 39px; height: 39px; flex: 0 0 39px; border-radius: 12px; color: #fff; background: var(--admin-brand); box-shadow: 0 4px 8px #28745b18; }
.brand-mark svg { width: 24px; height: 24px; }
.brand-copy { display: flex; flex-direction: column; white-space: nowrap; gap: 5px; }
.brand-copy strong { font-size: 16px; letter-spacing: -.4px; }
.brand-copy small { font-size: 9px; font-weight: 600; letter-spacing: 2.2px; color: #8b978e; }
.sidebar-nav { padding: 10px 14px; flex: 1; overflow-y: auto; scrollbar-width: thin; }
.nav-group + .nav-group { margin-top: 25px; }
.nav-group-label { padding: 0 14px; margin-bottom: 10px; font-size: 11px; color: #939d96; letter-spacing: 1px; }
.nav-item { display: flex; height: 46px; align-items: center; gap: 13px; padding: 0 15px; margin: 5px 0; border-radius: 9px; color: #68776e; text-decoration: none; transition: background .18s ease, color .18s ease; white-space: nowrap; font-size: 13px; font-weight: 500; }
.nav-item > .el-icon { font-size: 19px; flex-shrink: 0; }
.nav-item:hover { color: var(--admin-brand); background: #f3f7f3; }
.nav-item.router-link-active { background: var(--admin-brand); color: white; box-shadow: 0 3px 6px #28745b14; }
.nav-active-dot { display: none; width: 5px; height: 5px; border-radius: 50%; background: #a9d1b9; margin-left: auto; }
.router-link-active .nav-active-dot { display: block; }
.sidebar-bottom { padding: 20px 22px; }
.care-note { position: relative; padding: 17px 14px; overflow: hidden; border: 1px solid #eaf0e7; border-radius: 12px; background: #f5f8f1; }
.care-note-icon { display: block; width: 27px; height: 27px; color: #5e8768; margin-bottom: 12px; transform: rotate(-12deg); }
.care-note strong { font-size: 11px; font-weight: 600; color: #526e50; position: relative; }
.care-note p { margin-top: 6px; font-size: 10px; color: #8a9784; }
.service-status { display: flex; align-items: center; gap: 8px; margin-top: 22px; font-size: 10px; color: #879489; }
.status-dot { flex: 0 0 6px; height: 6px; border-radius: 50%; background: #a3ada6; }
.status-dot.online { background: #68a97b; box-shadow: 0 0 0 3px #68a97b12; }
.status-dot.offline { background: #d48662; }
.version { margin-left: auto; color: #a4aca6; }
.main-content { min-width: 0; flex: 1; display: flex; flex-direction: column; }
.workspace-header { height: 76px; flex: 0 0 76px; display: flex; justify-content: space-between; align-items: center; padding: 0 32px; background: #fff; border-bottom: 1px solid var(--admin-line); gap: 16px; }
.header-left, .header-right { display: flex; align-items: center; gap: 20px; }
.icon-button { display: grid; place-items: center; width: 32px; height: 32px; color: #7c8880; border: none; border-radius: 7px; background: transparent; cursor: pointer; font-size: 18px; }
.icon-button:hover { background: #f1f5f1; color: var(--admin-brand); }
.header-breadcrumb { display: flex; gap: 16px; align-items: center; font-size: 12px; white-space: nowrap; }
.header-breadcrumb > span { color: #8d988f; }
.header-breadcrumb .breadcrumb-slash { color: #d0d7d1; font-size: 14px; }
.header-breadcrumb strong { color: #4b6052; font-weight: 500; }
.visit-site { display: flex; align-items: center; gap: 6px; text-decoration: none; color: #6e7f73; font-size: 11px; }
.visit-site:hover { color: var(--admin-brand); }
.visit-site .el-icon { font-size: 16px; }
.header-divider { height: 22px; width: 1px; background: var(--admin-line); }
.profile-button { border: none; background: transparent; display: flex; align-items: center; gap: 10px; cursor: pointer; padding: 5px; border-radius: 8px; }
.profile-button:hover { background: #f7f9f6; }
.profile-button .el-avatar { color: #427b5f; background: #edf3e9; border: 1px solid #e0e9da; font-size: 14px; font-weight: 600; }
.profile-copy { display: flex; flex-direction: column; gap: 4px; text-align: left; }
.profile-copy strong { color: var(--admin-ink); font-size: 12px; font-weight: 600; }
.profile-copy small { font-size: 10px; color: #8d988f; }
.profile-chevron { color: #89958d; font-size: 11px; margin-left: 8px; }
.workspace-content { overflow-y: auto; flex: 1; padding: 30px 32px 0; scrollbar-width: thin; scrollbar-color: #d7dfd6 transparent; }
.content-inner { max-width: 1520px; margin: 0 auto; min-height: calc(100% - 68px); }
.workspace-footer { max-width: 1520px; margin: 24px auto 0; display: flex; justify-content: space-between; padding: 18px 0 22px; border-top: 1px solid #e7ebe4; font-size: 10px; letter-spacing: .5px; color: #9aa498; gap: 12px; }
.session-status { position: fixed; bottom: 20px; right: 24px; display: flex; align-items: center; gap: 8px; padding: 11px 16px; color: var(--admin-muted); font-size: 12px; background: white; border: 1px solid var(--admin-line); border-radius: 10px; box-shadow: 0 4px 16px #263b3310; z-index: 50; }
.nav-collapsed .sidebar { width: 80px; flex-basis: 80px; }
.nav-collapsed .sidebar-brand { padding: 0 20px; }
.nav-collapsed .brand-copy, .nav-collapsed .nav-group-label, .nav-collapsed .nav-label, .nav-collapsed .nav-active-dot, .nav-collapsed .care-note, .nav-collapsed .service-copy, .nav-collapsed .version { display: none; }
.nav-collapsed .sidebar-nav { padding: 10px 12px; }
.nav-collapsed .nav-item { justify-content: center; padding: 0; }
.nav-collapsed .nav-group + .nav-group { border-top: 1px solid var(--admin-line); margin-top: 18px; padding-top: 18px; }
.nav-collapsed .service-status { justify-content: center; }
.nav-backdrop { position: fixed; inset: 0; z-index: 25; background: #15291d5c; border: none; }
@media (max-width: 1100px) { .sidebar { width: 210px; flex-basis: 210px; } .sidebar-brand { padding: 0 18px; } .workspace-header { padding: 0 24px; } .workspace-content { padding: 24px 24px 0; } }
@media (max-width: 760px) { .visit-site span, .header-divider { display: none; } .header-left, .header-right { gap: 10px; } .header-breadcrumb { gap: 9px; } .workspace-header { padding: 0 16px; } .workspace-content { padding: 22px 16px 0; } }
@media (max-width: 640px) {
  .sidebar, .nav-collapsed .sidebar { position: fixed; inset: 0 auto 0 0; width: 232px; transform: translateX(-100%); box-shadow: 8px 0 40px #1b372418; }
  .sidebar.mobile-open { transform: translateX(0); }
  .nav-collapsed .sidebar-brand { padding: 0 24px; }
  .nav-collapsed .brand-copy { display: flex; }
  .nav-collapsed .nav-group-label, .nav-collapsed .nav-label, .nav-collapsed .care-note, .nav-collapsed .service-copy, .nav-collapsed .version { display: block; }
  .nav-collapsed .nav-item { justify-content: flex-start; padding: 0 15px; }
  .nav-collapsed .nav-group + .nav-group { border: none; padding: 0; margin-top: 25px; }
  .nav-collapsed .service-status { justify-content: flex-start; }
  .workspace-header { height: 66px; flex-basis: 66px; }
  .header-breadcrumb > span, .profile-copy, .profile-chevron { display: none; }
  .workspace-footer { font-size: 9px; }
}
</style>
