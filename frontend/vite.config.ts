import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: true,
      eslintrc: {
        enabled: true
      }
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: true
    })
  ],
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler', // 使用新的 Sass API
        silenceDeprecations: ['legacy-js-api'] // 抑制来自第三方库的废弃警告
      }
    }
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@modules': resolve(__dirname, 'src/modules'),
      '@shared': resolve(__dirname, 'src/shared')
    }
  },
  server: {
    port: 3000,
    open: true,
    cors: true,
    // 支持 SPA History 模式路由
    historyApiFallback: true,
    // 明确指定 HMR 配置
    hmr: {
      protocol: 'ws',
      host: 'localhost'
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'static',
    sourcemap: false,
    minify: 'terser',
    chunkSizeWarningLimit: 1500,
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    },
    rollupOptions: {
      output: {
        chunkFileNames: 'static/js/[name]-[hash].js',
        entryFileNames: 'static/js/[name]-[hash].js',
        assetFileNames: 'static/[ext]/[name]-[hash].[ext]',
        manualChunks(id) {
          // 将 node_modules 中的依赖分离成单独的 chunk
          if (id.includes('node_modules')) {
            // ===== 重度依赖拆分 (P0优化) =====
            // BPMN工作流设计器 (~1.5MB) - 仅工作流页面加载
            if (id.includes('bpmn-js') || id.includes('camunda')) {
              return 'bpmn-designer'
            }
            // ECharts图表库 (~800KB) - 仅图表页面加载
            if (id.includes('echarts')) {
              return 'echarts'
            }
            // Excel处理库 (~600KB) - 仅导入导出时加载
            if (id.includes('xlsx')) {
              return 'xlsx'
            }
            // 富文本编辑器 (~500KB)
            if (id.includes('@wangeditor')) {
              return 'editor'
            }
            // TinyMCE 富文本编辑器
            if (id.includes('tinymce')) {
              return 'tinymce'
            }

            // ===== 中型依赖拆分 =====
            // 图标库 - 按需拆分避免全量打包
            if (id.includes('lucide-vue-next')) {
              return 'icons-lucide'
            }
            if (id.includes('@heroicons')) {
              return 'icons-heroicons'
            }
            // Markdown 解析
            if (id.includes('marked') || id.includes('dompurify')) {
              return 'markdown'
            }
            // 拖拽库
            if (id.includes('vuedraggable') || id.includes('sortablejs')) {
              return 'draggable'
            }
            // Konva 画布引擎
            if (id.includes('konva')) {
              return 'konva'
            }

            // ===== 核心框架拆分 =====
            // Element Plus UI库
            if (id.includes('element-plus')) {
              return 'element-plus'
            }
            // Vue 全家桶
            if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) {
              return 'vue-vendor'
            }
            // HTTP客户端
            if (id.includes('axios')) {
              return 'axios'
            }

            // 其他第三方库 (工具类、图标等)
            return 'vendor'
          }
        }
      }
    }
  }
})