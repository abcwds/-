
<template>
  <div class="announcement-manage management-page">
    <section class="page-intro">
      <div><div class="page-kicker">NEWS & UPDATES</div><h1>公告资讯</h1><p>分享有用的养宠知识，传递平台的每一个新消息。</p></div>
      <el-button :loading="loading" @click="loadAnnouncements"><el-icon><Refresh /></el-icon>刷新列表</el-button>
    </section>
    <section class="filter-panel"><div class="search-bar">
      <el-input v-model="keyword" class="search-field" placeholder="搜索公告标题" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="type" class="filter-select" placeholder="全部分类" aria-label="公告分类筛选" filterable allow-create default-first-option clearable @change="search"><el-option label="平台公告" value="notice" /><el-option label="养宠资讯" value="news" /></el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="primary" class="toolbar-action" @click="openCreate"><el-icon><Plus /></el-icon>添加公告</el-button>
    </div></section>

    <el-alert v-if="loadError" title="公告资讯加载失败，请刷新重试" type="warning" :closable="false" show-icon />
    <section class="table-panel">
    <div class="table-heading"><h2>公告列表 <span class="table-count">{{ loadError ? '—' : total }}</span></h2><span>内容编辑与发布管理</span></div>
    <el-table v-loading="loading" :data="announcements" style="width: 100%">
      <el-table-column prop="id" label="编号" width="72"><template #default="scope"><span class="muted-cell">#{{ scope.row.id }}</span></template></el-table-column>
      <el-table-column prop="title" label="公告内容" min-width="330">
        <template #default="scope"><div class="article-cell"><span class="article-icon"><el-icon><Document /></el-icon></span><div><strong>{{ scope.row.title }}</strong><p>{{ contentPreview(scope.row.content) }}</p></div></div></template>
      </el-table-column>
      <el-table-column label="分类 / 排序" width="120"><template #default="scope"><div class="article-meta"><strong>{{ typeText(scope.row.type) }}</strong><span>排序 {{ scope.row.sortOrder ?? 0 }}</span></div></template></el-table-column>
      <el-table-column prop="author" label="作者" width="100" />
      <el-table-column prop="status" label="发布状态" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '已发布' : '草稿' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160"><template #default="scope"><span class="muted-cell">{{ formatDate(scope.row.createdAt) }}</span></template></el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="preview(scope.row)">预览</el-button>
          <el-button link type="primary" @click="editAnnouncement(scope.row)">编辑</el-button>
          <el-button link type="warning" :loading="updatingIds.includes(scope.row.id)" @click="toggleStatus(scope.row)">{{ scope.row.status === 1 ? '下架' : '发布' }}</el-button>
          <el-button link type="danger" @click="deleteAnnouncement(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty :image-size="76" :description="loadError ? '加载失败，请刷新重试' : '暂无符合条件的公告'" /></template>
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

    <el-dialog v-model="showAddModal" :title="editForm.id ? '编辑公告' : '添加公告'" width="780px" @closed="resetForm">
      <p class="editor-note">让信息清晰、温暖地传递给每一位领养人。</p>
      <el-form :model="editForm" :rules="rules" ref="formRef" label-position="top" class="announcement-form" :disabled="saving">
        <el-divider content-position="left">内容与发布</el-divider>
        <el-form-item label="公告标题" prop="title"><el-input v-model="editForm.title" placeholder="输入清晰、简洁的标题" /></el-form-item>
        <el-form-item label="正文内容" prop="content"><el-input v-model="editForm.content" type="textarea" :rows="7" placeholder="分享平台动态、领养须知或养宠知识…" /></el-form-item>
        <div class="editor-grid"><el-form-item label="作者"><el-input v-model.trim="editForm.author" placeholder="发布者或编辑署名" /></el-form-item><el-form-item label="内容分类"><el-select v-model="editForm.type" filterable allow-create default-first-option placeholder="选择或填写分类"><el-option label="平台公告" value="notice" /><el-option label="养宠资讯" value="news" /></el-select></el-form-item><el-form-item label="显示排序"><el-input-number v-model="editForm.sortOrder" :precision="0" :step="1" /><span class="editor-field-note">数值越大，展示越靠前。</span></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1" border>立即发布</el-radio>
            <el-radio :value="0" border>保存为草稿</el-radio>
          </el-radio-group>
        </el-form-item>
        </div>
        <el-divider content-position="left">引用来源（选填）</el-divider>
        <el-form-item label="来源名称"><el-input v-model.trim="editForm.sourceName" placeholder="填写参考机构或原文名称" /></el-form-item>
        <el-form-item label="来源链接" prop="sourceUrl"><el-input v-model.trim="editForm.sourceUrl" placeholder="https://…" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="preview(editForm)">预览内容</el-button>
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAnnouncement">{{ editForm.status === 1 ? '保存并发布' : '保存草稿' }}</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showPreview" title="公告预览" width="760px">
      <article v-if="previewItem" class="announcement-preview"><div class="preview-meta"><el-tag :type="previewItem.status === 1 ? 'success' : 'info'">{{ previewItem.status === 1 ? '已发布' : '草稿' }}</el-tag><span>{{ typeText(previewItem.type) }}</span><span>排序 {{ previewItem.sortOrder ?? 0 }}</span></div><h2>{{ previewItem.title || '未填写标题' }}</h2><p class="article-byline">{{ previewItem.author || '未填写作者' }}<span v-if="previewItem.createdAt"> · {{ formatDate(previewItem.createdAt) }}</span></p><div class="article-body">{{ previewItem.content || '未填写正文' }}</div><DetailSections :sections="previewSections" /></article>
      <template #footer><el-button @click="showPreview = false">关闭预览</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Document } from '@element-plus/icons-vue'
import request from '../utils/request'
import DetailSections from '../components/DetailSections.vue'
import { safeSourceUrl } from '../utils/navigation'

const keyword = ref('')
const type = ref('')
const page = ref(1)
const size = ref(10)
const announcements = ref([])
const total = ref(0)
const showAddModal = ref(false)
const formRef = ref(null)
const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const updatingIds = ref([])
const showPreview = ref(false)
const previewItem = ref(null)
let loadSequence = 0
const formatDate = value => value ? String(value).replace('T', ' ').slice(0, 16) : '—'
const contentPreview = value => String(value || '').replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').slice(0, 160)

const emptyForm = () => ({ id: null, title: '', content: '', author: '', type: 'notice', status: 1, sortOrder: 0, sourceName: '', sourceUrl: '' })
const editForm = reactive(emptyForm())
const typeText = value => ({ notice: '平台公告', news: '养宠资讯' })[value] || value || '未分类'
const previewSections = computed(() => [{ title: '来源与记录', fields: [{ label: '来源名称', value: previewItem.value?.sourceName }, { label: '最近更新', value: previewItem.value?.updatedAt ? formatDate(previewItem.value.updatedAt) : '暂未记录' }, { label: '来源链接', value: previewItem.value?.sourceUrl, link: true, wide: true }] }])
const preview = item => { previewItem.value = { ...item }; showPreview.value = true }

const rules = {
  title: [{ required: true, whitespace: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, whitespace: true, message: '请输入内容', trigger: 'blur' }],
  sourceUrl: [{ validator: (_, value, callback) => !value || safeSourceUrl(value) ? callback() : callback(new Error('请输入完整的 http(s) 来源链接')), trigger: 'blur' }]
}

onMounted(() => {
  loadAnnouncements()
})

const loadAnnouncements = async () => {
  const sequence = ++loadSequence
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/admin/announcements', {
      params: { page: page.value, size: size.value, keyword: keyword.value, type: type.value || undefined }
    })
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    if (sequence !== loadSequence) return
    announcements.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    if (sequence !== loadSequence) return
    loadError.value = true
    announcements.value = []
    total.value = 0
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}
const resetSearch = () => { keyword.value = ''; type.value = ''; search() }

const search = () => {
  page.value = 1
  loadAnnouncements()
}

const handlePageChange = (val) => {
  page.value = val
  loadAnnouncements()
}

const editAnnouncement = async (ann) => {
  resetForm()
  Object.assign(editForm, Object.fromEntries(Object.keys(emptyForm()).map(key => [key, ann[key] ?? emptyForm()[key]])))
  showAddModal.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const resetForm = () => {
  Object.keys(editForm).forEach(key => delete editForm[key])
  Object.assign(editForm, emptyForm())
  formRef.value?.clearValidate()
}

const openCreate = () => {
  resetForm()
  showAddModal.value = true
}

const saveAnnouncement = async () => {
  if (!formRef.value || saving.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const res = editForm.id
      ? await request.put(`/admin/announcements/${editForm.id}`, editForm)
      : await request.post('/admin/announcements', editForm)
    if (res.code !== 200) throw new Error(res.message || '保存失败')
    ElMessage.success(res.message || '公告已保存')
    showAddModal.value = false
    await loadAnnouncements()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '公告保存失败，请重试')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (ann) => {
  if (updatingIds.value.includes(ann.id)) return
  updatingIds.value.push(ann.id)
  const nextStatus = ann.status === 1 ? 0 : 1
  try {
    const res = await request.put(`/admin/announcements/${ann.id}/status`, { status: nextStatus })
    if (res.code !== 200) throw new Error(res.message || '更新失败')
    ElMessage.success(res.message || '发布状态已更新')
    ann.status = nextStatus
  } catch {
    ElMessage.error('公告状态更新失败，请重试')
  } finally {
    updatingIds.value = updatingIds.value.filter(id => id !== ann.id)
  }
}

const deleteAnnouncement = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该公告吗？', '删除公告', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' })
    const res = await request.delete(`/admin/announcements/${id}`)
    if (res.code !== 200) throw new Error(res.message || '删除失败')
    ElMessage.success(res.message || '删除成功')
    if (announcements.value.length === 1 && page.value > 1) page.value -= 1
    await loadAnnouncements()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('公告删除失败，请重试')
  }
}
</script>

<style scoped>
.editor-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 22px; }
.editor-grid .el-input-number { width: 100%; }
.editor-field-note { display: block; color: #99a28e; font-size: 10px; line-height: 1.7; margin-top: 5px; }
.article-meta strong, .article-meta span { display: block; }
.article-meta strong { font-size: 12px; font-weight: 500; }
.article-meta span { margin-top: 4px; color: #99a28e; font-size: 10px; }
.preview-meta { display: flex; align-items: center; gap: 12px; font-size: 11px; color: #85927d; }
.announcement-preview h2 { margin: 18px 0 12px; color: #2e4c38; font-size: 23px; line-height: 1.6; overflow-wrap: anywhere; }
.article-byline { color: #96a089; font-size: 11px; }
.article-body { margin: 24px 0; padding: 24px 0; border-top: 1px solid #e9ede6; border-bottom: 1px solid #e9ede6; white-space: pre-wrap; line-height: 2; font-size: 13px; color: #587057; overflow-wrap: anywhere; }
@media (max-width: 560px) { .editor-grid { grid-template-columns: 1fr; } .announcement-preview h2 { font-size: 20px; } }
.article-cell { display: flex; align-items: center; gap: 13px; padding: 3px 0; }
.article-cell > div { min-width: 0; }
.article-icon { display: grid; place-items: center; flex: 0 0 40px; height: 44px; background: #f0f4ed; color: #79906b; border-radius: 10px; font-size: 20px; }
.article-cell strong { display: block; color: #263b33; font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.article-cell p { margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #7c8980; font-size: 12px; }
.muted-cell { color: #7c8980; font-size: 12px; font-variant-numeric: tabular-nums; }
.editor-note { margin-bottom: 24px; color: #7c8980; font-size: 13px; line-height: 1.8; }
</style>
