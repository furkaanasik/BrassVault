import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api'
import { useAuth } from '../auth'

export function ChangePassword({ forced }: { forced?: boolean }) {
  const { refresh } = useAuth()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (next !== confirm) {
      setError('Yeni şifreler eşleşmiyor')
      return
    }
    if (next.length < 12) {
      setError('Yeni şifre en az 12 karakter olmalı')
      return
    }
    try {
      await api.post('/api/me/password', { currentPassword: current, newPassword: next })
      setDone(true)
      await refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Şifre değiştirilemedi')
    }
  }

  const form = (
    <form className="card panel" onSubmit={submit} aria-label="change-password" style={{ maxWidth: 420 }}>
      <h2>Şifre değiştir</h2>
      {forced && <p className="muted" style={{ marginTop: 0 }}>Devam etmeden önce geçici şifreni değiştirmelisin.</p>}
      <label>
        Mevcut şifre
        <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} required />
      </label>
      <label>
        Yeni şifre (en az 12 karakter)
        <input type="password" value={next} onChange={(e) => setNext(e.target.value)} required minLength={12} />
      </label>
      <label>
        Yeni şifre (tekrar)
        <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
      </label>
      {error && <p className="alert error" role="alert">{error}</p>}
      {done && <p className="alert success">Şifre güncellendi.</p>}
      <div className="form-actions">
        <button type="submit">Kaydet</button>
      </div>
    </form>
  )

  if (forced) return <div className="centered-card">{form}</div>

  return (
    <section>
      <div className="page-head">
        <h2>Hesap</h2>
        <p className="sub">Giriş şifreni buradan güncellersin.</p>
      </div>
      {form}
    </section>
  )
}
