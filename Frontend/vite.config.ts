import react from '@vitejs/plugin-react'
import { defineConfig, type ProxyOptions } from 'vite'

// The browser talks to Vite on the same origin. Do not forward that development
// origin to Spring, whose CORS policy applies to direct cross-origin clients.
const backendProxy = {
  target: process.env.ANIMERCH_API_TARGET || 'http://localhost:8080',
  changeOrigin: true,
  configure(proxy) {
    proxy.on('proxyReq', (request) => request.removeHeader('origin'))
  },
} satisfies ProxyOptions

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { '/api': backendProxy },
  },
  preview: {
    proxy: { '/api': backendProxy },
  },
})
