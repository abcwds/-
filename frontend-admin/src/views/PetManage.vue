
<template>
  <div class="pet-manage management-page">
    <section class="page-intro">
      <div>
        <div class="page-kicker">PET DIRECTORY</div>
        <h1>宠物档案</h1>
        <p>让每一份档案，连接一个温暖的新家。</p>
      </div>
      <el-button :loading="loading" aria-label="刷新宠物列表" @click="loadPets">
        <el-icon><Refresh /></el-icon>刷新列表
      </el-button>
    </section>

    <section class="filter-panel">
      <div class="search-bar">
        <el-input v-model="keyword" class="search-field" placeholder="搜索宠物名称/品种" clearable @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="petType" placeholder="全部类型" clearable class="filter-select" :loading="typesLoading" @change="search" @visible-change="visible => visible && loadPetTypes()">
          <el-option label="犬" value="Dog" />
          <el-option label="猫" value="Cat" />
          <el-option-group v-if="otherPetTypes.length" label="其他类型">
            <el-option v-for="type in otherPetTypes" :key="type" :label="type" :value="type" />
          </el-option-group>
          <el-option v-else :label="typesError ? '其他类型 · 加载失败，请重新展开' : '其他类型 · 暂无记录'" value="__unavailable" disabled />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable class="filter-select" @change="search">
          <el-option :label="statusText(0)" :value="0" />
          <el-option :label="statusText(1)" :value="1" />
          <el-option :label="statusText(2)" :value="2" />
        </el-select>
        <el-button @click="search">查询</el-button>
        <el-button v-if="keyword || petType || status === 0 || status === 1 || status === 2" text @click="resetFilters">重置</el-button>
        <el-button class="toolbar-action" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          添加宠物
        </el-button>
      </div>
    </section>

    <el-alert v-if="loadError" class="load-alert" title="宠物档案加载失败" description="请检查后端服务后重试。" type="warning" show-icon :closable="false">
      <el-button link type="warning" :loading="loading" @click="loadPets">重新加载</el-button>
    </el-alert>

    <section class="table-panel">
      <div class="table-heading"><h2>宠物档案 <span class="table-count">{{ loadError ? '—' : total }}</span></h2><span>{{ keyword || petType || status === 0 || status === 1 || status === 2 ? '当前筛选结果' : '全部记录' }}</span></div>
      <el-table v-loading="loading" :data="pets" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="宠物" min-width="190">
        <template #default="scope">
          <div class="pet-cell">
            <img :src="resolveImage(scope.row)" :style="imageStyle(scope.row)" :alt="scope.row.name" @error="handleImageError($event, scope.row)" />
            <div>
              <strong>{{ scope.row.name }}</strong>
              <span>{{ typeText(scope.row.type) }} · {{ scope.row.breed || '未填写品种' }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄(月)" width="100" />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="scope">{{ genderText(scope.row.gender) }}</template>
      </el-table-column>
      <el-table-column prop="weight" label="体重(kg)" width="100" />
      <el-table-column prop="healthStatus" label="健康状况" />
      <el-table-column prop="adoptFee" label="领养费" width="110"><template #default="scope">{{ formatMoney(scope.row.adoptFee) }}</template></el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" min-width="220">
        <template #default="scope">
          <div class="source-cell">
            <strong>{{ scope.row.sourceName || '待补来源' }}</strong>
            <a v-if="scope.row.sourceUrl" :href="scope.row.sourceUrl" target="_blank" rel="noreferrer">查看核验链接</a>
            <span v-else>请补充公开来源</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="viewDetail(scope.row)">详情</el-button>
          <el-button link type="primary" @click="editPet(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="deletePet(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
        <template #empty><el-empty :image-size="76" :description="loadError ? '加载失败，请刷新重试' : '暂无符合条件的宠物档案'" /></template>
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

    <el-dialog v-model="showDetailModal" title="宠物完整档案" width="800px" class="pet-detail-dialog">
      <div v-if="detailLoading" class="detail-loading"><el-skeleton :rows="8" animated /></div>
      <div v-else-if="detailError" class="detail-error"><el-empty description="档案暂时无法加载" :image-size="72" /><el-button @click="loadDetail">重新加载</el-button></div>
      <template v-else-if="selectedPet">
        <div class="pet-profile-banner">
          <img :src="resolveImage(selectedPet)" :style="imageStyle(selectedPet)" :alt="selectedPet.name" @error="handleImageError($event, selectedPet)" />
          <div class="pet-profile-copy"><span class="detail-eyebrow">PET PROFILE · #{{ selectedPet.id }}</span><h2>{{ selectedPet.name || '未命名宠物' }}</h2><p>{{ typeText(selectedPet.type) }} · {{ selectedPet.breed || '品种未填写' }}</p></div>
          <el-tag :type="statusType(selectedPet.status)">{{ statusText(selectedPet.status) }}</el-tag>
        </div>
        <RecordDetailSections :sections="detailSections" />
      </template>
      <template #footer><el-button @click="showDetailModal = false">关闭</el-button><el-button type="primary" :disabled="detailLoading || detailError || !selectedPet" @click="editFromDetail">编辑档案</el-button></template>
    </el-dialog>

    <el-dialog v-model="showAddModal" :title="editForm.id ? '编辑宠物' : '添加宠物'" width="740px" @closed="resetForm">
      <p class="form-intro">完善基础资料、领养信息与来源记录，让每份档案清晰可信。</p>
      <el-form :model="editForm" :rules="rules" ref="formRef" label-position="top" class="record-form">
        <el-divider content-position="left">宠物基本信息</el-divider>
        <el-form-item label="名称" prop="name"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="类型" prop="type"><el-select v-model="editForm.type" filterable allow-create default-first-option placeholder="选择或输入宠物类型"><el-option label="犬 / Dog" value="Dog" /><el-option label="猫 / Cat" value="Cat" /></el-select></el-form-item>
        <el-form-item label="品种"><el-input v-model="editForm.breed" /></el-form-item>
        <el-form-item label="年龄(月)"><el-input-number v-model="editForm.age" :min="0" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="0">公</el-radio>
            <el-radio :value="1">母</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="体重(kg)"><el-input-number v-model="editForm.weight" :min="0" :step="0.1" /></el-form-item>
        <el-form-item label="毛色"><el-input v-model="editForm.color" /></el-form-item>
        <el-form-item class="form-wide" label="描述"><el-input v-model="editForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item class="form-wide" label="图片">
          <div class="image-editor">
            <img :src="resolveImage(editForm)" :style="imageStyle(editForm)" alt="图片预览" @error="handleImageError($event, editForm)" />
            <el-input v-model="editForm.image" placeholder="输入公开图片 URL 或 /uploads 本地路径" />
          </div>
        </el-form-item>
        <el-divider content-position="left">领养与照护</el-divider>
        <el-form-item label="健康状况"><el-input v-model="editForm.healthStatus" /></el-form-item>
        <el-form-item label="档案状态"><el-select v-model="editForm.status"><el-option v-for="value in [0, 1, 2]" :key="value" :label="statusText(value)" :value="value" /></el-select></el-form-item>
        <el-form-item label="领养费"><el-input-number v-model="editForm.adoptFee" :min="0" :step="0.01" /></el-form-item>
        <el-form-item label="救助站"><el-input v-model="editForm.shelter" /></el-form-item>
        <el-divider content-position="left">来源与核验</el-divider>
        <el-form-item label="资料来源"><el-input v-model="editForm.sourceName" placeholder="例如：AKC Breed Info CSV（品种资料）" /></el-form-item>
        <el-form-item label="来源核验日期"><el-date-picker v-model="editForm.sourceCheckedAt" type="date" value-format="YYYY-MM-DD" placeholder="选择实际核验日期" style="width: 100%" /></el-form-item>
        <el-form-item class="form-wide" label="资料链接"><el-input v-model="editForm.sourceUrl" placeholder="可公开访问的来源链接" /></el-form-item>
        <el-form-item label="图片来源"><el-input v-model="editForm.imageSourceName" placeholder="例如：Dog CEO Dog API" /></el-form-item>
        <el-form-item class="form-wide" label="图片链接"><el-input v-model="editForm.imageSourceUrl" placeholder="图片接口或原图链接" /></el-form-item>
        <el-form-item class="form-wide" label="核验说明"><el-input v-model="editForm.sourceNote" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePet">保存档案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import request from '../utils/request'
import RecordDetailSections from '../components/RecordDetailSections.vue'
import { getPetImageStyle, handlePetImageError, resolvePetImage } from '../utils/petImage'

const keyword = ref('')
const petType = ref('')
const otherPetTypes = ref([])
const typesLoading = ref(false)
const typesError = ref(false)
const status = ref(null)
const page = ref(1)
const size = ref(10)
const pets = ref([])
const total = ref(0)
const showAddModal = ref(false)
const formRef = ref(null)
const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const showDetailModal = ref(false)
const selectedPet = ref(null)
const detailLoading = ref(false)
const detailError = ref(false)
let detailVersion = 0
const displayValue = value => value === null || value === undefined || value === '' ? null : value
const withUnit = (value, unit) => displayValue(value) === null ? null : `${value} ${unit}`
const formatDate = value => value ? String(value).replace('T', ' ').slice(0, 16) : null
const genderText = value => value === 0 ? '公' : value === 1 ? '母' : '未填写'
const formatMoney = value => displayValue(value) === null ? '—' : `¥${Number(value).toFixed(2)}`
const detailSections = computed(() => {
  const pet = selectedPet.value
  if (!pet) return []
  return [
    { title: '基础资料', description: '身份与体征', fields: [
      { label: '档案编号', value: pet.id }, { label: '名称', value: pet.name },
      { label: '类型', value: typeText(pet.type) }, { label: '品种', value: pet.breed },
      { label: '年龄', value: withUnit(pet.age, '个月') }, { label: '性别', value: genderText(pet.gender) },
      { label: '体重', value: withUnit(pet.weight, 'kg') }, { label: '毛色', value: pet.color },
      { label: '描述', value: pet.description, wide: true }
    ] },
    { title: '领养与照护', fields: [
      { label: '领养状态', value: statusText(pet.status) }, { label: '健康状况', value: pet.healthStatus },
      { label: '领养费用', value: displayValue(pet.adoptFee) === null ? null : formatMoney(pet.adoptFee) }, { label: '救助站', value: pet.shelter }
    ] },
    { title: '来源与图片', description: '保留原始记录', fields: [
      { label: '资料来源', value: pet.sourceName }, { label: '来源核验日期', value: pet.sourceCheckedAt },
      { label: '资料链接', value: pet.sourceUrl, link: true, wide: true }, { label: '图片来源', value: pet.imageSourceName, wide: true },
      { label: '图片原始链接', value: pet.imageSourceUrl, link: true, wide: true }, { label: '当前图片地址', value: pet.image, link: true, wide: true },
      { label: '核验说明', value: pet.sourceNote, wide: true }
    ] },
    { title: '档案时间', fields: [{ label: '创建时间', value: formatDate(pet.createdAt) }, { label: '最后更新时间', value: formatDate(pet.updatedAt) }] }
  ]
})

const viewDetail = pet => { selectedPet.value = { ...pet }; showDetailModal.value = true; loadDetail() }
const loadDetail = async () => {
  if (!selectedPet.value?.id) return
  const version = ++detailVersion
  const id = selectedPet.value.id
  detailLoading.value = true
  detailError.value = false
  try {
    const res = await request.get(`/admin/pets/${id}`)
    if (res.code !== 200 || !res.data) throw new Error('档案不存在')
    if (version === detailVersion) selectedPet.value = res.data
  } catch {
    if (version === detailVersion) detailError.value = true
  } finally {
    if (version === detailVersion) detailLoading.value = false
  }
}
const editFromDetail = () => { const pet = { ...selectedPet.value }; showDetailModal.value = false; editPet(pet) }

const editForm = reactive({
  id: null, name: '', type: '', breed: '', age: null, gender: 0,
  weight: null, color: '', description: '', image: '',
  healthStatus: '健康', adoptFee: 0, shelter: '', status: 0,
  sourceName: '', sourceUrl: '', sourceCheckedAt: '', imageSourceName: '',
  imageSourceUrl: '', sourceNote: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请输入类型', trigger: 'blur' }]
}

onMounted(() => {
  loadPets()
})

const loadPetTypes = async () => {
  if (typesLoading.value) return
  typesLoading.value = true
  typesError.value = false
  try {
    const res = await request.get('/pets/types')
    if (res.code !== 200 || !Array.isArray(res.data)) throw new Error('类型加载失败')
    otherPetTypes.value = [...new Set(res.data.map(item => item.type).filter(type => typeof type === 'string' && type.trim() && type !== 'Dog' && type !== 'Cat'))]
  } catch {
    typesError.value = true
  } finally {
    typesLoading.value = false
  }
}

const loadPets = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/admin/pets', {
      params: { page: page.value, size: size.value, keyword: keyword.value, type: petType.value || undefined, status: status.value }
    })
    if (res.code === 200) {
      pets.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      throw new Error(res.message || '加载失败')
    }
  } catch (error) {
    loadError.value = true
    pets.value = []
    ElMessage.error('宠物档案加载失败')
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  loadPets()
}

const resetFilters = () => {
  keyword.value = ''
  petType.value = ''
  status.value = null
  search()
}

const handlePageChange = (val) => {
  page.value = val
  loadPets()
}

const statusText = (status) => {
  const map = { 0: '待领养', 1: '已领养', 2: '已下架' }
  return map[status] || '未知'
}

const statusType = (status) => {
  const map = { 0: 'success', 1: 'info', 2: 'danger' }
  return map[status] || ''
}

const editPet = (pet) => {
  Object.assign(editForm, pet)
  showAddModal.value = true
}

const openCreate = () => {
  resetForm()
  showAddModal.value = true
}

const resetForm = () => {
  Object.keys(editForm).forEach(key => delete editForm[key])
  Object.assign(editForm, {
    id: null, name: '', type: 'Dog', breed: '', age: null, gender: 0,
    weight: null, color: '', description: '', image: '',
    healthStatus: '健康', adoptFee: 0, shelter: '', status: 0,
    sourceName: '', sourceUrl: '', sourceCheckedAt: '', imageSourceName: '',
    imageSourceUrl: '', sourceNote: ''
  })
  formRef.value?.clearValidate()
}

const savePet = async () => {
  if (!formRef.value || saving.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
      const res = editForm.id
        ? await request.put(`/admin/pets/${editForm.id}`, editForm)
        : await request.post('/admin/pets', editForm)
      if (res.code !== 200) throw new Error(res.message || '保存失败')
      ElMessage.success(res.message || '档案已保存')
      showAddModal.value = false
      await loadPets()
  } catch {
    ElMessage.error('宠物档案保存失败，请重试')
  } finally {
    saving.value = false
  }
}

const deletePet = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该宠物档案吗？', '删除宠物档案', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' })
    const res = await request.delete(`/admin/pets/${id}`)
    if (res.code !== 200) throw new Error(res.message || '删除失败')
    ElMessage.success(res.message || '删除成功')
    if (pets.value.length === 1 && page.value > 1) page.value -= 1
    await loadPets()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('宠物档案删除失败，请重试')
  }
}

const typeText = (type) => type === 'Dog' ? '犬' : type === 'Cat' ? '猫' : (type || '未知')

const resolveImage = resolvePetImage
const imageStyle = getPetImageStyle
const handleImageError = handlePetImageError
</script>

<style scoped>
.pet-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pet-cell img,
.image-editor img {
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  object-fit: cover;
  border-radius: 10px;
  background: #edf6f2;
}

.pet-cell strong,
.pet-cell span,
.source-cell strong,
.source-cell span,
.source-cell a {
  display: block;
}

.pet-cell strong {
  color: #25483d;
  font-size: 13px;
}

.pet-cell span,
.source-cell span,
.source-cell a {
  margin-top: 3px;
  color: #7c8980;
  font-size: 12px;
}

.source-cell strong {
  color: #44584c;
  font-size: 12px;
}

.source-cell a {
  color: #28745b;
  text-decoration: none;
}

.source-cell a:hover {
  text-decoration: underline;
}

.image-editor {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.image-editor img {
  width: 64px;
  height: 64px;
  border-radius: 8px;
}

.record-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 20px; }
.record-form .form-wide, .record-form .el-divider { grid-column: 1 / -1; }
.record-form :deep(.el-form-item) { min-width: 0; }
.record-form :deep(.el-input-number) { width: 100%; }
.form-intro { margin: 0 0 12px; color: #7c8980; font-size: 13px; line-height: 1.8; }
.pet-profile-banner { display: flex; align-items: center; gap: 18px; margin-bottom: 24px; padding: 23px; background: #f1f5ed; border: 1px solid #e0e8da; border-radius: 16px; }
.pet-profile-banner > img { width: 88px; height: 88px; flex-shrink: 0; object-fit: cover; border: 4px solid white; border-radius: 20px; box-shadow: 0 5px 15px #3548300a; }
.pet-profile-copy { flex: 1; min-width: 0; }
.detail-eyebrow { color: #7d9079; font-size: 10px; letter-spacing: .13em; }
.pet-profile-copy h2 { margin: 7px 0; color: #263b33; font-size: 24px; overflow-wrap: anywhere; }
.pet-profile-copy p { margin: 0; color: #72836e; font-size: 13px; overflow-wrap: anywhere; }
.detail-loading { padding: 20px 8px; }
.detail-error { display: grid; justify-items: center; padding-bottom: 20px; }
@media (max-width: 600px) {
  .record-form { grid-template-columns: 1fr; }
  .pet-profile-banner { padding: 16px; gap: 12px; flex-wrap: wrap; }
  .pet-profile-banner > img { width: 68px; height: 68px; }
  .pet-profile-banner > .el-tag { margin-left: 80px; }
}
</style>
