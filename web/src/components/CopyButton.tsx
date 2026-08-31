import { useEffect, useRef, useState } from 'react'
import { CheckIcon, CopyIcon } from './Icons'

type CopyState = 'idle' | 'busy' | 'copied' | 'failed'

const FEEDBACK_MS = 1500

/**
 * Icon-only copy button. `getText` may be async (e.g. fetching a secret
 * server-side) — nothing is shown on screen, only put on the clipboard.
 */
export function CopyButton({ getText, label = 'Kopyala' }: {
  getText: () => string | Promise<string>
  label?: string
}) {
  const [state, setState] = useState<CopyState>('idle')
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => () => {
    if (timer.current) clearTimeout(timer.current)
  }, [])

  const copy = async () => {
    if (state === 'busy') return
    setState('busy')
    try {
      await navigator.clipboard.writeText(await getText())
      setState('copied')
    } catch {
      setState('failed')
    }
    if (timer.current) clearTimeout(timer.current)
    timer.current = setTimeout(() => setState('idle'), FEEDBACK_MS)
  }

  const title = state === 'copied' ? 'Kopyalandı ✓' : state === 'failed' ? 'Kopyalanamadı' : label
  return (
    <button
      type="button"
      className={state === 'copied' ? 'icon-btn pop' : state === 'failed' ? 'icon-btn failed' : 'icon-btn'}
      title={title}
      aria-label={title}
      onClick={() => void copy()}
      disabled={state === 'busy'}
    >
      {state === 'copied' ? <CheckIcon size={14} /> : <CopyIcon size={14} />}
    </button>
  )
}
