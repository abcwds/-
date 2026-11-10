<template>
  <section class="platform-data">
    <div class="platform-intro"><div><h2>换个角度，了解我们的伙伴</h2><p>个人申请与公开宠物档案分别展示，健康图表仅供了解平台参考记录。</p></div><el-button :loading="loading" @click="loadData">刷新数据</el-button></div>
    <p v-if="loadError" class="platform-alert" role="alert">部分公开档案暂未获取成功，相关图表仅统计本次成功加载的记录；你可以刷新重试。</p>
    <p class="sample-note">{{ loading ? '正在加载公开档案，请稍候…' : '当前样本：' + pets.length + ' / ' + (petTotal === null ? '未知' : petTotal) + ' 份可领养宠物档案' }}<span v-if="petTotal > pets.length && !loading"> · 最多展示200份档案的参考数据</span></p>
    <el-skeleton v-if="loading" :rows="9" animated class="platform-skeleton" />
    <div v-show="!loading" class="charts-grid">
      <article class="chart-panel"><h3>我的申请状态</h3><p>当前账号的全部申请，按审核状态统计</p><div ref="applicationRef" class="chart"></div></article>
      <article class="chart-panel"><h3>公开伙伴的犬猫构成</h3><p>依据当前加载的可领养档案统计</p><div ref="mixRef" class="chart"></div></article>
      <article class="chart-panel wide"><div class="chart-heading"><div><h3>年龄与体重分布</h3><p>查看不同伙伴的档案信息，不作为健康诊断依据</p></div><el-select v-model="scatterType" aria-label="宠物类型" class="type-filter"><el-option label="全部伙伴" value="all" /><el-option label="犬类" value="Dog" /><el-option label="猫咪" value="Cat" /></el-select></div><div ref="scatterRef" class="chart wide-chart"></div></article>
      <article class="chart-panel wide"><h3>健康与接种参考记录</h3><p>当前样本档案中的年度记录，可能包含平台参考数据</p><div ref="healthRef" class="chart wide-chart"></div></article>
    </div>
    <div class="source-note"><el-icon><InfoFilled /></el-icon><div><strong>关于这些数据</strong><p>公开档案包含品种资料、图片及平台参考记录，不代表真实临床记录。领养前请向救助方核对实际情况，健康和接种信息以就诊、接种凭证为准。</p><div class="source-links"><a href="https://dog.ceo/dog-api/" target="_blank" rel="noopener noreferrer">Dog CEO 图片来源<el-icon><TopRight /></el-icon></a><a href="https://api.thecatapi.com/v1/breeds" target="_blank" rel="noopener noreferrer">The Cat API 品种来源<el-icon><TopRight /></el-icon></a><a href="https://raw.githubusercontent.com/tmfilho/akcdata/master/data/akc-data-latest.csv" target="_blank" rel="noopener noreferrer">AKC 品种资料<el-icon><TopRight /></el-icon></a></div></div></div>
  </section>
</template>
<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { InfoFilled, TopRight } from '@element-plus/icons-vue'
import request from '../utils/request'
const props = defineProps({ active: Boolean, summary: { type: Object, required: true } })
const applicationRef = ref(null), mixRef = ref(null), scatterRef = ref(null), healthRef = ref(null)
const pets = ref([]), petTotal = ref(null), healthRecords = ref([]), vaccineRecords = ref([])
const scatterType = ref('all'), loading = ref(false), loadError = ref(false)
const charts = new Map()
let disposed = false
let observer
const empty = text => ({ title: { text, top: 'middle', left: 'center', textStyle: { color: '#9aa88b', fontSize: 12, fontWeight: 400 } } })
const palette = ['#8ca87a', '#709882', '#d6bd8f']
function setChart(element, option) {
  if (!element || disposed) return
  let chart = charts.get(element)
  if (!chart) { chart = echarts.init(element); charts.set(element, chart) }
  chart.setOption(option, true)
  chart.resize()
}
const tools = { right: 4, top: 0, feature: { restore: { title: '还原' }, saveAsImage: { title: '保存图片', pixelRatio: 2 } } }
function pieOption(data, note) {
  if (!data.some(item => item.value > 0)) return empty(note)
  return { color: palette, tooltip: { trigger: 'item' }, toolbox: tools, legend: { bottom: 0, textStyle: { color: '#869477', fontSize: 11 }, icon: 'circle', itemHeight: 7 }, series: [{ type: 'pie', radius: ['44%', '68%'], center: ['50%', '44%'], label: { show: false }, emphasis: { label: { show: true, formatter: '{b}\n{c}', color: '#617f53' } }, itemStyle: { borderColor: '#fff', borderWidth: 4, borderRadius: 5 }, data }] }
}
function renderApplication() {
  const values = [props.summary.pending, props.summary.approved, props.summary.rejected]
  setChart(applicationRef.value, values.some(value => value === null) ? empty('个人申请统计暂不可用') : pieOption([{ name: '待审核', value: values[0] }, { name: '已通过', value: values[1] }, { name: '未通过', value: values[2] }], '暂时还没有领养申请'))
}
function renderScatter() {
  const data = pets.value.filter(pet => scatterType.value === 'all' || pet.type === scatterType.value).filter(pet => Number(pet.age) > 0 && Number(pet.weight) > 0).map(pet => [Number(pet.age), Number(pet.weight), pet.name, pet.breed])
  if (!data.length) { setChart(scatterRef.value, empty('暂无可展示的年龄和体重记录')); return }
  setChart(scatterRef.value, {
    tooltip: { formatter: params => { const el = document.createElement('div'); el.style.whiteSpace = 'pre-line'; el.textContent = String(params.data[2] || '伙伴') + '\n品种：' + String(params.data[3] || '未记录') + '\n年龄：' + params.data[0] + ' 个月\n体重：' + params.data[1] + ' kg'; return el } },
    toolbox: tools, grid: { top: 45, left: 18, right: 45, bottom: 28, containLabel: true },
    xAxis: { name: '月龄', type: 'value', axisLine: { lineStyle: { color: '#dce5d4' } }, axisLabel: { color: '#96a288', fontSize: 10 }, splitLine: { lineStyle: { color: '#f0f3e9', type: 'dashed' } } },
    yAxis: { name: '体重 kg', type: 'value', axisLabel: { color: '#96a288', fontSize: 10 }, splitLine: { lineStyle: { color: '#edf1e7', type: 'dashed' } } },
    series: [{ type: 'scatter', data, symbolSize: value => Math.max(10, Math.min(27, value[1] + 7)), itemStyle: { color: '#8cae7b', opacity: .8 } }]
  })
}
function renderCharts() {
  renderApplication()
  setChart(mixRef.value, pieOption([{ name: '犬类', value: pets.value.filter(pet => pet.type === 'Dog').length }, { name: '猫咪', value: pets.value.filter(pet => pet.type === 'Cat').length }, { name: '其他', value: pets.value.filter(pet => !['Dog', 'Cat'].includes(pet.type)).length }], '暂无公开宠物档案'))
  renderScatter()
  const yearOf = value => { const year = Number(String(value || '').slice(0, 4)); return year > 1900 && year < 3000 ? year : null }
  const years = [...new Set([...healthRecords.value.map(item => yearOf(item.recordDate)), ...vaccineRecords.value.map(item => yearOf(item.vaccinateDate))].filter(Boolean))].sort()
  setChart(healthRef.value, !years.length ? empty('当前样本暂无健康与接种记录') : { tooltip: { trigger: 'axis' }, legend: { top: 0, textStyle: { color: '#8c997c', fontSize: 11 } }, toolbox: tools, grid: { top: 45, left: 18, right: 30, bottom: 25, containLabel: true }, xAxis: { type: 'category', data: years, axisLine: { lineStyle: { color: '#dfe8d5' } }, axisLabel: { color: '#8e9b7f' } }, yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#8e9b7f', fontSize: 10 }, splitLine: { lineStyle: { color: '#edf1e7', type: 'dashed' } } }, series: [{ name: '健康记录', type: 'bar', barMaxWidth: 28, data: years.map(year => healthRecords.value.filter(item => yearOf(item.recordDate) === year).length), itemStyle: { color: '#8eaa7c', borderRadius: [5, 5, 0, 0] } }, { name: '接种记录', type: 'line', smooth: true, data: years.map(year => vaccineRecords.value.filter(item => yearOf(item.vaccinateDate) === year).length), itemStyle: { color: '#ccb685' }, lineStyle: { width: 3 } }] })
}
async function loadData() {
  if (loading.value) return
  loading.value = true
  loadError.value = false
  pets.value = []
  petTotal.value = null
  healthRecords.value = []
  vaccineRecords.value = []
  try {
    for (let page = 1; page <= 4; page++) {
      const res = await request.get('/pets', { params: { page, size: 50 } })
      if (disposed) return
      if (res.code !== 200 || !Array.isArray(res.data?.records)) throw new Error('档案不可用')
      petTotal.value = Number(res.data.total) || 0
      pets.value.push(...res.data.records)
      if (!res.data.records.length || pets.value.length >= petTotal.value) break
    }
    for (let start = 0; start < pets.value.length; start += 3) {
      if (disposed) return
      await Promise.all(pets.value.slice(start, start + 3).map(async pet => {
        await Promise.all([['/health/pet/', healthRecords], ['/vaccine/pet/', vaccineRecords]].map(async ([prefix, target]) => {
          try {
            const res = await request.get(prefix + pet.id)
            if (res.code !== 200 || !Array.isArray(res.data)) throw new Error('记录不可用')
            if (!disposed) target.value.push(...res.data)
          } catch { if (!disposed) loadError.value = true }
        }))
      }))
    }
  } catch { if (!disposed) loadError.value = true }
  finally {
    if (!disposed) { loading.value = false; await nextTick(); renderCharts() }
  }
}
function resizeCharts() { if (props.active && !loading.value) charts.forEach(chart => chart.resize()) }
watch(scatterType, renderScatter)
watch(() => props.active, async value => { if (value) { await nextTick(); resizeCharts() } })
watch(() => [props.summary.pending, props.summary.approved, props.summary.rejected], () => { if (!loading.value && charts.size) renderApplication() })
onMounted(() => { observer = new ResizeObserver(resizeCharts); if (applicationRef.value) observer.observe(applicationRef.value.parentElement); loadData() })
onBeforeUnmount(() => { disposed = true; observer?.disconnect(); charts.forEach(chart => chart.dispose()); charts.clear() })
</script>
<style scoped>
.platform-intro { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 17px; }
.platform-intro h2 { color: #607b53; font-size: 17px; font-weight: 500; }
.platform-intro p { color: #98a487; font-size: 11px; line-height: 1.8; margin-top: 8px; }
.platform-intro .el-button { font-size: 12px; }
.sample-note { color: #9aa88c; font-size: 10px; line-height: 1.8; margin-bottom: 20px; }
.platform-alert { padding: 13px 17px; margin-bottom: 15px; color: #b0915e; background: #faf4e5; border: 1px solid #ebe3cb; border-radius: 9px; font-size: 12px; line-height: 1.8; }
.platform-skeleton { padding: 35px; background: white; border: 1px solid #e3e9df; border-radius: 16px; }
.charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 22px; }
.chart-panel { min-width: 0; padding: 25px 27px 20px; border: 1px solid #e3e9df; border-radius: 16px; background: white; }
.chart-panel.wide { grid-column: 1 / -1; }
.chart-panel h3 { color: #5c7750; font-size: 15px; font-weight: 500; }
.chart-panel p { color: #a0aa91; font-size: 10px; line-height: 1.8; margin: 8px 0 16px; }
.chart { height: 260px; width: 100%; }
.wide-chart { height: 290px; }
.chart-heading { display: flex; align-items: center; justify-content: space-between; gap: 15px; }
.type-filter { flex-shrink: 0; width: 130px; }
.source-note { display: flex; align-items: flex-start; gap: 12px; padding: 24px 4px 0; color: #9eac8e; }
.source-note > .el-icon { margin-top: 3px; font-size: 17px; }
.source-note strong { color: #8d9c7c; font-size: 12px; font-weight: 500; }
.source-note p { font-size: 10px; line-height: 1.9; margin-top: 8px; }
.source-links { display: flex; flex-wrap: wrap; gap: 17px; margin-top: 14px; }
.source-links a { display: inline-flex; align-items: center; gap: 4px; color: #8da079; font-size: 10px; text-decoration: none; }
@media (max-width: 650px) { .platform-intro { align-items: flex-start; } .platform-intro h2 { font-size: 15px; line-height: 1.7; } .charts-grid { grid-template-columns: 1fr; gap: 18px; } .chart-panel { padding: 22px 18px 17px; } .chart-heading { flex-wrap: wrap; } .type-filter { width: 120px; margin-bottom: 10px; } .chart { height: 245px; } }
</style>
