import { expect, test } from '@playwright/test'

test('main page cycles room images and image urls respond 200', async ({ page }) => {
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'Unauthorized' }),
    })
  })

  await page.route('**/api/rooms**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 701,
          name: 'Многокарточная активность',
          description: 'Проверка image carousel на карточке',
          category: 'SPORT',
          dateOfStartEvent: '2026-05-01T09:00:00Z',
          creator: { userName: 'owner' },
          participantCount: 1,
          maximumParticipants: 10,
          images: [
            { id: 1, url: '/__e2e/room-image-1.png', order: 0 },
            { id: 2, url: '/__e2e/room-image-2.png', order: 1 },
          ],
        },
      ]),
    })
  })

  await page.route('**/__e2e/room-image-*.png', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'image/png',
      body: 'mock-image',
    })
  })

  await page.goto('/main')

  const cardLink = page.locator('.activity-card-outer-link').first()
  await expect(cardLink.getByRole('heading', { name: 'Многокарточная активность' })).toBeVisible()
  const cardImage = cardLink.locator('.activity-card-image')
  await expect(cardImage).toBeVisible()

  const firstSrc = await cardImage.getAttribute('src')
  expect(firstSrc).toBeTruthy()

  await cardLink.locator('.activity-card-image-shell').hover()
  await expect
    .poll(async () => cardImage.getAttribute('src'), { timeout: 3000 })
    .not.toBe(firstSrc)

  const secondSrc = await cardImage.getAttribute('src')
  expect(secondSrc).toBeTruthy()

  const firstImageUrl = new URL(firstSrc, page.url()).toString()
  const firstResponse = await page.request.get(firstImageUrl)
  expect(firstResponse.status()).toBe(200)

  const secondImageUrl = new URL(secondSrc, page.url()).toString()
  const secondResponse = await page.request.get(secondImageUrl)
  expect(secondResponse.status()).toBe(200)
})
