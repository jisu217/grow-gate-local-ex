import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // /api 로 시작하는 요청은 백엔드(8080)로 전달 → CORS 우회
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
