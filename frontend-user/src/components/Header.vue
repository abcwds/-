<template>
  <header class="site-header" :class="{ compact }">
    <div class="header-inner">
      <router-link to="/" class="brand" aria-label="宠物领养首页">
        <span class="brand-symbol"><PawMark /></span>
        <span class="brand-copy"><strong>宠物领养</strong><small>让爱，有个家。</small></span>
      </router-link>
      <router-link v-if="compact" to="/" class="back-home"><el-icon><ArrowLeft /></el-icon>返回首页</router-link>
      <template v-else>
        <nav id="primary-navigation" class="primary-nav" :class="{ open: menuOpen }" aria-label="主导航">
          <router-link v-for="item in links" :key="item.path" :to="item.path" :class="{ active: activePath(item.path) }">{{ item.label }}</router-link>
        </nav>
        <div class="header-actions">
          <template v-if="user">
            <router-link to="/adoptions" class="adoptions-link">我的领养</router-link>
            <el-dropdown trigger="click" @command="handleCommand">
              <button class="account-button" aria-label="打开个人账号菜单">
                <el-avatar :size="34" :src="user.avatar">{{ (user.nickname || user.username || '友').slice(0, 1) }}</el-avatar>
                <span>{{ user.nickname || user.username }}</span><el-icon class="chevron"><ArrowDown /></el-icon>
              </button>
              <template #dropdown><el-dropdown-menu>
                <el-dropdown-item command="/profile" :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item command="/adoptions" :icon="DocumentChecked">我的领养</el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu></template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link :to="{ path: '/login', query: { redirect: destination } }" class="login-link">登录</router-link>
            <router-link :to="{ path: '/register', query: { redirect: destination } }" class="join-link">加入我们<el-icon><ArrowRight /></el-icon></router-link>
          </template>
          <button class="menu-toggle" :aria-expanded="menuOpen" aria-controls="primary-navigation" :aria-label="menuOpen ? '收起导航' : '展开导航'" @click="menuOpen = !menuOpen"><el-icon><Close v-if="menuOpen" /><Menu v-else /></el-icon></button>
        </div>
      </template>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, ArrowLeft, ArrowRight, Close, DocumentChecked, Menu, SwitchButton, User } from '@element-plus/icons-vue'
import PawMark from './PawMark.vue'
import { currentUser as user, clearSession, safeRedirect } from '../utils/session'
defineProps({ compact: Boolean })
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const links = [{ path: '/', label: '首页' }, { path: '/pets', label: '寻找伙伴' }, { path: '/health-radar', label: '健康雷达' }, { path: '/care-plan', label: '照护计划' }, { path: '/announcements', label: '公告资讯' }]
const destination = computed(() => safeRedirect(route.fullPath))
const activePath = path => path === '/' ? route.path === '/' : route.path === path || (path === '/pets' && route.path.startsWith('/pet/'))
watch(() => route.fullPath, () => { menuOpen.value = false })
function handleCommand(command) {
  if (command === 'logout') { clearSession(); router.replace('/login'); return }
  router.push(command)
}
function onEscape(event) { if (event.key === 'Escape') menuOpen.value = false }
onMounted(() => window.addEventListener('keydown', onEscape))
onBeforeUnmount(() => window.removeEventListener('keydown', onEscape))
</script>

<style scoped>
.site-header { position: sticky; top: 0; z-index: 100; background: #ffffffef; backdrop-filter: blur(14px); border-bottom: 1px solid #e5eadf; }
.header-inner { position: relative; max-width: 1240px; min-height: 80px; padding: 0 28px; display: flex; align-items: center; justify-content: space-between; gap: 24px; margin: 0 auto; }
.brand { display: flex; align-items: center; gap: 11px; flex-shrink: 0; color: #263b33; text-decoration: none; }
.brand-symbol { width: 40px; height: 40px; display: grid; place-items: center; color: white; background: #28745b; border-radius: 13px; }
.brand-symbol svg { width: 25px; height: 25px; }
.brand-copy { display: flex; flex-direction: column; gap: 3px; }
.brand-copy strong { font-size: 19px; font-weight: 650; letter-spacing: 1px; }
.brand-copy small { color: #879580; font-size: 10px; letter-spacing: 2px; }
.primary-nav { display: flex; gap: 28px; align-items: center; justify-content: center; }
.primary-nav a { position: relative; color: #6d7a6e; font-size: 13px; line-height: 80px; text-decoration: none; white-space: nowrap; transition: color .18s; }
.primary-nav a:hover, .primary-nav a.active { color: #28745b; }
.primary-nav a.active::after { position: absolute; content: ''; left: 50%; bottom: 17px; transform: translateX(-50%); width: 15px; height: 3px; border-radius: 3px; background: #28745b; }
.header-actions { display: flex; align-items: center; gap: 20px; }
.login-link, .adoptions-link { color: #5f7362; font-size: 13px; white-space: nowrap; text-decoration: none; }
.join-link { display: flex; align-items: center; gap: 10px; padding: 11px 17px; border-radius: 9px; background: #28745b; color: white; font-size: 12px; text-decoration: none; white-space: nowrap; }
.join-link:hover { background: #234d3d; }
.account-button { border: none; background: transparent; color: #3e5947; display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px; border-radius: 8px; font-size: 12px; }
.account-button > span:not(.el-avatar) { max-width: 92px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.account-button .el-avatar { color: #557852; background: #edf2e6; font-weight: 600; border: 1px solid #e1e8d8; }
.account-button .chevron { font-size: 10px; color: #8c9988; margin-left: 4px; }
.menu-toggle { display: none; border: 1px solid #e1e7dc; border-radius: 8px; background: white; color: #426a4c; width: 35px; height: 35px; place-items: center; font-size: 18px; cursor: pointer; }
.back-home { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #71816d; text-decoration: none; }
.compact { position: relative; background: #f7f8f4; border-bottom: none; }
.compact .header-inner { min-height: 86px; }
@media (max-width: 1050px) { .primary-nav { gap: 17px; } .header-inner { gap: 16px; padding: 0 22px; } .header-actions { gap: 12px; } .brand-copy small { display: none; } .brand-copy strong { font-size: 17px; } }
@media (max-width: 850px) {
  .header-inner { min-height: 70px; padding: 0 20px; }
  .primary-nav { display: none; position: absolute; top: 100%; left: 0; right: 0; background: white; border-bottom: 1px solid #e3e9df; padding: 12px 20px 18px; box-shadow: 0 12px 20px #263b3308; }
  .primary-nav.open { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; }
  .primary-nav a { padding: 12px 5px; line-height: 1.6; text-align: center; border-radius: 8px; }
  .primary-nav a.active { background: #eff4e9; }
  .primary-nav a.active::after { display: none; }
  .menu-toggle { display: grid; }
}
@media (max-width: 480px) {
  .header-inner { padding: 0 16px; gap: 12px; min-height: 68px; }
  .brand-symbol { width: 34px; height: 34px; border-radius: 11px; }
  .brand-symbol svg { width: 22px; height: 22px; }
  .brand-copy strong { font-size: 16px; letter-spacing: .5px; }
  .header-actions { gap: 12px; }
  .join-link { padding: 9px 12px; font-size: 11px; }
  .join-link .el-icon, .adoptions-link, .account-button > span:not(.el-avatar), .account-button .chevron { display: none; }
  .compact .header-inner { min-height: 74px; }
}
</style>
