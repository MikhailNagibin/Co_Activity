import StyledDropdown from './StyledDropdown.jsx'
import { ROOM_CATEGORY_OPTIONS } from '../constants/categoryOptions.js'
import { ROOM_STATUS_OPTIONS } from '../constants/roomStatusOptions.js'

function RoomForm({
  title,
  subtitle = '',
  formData,
  errorMessage = '',
  isSubmitting = false,
  isSubmitDisabled = false,
  submitLabel,
  onFieldChange,
  onSubmit,
  showStatus = false,
  children = null,
}) {
  return (
    <>
      <section className="main-hero">
        <h2>{title}</h2>
        {subtitle ? <h3 className="gray-elem">{subtitle}</h3> : null}
      </section>

      <main className="create-room-page">
        <form className="create-room-form room-editor-form" onSubmit={onSubmit}>
          <div className="create-room-form-row">
            <label htmlFor="name">Название</label>
            <input
              id="name"
              name="name"
              type="text"
              autoComplete="off"
              minLength={3}
              maxLength={100}
              value={formData.name}
              onChange={onFieldChange}
              disabled={isSubmitting}
              required
            />
          </div>

          <div className="create-room-form-row">
            <label htmlFor="category">Категория</label>
            <StyledDropdown
              variant="form"
              id="category"
              ariaLabel="Категория активности"
              options={ROOM_CATEGORY_OPTIONS}
              value={formData.category}
              onChange={(next) =>
                onFieldChange({
                  target: {
                    name: 'category',
                    value: next,
                    type: 'text',
                  },
                })
              }
              disabled={isSubmitting}
            />
          </div>

          {showStatus ? (
            <div className="create-room-form-row">
              <label htmlFor="status">Статус</label>
              <StyledDropdown
                variant="form"
                id="status"
                ariaLabel="Статус активности"
                options={ROOM_STATUS_OPTIONS}
                value={formData.status}
                onChange={(next) =>
                  onFieldChange({
                    target: {
                      name: 'status',
                      value: next,
                      type: 'text',
                    },
                  })
                }
                disabled={isSubmitting}
              />
            </div>
          ) : null}

          <div className="create-room-form-row">
            <label htmlFor="description">Описание</label>
            <textarea
              id="description"
              name="description"
              rows={6}
              maxLength={2000}
              value={formData.description}
              onChange={onFieldChange}
              disabled={isSubmitting}
              required
            />
          </div>

          <div className="create-room-form-row split-fields">
            <div>
              <label htmlFor="maximumNumberOfPeople">Максимум участников</label>
              <input
                id="maximumNumberOfPeople"
                name="maximumNumberOfPeople"
                type="number"
                min={2}
                max={100000}
                value={formData.maximumNumberOfPeople}
                onChange={onFieldChange}
                disabled={isSubmitting}
                required
              />
            </div>
            <div>
              <label htmlFor="ageRating">Возрастной рейтинг (0-21)</label>
              <input
                id="ageRating"
                name="ageRating"
                type="number"
                min={0}
                max={21}
                value={formData.ageRating}
                onChange={onFieldChange}
                disabled={isSubmitting}
                required
              />
            </div>
          </div>

          <div className="create-room-form-row split-fields">
            <div>
              <label htmlFor="country">Страна (необязательно)</label>
              <input
                id="country"
                name="country"
                type="text"
                maxLength={100}
                autoComplete="country-name"
                value={formData.country}
                onChange={onFieldChange}
                disabled={isSubmitting}
              />
            </div>
            <div>
              <label htmlFor="city">Город (необязательно)</label>
              <input
                id="city"
                name="city"
                type="text"
                maxLength={100}
                autoComplete="address-level2"
                value={formData.city}
                onChange={onFieldChange}
                disabled={isSubmitting}
              />
            </div>
          </div>

          <div className="create-room-form-row split-fields">
            <div>
              <label htmlFor="dateOfStartEvent">Начало (необязательно)</label>
              <input
                id="dateOfStartEvent"
                name="dateOfStartEvent"
                type="datetime-local"
                value={formData.dateOfStartEvent}
                onChange={onFieldChange}
                disabled={isSubmitting}
              />
            </div>
            <div>
              <label htmlFor="dateOfEndEvent">Окончание (необязательно)</label>
              <input
                id="dateOfEndEvent"
                name="dateOfEndEvent"
                type="datetime-local"
                value={formData.dateOfEndEvent}
                onChange={onFieldChange}
                disabled={isSubmitting}
              />
            </div>
          </div>

          <div className="create-room-form-row">
            <label htmlFor="frequency">Частота / следующее повторение (необязательно)</label>
            <input
              id="frequency"
              name="frequency"
              type="datetime-local"
              value={formData.frequency}
              onChange={onFieldChange}
              disabled={isSubmitting}
            />
          </div>

          <div className="create-room-form-row">
            <label htmlFor="chatLink">Ссылка на чат (необязательно)</label>
            <input
              id="chatLink"
              name="chatLink"
              type="url"
              placeholder="https://..."
              maxLength={255}
              value={formData.chatLink}
              onChange={onFieldChange}
              disabled={isSubmitting}
            />
          </div>

          <div className="create-room-form-row create-room-checkbox-row">
            <label htmlFor="isPublic">
              <input
                id="isPublic"
                name="isPublic"
                type="checkbox"
                checked={formData.isPublic}
                onChange={onFieldChange}
                disabled={isSubmitting}
              />
              Публичное событие (видно в общей ленте)
            </label>
          </div>

          {children}

          {errorMessage ? (
            <p className="create-room-error" role="alert">
              {errorMessage}
            </p>
          ) : null}

          <button
            type="submit"
            className="create-room-submit"
            disabled={isSubmitting || isSubmitDisabled}
          >
            {isSubmitting ? 'Сохранение...' : submitLabel}
          </button>
        </form>
      </main>
    </>
  )
}

export default RoomForm
