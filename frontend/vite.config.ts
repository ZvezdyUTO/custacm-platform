import { defineConfig } from 'vitest/config';
import { loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'DEV_');
  return {
    base: '/training-app/',
    plugins: [vue()],
    server: {
      host: '0.0.0.0',
      port: 5173,
      strictPort: true,
      watch: {
        usePolling: env.DEV_USE_POLLING === 'true',
        interval: 500,
      },
      hmr: {
        path: '/training-app-hmr',
        clientPort: env.DEV_HMR_CLIENT_PORT ? Number(env.DEV_HMR_CLIENT_PORT) : undefined,
      },
      proxy: {
        '/api': {
          target: env.DEV_API_TARGET || 'http://localhost:8090',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
    test: {
      environment: 'jsdom',
      environmentOptions: {
        jsdom: { url: 'http://localhost/training/multiple' },
      },
    },
  };
});
