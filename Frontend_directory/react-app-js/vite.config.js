import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  css: {
    preprocessorOptions: {
      scss: {
        // Bootstrap 5 SCSS still triggers Dart Sass deprecations; silence third-party noise only.
        silenceDeprecations: [
          'import',
          'global-builtin',
          'color-functions',
          'if-function',
          'legacy-js-api',
        ],
      },
    },
  },
})
