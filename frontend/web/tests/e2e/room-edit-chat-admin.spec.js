import { expect, test } from '@playwright/test'

test('room edit: admin cannot open owner-only edit form', async ({ page }) => {
  const roomId = 88
  const chatUrl = 'https://example.com/room-chat'

  const roomPayload = {
    id: roomId,
    name: 'Комната админа',
    description: 'Описание',
    category: 'SPORT',
    isPublic: true,
    ageRating: 0,
    participantCount: 2,
    maximumParticipants: 20,
    chatLink: chatUrl,
    status: 'ACTIVE',
    creator: { id: 101, userName: 'owner' },
    images: [],
  }

  const fulfillMe = (userId, username) =>
    page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: userId,
          userId,
          username,
          email: `${username}@example.com`,
        }),
      })
    })

  await fulfillMe(202, 'room-admin')
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

  await page.route(`**/api/rooms/${roomId}`, async (route) => {
    if (route.request().method() !== 'GET') {
      await route.fallback()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(roomPayload),
    })
  })

  await page.route(`**/api/rooms/${roomId}/membership/status`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        roomId,
        status: 'PARTICIPANT',
        role: 'ADMIN',
        canJoin: false,
      }),
    })
  })

  await page.goto(`/rooms/${roomId}/edit`)

  await expect(page.getByRole('alert')).toContainText('Недостаточно прав для редактирования этой комнаты.')
})

test('room edit: owner can change schedule, chat, and non-static fields; static fields hidden', async ({
  page,
}) => {
  const roomId = 89
  const chatUrl = 'https://example.com/owner-chat'

  const roomPayload = {
    id: roomId,
    name: 'Комната владельца',
    description: 'Описание',
    category: 'SPORT',
    isPublic: true,
    ageRating: 5,
    participantCount: 1,
    maximumParticipants: 15,
    chatLink: chatUrl,
    status: 'ACTIVE',
    creator: { id: 303, userName: 'owner-user' },
    images: [],
  }

  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 303,
        userId: 303,
        username: 'owner-user',
        email: 'owner-user@example.com',
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

  await page.route(`**/api/rooms/${roomId}`, async (route) => {
    if (route.request().method() !== 'GET') {
      await route.fallback()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(roomPayload),
    })
  })

  await page.route(`**/api/rooms/${roomId}/membership/status`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        roomId,
        status: 'PARTICIPANT',
        role: 'OWNER',
        canJoin: false,
      }),
    })
  })

  await page.goto(`/rooms/${roomId}/edit`)

  await expect(page.locator('#name')).toHaveCount(0)
  await expect(page.locator('#description')).toHaveCount(0)
  await expect(page.locator('#ageRating')).toHaveCount(0)
  await expect(page.locator('#isPublic')).toHaveCount(0)
  await expect(page.getByRole('button', { name: /категория активности/i })).toHaveCount(0)

  await expect(page.locator('#chatLink')).toBeEnabled()
  await expect(page.locator('#maximumNumberOfPeople')).toBeEnabled()
  await expect(page.locator('#country')).toBeEnabled()
  await expect(page.locator('#city')).toBeEnabled()
  await expect(page.locator('#dateOfStartEvent')).toBeEnabled()
  await expect(page.locator('#dateOfEndEvent')).toBeEnabled()
  await expect(page.locator('#frequency')).toBeEnabled()
})
