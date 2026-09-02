<template>
    <div class="battle-container">
        <h2>⚔️ 双人对战 · 命运轮盘</h2>

        <!-- 准备阶段 -->
        <div v-if="battleStage === 'prepare'" class="prepare-stage">
            <h3>准备阶段：请 {{ currentPlayer.name }} 转动头顶的属性转盘</h3>
            <div class="battle-field">
                <!-- 玩家1 -->
                <div class="fighter" :class="{ active: currentPlayer.id === 1 }">
                    <div class="wheel-wrapper">
                        <Wheel ref="wheel1" :options="currentStageOptions" @spin="() => handleSpinAttempt(1)"
                            class="mini-wheel" />
                    </div>
                    <div class="stickman red" :class="players[0].animation">
                        <svg viewBox="0 0 100 150" width="80" height="120">
                            <circle cx="50" cy="20" r="15" fill="#ff5555" />
                            <line x1="50" y1="35" x2="50" y2="90" stroke="#ff5555" stroke-width="6" />
                            <line x1="50" y1="50" x2="20" y2="80" stroke="#ff5555" stroke-width="5" />
                            <line x1="50" y1="50" x2="80" y2="80" stroke="#ff5555" stroke-width="5" />
                            <line x1="50" y1="90" x2="30" y2="130" stroke="#ff5555" stroke-width="5" />
                            <line x1="50" y1="90" x2="70" y2="130" stroke="#ff5555" stroke-width="5" />
                        </svg>
                    </div>
                    <p class="fighter-name">玩家1</p>
                    <div class="attrs">
                        <span>HP: {{ players[0].hp }}/{{ players[0].maxHp }}</span>
                        <span>攻: {{ players[0].attack }}</span>
                        <span>防: {{ players[0].defense }}</span>
                    </div>
                </div>

                <!-- 玩家2 -->
                <div class="fighter" :class="{ active: currentPlayer.id === 2 }">
                    <div class="wheel-wrapper">
                        <Wheel ref="wheel2" :options="currentStageOptions" @spin="() => handleSpinAttempt(2)"
                            class="mini-wheel" />
                    </div>
                    <div class="stickman blue" :class="players[1].animation">
                        <svg viewBox="0 0 100 150" width="80" height="120">
                            <circle cx="50" cy="20" r="15" fill="#5555ff" />
                            <line x1="50" y1="35" x2="50" y2="90" stroke="#5555ff" stroke-width="6" />
                            <line x1="50" y1="50" x2="20" y2="80" stroke="#5555ff" stroke-width="5" />
                            <line x1="50" y1="50" x2="80" y2="80" stroke="#5555ff" stroke-width="5" />
                            <line x1="50" y1="90" x2="30" y2="130" stroke="#5555ff" stroke-width="5" />
                            <line x1="50" y1="90" x2="70" y2="130" stroke="#5555ff" stroke-width="5" />
                        </svg>
                    </div>
                    <p class="fighter-name">玩家2</p>
                    <div class="attrs">
                        <span>HP: {{ players[1].hp }}/{{ players[1].maxHp }}</span>
                        <span>攻: {{ players[1].attack }}</span>
                        <span>防: {{ players[1].defense }}</span>
                    </div>
                </div>
            </div>
            <p class="hint">点击自己头顶的转盘来随机属性</p>
        </div>

        <!-- 战斗阶段 -->
        <div v-else class="battle-stage">
            <h3>战斗阶段：{{ currentPlayer.name }} 的回合</h3>
            <div class="battle-field">
                <!-- 玩家1 -->
                <div class="fighter" :class="{ active: currentPlayer.id === 1, dead: players[0].hp <= 0 }">
                    <div class="wheel-wrapper">
                        <Wheel ref="wheel1" :options="currentStageOptions" @spin="() => handleSpinAttempt(1)"
                            class="mini-wheel" />
                    </div>
                    <div class="stickman red" :class="players[0].animation">
                        <svg viewBox="0 0 100 150" width="80" height="120">
                            <circle cx="50" cy="20" r="15" fill="#ff5555" />
                            <line x1="50" y1="35" x2="50" y2="90" stroke="#ff5555" stroke-width="6" />
                            <line x1="50" y1="50" x2="20" y2="80" stroke="#ff5555" stroke-width="5" />
                            <line x1="50" y1="50" x2="80" y2="80" stroke="#ff5555" stroke-width="5" />
                            <line x1="50" y1="90" x2="30" y2="130" stroke="#ff5555" stroke-width="5" />
                            <line x1="50" y1="90" x2="70" y2="130" stroke="#ff5555" stroke-width="5" />
                        </svg>
                    </div>
                    <p class="fighter-name">玩家1</p>
                    <el-progress :percentage="hpPercent(players[0])" :color="hpColor(players[0])"
                        style="width: 120px;" />
                    <div class="attrs">
                        <span>HP: {{ players[0].hp }}/{{ players[0].maxHp }}</span>
                        <span>攻: {{ players[0].attack }}</span>
                        <span>防: {{ players[0].defense }}</span>
                        <span v-if="players[0].status">状态: {{ statusText(players[0].status) }}</span>
                    </div>
                </div>

                <!-- 玩家2 -->
                <div class="fighter" :class="{ active: currentPlayer.id === 2, dead: players[1].hp <= 0 }">
                    <div class="wheel-wrapper">
                        <Wheel ref="wheel2" :options="currentStageOptions" @spin="() => handleSpinAttempt(2)"
                            class="mini-wheel" />
                    </div>
                    <div class="stickman blue" :class="players[1].animation">
                        <svg viewBox="0 0 100 150" width="80" height="120">
                            <circle cx="50" cy="20" r="15" fill="#5555ff" />
                            <line x1="50" y1="35" x2="50" y2="90" stroke="#5555ff" stroke-width="6" />
                            <line x1="50" y1="50" x2="20" y2="80" stroke="#5555ff" stroke-width="5" />
                            <line x1="50" y1="50" x2="80" y2="80" stroke="#5555ff" stroke-width="5" />
                            <line x1="50" y1="90" x2="30" y2="130" stroke="#5555ff" stroke-width="5" />
                            <line x1="50" y1="90" x2="70" y2="130" stroke="#5555ff" stroke-width="5" />
                        </svg>
                    </div>
                    <p class="fighter-name">玩家2</p>
                    <el-progress :percentage="hpPercent(players[1])" :color="hpColor(players[1])"
                        style="width: 120px;" />
                    <div class="attrs">
                        <span>HP: {{ players[1].hp }}/{{ players[1].maxHp }}</span>
                        <span>攻: {{ players[1].attack }}</span>
                        <span>防: {{ players[1].defense }}</span>
                        <span v-if="players[1].status">状态: {{ statusText(players[1].status) }}</span>
                    </div>
                </div>
            </div>

            <div class="battle-log">
                <div v-for="(log, i) in logs" :key="i" class="log-entry">{{ log }}</div>
            </div>
            <div v-if="gameOver" class="game-over">
                <h3>{{ winner.name }} 获胜！</h3>
                <el-button type="danger" @click="resetBattle">重新开始</el-button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import Wheel from './Wheel.vue'

const players = reactive([
    { id: 1, name: '玩家1', maxHp: 100, attack: 10, defense: 5, hp: 100, status: '', animation: '' },
    { id: 2, name: '玩家2', maxHp: 100, attack: 10, defense: 5, hp: 100, status: '', animation: '' }
])

const battleStage = ref('prepare') // prepare | battle
const currentPlayerIndex = ref(0)
const currentPlayer = computed(() => players[currentPlayerIndex.value])
const isSpinning = ref(false)
const logs = ref([])
const gameOver = ref(false)
const winner = ref(null)

const attributeOptions = ref([
    { label: '生命强化', weight: 30, descText: '生命+30', attrGains: { maxHp: 30, hp: 30 } },
    { label: '攻击强化', weight: 30, descText: '攻击+5', attrGains: { attack: 5 } },
    { label: '防御强化', weight: 25, descText: '防御+3', attrGains: { defense: 3 } },
    { label: '均衡发展', weight: 15, descText: '生命+15，攻击+2，防御+1', attrGains: { maxHp: 15, hp: 15, attack: 2, defense: 1 } }
])

const battleOptions = ref([
    { label: '普通攻击', weight: 30, descText: '造成攻击伤害', effect: 'normal' },
    { label: '重击', weight: 15, descText: '造成1.8倍伤害，下回合眩晕', effect: 'heavy' },
    { label: '防御', weight: 20, descText: '本回合受到伤害减半', effect: 'defend' },
    { label: '治疗', weight: 20, descText: '回复20%最大生命', effect: 'heal' },
    { label: '反击', weight: 15, descText: '本回合反弹50%伤害', effect: 'counter' }
])

const currentStageOptions = computed(() => battleStage.value === 'prepare' ? attributeOptions.value : battleOptions.value)

const wheel1 = ref(null)
const wheel2 = ref(null)

// 触发火柴人动画
function triggerAnimation(player, animName) {
    player.animation = animName
    setTimeout(() => {
        player.animation = ''
    }, 600) // 动画持续时间，需要与 CSS 动画时长匹配
}

// 处理点击转盘
function handleSpinAttempt(playerId) {
    if (isSpinning.value || currentPlayer.value.id !== playerId || gameOver.value) return
    isSpinning.value = true

    const options = currentStageOptions.value
    const selected = weightedRandom(options)
    const idx = options.indexOf(selected)

    const wheel = playerId === 1 ? wheel1.value : wheel2.value

    if (wheel) {
        wheel.spinToTarget(idx, () => {
            if (battleStage.value === 'prepare') {
                applyAttribute(selected)
                isSpinning.value = false
                ElMessage.success(`${currentPlayer.value.name} 获得属性：${selected.label}`)
                if (currentPlayerIndex.value === 0) {
                    currentPlayerIndex.value = 1
                } else {
                    battleStage.value = 'battle'
                    logs.value.push('战斗开始！')
                }
                resetBothWheels()
            } else {
                executeAction(selected)
                isSpinning.value = false
            }
        })
    } else {
        isSpinning.value = false
    }
}

function applyAttribute(option) {
    const player = currentPlayer.value
    const gains = option.attrGains || {}
    if (gains.maxHp) {
        player.maxHp += gains.maxHp
        player.hp = Math.min(player.maxHp, player.hp + (gains.hp || gains.maxHp))
    }
    if (gains.attack) player.attack += gains.attack
    if (gains.defense) player.defense += gains.defense
}

function executeAction(action) {
    const attacker = currentPlayer.value
    const defender = players.find(p => p.id !== attacker.id)

    if (attacker.status === 'stun') {
        attacker.status = ''
        logs.value.push(`${attacker.name} 因眩晕无法行动！`)
        triggerAnimation(attacker, 'stun')
        endTurn()
        return
    }

    let log = `${attacker.name} 使用 ${action.label}！`
    let damage = 0
    let heal = 0

    switch (action.effect) {
        case 'normal':
            damage = calculateDamage(attacker, defender)
            defender.hp = Math.max(0, defender.hp - damage)
            triggerAnimation(attacker, 'attack')
            triggerAnimation(defender, 'hurt')
            log += ` 对 ${defender.name} 造成 ${damage} 点伤害`
            break
        case 'heavy':
            damage = Math.floor(calculateDamage(attacker, defender) * 1.8)
            defender.hp = Math.max(0, defender.hp - damage)
            attacker.status = 'stun'
            triggerAnimation(attacker, 'heavy')
            triggerAnimation(defender, 'hurt')
            log += ` 对 ${defender.name} 造成 ${damage} 点伤害，${attacker.name} 下回合眩晕`
            break
        case 'defend':
            attacker.status = 'defend'
            triggerAnimation(attacker, 'defend')
            log += ` 进入防御状态`
            break
        case 'heal':
            heal = Math.floor(attacker.maxHp * 0.2)
            attacker.hp = Math.min(attacker.maxHp, attacker.hp + heal)
            triggerAnimation(attacker, 'heal')
            log += ` 回复 ${heal} 点生命`
            break
        case 'counter':
            attacker.status = 'counter'
            triggerAnimation(attacker, 'counter')
            log += ` 进入反击状态`
            break
    }

    if (defender.status === 'counter' && damage > 0) {
        const reflect = Math.floor(damage * 0.5)
        attacker.hp = Math.max(0, attacker.hp - reflect)
        triggerAnimation(attacker, 'hurt')
        log += `，${defender.name} 反击造成 ${reflect} 点伤害`
        defender.status = ''
    }

    if (defender.status === 'defend' && damage > 0) {
        damage = Math.floor(damage / 2)
        defender.hp = Math.min(defender.hp + damage, defender.maxHp)
        triggerAnimation(defender, 'defend')
        log = log.replace(`${damage} 点伤害`, `${damage} 点伤害（防御减半）`)
        defender.status = ''
    }

    logs.value.push(log)
    checkBattleEnd()
    if (!gameOver.value) {
        endTurn()
    }
    resetBothWheels()
}

function resetBothWheels() {
    if (wheel1.value) wheel1.value.resetToGap()
    if (wheel2.value) wheel2.value.resetToGap()
}

function calculateDamage(attacker, defender) {
    let damage = Math.max(1, attacker.attack - defender.defense)
    if (defender.status === 'defend') damage = Math.floor(damage / 2)
    return damage
}

function checkBattleEnd() {
    const deadPlayer = players.find(p => p.hp <= 0)
    if (deadPlayer) {
        gameOver.value = true
        winner.value = players.find(p => p.id !== deadPlayer.id)
    }
}

function endTurn() {
    currentPlayerIndex.value = (currentPlayerIndex.value + 1) % players.length
}

function resetBattle() {
    players.forEach(p => {
        p.maxHp = 100; p.hp = 100; p.attack = 10; p.defense = 5; p.status = ''; p.animation = ''
    })
    currentPlayerIndex.value = 0
    battleStage.value = 'prepare'
    logs.value = []
    gameOver.value = false
    winner.value = null
    resetBothWheels()
}

function hpPercent(player) {
    return Math.floor((player.hp / player.maxHp) * 100)
}

function hpColor(player) {
    if (player.hp / player.maxHp > 0.6) return '#67C23A'
    if (player.hp / player.maxHp > 0.3) return '#E6A23C'
    return '#F56C6C'
}

function statusText(status) {
    const map = { stun: '眩晕', defend: '防御', counter: '反击' }
    return map[status] || ''
}

function weightedRandom(opts) {
    const shuffled = [...opts]
    for (let i = shuffled.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
            ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
    }
    const total = shuffled.reduce((sum, o) => sum + o.weight, 0)
    let rand = Math.random() * total
    for (const opt of shuffled) {
        rand -= opt.weight
        if (rand < 0) return opt
    }
    return shuffled[shuffled.length - 1]
}
</script>

<style scoped>
.battle-container {
    max-width: 800px;
    margin: 0 auto;
    padding: 20px;
}

.battle-field {
  display: flex;
  justify-content: space-around; /* 保持空间分布 */
  align-items: flex-end;
  height: 380px; /* 高度稍微加大一点，容纳下移后的元素 */
  margin-top: 50px; /* 原为 20px，加大数值让整体往下移 */
  padding: 0 30px; /* 增加左右内边距，让两边人物更靠中间，从而拉开两人距离 */
}

.fighter {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  width: 240px; /* 原为 200px，加大宽度让两者之间空出更多位置 */
}

.fighter.active .stickman {
    filter: drop-shadow(0 0 10px gold);
}

.fighter.dead {
    opacity: 0.4;
}

.wheel-wrapper {
  position: absolute;
  top: 20px; /* 原为 -20px，改为 20px 往下移 */
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
}

.mini-wheel {
    width: 150px;
    height: 150px;
}

.stickman {
  margin-top: 160px; /* 原为 130px，同步往下增加距离，保证头部露出来 */
  position: relative;
  z-index: 5;
}

.fighter-name {
    font-weight: bold;
    margin: 5px 0;
}

.attrs span {
    display: block;
    font-size: 0.8rem;
    color: #ccc;
}

.battle-log {
    max-height: 120px;
    overflow-y: auto;
    background: #0a0710;
    border-radius: 8px;
    padding: 10px;
    margin-top: 10px;
    text-align: left;
}

.log-entry {
    color: #e5d4b0;
    margin-bottom: 3px;
}

.hint {
    color: #8a7a8a;
    text-align: center;
}

/* 动画效果：攻击前冲、受击后仰、防御格挡、治疗光晕、眩晕摇晃、重击爆发、反击闪避 */
@keyframes attack {
    0% {
        transform: translateX(0) rotate(0);
    }

    25% {
        transform: translateX(40px) rotate(10deg);
    }

    75% {
        transform: translateX(-15px) rotate(-5deg);
    }

    100% {
        transform: translateX(0) rotate(0);
    }
}

.attack {
    animation: attack 0.4s ease-out;
}

@keyframes hurt {
    0% {
        transform: translateX(0);
        filter: brightness(1) drop-shadow(0 0 0 red);
    }

    20% {
        transform: translateX(-25px) rotate(-8deg);
        filter: brightness(2) drop-shadow(0 0 8px red);
    }

    60% {
        transform: translateX(20px) rotate(5deg);
    }

    100% {
        transform: translateX(0);
        filter: brightness(1) drop-shadow(0 0 0 red);
    }
}

.hurt {
    animation: hurt 0.5s ease-out;
}

@keyframes defend {
    0% {
        transform: translateY(0);
    }

    40% {
        transform: translateY(-15px) scale(1.05);
    }

    100% {
        transform: translateY(0);
    }
}

.defend {
    animation: defend 0.4s ease-out;
}

@keyframes heal {
    0% {
        filter: drop-shadow(0 0 5px #00ff00);
    }

    50% {
        filter: drop-shadow(0 0 25px #00ff00);
    }

    100% {
        filter: drop-shadow(0 0 0 #00ff00);
    }
}

.heal {
    animation: heal 0.6s ease-out;
}

@keyframes stun {
    0% {
        transform: rotate(0);
    }

    20% {
        transform: rotate(8deg);
    }

    40% {
        transform: rotate(-6deg);
    }

    60% {
        transform: rotate(4deg);
    }

    100% {
        transform: rotate(0);
    }
}

.stun {
    animation: stun 0.6s ease-out;
}

@keyframes heavy {
    0% {
        transform: scale(1);
    }

    50% {
        transform: scale(1.3);
    }

    100% {
        transform: scale(1);
    }
}

.heavy {
    animation: heavy 0.5s ease-out;
}

@keyframes counter {
    0% {
        opacity: 0.5;
        transform: translateX(-20px);
    }

    50% {
        opacity: 1;
        transform: translateX(20px);
    }

    100% {
        opacity: 1;
        transform: translateX(0);
    }
}

.counter {
    animation: counter 0.4s ease-out;
}
</style>