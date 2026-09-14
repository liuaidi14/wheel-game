<template>
  <div class="radar-wrapper" v-if="hasAttributes">
    <div class="radar-title">💪 角色属性</div>
    <div ref="chartRef" class="radar-chart"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick, computed } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  attributes: {
    type: Object,
    default: () => ({})
  },
  max: {
    type: Number,
    default: 25
  },
  radius: {
    type: String,
    default: '70%'
  }
})

const chartRef = ref(null)
let myChart = null
let resizeHandler = null

const hasAttributes = computed(() => {
  return props.attributes && Object.keys(props.attributes).length > 0
})

const renderChart = async () => {
  await nextTick()
  if (!chartRef.value || !hasAttributes.value) return

  if (!myChart) {
    myChart = echarts.init(chartRef.value, 'dark')
  }

  const attrNames = Object.keys(props.attributes)
  const attrValues = Object.values(props.attributes).map(v => Math.min(Number(v) || 0, props.max))

  const indicator = attrNames.map(name => ({
    name,
    max: props.max
  }))

  const option = {
    backgroundColor: 'transparent',
    animation: true,
    animationDuration: 800,
    animationEasing: 'cubicOut',
    radar: {
      indicator,
      shape: 'circle',
      center: ['50%', '50%'],
      radius: props.radius,
      splitNumber: 5,
      axisName: {
        color: '#f5e6b0',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.1)'
        }
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(255, 255, 255, 0.02)', 'rgba(255, 255, 255, 0.05)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.2)'
        }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: attrValues,
        name: '当前角色属性'
      }],
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(245, 215, 138, 0.8)' },
          { offset: 1, color: 'rgba(245, 215, 138, 0.1)' }
        ])
      },
      lineStyle: {
        color: '#f5d78a',
        width: 2
      },
      itemStyle: {
        color: '#f5d78a'
      },
      animationDuration: 1000,
      animationEasing: 'elasticOut'
    }]
  }

  myChart.setOption(option, true)
}

const clearChart = () => {
  if (myChart) {
    myChart.clear()
  }
}

watch(() => props.attributes, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    renderChart()
  } else {
    clearChart()
  }
}, { deep: true })

const handleResize = () => {
  if (myChart) {
    myChart.resize()
  }
}

onMounted(() => {
  renderChart()
  resizeHandler = handleResize
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  if (myChart) {
    myChart.dispose()
    myChart = null
  }
})
</script>

<style scoped>
.radar-wrapper {
  width: 100%;
  max-width: 280px;
  margin: 10px auto;
  background: #1d1428;
  border: 1px solid #5f4a3a;
  border-radius: 20px;
  padding: 10px 10px 0 10px;
}

.radar-title {
  color: #f5e6b0;
  font-weight: bold;
  text-align: center;
  margin-bottom: 0px;
}

.radar-chart {
  width: 100%;
  aspect-ratio: 1 / 1;
}
</style>
