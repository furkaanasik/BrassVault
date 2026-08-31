import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api'
import type { CreatedUser, Role, UserRow } from '../types'

export function AdminUsers() {
  const [users, setUsers] = useState<UserRow[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [email, setEmail] = useState('')
  const [fullName, setFullName] = useState('')
  const [role, setRole] = useState<Role>('USER')
  const [created, setCreated] = useState<CreatedUser | null>(null)

  const load = useCallback(() => {
    api.get<UserRow[]>('/api/admin/users').then(setUsers).catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const create = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      const user = await api.post<CreatedUser>('/api/admin/users', { email, fullName, role })
      setCreated(user)
      setEmail('')
      setFullName('')
      setRole('USER')
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kullanıcı oluşturulamadı')
    }
  }

  const patch = async (id: number, body: { role?: Role; active?: boolean }) => {
    setError(null)
    try {
      await api.patch(`/api/admin/users/${id}`, body)
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Güncellenemedi')
    }
  }

  return (
    <section>
      <div className="page-head">
        <h2>Kullanıcılar</h2>
        <p className="sub">Hesap oluştur, rol ata, erişimi yönet.</p>
      </div>
      {error && <p className="alert error">{error}</p>}
      <form className="card panel" onSubmit={create} aria-label="create-user">
        <h3>Yeni kullanıcı</h3>
        <label>E-posta<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
        <label>Ad Soyad<input value={fullName} onChange={(e) => setFullName(e.target.value)} required /></label>
        <label>
          Rol
          <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </label>
        <div className="form-actions">
          <button type="submit">Oluştur</button>
        </div>
      </form>
      {created && (
        <div className="card temp-password" role="alert">
          <strong>{created.email}</strong> oluşturuldu. Geçici şifre — sadece bir kez gösterilir,
          güvenli bir kanaldan ilet:
          <code className="secret-value">{created.tempPassword}</code>
          <span className="row" style={{ display: 'inline-flex' }}>
            <button type="button" className="sm" onClick={() => { void navigator.clipboard.writeText(created.tempPassword) }}>Kopyala</button>
            <button type="button" className="ghost sm" onClick={() => setCreated(null)}>Kapat</button>
          </span>
        </div>
      )}
      {users && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>E-posta</th><th>Ad</th><th>Rol</th><th>Durum</th><th></th></tr>
            </thead>
            <tbody>
              {users.map((u, i) => (
                <tr key={u.id} className={u.active ? '' : 'inactive'} style={{ '--i': i } as React.CSSProperties}>
                  <td>{u.email}</td>
                  <td>{u.fullName}</td>
                  <td>
                    <select value={u.role} onChange={(e) => void patch(u.id, { role: e.target.value as Role })}>
                      <option value="USER">USER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </td>
                  <td>
                    <span className="badge">{u.active ? 'Aktif' : 'Deaktif'}</span>
                    {u.mustChangePassword && <span className="badge badge-login" style={{ marginLeft: '0.35rem' }}>şifre bekliyor</span>}
                  </td>
                  <td>
                    <span className="actions">
                      <button type="button" className={u.active ? 'danger sm' : 'ghost sm'}
                              onClick={() => void patch(u.id, { active: !u.active })}>
                        {u.active ? 'Deaktive et' : 'Aktive et'}
                      </button>
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
