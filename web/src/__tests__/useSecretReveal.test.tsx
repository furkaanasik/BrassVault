import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { REVEAL_DURATION_MS, useSecretReveal } from '../hooks/useSecretReveal'

describe('useSecretReveal', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ password: 's3cr3t' }), { status: 200 }),
    )
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  it('reveals the secret and hides it again after 30 seconds', async () => {
    const { result } = renderHook(() => useSecretReveal(42))
    expect(result.current.password).toBeNull()

    await act(async () => {
      await result.current.reveal()
    })
    expect(result.current.password).toBe('s3cr3t')

    act(() => {
      vi.advanceTimersByTime(REVEAL_DURATION_MS - 1000)
    })
    expect(result.current.password).toBe('s3cr3t')

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(result.current.password).toBeNull()
  })

  it('hide() wipes the secret immediately', async () => {
    const { result } = renderHook(() => useSecretReveal(42))
    await act(async () => {
      await result.current.reveal()
    })
    expect(result.current.password).toBe('s3cr3t')
    act(() => {
      result.current.hide()
    })
    expect(result.current.password).toBeNull()
  })

  it('surfaces API errors without leaking a password', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ error: 'Item not found' }), { status: 404 }),
    )
    const { result } = renderHook(() => useSecretReveal(999))
    await act(async () => {
      await result.current.reveal()
    })
    expect(result.current.password).toBeNull()
    expect(result.current.error).toBe('Item not found')
  })
})
