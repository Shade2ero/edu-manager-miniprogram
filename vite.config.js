import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [uni()],
  server: {
    port: 3000,
    proxy: {
      // 开发环境代理后端 API 请求
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
