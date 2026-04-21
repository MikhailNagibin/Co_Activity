import { expect, test } from '@playwright/test'

async function mockSignInError(page, { code, detail }) {
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        type: 'urn:coactivity:error:AUTH_REQUIRED',
        title: 'Unauthorized',
        status: 401,
        detail: 'Authentication is required',
        code: 'AUTH_REQUIRED',
      }),
    })
  })

  await page.route('**/api/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ token: 'playwright-csrf-token' }),
    })
  })

  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      status: 403,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        type: `urn:coactivity:error:${code}`,
        title: 'Forbidden',
        status: 403,
        detail,
        code,
      }),
    })
  })
}

test.describe('Sign-in error messages', () => {
  test('shows user-friendly message for INVALID_CREDENTIALS', async ({ page }) => {
    await mockSignInError(page, {
      code: 'INVALID_CREDENTIALS',
      detail: 'Invalid email or password',
    })

    await page.goto('/sign-in')
    await page.getByLabel('Почта').fill('user@example.com')
    await page.getByLabel('Пароль').fill('Password123')
    await page.getByRole('button', { name: 'Войти' }).click()

    await expect(page.getByText('Неверная почта или пароль.')).toBeVisible()
  })

  test('shows user-friendly message for ACCOUNT_DISABLED', async ({ page }) => {
    await mockSignInError(page, {
      code: 'ACCOUNT_DISABLED',
      detail: 'User account is disabled',
    })

    await page.goto('/sign-in')
    await page.getByLabel('Почта').fill('disabled@example.com')
    await page.getByLabel('Пароль').fill('Password123')
    await page.getByRole('button', { name: 'Войти' }).click()

    await expect(
      page.getByText('Аккаунт отключён или заблокирован. Обратитесь в поддержку.'),
    ).toBeVisible()
  })
})
