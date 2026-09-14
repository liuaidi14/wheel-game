<template>
  <div class="battle-container">
    <el-button class="back-btn" @click="$emit('back')" round>🏠 返回主页</el-button>
    <h1>⚔️ 双人对战 · 命运轮盘</h1>
    <div class="group-bar" v-if="groups.length">
      <span class="group-label">当前剧本：</span>
      <el-select v-model="currentGroupId" @change="handleSwitchGroup" size="small" style="width: 180px;">
        <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <el-button size="small" round @click="openManager">✏️ 编辑剧本</el-button>
      <el-button size="small" round @click="openImportDialog">📋 导入人生剧本</el-button>
      <el-button size="small" round type="warning" plain @click="openTestDialog">🧪 测试模式</el-button>
    </div>
    <!-- 准备阶段 -->
    <div v-if="battleStage === 'prepare' && baseStages.length" class="stage-area">
      <h2 class="stage-header">
        准备阶段 · {{ currentBaseStage.name }}（{{ prepareIndex + 1 }}/{{ baseStages.length }}）：请 {{ currentPlayer.name }} 转动转盘
      </h2>
      <div class="battle-field">
        <!-- 玩家1：属性在左，人物在右 -->
        <div class="fighter-card fighter-left" :class="{ active: currentPlayer.id === 1, dead: players[0].hp <= 0 }">
          <div class="fighter-top">
            <Wheel ref="wheel1" :options="player1Options" @spin="() => handleSpinAttempt(1)" class="mini-wheel" />
            <div class="casino-dice" v-if="players[0].skill?.casino">
              <div class="casino-label" v-if="battleStage === 'battle' && isCasinoTurn(players[0], players[0].turnCount + 1)">🎰 开始抽奖</div>
              <div class="casino-dice-row">
                <div v-for="(d, i) in players[0].dice" :key="i + '-' + d" class="die" :class="{ active: players[0].boostTurns > 0 }">{{ d || '?' }}</div>
              </div>
            </div>
          </div>
          
          <!-- 术式介绍（完整显示） -->
          <div class="fighter-skill-area" v-if="players[0].skill">
            <div class="fighter-skill-name">{{ players[0].skill.name }}</div>
            <div class="fighter-skill-desc">{{ players[0].skill.desc }}</div>
          </div>
          <el-progress v-if="battleStage === 'battle'" :percentage="hpPercent(players[0])" :color="hpColor(players[0])" class="hp-bar" />
          <div class="fighter-body">
            <div class="fighter-stats">
              <AttributeRadar :attributes="players[0].characterAttributes" class="mini-radar" radius="60%" />
              <div class="attrs">
                <span>HP: {{ players[0].hp }}/{{ players[0].maxHp }}</span>
                <span>攻: {{ players[0].attack }}</span>
                <span>防: {{ players[0].defense }}</span>
                <span v-if="players[0].status">状态: {{ statusText(players[0].status) }}</span>
              </div>
            </div>
            <div class="fighter-char">
              <Stickman color="red" :action="getAnim(players[0])" :width="100" :height="150" />
              <p class="fighter-name">玩家1</p>
            </div>
          </div>
        </div>
        <!-- 玩家2：人物在左，属性在右 -->
        <div class="fighter-card fighter-right" :class="{ active: currentPlayer.id === 2, dead: players[1].hp <= 0 }">
          <div class="fighter-top">
            <Wheel ref="wheel2" :options="player2Options" @spin="() => handleSpinAttempt(2)" class="mini-wheel" />
            <div class="casino-dice" v-if="players[1].skill?.casino">
              <div class="casino-label" v-if="battleStage === 'battle' && isCasinoTurn(players[1], players[1].turnCount + 1)">🎰 开始抽奖</div>
              <div class="casino-dice-row">
                <div v-for="(d, i) in players[1].dice" :key="i + '-' + d" class="die" :class="{ active: players[1].boostTurns > 0 }">{{ d || '?' }}</div>
              </div>
            </div>
          </div>
          
          <!-- 术式介绍（完整显示） -->
          <div class="fighter-skill-area" v-if="players[1].skill">
            <div class="fighter-skill-name">{{ players[1].skill.name }}</div>
            <div class="fighter-skill-desc">{{ players[1].skill.desc }}</div>
          </div>
          <el-progress v-if="battleStage === 'battle'" :percentage="hpPercent(players[1])" :color="hpColor(players[1])" class="hp-bar" />
          <div class="fighter-body">
            <div class="fighter-char">
              <Stickman color="blue" :action="getAnim(players[1])" flip :width="100" :height="150" />
              <p class="fighter-name">玩家2</p>
            </div>
            <div class="fighter-stats">
              <AttributeRadar :attributes="players[1].characterAttributes" class="mini-radar" radius="60%" />
              <div class="attrs">
                <span>HP: {{ players[1].hp }}/{{ players[1].maxHp }}</span>
                <span>攻: {{ players[1].attack }}</span>
                <span>防: {{ players[1].defense }}</span>
                <span v-if="players[1].status">状态: {{ statusText(players[1].status) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
       
      <p class="hint">点击自己头顶的转盘来随机属性</p>
    </div>
    <!-- 战斗阶段（复用相同的布局） -->
    <div v-else-if="battleStage === 'battle'" class="stage-area">
      <h2 class="stage-header">战斗阶段 · 第{{ turnNumber }}回合 · {{ currentPlayer.name }} 的回合</h2>
      
      <div class="battle-field">
        <div class="fighter-card fighter-left" :class="{ active: currentPlayer.id === 1, dead: players[0].hp <= 0 }">
          <div class="fighter-top">
            <Wheel ref="wheel1" :options="player1Options" @spin="() => handleSpinAttempt(1)" class="mini-wheel" />
            <div class="casino-dice" v-if="players[0].skill?.casino">
              <div class="casino-label" v-if="battleStage === 'battle' && isCasinoTurn(players[0], players[0].turnCount + 1)">🎰 开始抽奖</div>
              <div class="casino-dice-row">
                <div v-for="(d, i) in players[0].dice" :key="i + '-' + d" class="die" :class="{ active: players[0].boostTurns > 0 }">{{ d || '?' }}</div>
              </div>
            </div>
          </div>
          <div class="fighter-skill-area" v-if="players[0].skill">
            <div class="fighter-skill-name">{{ players[0].skill.name }}</div>
            <div class="fighter-skill-desc">{{ players[0].skill.desc }}</div>
          </div>
          <el-progress :percentage="hpPercent(players[0])" :color="hpColor(players[0])" class="hp-bar" />
          <div class="fighter-body">
            <div class="fighter-stats">
              <AttributeRadar :attributes="players[0].characterAttributes" class="mini-radar" radius="60%" />
              <div class="attrs">
                <span>HP: {{ players[0].hp }}/{{ players[0].maxHp }}</span>
                <span>攻: {{ players[0].attack }}</span>
                <span>防: {{ players[0].defense }}</span>
                <span v-if="players[0].status">状态: {{ statusText(players[0].status) }}</span>
              </div>
            </div>
            <div class="fighter-char">
              <Stickman color="red" :action="getAnim(players[0])" :width="100" :height="150" />
              <p class="fighter-name">玩家1</p>
            </div>
          </div>
        </div>
        <div class="fighter-card fighter-right" :class="{ active: currentPlayer.id === 2, dead: players[1].hp <= 0 }">
          <div class="fighter-top">
            <Wheel ref="wheel2" :options="player2Options" @spin="() => handleSpinAttempt(2)" class="mini-wheel" />
            <div class="casino-dice" v-if="players[1].skill?.casino">
              <div class="casino-label" v-if="battleStage === 'battle' && isCasinoTurn(players[1], players[1].turnCount + 1)">🎰 开始抽奖</div>
              <div class="casino-dice-row">
                <div v-for="(d, i) in players[1].dice" :key="i + '-' + d" class="die" :class="{ active: players[1].boostTurns > 0 }">{{ d || '?' }}</div>
              </div>
            </div>
          </div>
          <div class="fighter-skill-area" v-if="players[1].skill">
            <div class="fighter-skill-name">{{ players[1].skill.name }}</div>
            <div class="fighter-skill-desc">{{ players[1].skill.desc }}</div>
          </div>
          <el-progress :percentage="hpPercent(players[1])" :color="hpColor(players[1])" class="hp-bar" />
          <div class="fighter-body">
            <div class="fighter-char">
              <Stickman color="blue" :action="getAnim(players[1])" flip :width="100" :height="150" />
              <p class="fighter-name">玩家2</p>
            </div>
            <div class="fighter-stats">
              <AttributeRadar :attributes="players[1].characterAttributes" class="mini-radar" radius="60%" />
              <div class="attrs">
                <span>HP: {{ players[1].hp }}/{{ players[1].maxHp }}</span>
                <span>攻: {{ players[1].attack }}</span>
                <span>防: {{ players[1].defense }}</span>
                <span v-if="players[1].status">状态: {{ statusText(players[1].status) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="battle-toolbar">
        <el-button size="small" round :type="autoBattle ? 'danger' : 'success'" @click="toggleAutoBattle">
          {{ autoBattle ? '⏸ 停止自动战斗' : '⚡ 自动战斗' }}
        </el-button>
        <el-button size="small" round @click="openTestDialog">🧪 测试模式</el-button>
      </div>
      <div class="battle-log">
        <div class="history-summary">
          <span>{{ logs[logs.length - 1] || '战斗开始！' }}</span>
        </div>
      </div>
      <div v-if="gameOver" class="game-over">
        <h3>{{ winner.name }} 获胜！</h3>
        <el-button type="danger" round @click="resetBattle">重新开始</el-button>
      </div>
    </div>
    <StageManager v-model:visible="isManagerVisible" :baseStages="baseStages" :lifeStages="[]"
      :groups="groups" :currentGroupId="currentGroupId" :userId="userId" :attributes="attributes"
      @save="handleSaveStages" @update:currentGroupId="handleSwitchGroup" @addGroup="handleAddGroup"
      @renameGroup="handleRenameGroup" @deleteGroup="handleDeleteGroup" @reloadAttributes="loadAttributes" />
    <el-dialog v-model="showImportDialog" title="📋 导入人生剧本配置" width="420px">
      <p style="color:#bdaa88;margin-bottom:12px;line-height:1.6;">
        选择一个已有的剧本组，将其基础阶段、选项和属性定义<strong style="color:#f5c542;">一键复制</strong>为新的对战剧本。<br/>
        新剧本独立保存，<strong style="color:#f5c542;">修改不影响原剧本</strong>。
      </p>
      <el-select v-model="importSourceGroupId" placeholder="选择要导入的剧本组" style="width:100%;">
        <el-option v-for="g in importableGroups" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="doImport" :disabled="!importSourceGroupId">确认导入</el-button>
      </template>
    </el-dialog>
    <!-- 测试模式对话框：快速指定术式+属性，跳过准备阶段直接开打 -->
    <el-dialog v-model="testDialogVisible" title="🧪 测试模式" width="460px">
      <p style="color:#bdaa88;margin-bottom:14px;line-height:1.6;">
        跳过属性转盘和术式抽取，<strong style="color:#f5c542;">直接指定双方术式</strong>开始战斗，配合「⚡ 自动战斗」快速验证机制。
      </p>
      <div style="display:flex;flex-direction:column;gap:12px;">
        <div>
          <div style="color:#f5e6b0;margin-bottom:6px;font-size:13px;">🔴 玩家1 术式</div>
          <el-select v-model="testSkill1" style="width:100%;">
            <el-option v-for="s in testSkillOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </div>
        <div>
          <div style="color:#f5e6b0;margin-bottom:6px;font-size:13px;">🔵 玩家2 术式</div>
          <el-select v-model="testSkill2" style="width:100%;">
            <el-option v-for="s in testSkillOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </div>
        <div>
          <div style="color:#f5e6b0;margin-bottom:6px;font-size:13px;">属性预设</div>
          <el-radio-group v-model="testAttrPreset">
            <el-radio :label="'max'">全满 25（黑闪/闪避拉满）</el-radio>
            <el-radio :label="'balanced'">均衡 15</el-radio>
            <el-radio :label="'plain'">白板 0</el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <el-button @click="testDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="startTestBattle">开始测试战斗</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { wheelApi } from '../api'
import Wheel from './Wheel.vue'
import Stickman from './Stickman.vue'
import StageManager from './StageManager.vue'
import AttributeRadar from './AttributeRadar.vue'
const sleep = ms => new Promise(r => setTimeout(r, ms))
// 自动战斗时加速等待（约8倍速），手动战斗保持原节奏
function wait(ms) { return sleep(autoBattle.value ? Math.max(120, Math.floor(ms / 8)) : ms) }
// ==================== 常量 ====================
const STORAGE_KEY = 'lastBattleGroupId'
const BASE_HP = 500
const BASE_ATK = 10
const BASE_DEF = 5
// ==================== 玩家状态 ====================
const players = reactive([
    { id: 1, name: '玩家1', maxHp: BASE_HP, attack: BASE_ATK, defense: BASE_DEF, hp: BASE_HP, status: '', animation: 'idle', characterAttributes: {}, stunTurns: 0, dice: [0, 0, 0], boostTurns: 0, attackBoost: 1, casinoRolled: false },
    { id: 2, name: '玩家2', maxHp: BASE_HP, attack: BASE_ATK, defense: BASE_DEF, hp: BASE_HP, status: '', animation: 'idle', characterAttributes: {}, stunTurns: 0, dice: [0, 0, 0], boostTurns: 0, attackBoost: 1, casinoRolled: false }
])
// ==================== 战斗状态 ====================
const battleStage = ref('prepare')
const currentPlayerIndex = ref(0)
const currentPlayer = computed(() => players[currentPlayerIndex.value])
const isSpinning = ref(false)
const logs = ref([])
// 黑闪连击：按玩家独立记录（玩家1/玩家2各自累计，互不清零，实现"连着第二次×2.25"）
const blackFlashStreakByPlayer = reactive({ 1: 0, 2: 0 })
const turnNumber = ref(1)
const gameOver = ref(false)
const winner = ref(null)
// ==================== 测试模式 ====================
const testDialogVisible = ref(false)
const testSkill1 = ref('无术式')
const testSkill2 = ref('无术式')
const testAttrPreset = ref('max')
const autoBattle = ref(false)
const testSkillOptions = computed(() => ['无术式', ...Object.keys(SKILLS)])
// ==================== 剧本组 & 阶段 & 属性（动态加载） ====================
const userId = ref(1)
const groups = ref([])
const currentGroupId = ref(parseInt(localStorage.getItem(STORAGE_KEY)) || null)
const baseStages = ref([])
const attributes = ref([])
const prepareIndex = ref(0)
const isManagerVisible = ref(false)
const showImportDialog = ref(false)
const importSourceGroupId = ref(null)
const allGroups = ref([])
const importableGroups = computed(() => allGroups.value.filter(g => g.id !== currentGroupId.value))
const currentBaseStage = computed(() => baseStages.value[prepareIndex.value] || { name: '无', options: [] })
// 战斗动作选项
const battleOptions = ref([
    { label: '攻击', weight: 55, descText: '造成攻击伤害', effect: 'normal' },
    { label: '防御', weight: 30, descText: '本回合只受30%伤害', effect: 'defend' },
    { label: '闪避', weight: 15, descText: '本回合闪避率+40%', effect: 'dodge' }
])
// ==================== 技能系统 ====================
const SKILLS = {
    '十种影法术': { id: 'shikigami', name: '十种影法术', attackType: 'melee', desc: '召唤两个式神：攻击式神随主人普攻额外造成50%伤害，敌人眩晕时自动追击造成100%伤害；防御式神在主人防御成功后眩晕攻击者1回合', shikigamiAttack: true, shikigamiDefend: true, shikigamiAtkMul: 0.5 },
    '十划咒法': { id: 'shihua', name: '十划咒法', attackType: 'melee', desc: '攻击时90%概率命中制造的弱点，造成150%伤害；10%未命中则为普通伤害', weakPointChance: 0.9, weakPointMul: 1.5 },
    '奇迹': { id: 'miracle', name: '奇迹', attackType: 'melee', desc: '被动：闪避成功后获得1颗星；受到致死伤害时，1颗星抵消1次致死伤害', dodgeStar: true },
    '黑鸟操术': { id: 'blackbird', name: '黑鸟操术', attackType: 'melee', desc: '每3回合自动召唤黑鸟攻击敌人，造成两段50%攻击力伤害', summonEveryTurns: 3, summonAtkMul: 0.5 },
    '刍灵咒法': { id: 'chuling', name: '刍灵咒法', attackType: 'ranged', desc: '远程攻击，命中5次后自动300%攻击力伤害（不占回合）', autoUltHits: 5, autoUltMul: 3.0 },
    '咒言术': { id: 'cursed-speech', name: '咒言术', attackType: 'melee', desc: '每4回合发动一次咒言：转盘变为咒言盘（不许动/攻击自己/炸裂吧 6:2:4），不许动眩晕敌人1回合；攻击自己令敌人下回合伤害反噬自身；炸裂吧造成150%伤害', cursedSpeech: true, cursedSpeechEveryTurns: 4 },
    '坐杀博徒': { id: 'casino', name: '坐杀博徒', attackType: 'melee', desc: '前5回合正常战斗，第6回合起进入抽奖模式连抽3回合（6/7/8）：每回合行动前自动掷3骰——三不同=小奖（回5%血/攻击110%持续1回合）；两同=中奖（回10%血/攻击120%持续1回合）；三同=大奖（回20%血/攻击150%持续3回合）；抽奖结束后隔6回合再抽', casino: true, casinoEveryTurns: 6, casinoDuration: 3 }
}
// 咒言术专用转盘：每4回合替换普通战斗转盘
const cursedSpeechOptions = ref([
    { label: '不许动', weight: 6, descText: '眩晕敌人1回合', effect: 'stunSpeech' },
    { label: '攻击自己', weight: 2, descText: '敌人下回合伤害反噬自身', effect: 'backfireSpeech' },
    { label: '炸裂吧', weight: 4, descText: '敌人受到150%伤害', effect: 'burstSpeech' }
])
// 奇迹术式加强：防御与闪避占比互换（防御30→15，闪避15→30），更易触发闪避获星
// 按玩家各自计算，两个转盘互不影响（只有奇迹玩家的转盘变化）
function battleOptionsForPlayer(player) {
    if (battleStage.value === 'prepare') return currentBaseStage.value.options || []
    if (player?.skill?.id === 'miracle') {
        return battleOptions.value.map(o => {
            if (o.effect === 'defend') return { ...o, weight: 15 }
            if (o.effect === 'dodge') return { ...o, weight: 30 }
            return o
        })
    }
    // 咒言术：先正常打3回合，第4回合（及第8、12…）发动咒言，转盘换成咒言盘
    if (player?.skill?.cursedSpeech && player.turnCount % player.skill.cursedSpeechEveryTurns === 0) {
        return cursedSpeechOptions.value
    }
    return battleOptions.value
}
const player1Options = computed(() => battleOptionsForPlayer(players[0]))
const player2Options = computed(() => battleOptionsForPlayer(players[1]))
const wheel1 = ref(null)
const wheel2 = ref(null)
// ==================== 动态属性获取 ====================
function getAttrVal(player, key) {
    return Number(player.characterAttributes[key]) || 0
}
function initPlayerAttributesFromDefinitions() {
    if (!attributes.value.length) return
    players.forEach(p => {
        attributes.value.forEach(attr => {
            const attrName = attr.name || attr.key
            if (attrName && p.characterAttributes[attrName] === undefined) {
                p.characterAttributes[attrName] = 0
            }
        })
        recalcBattleStats(p)
    })
}
function recalcBattleStats(player) {
    const life = getAttrVal(player, '生命')
    const atk = getAttrVal(player, '攻击')
    const def = getAttrVal(player, '防御')
    player.maxHp = BASE_HP + life * 8
    player.attack = BASE_ATK + atk * 4
    player.defense = BASE_DEF + def * 1
}
function applyAttributeGain(player, gains) {
    Object.entries(gains || {}).forEach(([key, value]) => {
        const num = Number(value)
        if (!isNaN(num)) {
            const old = Number(player.characterAttributes[key] || 0)
            player.characterAttributes[key] = old + num
        }
    })
    recalcBattleStats(player)
}
// ==================== 数据加载 ====================
async function loadGroups() {
    try {
        const res = await wheelApi.getGroups(userId.value, 'battle')
        groups.value = res.data || []
        if (groups.value.length === 0) {
            currentGroupId.value = null
            baseStages.value = []
            attributes.value = []
            return
        }
        const savedId = parseInt(localStorage.getItem(STORAGE_KEY))
        const exists = groups.value.some(g => g.id === savedId)
        currentGroupId.value = (savedId && exists) ? savedId : groups.value[0].id
        await loadBaseStages(currentGroupId.value)
        await loadAttributes(currentGroupId.value)
    } catch (error) {
        console.error('加载对战剧本组失败:', error)
        ElMessage.error('加载数据失败，请刷新重试')
    }
}
async function loadBaseStages(groupId) {
    if (!groupId) { baseStages.value = []; return }
    try {
        const res = await wheelApi.getStages(userId.value, groupId)
        baseStages.value = res.data.base || []
        resetBattle()
    } catch (error) { baseStages.value = [] }
}
async function loadAttributes(groupId) {
    if (!groupId) { attributes.value = []; return }
    try {
        const res = await wheelApi.getAttributes(userId.value, groupId)
        attributes.value = res.data || []
        initPlayerAttributesFromDefinitions()
    } catch (error) { console.error('加载属性列表失败:', error) }
}
// ==================== 剧本组管理 ====================
async function handleSwitchGroup(newGroupId) {
    currentGroupId.value = newGroupId
    await loadBaseStages(newGroupId)
    await loadAttributes(newGroupId)
}
async function handleAddGroup(name) {
    try {
        await wheelApi.createGroup(userId.value, name, 'battle')
        await loadGroups()
        let newGroup = null
        if (groups.value.length > 0) newGroup = groups.value.reduce((a, b) => (a.id > b.id ? a : b))
        if (newGroup) {
            currentGroupId.value = newGroup.id
            await loadBaseStages(newGroup.id)
            await loadAttributes(newGroup.id)
        }
        ElMessage.success('对战剧本创建成功！')
    } catch (error) { ElMessage.error('创建失败，请检查输入内容') }
}
async function handleRenameGroup({ groupId, newName }) {
    try {
        await wheelApi.renameGroup(userId.value, groupId, newName)
        await loadGroups()
        ElMessage.success('重命名成功！')
    } catch (error) { ElMessage.error('重命名失败') }
}
async function handleDeleteGroup({ groupId }) {
    try {
        await wheelApi.deleteGroup(userId.value, groupId)
        currentGroupId.value = null
        await loadGroups()
        ElMessage.success('对战剧本已删除！')
    } catch (error) { ElMessage.error('删除失败') }
}
async function handleSaveStages({ baseStages: newBase }) {
    if (!currentGroupId.value) { ElMessage.warning('请先选择或创建一个对战剧本！'); return }
    try {
        await wheelApi.saveStages(userId.value, currentGroupId.value, { base: newBase, life: [] })
        baseStages.value = newBase
        resetBattle()
        ElMessage.success('保存成功！')
    } catch (error) { ElMessage.error('保存失败') }
}
function openManager() { isManagerVisible.value = true }
async function openImportDialog() {
    try {
        const res = await wheelApi.getGroups(userId.value)
        allGroups.value = res.data || []
        importSourceGroupId.value = null
        showImportDialog.value = true
    } catch (error) { ElMessage.error('加载剧本列表失败') }
}
async function doImport() {
    if (!importSourceGroupId.value) { ElMessage.warning('请选择要导入的剧本组'); return }
    try {
        const sourceId = importSourceGroupId.value
        const sourceGroup = allGroups.value.find(g => g.id === sourceId)
        const targetName = sourceGroup ? `${sourceGroup.name}（对战版）` : '导入的对战剧本'
        const res = await wheelApi.copyGroup(userId.value, sourceId, targetName, 'battle')
        const newGroup = res.data
        await loadGroups()
        if (groups.value.some(g => g.id === newGroup.id)) currentGroupId.value = newGroup.id
        showImportDialog.value = false
        ElMessage.success('导入成功！')
    } catch (error) { ElMessage.error('导入失败：' + (error.response?.data?.message || error.message)) }
}
// ==================== 战斗逻辑 ====================
// ==================== 测试模式：快速指定术式+属性开打 ====================
function openTestDialog() {
    testDialogVisible.value = true
}
function startTestBattle() {
    if (testSkill1.value === '无术式' && testSkill2.value === '无术式') {
        ElMessage.warning('请至少给一方选择术式')
        return
    }
    const presetAttrs = {
        max: { '生命': 25, '攻击': 25, '防御': 25, '速度': 25, '手感': 25, '体质': 25 },
        balanced: { '生命': 15, '攻击': 15, '防御': 15, '速度': 15, '手感': 15, '体质': 15 },
        plain: { '生命': 0, '攻击': 0, '防御': 0, '速度': 0, '手感': 0, '体质': 0 }
    }
    const attrs = presetAttrs[testAttrPreset.value] || presetAttrs.max
    players.forEach((p, i) => {
        p.characterAttributes = { ...attrs }
        p.skill = (i === 0 ? SKILLS[testSkill1.value] : SKILLS[testSkill2.value]) || null
        p.hitCount = 0
        p.ultReady = false
        p.stars = 0
        p.turnCount = 0
        p.stunTurns = 0
        p.backfire = false
        p.dice = [0, 0, 0]
        p.boostTurns = 0
        p.attackBoost = 1
        p.casinoRolled = false
        p.status = ''
        p.animation = 'idle'
        recalcBattleStats(p)
        p.hp = p.maxHp
    })
    blackFlashStreakByPlayer[1] = 0
    blackFlashStreakByPlayer[2] = 0
    turnNumber.value = 1
    battleStage.value = 'battle'
    prepareIndex.value = 0
    gameOver.value = false
    winner.value = null
    logs.value = [`🧪 测试模式：${players[0].skill ? players[0].skill.name : '无术式'} vs ${players[1].skill ? players[1].skill.name : '无术式'}（属性${testAttrPreset.value === 'max' ? '全满25' : testAttrPreset.value === 'balanced' ? '均衡15' : '白板0'}）`]
    const p1Speed = getAttrVal(players[0], '速度')
    const p2Speed = getAttrVal(players[1], '速度')
    currentPlayerIndex.value = p2Speed > p1Speed ? 1 : 0
    logs.value.push(currentPlayerIndex.value === 1 ? `${players[1].name} 速度更快，先手攻击！` : `${players[0].name} 速度更快，先手攻击！`)
    testDialogVisible.value = false
    resetBothWheels()
    ElMessage.success('测试战斗开始！可点击「⚡ 自动战斗」快速跑完')
}
// 自动战斗：循环执行动作直到分出胜负或手动停止
async function toggleAutoBattle() {
    if (autoBattle.value) { autoBattle.value = false; return }
    autoBattle.value = true
    while (autoBattle.value && !gameOver.value) {
        const options = currentPlayer.value.id === 1 ? player1Options.value : player2Options.value
        if (!options.length) break
        const selected = weightedRandom(options)
        await executeAction(selected)
    }
    autoBattle.value = false
}
// ==================== 程序化音效（Web Audio 合成，无需音频文件） ====================
let audioCtx = null
function getAudioCtx() {
    if (autoBattle.value) return null  // 自动战斗快速跑时静音，避免音效重叠轰炸
    if (!audioCtx) {
        try { audioCtx = new (window.AudioContext || window.webkitAudioContext)() } catch (e) { return null }
    }
    if (audioCtx.state === 'suspended') audioCtx.resume().catch(() => {})
    return audioCtx
}
function playTone(freq, dur, type = 'sine', vol = 0.2, delay = 0) {
    const ctx = getAudioCtx()
    if (!ctx) return
    try {
        const t = ctx.currentTime + delay
        const osc = ctx.createOscillator()
        const gain = ctx.createGain()
        osc.type = type
        osc.frequency.setValueAtTime(freq, t)
        gain.gain.setValueAtTime(0.0001, t)
        gain.gain.exponentialRampToValueAtTime(vol, t + 0.012)
        gain.gain.exponentialRampToValueAtTime(0.0001, t + dur)
        osc.connect(gain).connect(ctx.destination)
        osc.start(t)
        osc.stop(t + dur + 0.05)
    } catch (e) { /* 音效错误忽略 */ }
}
// 骰子弹出：短促嗒声
function sfxDice() { playTone(500 + Math.random() * 300, 0.07, 'square', 0.12) }
// 中奖：上行三音
function sfxWin() {
    playTone(523, 0.12, 'sine', 0.18)
    playTone(659, 0.12, 'sine', 0.18, 0.11)
    playTone(784, 0.25, 'sine', 0.2, 0.22)
}
// 大奖：四音琶音
function sfxBigWin() {
    playTone(523, 0.1, 'sine', 0.18)
    playTone(659, 0.1, 'sine', 0.18, 0.09)
    playTone(784, 0.1, 'sine', 0.18, 0.18)
    playTone(1047, 0.4, 'sine', 0.22, 0.27)
}
// 黑闪：低频轰鸣 + 噪声爆发
function sfxBlackFlash() {
    const ctx = getAudioCtx()
    if (!ctx) return
    try {
        const t = ctx.currentTime
        const osc = ctx.createOscillator()
        const gain = ctx.createGain()
        osc.type = 'sawtooth'
        osc.frequency.setValueAtTime(140, t)
        osc.frequency.exponentialRampToValueAtTime(38, t + 0.28)
        gain.gain.setValueAtTime(0.28, t)
        gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.32)
        osc.connect(gain).connect(ctx.destination)
        osc.start(t); osc.stop(t + 0.36)
        const buf = ctx.createBuffer(1, Math.floor(ctx.sampleRate * 0.18), ctx.sampleRate)
        const data = buf.getChannelData(0)
        for (let i = 0; i < data.length; i++) data[i] = (Math.random() * 2 - 1) * (1 - i / data.length)
        const src = ctx.createBufferSource()
        src.buffer = buf
        const ng = ctx.createGain()
        ng.gain.setValueAtTime(0.22, t)
        ng.gain.exponentialRampToValueAtTime(0.0001, t + 0.18)
        src.connect(ng).connect(ctx.destination)
        src.start(t)
    } catch (e) { /* 音效错误忽略 */ }
}
// ==================== 坐杀博徒：掷骰抽奖 ====================
// 抽奖回合判定：第6回合起连抽3回合（6/7/8），术式结束后隔6回合再抽（15/16/17、24/25/26…）
// n = 本次行动次数（行动前调用传 turnCount+1；行动中 turnCount++ 后传 turnCount）
function isCasinoTurn(player, n) {
    if (!player?.skill?.casino) return false
    const every = player.skill.casinoEveryTurns
    const dur = player.skill.casinoDuration || 3
    if (n < every) return false
    return n % (every + dur) >= every
}
// 掷骰：3个骰子逐个弹出，出完再判定奖级
async function rollCasino(player) {
    player.casinoRolled = true  // 先置标记，防止转盘转动期间重复触发
    player.dice = [0, 0, 0]
    const results = [0, 1, 2].map(() => Math.floor(Math.random() * 6) + 1)
    for (let i = 0; i < 3; i++) {
        await wait(350)
        player.dice[i] = results[i]
        sfxDice()
    }
    const unique = new Set(player.dice).size
    let tier, healPct, boostMul, boostTurns
    if (unique === 1) { tier = '🎰 大奖'; healPct = 0.2; boostMul = 1.5; boostTurns = 3; sfxBigWin() }
    else if (unique === 2) { tier = '🎉 中奖'; healPct = 0.1; boostMul = 1.2; boostTurns = 1; sfxWin() }
    else { tier = '🎲 小奖'; healPct = 0.05; boostMul = 1.1; boostTurns = 1 }
    const heal = Math.floor(player.maxHp * healPct)
    player.hp = Math.min(player.maxHp, player.hp + heal)
    player.attackBoost = boostMul
    player.boostTurns = boostTurns
    triggerAnimation(player, 'heal')
    logs.value.push(`${tier}！${player.name} 掷骰【${player.dice.join(' ')}】→ 恢复 ${heal} 血，攻击力 ${Math.round(boostMul * 100)}%（持续${boostTurns}回合）`)
}
// buff 回合衰减：行动结束后剩余回合-1，归零恢复攻击力
function decayBoost(player) {
    if (player.boostTurns > 0) {
        player.boostTurns--
        if (player.boostTurns <= 0) player.attackBoost = 1
    }
}
function handleSpinAttempt(playerId) {
    if (isSpinning.value || currentPlayer.value.id !== playerId || gameOver.value || autoBattle.value) return
    isSpinning.value = true
    const options = playerId === 1 ? player1Options.value : player2Options.value
    if (options.length === 0) { isSpinning.value = false; ElMessage.warning('当前阶段没有可选项'); return }
    // 坐杀博徒：战斗阶段抽奖回合，转盘转动前自动掷骰
    if (battleStage.value === 'battle' && isCasinoTurn(currentPlayer.value, currentPlayer.value.turnCount + 1) && !currentPlayer.value.casinoRolled) {
        rollCasino(currentPlayer.value)
    }
    const selected = weightedRandom(options)
    const idx = options.indexOf(selected)
    const wheel = playerId === 1 ? wheel1.value : wheel2.value
    if (wheel) {
        wheel.spinToTarget(idx, async () => {
            if (battleStage.value === 'prepare') {
                applyAttributeGain(currentPlayer.value, selected.attributeGains || {})
                if (SKILLS[selected.label]) {
                    currentPlayer.value.skill = SKILLS[selected.label]
                    currentPlayer.value.hitCount = 0
                    currentPlayer.value.ultReady = false
                    ElMessage.success(`${currentPlayer.value.name} 觉醒术式：${selected.label}！`)
                } else {
                    ElMessage.success(`${currentPlayer.value.name} 获得：${selected.label}`)
                }
                isSpinning.value = false
                if (currentPlayerIndex.value === 0) {
                    currentPlayerIndex.value = 1
                } else {
                    currentPlayerIndex.value = 0
                    prepareIndex.value++
                    if (prepareIndex.value >= baseStages.value.length) {
                        battleStage.value = 'battle'
                        players.forEach(p => { p.hp = p.maxHp })
                        logs.value.push('战斗开始！')
                        const p1Speed = getAttrVal(players[0], '速度')
                        const p2Speed = getAttrVal(players[1], '速度')
                        if (p2Speed > p1Speed) {
                            currentPlayerIndex.value = 1
                            logs.value.push(`${players[1].name} 速度更快，先手攻击！`)
                        } else {
                            currentPlayerIndex.value = 0
                            logs.value.push(`${players[0].name} 速度更快，先手攻击！`)
                        }
                    }
                }
                resetBothWheels()
            } else {
                await executeAction(selected)
                isSpinning.value = false
            }
        })
    } else { isSpinning.value = false }
}
// ==================== 统一扣血：所有伤害都走这里，奇迹之星可抵消致死 ====================
function applyDamage(player, amount) {
    let dmg = Math.max(0, Math.floor(amount))
    if (player.hp - dmg <= 0 && player.stars > 0) {
        player.stars--
        dmg = 0
        return { dmg, logPart: ` ⭐ ${player.name} 用奇迹之星抵消致死伤害！` }
    }
    player.hp = Math.max(0, player.hp - dmg)
    return { dmg, logPart: '' }
}
function processDamage(attacker, defender, baseDamage, opts = {}) {
    let damage = baseDamage
    let logPart = ''
    const atkType = opts.atkType || 'melee'
    // 黑闪判定开关：只有普通攻击动作（近战/远程）可触发，术式伤害（咒言炸裂吧等）不触发
    const canBlackFlash = opts.allowBlackFlash !== false
    if (defender.status === 'dodge' && Math.random() < getDodgeRate(defender, atkType)) {
        triggerAnimation(attacker, atkType)
        triggerAnimation(defender, 'dodge')
        if (canBlackFlash) blackFlashStreakByPlayer[attacker.id] = 0  // 攻击被闪避=未命中，攻击者黑闪连击断开
        logPart += ` ${defender.name} 闪避了攻击！`
        if (defender.skill?.dodgeStar) {
            defender.stars++
            logPart += ` ⭐ 获得1颗奇迹之星（共${defender.stars}颗）`
        }
        defender.status = ''  // 闪避成功后清除闪避状态，修复持续闪避bug
        return { damage: 0, logPart }
    }
    // 闪避失败也清除闪避状态
    if (defender.status === 'dodge') defender.status = ''
    if (canBlackFlash) {
        const bfRate = Math.max(0, getBlackFlashRate(attacker) - getAntiBlackFlashRate(defender))
        if (Math.random() < bfRate) {
            blackFlashStreakByPlayer[attacker.id]++
            const streak = blackFlashStreakByPlayer[attacker.id]
            const bfMul = Math.pow(1.5, streak)
            damage = Math.floor(damage * bfMul)
            const bfHeal = Math.floor(damage * 0.1)
            attacker.hp = Math.min(attacker.maxHp, attacker.hp + bfHeal)
            sfxBlackFlash()
            logPart += ` ⚡ 黑闪×${streak}（${bfMul.toFixed(2)}倍），吸血${bfHeal}`
        } else {
            blackFlashStreakByPlayer[attacker.id] = 0  // 黑闪未触发，该玩家连击清零
        }
    }
    if (defender.status === 'defend') {
        if (damage > 0) {
            damage = Math.floor(damage * 0.3)
            logPart += `（防御只受30%）`
            triggerAnimation(defender, 'defend')
            // 十种影法术·防御式神：主人防御成功后眩晕攻击者1回合
            if (defender.skill?.shikigamiDefend) {
                attacker.status = 'stun'
                attacker.stunTurns = 1
                logPart += `，防御式神眩晕了 ${attacker.name}（1回合）！`
            }
        }
        defender.status = ''  // 无论是否受伤都清除防御状态
    }
    // 奇迹之星致死抵消 + 扣血
    const applied = applyDamage(defender, damage)
    damage = applied.dmg
    logPart += applied.logPart
    triggerAnimation(attacker, atkType)
    if (damage > 0) triggerAnimation(defender, 'hurt')
    logPart += ` 对 ${defender.name} 造成 ${damage} 点伤害`
    if (damage > 0) {
        if (attacker.skill?.shikigamiAttack) {
            const sd = Math.max(1, Math.floor(attacker.attack * attacker.skill.shikigamiAtkMul) - defender.defense)
            const sApp = applyDamage(defender, sd)
            logPart += `；攻击式神追加 ${sApp.dmg} 伤害${sApp.logPart}`
        }
        if (attacker.skill?.autoUltHits) {
            attacker.hitCount++
            if (attacker.hitCount >= attacker.skill.autoUltHits) {
                attacker.hitCount = 0
                const ad = Math.max(1, Math.floor(attacker.attack * attacker.skill.autoUltMul) - defender.defense)
                const aApp = applyDamage(defender, ad)
                triggerAnimation(attacker, 'ranged')
                logPart += `；💥 刍灵咒法自动造成${aApp.dmg} 伤害！${aApp.logPart}`
            }
        }
    }
    return { damage, logPart }
}
async function executeAction(action) {
    const attacker = currentPlayer.value
    const defender = players.find(p => p.id !== attacker.id)
    if (attacker.status === 'dodge' || attacker.status === 'defend') attacker.status = ''
    if (attacker.status === 'stun') {
        attacker.stunTurns--
        const remain = attacker.stunTurns
        if (attacker.stunTurns <= 0) attacker.status = ''
        logs.value.push(`${attacker.name} 因眩晕无法行动！${remain > 0 ? `（剩余${remain}回合）` : '（眩晕结束）'}`)
        triggerAnimation(attacker, 'stun')
        await wait(1200)
        endTurn()
        return
    }
    // 行动计数：turnCount = 该玩家已完成行动次数（眩晕回合不计数，保证双方第N次行动时 turnCount=N，周期判定一致）
    attacker.turnCount++
    // 黑鸟操术：每3回合回合开始时自动召唤黑鸟攻击（两段50%）
    if (attacker.skill?.summonEveryTurns && attacker.turnCount % attacker.skill.summonEveryTurns === 0) {
        const bDefender = players.find(p => p.id !== attacker.id)
        let totalDmg = 0
        for (let i = 0; i < 2; i++) {
            if (bDefender.hp <= 0) break
            const bd = Math.max(1, Math.floor(attacker.attack * attacker.skill.summonAtkMul) - bDefender.defense)
            const bApp = applyDamage(bDefender, bd)
            totalDmg += bApp.dmg
            if (bApp.logPart) logs.value.push(`🐦 ${bApp.logPart.trim()}`)
            triggerAnimation(attacker, 'ranged')
            if (bApp.dmg > 0) triggerAnimation(bDefender, 'hurt')
            await wait(400)
        }
        logs.value.push(`🐦 ${attacker.name} 的黑鸟操术触发！黑鸟两段攻击 ${bDefender.name} 共造成 ${totalDmg} 点伤害`)
        await wait(800)
        checkBattleEnd()
        if (gameOver.value) return  // 黑鸟直接终结战斗，不再执行本回合动作
    }
    let log = `${attacker.name} 使用 ${action.label}！`
    // 坐杀博徒：抽奖回合自动掷骰（自动战斗也生效；手动模式已在转盘转动前掷过，casinoRolled 防重复）
    if (isCasinoTurn(attacker, attacker.turnCount) && !attacker.casinoRolled) {
        await rollCasino(attacker)
    }
    let damage = 0
    // 十种影法术：敌人眩晕时，无论主人转到什么，攻击式神自动造成100%伤害
    if (defender.status === 'stun' && attacker.skill?.shikigamiAttack) {
        const sd = Math.max(1, Math.floor(attacker.attack * 1.0) - defender.defense)
        const sApp = applyDamage(defender, sd)
        log += `；🐺 攻击式神趁眩晕追击造成 ${sApp.dmg} 伤害${sApp.logPart}`
        triggerAnimation(attacker, 'melee')
        if (sApp.dmg > 0) triggerAnimation(defender, 'hurt')
    }
    switch (action.effect) {
        case 'normal': {
            // 咒言「攻击自己」反噬：本回合攻击的伤害打向自己
            if (attacker.backfire) {
                attacker.backfire = false
                const selfDmg = calculateDamage(attacker, defender)
                attacker.hp = Math.max(0, attacker.hp - selfDmg)
                triggerAnimation(attacker, 'hurt')
                log += ` 💢 咒言反噬！${attacker.name} 的攻击伤害打向自己，受到 ${selfDmg} 点伤害`
                damage = 0
                break
            }
            const baseDmg = calculateDamage(attacker, defender)
            if (attacker.weakPointHit) log += ` 🎯 命中弱点！`
            // 普通攻击（近战/远程均可）触发黑闪判定
            const result = processDamage(attacker, defender, baseDmg, { atkType: attacker.skill?.attackType || 'melee', allowBlackFlash: true })
            log += result.logPart
            damage = result.damage
            break
        }
        // 咒言术：不许动（眩晕敌人1回合）
        case 'stunSpeech': {
            defender.status = 'stun'
            defender.stunTurns = 1
            triggerAnimation(attacker, 'melee')
            triggerAnimation(defender, 'stun')
            log += ` 🔇 咒言「不许动」！${defender.name} 眩晕1回合`
            break
        }
        // 咒言术：攻击自己（敌人下回合攻击伤害反噬自身）
        case 'backfireSpeech': {
            defender.backfire = true
            triggerAnimation(attacker, 'melee')
            log += ` 🗣️ 咒言「攻击自己」！${defender.name} 下回合的伤害将反噬自身`
            break
        }
        // 咒言术：炸裂吧（150%伤害，术式伤害不触发黑闪）
        case 'burstSpeech': {
            const baseDmg = Math.floor(calculateDamage(attacker, defender) * 1.5)
            const result = processDamage(attacker, defender, baseDmg, { atkType: 'melee', allowBlackFlash: false })
            log += ` 💥 咒言「炸裂吧」！` + result.logPart
            damage = result.damage
            break
        }
        case 'defend':
            attacker.status = 'defend'
            triggerAnimation(attacker, 'defend')
            log += ` 进入防御状态`
            break
        case 'dodge':
            attacker.status = 'dodge'
            triggerAnimation(attacker, 'dodge')
            log += ` 进入闪避姿态（闪避率+40%）`
            break
    }
    logs.value.push(log)
    await wait(1600)
    // 坐杀博徒：本回合抽奖标记复位，buff 剩余回合衰减
    attacker.casinoRolled = false
    decayBoost(attacker)
    checkBattleEnd()
    if (!gameOver.value) await endTurn()
    resetBothWheels()
}
function calculateDamage(attacker, defender) {
    let atk = attacker.attack
    // 坐杀博徒：骰子抽奖的攻击力加成（小奖110% / 中奖120% / 大奖150%）
    if (attacker.attackBoost && attacker.attackBoost !== 1) atk = Math.floor(atk * attacker.attackBoost)
    // 十划咒法：90%概率命中制造的弱点，造成150%伤害；10%未命中弱点为普通伤害
    attacker.weakPointHit = false
    if (attacker.skill?.weakPointChance && Math.random() < attacker.skill.weakPointChance) {
        attacker.weakPointHit = true
        atk = Math.floor(atk * attacker.skill.weakPointMul)
    }
    return Math.max(1, atk - defender.defense)
}
function getBlackFlashRate(player) {
    const feel = getAttrVal(player, '手感')
    return Math.min(0.2 + feel * 0.02, 0.75)
}
function getDodgeRate(defender, attackType) {
    const speed = getAttrVal(defender, '速度')
    return Math.min(0.6 + speed * 0.01, 0.95)
}
function getAntiBlackFlashRate(defender) {
    const con = getAttrVal(defender, '体质')
    return Math.min(con * 0.02, 0.5)
}
function checkBattleEnd() {
    const deadPlayer = players.find(p => p.hp <= 0)
    if (deadPlayer) {
        gameOver.value = true
        winner.value = players.find(p => p.id !== deadPlayer.id)
    }
}
async function endTurn() {
    currentPlayerIndex.value = (currentPlayerIndex.value + 1) % players.length
    if (currentPlayerIndex.value === 0) turnNumber.value++
    const nextPlayer = currentPlayer.value
    // 新回合开始，清除上一回合的防御/闪避状态
    if (nextPlayer.status === 'defend' || nextPlayer.status === 'dodge') {
        nextPlayer.status = ''
    }
}
function resetBattle() {
    players.forEach(p => {
        p.maxHp = BASE_HP; p.hp = BASE_HP; p.attack = BASE_ATK; p.defense = BASE_DEF
        p.status = ''; p.animation = 'idle'; p.characterAttributes = {}
        p.skill = null; p.hitCount = 0; p.ultReady = false
        p.stars = 0; p.turnCount = 0; p.stunTurns = 0; p.backfire = false
        p.dice = [0, 0, 0]; p.boostTurns = 0; p.attackBoost = 1; p.casinoRolled = false
    })
    blackFlashStreakByPlayer[1] = 0
    blackFlashStreakByPlayer[2] = 0
    autoBattle.value = false
    turnNumber.value = 1
    currentPlayerIndex.value = 0
    battleStage.value = 'prepare'
    prepareIndex.value = 0
    logs.value = []
    gameOver.value = false
    winner.value = null
    resetBothWheels()
    initPlayerAttributesFromDefinitions()
}
function hpPercent(player) { return Math.floor((player.hp / player.maxHp) * 100) }
function hpColor(player) {
    if (player.hp / player.maxHp > 0.6) return '#67C23A'
    if (player.hp / player.maxHp > 0.3) return '#E6A23C'
    return '#F56C6C'
}
function statusText(status) {
    const map = { stun: '眩晕', defend: '防御', counter: '反击', dodge: '闪避' }
    return map[status] || ''
}
function weightedRandom(opts) {
    const total = opts.reduce((sum, o) => sum + o.weight, 0)
    let rand = Math.random() * total
    for (const opt of opts) {
        rand -= opt.weight
        if (rand < 0) return opt
    }
    return opts[opts.length - 1]
}
function getAnim(player) {
    if (player.hp <= 0) return 'dead'
    if (player.status === 'defend' || player.status === 'counter') return 'defend'
    return player.animation || 'idle'
}
function triggerAnimation(player, animName) {
    player.animation = animName
}
function resetBothWheels() {
    if (wheel1.value) wheel1.value.resetToGap()
    if (wheel2.value) wheel2.value.resetToGap()
}
watch(currentGroupId, (newVal) => {
    if (newVal) localStorage.setItem(STORAGE_KEY, String(newVal))
    else localStorage.removeItem(STORAGE_KEY)
})
onMounted(() => { loadGroups() })
</script>
<style scoped>
.battle-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  background: #0a0710;
  color: #e5d4b0;
  min-height: 100vh;
  text-align: center;
  position: relative;
}
.back-btn { position: absolute; top: 20px; left: 20px; }
h1 { color: #f5e6b0; text-shadow: 0 0 20px #f0b34b; margin-bottom: 20px; }
.group-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  background: #1f1525;
  border: 1px solid #4a3a3a;
  border-radius: 20px;
  padding: 12px 15px;
}
.group-label { color: #bdaa88; font-size: 0.9rem; }
.stage-header {
  color: #f5e6b0;
  margin: 10px 0 20px 0;
  font-size: 1.2rem;
  text-shadow: 0 0 10px rgba(245, 230, 176, 0.5);
}
.battle-field {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: flex-start;
  gap: 30px;
  width: 100%;
  margin: 20px 0;
}
.fighter-card {
  width: 380px;
  background: #1f1525;
  border: 1px solid #4a3a3a;
  border-radius: 20px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.5);
  transition: all 0.3s;
}
.fighter-card.active { border-color: #f5d78a; box-shadow: 0 0 20px rgba(245, 215, 138, 0.3); }
.fighter-card.dead { opacity: 0.4; filter: grayscale(1); }
.fighter-top { display: flex; justify-content: center; align-items: center; gap: 10px; margin-bottom: 10px; }
/* 坐杀博徒：人物卡侧面的骰子列（横排） */
.casino-dice { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.casino-label {
  font-size: 11px;
  font-weight: bold;
  color: #f5c542;
  background: rgba(245, 197, 66, 0.12);
  border: 1px solid rgba(245, 197, 66, 0.4);
  border-radius: 8px;
  padding: 2px 6px;
  white-space: nowrap;
  animation: labelFlash 1s ease infinite;
}
@keyframes labelFlash {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}
.casino-dice-row { display: flex; flex-direction: row; gap: 4px; }
.die {
  width: 26px; height: 26px;
  border-radius: 6px;
  background: #2a1f35;
  border: 1px solid #6a4a6a;
  color: #bdaa88;
  font-size: 14px; font-weight: bold;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.die.active {
  background: #4a3520;
  border-color: #f5c542;
  color: #f5c542;
  box-shadow: 0 0 8px rgba(245, 197, 66, 0.5);
  animation: diePop 0.35s ease;
}
@keyframes diePop {
  0% { transform: rotate(-25deg) scale(0.5); }
  100% { transform: rotate(0deg) scale(1); }
}
.mini-wheel { width: 150px !important; height: 150px !important; }
.mini-wheel :deep(.wheel-container) { width: 150px !important; }
.mini-wheel :deep(canvas) { width: 150px !important; height: 150px !important; }
.mini-wheel :deep(.pointer) { top: -6px; border-left: 8px solid transparent; border-right: 8px solid transparent; border-top: 16px solid gold; filter: drop-shadow(0 0 8px orange); }
.fighter-skill-area {
    width: 100%;
    min-height: 75px;
    background: rgba(245, 197, 66, 0.1);
    border-radius: 8px;
    padding: 12px;
    margin: 10px 0;
    text-align: left;
    box-sizing: border-box;
}
.fighter-skill-name {
    font-size: 14px;
    font-weight: bold;
    color: #f5d78a;
    margin-bottom: 6px;
}
.fighter-skill-desc {
    font-size: 12px;
    color: #ccc;
    line-height: 1.6;
}
.hp-bar { width: 100%; margin: 10px 0 15px 0; }
.fighter-body {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  width: 100%;
  gap: 10px;
}
.fighter-stats { display: flex; flex-direction: column; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.fighter-char { display: flex; flex-direction: column; align-items: center; flex: 0 0 120px; }
.fighter-name { font-weight: bold; margin: 5px 0; color: #e5d4b0; }
.attrs span { display: block; font-size: 0.9rem; color: #ccc; }
.mini-radar {
    width: 100%;
    max-width: 160px;
    padding: 0;
    box-sizing: border-box;
}
.mini-radar :deep(.radar-wrapper) {
    max-width: 160px !important;
    background: transparent !important;
    border: none !important;
    padding: 0 !important;
}
.mini-radar :deep(.radar-chart) {
    height: 160px !important;
    width: 100% !important;
}
.mini-radar :deep(.radar-title) {
    display: none !important;
}
.battle-log { background: #1f1525; border: 1px solid #4a3a3a; border-radius: 20px; padding: 15px 20px; margin-top: 20px; min-height: 50px; text-align: center; }
.battle-log .history-summary { color: #f5e6b0; font-size: 1rem; }
.battle-toolbar {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin: 16px 0 8px 0;
  flex-wrap: wrap;
}
.hint { color: #8a7a8a; text-align: center; margin-top: 20px; font-size: 0.9rem; }
.empty-tip { background: #1f1525; border: 1px solid #4a3a3a; border-radius: 20px; padding: 20px; margin: 20px 0; color: #8a7a8a; }
.game-over { text-align: center; margin-top: 20px; padding: 20px; background: #2a1a1a; border-radius: 20px; border: 2px solid #f5c542; }
.game-over h3 { color: #f5c542; margin-bottom: 15px; }
@media (max-width: 850px) {
  .battle-field { flex-direction: column; align-items: center; }
  .fighter-card { width: 100%; max-width: 420px; }
}
</style>
