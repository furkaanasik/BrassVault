import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from '../api'

describe('api client', () => {
  afterEach(() => vi.restoreAllMocks())

  it('sends credentials with every request', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200 }),
    )
    await api.get('/api/me')
    expect(fetchMock).toHaveBeenCalledWith('/api/me', expect.objectContaining({ credentials: 'include' }))
  })

  it('throws ApiError with the server message on failure', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ error: 'Invalid credentials' }), { status: 401 }),
    )
    await expect(api.post('/api/auth/login', { email: 'x', password: 'y' })).rejects.toMatchObject({
      status: 401,
      message: 'Invalid credentials',
    })
  })

  it('throws a generic ApiError when the body is not JSON', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('boom', { status: 500 }))
    const err: unknown = await api.get('/api/teams').catch((e: unknown) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect((err as ApiError).status).toBe(500)
  })

  it('returns undefined for 204 responses', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))
    await expect(api.post('/api/auth/logout')).resolves.toBeUndefined()
  })
})
