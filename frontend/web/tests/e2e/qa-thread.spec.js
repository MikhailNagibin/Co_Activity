import { expect, test } from '@playwright/test'

function buildThreadJson(questionOverrides = {}) {
  return {
    question: {
      id: 555,
      category: 'EDUCATION',
      question: 'Исходный текст вопроса для теста',
      author: {
        id: 99,
        userName: 'other-user',
        dateOfBirth: '2000-01-01T00:00:00Z',
        city: null,
        country: null,
        description: null,
        avatarId: null,
        avatarUrl: null,
      },
      createdAt: '2026-04-14T10:00:00Z',
      ...questionOverrides,
    },
    answers: [],
  }
}

test.describe('Q&A thread', () => {
  test('guest sees thread but not question edit/delete controls', async ({ page }) => {
    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Unauthorized' }),
      })
    })

    await page.route('**/api/qa/questions/555', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.fallback()
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(buildThreadJson()),
      })
    })

    await page.goto('/questions/555')

    await expect(page.getByRole('heading', { name: /Исходный текст вопроса/ })).toBeVisible()
    await expect(page.getByTestId('qa-thread-edit-question')).toHaveCount(0)
    await expect(page.getByTestId('qa-thread-delete-question')).toHaveCount(0)
  })

  test('authenticated author can edit own question', async ({ page }) => {
    let questionBody = 'Мой вопрос до правки'

    await page.route('**/api/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 42,
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

    await page.route('**/api/qa/questions/777', async (route) => {
      const req = route.request()
      const method = req.method()
      if (method === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(
            buildThreadJson({
              id: 777,
              question: questionBody,
              author: {
                id: 42,
                userName: 'owner',
                dateOfBirth: '2000-01-01T00:00:00Z',
                city: null,
                country: null,
                description: null,
                avatarId: null,
                avatarUrl: null,
              },
            }),
          ),
        })
        return
      }
      if (method === 'PUT') {
        const json = req.postDataJSON()
        questionBody = json.question
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 777,
            category: json.category,
            question: json.question,
            author: {
              id: 42,
              userName: 'owner',
              dateOfBirth: '2000-01-01T00:00:00Z',
              city: null,
              country: null,
              description: null,
              avatarId: null,
              avatarUrl: null,
            },
          }),
        })
        return
      }
      await route.fallback()
    })

    await page.goto('/questions/777')

    await expect(page.getByTestId('qa-thread-edit-question')).toBeVisible()
    await page.getByTestId('qa-thread-edit-question').click()

    await expect(page.getByTestId('qa-thread-question-editor')).toBeVisible()
    const editor = page.locator('#qa-edit-question-text')
    await editor.fill('Мой вопрос после правки')

    await page.getByTestId('qa-thread-save-question').click()

    await expect(page.getByText('Мой вопрос после правки')).toBeVisible()
  })
})
