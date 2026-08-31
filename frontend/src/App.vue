<template>
  <div id="app">
    <h1>⚜️ 重启人生 · {{ currentGroupName }}</h1>

    <div class="custom-container">
      <!-- 历史摘要 -->
      <div class="history-summary" v-if="baseHistory.length">
        <div class="history-row">
          <span class="tag-base">📘 已确定基础：</span>
          <span>{{ baseHistory.join(' + ') }}</span>
        </div>
      </div>
      <div class="history-summary" v-else>
        <div class="history-row" style="color: #6a5a6a;">等待开启命运...</div>
      </div>

      <!-- 游戏进行中 -->
      <div class="top-section" v-if="phase !== 'done'">
        <div v-if="phase === 'base'" class="stage-header">
          <h2>📘 基础信息：{{ currentBaseStage.name }}</h2>
        </div>
        <div v-else-if="phase === 'life'" class="stage-header">
          <h2>📙 人生经历：{{ currentLifeStage.name }}</h2>
        </div>

        <div class="controls-top">
          <el-button type="primary" @click="handleSpin" :disabled="isSpinning" round>
            {{ isSpinning ? '转动中...' : '抽取命运' }}
          </el-button>
          <el-button v-if="!isSpinning && (baseCurrentResult || lifeCurrentResult)" type="success"
            @click="confirmResult" round>
            ✅ 确认此结果
          </el-button>
          <el-button type="info" @click="openManager" round>✏️ 编辑阶段</el-button>
        </div>
      </div>

      <!-- 转盘与属性面板 -->
      <div class="game-stage" v-if="phase !== 'done'">
        <div class="wheel-wrapper">
          <Wheel ref="wheelComp" :options="currentOptions" @spin="handleSpin" />
        </div>
        <AttributeRadar :attributes="characterAttributes" />
      </div>

      <!-- 结果展示 -->
      <div class="bottom-section" v-if="phase !== 'done'">
        <div class="result-area" v-if="baseCurrentResult || lifeCurrentResult">
          <div class="current-result">
            <div class="result-label" v-if="baseCurrentResult">{{ baseCurrentResult.label }}</div>
            <div class="result-label" v-else-if="lifeCurrentResult">{{ lifeCurrentResult.label }}</div>
            <div class="result-desc" v-if="baseCurrentResult">{{ baseCurrentResult.descText }}</div>
            <div class="result-desc" v-else-if="lifeCurrentResult">{{ lifeCurrentResult.descText }}</div>
          </div>
        </div>
      </div>

      <!-- 完成状态 -->
      <div v-else class="complete-msg">
        🎉 人生轨迹已生成！
        <div class="final-summary">
          <div><strong>基础信息：</strong>{{ baseHistory.join(' + ') || '无' }}</div>
          <div><strong>人生经历：</strong>{{ lifeHistory.join(' → ') || '无' }}</div>
        </div>
        <el-button type="danger" @click="resetGame" round>重新开始</el-button>
      </div>
    </div>

    <!-- 阶段管理弹窗 -->
    <StageManager v-model:visible="isManagerVisible" :baseStages="baseStages" :lifeStages="lifeStages" :groups="groups"
      :currentGroupId="currentGroupId" :userId="userId" :attributes="attributes" @save="handleSaveStages"
      @update:currentGroupId="handleSwitchGroup" @addGroup="handleAddGroup" @renameGroup="handleRenameGroup"
      @deleteGroup="handleDeleteGroup" @reloadAttributes="loadAttributes" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { wheelApi } from './api'
import Wheel from './components/Wheel.vue'
import StageManager from './components/StageManager.vue'
import AttributeRadar from './components/AttributeRadar.vue'


// ==================== 常量 ====================
const STORAGE_KEY_GROUP = 'lastGroupId'

// ==================== 状态 ====================
const userId = ref(1)
const isManagerVisible = ref(false)
const groups = ref([])
const currentGroupId = ref(parseInt(localStorage.getItem(STORAGE_KEY_GROUP)) || null)
const baseStages = ref([])
const lifeStages = ref([])
const attributes = ref([])
const characterAttributes = ref({})

const phase = ref('base')
const baseIndex = ref(0)
const lifeIndex = ref(0)
const isSpinning = ref(false)
const baseHistory = ref([])
const lifeHistory = ref([])
const baseCurrentResult = ref(null)
const lifeCurrentResult = ref(null)
const wheelComp = ref(null)
const ATTR_MAX = 100

// ==================== 计算属性 ====================
const currentBaseStage = computed(() => baseStages.value[baseIndex.value] || { name: '无', options: [] })
const currentLifeStage = computed(() => lifeStages.value[lifeIndex.value] || { name: '无', options: [] })
const currentOptions = computed(() => {
  if (phase.value === 'base') return currentBaseStage.value.options || []
  if (phase.value === 'life') return currentLifeStage.value.options || []
  return []
})

const currentGroupName = computed(() => {
  const group = groups.value.find(g => g.id === currentGroupId.value)
  return group ? group.name : '命运轮盘'
})

// ==================== 数据加载 ====================
async function loadGroups() {
  try {
    const res = await wheelApi.getGroups(userId.value)
    groups.value = res.data || []
    if (groups.value.length === 0) {
      currentGroupId.value = null
      baseStages.value = []
      lifeStages.value = []
      return
    }

    const savedId = parseInt(localStorage.getItem(STORAGE_KEY_GROUP))
    const exists = groups.value.some(g => g.id === savedId)
    currentGroupId.value = (savedId && exists) ? savedId : groups.value[0].id

    await loadAllStages(currentGroupId.value)
    await loadAttributes(currentGroupId.value)
  } catch (error) {
    console.error('加载组列表失败:', error)
    ElMessage.error('加载数据失败，请刷新重试')
  }
}

async function loadAttributes(groupId) {
  if (!groupId) {
    attributes.value = []
    return
  }
  try {
    const res = await wheelApi.getAttributes(userId.value, groupId)
    attributes.value = res.data || []
  } catch (error) {
    console.error('加载属性列表失败:', error)
  }
}

async function loadAllStages(groupId) {
  if (!groupId) return
  try {
    const res = await wheelApi.getStages(userId.value, groupId)
    baseStages.value = res.data.base || []
    lifeStages.value = res.data.life || []
    resetGame()
  } catch (error) {
    console.error('加载阶段数据失败:', error)
    baseStages.value = []
    lifeStages.value = []
  }
}

// ==================== 组管理 ====================
async function handleSwitchGroup(newGroupId) {
  currentGroupId.value = newGroupId
  await loadAllStages(newGroupId)
  await loadAttributes(newGroupId)
}

async function handleAddGroup(name) {
  try {
    await wheelApi.createGroup(userId.value, name)
    await loadGroups()
  } catch (error) {
    ElMessage.error('创建失败，请检查输入内容')
  }
}

async function handleRenameGroup({ groupId, newName }) {
  try {
    await wheelApi.renameGroup(userId.value, groupId, newName)
    await loadGroups()
    ElMessage.success('重命名成功！')
  } catch (error) {
    ElMessage.error('重命名失败，请检查输入')
  }
}

async function handleDeleteGroup({ groupId }) {
  try {
    await wheelApi.deleteGroup(userId.value, groupId)
    currentGroupId.value = null
    await loadGroups()
    ElMessage.success('剧本组已成功删除！')
  } catch (error) {
    ElMessage.error('删除失败，请检查网络或日志')
  }
}

// ==================== 阶段保存 ====================
async function handleSaveStages({ baseStages: newBase, lifeStages: newLife }) {
  if (!currentGroupId.value) {
    ElMessage.warning('请先在顶部下拉框中选择或创建一个剧本组！')
    return
  }
  try {
    const payload = { base: newBase, life: newLife }
    await wheelApi.saveStages(userId.value, currentGroupId.value, payload)
    baseStages.value = newBase
    lifeStages.value = newLife
    resetGame()
    ElMessage.success('保存成功！')
  } catch (error) {
    ElMessage.error('保存失败，请检查控制台或网络错误')
  }
}

// ==================== 游戏逻辑 ====================
function weightedRandom(opts) {
  // 先打乱顺序，避免权重相同时总是选中第一个
  const shuffled = [...opts]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
      ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  const totalWeight = shuffled.reduce((sum, opt) => sum + opt.weight, 0)
  let rand = Math.random() * totalWeight
  for (const opt of shuffled) {
    rand -= opt.weight
    if (rand < 0) return opt
  }
  return shuffled[shuffled.length - 1]
}

function handleSpin() {
  if (isSpinning.value || phase.value === 'done' || currentOptions.value.length === 0) return
  isSpinning.value = true

  const selected = weightedRandom(currentOptions.value)
  const idx = currentOptions.value.indexOf(selected)
  if (idx !== -1 && wheelComp.value) {
    wheelComp.value.spinToTarget(idx, () => {
      if (phase.value === 'base') baseCurrentResult.value = selected
      else lifeCurrentResult.value = selected
      isSpinning.value = false
    })
  } else {
    // 理论上不会走到这里，但保底处理
    isSpinning.value = false
  }
}

function confirmResult() {
  const currentPhase = phase.value
  let selected = null

  if (currentPhase === 'base' && baseCurrentResult.value) {
    selected = baseCurrentResult.value
    baseHistory.value.push(selected.label)
    baseCurrentResult.value = null
  } else if (currentPhase === 'life' && lifeCurrentResult.value) {
    selected = lifeCurrentResult.value
    lifeHistory.value.push(selected.label)
    lifeCurrentResult.value = null
  } else {
    return
  }

  console.log('选中选项:', selected.label, 'attributeGains:', selected.attributeGains);
  console.log('累加前 characterAttributes:', JSON.parse(JSON.stringify(characterAttributes.value)));

  // 应用属性增益
  if (selected.attributeGains) {
    Object.entries(selected.attributeGains).forEach(([key, value]) => {
      const oldVal = Number(characterAttributes.value[key] || 0)
      const numValue = Number(value)
      if (!isNaN(numValue)) {
        let newVal = oldVal + numValue
        if (newVal > ATTR_MAX) {
          newVal = ATTR_MAX
          ElMessage.warning(`属性【${key}】已达到上限 ${ATTR_MAX}，溢出部分被忽略`)
        }
        characterAttributes.value[key] = newVal
      }
    })
  }

  console.log('累加后 characterAttributes:', JSON.parse(JSON.stringify(characterAttributes.value)));

  // 根据 nextStageName 跳转
  // 新代码：使用 nextStageId 关联
  const nextStageId = selected.nextStageId
  if (nextStageId) {
    const baseTarget = baseStages.value.findIndex(s => s.id === nextStageId)
    if (baseTarget !== -1) {
      phase.value = 'base'
      baseIndex.value = baseTarget
      resetWheel()
      return
    }
    const lifeTarget = lifeStages.value.findIndex(s => s.id === nextStageId)
    if (lifeTarget !== -1) {
      phase.value = 'life'
      lifeIndex.value = lifeTarget
      resetWheel()
      return
    }
  }

  // 默认顺序推进
  if (currentPhase === 'base') {
    baseIndex.value++
    if (baseIndex.value >= baseStages.value.length) {
      phase.value = 'life'
      lifeIndex.value = 0
    }
  } else {
    lifeIndex.value++
    if (lifeIndex.value >= lifeStages.value.length) {
      phase.value = 'done'
    }
  }
  resetWheel()
}

function resetWheel() {
  if (wheelComp.value) wheelComp.value.resetToGap()
}

function resetGame() {
  baseIndex.value = 0
  lifeIndex.value = 0
  baseHistory.value = []
  lifeHistory.value = []
  baseCurrentResult.value = null
  lifeCurrentResult.value = null
  characterAttributes.value = {}
  phase.value = 'base'
  resetWheel()
}

function openManager() {
  isManagerVisible.value = true
}

// ==================== 监听器 ====================
// 保存当前组ID到本地存储
watch(currentGroupId, (newVal) => {
  if (newVal) {
    localStorage.setItem(STORAGE_KEY_GROUP, String(newVal))
  } else {
    localStorage.removeItem(STORAGE_KEY_GROUP)
  }
})

// 初始加载
onMounted(() => {
  loadGroups()
})
</script>

<style>
body {
  margin: 0;
  background: #0a0710;
}

#app {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-family: 'Segoe UI', sans-serif;
  background: #0a0710;
  color: #e5d4b0;
  min-height: 100vh;
  text-align: center;
}

h1 {
  color: #f5e6b0;
  text-shadow: 0 0 20px #f0b34b;
}

.history-summary {
  width: 100%;
  background: #1f1525;
  border: 1px solid #4a3a3a;
  border-radius: 20px;
  padding: 10px 20px;
  margin-bottom: 15px;
  min-height: 50px;
  text-align: left;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-row {
  font-size: 1.1rem;
  font-weight: 500;
}

.tag-base {
  color: #4a7a9a;
  font-weight: bold;
  margin-right: 8px;
}

.top-section {
  margin: 10px 0 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.stage-header h2 {
  color: #f5e6b0;
  margin: 0;
}

.controls-top {
  display: flex;
  gap: 15px;
  justify-content: center;
  flex-wrap: wrap;
}

.game-stage {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 30px;
  width: 100%;
  max-width: 800px;
  margin: 10px auto;
  flex-wrap: wrap;
}

.bottom-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  width: 100%;
}

.result-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.current-result {
  background: #1d1428;
  border: 1px solid #5f4a3a;
  border-radius: 20px;
  padding: 15px 25px;
  min-width: 200px;
  margin: 0 auto;
}

.result-label {
  font-size: 1.4rem;
  font-weight: bold;
  color: #f5e6b0;
}

.result-desc {
  font-size: 0.9rem;
  color: #bdaa88;
}

.wheel-wrapper {
  position: relative;
  width: 100%;
  max-width: 400px;
  margin: 10px auto;
}

.complete-msg {
  width: 100%;
  background: #2a1a1a;
  padding: 20px;
  border-radius: 20px;
  border: 2px solid #f5c542;
  color: #f5c542;
  font-size: 1.2rem;
  margin-top: 20px;
}

.final-summary {
  margin: 15px 0;
  font-size: 1rem;
  color: #e5d4b0;
}
</style>