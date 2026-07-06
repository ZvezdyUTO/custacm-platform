import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/training-data': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/health/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: () => '/health',
      },
      '/health/training-data': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: () => '/health',
      },
    },
  },
});
