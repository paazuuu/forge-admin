import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const uniPlugin = require('@dcloudio/vite-plugin-uni')

import { defineConfig, loadEnv } from 'vite'
import Unocss from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import path from 'node:path'
import { pluginIcons, pluginPagePathes } from './build/plugin-isme/index.js'

const uni = uniPlugin.default

export default defineConfig(({ mode }) => {
  const viteEnv = loadEnv(mode, process.cwd())
  const { VITE_HTTP_PORT, VITE_REQUEST_PREFIX, VITE_PUBLIC_PATH, VITE_HTTP_PROXY_TARGET } = viteEnv

  return {
    base: VITE_PUBLIC_PATH || '/',
    plugins: [
      uni(),
      Unocss(),
      AutoImport({
        imports: ['vue'],
        dts: false,
      }),
      Components({
        dts: false,
      }),
      pluginPagePathes(),
      pluginIcons(),
    ],
    resolve: {
      alias: {
        '@': path.resolve(process.cwd(), 'src'),
        '~': path.resolve(process.cwd()),
      },
    },
    define: {
      global: 'window',
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@import "@/styles/variables.scss";`,
          silenceDeprecations: ['legacy-js-api', 'color-functions', 'import'],
        },
      },
    },
    server: {
      port: VITE_HTTP_PORT || 3009,
      open: false,
      host: '0.0.0.0',
      proxy: {
        // App 服务代理
        [VITE_REQUEST_PREFIX || '/cbc-server']: {
          target: VITE_HTTP_PROXY_TARGET || 'http://localhost:8581/',
          changeOrigin: true,
          secure: false,
          configure: (proxy, options) => {
            proxy.on('proxyRes', (proxyRes, req) => {
              proxyRes.headers['x-real-url'] = new URL(req.url || '', options.target)?.href || ''
            })
          },
        },
        // WebSocket
        '/ws': {
          target: VITE_HTTP_PROXY_TARGET || 'http://localhost:8581/',
          changeOrigin: true,
          ws: true,
          secure: false,
        },
      },
    },
    build: {
      chunkSizeWarningLimit: 1024,
    },
  }
})
