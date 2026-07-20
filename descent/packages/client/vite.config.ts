import { defineConfig } from 'vite';
import preact from '@preact/preset-vite';

// Bundle budget: 200KB gzip total (spec 6.3). Check with `vite build` output; task D-124.
export default defineConfig({
  plugins: [preact()],
  build: { target: 'es2022', assetsInlineLimit: 8192 },
  server: {
    proxy: {
      '/api': 'http://localhost:8787',
      '/ws': { target: 'ws://localhost:8787', ws: true },
    },
  },
});
