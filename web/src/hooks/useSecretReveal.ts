import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api'

export const REVEAL_DURATION_MS = 30_000

interface RevealState {
  password: string | null
  error: string | null
  loading: boolean
}

/**
 * Fetches a secret via /api/items/{id}/secret and keeps it visible for
 * 30 seconds, then wipes it from state again.
 */
export function useSecretReveal(itemId: number) {
  const [state, setState] = useState<RevealState>({ password: null, error: null, loading: false })
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const hide = useCallback(() => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
    setState({ password: null, error: null, loading: false })
  }, [])

  const reveal = useCallback(async () => {
    setState((s) => ({ ...s, loading: true, error: null }))
    try {
      const { password } = await api.get<{ password: string }>(`/api/items/${itemId}/secret`)
      setState({ password, error: null, loading: false })
      if (timer.current) clearTimeout(timer.current)
      timer.current = setTimeout(hide, REVEAL_DURATION_MS)
    } catch (e) {
      setState({ password: null, error: e instanceof Error ? e.message : 'Failed', loading: false })
    }
  }, [itemId, hide])

  useEffect(() => hide, [hide])

  return { ...state, reveal, hide }
}
