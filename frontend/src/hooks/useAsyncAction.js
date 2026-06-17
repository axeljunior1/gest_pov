import { useCallback, useRef, useState } from 'react'
import { getErrorDetails } from '../utils/errors'
import { useNotification } from '../context/NotificationContext'

export function useAsyncAction() {
  const notify = useNotification()
  const [submitting, setSubmitting] = useState(false)
  const inFlightRef = useRef(false)

  const run = useCallback(async (action, options = {}) => {
    const { successMessage, onSuccess, silent = false, errorContext } = options
    if (inFlightRef.current) {
      return { ok: false, error: 'Action déjà en cours.' }
    }
    inFlightRef.current = true
    setSubmitting(true)
    try {
      const result = await action()
      if (successMessage) notify.success(successMessage)
      onSuccess?.(result)
      return { ok: true, data: result }
    } catch (error) {
      const details = getErrorDetails(error, errorContext)
      if (!silent) notify.error(details.message)
      return { ok: false, error: details.message, fieldErrors: details.fieldErrors, details }
    } finally {
      inFlightRef.current = false
      setSubmitting(false)
    }
  }, [notify])

  return { run, submitting }
}
