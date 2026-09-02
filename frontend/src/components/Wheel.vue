<template>
  <div class="wheel-container">
    <canvas ref="canvas" width="600" height="600" @click="spin"></canvas>
    <div class="pointer"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, defineProps, defineEmits, nextTick, watch } from 'vue'

const props = defineProps({
  options: { type: Array, required: true }
})
const emit = defineEmits(['spin'])

const canvas = ref(null)
let ctx = null
let rotation = 0
let animId = null
let spinning = false

// 音效
let audioSpin = null
let audioStop = null
try {
  audioSpin = new Audio('/sounds/spin.mp3')
  audioStop = new Audio('/sounds/stop.mp3')
} catch (e) {
  console.warn('找不到音效文件，将静音运行。')
}

// 常量配置
const COLORS = {
  BASE: { hueStart: 200, hueStep: 10, saturation: 70, lightness: 55 },
  LIFE: { hueStart: 10, hueStep: 15, saturation: 80, lightness: 55 },
  DEFAULT: { saturation: 70, lightness: 50 }
}
const ANIMATION_DURATION = { min: 2500, max: 3170 } // 2.5s ~ 3.17s
const EXTRA_SPINS_MIN = 10
const EXTRA_SPINS_RANGE = 8

// ---- 核心绘制函数 ----
function draw(angle) {
  if (!canvas.value) return
  if (!ctx) {
    ctx = canvas.value.getContext('2d')
    if (!ctx) return
  }
  const w = canvas.value.width
  const h = canvas.value.height
  const radius = Math.min(w, h) * 0.42
  const cx = w / 2
  const cy = h / 2
  ctx.clearRect(0, 0, w, h)

  const opts = props.options
  if (!opts || opts.length === 0) {
    // 绘制空状态
    ctx.fillStyle = '#333'
    ctx.beginPath()
    ctx.arc(cx, cy, radius, 0, 2 * Math.PI)
    ctx.fill()
    ctx.fillStyle = '#aaa'
    ctx.font = '24px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('暂无选项', cx, cy)
    return
  }

  const total = opts.reduce((sum, o) => sum + o.weight, 0)
  let start = angle

  opts.forEach((opt, i) => {
    const slice = (opt.weight / total) * 2 * Math.PI
    const end = start + slice

    // 绘制扇形
    ctx.beginPath()
    ctx.moveTo(cx, cy)
    ctx.arc(cx, cy, radius, start, end)
    ctx.closePath()
    ctx.fillStyle = getSliceColor(opt, i, opts.length)
    ctx.fill()
    ctx.strokeStyle = '#fff'
    ctx.lineWidth = 2
    ctx.stroke()

    // 绘制标签
    ctx.save()
    ctx.translate(cx, cy)
    ctx.rotate(start + slice / 2)
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 26px "Microsoft YaHei", sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const label = opt.label.length > 6 ? opt.label.slice(0, 5) + '…' : opt.label
    ctx.fillText(label, radius * 0.65, 0)
    ctx.restore()

    start = end
  })

  // 绘制中心圆
  ctx.shadowBlur = 20
  ctx.shadowColor = '#f5b342'
  ctx.beginPath()
  ctx.arc(cx, cy, radius * 0.1, 0, 2 * Math.PI)
  ctx.fillStyle = '#2d1f2a'
  ctx.fill()
  ctx.strokeStyle = '#f5d78a'
  ctx.lineWidth = 3
  ctx.stroke()
  ctx.shadowBlur = 0
}

// 根据选项类型获取颜色
function getSliceColor(opt, index, totalLength) {
  let hue
  if (opt.type === '基础信息') {
    hue = COLORS.BASE.hueStart + (index % 5) * COLORS.BASE.hueStep
    return `hsl(${hue}, ${COLORS.BASE.saturation}%, ${COLORS.BASE.lightness}%)`
  } else if (opt.type === '经历') {
    hue = COLORS.LIFE.hueStart + (index % 5) * COLORS.LIFE.hueStep
    return `hsl(${hue}, ${COLORS.LIFE.saturation}%, ${COLORS.LIFE.lightness}%)`
  } else {
    hue = (index * 360 / totalLength + 20) % 360
    return `hsl(${hue}, ${COLORS.DEFAULT.saturation}%, ${COLORS.DEFAULT.lightness}%)`
  }
}

// ---- 重置到缝隙 ----
function resetToGap() {
  rotation = -Math.PI / 2
  draw(rotation)
}

// 播放旋转音效
function playSpinSound() {
  if (audioSpin) {
    audioSpin.volume = 0.5;
    audioSpin.currentTime = 0
    audioSpin.loop = true
    audioSpin.play().catch(() => {})
  }
}

// 停止旋转音效并播放停止音效
function stopSpinSound() {
  if (audioSpin) {
    audioSpin.pause()
    audioSpin.currentTime = 0
  }
  if (audioStop) {
    audioStop.currentTime = 0
    audioStop.play().catch(() => {})
  }
}

// 计算目标角度
function calculateTargetAngle(targetIndex) {
  const opts = props.options
  if (!opts || opts.length === 0 || targetIndex < 0 || targetIndex >= opts.length) {
    return null
  }
  const total = opts.reduce((s, o) => s + o.weight, 0)
  let cum = 0
  for (let i = 0; i < targetIndex; i++) {
    cum += (opts[i].weight / total) * 2 * Math.PI
  }
  const slice = (opts[targetIndex].weight / total) * 2 * Math.PI
  const randomOffset = (Math.random() - 0.5) * slice * 0.55
  const targetCenter = cum + slice / 2 + randomOffset
  const extraSpins = EXTRA_SPINS_MIN + Math.floor(Math.random() * EXTRA_SPINS_RANGE)
  return -Math.PI / 2 - targetCenter + extraSpins * 2 * Math.PI
}

// 执行旋转动画
function spinToTarget(targetIndex, callback) {
  if (spinning) return

  const targetAngle = calculateTargetAngle(targetIndex)
  if (targetAngle === null) {
    if (callback) callback()
    return
  }

  if (animId) cancelAnimationFrame(animId)
  spinning = true
  playSpinSound()

  const startAngle = rotation
  const duration = ANIMATION_DURATION.min + Math.random() * (ANIMATION_DURATION.max - ANIMATION_DURATION.min)
  const startTime = performance.now()

  const animate = (time) => {
    const elapsed = time - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3) // ease-out cubic
    rotation = startAngle + (targetAngle - startAngle) * eased
    draw(rotation)

    if (progress < 1) {
      animId = requestAnimationFrame(animate)
    } else {
      rotation = targetAngle
      draw(rotation)
      spinning = false
      stopSpinSound()
      if (callback) callback()
    }
  }
  animId = requestAnimationFrame(animate)
}

// 点击转盘触发 spin 事件
function spin() {
  if (!spinning) {
    emit('spin')
  }
}

// 外部调用：直接绘制指定角度
function drawWheel(angle) {
  if (angle === undefined || angle === 0) {
    resetToGap()
  } else {
    rotation = angle
    draw(rotation)
  }
}

defineExpose({ spinToTarget, drawWheel, resetToGap })

onMounted(() => {
  nextTick(() => {
    if (canvas.value) {
      ctx = canvas.value.getContext('2d')
      resetToGap()
    }
  })
})

onBeforeUnmount(() => {
  if (animId) {
    cancelAnimationFrame(animId)
    animId = null
  }
  // 彻底停止并释放音频
  if (audioSpin) {
    audioSpin.pause()
    audioSpin.currentTime = 0
    audioSpin = null
  }
  if (audioStop) {
    audioStop.pause()
    audioStop.currentTime = 0
    audioStop = null
  }
  spinning = false
})

watch(() => props.options, () => {
  if (canvas.value && ctx) {
    resetToGap()
  }
}, { deep: true })
</script>

<style scoped>
.wheel-container {
  position: relative;
  width: 400px;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
}
.pointer {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 12px solid transparent;
  border-right: 12px solid transparent;
  border-top: 28px solid gold;
  filter: drop-shadow(0 0 8px orange);
  z-index: 10;
  pointer-events: none;
}
canvas {
  cursor: pointer;
  width: 100%;
  height: auto;
}
</style>