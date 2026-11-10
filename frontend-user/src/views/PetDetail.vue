<template>
  <div class="pet-detail">
    <div class="decor-bg"></div>
    <div v-if="loading" class="detail-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在加载宠物档案…</span>
    </div>
    <el-alert v-else-if="loadError" class="detail-error" title="宠物档案暂时无法加载" description="请稍后重试，或返回宠物列表重新选择。" type="warning" show-icon :closable="false">
      <el-button link type="warning" @click="loadPet">重新加载</el-button>
    </el-alert>
    
    <div class="container" v-else-if="pet">
      <transition name="fade-up">
        <div class="pet-header glass-card">
          <div class="pet-main-image">
            <img :src="resolveImageUrl(pet)" :style="imageStyle(pet)" :alt="pet.name" @error="handleImageError($event, pet)" />
            <div class="image-overlay">
              <span class="image-tag">{{ pet.type }}</span>
            </div>
          </div>
          <div class="pet-basic-info">
            <div class="name-row">
              <h1>{{ pet.name }}</h1>
              <span class="breed-tag">{{ pet.breed }}</span>
            </div>
            <div class="pet-tags">
              <span class="tag gender-tag" :class="pet.gender === 0 ? 'male' : 'female'">
                {{ pet.gender === 0 ? '公' : '母' }}
              </span>
              <span class="tag">{{ pet.age }}个月</span>
              <span class="tag">{{ pet.weight }}kg</span>
              <span class="tag">{{ pet.color }}</span>
            </div>
            <div class="pet-status-bar">
              <span class="status-badge health" :class="pet.healthStatus === 'healthy' ? 'good' : 'warning'">
                <i class="el-icon-heart"></i>
                {{ pet.healthStatus === 'healthy' ? '健康' : pet.healthStatus }}
              </span>
              <span class="status-badge adopt" :class="pet.status === 0 ? 'pending' : 'adopted'">
                <i class="el-icon-user"></i>
                {{ statusText(pet.status) }}
              </span>
            </div>
            <div class="pet-fee">
              <span>领养费</span>
              <span class="fee-amount">¥{{ pet.adoptFee }}</span>
              <span class="fee-unit">起</span>
            </div>
            <div class="pet-shelter">
              <i class="el-icon-map-location"></i>
              <span>{{ pet.shelter }}</span>
            </div>
            <button 
              class="adopt-btn" 
              @click="handleAdoptClick" 
              :disabled="pet.status !== 0"
            >
              <i class="el-icon-star"></i>
              {{ pet.status === 0 ? '申请领养' : '已被领养' }}
            </button>
          </div>
        </div>
      </transition>

      <div class="pet-content">
        <transition name="fade-up" appear>
          <div class="content-section glass-card">
            <div class="section-header">
              <h2><i class="el-icon-file-text"></i> 宠物介绍</h2>
            </div>
            <p>{{ pet.description }}</p>
          </div>
        </transition>

        <transition name="fade-up" appear>
          <div class="content-section glass-card">
            <AiAdvisor :pet="pet" />
          </div>
        </transition>

        <transition name="fade-up" appear>
          <div class="content-section glass-card">
            <div class="section-header">
              <h2><i class="el-icon-bar-chart"></i> 健康数据统计</h2>
            </div>
            <HealthStatsChart :healthRecords="healthRecords" :vaccineRecords="vaccineRecords" />
          </div>
        </transition>

        <div class="chart-row">
          <transition name="fade-up" appear>
            <div class="content-section glass-card chart-card">
              <div class="section-header">
                <h2><i class="el-icon-trending-up"></i> 体重变化趋势</h2>
              </div>
              <WeightChart :data="weightData" />
              <p class="chart-source-note">图表根据平台健康档案绘制，包含参考记录，请以就诊凭证为准。</p>
            </div>
          </transition>
          <transition name="fade-up" appear>
            <div class="content-section glass-card chart-card">
              <div class="section-header">
                <h2><i class="el-icon-thermometer"></i> 体温变化趋势</h2>
              </div>
              <TemperatureChart :data="temperatureData" />
              <p class="chart-source-note">参考：MSD/Merck Veterinary Manual 犬猫正常直肠温度范围。</p>
            </div>
          </transition>
        </div>
        <div class="chart-row">
          <transition name="fade-up" appear>
            <div class="content-section glass-card chart-card">
              <div class="section-header">
                <h2><i class="el-icon-clock"></i> 疫苗接种时间线</h2>
              </div>
              <VaccineTimeline :data="vaccineRecords" />
              <p class="chart-source-note">参考：AAHA 犬疫苗指南、AAHA/AAFP 猫疫苗指南，接种日期请以疫苗本或医院凭证为准。</p>
            </div>
          </transition>
          <transition name="fade-up" appear>
            <div class="content-section glass-card chart-card">
              <div class="section-header">
                <h2><i class="el-icon-pie-chart"></i> 健康统计分析</h2>
              </div>
              <HealthStatsChart :healthRecords="healthRecords" :vaccineRecords="vaccineRecords" />
              <p class="chart-source-note">来源：健康检查和疫苗接种记录聚合统计，点击图表可查看单项数据。</p>
            </div>
          </transition>
        </div>

        <transition name="fade-up" appear>
          <div class="content-section glass-card" v-if="healthRecords.length > 0">
            <div class="section-header">
              <h2><i class="el-icon-heart-pulse"></i> 健康档案</h2>
              <span class="record-count">{{ healthRecords.length }}条记录</span>
            </div>
            <div class="record-list">
              <div class="record-item" v-for="record in healthRecords" :key="record.id">
                <div class="record-date">
                  <span class="date-day">{{ formatDay(record.recordDate) }}</span>
                  <span class="date-month">{{ formatMonth(record.recordDate) }}</span>
                </div>
                <div class="record-content">
                  <div class="record-header">
                    <span class="record-diagnosis" :class="record.diagnosis && record.diagnosis.includes('健康') ? 'healthy' : ''">
                      {{ record.diagnosis || '健康检查' }}
                    </span>
                    <span class="record-cost" v-if="record.cost">¥{{ record.cost }}</span>
                  </div>
                  <div class="record-details">
                    <span v-if="record.symptom" class="detail-item"><i class="el-icon-warning"></i> {{ record.symptom }}</span>
                    <span v-if="record.hospital" class="detail-item"><i class="el-icon-building"></i> {{ record.hospital }}</span>
                    <span v-if="record.veterinarian" class="detail-item"><i class="el-icon-user"></i> {{ record.veterinarian }}</span>
                  </div>
                  <div v-if="record.notes" class="record-notes">{{ record.notes }}</div>
                  <div v-if="record.sourceName" class="record-source">
                    数据来源：{{ record.sourceName }}
                    <a v-if="record.sourceUrl" :href="record.sourceUrl" target="_blank" rel="noreferrer">核验链接</a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>

        <transition name="fade-up" appear>
          <div class="content-section glass-card" v-if="vaccineRecords.length > 0">
            <div class="section-header">
              <h2><i class="el-icon-syringe"></i> 疫苗记录</h2>
              <span class="record-count">{{ vaccineRecords.length }}条记录</span>
            </div>
            <div class="record-list vaccine-list">
              <div class="record-item" v-for="record in vaccineRecords" :key="record.id">
                <div class="record-date">
                  <span class="date-day">{{ formatDay(record.vaccinateDate) }}</span>
                  <span class="date-month">{{ formatMonth(record.vaccinateDate) }}</span>
                </div>
                <div class="record-content">
                  <div class="record-header">
                    <span class="record-diagnosis">{{ record.vaccineName }}</span>
                  </div>
                  <div class="record-details">
                    <span v-if="record.vaccineType" class="detail-item">{{ record.vaccineType }}</span>
                    <span v-if="record.hospital" class="detail-item"><i class="el-icon-building"></i> {{ record.hospital }}</span>
                    <span v-if="record.doctor" class="detail-item"><i class="el-icon-user"></i> {{ record.doctor }}</span>
                  </div>
                  <div v-if="record.nextDate" class="next-vaccine">
                    <i class="el-icon-calendar"></i>
                    下次接种：{{ formatDate(record.nextDate) }}
                  </div>
                  <div v-if="record.sourceName" class="record-source">
                    数据来源：{{ record.sourceName }}
                    <a v-if="record.sourceUrl" :href="record.sourceUrl" target="_blank" rel="noreferrer">核验链接</a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>

        <transition name="fade-up" appear>
          <div class="content-section glass-card data-source-section">
            <div class="section-header">
              <h2><i class="el-icon-link"></i> 数据来源说明</h2>
            </div>
            <div class="data-source-list">
              <div class="source-item" v-if="pet.sourceName || pet.imageSourceName">
                <div class="source-icon">
                  <i class="el-icon-document-checked"></i>
                </div>
                <div class="source-info">
                  <h4>当前宠物档案</h4>
                  <p v-if="pet.sourceName">品种资料：<a v-if="pet.sourceUrl" :href="pet.sourceUrl" target="_blank" rel="noreferrer">{{ pet.sourceName }}</a><span v-else>{{ pet.sourceName }}</span></p>
                  <p v-if="pet.imageSourceName">图片来源：<a v-if="pet.imageSourceUrl" :href="pet.imageSourceUrl" target="_blank" rel="noreferrer">{{ pet.imageSourceName }}</a><span v-else>{{ pet.imageSourceName }}</span></p>
                  <p v-if="pet.sourceCheckedAt">最近核验：{{ pet.sourceCheckedAt }}</p>
                  <p class="source-desc">{{ pet.sourceNote || '宠物资料请以线下核验的救助记录和接种凭证为准。' }}</p>
                </div>
              </div>
              <div class="source-item">
                <div class="source-icon">
                  <i class="el-icon-picture"></i>
                </div>
                <div class="source-info">
                  <h4>图片参考来源</h4>
                  <p>来源：Dog CEO Dog API / The Cat API</p>
                  <p class="source-desc">图片用于品种展示与参考，宠物实际情况请以救助站提供的资料为准。</p>
                </div>
              </div>
              <div class="source-item">
                <div class="source-icon">
                  <i class="el-icon-heart-pulse"></i>
                </div>
                <div class="source-info">
                  <h4>犬种资料</h4>
                  <p>来源：<a href="https://raw.githubusercontent.com/tmfilho/akcdata/master/data/akc-data-latest.csv" target="_blank">AKC Breed Info CSV</a></p>
                  <p class="source-desc">犬种体重、寿命、性格与护理描述来自公开 AKC 数据集</p>
                </div>
              </div>
              <div class="source-item">
                <div class="source-icon">
                  <i class="el-icon-syringe"></i>
                </div>
                <div class="source-info">
                  <h4>猫种资料</h4>
                  <p>来源：<a href="https://api.thecatapi.com/v1/breeds" target="_blank">The Cat API</a></p>
                  <p class="source-desc">猫种体重、寿命、性格、来源地与图片参考 The Cat API。</p>
                </div>
              </div>
              <div class="source-item">
                <div class="source-icon">
                  <i class="el-icon-home"></i>
                </div>
                <div class="source-info">
                  <h4>照护咨询参考</h4>
                  <p>来源：<a href="/api/ai/sources" target="_blank">查看品种与照护参考资料</a></p>
                  <p class="source-desc">咨询结合品种资料与当前档案提供日常照护参考，不能替代兽医诊断。</p>
                </div>
              </div>
              <div class="source-item">
                <div class="source-icon">
                  <i class="el-icon-first-aid-kit"></i>
                </div>
                <div class="source-info">
                  <h4>健康/疫苗图表</h4>
                  <p>来源：<a href="https://www.msdvetmanual.com/multimedia/table/normal-rectal-temperature-ranges" target="_blank">MSD/Merck Veterinary Manual 体温表</a></p>
                  <p>来源：<a href="https://www.aaha.org/resources/2022-aaha-canine-vaccination-guidelines/" target="_blank">AAHA 2022 Canine Vaccination Guidelines</a></p>
                  <p>来源：<a href="https://www.aaha.org/resources/2020-aahaaafp-feline-vaccination-guidelines/" target="_blank">2020 AAHA/AAFP Feline Vaccination Guidelines</a></p>
                  <p class="source-desc">图表展示平台健康与疫苗档案中的参考记录，不代表真实临床记录；请核对就诊与接种凭证。</p>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <el-dialog v-model="showAdoptModal" title="领养申请" width="600px" class="adopt-dialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="住址" prop="address">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="工作状况" prop="workStatus">
          <el-select v-model="form.workStatus">
            <el-option label="在职" value="在职" />
            <el-option label="自由职业" value="自由职业" />
            <el-option label="退休" value="退休" />
            <el-option label="学生" value="学生" />
          </el-select>
        </el-form-item>
        <el-form-item label="住房类型" prop="housingType">
          <el-select v-model="form.housingType">
            <el-option label="自有住房" value="自有住房" />
            <el-option label="租房" value="租房" />
            <el-option label="合租" value="合租" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否有其他宠物">
          <el-switch v-model="form.hasOtherPet" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="养宠经验">
          <el-input v-model="form.experience" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="领养理由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdoptModal = false">取消</el-button>
        <el-button type="primary" @click="submitAdoption">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import WeightChart from '../components/WeightChart.vue'
import TemperatureChart from '../components/TemperatureChart.vue'
import VaccineTimeline from '../components/VaccineTimeline.vue'
import HealthStatsChart from '../components/HealthStatsChart.vue'
import AiAdvisor from '../components/AiAdvisor.vue'
import { getPetImageStyle, handlePetImageError, resolvePetImage } from '../utils/petImage'

const route = useRoute()
const router = useRouter()
const pet = ref(null)
const healthRecords = ref([])
const vaccineRecords = ref([])
const loading = ref(true)
const loadError = ref(false)
const showAdoptModal = ref(false)
const formRef = ref(null)

const form = reactive({
  realName: '',
  phone: '',
  address: '',
  workStatus: '',
  housingType: '',
  hasOtherPet: 0,
  experience: '',
  reason: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入住址', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入领养理由', trigger: 'blur' }]
}

const weightData = computed(() => {
  return healthRecords.value.filter(r => r.weight).sort((a, b) => new Date(a.recordDate) - new Date(b.recordDate))
})

const temperatureData = computed(() => {
  return healthRecords.value.filter(r => r.temperature).sort((a, b) => new Date(a.recordDate) - new Date(b.recordDate))
})

onMounted(() => {
  loadPet()
})

const loadPet = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await request.get(`/pets/${route.params.id}`)
    if (res.code !== 200 || !res.data) throw new Error(res.message || '宠物不存在')
    pet.value = res.data
    await Promise.allSettled([loadHealthRecords(), loadVaccineRecords()])
  } catch (error) {
    pet.value = null
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const loadHealthRecords = async () => {
  try {
    const res = await request.get(`/health/pet/${pet.value.id}`)
    if (res.code === 200) healthRecords.value = res.data || []
  } catch (error) {
    healthRecords.value = []
  }
}

const loadVaccineRecords = async () => {
  try {
    const res = await request.get(`/vaccine/pet/${pet.value.id}`)
    if (res.code === 200) {
      vaccineRecords.value = (res.data || []).sort((a, b) => new Date(a.vaccinateDate) - new Date(b.vaccinateDate))
    }
  } catch (error) {
    vaccineRecords.value = []
  }
}

const statusText = (status) => {
  const map = { 0: '待领养', 1: '已领养', 2: '已下架' }
  return map[status] || '未知'
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const formatDay = (date) => {
  if (!date) return ''
  return new Date(date).getDate()
}

const formatMonth = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getMonth() + 1}月`
}

const resolveImageUrl = resolvePetImage
const imageStyle = getPetImageStyle
const handleImageError = handlePetImageError

const handleAdoptClick = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再申请领养')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  showAdoptModal.value = true
}

const submitAdoption = async () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (valid) {
      const res = await request.post('/adoptions', { ...form, petId: pet.value.id })
      if (res.code === 200) {
        ElMessage.success(res.message)
        showAdoptModal.value = false
      } else {
        ElMessage.error(res.message)
      }
    }
  })
}
</script>

<style scoped>
.pet-detail {
  min-height: 100vh;
  position: relative;
  padding-bottom: 60px;
  overflow-x: hidden;
}

.decor-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 50%, #f0f2f8 100%);
  z-index: -1;
}

.decor-pet {
  position: fixed;
  width: 80px;
  height: 80px;
  opacity: 0.15;
  z-index: 0;
  pointer-events: none;
  object-fit: contain;
}

.pet-1 {
  top: 10%;
  left: 5%;
  animation: float 6s ease-in-out infinite;
}

.pet-2 {
  top: 30%;
  right: 8%;
  animation: float 8s ease-in-out infinite reverse;
}

.pet-3 {
  bottom: 20%;
  left: 10%;
  animation: float 7s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
  z-index: 1;
}

.glass-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.pet-header {
  display: flex;
  gap: 40px;
  margin-bottom: 30px;
  padding: 30px;
}

.pet-main-image {
  position: relative;
  width: 400px;
  height: 400px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.pet-main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.pet-main-image:hover img {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  top: 16px;
  left: 16px;
}

.image-tag {
  background: rgba(255, 255, 255, 0.9);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: bold;
  color: #667eea;
}

.pet-basic-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.pet-basic-info h1 {
  font-size: 36px;
  font-weight: bold;
  color: #1a1a2e;
  margin: 0;
}

.breed-tag {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.pet-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.tag {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  padding: 6px 14px;
  border-radius: 12px;
  font-size: 13px;
}

.tag.gender-tag.male {
  background: rgba(67, 156, 255, 0.15);
  color: #439cff;
}

.tag.gender-tag.female {
  background: rgba(245, 92, 150, 0.15);
  color: #f55c96;
}

.pet-status-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 12px;
  font-size: 14px;
}

.status-badge.health.good {
  background: rgba(71, 233, 123, 0.15);
  color: #47e97b;
}

.status-badge.health.warning {
  background: rgba(255, 193, 7, 0.15);
  color: #ffc107;
}

.status-badge.adopt.pending {
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
}

.status-badge.adopt.adopted {
  background: rgba(103, 194, 58, 0.15);
  color: #67c23a;
}

.pet-fee {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 12px;
}

.pet-fee span:first-child {
  font-size: 16px;
  color: #666;
}

.fee-amount {
  font-size: 36px;
  font-weight: bold;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.fee-unit {
  font-size: 16px;
  color: #666;
}

.pet-shelter {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
  margin-bottom: 24px;
}

.adopt-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: #fff;
  padding: 16px 40px;
  font-size: 16px;
  font-weight: bold;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.adopt-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

.adopt-btn:disabled {
  background: #e0e0e0;
  cursor: not-allowed;
  box-shadow: none;
}

.pet-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.content-section {
  padding: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-header h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
  color: #1a1a2e;
  margin: 0;
}

.section-header h2 i {
  color: #667eea;
}

.record-count {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.content-section p {
  font-size: 15px;
  color: #333;
  line-height: 1.8;
  margin: 0;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.chart-card {
  min-height: 300px;
}

.detail-state {
  min-height: 360px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  color: #667085;
  font-size: 15px;
}

.detail-state .el-icon {
  color: #2d9f86;
  font-size: 30px;
}

.detail-error {
  width: min(680px, calc(100% - 40px));
  margin: 60px auto;
}

.chart-source-note {
  margin-top: 10px !important;
  color: #6f7f79 !important;
  font-size: 12px !important;
  line-height: 1.6 !important;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.record-item:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateX(4px);
}

.record-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 60px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  border-radius: 10px;
  padding: 10px 0;
}

.date-day {
  font-size: 20px;
  font-weight: bold;
  color: #667eea;
}

.date-month {
  font-size: 11px;
  color: #666;
}

.record-content {
  flex: 1;
}

.record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.record-diagnosis {
  font-size: 15px;
  font-weight: bold;
  color: #333;
}

.record-diagnosis.healthy {
  color: #47e97b;
}

.record-cost {
  font-size: 14px;
  color: #f5576c;
  font-weight: bold;
}

.record-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.record-notes {
  font-size: 13px;
  color: #666;
  padding-top: 8px;
  border-top: 1px dashed #e8e8e8;
}

.record-source {
  margin-top: 8px;
  color: #8a9691;
  font-size: 11px;
  line-height: 1.6;
}

.record-source a {
  margin-left: 8px;
  color: #2d9f86;
  text-decoration: none;
}

.next-vaccine {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #667eea;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e8e8e8;
}

.fade-up-enter-active {
  transition: all 0.6s ease-out;
}

.fade-up-leave-active {
  transition: all 0.3s ease-in;
}

.fade-up-enter-from {
  opacity: 0;
  transform: translateY(30px);
}

.fade-up-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.adopt-dialog {
  .el-dialog__header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px 16px 0 0;
  }
  .el-dialog__title {
    color: #fff;
  }
  .el-dialog__headerbtn .el-dialog__close {
    color: #fff;
  }
}

.data-source-section {
  background: rgba(102, 126, 234, 0.05);
}

.data-source-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.source-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.source-item:hover {
  background: rgba(255, 255, 255, 1);
  transform: translateY(-2px);
}

.source-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
}

.source-info h4 {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin: 0 0 4px 0;
}

.source-info p {
  font-size: 12px;
  color: #667eea;
  margin: 0 0 4px 0;
}

.source-info p a {
  color: #667eea;
  text-decoration: none;
}

.source-info p a:hover {
  text-decoration: underline;
}

.source-desc {
  color: #666 !important;
}

@media (max-width: 768px) {
  .pet-header {
    flex-direction: column;
    gap: 24px;
  }
  .pet-main-image {
    width: 100%;
    height: 280px;
  }
  .chart-row {
    grid-template-columns: 1fr;
  }
  .data-source-list {
    grid-template-columns: 1fr;
  }
}
</style>
