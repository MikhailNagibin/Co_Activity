import { useCallback, useEffect, useId, useRef, useState } from 'react'

/**
 * Кастомный выпадающий список (вместо нативного select): единый стиль с тулбаром и формами.
 */
function StyledDropdown({
  options,
  value,
  onChange,
  id,
  ariaLabel,
  disabled = false,
  variant = 'toolbar',
  className = '',
}) {
  const [open, setOpen] = useState(false)
  const [highlight, setHighlight] = useState(-1)
  const rootRef = useRef(null)
  const listId = useId()

  const selected = options.find((o) => o.value === value) ?? options[0]
  const selectedLabel = selected?.label ?? '—'

  const close = useCallback(() => {
    setOpen(false)
    setHighlight(-1)
  }, [])

  const pick = useCallback(
    (nextValue) => {
      onChange(nextValue)
      close()
    },
    [onChange, close],
  )

  useEffect(() => {
    if (!open) {
      return undefined
    }
    const onDocPointer = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        close()
      }
    }
    document.addEventListener('mousedown', onDocPointer)
    document.addEventListener('touchstart', onDocPointer, { passive: true })
    return () => {
      document.removeEventListener('mousedown', onDocPointer)
      document.removeEventListener('touchstart', onDocPointer)
    }
  }, [open, close])

  useEffect(() => {
    if (!open) {
      return undefined
    }

    const onKey = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        close()
        return
      }
      if (event.key === 'ArrowDown') {
        event.preventDefault()
        setHighlight((h) => {
          const base = h < 0 ? -1 : h
          return Math.min(options.length - 1, base + 1)
        })
      }
      if (event.key === 'ArrowUp') {
        event.preventDefault()
        setHighlight((h) => {
          const base = h < 0 ? options.length : h
          return Math.max(0, base - 1)
        })
      }
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault()
        const i = highlight >= 0 ? highlight : options.findIndex((o) => o.value === value)
        const safe = i >= 0 ? i : 0
        if (options[safe]) {
          pick(options[safe].value)
        }
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, options, value, highlight, close, pick])

  const toggle = () => {
    if (disabled) {
      return
    }
    if (open) {
      close()
    } else {
      const idx = options.findIndex((o) => o.value === value)
      setHighlight(idx >= 0 ? idx : 0)
      setOpen(true)
    }
  }

  const onTriggerKeyDown = (event) => {
    if (disabled) {
      return
    }
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      if (!open) {
        const idx = options.findIndex((o) => o.value === value)
        setHighlight(idx >= 0 ? idx : 0)
        setOpen(true)
      }
      return
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      const idx = options.findIndex((o) => o.value === value)
      setHighlight(idx >= 0 ? idx : 0)
      setOpen(true)
    }
  }

  return (
    <div
      ref={rootRef}
      className={`styled-dropdown styled-dropdown--${variant}${open ? ' styled-dropdown--open' : ''}${className ? ` ${className}` : ''}`.trim()}
    >
      <button
        type="button"
        id={id}
        className="styled-dropdown__trigger"
        aria-label={ariaLabel}
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-controls={listId}
        disabled={disabled}
        onClick={toggle}
        onKeyDown={onTriggerKeyDown}
      >
        <span className="styled-dropdown__value">{selectedLabel}</span>
        <i
          className={`fa-solid fa-chevron-down styled-dropdown__chevron${open ? ' styled-dropdown__chevron--open' : ''}`}
          aria-hidden="true"
        />
      </button>
      {open ? (
        <ul
          id={listId}
          role="listbox"
          className="styled-dropdown__panel"
          {...(highlight >= 0 ? { 'aria-activedescendant': `${listId}-opt-${highlight}` } : {})}
        >
          {options.map((opt, i) => (
            <li
              key={String(opt.value)}
              id={`${listId}-opt-${i}`}
              role="option"
              aria-selected={opt.value === value}
              className={`styled-dropdown__option${opt.value === value ? ' styled-dropdown__option--selected' : ''}${i === highlight ? ' styled-dropdown__option--highlight' : ''}`}
              onMouseEnter={() => setHighlight(i)}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => pick(opt.value)}
            >
              <span className="styled-dropdown__option-label">{opt.label}</span>
              {opt.value === value ? (
                <i className="fa-solid fa-check styled-dropdown__check" aria-hidden="true" />
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}

export default StyledDropdown
