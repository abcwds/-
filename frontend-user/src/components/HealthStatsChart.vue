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
  healthRecords: {
    type: Array,
    default: () => []
  },
  vaccineRecords: {
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

const showEmpty = () => {
  const chart = ensureChart()
  if (!chart) return
  chart.clear()
  chart.setOption({
    title: {
      text: '暂无健康统计数据',
      left: 'center',
      top: 'middle',
      textStyle: { color: '#8a9a94', fontSize: 14, fontWeight: 400 }
    }
  })
}

const moneyValue = (value) => {
  const num = typeof value === 'number' ? value : Number.parseFloat(value)
  return Number.isFinite(num) ? num : 0
}

const buildSymptomData = () => {
  const map = new Map()
  props.healthRecords.forEach(record => {
    const name = record.symptom || '常规检查'
    map.set(name, (map.get(name) || 0) + 1)
  })
  return Array.from(map.entries()).map(([name, value]) => ({ name, value }))
}

const initChart = () => {
  const totalHealth = props.healthRecords.length
  const totalVaccine = props.vaccineRecords.length

  if (totalHealth === 0 && totalVaccine === 0) {
    showEmpty()
    return
  }

  const chart = ensureChart()
  if (!chart) return

  const avgCost = totalHealth
    ? Math.round(props.healthRecords.reduce((sum, row) => sum + moneyValue(row.cost), 0) / totalHealth)
    : 0
  const finishedVaccines = props.vaccineRecords.filter(row => row.vaccinateDate).length
  const nextVaccines = props.vaccineRecords.filter(row => row.nextDate).length
  const symptomData = buildSymptomData()

  chart.setOption({
    backgroundColor: 'transparent',
    color: ['#2d9f86', '#ff9f5a', '#6c8df6', '#e86f51', '#8c6bd8'],
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#dce6e1',
      borderWidth: 1,
      textStyle: { color: '#243b35' }
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#52645f' }
    },
    toolbox: {
      right: 8,
      top: 0,
      feature: {
        restore: {},
        saveAsImage: { pixelRatio: 2 }
      }
    },
    grid: {
      left: '7%',
      right: '48%',
      top: 46,
      bottom: 54,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['检查', '已接种', '待复种', '均费'],
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#dce6e1' } },
      axisLabel: { color: '#52645f' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#edf3f1', type: 'dashed' } },
      axisLabel: { color: '#52645f' }
    },
    series: [
      {
        name: '统计值',
        type: 'bar',
        barWidth: 24,
        data: [
          { value: totalHealth, name: '健康检查' },
          { value: finishedVaccines, name: '已接种疫苗' },
          { value: nextVaccines, name: '需复种提醒' },
          { value: avgCost, name: '平均费用' }
        ],
        label: { show: true, position: 'top', color: '#243b35' },
        itemStyle: {
          borderRadius: [6, 6, 0, 0]
        }
      },
      {
        name: '症状/检查类型',
        type: 'pie',
        radius: ['36%', '62%'],
        center: ['76%', '48%'],
        avoidLabelOverlap: true,
        label: {
          formatter: '{b}\n{c}次',
          color: '#52645f',
          fontSize: 12
        },
        emphasis: {
          scale: true,
          scaleSize: 8
        },
        data: symptomData.length ? symptomData : [{ name: '常规检查', value: totalHealth }]
      }
    ],
    animationDuration: 700
  }, true)

  chart.off('click')
  chart.on('click', params => {
    const unit = params.name.includes('费用') || params.name === '均费' ? '元' : '条'
    ElMessage.info(`${params.name}：${params.value}${unit}`)
  })
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  initChart()
})

watch([() => props.healthRecords, () => props.vaccineRecords], () => {
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
  height: 290px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
