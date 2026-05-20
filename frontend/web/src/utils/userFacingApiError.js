import { isApiError } from '../api/httpClient.js'

function hasCyrillic(text) {
  return /[\u0400-\u04FF]/.test(String(text))
}

/**
 * API иногда возвращает problem+json, где после текста «Доступ запрещён» через двоеточие
 * дописан целиком JSON тела (например, снимок комнаты) — для пользователя это шум.
 */
function stripTrailingJsonObjectAfterColon(message) {
  const s = String(message ?? '').trim()
  const patterns = [
    /^([\s\S]*?):\s*(\{[\s\S]*\})\s*$/,
    /^([\s\S]*?)\.\s+(\{[\s\S]*\})\s*$/,
  ]
  for (const re of patterns) {
    const match = re.exec(s)
    if (!match) {
      continue
    }
    try {
      JSON.parse(match[2])
    } catch {
      continue
    }
    return match[1].trim()
  }
  return s
}

const BY_STATUS = {
  400: 'Запрос не выполнен. Проверьте введённые данные.',
  401: 'Сессия истекла или требуется вход. Войдите в аккаунт.',
  403: 'Недостаточно прав для этого действия.',
  404: 'Запрашиваемые данные не найдены.',
  409: 'Операция невозможна из-за конфликта данных.',
  422: 'Данные не прошли проверку.',
  429: 'Слишком много запросов. Подождите немного.',
  500: 'Что-то пошло не так. Попробуйте позже.',
  502: 'Сервис временно недоступен. Попробуйте позже.',
  503: 'Сервис временно недоступен. Попробуйте позже.',
  504: 'Ответ занял слишком много времени. Попробуйте позже.',
}

/**
 * Стабильные коды из docs/error-codes.md (ветвление по `code`, не по `detail`).
 */
const BY_CODE = {
  INTERNAL_ERROR: 'Произошла внутренняя ошибка. Попробуйте позже.',
  AUTH_REQUIRED: 'Сессия истекла или требуется вход. Войдите в аккаунт.',
  ACCESS_DENIED: 'Доступ запрещён. Войдите в аккаунт или проверьте права на это действие.',
  ONLY_ROOM_OWNER: 'Приглашать пользователей может только создатель активности.',
  VALIDATION_FAILED: 'Проверьте введённые данные и исправьте отмеченные поля.',
  STORAGE_UNAVAILABLE: 'Хранилище файлов временно недоступно. Попробуйте позже.',
  NOTIFICATION_DELIVERY_FAILED:
    'Не удалось отправить письмо с кодом. Проверьте почту или попробуйте позже.',

  ALREADY_ROOM_MEMBER: 'Вы уже участник этой активности.',
  ALREADY_MEMBER: 'Этот пользователь уже участник этой активности.',
  USER_BANNED: 'Этот пользователь заблокирован в этой активности. Сначала снимите блокировку.',
  USERNAME_ALREADY_TAKEN: 'Это имя пользователя уже занято.',
  EMAIL_ALREADY_REGISTERED: 'Эта почта уже зарегистрирована.',
  EMAIL_ALREADY_VERIFIED: 'Эта почта уже подтверждена. Войдите в аккаунт.',
  USER_REGISTRATION_CONFLICT:
    'Регистрация не может быть завершена из-за конфликта данных. Обновите страницу или выберите другую почту.',
  OWNED_ROOMS_RESOLUTION_REQUIRED:
    'Сначала решите, что делать с активностями, где вы владелец: удалить или передать владение.',
  INVALID_OWNERSHIP_TRANSFER: 'Передать владение можно только действующему участнику этой активности.',

  ALREADY_FOLLOWING: 'Вы уже подписаны на этого пользователя.',
  NOT_FOLLOWING: 'Подписка уже отменена.',

  ROOM_NOT_FOUND: 'Активность не найдена или удалена.',
  ROOM_IMAGE_NOT_FOUND: 'Изображение активности не найдено.',
  USER_NOT_FOUND: 'Пользователь не найден.',
  QUESTION_NOT_FOUND: 'Вопрос не найден или удалён.',
  ANSWER_NOT_FOUND: 'Ответ не найден или удалён.',
  JOIN_REQUEST_NOT_FOUND: 'Заявка на вступление не найдена.',
  REQUEST_NOT_FOUND: 'Заявка не найдена.',
  PICTURE_NOT_FOUND: 'Файл изображения не найден.',
  BULLETIN_BOARD_NOT_FOUND: 'Доска объявлений для этой активности отсутствует.',
  AVATAR_NOT_FOUND: 'Аватар не найден.',
  AVATAR_METADATA_NOT_FOUND: 'Данные аватара не найдены.',
  ROOM_MEMBERSHIP_NOT_FOUND:
    'Участие в активности не найдено. Возможно, вы в ней не состоите.',
  ROOM_BAN_NOT_FOUND: 'Запись о блокировке не найдена.',

  USER_BANNED_FROM_ROOM: 'Вы заблокированы в этой активности и не можете в неё вступить.',
  INVALID_VERIFICATION_CODE: 'Неверный код подтверждения почты.',
  VERIFICATION_CODE_EXPIRED: 'Срок действия кода истёк. Запросите новый код.',
  INVALID_PASSWORD_RESET_CODE: 'Неверный код сброса пароля.',
  PASSWORD_RESET_CODE_EXPIRED: 'Срок действия кода сброса истёк. Запросите новый код.',
  EMAIL_NOT_VERIFIED: 'Подтвердите почту перед входом или этим действием.',
  INVALID_CREDENTIALS: 'Неверная почта или пароль.',
  ACCOUNT_DISABLED: 'Аккаунт отключён или заблокирован. Обратитесь в поддержку.',
  AUTHENTICATION_FAILED: 'Не удалось выполнить вход. Проверьте данные и попробуйте снова.',

  REGISTRATION_CODE_RESEND_COOLDOWN: 'Новый код можно запросить не чаще чем раз в минуту.',

  TOO_MANY_VERIFICATION_ATTEMPTS: 'Слишком много попыток ввода кода. Запросите новый код.',
}

/** Подписи полей для `errors[].field` (см. ApiFieldError в docs/api-docs.yaml). */
const FIELD_LABELS = {
  name: 'Название',
  category: 'Категория',
  description: 'Описание',
  status: 'Статус',
  city: 'Город',
  country: 'Страна',
  username: 'Имя пользователя',
  userName: 'Имя пользователя',
  email: 'Электронная почта',
  password: 'Пароль',
  currentPassword: 'Текущий пароль',
  newPassword: 'Новый пароль',
  confirmPassword: 'Подтверждение пароля',
  dateOfBirth: 'Дата рождения',
  verificationCode: 'Код подтверждения',
  file: 'Файл',
  roomId: 'Активность',
  userId: 'Пользователь',
  requestId: 'Заявка',
  questionId: 'Вопрос',
  answerId: 'Ответ',
  question: 'Текст вопроса',
  answer: 'Текст ответа',
  maximumNumberOfPeople: 'Максимум участников',
  maximumParticipants: 'Максимум участников',
  ageRating: 'Возрастной рейтинг',
  chatLink: 'Ссылка на чат',
  dateOfStartEvent: 'Дата и время начала',
  dateOfEndEvent: 'Дата и время окончания',
  frequency: 'Частота / следующее повторение',
  isPublic: 'Публичная активность',
  bulletinBoard: 'Доска объявлений',
  content: 'Текст',
  imageId: 'Изображение',
  action: 'Действие',
  importantRoomUpdates: 'Важные обновления по активностям',
  membershipAccepted: 'Уведомление: заявка принята',
  membershipRejected: 'Уведомление: заявка отклонена',
  activityClosed: 'Уведомление: активность закрыта',
  newJoinRequest: 'Уведомление: новая заявка',
}

function resolveFieldKey(field) {
  const raw = String(field ?? '').trim()
  if (!raw) {
    return ''
  }
  const noIndex = raw.replace(/\[\d+]/g, '')
  const segments = noIndex.split('.').filter(Boolean)
  const last = segments.length > 0 ? segments[segments.length - 1] : noIndex
  return String(last).trim()
}

function translateFieldLabel(field) {
  const key = resolveFieldKey(field)
  if (!key) {
    return ''
  }
  if (FIELD_LABELS[key]) {
    return FIELD_LABELS[key]
  }
  const spaced = key
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/_/g, ' ')
    .trim()
  return spaced ? spaced.charAt(0).toUpperCase() + spaced.slice(1) : key
}

function translateConstraintMessage(message, javaxCode) {
  const code = String(javaxCode ?? '').trim()
  if (code === 'NotBlank' || code === 'NotNull') {
    return 'обязательно для заполнения'
  }
  if (code === 'Email') {
    return 'укажите корректный адрес почты'
  }
  if (code === 'Pattern') {
    return 'неверный формат'
  }
  if (code === 'Min' || code === 'DecimalMin') {
    return 'значение слишком мало'
  }
  if (code === 'Max' || code === 'DecimalMax') {
    return 'значение слишком велико'
  }

  const m = String(message ?? '').trim()
  if (!m) {
    return 'значение не проходит проверку'
  }
  const lower = m.toLowerCase()

  if (
    lower === 'must not be blank' ||
    lower === 'must not be empty' ||
    lower === 'must not be null'
  ) {
    return 'обязательно для заполнения'
  }

  let match = /^size must be between (\d+) and (\d+)$/i.exec(m)
  if (match) {
    return `допустимая длина или размер от ${match[1]} до ${match[2]}`
  }
  match = /^length must be between (\d+) and (\d+)$/i.exec(m)
  if (match) {
    return `длина от ${match[1]} до ${match[2]} символов`
  }
  match = /^must be (?:greater than or equal to|>=) (\d+)$/i.exec(m)
  if (match) {
    return `значение не меньше ${match[1]}`
  }
  match = /^must be (?:less than or equal to|<=) (\d+)$/i.exec(m)
  if (match) {
    return `значение не больше ${match[1]}`
  }
  match = /^must be (?:greater than|>) (\d+)$/i.exec(m)
  if (match) {
    return `значение должно быть больше ${match[1]}`
  }
  match = /^must be (?:less than|<) (\d+)$/i.exec(m)
  if (match) {
    return `значение должно быть меньше ${match[1]}`
  }

  if (/well-formed email|valid email/i.test(m)) {
    return 'укажите корректный адрес почты'
  }
  if (/must match/i.test(lower)) {
    return 'значение не соответствует требуемому формату'
  }

  return 'значение не проходит проверку'
}

function formatFieldErrors(fieldErrors) {
  const parts = fieldErrors.map(({ field, message, code }) => {
    const label = translateFieldLabel(field)
    const text = translateConstraintMessage(message, code)
    if (label) {
      return `${label} — ${text}`
    }
    return text
  })
  return `Проверьте введённые данные: ${parts.join('; ')}.`
}

/** @type {Array<[RegExp, string]>} */
const ENGLISH_PATTERNS = [
  [/invalid login or password|invalid email or password/i, 'Неверная почта или пароль.'],
  [/invalid verification code/i, 'Неверный код подтверждения.'],
  [/verification code expired|verification code is expired or missing/i, 'Срок действия кода истёк. Запросите новый код.'],
  [/too many verification attempts/i, 'Слишком много попыток. Запросите новый код.'],
  [/email is not verified/i, 'Подтвердите почту перед входом.'],
  [/user account is disabled/i, 'Аккаунт временно недоступен. Обратитесь в поддержку.'],
  [/authentication failed/i, 'Не удалось выполнить вход. Проверьте данные и попробуйте снова.'],
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
  [/validation failed/i, 'Проверьте введённые данные.'],
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

  let msg = String(error.message ?? '').trim()
  if (msg && hasCyrillic(msg)) {
    const withoutBlob = stripTrailingJsonObjectAfterColon(msg)
    if (/^Доступ запрещ[её]н\.?$/i.test(withoutBlob)) {
      return BY_CODE.ACCESS_DENIED
    }
    return withoutBlob
  }

  const fieldErrors = Array.isArray(error.fieldErrors) ? error.fieldErrors : null
  if (fieldErrors && fieldErrors.length > 0) {
    return formatFieldErrors(fieldErrors)
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
