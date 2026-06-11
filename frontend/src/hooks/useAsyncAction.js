import { useCallback, useState } from 'react'
import { getErrorMessage } from '../utils/errors'
import { useNotification } from '../context/NotificationContext'

export function useAsyncAction() {
  const notify = useNotification()
  const [submitting, setSubmitting] = useState(false)

  const run = useCallback(async (action, options = {}) => {
    const { successMessage, onSuccess, silent = false } = options
    setSubmitting(true)
    try {
      const result = await action()
      if (successMessage) notify.success(successMessage)
      onSuccess?.(result)
      return { ok: true, data: result }
    } catch (error) {
      const message = getErrorMessage(error)
      if (!silent) notify.error(message)
      return { ok: false, error: message }
    } finally {
      setSubmitting(false)
    }
  }, [notify])

  return { run, submitting }
}
