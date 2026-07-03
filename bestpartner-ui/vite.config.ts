import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/llm': { target: 'http://localhost:80', changeOrigin: true },
      // 僅代理實際登入 API 路徑（POST /login/），保留裸 `/login` 給 SPA 前端路由。
      // 否則 401 攔截器 `window.location.href='/login'` 會被代理到後端 → 回 405。
      '/login/': { target: 'http://localhost:80', changeOrigin: true },
    },
  },
})
