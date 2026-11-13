import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // 本地开发默认连本机后端；需要连 Docker 中的后端时设置 VITE_PROXY_TARGET
  const backendUrl = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    server: {
      port: 5173,
      host: '0.0.0.0',
      proxy: {
        '/api': {
          target: backendUrl,
          changeOrigin: true
        }
      }
    },
    build: {
      rollupOptions: {
        output: {
          // 拆分体积较大的第三方库，避免单个 chunk 超过 1MB 拖慢首屏加载
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            'element-plus': ['element-plus', '@element-plus/icons-vue'],
            echarts: ['echarts']
          }
        }
      }
    }
  }
})
