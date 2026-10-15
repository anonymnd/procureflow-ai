import {defineConfig} from 'vite';
export default defineConfig({
  build: {outDir: '../src/main/resources/static', emptyOutDir: true, sourcemap: false},
  server: {proxy: {'/api': 'http://127.0.0.1:18084'}}
});
