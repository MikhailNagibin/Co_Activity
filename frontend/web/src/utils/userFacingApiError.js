import { isApiError } from '../api/httpClient.js'

function hasCyrillic(text) {
  return /[\u0400-\u04FF]/.test(String(text))
}

const BY_STATUS = {
  400: 'Запрос не выполнен. Проверьте введённые данные.',
  401: 'Сессия истекла или требуется вход. Войдите в аккаунт.',
  403: 'Недостаточно прав для этого действия.',
  404: 'Запрашиваемые данные не найдены.',
  409: 'Операция невозможна из-за конфликта данных.',
  422: 'Данные не прошли проверку.',
  429: 'Слишком много запросов. Подождите немного.',
  500: 'На сервере что-то пошло не так. Попробуйте позже.',
  502: 'Сервис временно недоступен. Попробуйте позже.',
  503: 'Сервис временно недоступен. Попробуйте позже.',
  504: 'Сервер не ответил вовремя. Попробуйте позже.',
}

/** @type {Array<[RegExp, string]>} */
const ENGLISH_PATTERNS = [
  [/invalid login or password/i, 'Неверная почта или пароль.'],
  [/invalid verification code/i, 'Неверный код подтверждения.'],
  [/verification code expired/i, 'Срок действия кода истёк. Запросите новый код.'],
  [/no pending verification found/i, 'Подтверждение входа устарело. Начните вход заново.'],
  [/invalid verification request/i, 'Некорректный запрос подтверждения.'],
  [/user not found/i, 'Пользователь не найден.'],
  [/resource not found/i, 'Данные не найдены.'],
  [/authorization token is required/i, 'Войдите в аккаунт, чтобы продолжить.'],
  [/token is inactive or expired/i, 'Сессия истекла. Войдите снова.'],
  [/invalid or expired token/i, 'Сессия недействительна. Войдите снова.'],
  [/invalid token/i, 'Сессия недействительна. Войдите снова.'],
  [/token is required/i, 'Требуется войти в аккаунт.'],
  [/invalid token subject/i, 'Сессия недействительна. Войдите снова.'],
  [/user id must be positive/i, 'Ошибка авторизации. Войдите снова.'],
  [/jwt secret is not configured|invalid jwt secret configuration/i, 'Сервис авторизации временно недоступен.'],
  [/qa service error/i, 'Сервис вопросов временно недоступен. Попробуйте позже.'],
  [/qa service is unavailable/i, 'Сервис вопросов недоступен. Попробуйте позже.'],
  [/unable to deliver verification code/i, 'Не удалось отправить код на почту. Попробуйте позже.'],
  [/unable to initiate login/i, 'Не удалось начать вход. Попробуйте позже.'],
  [/unable to verify login/i, 'Не удалось подтвердить вход. Попробуйте снова.'],
  [/unable to update password/i, 'Не удалось сменить пароль. Попробуйте позже.'],
  [/unable to register user/i, 'Не удалось зарегистрироваться. Попробуйте позже.'],
  [/unable to load profile/i, 'Не удалось загрузить профиль. Попробуйте позже.'],
  [/unable to update profile/i, 'Не удалось сохранить профиль. Попробуйте позже.'],
  [/unable to delete account/i, 'Не удалось удалить аккаунт. Попробуйте позже.'],
  [/unable to update notification settings/i, 'Не удалось сохранить настройки уведомлений.'],
  [/current password is incorrect/i, 'Текущий пароль указан неверно.'],
  [/new password must be different/i, 'Новый пароль должен отличаться от текущего.'],
  [/password values must not be empty/i, 'Заполните поля пароля.'],
  [/only owners can delete rooms/i, 'Удалять комнату может только владелец.'],
  [/only admin users can be demoted/i, 'Снять права администратора можно только с администратора.'],
  [/only room owner can perform this action/i, 'Это действие доступно только владельцу комнаты.'],
  [/room owner cannot leave the room/i, 'Владелец не может покинуть комнату.'],
  [/user is banned from this room/i, 'Вы заблокированы в этой комнате.'],
  [/user lacks moderation rights/i, 'Недостаточно прав для модерации.'],
  [/cannot cancel request created by another user/i, 'Нельзя отменить чужую заявку.'],
  [/insufficient privileges/i, 'Недостаточно прав для этой операции.'],
  [/user is not a participant/i, 'Вы не участник этой комнаты.'],
  [/target user is not a member/i, 'Пользователь не состоит в комнате.'],
  [/room capacity exceeded/i, 'Достигнут лимит участников.'],
  [/join request already processed/i, 'Заявка уже обработана.'],
  [/join request not found/i, 'Заявка не найдена.'],
  [/unsupported join request action/i, 'Действие с заявкой не поддерживается.'],
  [/room not found/i, 'Комната не найдена.'],
  [/room could not be created/i, 'Не удалось создать комнату. Попробуйте позже.'],
  [/failed to update bulletin board/i, 'Не удалось обновить доску объявлений.'],
  [/bulletin content cannot be empty/i, 'Текст объявления не может быть пустым.'],
  [/room name cannot be empty/i, 'Укажите название комнаты.'],
  [/category is required/i, 'Выберите категорию.'],
  [/maximum number of people is required/i, 'Укажите максимальное число участников.'],
  [/owner id is required|room id is required|user id is required|request id is required|action is required|author id is required/i, 'Некорректные данные запроса. Обновите страницу и попробуйте снова.'],
  [/room creation request is required/i, 'Заполните данные о комнате.'],
  [/update request is required/i, 'Нет данных для сохранения.'],
  [/notification settings are required/i, 'Укажите настройки уведомлений.'],
  [/filtering rooms by city or country is not supported/i, 'Такой фильтр пока не поддерживается.'],
  [/cannot process consideration status/i, 'Некорректный статус заявки.'],
  [/request failed with status \d+/i, 'Запрос не выполнен. Попробуйте позже.'],
]

/**
 * Превращает ответ API в короткое сообщение без «технического» английского для пользователя.
 * Текст с кириллицей возвращается как есть (уже локализован или с фронта).
 */
export function getUserFacingApiMessage(error, fallback = 'Что-то пошло не так. Попробуйте снова.') {
  if (!isApiError(error)) {
    return fallback
  }

  const msg = String(error.message ?? '').trim()
  if (msg && hasCyrillic(msg)) {
    return msg
  }

  for (const [pattern, ru] of ENGLISH_PATTERNS) {
    if (pattern.test(msg)) {
      return ru
    }
  }

  const byStatus = BY_STATUS[error.status]
  if (byStatus) {
    return byStatus
  }

  if (!msg) {
    return fallback
  }

  return fallback
}
