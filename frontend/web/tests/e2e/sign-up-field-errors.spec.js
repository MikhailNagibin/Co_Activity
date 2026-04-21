import { expect, test } from '@playwright/test'

test('sign-up highlights specific invalid fields', async ({ page }) => {
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

  await page.goto('/sign-up')

  await page.getByLabel('Почта').fill('broken-email')
  await page.getByLabel('Имя пользователя').fill('A')
  await page.getByLabel('Пароль', { exact: true }).fill('123')
  await page.getByLabel('Повтор пароля').fill('456')
  await page.getByLabel('Город').fill('Москва')

  await page.getByRole('button', { name: 'Создать аккаунт' }).click()

  await expect(page.getByText('Исправьте подсвеченные поля и попробуйте снова.')).toBeVisible()
  await expect(page.getByText('Введите корректную почту')).toBeVisible()
  await expect(page.getByText('Имя пользователя: от 2 до 20 символов')).toBeVisible()
  await expect(page.getByText('Пароль: от 8 до 128 символов')).toBeVisible()
  await expect(page.getByText('Пароли не совпадают')).toBeVisible()
  await expect(page.getByText('Укажите дату рождения')).toBeVisible()
  await expect(page.getByText('Сначала укажите страну')).toBeVisible()
  await expect(page.getByText('Город можно указать только после страны')).toBeVisible()

  await expect(page.getByLabel('Почта')).toHaveAttribute('aria-invalid', 'true')
  await expect(page.getByLabel('Пароль', { exact: true })).toHaveAttribute('aria-invalid', 'true')
  await expect(page.getByLabel('Повтор пароля')).toHaveAttribute('aria-invalid', 'true')
})
