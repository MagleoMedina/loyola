import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const API = 'http://localhost:8082'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/upload': { target: API, changeOrigin: true },
      '/parse-names': { target: API, changeOrigin: true },
      '/student-report': { target: API, changeOrigin: true },
      '/momento': { target: API, changeOrigin: true },
      '/literals': { target: API, changeOrigin: true },
      '/generate-docx': { target: API, changeOrigin: true },
      '/ia-test': { target: API, changeOrigin: true },
    }
  }
})
