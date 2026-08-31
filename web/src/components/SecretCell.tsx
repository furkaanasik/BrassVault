import { useEffect, useState } from 'react'
import { api } from '../api'
import { REVEAL_DURATION_MS, useSecretReveal } from '../hooks/useSecretReveal'
import { CopyButton } from './CopyButton'

const SCRAMBLE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789#$%&@!'
const SCRAMBLE_MS = 700

/* decrypt effect: characters shuffle, then lock in left to right */
function useScramble(target: string | null): string {
  const [display, setDisplay] = useState('')

  useEffect(() => {
    if (!target) {
      setDisplay('')
      return
    }
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setDisplay(target)
      return
    }
    const start = performance.now()
    let raf = 0
    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / SCRAMBLE_MS)
      const settled = Math.floor(t * target.length)
      let out = target.slice(0, settled)
      for (let i = settled; i < target.length; i++) {
        out += SCRAMBLE_CHARS[Math.floor(Math.random() * SCRAMBLE_CHARS.length)]
      }
      setDisplay(out)
      if (t < 1) raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [target])

  return display
}

export function SecretCell({ itemId }: { itemId: number }) {
  const { password, error, loading, reveal, hide } = useSecretReveal(itemId)
  const scrambled = useScramble(password)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!password) return
    setSecondsLeft(REVEAL_DURATION_MS / 1000)
    const interval = setInterval(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000)
    return () => clearInterval(interval)
  }, [password])

  const copy = async () => {
    if (!password) return
    await navigator.clipboard.writeText(password)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  if (password) {
    return (
      <span className="secret-cell">
        <span className="secret-chip">
          <code className="secret-value">{scrambled}</code>
          <span className="countdown-bar" style={{ animationDuration: `${REVEAL_DURATION_MS}ms` }} />
        </span>
        <button type="button" className={copied ? 'ghost sm pop' : 'ghost sm'} onClick={copy}>{copied ? 'Kopyalandı ✓' : 'Kopyala'}</button>
        <button type="button" className="ghost sm" onClick={hide}>Gizle</button>
        <span className="countdown">{secondsLeft}s</span>
      </span>
    )
  }
  // copy-without-reveal still hits the secret endpoint, so it is audited server-side
  const fetchSecret = async () =>
    (await api.get<{ password: string }>(`/api/items/${itemId}/secret`)).password

  return (
    <span className="secret-cell">
      <span className="secret-mask">••••••••</span>
      <button type="button" className="ghost sm" onClick={() => void reveal()} disabled={loading}>
        {loading ? '…' : 'Göster'}
      </button>
      <CopyButton label="Göstermeden kopyala" getText={fetchSecret} />
      {error && <span className="error">{error}</span>}
    </span>
  )
}
