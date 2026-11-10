<template>
  <div class="home">
    <section class="hero">
      <div class="mascot mascot-dog">
        <span class="ear left"></span>
        <span class="ear right"></span>
        <span class="face">U</span>
      </div>
      <div class="mascot mascot-cat">
        <span class="ear left"></span>
        <span class="ear right"></span>
        <span class="face">m</span>
      </div>
      <div class="paw paw-one">paw</div>
      <div class="paw paw-two">meow</div>
      <div class="paw paw-three">woof</div>

      <div class="hero-inner">
        <div class="hero-copy">
          <span class="eyebrow">遇见伙伴 · 健康档案 · 日常照护</span>
          <h1>给它一个家，也把健康照顾到每一天</h1>
          <p>从品种特点到日常照护，了解待领养的猫猫狗狗，找到与你合拍的小伙伴。</p>
          <div class="hero-actions">
            <button class="btn primary" @click="$router.push('/pets')">浏览宠物</button>
            <button class="btn secondary" @click="$router.push('/health-radar')">健康风险雷达</button>
          </div>
        </div>

        <div class="hero-board">
          <el-carousel
            v-if="carouselPets.length"
            class="pet-carousel"
            height="430px"
            indicator-position="outside"
            arrow="always"
            :interval="3600"
          >
            <el-carousel-item v-for="pet in carouselPets" :key="pet.id">
              <div class="board-main" @click="$router.push(`/pet/${pet.id}`)">
                <img :src="resolveImage(pet)" :style="imageStyle(pet)" :alt="pet.name" @error="handleImageError($event, pet)" />
                <div class="carousel-caption">
                  <span class="pet-pill">{{ pet.type }}</span>
                  <h3>{{ pet.name }}</h3>
                  <p>{{ pet.breed }} · {{ pet.age }}个月 · {{ pet.weight }}kg</p>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
          <div class="mini-grid">
            <div class="mini-card coral">
              <strong>{{ summary.available }}</strong>
              <span>待领养</span>
            </div>
            <div class="mini-card mint">
              <strong>{{ summary.healthRecords }}</strong>
              <span>健康记录</span>
            </div>
            <div class="mini-card blue">
              <strong>{{ summary.vaccineRecords }}</strong>
              <span>疫苗记录</span>
            </div>
            <div class="mini-card yellow">
              <strong>{{ summary.sources }}</strong>
              <span>公开来源</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="quick-health">
      <div class="container">
        <div class="section-header">
          <div>
            <span class="eyebrow dark">日常健康照护</span>
            <h2>领养前后都能用的健康工具</h2>
          </div>
        </div>
        <div class="health-tools">
          <article class="tool-card radar" @click="$router.push('/health-radar')">
            <div class="tool-art">
              <div class="orbit"></div>
              <span>HR</span>
            </div>
            <h3>健康风险雷达</h3>
            <p>自动聚合体温、体重、疫苗复种日期，给出低/中/高风险分级和处理建议。</p>
            <button>进入分析</button>
          </article>
          <article class="tool-card calendar" @click="$router.push('/care-plan')">
            <div class="tool-art calendar-art">
              <span>7</span>
              <i></i>
            </div>
            <h3>个性化照护计划</h3>
            <p>按宠物品种、年龄、疫苗日期和健康记录生成可勾选的照护任务清单。</p>
            <button>生成计划</button>
          </article>
          <article class="tool-card ai" @click="$router.push('/pets')">
            <div class="tool-art ai-art">
              <span>AI</span>
            </div>
            <h3>宠物照护咨询</h3>
            <p>结合宠物档案，了解品种特点，咨询日常照护与领养问题。</p>
            <button>选择宠物</button>
          </article>
        </div>
      </div>
    </section>

    <section class="pets-preview">
      <div class="container">
        <div class="section-header">
          <div>
            <span class="eyebrow dark">等待相遇的小伙伴</span>
            <h2>近期可领养宠物</h2>
          </div>
          <router-link to="/pets" class="view-all">查看全部</router-link>
        </div>
        <div class="pet-grid">
          <article class="pet-card" v-for="pet in pets" :key="pet.id" @click="$router.push(`/pet/${pet.id}`)">
            <div class="pet-image">
              <img :src="resolveImage(pet)" :style="imageStyle(pet)" :alt="pet.name" @error="handleImageError($event, pet)" />
              <span>{{ pet.status === 0 ? '待领养' : '已领养' }}</span>
            </div>
            <div class="pet-info">
              <div class="pet-name-row">
                <h3>{{ pet.name }}</h3>
                <b>{{ pet.gender === 0 ? '公' : '母' }}</b>
              </div>
              <p>{{ pet.type }} · {{ pet.breed }}</p>
              <div class="pet-meta">
                <span>{{ pet.age }}个月</span>
                <span>{{ pet.weight }}kg</span>
                <span :class="['health-chip', pet.healthStatus]">{{ pet.healthStatus === 'healthy' ? '健康' : pet.healthStatus || '待核验' }}</span>
              </div>
              <small>{{ pet.shelter || '救助站信息待补充' }} · 资料：{{ pet.sourceName || '公开资料' }}</small>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="color-strip">
      <div class="container strip-grid">
        <div class="strip-text">
          <span class="eyebrow">用心照顾每一天</span>
          <h2>不只展示宠物，也持续记录健康状态</h2>
          <p>在宠物档案中查看体重变化、接种安排，了解日常照护建议，为接下来的陪伴做好准备。</p>
        </div>
        <div class="bubble-wrap">
          <div class="bubble dog-bubble">dog</div>
          <div class="bubble cat-bubble">cat</div>
          <div class="bubble heart-bubble">care</div>
        </div>
      </div>
    </section>

    <section class="announcements-preview">
      <div class="container">
        <div class="section-header">
          <div>
            <span class="eyebrow dark">站内动态</span>
            <h2>最新公告资讯</h2>
          </div>
          <router-link to="/announcements" class="view-all">查看全部</router-link>
        </div>
        <div class="announcement-list">
          <article class="announcement-item" v-for="ann in announcements" :key="ann.id">
            <span :class="['announcement-type', ann.type]">{{ ann.type === 'notice' ? '公告' : '资讯' }}</span>
            <div>
              <h3>{{ ann.title }}</h3>
              <p>{{ ann.content?.slice(0, 70) }}...</p>
              <small>{{ formatDate(ann.createdAt) }} · {{ ann.sourceName || '平台公告' }}</small>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="source-section">
      <div class="container">
        <div class="source-card">
          <h2>数据来源</h2>
          <p>品种资料和图片参考公开来源。健康图表包含平台参考记录，不代表真实临床记录；领养前请核对救助资料与就诊、接种凭证。</p>
          <div class="source-list">
            <a href="https://raw.githubusercontent.com/tmfilho/akcdata/master/data/akc-data-latest.csv" target="_blank">AKC Breed Info CSV</a>
            <a href="https://dog.ceo/dog-api/" target="_blank">Dog CEO Dog API</a>
            <a href="https://api.thecatapi.com/v1/breeds" target="_blank">The Cat API</a>
            <a href="https://www.msdvetmanual.com/multimedia/table/normal-rectal-temperature-ranges" target="_blank">MSD/Merck Veterinary Manual</a>
            <a href="https://www.aaha.org/resources/2022-aaha-canine-vaccination-guidelines/" target="_blank">AAHA 犬疫苗指南</a>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../utils/request'
import { getPetImageStyle, handlePetImageError, resolvePetImage } from '../utils/petImage'

const pets = ref([])
const carouselPets = ref([])
const announcements = ref([])
const healthCount = ref(0)
const vaccineCount = ref(0)
const totalPets = ref(0)

const summary = computed(() => ({
  available: totalPets.value,
  healthRecords: healthCount.value,
  vaccineRecords: vaccineCount.value,
  sources: 5
}))

onMounted(async () => {
  await loadPets()
  loadAnnouncements()
  loadHealthSummary()
})

const loadPets = async () => {
  const res = await request.get('/pets', { params: { page: 1, size: 12 } })
  if (res.code === 200) {
    pets.value = res.data.records || []
    totalPets.value = res.data.total || pets.value.length
    carouselPets.value = shufflePets(pets.value).slice(0, 5)
  }
}

const shufflePets = (list) => {
  const result = [...list]
  for (let index = result.length - 1; index > 0; index -= 1) {
    const randomIndex = Math.floor(Math.random() * (index + 1))
    ;[result[index], result[randomIndex]] = [result[randomIndex], result[index]]
  }
  return result
}

const loadHealthSummary = async () => {
  const targets = pets.value.slice(0, 12)
  const result = await Promise.all(targets.map(async pet => {
    const [health, vaccine] = await Promise.all([
      request.get(`/health/pet/${pet.id}`).catch(() => ({ data: [] })),
      request.get(`/vaccine/pet/${pet.id}`).catch(() => ({ data: [] }))
    ])
    return {
      health: health.data?.length || 0,
      vaccine: vaccine.data?.length || 0
    }
  }))
  healthCount.value = result.reduce((sum, item) => sum + item.health, 0)
  vaccineCount.value = result.reduce((sum, item) => sum + item.vaccine, 0)
}

const loadAnnouncements = async () => {
  const res = await request.get('/announcements')
  if (res.code === 200) {
    announcements.value = (res.data || []).slice(0, 3)
  }
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
.home {
  overflow: hidden;
  color: #243b35;
  background: #f7faf8;
}

.hero {
  position: relative;
  min-height: 660px;
  display: flex;
  align-items: center;
  background:
    radial-gradient(circle at 12% 18%, rgba(255, 180, 115, 0.34), transparent 24%),
    radial-gradient(circle at 86% 12%, rgba(108, 141, 246, 0.25), transparent 24%),
    linear-gradient(135deg, #f7faf8 0%, #e8f5ef 48%, #fff4e8 100%);
}

.hero-inner,
.container {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
}

.hero-inner {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 44px;
  align-items: center;
  position: relative;
  z-index: 2;
}

.eyebrow {
  display: inline-flex;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(45, 159, 134, 0.85);
  margin-bottom: 14px;
}

.eyebrow.dark {
  color: #2d9f86;
  background: rgba(45, 159, 134, 0.12);
}

.hero-copy h1 {
  font-size: 54px;
  line-height: 1.12;
  margin: 0 0 20px;
  max-width: 620px;
}

.hero-copy p {
  font-size: 18px;
  line-height: 1.8;
  color: #52645f;
  max-width: 580px;
  margin: 0 0 32px;
}

.hero-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.btn,
.tool-card button {
  height: 44px;
  padding: 0 24px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 15px;
  font-weight: 700;
}

.btn.primary {
  color: #fff;
  background: #2d9f86;
  box-shadow: 0 12px 28px rgba(45, 159, 134, 0.25);
}

.btn.secondary {
  color: #81542f;
  background: #ffe2c3;
}

.hero-board {
  display: grid;
  gap: 16px;
}

.board-main,
.pet-card,
.tool-card,
.announcement-item,
.source-card {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(45, 159, 134, 0.13);
  box-shadow: 0 18px 44px rgba(38, 59, 53, 0.09);
}

.pet-carousel {
  border-radius: 8px;
  overflow: hidden;
}

.board-main {
  position: relative;
  height: 100%;
  overflow: hidden;
  cursor: pointer;
}

.board-main img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.carousel-caption {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 18px;
  padding: 18px;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(180deg, rgba(36, 59, 53, 0.08), rgba(36, 59, 53, 0.78));
  backdrop-filter: blur(8px);
}

.pet-pill {
  display: inline-flex;
  color: #fff;
  background: #ff9f5a;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  margin-bottom: 8px;
}

.board-main h3 {
  font-size: 26px;
  margin: 0 0 6px;
}

.board-main p {
  margin: 0;
  color: rgba(255,255,255,0.88);
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.mini-card {
  border-radius: 8px;
  padding: 18px 12px;
  color: #243b35;
}

.mini-card strong {
  display: block;
  font-size: 28px;
  line-height: 1;
  margin-bottom: 8px;
}

.mini-card span {
  font-size: 13px;
}

.mini-card.coral { background: #ffe6df; }
.mini-card.mint { background: #daf3ed; }
.mini-card.blue { background: #e0e9ff; }
.mini-card.yellow { background: #fff0c4; }

.mascot {
  position: absolute;
  z-index: 1;
  width: 116px;
  height: 104px;
  border-radius: 46% 46% 42% 42%;
  display: grid;
  place-items: center;
  color: rgba(36, 59, 53, 0.55);
  font-weight: 900;
  box-shadow: 0 20px 50px rgba(38, 59, 53, 0.12);
  animation: float 6s ease-in-out infinite;
}

.mascot .ear {
  position: absolute;
  top: -16px;
  width: 38px;
  height: 42px;
  background: inherit;
  border-radius: 12px 12px 20px 20px;
}

.mascot .ear.left { left: 16px; transform: rotate(-22deg); }
.mascot .ear.right { right: 16px; transform: rotate(22deg); }
.mascot .face { font-size: 30px; letter-spacing: 0; }

.mascot-dog {
  top: 84px;
  left: 5%;
  background: #ffd6a5;
}

.mascot-cat {
  right: 6%;
  bottom: 80px;
  background: #cde7ff;
  animation-delay: 1.5s;
}

.paw,
.bubble {
  position: absolute;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: rgba(36, 59, 53, 0.46);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
}

.paw-one {
  width: 74px;
  height: 74px;
  left: 8%;
  bottom: 90px;
  background: #f7c6d9;
}

.paw-two {
  width: 56px;
  height: 56px;
  right: 32%;
  top: 90px;
  background: #d6f5ca;
}

.paw-three {
  width: 64px;
  height: 64px;
  right: 9%;
  top: 160px;
  background: #fff0c4;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-16px); }
}

.quick-health,
.pets-preview,
.announcements-preview,
.source-section {
  padding: 76px 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 20px;
  margin-bottom: 28px;
}

.section-header h2,
.strip-text h2,
.source-card h2 {
  margin: 0;
  font-size: 34px;
}

.view-all {
  color: #2d9f86;
  text-decoration: none;
  font-weight: 700;
}

.health-tools {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.tool-card {
  padding: 24px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.tool-card:hover,
.pet-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 22px 48px rgba(38, 59, 53, 0.14);
}

.tool-art {
  width: 76px;
  height: 76px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  margin-bottom: 18px;
  position: relative;
  font-weight: 900;
  color: #fff;
  background: #2d9f86;
}

.tool-card.radar .tool-art { background: #6c8df6; }
.tool-card.calendar .tool-art { background: #ff9f5a; }
.tool-card.ai .tool-art { background: #2d9f86; }

.orbit {
  position: absolute;
  width: 54px;
  height: 54px;
  border: 2px dashed rgba(255,255,255,0.8);
  border-radius: 50%;
}

.calendar-art i {
  position: absolute;
  bottom: 12px;
  width: 42px;
  height: 6px;
  border-radius: 999px;
  background: rgba(255,255,255,0.72);
}

.tool-card h3,
.pet-info h3,
.announcement-item h3 {
  margin: 0 0 10px;
}

.tool-card p {
  min-height: 72px;
  color: #65746f;
  line-height: 1.7;
  margin: 0 0 18px;
}

.tool-card button {
  color: #fff;
  background: #243b35;
}

.pet-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.pet-card {
  overflow: hidden;
  cursor: pointer;
}

.pet-image {
  position: relative;
  height: 210px;
}

.pet-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.pet-image span {
  position: absolute;
  right: 12px;
  top: 12px;
  color: #fff;
  background: rgba(45, 159, 134, 0.92);
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
}

.pet-info {
  padding: 16px;
}

.pet-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pet-name-row b {
  color: #e86f51;
  font-size: 13px;
}

.pet-info p {
  margin: 0 0 8px;
  color: #65746f;
}

.pet-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin: 0 0 9px;
}

.pet-meta span {
  color: #58716a;
  background: #f0f7f4;
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 11px;
}

.pet-meta .health-chip.healthy {
  color: #207a68;
  background: #e3f5ee;
}

.pet-meta .health-chip:not(.healthy) {
  color: #a66235;
  background: #fff0e4;
}

.pet-info small {
  color: #87918d;
  line-height: 1.6;
}

.color-strip {
  position: relative;
  padding: 72px 0;
  background: linear-gradient(135deg, #2d9f86 0%, #6c8df6 52%, #ff9f5a 100%);
  color: #fff;
}

.strip-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  align-items: center;
  gap: 40px;
}

.strip-text p {
  max-width: 640px;
  color: rgba(255,255,255,0.88);
  line-height: 1.8;
  margin: 18px 0 0;
}

.bubble-wrap {
  position: relative;
  min-height: 210px;
}

.bubble {
  box-shadow: 0 16px 36px rgba(0,0,0,0.13);
}

.dog-bubble {
  width: 132px;
  height: 132px;
  left: 0;
  top: 18px;
  background: #ffe2c3;
}

.cat-bubble {
  width: 112px;
  height: 112px;
  right: 34px;
  top: 0;
  background: #d8ecff;
}

.heart-bubble {
  width: 96px;
  height: 96px;
  right: 120px;
  bottom: 0;
  background: #ffd0df;
}

.announcement-list {
  display: grid;
  gap: 14px;
}

.announcement-item {
  display: grid;
  grid-template-columns: 78px 1fr;
  gap: 16px;
  align-items: start;
  padding: 18px;
}

.announcement-type {
  display: inline-grid;
  place-items: center;
  height: 32px;
  border-radius: 999px;
  font-size: 13px;
  color: #fff;
  background: #6c8df6;
}

.announcement-type.notice {
  background: #e86f51;
}

.announcement-item p,
.announcement-item small {
  color: #65746f;
}

.announcement-item p {
  margin: 0 0 8px;
  line-height: 1.6;
}

.source-section {
  padding-top: 0;
}

.source-card {
  padding: 28px;
}

.source-card p {
  color: #65746f;
  line-height: 1.7;
  margin: 12px 0 18px;
}

.source-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.source-list a {
  color: #2d9f86;
  background: rgba(45, 159, 134, 0.1);
  border-radius: 999px;
  padding: 8px 12px;
  text-decoration: none;
  font-size: 13px;
}

@media (max-width: 980px) {
  .hero-inner,
  .strip-grid,
  .health-tools,
  .pet-grid {
    grid-template-columns: 1fr 1fr;
  }

  .hero-copy {
    grid-column: 1 / -1;
  }
}

@media (max-width: 680px) {
  .hero {
    padding: 70px 0;
  }

  .hero-inner,
  .strip-grid,
  .health-tools,
  .pet-grid,
  .mini-grid {
    grid-template-columns: 1fr;
  }

  .hero-copy h1 {
    font-size: 36px;
  }

  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .mascot,
  .paw {
    opacity: 0.35;
  }
}
</style>
