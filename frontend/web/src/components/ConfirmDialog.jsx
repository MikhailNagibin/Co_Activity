import { useEffect, useId } from 'react'

/**
 * Themed confirmation modal (light/dark via existing .qa-modal* styles).
 * @param {object} props
 * @param {boolean} props.open
 * @param {string} [props.title]
 * @param {string} props.message
 * @param {string} [props.confirmLabel]
 * @param {string} [props.cancelLabel]
 * @param {'danger'|'primary'} [props.confirmVariant]
 * @param {boolean} [props.confirmDisabled]
 * @param {() => void} props.onConfirm
 * @param {() => void} props.onCancel
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'OK',
  cancelLabel = 'Отмена',
  confirmVariant = 'danger',
  confirmDisabled = false,
  onConfirm,
  onCancel,
}) {
  const titleId = useId()
  const bodyId = useId()

  useEffect(() => {
    if (!open) {
      return undefined
    }
    const onKey = (e) => {
      if (e.key === 'Escape') {
        onCancel?.()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onCancel])

  if (!open) {
    return null
  }

  const heading = title?.trim() ? title.trim() : ''
  const confirmButtonClass =
    confirmVariant === 'primary'
      ? 'qa-modal-confirm-btn--primary'
      : 'qa-thread-action-btn qa-thread-action-btn--danger'

  return (
    <div
      className="qa-modal-overlay"
      role="presentation"
      onClick={() => onCancel?.()}
    >
      <div
        className="qa-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby={heading ? titleId : undefined}
        aria-describedby={bodyId}
        aria-label={heading ? undefined : 'Подтверждение'}
        onClick={(e) => e.stopPropagation()}
      >
        {heading ? (
          <h2 id={titleId} className="qa-modal-title">
            {heading}
          </h2>
        ) : null}
        <p id={bodyId} className="qa-modal-body">
          {message}
        </p>
        <div className="qa-modal-actions">
          <button
            type="button"
            className="qa-thread-action-btn"
            onClick={() => onCancel?.()}
            disabled={confirmDisabled}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            className={confirmButtonClass}
            onClick={() => onConfirm?.()}
            disabled={confirmDisabled}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
