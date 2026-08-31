import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CopyButton } from '../components/CopyButton'

describe('CopyButton', () => {
  const writeText = vi.fn()

  beforeEach(() => {
    vi.useFakeTimers()
    writeText.mockReset().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('copies the text and shows feedback, then resets', async () => {
    render(<CopyButton getText={() => 'user1'} />)
    await act(async () => {
      fireEvent.click(screen.getByRole('button'))
    })
    expect(writeText).toHaveBeenCalledWith('user1')
    expect(screen.getByRole('button').getAttribute('aria-label')).toBe('Kopyalandı ✓')

    act(() => {
      vi.advanceTimersByTime(1500)
    })
    expect(screen.getByRole('button').getAttribute('aria-label')).toBe('Kopyala')
  })

  it('copies async text (secret fetch) without rendering it anywhere', async () => {
    render(<CopyButton label="Göstermeden kopyala" getText={async () => 's3cret'} />)
    await act(async () => {
      fireEvent.click(screen.getByRole('button'))
    })
    expect(writeText).toHaveBeenCalledWith('s3cret')
    expect(document.body.textContent).not.toContain('s3cret')
  })

  it('shows a failure state when getText rejects', async () => {
    render(<CopyButton getText={async () => { throw new Error('Item not found') }} />)
    await act(async () => {
      fireEvent.click(screen.getByRole('button'))
    })
    expect(writeText).not.toHaveBeenCalled()
    expect(screen.getByRole('button').getAttribute('aria-label')).toBe('Kopyalanamadı')
  })
})
