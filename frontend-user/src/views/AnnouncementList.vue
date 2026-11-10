<template>
  <div class="announcement-page">
    <section class="page-hero">
      <div>
        <span class="eyebrow">真实来源资讯</span>
        <h1>公告资讯</h1>
        <p>内容来自 CDC、AVMA、AAHA、ASPCA、MSD/Merck Veterinary Manual 等公开权威资料，页面会标明来源链接。</p>
      </div>
      <div class="hero-badge">
        <strong>{{ announcements.length }}</strong>
        <span>条内容</span>
      </div>
    </section>

    <section class="toolbar">
      <div class="tabs">
        <button class="tab" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">全部</button>
        <button class="tab" :class="{ active: activeTab === 'notice' }" @click="activeTab = 'notice'">公告</button>
        <button class="tab" :class="{ active: activeTab === 'news' }" @click="activeTab = 'news'">资讯</button>
      </div>
      <el-input v-model.trim="keyword" class="search-input" placeholder="搜索标题、内容或来源" clearable />
    </section>

    <section class="featured" v-if="featured">
      <div class="featured-copy">
        <span :class="['type-tag', featured.type]">{{ typeText(featured.type) }}</span>
        <h2>{{ featured.title }}</h2>
        <p>{{ featured.content }}</p>
        <div class="source-row">
          <span>来源：{{ featured.sourceName || '项目公告' }}</span>
          <a v-if="featured.sourceUrl" :href="featured.sourceUrl" target="_blank" @click.stop>查看原文</a>
        </div>
      </div>
      <div class="featured-art">
        <span class="round dog">dog</span>
        <span class="round cat">cat</span>
        <span class="round care">care</span>
      </div>
    </section>

    <section class="list-container">
      <article
        class="announcement-card"
        v-for="ann in restAnnouncements"
        :key="ann.id"
        @click="showDetail(ann)"
      >
        <div class="card-top">
          <span :class="['type-tag', ann.type]">{{ typeText(ann.type) }}</span>
          <span class="date">{{ formatDate(ann.createdAt) }}</span>
        </div>
        <h3>{{ ann.title }}</h3>
        <p>{{ ann.content }}</p>
        <div class="source-row">
          <span>来源：{{ ann.sourceName || '项目公告' }}</span>
          <a v-if="ann.sourceUrl" :href="ann.sourceUrl" target="_blank" @click.stop>查看原文</a>
        </div>
      </article>
    </section>

    <el-empty v-if="!filteredAnnouncements.length" description="暂无匹配内容" />

    <section class="source-panel">
      <h2>本页来源网站</h2>
      <div class="source-list">
        <a v-for="source in sourceLinks" :key="source.url" :href="source.url" target="_blank">
          {{ source.name }}
        </a>
      </div>
    </section>

    <el-dialog v-model="showModal" title="资讯详情" width="680px">
      <div class="detail-content" v-if="selectedAnn">
        <div class="detail-header">
          <span :class="['type-tag', selectedAnn.type]">{{ typeText(selectedAnn.type) }}</span>
          <span class="date">{{ formatDate(selectedAnn.createdAt) }}</span>
        </div>
        <h2>{{ selectedAnn.title }}</h2>
        <p>{{ selectedAnn.content }}</p>
        <div class="detail-source">
          <span>数据来源：{{ selectedAnn.sourceName || '项目公告' }}</span>
          <a v-if="selectedAnn.sourceUrl" :href="selectedAnn.sourceUrl" target="_blank">打开来源网站</a>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../utils/request'

const announcements = ref([])
const activeTab = ref('all')
const keyword = ref('')
const showModal = ref(false)
const selectedAnn = ref(null)

onMounted(() => {
  loadAnnouncements()
})

const loadAnnouncements = async () => {
  const res = await request.get('/announcements')
  if (res.code === 200) {
    announcements.value = res.data || []
  }
}

const filteredAnnouncements = computed(() => {
  const text = keyword.value.toLowerCase()
  return announcements.value.filter(item => {
    const typeOk = activeTab.value === 'all' || item.type === activeTab.value
    const textOk = !text
      || item.title?.toLowerCase().includes(text)
      || item.content?.toLowerCase().includes(text)
      || item.sourceName?.toLowerCase().includes(text)
    return typeOk && textOk
  })
})

const featured = computed(() => filteredAnnouncements.value[0])
const restAnnouncements = computed(() => filteredAnnouncements.value.slice(1))

const sourceLinks = computed(() => {
  const map = new Map()
  announcements.value.forEach(item => {
    if (item.sourceName && item.sourceUrl) {
      map.set(item.sourceUrl, item.sourceName)
    }
  })
  return Array.from(map.entries()).map(([url, name]) => ({ url, name }))
})

const showDetail = (ann) => {
  selectedAnn.value = ann
  showModal.value = true
}

const typeText = (type) => type === 'notice' ? '公告' : '资讯'

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.announcement-page {
  min-height: 100vh;
  padding: 28px 20px 70px;
  color: #243b35;
  background:
    radial-gradient(circle at 10% 8%, rgba(255, 159, 90, 0.18), transparent 24%),
    radial-gradient(circle at 88% 6%, rgba(45, 159, 134, 0.16), transparent 24%),
    #f7faf8;
}

.page-hero,
.toolbar,
.featured,
.list-container,
.source-panel {
  width: min(1060px, 100%);
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
  background: linear-gradient(135deg, #2d9f86, #6c8df6 56%, #ff9f5a);
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
  margin: 14px 0 10px;
  font-size: 42px;
}

.page-hero p {
  margin: 0;
  max-width: 710px;
  color: rgba(255,255,255,0.9);
  line-height: 1.8;
}

.hero-badge {
  width: 118px;
  height: 118px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  align-content: center;
  background: rgba(255,255,255,0.18);
  flex-shrink: 0;
}

.hero-badge strong {
  font-size: 36px;
}

.hero-badge span {
  font-size: 13px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-top: 18px;
  padding: 16px;
  border-radius: 8px;
  background: rgba(255,255,255,0.92);
  border: 1px solid rgba(45, 159, 134, 0.12);
}

.tabs {
  display: flex;
  gap: 10px;
}

.tab {
  height: 38px;
  padding: 0 18px;
  border: none;
  background: #edf3f1;
  border-radius: 8px;
  cursor: pointer;
  color: #52645f;
  font-weight: 700;
}

.tab.active {
  background: #2d9f86;
  color: #fff;
}

.search-input {
  max-width: 320px;
}

.featured,
.announcement-card,
.source-panel {
  border-radius: 8px;
  background: rgba(255,255,255,0.94);
  border: 1px solid rgba(45,159,134,0.12);
  box-shadow: 0 16px 42px rgba(38,59,53,0.08);
}

.featured {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 28px;
  align-items: center;
  margin-top: 18px;
  padding: 28px;
}

.featured h2 {
  margin: 14px 0 12px;
  font-size: 30px;
}

.featured p,
.announcement-card p,
.detail-content p {
  color: #52645f;
  line-height: 1.9;
}

.featured-art {
  position: relative;
  min-height: 220px;
}

.round {
  position: absolute;
  display: grid;
  place-items: center;
  border-radius: 999px;
  color: rgba(36,59,53,0.48);
  font-weight: 900;
  letter-spacing: 0;
}

.round.dog {
  width: 132px;
  height: 132px;
  left: 0;
  top: 18px;
  background: #ffe2c3;
}

.round.cat {
  width: 118px;
  height: 118px;
  right: 0;
  top: 0;
  background: #d8ecff;
}

.round.care {
  width: 92px;
  height: 92px;
  right: 88px;
  bottom: 0;
  background: #daf3ed;
}

.list-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 18px;
}

.announcement-card {
  padding: 22px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.announcement-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 22px 48px rgba(38,59,53,0.13);
}

.card-top,
.detail-header,
.source-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.type-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.type-tag.notice {
  background: #fff1ee;
  color: #e86f51;
}

.type-tag.news {
  background: #eaf1ff;
  color: #4f70d6;
}

.date {
  font-size: 13px;
  color: #87918d;
}

.announcement-card h3 {
  margin: 14px 0 10px;
  font-size: 20px;
}

.announcement-card p {
  margin: 0 0 16px;
}

.source-row {
  padding-top: 12px;
  border-top: 1px solid #edf3f1;
  color: #65746f;
  font-size: 13px;
}

.source-row a,
.detail-source a {
  color: #2d9f86;
  text-decoration: none;
  font-weight: 800;
}

.source-panel {
  margin-top: 18px;
  padding: 24px;
}

.source-panel h2 {
  margin: 0 0 14px;
}

.source-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.source-list a {
  color: #2d9f86;
  background: rgba(45,159,134,0.1);
  border-radius: 999px;
  padding: 8px 12px;
  text-decoration: none;
  font-size: 13px;
}

.detail-content {
  padding: 4px 0;
}

.detail-content h2 {
  margin: 18px 0 14px;
  color: #243b35;
}

.detail-source {
  margin-top: 18px;
  padding: 14px;
  border-radius: 8px;
  background: #f1f8f5;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #52645f;
}

@media (max-width: 840px) {
  .page-hero,
  .toolbar,
  .detail-source {
    flex-direction: column;
    align-items: flex-start;
  }

  .featured,
  .list-container {
    grid-template-columns: 1fr;
  }

  .search-input {
    max-width: 100%;
    width: 100%;
  }
}
</style>
