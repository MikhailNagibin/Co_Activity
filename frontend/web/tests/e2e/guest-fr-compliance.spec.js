/**
 * E2E: неавторизованный пользователь (ФТ п. 9–16, фронтенд).
 * Фиксирует ожидаемое поведение гостя в соответствии с функциональными требованиями.
 */
import { expect, test } from '@playwright/test'

async function mockGuestSession(page) {
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'Unauthorized' }),
    })
  })
}

test.describe('Guest FR 9–16 (unauthenticated)', () => {
  test('main: no create activity, header offers sign-in', async ({ page }) => {
    await mockGuestSession(page)
    await page.route('**/api/rooms**', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.fallback()
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 902,
            name: 'Гостевая проверка ФТ',
            description: 'Описание',
            category: 'SPORT',
            dateOfStartEvent: '2026-06-01T12:00:00Z',
            creator: { id: 1, userName: 'creator' },
            participantCount: 2,
            maximumParticipants: 20,
            isPublic: true,
            hasProtectedAccess: false,
          },
        ]),
      })
    })

    await page.goto('/main')

    await expect(page.locator('header#main-header a.app-header-auth')).toHaveText('Войти')
    await expect(page.getByRole('link', { name: 'Создать активность' })).toHaveCount(0)
    await expect(page.getByRole('heading', { name: 'Гостевая проверка ФТ' })).toBeVisible()
  })

  test('qa list: guest hero copy, no ask button, no profile links on cards', async ({ page }) => {
    await mockGuestSession(page)
    await page.route('**/api/qa/questions**', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.fallback()
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 503,
            question: 'Вопрос из списка для гостя',
            category: 'EDUCATION',
            author: { id: 101, userName: 'CardAuthor' },
            createdAt: '2026-03-01T10:00:00Z',
            answersCount: 0,
          },
        ]),
      })
    })

    await page.goto('/qa')

    await expect(
      page.getByText(
        'Читайте обсуждения сообщества. Чтобы задать вопрос или ответить, войдите в аккаунт.',
      ),
    ).toBeVisible()
    await expect(page.getByRole('link', { name: 'Задать вопрос' })).toHaveCount(0)
    const card = page.locator('.qa-question-card').first()
    await expect(card.getByRole('link', { name: 'CardAuthor' })).toHaveCount(0)
    await expect(card.getByText('CardAuthor')).toBeVisible()
  })

  test('room page: guest sees join hint, no bulletin board section', async ({ page }) => {
    await mockGuestSession(page)
    await page.route('**/api/rooms/903', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.fallback()
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 903,
          name: 'Комната для гостя',
          status: 'ACTIVE',
          isPublic: true,
          category: 'MUSIC',
          description: 'Описание комнаты',
          participantCount: 3,
          maximumParticipants: 30,
          hasProtectedAccess: false,
          createdAt: '2026-04-10T08:00:00.000Z',
          creator: { id: 202, userName: 'RoomOwnerFr' },
        }),
      })
    })

    await page.goto('/rooms/903')

    await expect(page.getByText('Для гостей вступление и заявки недоступны')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Доска объявлений' })).toHaveCount(0)
    await expect(page.getByRole('link', { name: 'RoomOwnerFr' })).toHaveCount(0)
    await expect(page.getByText('RoomOwnerFr')).toBeVisible()
    await expect(page.getByText('Создано', { exact: true }).first()).toBeVisible()
  })
})
