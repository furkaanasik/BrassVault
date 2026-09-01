import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api'
import type { SecretPayload } from '../types'

export const REVEAL_DURATION_MS = 30_000

interface RevealState {
  password: string | null
  fields: Record<string, string> | null
  error: string | null
  loading: boolean
}

/**
 * Fetches a secret via /api/items/{id}/secret and keeps it visible for
 * 30 seconds, then wipes it from state again.
 */
export function useSecretReveal(itemId: number) {
  const [state, setState] = useState<RevealState>({ password: null, fields: null, error: null, loading: false })
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const hide = useCallback(() => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
    setState({ password: null, fields: null, error: null, loading: false })
  }, [])

  const reveal = useCallback(async () => {
    setState((s) => ({ ...s, loading: true, error: null }))
    try {
      const { password, fields } = await api.get<SecretPayload>(`/api/items/${itemId}/secret`)
      setState({ password, fields, error: null, loading: false })
      if (timer.current) clearTimeout(timer.current)
      timer.current = setTimeout(hide, REVEAL_DURATION_MS)
    } catch (e) {
      setState({ password: null, fields: null, error: e instanceof Error ? e.message : 'Failed', loading: false })
    }
  }, [itemId, hide])

  useEffect(() => hide, [hide])

  return { ...state, reveal, hide }
}
