import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 将 /api 开头的请求代理到本地 SpringBoot 后端，避免浏览器跨域问题
      // 后端接口地址前缀：http://localhost:8123/api
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
      },
    },
  },
})
