<template>
  <div class="stickman-wrap" :class="{ flip }">
    <div class="stickman" :class="currentAnim">
      <svg viewBox="0 0 100 150" :width="width" :height="height">
        <!-- 地面线 -->
        <line x1="10" y1="146" x2="90" y2="146" stroke="#3a3a4a" stroke-width="1.5"/>
        <g class="j-body">
          <!-- 后腿（后侧，深一档色） -->
          <g class="j-leg-bu">
            <line x1="50" y1="92" x2="43" y2="114" :stroke="darkColor" stroke-width="6" stroke-linecap="round"/>
            <g class="j-leg-bl">
              <line x1="43" y1="114" x2="38" y2="138" :stroke="darkColor" stroke-width="5.5" stroke-linecap="round"/>
              <line x1="38" y1="138" x2="47" y2="144" :stroke="darkColor" stroke-width="5" stroke-linecap="round"/>
            </g>
          </g>
          <!-- 前腿（前侧，主色） -->
          <g class="j-leg-fu">
            <line x1="50" y1="92" x2="59" y2="116" :stroke="mainColor" stroke-width="6" stroke-linecap="round"/>
            <g class="j-leg-fl">
              <line x1="59" y1="116" x2="64" y2="140" :stroke="mainColor" stroke-width="5.5" stroke-linecap="round"/>
              <line x1="64" y1="140" x2="74" y2="145" :stroke="mainColor" stroke-width="5" stroke-linecap="round"/>
            </g>
          </g>
          <!-- 躯干 -->
          <rect x="46" y="34" width="8" height="58" rx="4" :fill="mainColor"/>
          <!-- 后臂（后侧，深一档色，固定） -->
          <g class="j-arm-bu">
            <line x1="50" y1="49" x2="43" y2="62" :stroke="darkColor" stroke-width="5" stroke-linecap="round"/>
            <line x1="43" y1="62" x2="39" y2="80" :stroke="darkColor" stroke-width="4.5" stroke-linecap="round"/>
          </g>
          <!-- 前臂（前侧，主色，肩+肘可动） -->
          <g class="j-arm-fu">
            <line x1="50" y1="49" x2="58" y2="64" :stroke="mainColor" stroke-width="5" stroke-linecap="round"/>
            <g class="j-arm-fl">
              <line x1="58" y1="64" x2="63" y2="87" :stroke="mainColor" stroke-width="4.5" stroke-linecap="round"/>
            </g>
          </g>
          <!-- 头（在躯干组内，防脱节） -->
          <g class="j-head">
            <circle cx="52" cy="20" r="14" :fill="mainColor"/>
            <circle cx="58" cy="18" r="2.5" fill="#1a1a2e"/>
          </g>
          <!-- 特效：近战冲击环 -->
          <g class="pf"><circle cx="72" cy="38" r="8" fill="none" stroke="#ffffff" stroke-width="2.5"/></g>
          <!-- 特效：火球（核心+光晕+尾焰） -->
          <g class="fb">
            <circle cx="72" cy="36" r="5" fill="#ffd166"/>
            <circle cx="72" cy="36" r="9" fill="#ff8c42" opacity="0.5"/>
            <ellipse cx="62" cy="40" rx="11" ry="3.5" fill="#ff6b35" opacity="0.6"/>
            <ellipse cx="54" cy="43" rx="7" ry="2.5" fill="#ff8c42" opacity="0.35"/>
          </g>
          <!-- 特效：闪避速度线 -->
          <g class="dl">
            <line x1="66" y1="18" x2="94" y2="18" stroke="#cdd6f4" stroke-width="1.6"/>
            <line x1="62" y1="30" x2="100" y2="30" stroke="#cdd6f4" stroke-width="1.6"/>
            <line x1="66" y1="42" x2="92" y2="42" stroke="#cdd6f4" stroke-width="1.6"/>
          </g>
          <!-- 特效：治疗绿色光点 -->
          <g class="hd">
            <circle cx="66" cy="12" r="2.5" fill="#7ce38b"/>
            <circle cx="76" cy="6" r="2" fill="#7ce38b"/>
            <circle cx="84" cy="16" r="2.5" fill="#7ce38b"/>
          </g>
          <!-- 特效：受击红色冲击圈 -->
          <g class="hs"><circle cx="70" cy="26" r="6" fill="none" stroke="#ff4444" stroke-width="2.5"/></g>
          <!-- 特效：死亡 X 眼 -->
          <g class="dx">
            <line x1="46" y1="16" x2="58" y2="24" stroke="#ffffff" stroke-width="2.6" stroke-linecap="round"/>
            <line x1="58" y1="16" x2="46" y2="24" stroke="#ffffff" stroke-width="2.6" stroke-linecap="round"/>
          </g>
        </g>
      </svg>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  color:  { type: String,  default: 'red' },   // red | blue
  action: { type: String,  default: 'idle' },  // idle|melee|ranged|hurt|defend|dodge|heal|block|dodge-hit|dead
  flip:   { type: Boolean, default: false },   // 玩家2 镜像（面对面）
  width:  { type: Number,  default: 200 },
  height: { type: Number,  default: 300 }
})

const mainColor = computed(() => props.color === 'blue' ? '#5577ff' : '#ff5555')
const darkColor = computed(() => props.color === 'blue' ? '#3f55c2' : '#b84343')

// 瞬发动作：播完自动回 idle；常驻动作(defend/dodge/dead)：保持直到父组件改变
const instantActions = ['melee', 'ranged', 'hurt', 'heal', 'block', 'dodge-hit']
const currentAnim = ref('idle')
let timer = null

watch(() => props.action, (newAction) => {
  if (timer) { clearTimeout(timer); timer = null }
  if (!newAction || newAction === 'idle') {
    currentAnim.value = 'idle'
    return
  }
  // 相同动作再次触发：先回 idle 强制重放动画（否则 watch 不触发，动作不播放）
  if (currentAnim.value === newAction) {
    currentAnim.value = 'idle'
    requestAnimationFrame(() => {
      currentAnim.value = newAction
      if (instantActions.includes(newAction)) {
        timer = setTimeout(() => {
          if (currentAnim.value === newAction) currentAnim.value = 'idle'
        }, 1800)
      }
    })
    return
  }
  currentAnim.value = newAction
  if (instantActions.includes(newAction)) {
    timer = setTimeout(() => {
      if (currentAnim.value === newAction) currentAnim.value = 'idle'
    }, 1800)
  }
}, { immediate: true })
</script>

<style scoped>
.stickman-wrap { display: inline-block; line-height: 0; }
.stickman-wrap.flip { transform: scaleX(-1); }
.stickman { position: relative; z-index: 2; transform-origin: 50px 120px; }
.stickman svg { display: block; overflow: visible; }

/* 各关节旋转轴（SVG 坐标） */
.j-head   { transform-origin: 52px 34px; transform-box: view-box; }
.j-arm-fu { transform-origin: 50px 49px; transform-box: view-box; }
.j-arm-fl { transform-origin: 58px 63px; transform-box: view-box; }
.j-arm-bu { transform-origin: 50px 49px; transform-box: view-box; }
.j-leg-fu { transform-origin: 50px 92px; transform-box: view-box; }
.j-leg-fl { transform-origin: 59px 116px; transform-box: view-box; }
.j-leg-bu { transform-origin: 50px 92px; transform-box: view-box; }
.j-leg-bl { transform-origin: 43px 114px; transform-box: view-box; }

/* ========== ① 待机：迈步侧站 + 自然呼吸 ========== */
.idle { animation: idb 2s ease-in-out infinite; }
@keyframes idb {
  0%, 100% { transform: translateY(0) rotate(1.5deg); }
  50%      { transform: translateY(-2.5px) rotate(3deg); }
}
.idle .j-arm-fu { animation: ia1 2s ease-in-out infinite; }
@keyframes ia1 { 0%,100%{transform:rotate(2deg)} 50%{transform:rotate(-6deg)} }
.idle .j-arm-fl { animation: ia2 2s ease-in-out infinite; }
@keyframes ia2 { 0%,100%{transform:rotate(1deg)} 50%{transform:rotate(-3deg)} }
.idle .j-arm-bu { animation: ia3 2s ease-in-out infinite; }
@keyframes ia3 { 0%,100%{transform:rotate(-2deg)} 50%{transform:rotate(6deg)} }
.idle .j-head   { animation: ih  2s ease-in-out infinite; }
@keyframes ih  { 0%,100%{transform:rotate(1deg)} 50%{transform:rotate(-1.5deg)} }

/* ========== ② 近战攻击：冲拳 + 冲击环 ========== */
.melee { animation: ml 1.6s ease-in-out; }
@keyframes ml {
  0%,100% { transform: rotate(2deg) translateX(0); }
  30%     { transform: rotate(-3deg) translateX(-6px); }
  50%     { transform: rotate(8deg) translateX(14px); }
  70%     { transform: rotate(4deg) translateX(6px); }
}
.melee .j-arm-fu { animation: maf 1.6s ease-in-out; }
@keyframes maf { 0%,100%{transform:rotate(0)} 30%{transform:rotate(-30deg)} 50%{transform:rotate(-65deg)} 70%{transform:rotate(-16deg)} }
.melee .j-arm-fl { animation: mal 1.6s ease-in-out; }
@keyframes mal { 0%,100%{transform:rotate(0)} 30%{transform:rotate(-20deg)} 50%{transform:rotate(14deg)} }
.pf { opacity: 0; transform-box: view-box; transform-origin: 72px 38px; }
.melee .pf { animation: pfa 1.2s ease-out; }
@keyframes pfa { 0%{opacity:1; transform:scale(0.4)} 100%{opacity:0; transform:scale(2.4)} }

/* ========== ③ 远程：先抬前臂蓄力 → 前挥扔火球 ========== */
.ranged { animation: rl 1.8s ease-in-out; }
@keyframes rl {
  0%,100% { transform: rotate(2deg) translateY(0); }
  25%     { transform: rotate(-4deg) translateY(1px); }
  50%     { transform: rotate(8deg) translateY(2px); }
  75%     { transform: rotate(3deg) translateY(1px); }
}
.ranged .j-arm-fu { animation: rlf 1.8s ease-in-out; }
@keyframes rlf {
  0%,100% { transform: rotate(0); }
  22%     { transform: rotate(-55deg); }  /* 抬前臂举到头上 */
  38%     { transform: rotate(-58deg); }  /* 保持抬起蓄力 */
  55%     { transform: rotate(-82deg); }  /* 前挥抛出 */
  72%     { transform: rotate(-48deg); }
}
.ranged .j-arm-fl { animation: rla 1.8s ease-in-out; }
@keyframes rla {
  0%,100% { transform: rotate(0); }
  22%     { transform: rotate(-42deg); }
  38%     { transform: rotate(-45deg); }
  55%     { transform: rotate(28deg); }
  72%     { transform: rotate(8deg); }
}
.fb { opacity: 0; transform-box: view-box; transform-origin: 72px 36px; }
.ranged .fb { animation: fb 1.8s ease-out; }
@keyframes fb {
  0%   { opacity: 0; transform: translate(0,0) scale(0.3); }
  20%  { opacity: 1; transform: translate(0,0) scale(1); }
  42%  { opacity: 1; transform: translate(2px,-2px) scale(1.1); }
  60%  { transform: translate(18px,-22px) scale(1.2) rotate(50deg); }
  100% { opacity: 0; transform: translate(62px,-28px) scale(1.7) rotate(130deg); }
}

/* ========== ④ 受击：被打退踉跄 + 红圈爆开 ========== */
.hurt { animation: hr 1.5s ease-out; }
@keyframes hr {
  0%   { transform: rotate(2deg) translateX(0); filter: brightness(1); }
  18%  { transform: rotate(-8deg) translateX(-9px); filter: brightness(2.2); }
  35%  { transform: rotate(-22deg) translateX(-20px); filter: brightness(1.5); }
  55%  { transform: rotate(9deg) translateX(7px); filter: brightness(1.2); }
  75%  { transform: rotate(-6deg) translateX(-4px); }
  100% { transform: rotate(2deg) translateX(0); filter: brightness(1); }
}
.hurt .j-head { animation: hh 1.5s ease-out; }
@keyframes hh {
  0%,100% { transform: rotate(0); }
  25%     { transform: rotate(-30deg); }
  55%     { transform: rotate(10deg); }
  75%     { transform: rotate(-4deg); }
}
.hs { opacity: 0; transform-box: view-box; transform-origin: 70px 26px; }
.hurt .hs { animation: hsa 0.7s ease-out; }
@keyframes hsa { 0%{opacity:1; transform:scale(0.3)} 100%{opacity:0; transform:scale(2.8)} }

/* ========== ⑤ 防御（常驻）：双手上抬格挡 + 金光 ========== */
.defend { animation: df 0.5s ease-out forwards; filter: drop-shadow(0 0 8px gold); }
@keyframes df { 0%{transform:rotate(2deg)} 100%{transform:rotate(-1deg) translateY(-2px)} }
.defend .j-arm-fu { animation: daf 0.5s ease-out forwards; }
@keyframes daf { 0%{transform:rotate(0)} 100%{transform:rotate(-70deg)} }
.defend .j-arm-fl { animation: dal 0.5s ease-out forwards; }
@keyframes dal { 0%{transform:rotate(0)} 100%{transform:rotate(-50deg)} }
.defend .j-arm-bu { animation: dab 0.5s ease-out forwards; }
@keyframes dab { 0%{transform:rotate(0)} 100%{transform:rotate(-60deg)} }

/* ========== ⑥ 闪避（常驻）：侧身倾斜 + 速度线 ========== */
.dodge { animation: dg 0.5s ease-out forwards; }
@keyframes dg { 0%{transform:rotate(2deg)} 100%{transform:rotate(18deg) translateX(-8px)} }
.dodge .j-head { animation: dgh 0.5s ease-out forwards; }
@keyframes dgh { 0%{transform:rotate(0)} 100%{transform:rotate(14deg)} }
.dl { opacity: 0; }
.dodge .dl { animation: dla 0.8s ease-out; }
@keyframes dla { 0%{opacity:0} 30%{opacity:1} 100%{opacity:0} }

/* ========== ⑦ 治疗：举手 + 绿色光晕 + 光点 ========== */
.heal { animation: hl 1.6s ease-out; }
@keyframes hl {
  0%,100% { transform: rotate(2deg); filter: drop-shadow(0 0 0 #7ce38b); }
  50%     { transform: rotate(-1deg) translateY(-3px); filter: drop-shadow(0 0 18px #7ce38b); }
}
.heal .j-arm-fu { animation: haf 1.6s ease-out; }
@keyframes haf { 0%,100%{transform:rotate(0)} 30%{transform:rotate(-80deg)} 60%{transform:rotate(-75deg)} }
.hd { opacity: 0; }
.heal .hd { animation: hda 1.6s ease-out; }
@keyframes hda { 0%{opacity:0; transform:translateY(0)} 30%{opacity:1} 100%{opacity:0; transform:translateY(-20px)} }

/* ========== ⑧ 格挡成功：双手上抬 + 震动 ========== */
.block { animation: bk 1s ease-out; }
@keyframes bk {
  0%   { transform: rotate(2deg); }
  30%  { transform: rotate(-5deg) translateX(-5px); }
  50%  { transform: rotate(3deg) translateX(3px); }
  100% { transform: rotate(2deg); }
}
.block .j-arm-fu { animation: baf 1s ease-out; }
@keyframes baf { 0%,100%{transform:rotate(0)} 30%{transform:rotate(-68deg)} 50%{transform:rotate(-72deg)} }
.block .j-arm-fl { animation: bal 1s ease-out; }
@keyframes bal { 0%,100%{transform:rotate(0)} 30%{transform:rotate(-48deg)} 50%{transform:rotate(-52deg)} }

/* ========== ⑨ 闪避成功：整体后躲 ========== */
.dodge-hit { animation: dh2 1.1s ease-out; }
@keyframes dh2 {
  0%   { transform: translateY(5px) rotate(11deg); }
  35%  { transform: translateX(-34px) rotate(-12deg); }
  75%  { transform: translateX(8px) rotate(5deg); }
  100% { transform: translateX(0) rotate(2deg); }
}

/* ========== ⑩ 死亡：后仰倒地贴地 + X 眼 ========== */
.dead { transform-origin: 100px 290px; animation: dd 1.4s ease-in forwards; }
@keyframes dd {
  0%   { transform: rotate(2deg) translateY(0); }
  25%  { transform: rotate(-8deg) translateY(-2px); }
  50%  { transform: rotate(-42deg) translateY(0); }
  75%  { transform: rotate(-78deg) translateY(1px); }
  100% { transform: rotate(-88deg) translateY(0); }
}
.dead .j-head { animation: ddh 1.4s ease-in forwards; }
@keyframes ddh { 0%,30%{transform:rotate(0)} 60%{transform:rotate(-28deg)} 100%{transform:rotate(-20deg)} }
.dead .j-arm-fu { animation: daf2 1.4s ease-in forwards; }
@keyframes daf2 { 0%,100%{transform:rotate(0)} 30%{transform:rotate(35deg)} 60%{transform:rotate(-14deg)} }
.dx { opacity: 0; }
.dead .dx { animation: dxf 1.2s ease-out forwards; }
@keyframes dxf { 0%{opacity:0} 72%{opacity:0} 100%{opacity:1} }
</style>
