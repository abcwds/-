
<template>
  <div class="adoption-manage management-page">
    <section class="page-intro">
      <div><div class="page-kicker">ADOPTION APPLICATIONS</div><h1>领养审核</h1><p>认真了解每一份申请，为它们找到安心的归属。</p></div>
      <el-button :loading="loading" @click="loadApplications"><el-icon><Refresh /></el-icon>刷新列表</el-button>
    </section>
    <div class="review-guide"><div><span>01</span><p><strong>查看申请资料</strong><small>了解申请人和生活环境</small></p></div><el-icon><ArrowRight /></el-icon><div><span>02</span><p><strong>核对领养条件</strong><small>结合经验、意愿与照护能力</small></p></div><el-icon><ArrowRight /></el-icon><div><span>03</span><p><strong>记录审核结果</strong><small>通过或填写明确的拒绝理由</small></p></div></div>
    <section class="filter-panel"><div class="search-bar">
      <el-input v-model="keyword" class="search-field" placeholder="搜索申请人或联系电话" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="status" placeholder="全部状态" clearable class="filter-select" @change="search">
        <el-option :label="statusText(0)" :value="0" />
        <el-option :label="statusText(1)" :value="1" />
        <el-option :label="statusText(2)" :value="2" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div></section>

    <el-alert v-if="loadError" title="领养申请加载失败，请刷新重试" type="warning" :closable="false" show-icon />
    <section class="table-panel">
    <div class="table-heading"><h2>{{ status == null || status === '' ? '全部申请' : statusText(status) + '申请' }} <span class="table-count">{{ loadError ? '—' : total }}</span></h2><span>每一次审核，都关乎一个新家</span></div>
    <el-table v-loading="loading" :data="applications" style="width: 100%">
      <el-table-column prop="id" label="编号" width="72"><template #default="scope"><span class="muted-cell">#{{ scope.row.id }}</span></template></el-table-column>
      <el-table-column prop="petName" label="领养宠物" min-width="170"><template #default="scope"><div class="application-pet"><img :src="resolvePetImage(scope.row.petImage)" :style="getPetImageStyle(scope.row.petImage)" :alt="scope.row.petName || '领养宠物'" @error="handlePetImageError($event, scope.row.petImage)" /><div><strong class="pet-name">{{ scope.row.petName || '宠物 #' + scope.row.petId }}</strong><span>宠物 #{{ scope.row.petId }}</span></div></div></template></el-table-column>
      <el-table-column label="申请人" min-width="145"><template #default="scope"><div class="stacked-cell"><strong>{{ scope.row.realName }}</strong><span>{{ scope.row.phone || '未填写电话' }}</span></div></template></el-table-column>
      <el-table-column prop="address" label="联系地址" min-width="180" show-overflow-tooltip />
      <el-table-column label="居住与工作" min-width="145"><template #default="scope"><div class="stacked-cell"><strong>{{ scope.row.housingType || '未填写住房' }}</strong><span>{{ scope.row.workStatus || '未填写工作' }}</span></div></template></el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="155"><template #default="scope"><span class="muted-cell">{{ formatDate(scope.row.createdAt) }}</span></template></el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" :loading="detailId === scope.row.id" @click="viewDetail(scope.row)">详情</el-button>
          <el-button v-if="scope.row.status === 0" link type="success" :loading="reviewingIds.includes(scope.row.id)" @click="approve(scope.row.id)">通过</el-button>
          <el-button v-if="scope.row.status === 0" link type="danger" @click="reject(scope.row.id)">拒绝</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty :image-size="76" :description="loadError ? '加载失败，请刷新重试' : '暂无符合条件的申请'" /></template>
    </el-table>
    </section>

    <el-pagination
      :current-page="page"
      :page-size="size"
      :total="total"
      :pager-count="5"
      @current-change="handlePageChange"
      layout="total, prev, pager, next"
      class="pagination"
    />

    <el-dialog v-model="showDetailModal" title="申请详情" width="760px">
      <div v-if="selectedApp" class="detail-content">
        <div class="detail-summary"><div class="summary-mark"><el-icon><DocumentChecked /></el-icon></div><div><strong>{{ selectedApp.realName }} 的领养申请</strong><span>申请编号 #{{ selectedApp.id }} · {{ formatDate(selectedApp.createdAt) }}</span></div><el-tag :type="statusType(selectedApp.status)">{{ statusText(selectedApp.status) }}</el-tag></div>
        <DetailSections :sections="applicationSections" />
      </div>
      <template #footer><el-button @click="showDetailModal = false">关闭详情</el-button><template v-if="selectedApp?.status === 0"><el-button type="danger" plain @click="reject(selectedApp.id)">拒绝申请</el-button><el-button type="primary" :loading="reviewingIds.includes(selectedApp.id)" @click="approve(selectedApp.id)">通过申请</el-button></template></template>
    </el-dialog>

    <el-dialog v-model="showRejectModal" title="拒绝申请" width="460px">
      <p class="reject-note">请清晰说明原因，帮助申请人了解审核结果。</p>
      <el-form :model="rejectForm" label-position="top">
        <el-form-item label="拒绝理由">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRejectModal = false">取消</el-button>
        <el-button type="primary" :loading="rejecting" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Search, Refresh, DocumentChecked } from '@element-plus/icons-vue'
import request from '../utils/request'
import DetailSections from '../components/DetailSections.vue'
import { getPetImageStyle, handlePetImageError, resolvePetImage } from '../utils/petImage'

const route = useRoute()
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const readStatus = value => ['0', '1', '2'].includes(String(value)) ? Number(value) : null
const status = ref(readStatus(route.query.status))
const page = ref(1)
const size = ref(10)
const applications = ref([])
const total = ref(0)
const showDetailModal = ref(false)
const showRejectModal = ref(false)
const selectedApp = ref(null)
const rejectId = ref(null)
const loading = ref(false)
const loadError = ref(false)
const reviewingIds = ref([])
const rejecting = ref(false)
const detailId = ref(null)
let loadSequence = 0
let detailSequence = 0
const formatDate = value => value ? String(value).replace('T', ' ').slice(0, 16) : '—'
const applicationSections = computed(() => {
  const value = selectedApp.value || {}
  return [
    { title: '领养对象与申请人', fields: [{ label: '宠物名称', value: value.petName || `宠物 #${value.petId}` }, { label: '申请账号', value: value.username || `用户 #${value.userId}` }, { label: '真实姓名', value: value.realName }, { label: '联系电话', value: value.phone }, { label: '联系地址', value: value.address, wide: true }] },
    { title: '居住环境与领养准备', fields: [{ label: '工作状况', value: value.workStatus }, { label: '住房类型', value: value.housingType }, { label: '是否有其他宠物', value: value.hasOtherPet === 1 ? '是' : value.hasOtherPet === 0 ? '否' : null }, { label: '养宠经验', value: value.experience }, { label: '领养理由', value: value.reason, wide: true }] },
    { title: '审核与时间记录', fields: [{ label: '审核状态', value: statusText(value.status) }, { label: '申请时间', value: formatDate(value.createdAt) }, { label: '领养记录时间', value: value.adoptedAt ? formatDate(value.adoptedAt) : '暂无领养记录' }, { label: '最近更新', value: formatDate(value.updatedAt) }, { label: '拒绝理由', value: value.rejectReason || '无', wide: true }] }
  ]
})

const rejectForm = reactive({ reason: '' })

onMounted(() => {
  loadApplications()
  openRequestedApplication(route.query.applicationId)
})

const loadApplications = async () => {
  const sequence = ++loadSequence
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/admin/adoptions', {
      params: { page: page.value, size: size.value, keyword: keyword.value, status: status.value }
    })
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    if (sequence !== loadSequence) return
    applications.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    if (sequence !== loadSequence) return
    loadError.value = true
    applications.value = []
    total.value = 0
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

const resetSearch = () => { keyword.value = ''; status.value = null; search() }

const search = () => {
  page.value = 1
  loadApplications()
}

watch(() => route.query, query => {
  status.value = readStatus(query.status)
  keyword.value = typeof query.keyword === 'string' ? query.keyword : ''
  search()
  openRequestedApplication(query.applicationId)
})

const handlePageChange = (val) => {
  page.value = val
  loadApplications()
}

const statusText = (status) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return map[status] || '未知'
}

const statusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || ''
}

const viewDetail = async (app) => {
  const sequence = ++detailSequence
  detailId.value = app.id
  try {
    const res = await request.get(`/admin/adoptions/${app.id}`)
    if (res.code !== 200 || !res.data) throw new Error(res.message || '申请不存在')
    const value = { ...app, ...res.data, petName: res.data.petName || app.petName, petImage: res.data.petImage || app.petImage, username: res.data.username || app.username }
    const extra = await Promise.allSettled([
      value.petName ? Promise.resolve(null) : request.get(`/admin/pets/${value.petId}`),
      value.username ? Promise.resolve(null) : request.get(`/admin/users/${value.userId}`)
    ])
    if (sequence !== detailSequence) return
    if (extra[0].status === 'fulfilled' && extra[0].value?.data) { value.petName = extra[0].value.data.name; value.petImage = extra[0].value.data.image }
    if (extra[1].status === 'fulfilled' && extra[1].value?.data) value.username = extra[1].value.data.username
    selectedApp.value = value
    showDetailModal.value = true
  } catch {
    if (sequence === detailSequence) ElMessage.error('申请详情加载失败，请重试')
  } finally { if (sequence === detailSequence) detailId.value = null }
}

const openRequestedApplication = value => {
  const id = Number(value)
  if (Number.isSafeInteger(id) && id > 0) viewDetail({ id })
}

const approve = async (id) => {
  if (reviewingIds.value.includes(id)) return
  reviewingIds.value.push(id)
  try {
    await ElMessageBox.confirm('通过后，宠物将标记为已领养，同一宠物的其他待审核申请会同步处理。请确认已核对申请资料。', '确认通过领养申请', { type: 'warning', confirmButtonText: '确认通过', cancelButtonText: '再核对一下' })
    const res = await request.put(`/admin/adoptions/${id}/review`, { status: 1 })
    if (res.code !== 200) throw new Error(res.message || '审核失败')
    ElMessage.success(res.message || '申请已通过')
    await loadApplications()
    if (showDetailModal.value && selectedApp.value?.id === id) await viewDetail(selectedApp.value)
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.message || '审核失败，请重试')
  } finally {
    reviewingIds.value = reviewingIds.value.filter(value => value !== id)
  }
}

const reject = (id) => {
  rejectId.value = id
  rejectForm.reason = ''
  showRejectModal.value = true
}

const confirmReject = async () => {
  if (rejecting.value) return
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请填写拒绝理由')
    return
  }
  rejecting.value = true
  try {
    const res = await request.put(`/admin/adoptions/${rejectId.value}/review`, { status: 2, rejectReason: rejectForm.reason.trim() })
    if (res.code !== 200) throw new Error(res.message || '审核失败')
    ElMessage.success(res.message || '审核结果已保存')
    showRejectModal.value = false
    await loadApplications()
    if (showDetailModal.value && selectedApp.value?.id === rejectId.value) await viewDetail(selectedApp.value)
  } catch {
    ElMessage.error('审核失败，请重试')
  } finally {
    rejecting.value = false
  }
}
</script>

<style scoped>
.review-guide { display: flex; align-items: center; justify-content: space-between; gap: 15px; background: #eef3e9; border: 1px solid #e0e8d7; border-radius: 14px; padding: 20px 26px; }
.review-guide > div { display: flex; align-items: center; gap: 12px; }
.review-guide > div > span { font-size: 12px; color: #81936e; background: #fff9; border: 1px solid #dce6d3; width: 32px; height: 32px; border-radius: 10px; display: grid; place-items: center; }
.review-guide strong { display: block; color: #4e6848; font-size: 12px; font-weight: 500; }
.review-guide small { display: block; margin-top: 5px; color: #8d9c80; font-size: 10px; }
.review-guide > .el-icon { color: #b0bda6; font-size: 12px; }
.application-pet { display: flex; align-items: center; gap: 10px; }
.application-pet img { width: 39px; height: 39px; object-fit: cover; border-radius: 10px; background: #f1f5ed; }
.application-pet span { display: block; color: #99a28f; font-size: 10px; margin-top: 4px; }
@media (max-width: 1050px) { .review-guide { padding: 18px; } .review-guide small { display: none; } }
@media (max-width: 560px) { .review-guide { gap: 12px; align-items: flex-start; } .review-guide > div { flex-direction: column; align-items: flex-start; gap: 9px; } .review-guide > .el-icon { display: none; } .review-guide strong { font-size: 11px; } }
.pet-name { color: #263b33; font-weight: 600; }
.stacked-cell strong, .stacked-cell span { display: block; }
.stacked-cell strong { font-size: 13px; font-weight: 500; }
.stacked-cell span, .muted-cell { margin-top: 3px; color: #7c8980; font-size: 12px; }
.detail-summary { display: flex; align-items: center; gap: 12px; padding: 18px; margin-bottom: 14px; background: #f3f6f2; border-radius: 12px; }
.summary-mark { display: grid; place-items: center; width: 44px; height: 44px; border-radius: 12px; color: #28745b; background: #e3ede2; font-size: 22px; flex-shrink: 0; }
.detail-summary strong, .detail-summary span { display: block; }
.detail-summary strong { color: #263b33; font-size: 15px; }
.detail-summary span { margin-top: 4px; color: #7c8980; font-size: 12px; }
.detail-summary > .el-tag { margin-left: auto; flex-shrink: 0; }
.detail-row { display: flex; align-items: flex-start; gap: 12px; padding: 13px 4px; color: #44584c; line-height: 1.7; border-bottom: 1px solid #edf0eb; overflow-wrap: anywhere; }
.detail-row .label { flex: 0 0 112px; color: #7c8980; font-size: 13px; }
.reject-note { margin-bottom: 20px; color: #7c8980; font-size: 13px; line-height: 1.8; }
@media (max-width: 500px) { .detail-summary { flex-wrap: wrap; } .detail-row .label { flex-basis: 100px; } }
</style>
