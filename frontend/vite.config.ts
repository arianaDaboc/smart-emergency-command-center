import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development every /api request is forwarded to the Spring Boot server.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
