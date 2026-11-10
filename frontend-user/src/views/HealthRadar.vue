<template>
  <div class="health-radar-page">
    <section class="page-hero">
      <div>
        <span class="eyebrow">创新点 1</span>
        <h1>健康风险雷达</h1>
        <p>基于数据库中的体温、体重、疫苗复种日期和健康状态，自动筛出需要重点关注的宠物。</p>
      </div>
      <div class="hero-mascot">
        <span class="ring"></span>
        <strong>HR</strong>
      </div>
    </section>

    <section class="summary-grid">
      <div class="summary-card high">
        <span>高风险</span>
        <strong>{{ levelCounts.high }}</strong>
      </div>
      <div class="summary-card medium">
        <span>中风险</span>
        <strong>{{ levelCounts.medium }}</strong>
      </div>
      <div class="summary-card low">
        <span>低风险</span>
        <strong>{{ levelCounts.low }}</strong>
      </div>
      <div class="summary-card source">
        <span>已分析记录</span>
        <strong>{{ totalRecords }}</strong>
      </div>
    </section>

    <section class="workspace">
      <aside class="filter-panel">
        <h2>筛选</h2>
        <el-radio-group v-model="filters.level" class="radio-column">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="high">高风险</el-radio-button>
          <el-radio-button label="medium">中风险</el-radio-button>
          <el-radio-button label="low">低风险</el-radio-button>
        </el-radio-group>

        <el-select v-model="filters.type" placeholder="宠物类型" clearable class="wide-control">
          <el-option label="犬 Dog" value="Dog" />
          <el-option label="猫 Cat" value="Cat" />
        </el-select>

        <div class="rule-box">
          <h3>判定规则</h3>
          <p>体温超出 37.5-39.2℃、体重波动超过 15%、疫苗逾期或 60 天内到期时，会提高风险分。</p>
        </div>
      </aside>

      <main class="radar-main">
        <div class="chart-panel">
          <div class="panel-header">
            <div>
              <h2>风险维度概览</h2>
              <p>点击下方宠物卡片可切换图表</p>
            </div>
            <el-button type="primary" @click="refreshData" :loading="loading">刷新数据</el-button>
          </div>
          <div ref="chartRef" class="radar-chart"></div>
        </div>

        <div class="pet-risk-grid">
          <article
            v-for="item in filteredItems"
            :key="item.pet.id"
            :class="['risk-card', item.level, { active: selectedId === item.pet.id }]"
            @click="selectItem(item)"
          >
            <img :src="resolveImage(item.pet)" :style="imageStyle(item.pet)" :alt="item.pet.name" @error="handleImageError($event, item.pet)" />
            <div class="risk-content">
              <div class="risk-title">
                <h3>{{ item.pet.name }}</h3>
                <span>{{ levelText(item.level) }}</span>
              </div>
              <p>{{ item.pet.breed }} · {{ item.pet.age }}个月 · {{ item.pet.weight }}kg</p>
              <div class="risk-meter">
                <i :style="{ width: `${Math.min(item.score, 100)}%` }"></i>
              </div>
              <ul>
                <li v-for="tip in item.tips.slice(0, 3)" :key="tip">{{ tip }}</li>
              </ul>
            </div>
          </article>
        </div>
      </main>
    </section>

    <section class="source-section">
      <h2>数据来源说明</h2>
      <p>宠物基础资料来自项目数据库 pet 表，图片和品种资料由 Dog CEO、The Cat API、AKC 数据集爬取生成；健康记录来自 health_record 表，疫苗记录来自 vaccine_record 表。</p>
      <div class="source-links">
        <a href="https://www.msdvetmanual.com/multimedia/table/normal-rectal-temperature-ranges" target="_blank">MSD/Merck Veterinary Manual 体温范围</a>
        <a href="https://www.aaha.org/resources/2022-aaha-canine-vaccination-guidelines/" target="_blank">AAHA 犬疫苗指南</a>
        <a href="https://www.aaha.org/resources/2020-aahaaafp-feline-vaccination-guidelines/" target="_blank">AAHA/AAFP 猫疫苗指南</a>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import request from '../utils/request'
import { getPetImageStyle, handlePetImageError, resolvePetImage } from '../utils/petImage'

const loading = ref(false)
const items = ref([])
const selectedId = ref(null)
const chartRef = ref(null)
let chartInstance = null

const filters = reactive({
  level: 'all',
  type: ''
})

const filteredItems = computed(() => {
  return items.value.filter(item => {
    const levelOk = filters.level === 'all' || item.level === filters.level
    const typeOk = !filters.type || item.pet.type === filters.type
    return levelOk && typeOk
  })
})

const selectedItem = computed(() => {
  return items.value.find(item => item.pet.id === selectedId.value) || filteredItems.value[0] || items.value[0]
})

const levelCounts = computed(() => ({
  high: items.value.filter(item => item.level === 'high').length,
  medium: items.value.filter(item => item.level === 'medium').length,
  low: items.value.filter(item => item.level === 'low').length
}))

const totalRecords = computed(() => {
  return items.value.reduce((sum, item) => sum + item.healthRecords.length + item.vaccineRecords.length, 0)
})

onMounted(() => {
  refreshData()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
})

watch(selectedItem, () => {
  nextTick(renderChart)
})

const refreshData = async () => {
  loading.value = true
  try {
    const petRes = await request.get('/pets', { params: { page: 1, size: 50 } })
    const pets = petRes.code === 200 ? petRes.data.records || [] : []
    const rows = await Promise.all(pets.map(async pet => {
      const [healthRes, vaccineRes] = await Promise.all([
        request.get(`/health/pet/${pet.id}`).catch(() => ({ data: [] })),
        request.get(`/vaccine/pet/${pet.id}`).catch(() => ({ data: [] }))
      ])
      return buildRiskItem(pet, healthRes.data || [], vaccineRes.data || [])
    }))
    items.value = rows.sort((a, b) => b.score - a.score)
    selectedId.value = items.value[0]?.pet.id || null
    nextTick(renderChart)
  } finally {
    loading.value = false
  }
}

const buildRiskItem = (pet, healthRecords, vaccineRecords) => {
  const sortedHealth = [...healthRecords].sort((a, b) => new Date(a.recordDate) - new Date(b.recordDate))
  const temps = sortedHealth.map(row => Number.parseFloat(row.temperature)).filter(Number.isFinite)
  const weights = sortedHealth.map(row => Number.parseFloat(row.weight)).filter(Number.isFinite)
  const abnormalTemp = temps.filter(temp => temp < 37.5 || temp > 39.2).length
  const weightChange = weights.length > 1 ? Math.abs(weights[weights.length - 1] - weights[0]) / Math.max(weights[0], 1) : 0
  const today = new Date()
  const overdueVaccines = vaccineRecords.filter(row => row.nextDate && new Date(row.nextDate) < today).length
  const upcomingVaccines = vaccineRecords.filter(row => {
    if (!row.nextDate) return false
    const days = (new Date(row.nextDate) - today) / 86400000
    return days >= 0 && days <= 60
  }).length
  const recovering = pet.healthStatus !== 'healthy'

  let score = 0
  score += abnormalTemp * 24
  score += weightChange > 0.15 ? 24 : weightChange > 0.08 ? 12 : 0
  score += overdueVaccines * 32
  score += upcomingVaccines * 12
  score += recovering ? 18 : 0

  const tips = []
  if (abnormalTemp) tips.push(`发现 ${abnormalTemp} 次体温超出参考区间`)
  if (weightChange > 0.15) tips.push(`体重波动 ${(weightChange * 100).toFixed(1)}%，建议复查`)
  if (overdueVaccines) tips.push(`${overdueVaccines} 项疫苗已超过复种日期`)
  if (upcomingVaccines) tips.push(`${upcomingVaccines} 项疫苗 60 天内需要复种`)
  if (recovering) tips.push('当前健康状态不是 healthy，需要继续观察')
  if (!tips.length) tips.push('当前记录稳定，保持日常观察')

  return {
    pet,
    healthRecords,
    vaccineRecords,
    score: Math.min(Math.round(score), 100),
    level: score >= 60 ? 'high' : score >= 30 ? 'medium' : 'low',
    metrics: {
      temperature: Math.min(abnormalTemp * 30, 100),
      weight: Math.min(Math.round(weightChange * 220), 100),
      vaccine: Math.min(overdueVaccines * 45 + upcomingVaccines * 18, 100),
      status: recovering ? 70 : 15,
      data: Math.min((healthRecords.length + vaccineRecords.length) * 8, 100)
    },
    tips
  }
}

const selectItem = (item) => {
  selectedId.value = item.pet.id
}

const renderChart = () => {
  if (!chartRef.value || !selectedItem.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const item = selectedItem.value
  chartInstance.setOption({
    tooltip: {},
    radar: {
      radius: '66%',
      indicator: [
        { name: '体温', max: 100 },
        { name: '体重', max: 100 },
        { name: '疫苗', max: 100 },
        { name: '状态', max: 100 },
        { name: '数据量', max: 100 }
      ],
      axisName: { color: '#52645f' },
      splitLine: { lineStyle: { color: '#dce6e1' } },
      splitArea: { areaStyle: { color: ['rgba(45,159,134,0.04)', 'rgba(255,159,90,0.05)'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        name: item.pet.name,
        value: [
          item.metrics.temperature,
          item.metrics.weight,
          item.metrics.vaccine,
          item.metrics.status,
          item.metrics.data
        ],
        areaStyle: { color: 'rgba(45,159,134,0.25)' },
        lineStyle: { color: '#2d9f86', width: 3 },
        itemStyle: { color: '#ff9f5a' }
      }]
    }]
  }, true)
}

const resizeChart = () => {
  chartInstance?.resize()
}

const levelText = (level) => {
  return { high: '高风险', medium: '中风险', low: '低风险' }[level] || '未知'
}

const resolveImage = resolvePetImage
const imageStyle = getPetImageStyle
const handleImageError = handlePetImageError
</script>

<style scoped>
.health-radar-page {
  min-height: 100vh;
  padding: 28px 20px 70px;
  color: #243b35;
  background:
    radial-gradient(circle at 8% 10%, rgba(255, 159, 90, 0.18), transparent 24%),
    radial-gradient(circle at 88% 6%, rgba(108, 141, 246, 0.16), transparent 26%),
    #f7faf8;
}

.page-hero,
.summary-grid,
.workspace,
.source-section {
  width: min(1180px, 100%);
  margin: 0 auto;
}

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2d9f86, #6c8df6);
  color: #fff;
}

.eyebrow {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255,255,255,0.2);
  font-size: 13px;
  font-weight: 800;
}

.page-hero h1 {
  font-size: 42px;
  margin: 14px 0 10px;
}

.page-hero p {
  max-width: 650px;
  margin: 0;
  color: rgba(255,255,255,0.88);
  line-height: 1.8;
}

.hero-mascot {
  position: relative;
  width: 130px;
  height: 130px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255,255,255,0.18);
  flex-shrink: 0;
}

.ring {
  position: absolute;
  inset: 22px;
  border-radius: 50%;
  border: 3px dashed rgba(255,255,255,0.75);
}

.hero-mascot strong {
  font-size: 28px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 18px;
}

.summary-card,
.filter-panel,
.chart-panel,
.risk-card,
.source-section {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(45, 159, 134, 0.12);
  box-shadow: 0 16px 42px rgba(38, 59, 53, 0.08);
}

.summary-card {
  padding: 22px;
}

.summary-card span {
  color: #65746f;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 38px;
}

.summary-card.high strong { color: #e86f51; }
.summary-card.medium strong { color: #ff9f5a; }
.summary-card.low strong { color: #2d9f86; }
.summary-card.source strong { color: #6c8df6; }

.workspace {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 18px;
  margin-top: 18px;
}

.filter-panel,
.chart-panel,
.source-section {
  padding: 22px;
}

.filter-panel h2,
.chart-panel h2,
.source-section h2 {
  margin: 0 0 16px;
}

.radio-column {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
}

.wide-control {
  width: 100%;
}

.rule-box {
  margin-top: 18px;
  padding: 16px;
  border-radius: 8px;
  background: #f1f8f5;
}

.rule-box h3 {
  margin: 0 0 8px;
}

.rule-box p,
.source-section p {
  margin: 0;
  color: #65746f;
  line-height: 1.7;
}

.radar-main {
  display: grid;
  gap: 18px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.panel-header p {
  margin: 0;
  color: #65746f;
}

.radar-chart {
  height: 360px;
}

.pet-risk-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.risk-card {
  display: grid;
  grid-template-columns: 132px 1fr;
  gap: 16px;
  padding: 14px;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.risk-card:hover,
.risk-card.active {
  transform: translateY(-4px);
  border-color: #2d9f86;
}

.risk-card img {
  width: 132px;
  height: 132px;
  border-radius: 8px;
  object-fit: cover;
}

.risk-title {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.risk-title h3 {
  margin: 0;
}

.risk-title span {
  color: #fff;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
}

.risk-card.high .risk-title span { background: #e86f51; }
.risk-card.medium .risk-title span { background: #ff9f5a; }
.risk-card.low .risk-title span { background: #2d9f86; }

.risk-content p {
  color: #65746f;
  margin: 8px 0;
}

.risk-meter {
  height: 8px;
  border-radius: 999px;
  background: #edf3f1;
  overflow: hidden;
  margin: 12px 0;
}

.risk-meter i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2d9f86, #ff9f5a, #e86f51);
}

.risk-content ul {
  margin: 0;
  padding-left: 18px;
  color: #52645f;
  line-height: 1.7;
  font-size: 13px;
}

.source-section {
  margin-top: 18px;
}

.source-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.source-links a {
  color: #2d9f86;
  text-decoration: none;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(45, 159, 134, 0.1);
  font-size: 13px;
}

@media (max-width: 980px) {
  .workspace,
  .pet-risk-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .page-hero {
    align-items: flex-start;
  }
}

@media (max-width: 620px) {
  .page-hero {
    flex-direction: column;
  }

  .risk-card {
    grid-template-columns: 1fr;
  }

  .risk-card img {
    width: 100%;
  }
}
</style>
