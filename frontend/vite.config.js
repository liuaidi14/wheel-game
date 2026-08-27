import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端地址
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, '') // 如果后端没有 /api 前缀可以去掉
      }
    }
  },
  // 【核心新增】强制 Vite 对 G6 进行预编译，彻底解决 default 导出报错
  optimizeDeps: {
    include: ['@antv/g6']
  }
})