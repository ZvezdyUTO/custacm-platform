import {fileURLToPath, URL} from 'node:url'
import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

const apiProxy = {
	target: 'http://localhost:8090',
	changeOrigin: true,
	rewrite: path => path.replace(/^\/api/, ''),
}

const trainingProxy = {
	target: 'http://localhost:5173',
	changeOrigin: true,
	ws: true,
}

// Author: huangbingrui.awa
export default defineConfig({
	plugins: [vue()],
	resolve: {
		alias: {
			'@': fileURLToPath(new URL('./src', import.meta.url)),
			assets: fileURLToPath(new URL('./src/assets', import.meta.url)),
			common: fileURLToPath(new URL('./src/common', import.meta.url)),
			components: fileURLToPath(new URL('./src/components', import.meta.url)),
			api: fileURLToPath(new URL('./src/api', import.meta.url)),
			views: fileURLToPath(new URL('./src/views', import.meta.url)),
			plugins: fileURLToPath(new URL('./src/plugins', import.meta.url)),
		},
		extensions: ['.mjs', '.js', '.mts', '.ts', '.jsx', '.tsx', '.json', '.vue'],
	},
	server: {
		host: '0.0.0.0',
		port: 4180,
		strictPort: true,
		proxy: {
			'/api': apiProxy,
			'/training-app': trainingProxy,
			'/training-app-hmr': trainingProxy,
		},
	},
	preview: {
		port: 4174,
		proxy: {
			'/api': apiProxy,
		},
	},
	test: {
		environment: 'jsdom',
		environmentOptions: {
			jsdom: {
				url: 'http://localhost/',
			},
		},
	},
})
