import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Beim Entwickeln läuft der Server auf dem PC (./gradlew :backend:server:run, Port 8080).
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/ws': { target: 'ws://127.0.0.1:8080', ws: true },
    },
  },
  build: { target: 'safari15', sourcemap: false },
});
