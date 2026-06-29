import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/llm': { target: 'http://localhost:80', changeOrigin: true },
      '/login': { target: 'http://localhost:80', changeOrigin: true },
    },
  },
})
