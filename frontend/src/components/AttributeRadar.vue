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
  }
})

const chartRef = ref(null)
let myChart = null
let resizeHandler = null // 保存 resize 事件处理函数引用

const hasAttributes = computed(() => {
  return props.attributes && Object.keys(props.attributes).length > 0
})

// 构建并渲染/更新图表
const renderChart = async () => {
  await nextTick()
  if (!chartRef.value || !hasAttributes.value) return

  // 初始化图表实例（如果尚未创建）
  if (!myChart) {
    myChart = echarts.init(chartRef.value, 'dark')
  }

  const attrNames = Object.keys(props.attributes)
  const attrValues = Object.values(props.attributes)

  // 计算雷达图最大值：至少20，比实际最大值多5，避免贴边
  const maxVal = attrValues.length > 0 ? Math.max(20, ...attrValues) + 5 : 20

  const indicator = attrNames.map(name => ({
    name,
    max: maxVal
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
      radius: '70%',
      splitNumber: 4,
      axisName: {
        color: '#f5e6b0',
        fontSize: 14
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

  // 关键：notMerge: true 保证每次更新都重新执行动画
  myChart.setOption(option, true)
}

// 清空图表（当无属性时）
const clearChart = () => {
  if (myChart) {
    myChart.clear()
  }
}

// 监听属性变化
watch(() => props.attributes, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    renderChart()
  } else {
    clearChart()
  }
}, { deep: true })

// 处理窗口大小变化
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

// 清理资源：移除监听器、销毁图表实例
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
  height: 260px;
}
</style>