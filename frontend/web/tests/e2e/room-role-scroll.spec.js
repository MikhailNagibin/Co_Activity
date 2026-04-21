import { expect, test } from '@playwright/test'

test('role assignment keeps scroll position near management section', async ({ page }) => {
  let participants = [
    {
      id: 101,
      userName: 'owner',
      role: 'OWNER',
      city: 'Москва',
      country: 'Россия',
      description: 'Создатель комнаты',
    },
    {
      id: 202,
      userName: 'member-one',
      role: 'PARTICIPANT',
      city: 'Казань',
      country: 'Россия',
      description: 'Участник для назначения',
    },
  ]

  const roomPayload = {
    id: 77,
    name: 'Комната для ролей',
    description: `${'Длинное описание '.repeat(180)}`,
    category: 'EDUCATION',
    isPublic: false,
    ageRating: 12,
    participantCount: 2,
    maximumParticipants: 30,
    creator: {
      id: 101,
      userName: 'owner',
      city: 'Москва',
      country: 'Россия',
    },
    membershipStatus: {
      roomId: 77,
      status: 'PARTICIPANT',
      role: 'OWNER',
      canJoin: false,
    },
  }

  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 101,
        userId: 101,
        username: 'owner',
        email: 'owner@example.com',
      }),
    })
  })

  await page.route('**/api/auth/csrf', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      headers: {
        'Set-Cookie': 'XSRF-TOKEN=playwright-csrf-token; Path=/',
      },
      body: JSON.stringify({ token: 'playwright-csrf-token' }),
    })
  })

  await page.route('**/api/rooms/77', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        ...roomPayload,
        participantCount: participants.length,
      }),
    })
  })

  await page.route('**/api/rooms/77/membership/status', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        roomId: 77,
        status: 'PARTICIPANT',
        role: 'OWNER',
        canJoin: false,
      }),
    })
  })

  await page.route('**/api/rooms/77/participants', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(participants),
    })
  })

  await page.route('**/api/rooms/77/bans', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    })
  })

  await page.route('**/api/users/rooms/77/requests/pending', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    })
  })

  await page.route('**/api/users/rooms/77/admins/202', async (route) => {
    participants = participants.map((participant) =>
      participant.id === 202 ? { ...participant, role: 'ADMIN' } : participant,
    )

    await route.fulfill({
      status: 204,
      contentType: 'application/json',
      body: '',
    })
  })

  await page.goto('/rooms/77')

  await page.getByRole('tab', { name: 'Роли' }).click()
  await expect(page.getByRole('button', { name: 'Назначить админом' })).toBeVisible()

  await page.evaluate(() => window.scrollTo({ top: document.body.scrollHeight, behavior: 'auto' }))
  const beforeClickY = await page.evaluate(() => window.scrollY)

  page.once('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: 'Назначить админом' }).click()

  await expect(page.getByText('Роль администратора назначена.')).toBeVisible()
  await expect(page.getByText('Админ', { exact: true })).toBeVisible()

  await expect
    .poll(async () => page.evaluate(() => window.scrollY), { timeout: 5000 })
    .toBeGreaterThan(beforeClickY - 120)
})
