<template>
  <div class="adoption-page">
    <div class="journey-container">
      <router-link to="/profile" class="back-link"><el-icon><ArrowLeft /></el-icon>返回个人中心</router-link>
      <section class="journey-heading">
        <div><span class="page-eyebrow">MY ADOPTION JOURNEY</span><h1>我的领养</h1><p>从一份认真填写的申请，到一段温暖的陪伴。</p></div>
        <router-link to="/pets" class="browse-button"><el-icon><PawMark /></el-icon>寻找新的伙伴<el-icon><ArrowRight /></el-icon></router-link>
      </section>
      <div class="journey-toolbar">
        <div class="journey-tabs" role="group" aria-label="申请状态筛选">
          <button v-for="tab in tabs" :key="tab.label" :class="{ selected: selectedStatus === tab.value }" :aria-pressed="selectedStatus === tab.value" @click="selectStatus(tab.value)">{{ tab.label }}</button>
        </div>
        <el-button text :icon="Refresh" :loading="loading" @click="loadApplications">刷新</el-button>
      </div>
      <div class="results-heading"><span>{{ selectedLabel }} <strong>{{ total === null ? '—' : total }}</strong> 份</span><small>仅展示当前账号的领养申请</small></div>

      <div v-if="loading" class="loading-panel" role="status"><el-skeleton :rows="5" animated /><span class="sr-only">正在加载领养申请</span></div>
      <section v-else-if="loadError" class="journey-empty" role="alert">
        <span class="empty-symbol"><el-icon><Warning /></el-icon></span><h2>申请记录暂时无法加载</h2><p>请稍后重试，你的申请记录会在连接恢复后显示。</p><el-button :icon="Refresh" @click="loadApplications">重新加载</el-button>
      </section>
      <div v-else-if="applications.length" class="application-list">
        <article v-for="app in applications" :key="app.id" class="application-card">
          <div class="application-top">
            <router-link :to="app.petId ? '/pet/' + app.petId : '/pets'" class="pet-summary">
              <img :src="resolvePetImage(app.petImage)" :style="getPetImageStyle(app.petImage)" :alt="app.petName || '领养宠物'" @error="handlePetImageError($event, app.petImage)" />
              <div><span class="application-id">领养申请 #{{ app.id }}</span><h2>{{ app.petName || '宠物伙伴' }}</h2><p>提交于 {{ formatDate(app.createdAt) }}</p></div>
            </router-link>
            <span class="status-pill" :class="statusClass(app.status)"><i></i>{{ statusText(app.status) }}</span>
          </div>
          <div class="application-progress" :class="statusClass(app.status)" aria-label="领养进度">
            <div class="progress-step completed"><span><el-icon><Check /></el-icon></span><strong>申请已提交</strong></div><div class="progress-line"></div>
            <div class="progress-step" :class="{ current: app.status === 0, completed: app.status === 1 || app.status === 2 }"><span><el-icon><DocumentChecked /></el-icon></span><strong>{{ app.status === 0 ? '等待审核' : '审核已完成' }}</strong></div><div class="progress-line" :class="{ complete: app.status !== 0 }"></div>
            <div class="progress-step" :class="{ completed: app.status === 1, declined: app.status === 2 }"><span><el-icon><House v-if="app.status !== 2" /><Close v-else /></el-icon></span><strong>{{ app.status === 2 ? '本次未通过' : app.status === 1 ? '申请已通过' : '期待新的相伴' }}</strong></div>
          </div>
          <div class="status-explanation" :class="statusClass(app.status)">
            <el-icon><InfoFilled /></el-icon>
            <span v-if="app.status === 0">申请正在等待审核，你可以在这里查看最新进度。</span>
            <span v-else-if="app.status === 1">申请已通过，下一步请与救助方确认领养安排。<template v-if="app.adoptedAt">领养记录时间：{{ formatDate(app.adoptedAt) }}</template></span>
            <span v-else-if="app.status === 2">本次申请未通过。{{ app.rejectReason || '可以继续了解其他等待领养的伙伴。' }}</span>
            <span v-else>当前状态暂不可识别，请刷新后重试。</span>
          </div>
          <details class="application-details">
            <summary><span><el-icon><Document /></el-icon>查看申请资料</span><el-icon class="details-arrow"><ArrowDown /></el-icon></summary>
            <dl>
              <div><dt>申请人</dt><dd>{{ app.realName || '未填写' }}</dd></div>
              <div><dt>联系电话</dt><dd>{{ app.phone || '未填写' }}</dd></div>
              <div class="wide"><dt>居住地址</dt><dd>{{ app.address || '未填写' }}</dd></div>
              <div><dt>工作状况</dt><dd>{{ app.workStatus || '未填写' }}</dd></div>
              <div><dt>住房类型</dt><dd>{{ app.housingType || '未填写' }}</dd></div>
              <div><dt>其他宠物</dt><dd>{{ app.hasOtherPet === 1 ? '已有宠物' : app.hasOtherPet === 0 ? '暂无其他宠物' : '未填写' }}</dd></div>
              <div class="wide"><dt>养宠经验</dt><dd>{{ app.experience || '未填写' }}</dd></div>
              <div class="wide"><dt>领养理由</dt><dd>{{ app.reason || '未填写' }}</dd></div>
            </dl>
          </details>
        </article>
      </div>
      <section v-else class="journey-empty">
        <span class="empty-symbol"><PawMark /></span><h2>{{ selectedStatus === null ? '新的相遇，正在等你' : '这个分类下还没有申请' }}</h2><p>{{ selectedStatus === null ? '去认识那些等待一个家的小伙伴，找到心动的它。' : '可以切换到全部申请，查看你的完整领养记录。' }}</p>
        <router-link v-if="selectedStatus === null" to="/pets" class="browse-button">去认识它们<el-icon><ArrowRight /></el-icon></router-link><el-button v-else @click="selectStatus(null)">查看全部申请</el-button>
      </section>
      <el-pagination v-if="!loading && !loadError && total > size" :current-page="page" :page-size="size" :total="total" :pager-count="5" layout="total, prev, pager, next" class="journey-pagination" @current-change="changePage" />
      <p class="journey-note"><el-icon><Lock /></el-icon>认真对待每一份申请，也珍惜每一次相遇。</p>
    </div>
  </div>
</template>
<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, ArrowLeft, ArrowRight, Check, Close, Document, DocumentChecked, House, InfoFilled, Lock, Refresh, Warning } from '@element-plus/icons-vue'
import PawMark from '../components/PawMark.vue'
import request from '../utils/request'
import { getPetImageStyle, handlePetImageError, resolvePetImage } from '../utils/petImage'
const route = useRoute()
const router = useRouter()
const tabs = [{ label: '全部申请', value: null }, { label: '待审核', value: 0 }, { label: '已通过', value: 1 }, { label: '未通过', value: 2 }]
const selectedStatus = computed(() => ['0', '1', '2'].includes(String(route.query.status)) ? Number(route.query.status) : null)
const selectedLabel = computed(() => tabs.find(tab => tab.value === selectedStatus.value)?.label || '全部申请')
const page = computed(() => { const value = Number(route.query.page); return Number.isSafeInteger(value) && value > 0 ? value : 1 })
const size = 6
const applications = ref([])
const total = ref(null)
const loading = ref(false)
const loadError = ref(false)
let requestSequence = 0
async function loadApplications() {
  const sequence = ++requestSequence
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/adoptions/my', { params: { page: page.value, size, ...(selectedStatus.value === null ? {} : { status: selectedStatus.value }) } })
    if (res.code !== 200 || !res.data || !Array.isArray(res.data.records)) throw new Error('加载失败')
    if (sequence !== requestSequence) return
    applications.value = res.data.records
    total.value = Number(res.data.total) || 0
  } catch {
    if (sequence !== requestSequence) return
    applications.value = []
    total.value = null
    loadError.value = true
  } finally { if (sequence === requestSequence) loading.value = false }
}
function selectStatus(status) {
  router.replace({ path: '/adoptions', query: status === null ? {} : { status: String(status) } })
}
function changePage(value) {
  router.replace({ path: '/adoptions', query: { ...(selectedStatus.value === null ? {} : { status: String(selectedStatus.value) }), ...(value > 1 ? { page: String(value) } : {}) } })
}
const statusText = status => ({ 0: '待审核', 1: '申请已通过', 2: '本次未通过' })[status] || '状态待确认'
const statusClass = status => ({ 0: 'pending', 1: 'approved', 2: 'rejected' })[status] || 'unknown'
function formatDate(value) {
  if (!value) return '时间未记录'
  return String(value).replace('T', ' ').slice(0, 16)
}
watch(() => [route.query.status, route.query.page], loadApplications, { immediate: true })
onBeforeUnmount(() => { requestSequence++ })
</script>
<style scoped>
.adoption-page { color: #263b33; }
.journey-container { max-width: 1040px; padding: 20px 28px 0; margin: 0 auto; }
.back-link { display: inline-flex; align-items: center; gap: 7px; color: #82907b; font-size: 12px; text-decoration: none; margin-bottom: 22px; }
.journey-heading { display: flex; justify-content: space-between; align-items: center; gap: 24px; margin-bottom: 30px; }
.page-eyebrow { color: #8d9b81; font-size: 9px; letter-spacing: 2px; }
.journey-heading h1 { font-size: 30px; font-weight: 600; letter-spacing: 1px; margin: 10px 0; }
.journey-heading p { color: #819079; font-size: 13px; line-height: 1.8; }
.browse-button { display: inline-flex; justify-content: center; align-items: center; gap: 9px; color: white; background: #28745b; border-radius: 9px; padding: 13px 17px; font-size: 12px; text-decoration: none; white-space: nowrap; }
.browse-button:hover { background: #234d3d; }
.journey-toolbar { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e3e9df; gap: 10px; }
.journey-tabs { display: flex; gap: 22px; }
.journey-tabs button { padding: 15px 1px; color: #8b9684; border: none; border-bottom: 2px solid transparent; background: transparent; cursor: pointer; font-size: 13px; white-space: nowrap; }
.journey-tabs button.selected { color: #28745b; border-bottom-color: #28745b; font-weight: 600; }
.results-heading { display: flex; justify-content: space-between; align-items: center; gap: 10px; font-size: 12px; color: #87917e; padding: 22px 0 16px; }
.results-heading strong { color: #476441; font-weight: 500; margin: 0 5px; }
.results-heading small { font-size: 10px; color: #a0a991; }
.application-list { display: grid; gap: 20px; }
.application-card { border: 1px solid #e3e9df; background: white; border-radius: 16px; padding: 24px 28px 0; overflow: hidden; }
.application-top { display: flex; align-items: center; justify-content: space-between; gap: 15px; }
.pet-summary { min-width: 0; display: flex; align-items: center; gap: 16px; text-decoration: none; color: #263b33; }
.pet-summary img { flex-shrink: 0; width: 68px; height: 68px; object-fit: cover; border-radius: 14px; background: #f2f5ed; }
.application-id { color: #99a28d; font-size: 10px; }
.pet-summary h2 { font-size: 18px; font-weight: 600; margin: 5px 0; overflow-wrap: anywhere; }
.pet-summary p { font-size: 10px; color: #8a9680; }
.status-pill { display: inline-flex; align-items: center; gap: 6px; flex-shrink: 0; padding: 7px 10px; border: 1px solid; border-radius: 7px; font-size: 11px; white-space: nowrap; }
.status-pill i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
.status-pill.pending { color: #ad8a4d; background: #fcf7eb; border-color: #efe6d0; }
.status-pill.approved { color: #5d8561; background: #f0f6eb; border-color: #e1ead7; }
.status-pill.rejected { color: #b17b69; background: #fcf2ed; border-color: #efdfd5; }
.application-progress { display: flex; align-items: center; margin: 27px 0 20px; padding: 18px 25px; background: #f9faf6; border: 1px solid #eef1e8; border-radius: 10px; }
.progress-step { display: flex; align-items: center; gap: 8px; color: #b2baa6; font-size: 11px; flex-shrink: 0; }
.progress-step > span { display: grid; place-items: center; width: 25px; height: 25px; border-radius: 50%; background: #ecefe6; font-size: 13px; }
.progress-step strong { font-weight: 400; }
.progress-step.completed { color: #68936c; }
.progress-step.completed > span { color: white; background: #88a481; }
.progress-step.current { color: #b0955e; }
.progress-step.current > span { background: #f2e9cf; }
.progress-step.declined { color: #b68572; }
.progress-step.declined > span { background: #f2e1d7; }
.progress-line { height: 1px; flex: 1; background: #e3e9d9; min-width: 16px; margin: 0 16px; }
.progress-line.complete { background: #c0d0b7; }
.status-explanation { display: flex; align-items: flex-start; gap: 8px; padding: 0 3px 22px; font-size: 12px; color: #819075; line-height: 1.8; overflow-wrap: anywhere; }
.status-explanation .el-icon { flex-shrink: 0; margin-top: 4px; color: #9dab8e; }
.status-explanation.rejected { color: #a87964; }
.application-details { border-top: 1px solid #edf0e7; }
.application-details summary { list-style: none; cursor: pointer; display: flex; justify-content: space-between; align-items: center; padding: 16px 1px; color: #7b8a71; font-size: 11px; }
.application-details summary::-webkit-details-marker { display: none; }
.application-details summary > span { display: inline-flex; align-items: center; gap: 7px; }
.details-arrow { transition: transform .2s; }
.application-details[open] .details-arrow { transform: rotate(180deg); }
.application-details dl { display: grid; grid-template-columns: 1fr 1fr; padding: 5px 0 22px; gap: 18px 30px; }
.application-details dl > div { min-width: 0; }
.application-details .wide { grid-column: 1 / -1; }
.application-details dt { font-size: 11px; color: #9ca58f; margin-bottom: 6px; }
.application-details dd { font-size: 12px; color: #5a6c51; line-height: 1.8; overflow-wrap: anywhere; white-space: pre-wrap; }
.journey-empty, .loading-panel { display: flex; flex-direction: column; align-items: center; justify-content: center; border: 1px solid #e3e9df; border-radius: 16px; background: white; padding: 55px 24px; text-align: center; }
.loading-panel { align-items: stretch; padding: 36px; }
.empty-symbol { width: 64px; height: 64px; display: grid; place-items: center; color: #91a37c; background: #f0f4e9; border-radius: 22px; margin-bottom: 18px; font-size: 28px; }
.empty-symbol svg { width: 32px; height: 32px; }
.journey-empty h2 { font-size: 18px; font-weight: 500; margin-bottom: 10px; }
.journey-empty p { font-size: 12px; color: #8d987f; line-height: 1.9; margin-bottom: 24px; }
.journey-pagination { margin-top: 28px; }
.journey-note { display: flex; align-items: center; justify-content: center; gap: 7px; font-size: 10px; color: #a1aa94; margin-top: 28px; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
@media (max-width: 650px) { .journey-container { padding: 12px 18px 0; } .journey-heading { align-items: flex-start; flex-direction: column; gap: 18px; margin-bottom: 20px; } .journey-heading h1 { font-size: 26px; } .journey-tabs { gap: 19px; } .journey-tabs button { font-size: 12px; } .results-heading small { display: none; } .application-card { padding: 19px 18px 0; } .pet-summary { gap: 11px; } .pet-summary img { width: 54px; height: 54px; border-radius: 11px; } .pet-summary h2 { font-size: 16px; } .status-pill { font-size: 10px; padding: 6px 8px; } .application-progress { padding: 16px 11px; } .progress-step { flex-direction: column; gap: 7px; font-size: 10px; } .progress-line { margin: 0 8px; margin-top: -19px; } .application-details dl { gap: 16px; } }
</style>
