
<template>
  <div class="pet-list">
    <div class="container">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索宠物名称或品种" clearable class="search-input">
          <template #append>
            <el-button @click="search"><i class="el-icon-search"></i></el-button>
          </template>
        </el-input>
        <el-select v-model="type" placeholder="选择类型" clearable class="type-select">
          <el-option label="全部" value="" />
          <el-option v-for="t in types" :key="t.type" :label="t.type" :value="t.type" />
        </el-select>
      </div>

      <div v-if="loading" class="list-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在加载宠物档案…</span>
      </div>
      <el-alert v-else-if="loadError" title="宠物档案暂时无法加载" description="请稍后重试，看看有没有新的伙伴在等你。" type="warning" show-icon :closable="false">
        <el-button link type="warning" @click="loadPets">重新加载</el-button>
      </el-alert>
      <el-empty v-else-if="pets.length === 0" description="暂无符合条件的宠物" />
      <div v-else class="pet-grid">
        <div class="pet-card" v-for="pet in pets" :key="pet.id" @click="$router.push(`/pet/${pet.id}`)">
          <div class="pet-image">
            <img :src="resolveImageUrl(pet)" :style="imageStyle(pet)" :alt="pet.name" @error="handleImageError($event, pet)" />
            <div class="pet-gender">{{ pet.gender === 0 ? '公' : '母' }}</div>
          </div>
          <div class="pet-info">
            <h3>{{ pet.name }}</h3>
            <p class="pet-type">{{ pet.type }} · {{ pet.breed }}</p>
            <div class="pet-meta">
              <span>{{ pet.age }}个月</span>
              <span>{{ pet.weight }}kg</span>
              <span>{{ pet.color }}</span>
            </div>
            <p class="pet-desc">{{ pet.description?.slice(0, 40) || '暂无详细介绍，请进入详情页查看档案和来源。' }}</p>
            <div class="pet-footer">
              <span class="pet-fee">¥{{ pet.adoptFee }}</span>
              <span class="pet-status">{{ statusText(pet.status) }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-pagination
        v-if="total > 0"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="handlePageChange"
        layout="total, prev, pager, next, jumper"
        class="pagination"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import request from '../utils/request'
import { getPetImageStyle, handlePetImageError, resolvePetImage } from '../utils/petImage'

const keyword = ref('')
const type = ref('')
const page = ref(1)
const size = ref(12)
const pets = ref([])
const total = ref(0)
const types = ref([])
const loading = ref(false)
const loadError = ref(false)

onMounted(() => {
  loadTypes()
  loadPets()
})

const loadTypes = async () => {
  const res = await request.get('/pets/types')
  if (res.code === 200) {
    types.value = res.data
  }
}

const loadPets = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get('/pets', {
      params: { page: page.value, size: size.value, type: type.value, keyword: keyword.value }
    })
    if (res.code === 200) {
      pets.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      throw new Error(res.message || '加载失败')
    }
  } catch (error) {
    pets.value = []
    total.value = 0
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  loadPets()
}

const handlePageChange = (val) => {
  page.value = val
  loadPets()
}

const statusText = (status) => {
  const map = { 0: '待领养', 1: '已领养', 2: '已下架' }
  return map[status] || '未知'
}

const resolveImageUrl = resolvePetImage
const imageStyle = getPetImageStyle
const handleImageError = handlePetImageError
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 30px;
  justify-content: center;
}

.list-state {
  min-height: 220px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  color: #78928a;
  font-size: 14px;
}

.list-state .el-icon {
  color: #2d9f86;
  font-size: 26px;
}

.search-input {
  width: 300px;
}

.type-select {
  width: 150px;
}

.pet-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.pet-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}

.pet-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.pet-image {
  height: 220px;
  overflow: hidden;
  position: relative;
}

.pet-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.pet-card:hover .pet-image img {
  transform: scale(1.05);
}

.pet-gender {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  color: #667eea;
}

.pet-info {
  padding: 20px;
}

.pet-info h3 {
  font-size: 20px;
  margin-bottom: 8px;
  color: #333;
}

.pet-type {
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
}

.pet-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.pet-meta span {
  font-size: 12px;
  color: #666;
  background: #f5f7fa;
  padding: 4px 10px;
  border-radius: 8px;
}

.pet-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pet-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pet-fee {
  font-size: 18px;
  color: #667eea;
  font-weight: bold;
}

.pet-status {
  font-size: 12px;
  color: #fff;
  background: #67c23a;
  padding: 4px 12px;
  border-radius: 12px;
}

.pagination {
  margin-top: 40px;
  text-align: center;
}
</style>
