import { defineConfig, devices } from '@playwright/test'

const env = globalThis.process?.env ?? {}
const externalBaseUrl = env.PLAYWRIGHT_BASE_URL

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  timeout: 30_000,
  expect: {
    timeout: 5_000,
  },
  use: {
    baseURL: externalBaseUrl || 'http://127.0.0.1:4173',
    storageState: env.PLAYWRIGHT_STORAGE_STATE || undefined,
    trace: 'on-first-retry',
  },
  webServer: externalBaseUrl
    ? undefined
    : {
        command: 'npm run dev -- --host 127.0.0.1 --port 4173',
        url: 'http://127.0.0.1:4173',
        reuseExistingServer: true,
        timeout: 120_000,
      },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
