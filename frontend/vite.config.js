import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const backendTarget = 'http://127.0.0.1:8080'

const proxyOptions = {
  target: backendTarget,
  changeOrigin: true,
  timeout: 120_000,
  proxyTimeout: 120_000,
}

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': proxyOptions,
      '/uploads': proxyOptions,
    },
  },
})
