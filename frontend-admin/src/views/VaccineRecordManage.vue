<template>
  <div class="vaccine-manage management-page">
    <section class="page-intro">
      <div>
        <div class="page-kicker">VACCINATION RECORDS</div>
        <h1>疫苗记录</h1>
        <p>守护每一程成长，妥善记录接种与复种安排。</p>
      </div>
      <el-button :loading="loading" aria-label="刷新疫苗记录" @click="loadRecords">
        <el-icon><Refresh /></el-icon>刷新列表
      </el-button>
    </section>

    <section class="filter-panel">
      <div class="search-bar">
        <el-input v-model="keyword" class="search-field" placeholder="搜索疫苗名称或医院" clearable @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <RecordPetSelect v-model="petIdFilter" class="record-pet-filter" placeholder="筛选宠物名称 / 品种" @select="search" />
        <el-button @click="search">查询</el-button>
        <el-button v-if="keyword || petIdFilter" text @click="resetFilters">重置</el-button>
        <el-button class="toolbar-action" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          添加记录
        </el-button>
      </div>
    </section>

    <el-alert
      v-if="loadError"
      class="load-alert"
      title="疫苗记录加载失败"
      description="请确认后端服务已启动，修复连接后点击重新加载。"
      type="warning"
      show-icon
      :closable="false"
    >
      <el-button link type="warning" :loading="loading" @click="loadRecords">重新加载</el-button>
    </el-alert>

    <section class="table-panel">
      <div class="table-heading"><h2>接种档案 <span class="table-count">{{ loadError ? '—' : total }}</span></h2><span>{{ keyword ? '当前搜索结果' : '全部记录' }}</span></div>
      <el-table v-loading="loading" :data="records" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="petName" label="宠物名称" min-width="150"><template #default="scope"><div class="record-pet-cell"><strong>{{ scope.row.petName || '名称未填写' }}</strong><span>宠物编号 #{{ scope.row.petId }}</span></div></template></el-table-column>
        <el-table-column prop="vaccineName" label="疫苗名称" min-width="150" />
        <el-table-column prop="vaccineType" label="疫苗类型" min-width="120" />
        <el-table-column prop="dose" label="剂量" width="90" />
        <el-table-column prop="vaccinateDate" label="接种日期" width="120"><template #default="scope"><span class="record-date">{{ formatDate(scope.row.vaccinateDate) || '—' }}</span></template></el-table-column>
        <el-table-column prop="nextDate" label="下次接种" width="145"><template #default="scope"><div class="next-date-cell"><span class="record-date">{{ formatDate(scope.row.nextDate) || '未填写计划' }}</span><el-tag v-if="isPastDate(scope.row.nextDate)" size="small" type="warning" effect="plain">计划日期已过</el-tag></div></template></el-table-column>
        <el-table-column prop="hospital" label="医院" min-width="140" />
        <el-table-column prop="doctor" label="医生" width="100" />
        <el-table-column label="来源" min-width="210">
          <template #default="scope">
            <div class="source-cell">
              <strong>{{ scope.row.sourceName || '待补来源' }}</strong>
              <a v-if="scope.row.sourceUrl" :href="scope.row.sourceUrl" target="_blank" rel="noreferrer">查看核验链接</a>
              <span v-else>未填写核验链接</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="notes" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="viewDetail(scope.row)">详情</el-button>
            <el-button link type="primary" @click="editRecord(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="deleteRecord(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty :image-size="76" :description="loadError ? '加载失败，请刷新重试' : '暂无符合条件的接种档案'" /></template>
      </el-table>
      
    </section>

    <el-pagination
      :current-page="page"
      :page-size="size"
      :total="total"
      @current-change="handlePageChange"
      layout="total, prev, pager, next"
      class="pagination"
    />

    <el-dialog v-model="showDetailModal" title="疫苗记录详情" width="800px">
      <div v-if="detailLoading" class="detail-loading"><el-skeleton :rows="8" animated /></div>
      <div v-else-if="detailError" class="detail-error"><el-empty description="记录暂时无法加载" :image-size="72" /><el-button @click="loadDetail">重新加载</el-button></div>
      <template v-else-if="selectedRecord">
        <div class="record-profile-banner">
          <div class="record-profile-icon"><el-icon><Notebook /></el-icon></div>
          <div class="record-profile-copy"><span>RECORD #{{ selectedRecord.id }} · PET #{{ selectedRecord.petId }}</span><h2>{{ selectedRecord.petName || '宠物 #' + selectedRecord.petId }}</h2><p>接种日期 {{ formatDate(selectedRecord.vaccinateDate) || '未填写' }}</p></div>
          <el-tag type="success" effect="plain">接种档案</el-tag>
        </div>
        <RecordDetailSections :sections="detailSections" />
      </template>
      <template #footer><el-button @click="showDetailModal = false">关闭</el-button><el-button type="primary" :disabled="detailLoading || detailError || !selectedRecord" @click="editFromDetail">编辑记录</el-button></template>
    </el-dialog>

    <el-dialog v-model="showAddModal" :title="editForm.id ? '编辑疫苗记录' : '添加疫苗记录'" width="740px">
      <p class="form-intro">关联宠物后，记录本次接种信息与后续计划日期。</p>
      <el-form :key="formSession" :model="editForm" :rules="rules" ref="formRef" label-position="top" class="record-form">
        <el-divider content-position="left">关联宠物</el-divider>
        <el-form-item class="form-wide" label="选择宠物"><RecordPetSelect v-if="showAddModal" v-model="editForm.petId" :pet-name="editForm.petName" @select="applySelectedPet" /><p class="field-help">可搜索名称或品种；也可在下方直接填写已有宠物 ID。</p></el-form-item>
        <el-form-item label="宠物 ID" prop="petId"><el-input-number v-model="editForm.petId" :min="1" :step="1" :precision="0" /></el-form-item>
        <el-form-item label="宠物名称"><el-input v-model="editForm.petName" placeholder="便于后台识别，可不填" /></el-form-item>
        <el-divider content-position="left">疫苗与接种</el-divider>
        <el-form-item label="疫苗名称" prop="vaccineName"><el-input v-model="editForm.vaccineName" /></el-form-item>
        <el-form-item label="疫苗类型"><el-input v-model="editForm.vaccineType" /></el-form-item>
        <el-form-item label="剂量"><el-input v-model="editForm.dose" /></el-form-item>
        <el-form-item label="接种日期" prop="vaccinateDate">
          <el-date-picker v-model="editForm.vaccinateDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disableFutureDate" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下次接种">
          <el-date-picker v-model="editForm.nextDate" type="date" :disabled-date="disableNextDate" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-divider content-position="left">接种机构与备注</el-divider>
        <el-form-item label="医院"><el-input v-model="editForm.hospital" /></el-form-item>
        <el-form-item label="医生"><el-input v-model="editForm.doctor" /></el-form-item>
        <el-form-item class="form-wide" label="备注"><el-input v-model="editForm.notes" type="textarea" :rows="3" /></el-form-item>
        <el-divider content-position="left">来源与核验</el-divider>
        <el-form-item label="来源名称"><el-input v-model="editForm.sourceName" placeholder="例如：疫苗本、医院凭证、AAHA 指南" /></el-form-item>
        <el-form-item class="form-wide" label="来源链接"><el-input v-model="editForm.sourceUrl" placeholder="公开可访问的核验链接，没有则留空" /></el-form-item>
        <el-form-item class="form-wide" label="核验说明"><el-input v-model="editForm.sourceNote" type="textarea" :rows="3" placeholder="说明接种项目与日期是否为原始凭证记录" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRecord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Notebook } from '@element-plus/icons-vue'
import request from '../utils/request'
import RecordDetailSections from '../components/RecordDetailSections.vue'
import RecordPetSelect from '../components/RecordPetSelect.vue'

const keyword = ref('')
const petIdFilter = ref(null)
const page = ref(1)
const size = ref(10)
const records = ref([])
const total = ref(0)
const showAddModal = ref(false)
const formRef = ref(null)
const formSession = ref(0)
const loading = ref(false)
const saving = ref(false)
const loadError = ref(false)

const showDetailModal = ref(false)
const selectedRecord = ref(null)
const detailLoading = ref(false)
const detailError = ref(false)
let detailVersion = 0
const displayValue = value => value === null || value === undefined || value === '' ? null : value
const withUnit = (value, unit) => displayValue(value) === null ? null : String(value) + ' ' + unit
const formatDate = value => value ? String(value).slice(0, 10) : null
const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : null
const viewDetail = record => { selectedRecord.value = { ...record }; showDetailModal.value = true; loadDetail() }
const loadDetail = async () => {
  if (!selectedRecord.value?.id) return
  const version = ++detailVersion
  const id = selectedRecord.value.id
  detailLoading.value = true
  detailError.value = false
  try {
    const res = await request.get('/admin/vaccine/' + id)
    if (res.code !== 200 || !res.data) throw new Error('记录不存在')
    if (version === detailVersion) selectedRecord.value = { ...res.data, petName: res.data.petName || selectedRecord.value.petName }
  } catch {
    if (version === detailVersion) detailError.value = true
  } finally {
    if (version === detailVersion) detailLoading.value = false
  }
}
const editFromDetail = () => { const record = { ...selectedRecord.value }; showDetailModal.value = false; editRecord(record) }

const isPastDate = value => {
  if (!value) return false
  const date = new Date(String(value).slice(0, 10) + 'T00:00:00')
  return Number.isFinite(date.getTime()) && date.getTime() < new Date().setHours(0, 0, 0, 0)
}
const detailSections = computed(() => {
  const record = selectedRecord.value
  if (!record) return []
  return [
    { title: '关联宠物', fields: [
      { label: '记录编号', value: record.id }, { label: '宠物编号', value: record.petId },
      { label: '宠物名称', value: record.petName }, { label: '录入时间', value: formatTime(record.createdAt) }
    ] },
    { title: '疫苗与接种', fields: [
      { label: '疫苗名称', value: record.vaccineName }, { label: '疫苗类型', value: record.vaccineType },
      { label: '剂量', value: record.dose }, { label: '接种日期', value: formatDate(record.vaccinateDate) },
      { label: '下次接种计划', value: formatDate(record.nextDate) },
      { label: '计划说明', value: isPastDate(record.nextDate) ? '此条计划日期已过，请核对是否已有后续接种记录。' : record.nextDate ? '按此条记录保存的计划日期展示。' : '此条记录未填写下次接种计划。', wide: true }
    ] },
    { title: '接种机构与备注', fields: [
      { label: '医院', value: record.hospital }, { label: '医生', value: record.doctor }, { label: '备注', value: record.notes, wide: true }
    ] },
    { title: '来源与核验', fields: [
      { label: '来源名称', value: record.sourceName, wide: true }, { label: '来源链接', value: record.sourceUrl, link: true, wide: true },
      { label: '核验说明', value: record.sourceNote, wide: true }
    ] }
  ]
})


const emptyForm = () => ({
  id: null, petId: null, petName: '', vaccineName: '', vaccineType: '',
  dose: '', vaccinateDate: '', nextDate: '', hospital: '', doctor: '', notes: '',
  sourceName: '', sourceUrl: '', sourceNote: ''
})

const editForm = reactive(emptyForm())

const rules = {
  petId: [{ required: true, message: '请输入宠物ID', trigger: 'change' }],
  vaccineName: [{ required: true, message: '请输入疫苗名称', trigger: 'blur' }],
  vaccinateDate: [{ required: true, message: '请选择接种日期', trigger: 'change' }]
}

onMounted(loadRecords)

async function loadRecords() {
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/admin/vaccine', {
      params: { page: page.value, size: size.value, keyword: keyword.value, petId: petIdFilter.value || undefined }
    })
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    records.value = []
    total.value = 0
    loadError.value = true
    ElMessage.error('疫苗记录加载失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => { keyword.value = ''; petIdFilter.value = null; search() }
const applySelectedPet = pet => { editForm.petName = pet?.name || '' }

const search = () => {
  page.value = 1
  loadRecords()
}

const handlePageChange = value => {
  page.value = value
  loadRecords()
}

const editRecord = async record => {
  formSession.value += 1
  Object.keys(editForm).forEach(key => delete editForm[key])
  Object.assign(editForm, emptyForm(), record)
  showAddModal.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openCreate = async () => {
  formSession.value += 1
  Object.keys(editForm).forEach(key => delete editForm[key])
  Object.assign(editForm, emptyForm())
  formRef.value?.clearValidate()
  showAddModal.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const saveRecord = async () => {
  if (!formRef.value || saving.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
    saving.value = true
    try {
      if (editForm.nextDate && editForm.vaccinateDate && editForm.nextDate < editForm.vaccinateDate) {
        ElMessage.warning('下次接种日期不能早于本次接种日期')
        return
      }
      const res = editForm.id
        ? await request.put(`/admin/vaccine/${editForm.id}`, editForm)
        : await request.post('/admin/vaccine', editForm)
      if (res.code !== 200) throw new Error(res.message || '保存失败')
      ElMessage.success(res.message || '保存成功')
      showAddModal.value = false
      await loadRecords()
    } catch {
      ElMessage.error('疫苗记录保存失败，请检查日期与来源信息')
    } finally {
      saving.value = false
    }
}

const deleteRecord = async id => {
  try {
    await ElMessageBox.confirm('确定要删除该疫苗记录吗？', '提示', { type: 'warning' })
    const res = await request.delete(`/admin/vaccine/${id}`)
    if (res.code !== 200) throw new Error(res.message || '删除失败')
    ElMessage.success(res.message || '删除成功')
    await loadRecords()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('疫苗记录删除失败')
  }
}

const disableFutureDate = date => date.getTime() > new Date().setHours(23, 59, 59, 999)
const disableNextDate = date => editForm.vaccinateDate && date.getTime() < new Date(`${editForm.vaccinateDate}T00:00:00`).getTime()
</script>

<style scoped>
.record-pet-filter { width: 240px; }
.record-pet-cell strong, .record-pet-cell span { display: block; }
.record-pet-cell strong { color: #2c4437; font-size: 13px; font-weight: 600; }
.record-pet-cell span { margin-top: 4px; color: #879184; font-size: 11px; }
.record-date { color: #667864; font-variant-numeric: tabular-nums; font-size: 12px; }
.next-date-cell { display: flex; flex-direction: column; align-items: flex-start; gap: 5px; }
.form-intro { margin: 0 0 12px; color: #7c8980; font-size: 13px; line-height: 1.8; }
.field-help { margin: 7px 0 0; color: #899585; font-size: 11px; line-height: 1.6; }
.record-profile-banner { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; padding: 23px; background: #f1f5ed; border: 1px solid #e0e8da; border-radius: 16px; }
.record-profile-icon { display: grid; place-items: center; width: 64px; height: 64px; flex-shrink: 0; color: #5f8166; background: #e3ecdb; border: 3px solid white; border-radius: 18px; font-size: 28px; }
.record-profile-copy { flex: 1; min-width: 0; }
.record-profile-copy > span { color: #7d9079; font-size: 10px; letter-spacing: .12em; }
.record-profile-copy h2 { margin: 7px 0; color: #263b33; font-size: 22px; overflow-wrap: anywhere; }
.record-profile-copy p { margin: 0; color: #72836e; font-size: 12px; }
.detail-loading { padding: 20px 8px; }
.detail-error { display: grid; justify-items: center; padding-bottom: 20px; }

.source-cell strong,
.source-cell span,
.source-cell a {
  display: block;
}

.source-cell strong {
  color: #44584c;
  font-size: 12px;
}

.source-cell span,
.source-cell a {
  margin-top: 3px;
  color: #7c8980;
  font-size: 12px;
}

.source-cell a {
  color: #28745b;
  text-decoration: none;
}

.source-cell a:hover {
  text-decoration: underline;
}


.record-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 20px; }
.record-form .form-wide, .record-form .el-divider { grid-column: 1 / -1; }
.record-form :deep(.el-form-item) { min-width: 0; }
.record-form :deep(.el-input-number) { width: 100%; }
@media (max-width: 600px) {
  .record-form { grid-template-columns: 1fr; }
  .record-pet-filter { width: 100%; }
  .record-profile-banner { padding: 16px; gap: 12px; flex-wrap: wrap; }
  .record-profile-icon { width: 50px; height: 50px; }
  .record-profile-banner > .el-tag { margin-left: 62px; }
}
</style>
