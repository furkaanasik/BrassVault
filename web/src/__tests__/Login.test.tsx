import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'

function mockFetchRoutes(routes: Record<string, () => Response>) {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
    const key = Object.keys(routes).find((k) => url.startsWith(k))
    if (!key) return new Response(JSON.stringify({ error: 'not mocked: ' + url }), { status: 500 })
    return routes[key]()
  })
}

describe('login flow', () => {
  afterEach(() => vi.restoreAllMocks())

  it('shows the login form when unauthenticated and an error on bad credentials', async () => {
    mockFetchRoutes({
      '/api/me': () => new Response(JSON.stringify({ error: 'Authentication required' }), { status: 401 }),
      '/api/auth/login': () => new Response(JSON.stringify({ error: 'Invalid credentials' }), { status: 401 }),
    })
    render(<App />)

    const email = await screen.findByLabelText(/E-posta/)
    await userEvent.type(email, 'yusuf@brassvault.local')
    await userEvent.type(screen.getByLabelText(/^Şifre$/), 'wrong-password')
    await userEvent.click(screen.getByRole('button', { name: /Giriş yap/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid credentials')
  })

  it('forces the password change screen for a fresh user', async () => {
    mockFetchRoutes({
      '/api/me': () => new Response(JSON.stringify({ error: 'Password change required' }), { status: 403 }),
    })
    render(<App />)
    expect(await screen.findByText(/geçici şifreni değiştirmelisin/)).toBeInTheDocument()
  })
})
