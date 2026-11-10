<template>
  <div class="chart-container">
    <div ref="chartRef" class="chart"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chartInstance = null

const ensureChart = () => {
  if (!chartRef.value) return null
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  return chartInstance
}

const formatDate = (value) => {
  if (!value) return ''
  const d = new Date(value)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const toNumber = (value) => {
  const num = typeof value === 'number' ? value : Number.parseFloat(value)
  return Number.isFinite(num) ? Number(num.toFixed(1)) : null
}

const showEmpty = () => {
  const chart = ensureChart()
  if (!chart) return
  chart.clear()
  chart.setOption({
    title: {
      text: '暂无体温记录',
      left: 'center',
      top: 'middle',
      textStyle: { color: '#8a9a94', fontSize: 14, fontWeight: 400 }
    }
  })
}

const initChart = () => {
  const rows = props.data
    .map(item => ({ ...item, tempValue: toNumber(item.temperature) }))
    .filter(item => item.tempValue !== null)

  if (!rows.length) {
    showEmpty()
    return
  }

  const chart = ensureChart()
  if (!chart) return

  const dates = rows.map(item => formatDate(item.recordDate))
  const temps = rows.map(item => item.tempValue)

  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#f0ddd2',
      borderWidth: 1,
      textStyle: { color: '#243b35' },
      formatter: (params) => {
        const index = params[0].dataIndex
        const row = rows[index]
        return `<strong>${row.recordDate || params[0].name}</strong><br/>体温：${params[0].value} ℃<br/>医院：${row.hospital || '-'}`
      }
    },
    toolbox: {
      right: 8,
      top: 0,
      feature: {
        dataZoom: { yAxisIndex: 'none' },
        restore: {},
        saveAsImage: { pixelRatio: 2 }
      }
    },
    grid: {
      left: '4%',
      right: '5%',
      bottom: rows.length > 5 ? 42 : 20,
      top: 42,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#f0ddd2' } },
      axisLabel: { color: '#52645f', fontSize: 12 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      min: (value) => Math.max(36, Number((value.min - 0.3).toFixed(1))),
      max: (value) => Number((value.max + 0.3).toFixed(1)),
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f6eee9', type: 'dashed' } },
      axisLabel: { color: '#52645f', fontSize: 12, formatter: '{value} ℃' }
    },
    dataZoom: rows.length > 5 ? [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', height: 18, bottom: 8, start: 0, end: 100 }
    ] : [],
    series: [{
      name: '体温',
      type: 'line',
      data: temps,
      smooth: true,
      symbol: 'circle',
      symbolSize: 9,
      lineStyle: { color: '#e86f51', width: 3 },
      itemStyle: { color: '#e86f51', borderWidth: 2, borderColor: '#fff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(232,111,81,0.25)' },
          { offset: 1, color: 'rgba(232,111,81,0.04)' }
        ])
      },
      markArea: {
        silent: true,
        itemStyle: { color: 'rgba(45,159,134,0.08)' },
        label: { color: '#2d9f86', formatter: '常见正常区间' },
        data: [[{ yAxis: 37.5 }, { yAxis: 39.2 }]]
      }
    }],
    animationDuration: 700
  }, true)

  chart.off('click')
  chart.on('click', params => {
    const row = rows[params.dataIndex]
    ElMessage.info(`${row.recordDate || params.name} 体温 ${params.value} ℃`)
  })
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  initChart()
})

watch(() => props.data, () => {
  setTimeout(initChart, 80)
}, { deep: true })

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 270px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
