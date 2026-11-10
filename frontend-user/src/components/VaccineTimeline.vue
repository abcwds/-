<template>
  <div class="chart-container">
    <div ref="chartRef" class="chart"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
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
  if (!value) return '-'
  const d = new Date(value)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const showEmpty = () => {
  const chart = ensureChart()
  if (!chart) return
  chart.clear()
  chart.setOption({
    title: {
      text: '暂无疫苗记录',
      left: 'center',
      top: 'middle',
      textStyle: { color: '#8a9a94', fontSize: 14, fontWeight: 400 }
    }
  })
}

const initChart = () => {
  const rows = props.data
    .filter(item => item.vaccinateDate)
    .sort((a, b) => new Date(a.vaccinateDate) - new Date(b.vaccinateDate))

  if (!rows.length) {
    showEmpty()
    return
  }

  const chart = ensureChart()
  if (!chart) return

  const vaccineNames = Array.from(new Set(rows.map(item => item.vaccineName || '疫苗')))
  const doneData = rows.map((item, index) => ({
    name: item.vaccineName,
    value: [new Date(item.vaccinateDate).getTime(), item.vaccineName || '疫苗', index],
    record: item
  }))
  const nextData = rows
    .filter(item => item.nextDate)
    .map((item, index) => ({
      name: `${item.vaccineName}复种`,
      value: [new Date(item.nextDate).getTime(), item.vaccineName || '疫苗', index],
      record: item
    }))

  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#dce6e1',
      borderWidth: 1,
      textStyle: { color: '#243b35' },
      formatter: (params) => {
        const record = params.data.record
        const dateLabel = params.seriesName === '下次复种提醒'
          ? `下次复种：${formatDate(record.nextDate)}`
          : `接种日期：${formatDate(record.vaccinateDate)}`
        return `<strong>${record.vaccineName}</strong><br/>${dateLabel}<br/>类型：${record.vaccineType || '-'}<br/>医院：${record.hospital || '-'}<br/>医生：${record.doctor || '-'}`
      }
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#52645f' }
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
      left: '18%',
      right: '6%',
      top: 42,
      bottom: rows.length > 4 ? 52 : 38,
      containLabel: true
    },
    xAxis: {
      type: 'time',
      axisLine: { lineStyle: { color: '#dce6e1' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#52645f',
        formatter: (value) => {
          const d = new Date(value)
          return `${d.getMonth() + 1}/${d.getDate()}`
        }
      },
      splitLine: { lineStyle: { color: '#edf3f1', type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: vaccineNames,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#52645f', width: 90, overflow: 'truncate' },
      splitLine: { show: true, lineStyle: { color: '#f2f6f4' } }
    },
    dataZoom: rows.length > 4 ? [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', height: 18, bottom: 8, start: 0, end: 100 }
    ] : [],
    series: [
      {
        name: '已接种',
        type: 'scatter',
        symbolSize: 22,
        data: doneData,
        itemStyle: {
          color: '#2d9f86',
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: true,
          formatter: '已',
          color: '#fff',
          fontSize: 11,
          fontWeight: 700
        }
      },
      {
        name: '下次复种提醒',
        type: 'effectScatter',
        symbolSize: 18,
        rippleEffect: { scale: 3, brushType: 'stroke' },
        data: nextData,
        itemStyle: {
          color: '#ff9f5a',
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          position: 'top',
          formatter: '复种',
          color: '#8a5528',
          fontSize: 11
        }
      }
    ],
    animationDuration: 700
  }, true)

  chart.off('click')
  chart.on('click', params => {
    const record = params.data.record
    const date = params.seriesName === '下次复种提醒' ? record.nextDate : record.vaccinateDate
    ElMessage.info(`${record.vaccineName}：${formatDate(date)}`)
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
  height: 230px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
