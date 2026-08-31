import { lazy, Suspense, useState } from 'react'
import type { FormEvent } from 'react'
import { useAuth } from '../auth'
import { useMouseTilt } from '../hooks/useMouseTilt'
import { AuditIcon, LockIcon, ShieldIcon } from '../components/Icons'

// three.js only loads on this route
const VaultDial3D = lazy(() => import('../components/VaultDial3D'))

export function Login() {
  const { login } = useAuth()
  const sceneRef = useMouseTilt<HTMLDivElement>()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [shaking, setShaking] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(email, password)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Giriş başarısız')
      setShaking(true)
      setTimeout(() => setShaking(false), 400)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-scene" ref={sceneRef}>
      <div className="dial" aria-hidden="true">
        <Suspense fallback={null}>
          <VaultDial3D />
        </Suspense>
      </div>
      <div className="login-hero">
        <span className="brand">
          <ShieldIcon size={26} />
          BrassVault
        </span>
        <div className="login-hero-copy">
          <h1>
            Ekibinin şifreleri, <em>tek kasada</em>.
          </h1>
          <p>
            Ortak credential&apos;lar uçtan uca şifreli saklanır, ekipçe paylaşılır.
            Her görüntüleme denetim kaydına işlenir.
          </p>
        </div>
        <div className="login-hero-foot">
          <span><LockIcon size={14} />AES-256-GCM şifreleme</span>
          <span><AuditIcon size={14} />Tam denetim izi</span>
        </div>
      </div>
      <div className="login-form-side">
        <form className={`card panel${shaking ? ' shake' : ''}`} onSubmit={submit} aria-label="login">
          <h2>Giriş yap</h2>
          <p className="sub">Kurumsal hesabınla devam et.</p>
          <label>
            E-posta
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </label>
          <label>
            Şifre
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {error && <p className="alert error" role="alert">{error}</p>}
          <button type="submit" disabled={busy}>{busy ? 'Giriş yapılıyor…' : 'Giriş yap'}</button>
        </form>
      </div>
    </div>
  )
}
