<template>
  <div class="profile-page">
    <div class="profile-container">
      <div class="page-heading"><div><span class="page-eyebrow">MY LITTLE CORNER</span><h1>个人中心</h1><p>把关于你的故事，和每一次温暖的相遇，收藏在这里。</p></div><el-button :icon="Refresh" :loading="refreshing" @click="refreshData">刷新资料</el-button></div>
      <section class="identity-card">
        <div class="identity"><el-avatar :size="76" :src="user?.avatar">{{ initial }}</el-avatar><div><span class="identity-eyebrow">很高兴，再次见到你</span><h2>{{ user?.nickname || user?.username || '新朋友' }}<span class="member-badge">{{ user?.role === 1 ? '平台管理员' : '爱心领养人' }}</span></h2><p>@{{ user?.username || '加载中' }}<span v-if="user?.createdAt">加入于 {{ formatDate(user.createdAt) }}</span></p></div></div>
        <button class="edit-profile-button" @click="selectTab('settings')"><el-icon><EditPen /></el-icon>编辑个人资料<el-icon><ArrowRight /></el-icon></button>
        <span class="hero-paw" aria-hidden="true"><PawMark /></span>
      </section>
      <el-alert v-if="userError" title="个人资料暂未同步，当前可能显示已缓存的信息" type="warning" :closable="false" show-icon><el-button link @click="loadUser">重新加载</el-button></el-alert>
      <div class="personal-stats" :aria-busy="refreshing">
        <router-link v-for="item in statItems" :key="item.key" :to="item.to" class="personal-stat" :class="item.tone"><span class="stat-label"><span class="stat-icon"><el-icon><component :is="item.icon" /></el-icon></span>{{ item.label }}<el-icon class="stat-arrow"><TopRight /></el-icon></span><strong>{{ refreshing ? '···' : summary[item.key] === null ? '—' : summary[item.key] }}<small>份</small></strong><p>{{ item.description }}</p></router-link>
      </div>
      <p v-if="summaryError" class="inline-error">部分申请统计暂时不可用，刷新资料后可重试。</p>
      <div class="profile-tabs" role="group" aria-label="个人中心栏目"><button v-for="tab in tabs" :key="tab.key" :class="{ active: selectedTab === tab.key }" :aria-pressed="selectedTab === tab.key" @click="selectTab(tab.key)"><el-icon><component :is="tab.icon" /></el-icon>{{ tab.label }}</button></div>

      <div v-if="selectedTab === 'overview'" class="overview-layout">
        <section class="panel recent-panel"><div class="panel-heading"><div><h2>最近的领养申请</h2><p>每一份期待，都值得被认真回应。</p></div><router-link to="/adoptions">全部记录<el-icon><ArrowRight /></el-icon></router-link></div>
          <el-skeleton v-if="refreshing" :rows="4" animated class="panel-skeleton" />
          <div v-else-if="recentError" class="empty-state" role="alert"><span class="empty-mark"><Warning /></span><h3>申请记录暂时无法加载</h3><p>请检查连接后再试，你的记录会保留。</p><el-button @click="refreshData">重新加载</el-button></div>
          <div v-else-if="recent.length" class="recent-list"><router-link v-for="app in recent" :key="app.id" :to="'/adoptions?status=' + app.status" class="recent-row"><img :src="resolvePetImage(app.petImage)" :style="getPetImageStyle(app.petImage)" :alt="app.petName || '领养宠物'" @error="handlePetImageError($event, app.petImage)" /><div class="recent-copy"><strong>{{ app.petName || '宠物伙伴' }}</strong><span>申请 #{{ app.id }} · {{ formatDate(app.createdAt) }}</span></div><span class="application-status" :class="statusClass(app.status)">{{ statusText(app.status) }}</span><el-icon class="recent-arrow"><ArrowRight /></el-icon></router-link></div>
          <div v-else class="empty-state"><span class="empty-mark"><PawMark /></span><h3>你的陪伴故事，即将开始</h3><p>还没有领养申请，先去认识等待一个家的它们吧。</p><router-link to="/pets" class="text-link">寻找心动的伙伴<el-icon><ArrowRight /></el-icon></router-link></div>
        </section>
        <aside class="panel quick-panel"><div class="panel-heading"><div><h2>我的照护小站</h2><p>让陪伴，更有准备。</p></div><span class="small-paw"><PawMark /></span></div><router-link v-for="item in shortcuts" :key="item.to" :to="item.to" class="shortcut"><span class="shortcut-icon" :class="item.tone"><el-icon><component :is="item.icon" /></el-icon></span><div><strong>{{ item.title }}</strong><span>{{ item.note }}</span></div><el-icon><ArrowRight /></el-icon></router-link><div class="care-hint"><el-icon><Sunny /></el-icon><p>认真了解它的习惯，<br />也是一份温柔的领养准备。</p></div></aside>
        <section class="companionship-banner"><span><PawMark /></span><div><h2>家，是有你在的地方。</h2><p>给它一点时间，也给彼此一个温暖的开始。</p></div><router-link to="/pets">去认识它们<el-icon><ArrowRight /></el-icon></router-link></section>
      </div>

      <div v-if="selectedTab === 'settings'" class="settings-layout">
        <section class="panel settings-panel"><div class="panel-heading"><div><h2>你的个人资料</h2><p>留下一份准确的信息，让每一次沟通更顺畅。</p></div><span v-if="saved && !dirty" class="saved-label"><el-icon><CircleCheck /></el-icon>已保存</span></div>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="loadingUser || userError || saving" class="profile-form" @submit.prevent="saveProfile">
            <div class="form-grid"><el-form-item label="用户名"><el-input :model-value="user?.username || ''" disabled /><span class="field-note">用户名用于登录，暂不支持修改。</span></el-form-item><el-form-item label="昵称" prop="nickname"><el-input v-model.trim="form.nickname" maxlength="30" autocomplete="nickname" placeholder="希望我们怎么称呼你" /></el-form-item><el-form-item label="手机号" prop="phone"><el-input v-model.trim="form.phone" type="tel" inputmode="tel" maxlength="11" autocomplete="tel" placeholder="请输入常用手机号" /></el-form-item><el-form-item label="电子邮箱" prop="email"><el-input v-model.trim="form.email" autocomplete="email" placeholder="请输入常用邮箱" /></el-form-item></div>
            <el-divider />
            <div class="avatar-editor"><el-avatar :size="58" :src="form.avatar">{{ (form.nickname || user?.username || '友').slice(0, 1) }}</el-avatar><div><h3>个人头像</h3><p>可填写公开图片链接，或使用昵称首字作为头像。</p></div></div>
            <el-form-item label="头像链接（选填）" prop="avatar"><el-input v-model.trim="form.avatar" placeholder="https://… 或 /uploads/…" clearable /></el-form-item>
            <div class="form-actions"><span>{{ dirty ? '有尚未保存的修改' : '资料已与账号同步' }}</span><el-button :disabled="!dirty" @click="resetForm">取消修改</el-button><el-button type="primary" native-type="submit" :loading="saving" :disabled="!dirty || userError || loadingUser">保存修改</el-button></div>
          </el-form>
        </section>
        <aside class="settings-aside"><section class="panel completeness-panel"><span class="aside-icon"><User /></span><h2>让我们更了解你</h2><p>完善昵称、联系方式和头像，<br />让彼此的沟通多一份安心。</p><div class="completeness-label"><span>资料完善度</span><strong>{{ completeness }}%</strong></div><el-progress :percentage="completeness" :show-text="false" :stroke-width="5" color="#88a67b" /><span class="completeness-note">按已保存的昵称、手机、邮箱和头像计算</span></section><section class="account-note"><el-icon><Lock /></el-icon><div><strong>好好保管你的账号</strong><p>使用个人设备登录，离开时记得退出账号。密码不会在这里显示。</p></div></section></aside>
      </div>
      <ProfilePlatform v-if="platformVisited" v-show="selectedTab === 'platform'" :active="selectedTab === 'platform'" :summary="summary" />
    </div>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Calendar, CircleCheck, Close, DataAnalysis, Document, DocumentChecked, EditPen, FirstAidKit, Lock, Refresh, Sunny, TopRight, User, Warning } from '@element-plus/icons-vue'
import PawMark from '../components/PawMark.vue'
import request from '../utils/request'
import { currentUser as user, setUser } from '../utils/session'
import { getPetImageStyle, handlePetImageError, resolvePetImage } from '../utils/petImage'
const ProfilePlatform = defineAsyncComponent(() => import('../components/ProfilePlatform.vue'))
const route = useRoute()
const router = useRouter()
const tabs = [{ key: 'overview', label: '个人概览', icon: User }, { key: 'settings', label: '资料设置', icon: EditPen }, { key: 'platform', label: '平台数据', icon: DataAnalysis }]
const selectedTab = computed(() => tabs.some(tab => tab.key === route.query.tab) ? route.query.tab : 'overview')
const platformVisited = ref(false)
watch(selectedTab, value => { if (value === 'platform') platformVisited.value = true }, { immediate: true })
const initial = computed(() => (user.value?.nickname || user.value?.username || '友').slice(0, 1))
const summary = reactive({ all: null, pending: null, approved: null, rejected: null })
const statItems = [{ key: 'all', label: '我的申请', description: '记录每一次认真的期待', to: '/adoptions', icon: Document, tone: 'green' }, { key: 'pending', label: '待审核', description: '静候一次温暖的回应', to: '/adoptions?status=0', icon: Calendar, tone: 'amber' }, { key: 'approved', label: '已通过', description: '新的陪伴，即将开启', to: '/adoptions?status=1', icon: DocumentChecked, tone: 'blue' }, { key: 'rejected', label: '未通过', description: '下一次相遇，依然值得期待', to: '/adoptions?status=2', icon: Close, tone: 'rose' }]
const shortcuts = [{ to: '/pets', title: '寻找伙伴', note: '认识等待新家的小生命', icon: PawMark, tone: 'green' }, { to: '/health-radar', title: '健康雷达', note: '了解档案中的健康变化', icon: FirstAidKit, tone: 'blue' }, { to: '/care-plan', title: '照护计划', note: '把日常关爱安排妥当', icon: Calendar, tone: 'amber' }]
const recent = ref([])
const recentError = ref(false)
const summaryError = ref(false)
const userError = ref(false)
const loadingUser = ref(true)
const refreshing = ref(false)
const saving = ref(false)
const saved = ref(false)
const formRef = ref(null)
const form = reactive({ nickname: '', phone: '', email: '', avatar: '' })
const pickFields = value => ({ nickname: value?.nickname || '', phone: value?.phone || '', email: value?.email || '', avatar: value?.avatar || '' })
const dirty = computed(() => JSON.stringify(form) !== JSON.stringify(pickFields(user.value)))
const completeness = computed(() => Object.values(pickFields(user.value)).filter(value => String(value).trim()).length * 25)
const rules = {
  nickname: [{ required: true, whitespace: true, message: '请输入昵称', trigger: 'blur' }, { max: 30, message: '昵称不能超过30个字符', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }],
  email: [{ required: true, message: '请输入电子邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  avatar: [{ validator: (_, value, callback) => !value || /^https?:\/\/[^\s]+$/i.test(value) || /^\/uploads\/[^\s]*$/.test(value) ? callback() : callback(new Error('请使用 http(s) 图片链接或 /uploads/ 路径')), trigger: 'blur' }]
}
let disposed = false
let userSequence = 0
function selectTab(tab) { router.replace({ path: '/profile', query: tab === 'overview' ? {} : { tab } }) }
function resetForm() { Object.assign(form, pickFields(user.value)); saved.value = false; formRef.value?.clearValidate() }
async function loadUser() {
  const sequence = ++userSequence
  loadingUser.value = true
  userError.value = false
  try {
    const res = await request.get('/auth/me')
    if (res.code !== 200 || !res.data) throw new Error('无法同步资料')
    if (disposed || sequence !== userSequence) return
    setUser(res.data)
    resetForm()
  } catch { if (!disposed && sequence === userSequence) userError.value = true }
  finally { if (!disposed && sequence === userSequence) loadingUser.value = false }
}
async function refreshData() {
  if (refreshing.value) return
  refreshing.value = true
  summaryError.value = false
  recentError.value = false
  await Promise.all([
    loadUser(),
    ...[['all', null], ['pending', 0], ['approved', 1], ['rejected', 2]].map(async ([key, status]) => {
      try {
        const res = await request.get('/adoptions/my', { params: { page: 1, size: 1, ...(status === null ? {} : { status }) } })
        if (res.code !== 200 || !Number.isFinite(Number(res.data?.total))) throw new Error('统计不可用')
        if (!disposed) summary[key] = Number(res.data.total)
      } catch { if (!disposed) { summary[key] = null; summaryError.value = true } }
    }),
    (async () => {
      try {
        const res = await request.get('/adoptions/my', { params: { page: 1, size: 4 } })
        if (res.code !== 200 || !Array.isArray(res.data?.records)) throw new Error('申请不可用')
        if (!disposed) recent.value = res.data.records
      } catch { if (!disposed) { recent.value = []; recentError.value = true } }
    })()
  ])
  if (!disposed) refreshing.value = false
}
async function saveProfile() {
  if (!formRef.value || saving.value || loadingUser.value || userError.value) return
  if (!await formRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    const res = await request.put('/auth/profile', { ...form })
    if (res.code !== 200 || !res.data) throw new Error(res.message || '保存失败')
    if (disposed) return
    setUser(res.data)
    Object.assign(form, pickFields(res.data))
    formRef.value?.clearValidate()
    saved.value = true
    ElMessage.success('个人资料已保存')
  } catch (error) { if (!disposed) ElMessage.error(error.response?.data?.message || error.message || '保存失败，请稍后重试') }
  finally { if (!disposed) saving.value = false }
}
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 10) : '时间未记录' }
const statusText = status => ({ 0: '待审核', 1: '已通过', 2: '未通过' })[status] || '状态待确认'
const statusClass = status => ({ 0: 'pending', 1: 'approved', 2: 'rejected' })[status] || ''
Object.assign(form, pickFields(user.value))
refreshData()
onBeforeUnmount(() => { disposed = true; userSequence++ })
</script>

<style scoped>
.profile-page { color: #263b33; }
.profile-container { max-width: 1160px; padding: 20px 28px 0; margin: 0 auto; }
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
.page-eyebrow { color: #91a184; font-size: 9px; letter-spacing: 2px; }
.page-heading h1 { font-size: 29px; font-weight: 600; margin: 10px 0; letter-spacing: .6px; }
.page-heading p { font-size: 12px; line-height: 1.8; color: #85917b; }
.page-heading .el-button { height: 36px; font-size: 12px; }
.identity-card { position: relative; overflow: hidden; display: flex; align-items: center; justify-content: space-between; gap: 24px; border: 1px solid #dfe7d4; border-radius: 17px; background: #eaf0e3; padding: 28px 34px; margin-bottom: 24px; }
.identity { position: relative; z-index: 1; display: flex; align-items: center; gap: 21px; min-width: 0; }
.identity .el-avatar { color: #5d7f55; background: #f8faf3; border: 4px solid #ffffff9c; font-size: 28px; flex-shrink: 0; }
.identity-eyebrow { color: #849875; font-size: 10px; letter-spacing: 1px; }
.identity h2 { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin: 8px 0; font-size: 25px; font-weight: 600; color: #35563c; overflow-wrap: anywhere; }
.member-badge { padding: 3px 8px; font-size: 9px; font-weight: 500; border-radius: 5px; color: #839365; border: 1px solid #d6dfc7; background: #f0f5e7; }
.identity p { display: flex; flex-wrap: wrap; gap: 16px; color: #8c9a7f; font-size: 10px; }
.edit-profile-button { position: relative; z-index: 1; display: flex; align-items: center; gap: 9px; border: 1px solid #d5dfc9; border-radius: 9px; color: #6b835b; background: #f7f9f1d9; padding: 11px 14px; cursor: pointer; font-size: 11px; white-space: nowrap; }
.hero-paw { position: absolute; right: 65px; bottom: -30px; color: #dce6d2; width: 180px; height: 180px; transform: rotate(-20deg); }
.personal-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.personal-stat { display: block; padding: 20px 22px; border: 1px solid #e3e9df; border-radius: 14px; background: white; color: #263b33; text-decoration: none; transition: border-color .2s, transform .2s; }
.personal-stat:hover { border-color: #b3c8a7; transform: translateY(-2px); }
.stat-label { display: flex; align-items: center; gap: 9px; font-size: 12px; color: #7a8b70; }
.stat-icon { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 8px; color: #70906b; background: #f0f5e9; font-size: 15px; }
.amber .stat-icon { color: #b79b69; background: #faf4e6; }
.blue .stat-icon { color: #7d9aab; background: #edf3f6; }
.rose .stat-icon { color: #b18b7d; background: #faf0eb; }
.stat-arrow { margin-left: auto; color: #b2bba5; font-size: 12px; }
.personal-stat > strong { display: block; margin: 10px 0; font-size: 31px; font-weight: 550; color: #3c5841; font-variant-numeric: tabular-nums; }
.personal-stat small { font-size: 11px; color: #a3ad94; font-weight: 400; margin-left: 7px; }
.personal-stat p { color: #99a18e; font-size: 10px; line-height: 1.6; }
.inline-error { font-size: 12px; color: #b08b60; margin-top: 12px; }
.profile-tabs { display: flex; gap: 29px; margin: 26px 0 24px; border-bottom: 1px solid #e3e9df; }
.profile-tabs button { display: flex; align-items: center; gap: 7px; cursor: pointer; padding: 14px 2px; background: none; border: none; border-bottom: 2px solid transparent; color: #89957f; font-size: 13px; }
.profile-tabs button.active { color: #28745b; border-bottom-color: #28745b; }
.profile-tabs .el-icon { font-size: 15px; }
.overview-layout, .settings-layout { display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(290px, 1fr); gap: 22px; align-items: start; }
.panel { min-width: 0; padding: 25px 26px; border: 1px solid #e3e9df; border-radius: 16px; background: white; }
.panel-heading { display: flex; justify-content: space-between; align-items: center; gap: 14px; margin-bottom: 17px; }
.panel-heading h2 { font-size: 15px; font-weight: 600; color: #40583d; }
.panel-heading p { font-size: 10px; color: #9aa38c; margin-top: 7px; line-height: 1.7; }
.panel-heading > a { display: inline-flex; align-items: center; gap: 5px; font-size: 10px; color: #8a9a7c; text-decoration: none; white-space: nowrap; }
.recent-row { display: flex; align-items: center; gap: 12px; padding: 18px 0; border-bottom: 1px solid #eef1e8; text-decoration: none; }
.recent-row:last-child { border-bottom: none; padding-bottom: 4px; }
.recent-row img { width: 48px; height: 48px; object-fit: cover; border-radius: 11px; flex-shrink: 0; background: #f1f4eb; }
.recent-copy { min-width: 0; }
.recent-copy strong { color: #4a6448; font-size: 13px; font-weight: 550; display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.recent-copy span { display: block; margin-top: 7px; font-size: 9px; color: #a0a891; }
.application-status { margin-left: auto; padding: 4px 8px; white-space: nowrap; border-radius: 5px; font-size: 10px; }
.application-status.pending { color: #b1945b; background: #faf4e5; }
.application-status.approved { color: #7a9564; background: #f0f5e8; }
.application-status.rejected { color: #b58976; background: #fcf0e8; }
.recent-arrow { font-size: 10px; color: #b1bba5; }
.empty-state { display: flex; align-items: center; justify-content: center; flex-direction: column; text-align: center; padding: 33px 0 24px; }
.empty-mark { display: grid; place-items: center; width: 64px; height: 64px; border-radius: 20px; background: #f2f5e9; color: #9dae87; margin-bottom: 17px; }
.empty-mark svg { width: 30px; height: 30px; }
.empty-state h3 { font-size: 15px; font-weight: 500; color: #5b7552; }
.empty-state p { font-size: 11px; line-height: 1.8; color: #a0a98e; margin: 10px 0 20px; }
.text-link { display: flex; align-items: center; gap: 6px; color: #63835c; font-size: 12px; text-decoration: none; }
.panel-skeleton { padding: 22px 0; }
.small-paw { width: 25px; height: 25px; color: #b2c19d; transform: rotate(15deg); }
.shortcut { display: flex; align-items: center; gap: 12px; padding: 14px 0; border-bottom: 1px solid #edf0e7; text-decoration: none; }
.shortcut:last-of-type { border-bottom: none; }
.shortcut-icon { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 10px; background: #f0f5e9; color: #83a075; font-size: 17px; flex-shrink: 0; }
.shortcut-icon.blue { color: #87a1ae; background: #eff4f7; }
.shortcut-icon.amber { color: #b29b73; background: #faf5e8; }
.shortcut strong { display: block; color: #627a57; font-size: 12px; font-weight: 500; }
.shortcut div > span { display: block; margin-top: 5px; color: #a0a98f; font-size: 10px; }
.shortcut > .el-icon { margin-left: auto; color: #b1bea1; font-size: 11px; }
.care-hint { display: flex; gap: 12px; align-items: center; padding: 16px; margin-top: 14px; background: #f7f8f0; color: #99a780; border-radius: 9px; }
.care-hint > .el-icon { font-size: 24px; }
.care-hint p { font-size: 10px; line-height: 1.9; }
.companionship-banner { display: flex; align-items: center; gap: 20px; grid-column: 1 / -1; padding: 25px 30px; border: 1px solid #e9e6d5; background: #f5f1e5; border-radius: 14px; }
.companionship-banner > span { width: 40px; height: 40px; color: #c6ba92; flex-shrink: 0; transform: rotate(-15deg); }
.companionship-banner h2 { color: #8a805b; font-size: 16px; font-weight: 500; margin-bottom: 7px; }
.companionship-banner p { color: #aaa18a; font-size: 11px; line-height: 1.7; }
.companionship-banner a { display: flex; gap: 8px; align-items: center; color: #9d8c60; font-size: 12px; margin-left: auto; text-decoration: none; white-space: nowrap; }
.settings-layout { grid-template-columns: minmax(0, 1.8fr) minmax(270px, 1fr); }
.settings-panel { padding: 27px 30px; }
.profile-form { margin-top: 26px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 23px; }
.profile-form :deep(.el-form-item__label) { font-size: 12px; color: #6b805f; }
.profile-form :deep(.el-input__wrapper) { min-height: 40px; }
.profile-form :deep(.el-input__inner) { font-size: 12px; }
.profile-form :deep(.el-form-item__error) { font-size: 11px; position: static; }
.profile-form :deep(.el-form-item) { margin-bottom: 21px; }
.field-note { font-size: 9px; line-height: 1.7; color: #a2ab94; margin-top: 7px; }
.avatar-editor { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; }
.avatar-editor .el-avatar { color: #729260; background: #f0f5e7; flex-shrink: 0; }
.avatar-editor h3 { font-size: 13px; color: #68805c; font-weight: 500; }
.avatar-editor p { margin-top: 7px; font-size: 10px; line-height: 1.8; color: #9aa78a; }
.form-actions { display: flex; justify-content: flex-end; align-items: center; border-top: 1px solid #edf1e6; padding-top: 22px; gap: 10px; }
.form-actions > span { font-size: 10px; color: #a0aa90; margin-right: auto; }
.form-actions .el-button { font-size: 12px; height: 36px; margin: 0; }
.saved-label { display: flex; align-items: center; gap: 6px; color: #799763; font-size: 11px; white-space: nowrap; }
.settings-aside { display: grid; gap: 20px; }
.aside-icon { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 12px; background: #eff4e7; color: #8ea677; margin-bottom: 18px; }
.aside-icon svg { width: 20px; height: 20px; }
.completeness-panel h2 { font-size: 15px; color: #5a7650; font-weight: 500; margin-bottom: 10px; }
.completeness-panel p { color: #94a284; font-size: 11px; line-height: 1.9; }
.completeness-label { display: flex; justify-content: space-between; margin: 25px 0 12px; color: #95a086; font-size: 11px; }
.completeness-label strong { color: #80a068; font-size: 12px; font-weight: 500; }
.completeness-note { display: block; margin-top: 10px; color: #afb79e; font-size: 9px; line-height: 1.7; }
.account-note { display: flex; align-items: flex-start; gap: 12px; padding: 7px 9px; }
.account-note > .el-icon { color: #a4b291; font-size: 18px; }
.account-note strong { color: #8b9b7b; font-size: 11px; font-weight: 500; }
.account-note p { margin-top: 8px; color: #a4af96; font-size: 10px; line-height: 1.9; }
@media (max-width: 900px) { .overview-layout, .settings-layout { grid-template-columns: 1fr; } .personal-stats { gap: 12px; } .personal-stat { padding: 17px; } .quick-panel { padding-bottom: 22px; } .settings-aside { grid-template-columns: 1fr 1fr; align-items: center; } }
@media (max-width: 600px) { .profile-container { padding: 12px 18px 0; } .page-heading { align-items: flex-start; gap: 12px; } .page-heading h1 { font-size: 25px; } .page-heading p { max-width: 235px; font-size: 11px; } .page-heading > .el-button { font-size: 10px; padding: 9px; margin-top: 27px; } .identity-card { padding: 22px 20px; flex-wrap: wrap; gap: 16px; } .identity { gap: 14px; } .identity .el-avatar { width: 58px; height: 58px; font-size: 22px; } .identity h2 { font-size: 21px; gap: 8px; } .identity p { gap: 5px; flex-direction: column; } .identity .member-badge { font-size: 8px; } .edit-profile-button { margin-left: 72px; font-size: 10px; padding: 8px 11px; } .personal-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); } .personal-stat { padding: 16px; } .personal-stat > strong { font-size: 28px; } .personal-stat p { font-size: 9px; } .profile-tabs { gap: 25px; margin-top: 18px; } .profile-tabs button { font-size: 12px; } .panel, .settings-panel { padding: 22px 20px; } .recent-row { gap: 10px; } .recent-row img { width: 41px; height: 41px; } .recent-copy strong { font-size: 12px; } .recent-copy span { font-size: 8px; } .recent-arrow { display: none; } .application-status { font-size: 9px; padding: 4px 7px; } .companionship-banner { padding: 22px 20px; gap: 15px; flex-wrap: wrap; } .companionship-banner h2 { font-size: 15px; } .companionship-banner p { font-size: 10px; } .companionship-banner a { margin-left: 55px; font-size: 11px; } .form-grid { grid-template-columns: 1fr; } .form-actions { flex-wrap: wrap; } .form-actions > span { width: 100%; margin-bottom: 4px; } .settings-aside { grid-template-columns: 1fr; } }
</style>
