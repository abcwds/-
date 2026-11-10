<template>
  <div class="dashboard" :aria-busy="loading || applicationsLoading">
    <section class="dashboard-heading">
      <div>
        <div class="eyebrow">WORKSPACE / 工作概览</div>
        <h1>运营概览<span class="heading-dot"></span></h1>
        <p>欢迎回到工作台，今天也一起守护这些小小的期待。</p>
      </div>
      <div class="heading-actions">
        <div class="update-copy"><span class="today"><el-icon><Calendar /></el-icon>{{ today }}</span><small>{{ lastUpdated ? `最近更新 ${lastUpdated}` : '查看平台当前运营状态' }}</small></div>
        <el-button :loading="loading" :icon="Refresh" @click="refresh">刷新概览</el-button>
      </div>
    </section>

    <el-alert v-if="loadError" class="load-alert" title="部分数据暂时无法获取" type="warning" show-icon :closable="false">
      <template #default>暂不可用的数据已标记为“—”，请稍后刷新重试。</template>
    </el-alert>

    <section class="focus-banner" aria-label="领养审核待办">
      <div class="focus-copy">
        <span class="focus-kicker"><span></span>把每一份期待，认真放在心上</span>
        <h2>每一份用心，都让它们离家更近。</h2>
        <p>{{ loading ? '正在同步最新领养申请…' : careNote }}</p>
        <router-link :to="stats.pendingCount > 0 ? '/adoptions?status=0' : '/adoptions'" class="focus-action">{{ stats.pendingCount > 0 ? '处理待审核申请' : '查看领养记录' }}<el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <PawMark class="focus-watermark" aria-hidden="true" />
      <div class="focus-side">
        <span class="focus-side-label">领养与照护 · 每一步都重要</span>
        <router-link to="/pets"><span class="step-number">01</span><span><strong>完善宠物档案</strong><small>及时更新伙伴们的近况</small></span><el-icon><ArrowRight /></el-icon></router-link>
        <router-link to="/health"><span class="step-number">02</span><span><strong>记录健康照护</strong><small>让每一次关心都有迹可循</small></span><el-icon><ArrowRight /></el-icon></router-link>
      </div>
    </section>

    <div class="stats-grid">
      <router-link v-for="item in statCards" :key="item.key" :to="item.to" class="stat-card" :class="item.tone">
        <div class="stat-top">
          <span class="stat-label">{{ item.label }}</span>
          <span class="stat-icon"><el-icon><component :is="item.icon" /></el-icon></span>
        </div>
        <div class="stat-number"><strong>{{ loading ? '···' : displayNumber(stats[item.key]) }}</strong><span>{{ item.unit }}</span></div>
        <div class="stat-footer"><span>{{ item.description }}</span><el-icon><TopRight /></el-icon></div>
      </router-link>
    </div>

    <div class="overview-grid">
      <section class="overview-card">
        <div class="section-heading">
          <div><span class="panel-eyebrow">ADOPTION OVERVIEW</span><h2>宠物领养概况</h2><p>每一份档案，都是一段新生活的开始</p></div>
          <router-link to="/pets" class="subtle-link">查看档案<el-icon><ArrowRight /></el-icon></router-link>
        </div>
        <div v-if="loading" class="chart-placeholder" role="status"><el-skeleton :rows="3" animated /></div>
        <div v-else-if="!distributionReady" class="chart-placeholder unavailable"><el-icon><Warning /></el-icon><span>领养概况暂不可用</span></div>
        <div v-else class="distribution">
          <div class="donut-wrap" role="img" :aria-label="distributionDescription">
            <svg class="donut" viewBox="0 0 160 160" aria-hidden="true">
              <circle class="donut-track" cx="80" cy="80" r="65" />
              <circle v-for="segment in ringSegments" :key="segment.label" cx="80" cy="80" r="65" fill="none" :stroke="segment.color" stroke-width="14" :stroke-dasharray="segment.dash" :stroke-dashoffset="segment.offset" transform="rotate(-90 80 80)" />
            </svg>
            <div class="donut-label"><strong>{{ displayNumber(distributionTotal) }}</strong><span>宠物档案总数</span></div>
          </div>
          <div class="distribution-legend">
            <div v-for="item in petStatuses" :key="item.status" class="distribution-row">
              <div class="distribution-label"><span class="legend-dot" :style="{ background: item.color }"></span><span>{{ item.label }}</span><strong>{{ displayNumber(petCounts[item.status]) }}<small> 只</small></strong><span class="distribution-percent">{{ percentage(item.status).toFixed(0) }}%</span></div>
              <div class="progress-track"><span :style="{ width: percentage(item.status) + '%', background: item.color }"></span></div>
            </div>
            <p class="chart-note">{{ distributionTotal ? '根据当前宠物档案状态统计，含待领养、已领养及下架档案。' : '还没有宠物档案，添加后即可查看分布' }}</p>
          </div>
        </div>
        <div class="care-note"><el-icon><Sunny /></el-icon><span>{{ distributionReady && distributionTotal ? `已有 ${petCounts[1]} 只伙伴找到新家，感谢每一份善意。` : '从一份完整的档案，开始一段安心的领养。' }}</span><router-link to="/adoptions">前往审核<el-icon><ArrowRight /></el-icon></router-link></div>
      </section>

      <section class="quick-card">
        <div class="section-heading"><div><span class="panel-eyebrow">QUICK ACCESS</span><h2>快捷工作区</h2><p>常用管理入口，一步直达</p></div><span class="shortcut-label"><el-icon><TopRight /></el-icon></span></div>
        <div class="quick-list">
          <router-link v-for="item in quickLinks" :key="item.to" :to="item.to" class="quick-link">
            <span class="quick-icon" :class="item.tone"><el-icon><component :is="item.icon" /></el-icon></span>
            <span class="quick-copy"><strong>{{ item.label }}</strong><small>{{ item.description }}</small></span>
            <el-icon class="quick-arrow"><ArrowRight /></el-icon>
          </router-link>
        </div>
      </section>
    </div>

    <section class="recent-card">
      <div class="section-heading recent-heading">
        <div><span class="panel-eyebrow">APPLICATION ACTIVITY</span><div class="recent-title"><h2>最近领养申请</h2><span v-if="stats.pendingCount !== null" class="pending-pill"><i></i>{{ stats.pendingCount }} 条待审核</span></div></div>
        <router-link to="/adoptions" class="subtle-link">查看全部<el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <div class="application-toolbar">
        <div class="application-filters" role="group" aria-label="按审核状态筛选申请">
          <button v-for="option in applicationFilters" :key="option.value" type="button" :class="{ active: applicationFilter === option.value }" :aria-pressed="applicationFilter === option.value" @click="changeApplicationFilter(option.value)">{{ option.label }}</button>
        </div>
        <span class="table-scope">按申请时间由近到远排列</span>
      </div>
      <el-table v-loading="applicationsLoading" :data="recentApplications" class="recent-table">
        <el-table-column prop="petName" label="领养宠物" min-width="165">
          <template #default="{ row }"><div class="pet-cell"><span class="pet-avatar"><el-icon><PawMark /></el-icon></span><div><strong>{{ row.petName || '未命名宠物' }}</strong><small>申请编号 #{{ row.id }}</small></div></div></template>
        </el-table-column>
        <el-table-column prop="realName" label="申请人" min-width="100" />
        <el-table-column prop="phone" label="联系电话" min-width="140" />
        <el-table-column prop="createdAt" label="申请时间" min-width="170"><template #default="{ row }"><span class="date-text">{{ formatDate(row.createdAt) }}</span></template></el-table-column>
        <el-table-column prop="status" label="审核状态" min-width="100"><template #default="{ row }"><span class="status-badge" :class="statusClass(row.status)"><i></i>{{ statusText(row.status) }}</span></template></el-table-column>
        <el-table-column label="操作" width="90" align="right"><template #default="{ row }"><router-link :to="{ path: '/adoptions', query: { keyword: row.phone || row.realName || '', applicationId: row.id } }" class="table-action">查看<el-icon><ArrowRight /></el-icon></router-link></template></el-table-column>
        <template #empty><div class="table-empty"><el-icon><DocumentChecked /></el-icon><p>{{ applicationsError ? '申请数据暂时无法加载' : applicationsLoading ? '正在加载申请…' : applicationFilter === 'all' ? '还没有领养申请，新的相遇正在路上' : '当前状态下暂无领养申请' }}</p><el-button v-if="applicationsError" link type="primary" @click="loadRecentApplications">重新加载</el-button></div></template>
      </el-table>
      <div class="table-footer"><span>{{ applicationsError ? '申请记录暂不可用' : applicationsLoading ? '正在同步申请记录…' : `共 ${displayNumber(applicationsTotal)} 条${applicationFilter === 'all' ? '申请' : statusText(Number(applicationFilter)) + '申请'}，展示最近 ${recentApplications.length} 条` }}</span><router-link :to="applicationFilter === 'all' ? '/adoptions' : `/adoptions?status=${applicationFilter}`">进入申请管理<el-icon><ArrowRight /></el-icon></router-link></div>
    </section>
    <div class="dashboard-data-note"><el-icon><InfoFilled /></el-icon><p>统计口径：用户、宠物与健康记录为平台当前记录总数；待审核申请为当前尚未处理的申请。数据来自平台数据库，包含演示资料，不代表线下个体的真实诊疗记录。</p></div>
    <p class="dashboard-footnote">用认真对待每一份信任，用陪伴开启每一个新家。</p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Bell, Calendar, DocumentChecked, FirstAidKit, InfoFilled, Notebook, Refresh, Sunny, TopRight, User, Warning } from '@element-plus/icons-vue'
import PawMark from '../components/PawMark.vue'
import request from '../utils/request'

const stats = ref({ userCount: null, petCount: null, pendingCount: null, healthCount: null })
const petCounts = ref({ 0: null, 1: null, 2: null })
const recentApplications = ref([])
const loading = ref(false)
const statisticsError = ref(false)
const applicationsError = ref(false)
const loadError = computed(() => statisticsError.value || applicationsError.value)
const applicationsLoading = ref(false)
const applicationsTotal = ref(null)
const applicationFilter = ref('all')
let applicationRequestId = 0
const applicationFilters = [
  { value: 'all', label: '全部申请' },
  { value: '0', label: '待审核' },
  { value: '1', label: '已通过' },
  { value: '2', label: '已拒绝' }
]
const lastUpdated = ref('')
const today = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())

const statCards = [
  { key: 'petCount', label: '宠物档案', unit: '只', description: '用心记录每一位伙伴', icon: PawMark, tone: 'green', to: '/pets' },
  { key: 'userCount', label: '平台用户', unit: '位', description: '汇聚每一份温暖与善意', icon: User, tone: 'blue', to: '/users' },
  { key: 'pendingCount', label: '待审核申请', unit: '份', description: '新的相遇，等待你的确认', icon: DocumentChecked, tone: 'amber', to: '/adoptions?status=0' },
  { key: 'healthCount', label: '健康记录', unit: '条', description: '守护它们的健康与成长', icon: FirstAidKit, tone: 'mauve', to: '/health' }
]
const quickLinks = [
  { to: '/pets', label: '宠物档案', description: '更新伙伴们的近况', icon: PawMark, tone: 'green' },
  { to: '/adoptions', label: '领养审核', description: '为它们找到合适的新家', icon: DocumentChecked, tone: 'amber' },
  { to: '/health', label: '健康管理', description: '记录每一次细心照护', icon: FirstAidKit, tone: 'blue' },
  { to: '/vaccine', label: '疫苗记录', description: '查看接种与免疫档案', icon: Notebook, tone: 'mauve' },
  { to: '/users', label: '用户管理', description: '维护用户资料与状态', icon: User, tone: 'blue' },
  { to: '/announcements', label: '公告资讯', description: '分享平台的最新消息', icon: Bell, tone: 'mauve' }
]
const petStatuses = [
  { status: 0, label: '等待领养', color: '#77a18a' },
  { status: 1, label: '已找到新家', color: '#28745b' },
  { status: 2, label: '暂未开放领养', color: '#d7c7a5' }
]
const distributionReady = computed(() => petStatuses.every(item => petCounts.value[item.status] !== null))
const distributionTotal = computed(() => petStatuses.reduce((sum, item) => sum + (petCounts.value[item.status] || 0), 0))
const distributionDescription = computed(() => petStatuses.map(item => `${item.label} ${petCounts.value[item.status]} 只`).join('，'))
const percentage = status => distributionTotal.value ? petCounts.value[status] / distributionTotal.value * 100 : 0
const ringSegments = computed(() => {
  const circumference = 2 * Math.PI * 65
  let offset = 0
  return petStatuses.filter(item => petCounts.value[item.status] > 0).map(item => {
    const length = percentage(item.status) / 100 * circumference
    const segment = { ...item, dash: `${length} ${circumference - length}`, offset: -offset }
    offset += length
    return segment
  })
})
const careNote = computed(() => {
  if (stats.value.pendingCount === null) return '认真了解每份申请，为领养多一份保障。'
  return stats.value.pendingCount > 0 ? `还有 ${stats.value.pendingCount} 份申请等待审核，新的家正在等待。` : '当前申请均已处理，感谢你的每一份用心。'
})

const getPage = async (path, params = {}) => {
  const res = await request.get(path, { params: { page: 1, size: 1, ...params } })
  if (res.code !== 200 || !res.data || res.data.total == null || !Number.isFinite(Number(res.data.total))) throw new Error(res.message || '数据加载失败')
  return res.data
}

const loadRecentApplications = async () => {
  const requestId = ++applicationRequestId
  applicationsLoading.value = true
  applicationsError.value = false
  try {
    const data = await getPage('/admin/adoptions', { size: 5, ...(applicationFilter.value !== 'all' ? { status: Number(applicationFilter.value) } : {}) })
    if (requestId !== applicationRequestId) return
    recentApplications.value = data.records || []
    applicationsTotal.value = Number(data.total)
  } catch {
    if (requestId !== applicationRequestId) return
    recentApplications.value = []
    applicationsTotal.value = null
    applicationsError.value = true
  } finally {
    if (requestId === applicationRequestId) applicationsLoading.value = false
  }
}

const changeApplicationFilter = value => {
  if (applicationFilter.value === value) return
  applicationFilter.value = value
  loadRecentApplications()
}

const refresh = async () => {
  if (loading.value) return
  loading.value = true
  statisticsError.value = false
  const tasks = [
    ...[
      ['userCount', '/admin/users', {}],
      ['petCount', '/admin/pets', {}],
      ['pendingCount', '/admin/adoptions', { status: 0 }],
      ['healthCount', '/admin/health', {}]
    ].map(async ([key, path, params]) => {
      try { stats.value[key] = Number((await getPage(path, params)).total) }
      catch { stats.value[key] = null; statisticsError.value = true }
    }),
    ...petStatuses.map(async item => {
      try { petCounts.value[item.status] = Number((await getPage('/admin/pets', { status: item.status })).total) }
      catch { petCounts.value[item.status] = null; statisticsError.value = true }
    }),
    loadRecentApplications()
  ]
  await Promise.all(tasks)
  lastUpdated.value = !loadError.value ? new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(new Date()) : ''
  loading.value = false
}

const displayNumber = value => value === null ? '—' : Number(value).toLocaleString('zh-CN')
const formatDate = value => value ? String(value).replace('T', ' ').slice(0, 16) : '—'
const statusText = status => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' })[status] || '未知状态'
const statusClass = status => ({ 0: 'pending', 1: 'approved', 2: 'rejected' })[status] || 'unknown'
onMounted(refresh)
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 24px; min-width: 0; color: #263b33; }
.dashboard-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; padding: 3px 0 2px; }
.eyebrow { color: #839087; font-size: 10px; font-weight: 600; letter-spacing: 1.8px; margin-bottom: 10px; }
.dashboard-heading h1 { display: flex; align-items: center; gap: 10px; margin: 0; font-size: 28px; line-height: 1.45; font-weight: 650; letter-spacing: -.6px; }
.heading-dot { width: 7px; height: 7px; background: #8ca585; border-radius: 50%; }
.dashboard-heading p { margin: 8px 0 0; color: #7b887f; font-size: 13px; line-height: 1.7; }
.heading-actions { display: flex; align-items: center; gap: 14px; flex-shrink: 0; }
.update-copy { display: flex; flex-direction: column; align-items: flex-end; gap: 7px; }
.update-copy small { color: #96a094; font-size: 11px; }
.today { display: flex; align-items: center; gap: 7px; color: #829087; font-size: 12px; white-space: nowrap; }
.heading-actions .el-button { height: 38px; border-radius: 9px; font-size: 12px; color: #53675b; }
.focus-banner { position: relative; display: flex; justify-content: space-between; gap: 28px; overflow: hidden; padding: 28px 32px; border-radius: 18px; background: #234d3d; color: #fff; }
.focus-copy { position: relative; z-index: 1; flex: 1; min-width: 0; }
.focus-kicker { display: flex; align-items: center; gap: 7px; font-size: 11px; color: #b4ccb4; letter-spacing: 1px; }
.focus-kicker > span { width: 5px; height: 5px; border-radius: 50%; background: #bacc91; }
.focus-copy h2 { margin: 12px 0 8px; font-size: 23px; line-height: 1.5; font-weight: 550; letter-spacing: .1px; }
.focus-copy p { margin: 0; color: #b8ccbd; font-size: 12px; line-height: 1.8; }
.focus-action { display: inline-flex; align-items: center; gap: 22px; padding: 10px 14px; margin-top: 19px; border-radius: 8px; background: #ecf1df; color: #36543c; text-decoration: none; font-size: 12px; font-weight: 550; transition: background .2s; }
.focus-action:hover { background: #fff; }
.focus-watermark { position: absolute; width: 125px; height: 125px; right: 330px; top: 51px; color: #c5dec8; opacity: .06; transform: rotate(-18deg); pointer-events: none; }
.focus-side { position: relative; flex: 0 0 270px; display: flex; flex-direction: column; justify-content: center; padding: 0 0 0 26px; border-left: 1px solid #ffffff18; }
.focus-side-label { color: #a7c0aa; font-size: 10px; letter-spacing: .7px; margin-bottom: 6px; }
.focus-side a { display: flex; align-items: center; gap: 11px; padding: 12px 0; text-decoration: none; color: #e3ebe1; }
.focus-side a + a { border-top: 1px solid #ffffff10; }
.step-number { display: grid; place-items: center; width: 30px; height: 30px; flex-shrink: 0; border-radius: 9px; background: #ffffff0c; color: #adc3a5; font-family: 'Segoe UI', sans-serif; font-size: 12px; }
.focus-side a strong, .focus-side a small { display: block; }
.focus-side a strong { font-size: 12px; font-weight: 500; }
.focus-side a small { margin-top: 5px; font-size: 10px; color: #a3bda7; }
.focus-side a > .el-icon { margin-left: auto; color: #93b298; font-size: 11px; }
.focus-side a:hover strong { color: #fff; }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; }
.stat-card { display: block; position: relative; min-width: 0; padding: 20px 22px 16px; border: 1px solid #e6eae5; border-radius: 15px; color: inherit; background: #fff; text-decoration: none; box-shadow: 0 3px 8px #263b3303; transition: border-color .2s, transform .2s, box-shadow .2s; }
.stat-card:hover { border-color: #b5cdbd; transform: translateY(-2px); box-shadow: 0 8px 22px #263b3308; }
.stat-top { display: flex; align-items: center; justify-content: space-between; }
.stat-label { font-size: 13px; font-weight: 500; color: #687a70; }
.stat-icon, .quick-icon { display: grid; place-items: center; flex-shrink: 0; width: 36px; height: 36px; border-radius: 10px; font-size: 18px; }
.green .stat-icon, .quick-icon.green { background: #edf4ee; color: #508267; }
.blue .stat-icon, .quick-icon.blue { background: #edf2f6; color: #6588a0; }
.amber .stat-icon, .quick-icon.amber { background: #fcf3e5; color: #bd9354; }
.mauve .stat-icon, .quick-icon.mauve { background: #f2eef5; color: #9583a5; }
.stat-number { display: flex; gap: 8px; align-items: baseline; margin-top: 6px; }
.stat-number strong { font-family: 'Segoe UI', sans-serif; font-size: 37px; font-weight: 600; letter-spacing: -1.4px; line-height: 1.4; font-variant-numeric: tabular-nums; }
.stat-number > span { color: #8b988f; font-size: 12px; }
.stat-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0f2ef; color: #8b988f; font-size: 11px; gap: 4px; }
.stat-footer .el-icon { color: #a4b1a7; }
.overview-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(340px, 1fr); gap: 22px; }
.overview-card, .quick-card, .recent-card { min-width: 0; border: 1px solid #e6eae5; border-radius: 16px; background: #fff; box-shadow: 0 3px 8px #263b3303; }
.overview-card, .quick-card { padding: 23px 24px 18px; }
.panel-eyebrow { display: block; margin-bottom: 7px; font-size: 9px; letter-spacing: 1.5px; color: #9aa594; font-weight: 500; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.section-heading h2 { margin: 0; font-size: 17px; font-weight: 600; letter-spacing: .2px; }
.section-heading p { margin: 6px 0 0; color: #929d95; font-size: 11px; }
.subtle-link { display: flex; align-items: center; gap: 4px; flex-shrink: 0; color: #819086; text-decoration: none; font-size: 11px; }
.subtle-link:hover { color: #28745b; }
.distribution { display: flex; gap: 24px; align-items: center; justify-content: center; min-height: 216px; padding: 18px 0; }
.donut-wrap { position: relative; flex: 0 0 168px; height: 168px; }
.donut { width: 100%; height: 100%; }
.donut-track { fill: none; stroke: #f0f3ef; stroke-width: 14; }
.donut-label { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; }
.donut-label strong { font-family: 'Segoe UI', sans-serif; font-size: 31px; font-weight: 600; letter-spacing: -.6px; }
.donut-label span { font-size: 10px; color: #8b988f; }
.distribution-legend { flex: 1; min-width: 0; max-width: 300px; }
.distribution-row + .distribution-row { margin-top: 17px; }
.distribution-label { display: flex; align-items: center; gap: 7px; font-size: 12px; color: #6f7f73; }
.legend-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.distribution-label strong { margin-left: auto; color: #405448; font-size: 13px; font-weight: 600; white-space: nowrap; }
.distribution-label small { font-size: 10px; color: #89968d; font-weight: 400; }
.distribution-percent { flex: 0 0 30px; text-align: right; color: #9da69b; font-size: 10px; font-variant-numeric: tabular-nums; }
.progress-track { height: 4px; background: #f2f4f0; border-radius: 5px; margin-top: 7px; overflow: hidden; }
.progress-track span { display: block; height: 100%; border-radius: inherit; }
.chart-note { margin: 15px 0 0; color: #929c91; font-size: 10px; line-height: 1.7; }
.care-note { display: flex; align-items: center; gap: 8px; padding: 12px 13px; background: #f4f7f1; border-radius: 8px; color: #70816b; font-size: 10px; line-height: 1.6; }
.care-note > .el-icon { color: #94a376; font-size: 16px; flex-shrink: 0; }
.care-note > span { flex: 1; }
.care-note a { display: flex; align-items: center; gap: 2px; color: #52734b; text-decoration: none; white-space: nowrap; }
.shortcut-label { color: #a6afa7; font-size: 18px; }
.quick-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-top: 23px; }
.quick-link { display: flex; min-width: 0; align-items: center; gap: 10px; padding: 15px 10px; text-decoration: none; border: 1px solid #eff1eb; border-radius: 10px; background: #fafbf8; transition: background .2s, border-color .2s; }
.quick-link:hover { background: #f3f7f0; border-color: #d6e1d2; }
.quick-icon { width: 32px; height: 32px; border-radius: 9px; font-size: 17px; }
.quick-copy strong, .quick-copy small { display: block; }
.quick-copy { min-width: 0; }
.quick-copy strong { color: #4b6052; font-size: 13px; font-weight: 500; }
.quick-copy small { margin-top: 5px; color: #929c94; font-size: 10px; line-height: 1.6; }
.quick-arrow { margin-left: auto; flex-shrink: 0; color: #abb6ae; font-size: 10px; transition: transform .2s; }
.quick-link:hover .quick-arrow { color: #28745b; transform: translateX(3px); }
.recent-card { overflow: hidden; }
.recent-heading { padding: 22px 24px 20px; }
.application-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 0 24px 18px; }
.application-filters { display: inline-flex; flex-wrap: wrap; gap: 4px; padding: 4px; border-radius: 9px; background: #f3f5ef; }
.application-filters button { padding: 7px 12px; color: #7c897d; background: transparent; border: 0; border-radius: 6px; font-size: 12px; cursor: pointer; transition: background .2s, color .2s; }
.application-filters button:hover { color: #28745b; }
.application-filters button.active { background: #fff; color: #346346; box-shadow: 0 1px 5px #294d3610; font-weight: 550; }
.application-filters button:focus-visible { outline: 2px solid #78a787; outline-offset: 1px; }
.table-scope { color: #96a090; font-size: 11px; }
.recent-title { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.pending-pill { display: flex; align-items: center; gap: 5px; background: #fcf5e9; color: #b18e4e; border: 1px solid #f3e8d3; border-radius: 5px; padding: 4px 7px; font-size: 10px; }
.pending-pill i, .status-badge i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
.recent-table { width: 100%; --el-table-header-bg-color: #f8f9f6; --el-table-header-text-color: #8a978d; --el-table-border-color: #f0f2ef; --el-table-text-color: #5f7266; --el-table-row-hover-bg-color: #fafbf8; }
.recent-table :deep(.el-table__inner-wrapper::before) { display: none; }
.recent-table :deep(th.el-table__cell) { padding: 12px 0; font-size: 12px; font-weight: 500; }
.recent-table :deep(td.el-table__cell) { padding: 15px 0; font-size: 13px; }
.recent-table :deep(.el-table__cell:first-child .cell) { padding-left: 24px; }
.recent-table :deep(.el-table__cell:last-child .cell) { padding-right: 24px; }
.pet-cell { display: flex; align-items: center; gap: 10px; }
.pet-avatar { display: grid; place-items: center; width: 38px; height: 38px; border: 1px solid #e6ede3; background: #f4f7ef; border-radius: 11px; color: #8b9e7e; font-size: 18px; flex-shrink: 0; }
.pet-cell strong { color: #42584b; font-size: 13px; font-weight: 500; }
.pet-cell small { display: block; color: #9aa49c; font-size: 10px; line-height: 1.8; }
.date-text { font-size: 12px; font-variant-numeric: tabular-nums; color: #859189; }
.status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 9px; border-radius: 5px; background: #f1f3f1; font-size: 11px; line-height: 1.6; white-space: nowrap; }
.status-badge.pending { color: #b18a46; background: #fbf5e9; }
.status-badge.approved { color: #55876a; background: #edf5ee; }
.status-badge.rejected { color: #b27975; background: #f9efec; }
.table-action { display: inline-flex; align-items: center; gap: 3px; color: #57826b; font-size: 11px; text-decoration: none; white-space: nowrap; }
.table-action:hover { color: #255c43; }
.table-footer { padding: 17px 24px; display: flex; align-items: center; justify-content: space-between; border-top: 1px solid #f0f2ef; color: #929c90; font-size: 11px; }
.table-footer a { display: inline-flex; align-items: center; gap: 5px; color: #60816a; text-decoration: none; white-space: nowrap; }
.dashboard-data-note { display: flex; align-items: flex-start; gap: 8px; margin-top: -6px; padding: 0 4px; color: #899483; font-size: 11px; line-height: 1.8; }
.dashboard-data-note > .el-icon { margin-top: 4px; flex-shrink: 0; color: #9ca990; }
.dashboard-data-note p { margin: 0; }
.dashboard-footnote { margin: -3px 0 0; text-align: center; font-size: 10px; letter-spacing: .8px; color: #a2aca4; }
.chart-placeholder { display: flex; align-items: center; justify-content: center; min-height: 208px; padding: 35px 20px; }
.unavailable { gap: 8px; color: #a1aaa3; font-size: 12px; }
.table-empty { padding: 35px 12px; line-height: 1.8; color: #8c9a90; }
.table-empty > .el-icon { font-size: 28px; margin-bottom: 8px; }
.table-empty p { font-size: 12px; }
.dashboard a:focus-visible { outline: 3px solid #a5c8b4; outline-offset: 4px; border-radius: 5px; }
@media (max-width: 1200px) { .today { display: none; } .dashboard-heading h1 { font-size: 23px; } .distribution { gap: 18px; } .overview-card, .quick-card { padding-left: 20px; padding-right: 20px; } .stat-card { padding-left: 18px; padding-right: 18px; } }
@media (max-width: 1100px) { .focus-banner { padding: 24px; gap: 18px; } .focus-side { flex-basis: 230px; padding-left: 20px; } .focus-copy h2 { font-size: 21px; } .overview-grid { grid-template-columns: minmax(0, 1fr); } .quick-list { grid-template-columns: repeat(3, minmax(0, 1fr)); margin-top: 18px; } .distribution { justify-content: flex-start; gap: 40px; } .distribution-legend { max-width: none; } }
@media (max-width: 1000px) { .stats-grid { gap: 12px; } .stat-footer span { font-size: 10px; } .focus-side { display: none; } .focus-watermark { right: 28px; width: 135px; height: 135px; top: 40px; } }
@media (max-width: 650px) { .dashboard { gap: 18px; } .dashboard-heading { align-items: flex-start; flex-direction: column; gap: 12px; } .dashboard-heading h1 { font-size: 21px; } .heading-actions { width: 100%; justify-content: space-between; } .today { display: flex; } .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .stat-card { padding: 15px; } .stat-number strong { font-size: 30px; } .stat-icon { width: 30px; height: 30px; font-size: 16px; } .stat-footer { min-height: 36px; align-items: flex-start; line-height: 1.6; } .stat-footer .el-icon { display: none; } .stat-label { font-size: 12px; } .distribution { padding-left: 0; padding-right: 0; gap: 16px; } .donut-wrap { flex-basis: 140px; height: 140px; } .care-note { flex-wrap: wrap; } .care-note a { margin-left: 24px; } .recent-heading { padding: 20px; } .table-footer { padding: 15px 20px; gap: 12px; } .dashboard-footnote { font-size: 9px; } }
@media (max-width: 650px) { .update-copy { align-items: flex-start; } .focus-banner { padding: 22px; border-radius: 14px; } .focus-copy h2 { max-width: 260px; font-size: 21px; } .focus-copy p { max-width: 250px; } .focus-watermark { right: 8px; } .quick-list { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; } .quick-link { padding: 12px 9px; } .quick-arrow { display: none; } .distribution-percent { flex-basis: 25px; } .application-toolbar { padding: 0 18px 15px; } .table-scope { display: none; } .application-filters { gap: 0; } .application-filters button { padding: 7px 10px; font-size: 11px; } .table-footer { flex-wrap: wrap; } .dashboard-data-note { font-size: 10px; } }
@media (max-width: 400px) { .dashboard-heading h1 { font-size: 23px; } .quick-list { grid-template-columns: 1fr; } .quick-arrow { display: block; } .distribution { gap: 12px; } .donut-wrap { flex-basis: 110px; height: 110px; } .donut-label strong { font-size: 25px; } .donut-label span { font-size: 8px; } .distribution-label { font-size: 10px; gap: 4px; } .distribution-percent { display: none; } .overview-card, .quick-card { padding-left: 16px; padding-right: 16px; } .subtle-link { font-size: 10px; } .care-note a { margin-left: 0; } }
@media (prefers-reduced-motion: reduce) { .stat-card, .quick-arrow, .focus-action, .quick-link, .application-filters button { transition: none; } }
</style>
