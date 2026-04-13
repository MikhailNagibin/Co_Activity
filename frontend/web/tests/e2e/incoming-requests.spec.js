import { expect, test } from '@playwright/test'

const env = globalThis.process?.env ?? {}

async function mockAuthenticatedIncomingRequests(page, options = {}) {
  let requests = options.requests ?? []
  let processedAction = null

  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 101,
        userId: 101,
        username: 'moderator',
        email: 'moderator@example.com',
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

  await page.route('**/api/users/requests/pending', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(requests),
    })
  })

  await page.route('**/api/users/requests/*', async (route) => {
    const request = route.request()
    if (request.method() !== 'POST') {
      await route.fallback()
      return
    }

    const url = new URL(request.url())
    processedAction = url.searchParams.get('action')
    requests = options.afterProcessRequests ?? []

    await route.fulfill({ status: 204, body: '' })
  })

  return {
    getProcessedAction: () => processedAction,
  }
}

test('pending list disables actions when request is read-only', async ({ page }) => {
  await mockAuthenticatedIncomingRequests(page, {
    requests: [
      {
        requestId: 17,
        userId: 222,
        username: 'readonly-user',
        roomId: 15,
        roomName: 'Read only room',
        status: 'CONSIDERATION',
        createdAt: '2026-04-14T08:30:00Z',
        canManage: false,
      },
    ],
  })

  await page.goto('/profile/incoming-requests')

  await expect(page.getByRole('link', { name: 'readonly-user' })).toHaveAttribute('href', '/users/222')
  await expect(page.getByRole('button', { name: 'Принять', exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Отклонить', exact: true })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Отклонить с баном', exact: true })).toBeDisabled()
})

test('pending list accepts request and refreshes groups', async ({ page }) => {
  const mockedApi = await mockAuthenticatedIncomingRequests(page, {
    requests: [
      {
        requestId: 23,
        userId: 333,
        username: 'pending-user',
        roomId: 7,
        roomName: 'Chess club',
        status: 'CONSIDERATION',
        createdAt: '2026-04-14T09:45:00Z',
      },
    ],
    afterProcessRequests: [],
  })

  page.once('dialog', (dialog) => dialog.accept())

  await page.goto('/profile/incoming-requests')
  await page.getByRole('button', { name: 'Принять' }).click()

  await expect(page.getByText('Заявка принята.')).toBeVisible()
  await expect(page.getByText('Сейчас у вас нет ожидающих заявок на вступление.')).toBeVisible()
  expect(mockedApi.getProcessedAction()).toBe('ACCEPTED')
})

test.describe('real stand moderation', () => {
  test.skip(
    env.PLAYWRIGHT_REAL_E2E !== '1',
    'Set PLAYWRIGHT_REAL_E2E=1 and authenticated PLAYWRIGHT_STORAGE_STATE to run against a stand.',
  )

  test('accepts first pending request on stand', async ({ page }) => {
    await page.goto('/profile/incoming-requests')

    const acceptButton = page.getByRole('button', { name: 'Принять' }).first()
    await expect(acceptButton).toBeEnabled()

    page.once('dialog', (dialog) => dialog.accept())
    await acceptButton.click()

    await expect(page.getByText('Заявка принята.')).toBeVisible()
  })
})
