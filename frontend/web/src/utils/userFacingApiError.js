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

const BY_CODE = {
  AUTH_REQUIRED: 'Сессия истекла или требуется вход. Войдите в аккаунт.',
  ACCESS_DENIED: 'Недостаточно прав для этого действия.',
  EMAIL_ALREADY_REGISTERED: 'Эта почта уже занята.',
  EMAIL_ALREADY_VERIFIED: 'Эта почта уже подтверждена. Войдите в аккаунт.',
  EMAIL_NOT_VERIFIED: 'Подтвердите почту перед входом.',
  INVALID_VERIFICATION_CODE: 'Неверный код подтверждения.',
  INVALID_PASSWORD_RESET_CODE: 'Неверный код сброса пароля.',
  TOO_MANY_VERIFICATION_ATTEMPTS: 'Слишком много попыток. Запросите новый код.',
  VERIFICATION_CODE_EXPIRED: 'Срок действия кода истёк. Запросите новый код.',
  PASSWORD_RESET_CODE_EXPIRED: 'Срок действия кода сброса истёк. Запросите новый код заново.',
  REGISTRATION_CODE_RESEND_COOLDOWN: 'Новый код можно запросить только раз в минуту.',
  USER_NOT_FOUND: 'Пользователь не найден.',
  USERNAME_ALREADY_TAKEN: 'Это имя пользователя уже занято.',
  AVATAR_NOT_FOUND: 'Аватар не найден.',
  OWNED_ROOMS_RESOLUTION_REQUIRED: 'Сначала решите, что делать с активностями, где вы владелец.',
  INVALID_OWNERSHIP_TRANSFER: 'Передать владение можно только текущему участнику комнаты.',
}

/** @type {Array<[RegExp, string]>} */
const ENGLISH_PATTERNS = [
  [/invalid login or password|invalid email or password/i, 'Неверная почта или пароль.'],
  [/invalid verification code/i, 'Неверный код подтверждения.'],
  [/verification code expired|verification code is expired or missing/i, 'Срок действия кода истёк. Запросите новый код.'],
  [/too many verification attempts/i, 'Слишком много попыток. Запросите новый код.'],
  [/email is not verified/i, 'Подтвердите почту перед входом.'],
  [/invalid password reset code/i, 'Неверный код сброса пароля.'],
  [/password reset code expired|password reset code is expired or missing/i, 'Срок действия кода сброса истёк. Запросите новый код заново.'],
  [/password reset requested|password reset confirmed/i, 'Операция выполнена успешно.'],
  [/user not found/i, 'Пользователь не найден.'],
  [/resource not found/i, 'Данные не найдены.'],
  [/authentication is required/i, 'Требуется войти в аккаунт.'],
  [/access is denied/i, 'Недостаточно прав для этого действия.'],
  [/unable to deliver verification code/i, 'Не удалось отправить код на почту. Попробуйте позже.'],
  [/unable to deliver password reset code/i, 'Не удалось отправить код для сброса пароля. Попробуйте позже.'],
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

  if (error.code && BY_CODE[error.code]) {
    return BY_CODE[error.code]
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
