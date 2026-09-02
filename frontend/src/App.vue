<template>
  <div id="app">
    <!-- 主页 -->
    <div v-if="currentPage === 'home'" class="home-page">
      <h1 class="home-title">⚜️ 命运轮盘 · 双模式</h1>
      <p class="home-subtitle">选择你要体验的游戏模式</p>
      <div class="mode-buttons">
        <div class="mode-card" @click="currentPage = 'life'">
          <div class="mode-icon">📿</div>
          <h2>人生转盘</h2>
          <p>随机决定你的人生轨迹，体验多样人生</p>
        </div>
        <div class="mode-card" @click="currentPage = 'battle'">
          <div class="mode-icon">⚔️</div>
          <h2>命运转盘</h2>
          <p>双人对战，通过转盘决定攻防与命运</p>
        </div>
      </div>
    </div>

    <!-- 人生转盘 -->
    <LifeGame v-else-if="currentPage === 'life'" @back="currentPage = 'home'" />

    <!-- 命运转盘（对战） -->
    <BattlePage v-else-if="currentPage === 'battle'" @back="currentPage = 'home'" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import LifeGame from './components/LifeGame.vue'
import BattlePage from './components/BattlePage.vue'

const currentPage = ref('home') // 'home' | 'life' | 'battle'
</script>

<style>
/* 全局重置，保留 body 样式 */
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

/* 主页样式 */
.home-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
}
.home-title {
  font-size: 2.5rem;
  color: #f5e6b0;
  text-shadow: 0 0 20px #f0b34b;
}
.home-subtitle {
  color: #bdaa88;
  margin-bottom: 40px;
}
.mode-buttons {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
  justify-content: center;
}
.mode-card {
  background: #1f1525;
  border: 1px solid #4a3a3a;
  border-radius: 20px;
  padding: 30px;
  width: 250px;
  cursor: pointer;
  transition: transform 0.2s, border-color 0.2s, box-shadow 0.2s;
}
.mode-card:hover {
  transform: translateY(-5px);
  border-color: #f5d78a;
  box-shadow: 0 0 20px rgba(245, 215, 138, 0.3);
}
.mode-icon {
  font-size: 4rem;
  margin-bottom: 15px;
}
.mode-card h2 {
  color: #f5e6b0;
  margin: 10px 0;
}
.mode-card p {
  color: #bdaa88;
  font-size: 0.9rem;
}
</style>