<template>
  <div class="care-plan-page">
    <section class="page-hero">
      <div>
        <span class="eyebrow">创新点 2</span>
        <h1>个性化照护计划</h1>
        <p>选择一只宠物后，系统会结合品种资料、年龄、健康记录和疫苗复种日期，生成可执行的照护任务。</p>
      </div>
      <div class="calendar-mascot">
        <strong>{{ completionRate }}%</strong>
        <span>完成度</span>
      </div>
    </section>

    <section class="layout">
      <aside class="pet-selector">
        <h2>选择宠物</h2>
        <div
          v-for="pet in pets"
          :key="pet.id"
          :class="['pet-option', { active: selectedPetId === pet.id }]"
          @click="selectPet(pet.id)"
        >
          <img :src="resolveImage(pet)" :style="imageStyle(pet)" :alt="pet.name" @error="handleImageError($event, pet)" />
          <div>
            <strong>{{ pet.name }}</strong>
            <span>{{ pet.breed }}</span>
          </div>
        </div>
      </aside>

      <main class="plan-main" v-if="selectedPet">
        <section class="pet-profile">
              <img :src="resolveImage(selectedPet)" :style="imageStyle(selectedPet)" :alt="selectedPet.name" @error="handleImageError($event, selectedPet)" />
          <div>
            <span class="pet-type">{{ selectedPet.type }}</span>
            <h2>{{ selectedPet.name }} 的照护计划</h2>
            <p>{{ selectedPet.breed }} · {{ selectedPet.age }}个月 · {{ selectedPet.weight }}kg · {{ selectedPet.healthStatus }}</p>
          </div>
        </section>

        <section class="plan-overview">
          <div class="overview-card">
            <span>健康记录</span>
            <strong>{{ healthRecords.length }}</strong>
          </div>
          <div class="overview-card">
            <span>疫苗记录</span>
            <strong>{{ vaccineRecords.length }}</strong>
          </div>
          <div class="overview-card">
            <span>待办任务</span>
            <strong>{{ tasks.length }}</strong>
          </div>
          <div class="overview-card">
            <span>已完成</span>
            <strong>{{ doneCount }}</strong>
          </div>
        </section>

        <section class="task-board">
          <div class="board-header">
            <div>
              <h2>本阶段任务</h2>
              <p>任务会根据真实健康和疫苗数据自动更新，完成状态保存在本机。</p>
            </div>
            <el-button @click="resetTasks">重置勾选</el-button>
          </div>

          <div class="task-columns">
            <article v-for="group in groupedTasks" :key="group.key" class="task-column">
              <h3>{{ group.title }}</h3>
              <div
                v-for="task in group.tasks"
                :key="task.id"
                :class="['task-item', task.priority, { done: doneMap[task.id] }]"
              >
                <el-checkbox :model-value="!!doneMap[task.id]" @change="toggleTask(task.id)">
                  <strong>{{ task.title }}</strong>
                </el-checkbox>
                <p>{{ task.desc }}</p>
                <span>{{ task.dateLabel }}</span>
              </div>
            </article>
          </div>
        </section>

        <section class="advice-panel">
          <div>
            <h2>照护建议</h2>
            <p>{{ breedAdvice }}</p>
          </div>
          <div class="mini-mascots">
            <span class="mini-dog">dog</span>
            <span class="mini-cat">cat</span>
          </div>
        </section>
      </main>
    </section>

    <section class="source-section">
      <h2>数据来源说明</h2>
      <p>任务中的体温区间来自 MSD/Merck Veterinary Manual；犬猫疫苗复种提醒参考 AAHA 犬疫苗指南与 AAHA/AAFP 猫疫苗指南；品种描述和体重资料来自已爬取的 AKC、Dog CEO、The Cat API 数据，并保存在项目数据库中。</p>
      <div class="source-links">
        <a href="https://raw.githubusercontent.com/tmfilho/akcdata/master/data/akc-data-latest.csv" target="_blank">AKC Breed Info CSV</a>
        <a href="https://dog.ceo/dog-api/" target="_blank">Dog CEO Dog API</a>
        <a href="https://api.thecatapi.com/v1/breeds" target="_blank">The Cat API</a>
        <a href="https://www.msdvetmanual.com/multimedia/table/normal-rectal-temperature-ranges" target="_blank">MSD/Merck Veterinary Manual</a>
        <a href="https://www.aaha.org/resources/2022-aaha-canine-vaccination-guidelines/" target="_blank">AAHA 犬疫苗指南</a>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getPetImageStyle, handlePetImageError, resolvePetImage } from '../utils/petImage'

const pets = ref([])
const selectedPetId = ref(null)
const healthRecords = ref([])
const vaccineRecords = ref([])
const doneMap = reactive({})

const selectedPet = computed(() => pets.value.find(pet => pet.id === selectedPetId.value))

const tasks = computed(() => {
  if (!selectedPet.value) return []
  const today = new Date()
  const rows = []
  const latestHealth = [...healthRecords.value].sort((a, b) => new Date(b.recordDate) - new Date(a.recordDate))[0]
  const latestTemp = latestHealth?.temperature ? Number.parseFloat(latestHealth.temperature) : null
  const latestWeight = latestHealth?.weight ? Number.parseFloat(latestHealth.weight) : Number.parseFloat(selectedPet.value.weight)

  rows.push({
    id: `weekly-check-${selectedPet.value.id}`,
    group: 'daily',
    priority: latestTemp && (latestTemp < 37.5 || latestTemp > 39.2) ? 'high' : 'normal',
    title: '每周体温与精神状态检查',
    desc: latestTemp ? `最近体温 ${latestTemp}℃，参考区间 37.5-39.2℃。` : '暂无体温记录，建议补充一次基础体温。',
    dateLabel: '每周 1 次'
  })

  rows.push({
    id: `weight-${selectedPet.value.id}`,
    group: 'daily',
    priority: 'normal',
    title: '体重和食欲记录',
    desc: `当前记录体重约 ${Number.isFinite(latestWeight) ? latestWeight.toFixed(1) : selectedPet.value.weight}kg，建议持续观察食欲和饮水。`,
    dateLabel: '每周 1 次'
  })

  vaccineRecords.value.forEach(record => {
    if (!record.nextDate) return
    const days = Math.ceil((new Date(record.nextDate) - today) / 86400000)
    rows.push({
      id: `vaccine-${record.id}`,
      group: 'vaccine',
      priority: days < 0 ? 'high' : days <= 60 ? 'medium' : 'normal',
      title: `${record.vaccineName}复种提醒`,
      desc: days < 0 ? `已超过建议复种日期 ${Math.abs(days)} 天。` : `${days} 天后到达下一次复种日期。`,
      dateLabel: formatDate(record.nextDate)
    })
  })

  rows.push({
    id: `breed-care-${selectedPet.value.id}`,
    group: 'monthly',
    priority: selectedPet.value.healthStatus === 'healthy' ? 'normal' : 'medium',
    title: `${selectedPet.value.breed}品种照护复盘`,
    desc: '结合品种体型、年龄和当前健康状态，复查毛发、运动量、排泄和行为变化。',
    dateLabel: '每月 1 次'
  })

  rows.push({
    id: `adoption-ready-${selectedPet.value.id}`,
    group: 'monthly',
    priority: 'normal',
    title: '领养适配观察',
    desc: '记录与人互动、对笼舍或家庭环境反应、是否适合儿童或多宠家庭。',
    dateLabel: '领养前持续'
  })

  return rows
})

const groupedTasks = computed(() => {
  const groups = [
    { key: 'daily', title: '日常健康', tasks: [] },
    { key: 'vaccine', title: '疫苗提醒', tasks: [] },
    { key: 'monthly', title: '阶段复盘', tasks: [] }
  ]
  tasks.value.forEach(task => {
    const group = groups.find(item => item.key === task.group)
    group?.tasks.push(task)
  })
  return groups.filter(group => group.tasks.length)
})

const doneCount = computed(() => tasks.value.filter(task => doneMap[task.id]).length)
const completionRate = computed(() => tasks.value.length ? Math.round((doneCount.value / tasks.value.length) * 100) : 0)

const breedAdvice = computed(() => {
  if (!selectedPet.value) return ''
  const desc = selectedPet.value.description || ''
  const shortDesc = desc.length > 140 ? `${desc.slice(0, 140)}...` : desc
  return shortDesc || '建议根据宠物年龄、体重、运动能力和疫苗状态制定照护计划。'
})

onMounted(async () => {
  await loadPets()
})

watch(selectedPetId, async (id) => {
  if (!id) return
  await loadRecords(id)
  loadDoneState()
})

const loadPets = async () => {
  const res = await request.get('/pets', { params: { page: 1, size: 50 } })
  if (res.code === 200) {
    pets.value = res.data.records || []
    selectedPetId.value = pets.value[0]?.id || null
  }
}

const loadRecords = async (petId) => {
  const [healthRes, vaccineRes] = await Promise.all([
    request.get(`/health/pet/${petId}`).catch(() => ({ data: [] })),
    request.get(`/vaccine/pet/${petId}`).catch(() => ({ data: [] }))
  ])
  healthRecords.value = healthRes.data || []
  vaccineRecords.value = vaccineRes.data || []
}

const selectPet = (id) => {
  selectedPetId.value = id
}

const storageKey = computed(() => `care-plan-done-${selectedPetId.value || 'none'}`)

const loadDoneState = () => {
  Object.keys(doneMap).forEach(key => delete doneMap[key])
  const stored = localStorage.getItem(storageKey.value)
  if (!stored) return
  try {
    Object.assign(doneMap, JSON.parse(stored))
  } catch {
    localStorage.removeItem(storageKey.value)
  }
}

const saveDoneState = () => {
  localStorage.setItem(storageKey.value, JSON.stringify(doneMap))
}

const toggleTask = (taskId) => {
  doneMap[taskId] = !doneMap[taskId]
  saveDoneState()
}

const resetTasks = () => {
  Object.keys(doneMap).forEach(key => delete doneMap[key])
  saveDoneState()
  ElMessage.success('已重置照护计划勾选状态')
}

const resolveImage = resolvePetImage
const imageStyle = getPetImageStyle
const handleImageError = handlePetImageError

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.care-plan-page {
  min-height: 100vh;
  padding: 28px 20px 70px;
  color: #243b35;
  background:
    radial-gradient(circle at 10% 12%, rgba(255, 159, 90, 0.18), transparent 24%),
    radial-gradient(circle at 90% 8%, rgba(45, 159, 134, 0.16), transparent 24%),
    #f7faf8;
}

.page-hero,
.layout,
.source-section {
  width: min(1180px, 100%);
  margin: 0 auto;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 34px;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, #ff9f5a, #2d9f86 58%, #6c8df6);
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
  max-width: 660px;
  margin: 0;
  color: rgba(255,255,255,0.9);
  line-height: 1.8;
}

.calendar-mascot {
  width: 130px;
  height: 130px;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 8px;
  background: rgba(255,255,255,0.18);
  flex-shrink: 0;
}

.calendar-mascot strong {
  font-size: 34px;
}

.calendar-mascot span {
  font-size: 13px;
  color: rgba(255,255,255,0.84);
}

.layout {
  display: grid;
  grid-template-columns: 270px 1fr;
  gap: 18px;
  margin-top: 18px;
}

.pet-selector,
.pet-profile,
.overview-card,
.task-board,
.task-column,
.advice-panel,
.source-section {
  border-radius: 8px;
  background: rgba(255,255,255,0.94);
  border: 1px solid rgba(45,159,134,0.12);
  box-shadow: 0 16px 42px rgba(38,59,53,0.08);
}

.pet-selector {
  padding: 18px;
  max-height: 760px;
  overflow: auto;
}

.pet-selector h2,
.task-board h2,
.source-section h2 {
  margin: 0 0 16px;
}

.pet-option {
  display: grid;
  grid-template-columns: 52px 1fr;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
}

.pet-option:hover,
.pet-option.active {
  background: #f1f8f5;
  border-color: rgba(45,159,134,0.25);
}

.pet-option img {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  object-fit: cover;
}

.pet-option strong,
.pet-option span {
  display: block;
}

.pet-option span {
  color: #65746f;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-main {
  display: grid;
  gap: 18px;
}

.pet-profile {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 18px;
  align-items: center;
  padding: 18px;
}

.pet-profile img {
  width: 160px;
  height: 130px;
  object-fit: cover;
  border-radius: 8px;
}

.pet-type {
  display: inline-flex;
  color: #fff;
  background: #2d9f86;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  margin-bottom: 8px;
}

.pet-profile h2 {
  margin: 0 0 8px;
  font-size: 30px;
}

.pet-profile p {
  color: #65746f;
  margin: 0;
}

.plan-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.overview-card {
  padding: 18px;
}

.overview-card span {
  color: #65746f;
}

.overview-card strong {
  display: block;
  margin-top: 8px;
  font-size: 32px;
  color: #2d9f86;
}

.task-board,
.source-section,
.advice-panel {
  padding: 22px;
}

.board-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 18px;
}

.board-header p,
.source-section p,
.advice-panel p {
  margin: 0;
  color: #65746f;
  line-height: 1.8;
}

.task-columns {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.task-column {
  padding: 16px;
  box-shadow: none;
}

.task-column h3 {
  margin: 0 0 12px;
}

.task-item {
  border-radius: 8px;
  padding: 12px;
  background: #f8fbfa;
  border-left: 4px solid #2d9f86;
  margin-bottom: 10px;
}

.task-item.medium {
  border-left-color: #ff9f5a;
  background: #fff8ee;
}

.task-item.high {
  border-left-color: #e86f51;
  background: #fff1ee;
}

.task-item.done {
  opacity: 0.62;
}

.task-item p {
  color: #65746f;
  line-height: 1.6;
  margin: 8px 0;
  font-size: 13px;
}

.task-item span {
  color: #2d9f86;
  font-size: 12px;
  font-weight: 700;
}

.advice-panel {
  display: grid;
  grid-template-columns: 1fr 210px;
  gap: 20px;
  align-items: center;
}

.advice-panel h2 {
  margin: 0 0 10px;
}

.mini-mascots {
  position: relative;
  height: 150px;
}

.mini-mascots span {
  position: absolute;
  display: grid;
  place-items: center;
  border-radius: 999px;
  color: rgba(36,59,53,0.48);
  font-weight: 900;
  letter-spacing: 0;
}

.mini-dog {
  width: 118px;
  height: 118px;
  left: 0;
  top: 12px;
  background: #ffe2c3;
}

.mini-cat {
  width: 96px;
  height: 96px;
  right: 0;
  bottom: 8px;
  background: #d8ecff;
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
  background: rgba(45,159,134,0.1);
  font-size: 13px;
}

@media (max-width: 1020px) {
  .layout,
  .task-columns,
  .plan-overview,
  .advice-panel {
    grid-template-columns: 1fr;
  }

  .pet-selector {
    max-height: none;
  }
}

@media (max-width: 640px) {
  .page-hero,
  .board-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .pet-profile {
    grid-template-columns: 1fr;
  }

  .pet-profile img {
    width: 100%;
    height: 220px;
  }
}
</style>
