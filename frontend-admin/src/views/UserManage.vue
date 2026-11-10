
<template>
  <div class="user-manage management-page">
    <section class="page-intro">
      <div><div class="page-kicker">COMMUNITY MEMBERS</div><h1>用户管理</h1><p>每一个账号，都是一次温暖相遇的开始。管理用户资料与访问权限。</p></div>
      <el-button :loading="loading" @click="loadUsers"><el-icon><Refresh /></el-icon>刷新列表</el-button>
    </section>
    <section class="filter-panel"><div class="search-bar">
      <el-input v-model="keyword" class="search-field" placeholder="搜索用户名、昵称或手机号" clearable @keyup.enter="search" @clear="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <span class="toolbar-description toolbar-action">按用户名、昵称或手机号检索账号</span>
    </div></section>

    <el-alert v-if="loadError" title="用户列表加载失败，请刷新重试" type="warning" :closable="false" show-icon />
    <section class="table-panel">
    <div class="table-heading"><h2>全部用户 <span class="table-count">{{ loadError ? '—' : total }}</span></h2><span>账号资料与权限</span></div>
    <el-table v-loading="loading" :data="users" style="width: 100%">
      <el-table-column prop="id" label="编号" width="78"><template #default="scope"><span class="muted-cell">#{{ scope.row.id }}</span></template></el-table-column>
      <el-table-column label="用户" min-width="200">
        <template #default="scope"><div class="user-cell"><el-avatar :size="38" :src="scope.row.avatar">{{ (scope.row.nickname || scope.row.username || '用').slice(0, 1) }}</el-avatar><div><strong>{{ scope.row.nickname || scope.row.username }}</strong><span>@{{ scope.row.username }}</span></div></div></template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="135"><template #default="scope">{{ scope.row.phone || '—' }}</template></el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip><template #default="scope">{{ scope.row.email || '—' }}</template></el-table-column>
      <el-table-column prop="role" label="角色" width="110">
        <template #default="scope"><el-tag :type="scope.row.role === 1 ? 'success' : 'info'">{{ scope.row.role === 1 ? '管理员' : '普通用户' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="账号状态" width="128">
        <template #default="scope">
          <div class="account-status"><el-switch :model-value="scope.row.status === 1" :loading="updatingIds.includes(scope.row.id)" :aria-label="`${scope.row.username}账号状态`" @change="toggleStatus(scope.row)" /><span>{{ scope.row.status === 1 ? '启用' : '停用' }}</span></div>
        </template>
      </el-table-column>
      <el-table-column label="加入时间" width="160"><template #default="scope"><span class="muted-cell">{{ formatDate(scope.row.createdAt) }}</span></template></el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="viewUser(scope.row)">详情</el-button>
          <el-button link type="danger" @click="deleteUser(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty :image-size="76" :description="loadError ? '加载失败，请刷新重试' : '暂无符合条件的用户'" /></template>
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
    <p class="management-footnote"><el-icon><User /></el-icon>账号资料由用户维护；管理员可查看详情、启停账号及删除账号。</p>
    <el-dialog v-model="detailOpen" title="用户详情" width="680px">
      <template v-if="selectedUser">
        <div class="detail-lead"><el-avatar :size="52" :src="selectedUser.avatar">{{ (selectedUser.nickname || selectedUser.username || '用').slice(0, 1) }}</el-avatar><div><h2>{{ selectedUser.nickname || selectedUser.username }}</h2><p>@{{ selectedUser.username }} · 账号 #{{ selectedUser.id }}</p></div><el-tag :type="selectedUser.status === 1 ? 'success' : 'info'">{{ selectedUser.status === 1 ? '启用中' : '已停用' }}</el-tag></div>
        <DetailSections :sections="userSections" />
      </template>
      <template #footer><el-button @click="detailOpen = false">关闭详情</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, User } from '@element-plus/icons-vue'
import request from '../utils/request'
import DetailSections from '../components/DetailSections.vue'

const keyword = ref('')
const page = ref(1)
const size = ref(10)
const users = ref([])
const total = ref(0)
const loading = ref(false)
const loadError = ref(false)
const updatingIds = ref([])
const detailOpen = ref(false)
const selectedUser = ref(null)
const userSections = computed(() => {
  const value = selectedUser.value || {}
  return [
    { title: '账号与联系方式', fields: [{ label: '用户名', value: value.username }, { label: '昵称', value: value.nickname }, { label: '手机号', value: value.phone }, { label: '电子邮箱', value: value.email }, { label: '账号角色', value: value.role === 1 ? '管理员' : '普通用户' }, { label: '账号状态', value: value.status === 1 ? '启用' : '停用' }] },
    { title: '资料与时间', fields: [{ label: '头像链接', value: value.avatar, link: true, wide: true }, { label: '注册时间', value: formatDate(value.createdAt) }, { label: '最近更新', value: formatDate(value.updatedAt) }] }
  ]
})
let loadSequence = 0
const formatDate = value => value ? String(value).replace('T', ' ').slice(0, 16) : '—'

onMounted(() => {
  loadUsers()
})

const loadUsers = async () => {
  const sequence = ++loadSequence
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/admin/users', { params: { page: page.value, size: size.value, keyword: keyword.value } })
    if (res.code !== 200) throw new Error(res.message || '加载失败')
    if (sequence !== loadSequence) return
    users.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    if (sequence !== loadSequence) return
    loadError.value = true
    users.value = []
    total.value = 0
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

const resetSearch = () => { keyword.value = ''; search() }
const viewUser = async user => {
  try {
    const res = await request.get(`/admin/users/${user.id}`)
    if (res.code !== 200 || !res.data) throw new Error('用户不存在')
    selectedUser.value = res.data
    detailOpen.value = true
  } catch { ElMessage.error('用户详情加载失败，请重试') }
}

const search = () => {
  page.value = 1
  loadUsers()
}

const handlePageChange = (val) => {
  page.value = val
  loadUsers()
}

const toggleStatus = async (user) => {
  if (updatingIds.value.includes(user.id)) return
  updatingIds.value.push(user.id)
  const nextStatus = user.status === 1 ? 0 : 1
  try {
    const res = await request.put(`/admin/users/${user.id}/status`, { status: nextStatus })
    if (res.code !== 200) throw new Error(res.message || '更新失败')
    user.status = nextStatus
    ElMessage.success(res.message || '账号状态已更新')
  } catch {
    ElMessage.error('账号状态更新失败，请重试')
  } finally {
    updatingIds.value = updatingIds.value.filter(id => id !== user.id)
  }
}

const deleteUser = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？删除后无法恢复。', '删除用户', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' })
    const res = await request.delete(`/admin/users/${id}`)
    if (res.code !== 200) throw new Error(res.message || '删除失败')
    ElMessage.success(res.message || '删除成功')
    if (users.value.length === 1 && page.value > 1) page.value -= 1
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('用户删除失败，请重试')
  }
}
</script>

<style scoped>
.user-cell { display: flex; align-items: center; gap: 11px; }
.user-cell .el-avatar { flex: 0 0 auto; background: #edf3ed; color: #28745b; font-weight: 600; }
.user-cell strong, .user-cell span { display: block; }
.user-cell strong { color: #263b33; font-size: 13px; font-weight: 600; }
.user-cell span { margin-top: 2px; color: #7c8980; font-size: 12px; }
.account-status { display: flex; align-items: center; gap: 7px; }
.account-status span { color: #7c8980; font-size: 12px; }
.muted-cell { color: #7c8980; font-size: 12px; font-variant-numeric: tabular-nums; }
</style>
