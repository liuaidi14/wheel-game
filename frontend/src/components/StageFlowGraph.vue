<template>
  <div class="graph-wrapper">
    <div id="g6-container" ref="graphContainer" class="g6-content"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { wheelApi } from '../api'
import { Graph } from '@antv/g6'

const props = defineProps({
  currentGroupId: { type: Number, default: null },
  userId: { type: Number, default: 1 },
  baseStages: { type: Array, default: () => [] },
  lifeStages: { type: Array, default: () => [] },
  jumpToStage: { type: Function, default: () => {} }
})

const graphContainer = ref(null)
let graph = null
let isRendering = false // 防止并发渲染
let resizeHandler = null // 保存 resize 处理函数引用

// 构建节点类型映射
const buildStageTypeMap = () => {
  const map = new Map()
  props.baseStages.forEach(s => map.set(s.name, 'base'))
  props.lifeStages.forEach(s => map.set(s.name, 'life'))
  return map
}

// 渲染或更新图
const renderGraph = async () => {
  // 没有 groupId 时不渲染
  if (!props.currentGroupId) {
    clearGraph()
    return
  }

  // 防止重复请求（例如 watch immediate 和 onMounted 同时触发）
  if (isRendering) return
  isRendering = true

  try {
    await nextTick()
    if (!graphContainer.value) return

    const res = await wheelApi.getGraphData(props.userId, props.currentGroupId)
    const data = res.data

    // 清洗连线的 label
    if (data?.edges) {
      data.edges.forEach(edge => { delete edge.label })
    }

    // 给节点注入类型
    if (data?.nodes) {
      const typeMap = buildStageTypeMap()
      data.nodes.forEach(node => {
        node.type = typeMap.get(node.label) || 'base'
      })
    }

    // 初始化 graph（仅在首次创建）
    if (!graph) {
      const container = graphContainer.value
      const width = container.clientWidth || 400
      const height = container.clientHeight || 400

      graph = new Graph({
        container,
        width,
        height,
        modes: { default: ['drag-canvas', 'zoom-canvas', 'drag-node'] },
        defaultNode: {
          type: 'rect',
          size: [120, 40],
          style: { fill: '#2a1a2a', stroke: '#5f4a3a' },
          labelCfg: { style: { fill: '#e5d4b0', fontSize: 13 } }
        },
        defaultEdge: {
          type: 'line',
          style: { stroke: '#8a7a8a', lineWidth: 1.5 }
        },
        layout: {
          type: 'dagre',
          rankdir: 'TB',
          nodesep: 30,
          ranksep: 50
        }
      })

      // 绑定节点点击事件（仅一次）
      graph.on('node:click', (e) => {
        const nodeId = e.item.getID()
        const nodeData = data.nodes.find(n => n.id === nodeId)
        if (nodeData && props.jumpToStage) {
          props.jumpToStage(nodeData.label, nodeData.type)
        }
      })

      // 监听窗口大小变化，自动调整图大小
      resizeHandler = () => {
        if (graph) {
          graph.changeSize(
            graphContainer.value.clientWidth,
            graphContainer.value.clientHeight
          )
        }
      }
      window.addEventListener('resize', resizeHandler)
    }

    // 更新图数据并重新渲染
    graph.data(data)
    graph.render()
    // graph.refresh() 可以省略，render 已经足够
  } catch (error) {
    console.error('渲染图表失败:', error)
    // 可选：使用 ElMessage 提示用户
  } finally {
    isRendering = false
  }
}

// 清空图（当 currentGroupId 变为 null 或组件卸载时）
const clearGraph = () => {
  if (graph) {
    graph.clear()
  }
}

// 销毁图并清理资源
const disposeGraph = () => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  if (graph) {
    graph.destroy() // 使用 destroy 更彻底
    graph = null
  }
}

// 只在挂载后使用 watch immediate，避免重复调用
watch(
  () => props.currentGroupId,
  () => {
    renderGraph()
  },
  { immediate: true }
)

// 监听阶段数据变化，重新构建类型映射并更新图（如果图已存在）
watch(
  () => [props.baseStages, props.lifeStages],
  () => {
    // 仅当图存在且 currentGroupId 有效时，更新节点类型
    if (graph && props.currentGroupId) {
      const typeMap = buildStageTypeMap()
      const nodes = graph.getNodes()
      nodes.forEach(node => {
        const model = node.getModel()
        model.type = typeMap.get(model.label) || 'base'
        node.update(model) // 更新节点模型
      })
    }
  },
  { deep: true }
)

onBeforeUnmount(() => {
  disposeGraph()
})
</script>

<style scoped>
.graph-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
}
.g6-content {
  width: 100%;
  height: 100%;
}
</style>