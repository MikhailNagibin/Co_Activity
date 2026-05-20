import { useCallback, useRef, useState } from 'react'
import ConfirmDialog from '../components/ConfirmDialog.jsx'

const DEFAULTS = {
  title: 'Подтверждение',
  message: '',
  confirmLabel: 'Продолжить',
  cancelLabel: 'Отмена',
  variant: 'danger',
}

/** Promise-based confirm dialog; render `confirmDialog` in the component tree. */
export function useConfirmDialog() {
  const resolveRef = useRef(null)
  const [open, setOpen] = useState(false)
  const [config, setConfig] = useState(DEFAULTS)

  const requestConfirm = useCallback((options) => {
    return new Promise((resolve) => {
      resolveRef.current = resolve
      setConfig({ ...DEFAULTS, ...options })
      setOpen(true)
    })
  }, [])

  const finish = useCallback((value) => {
    setOpen(false)
    const r = resolveRef.current
    resolveRef.current = null
    if (r) {
      r(value)
    }
  }, [])

  const confirmDialog = open ? (
    <ConfirmDialog
      open
      title={config.title}
      message={config.message}
      confirmLabel={config.confirmLabel}
      cancelLabel={config.cancelLabel}
      confirmVariant={config.variant}
      onConfirm={() => finish(true)}
      onCancel={() => finish(false)}
    />
  ) : null

  return { requestConfirm, confirmDialog }
}
