import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api'
import type { Team, UserRow } from '../types'

export function AdminTeams() {
  const [teams, setTeams] = useState<Team[] | null>(null)
  const [users, setUsers] = useState<UserRow[]>([])
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [selected, setSelected] = useState<Team | null>(null)
  const [members, setMembers] = useState<UserRow[]>([])
  const [addUserId, setAddUserId] = useState('')

  const load = useCallback(() => {
    api.get<Team[]>('/api/admin/teams').then(setTeams).catch((e) => setError(e.message))
    api.get<UserRow[]>('/api/admin/users').then(setUsers).catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const openTeam = async (team: Team) => {
    setSelected(team)
    setError(null)
    try {
      setMembers(await api.get<UserRow[]>(`/api/admin/teams/${team.id}/members`))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Üyeler yüklenemedi')
    }
  }

  const create = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      await api.post('/api/admin/teams', { name, description: description || null })
      setName('')
      setDescription('')
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ekip oluşturulamadı')
    }
  }

  const addMember = async (e: FormEvent) => {
    e.preventDefault()
    if (!selected || !addUserId) return
    setError(null)
    try {
      await api.post(`/api/admin/teams/${selected.id}/members`, { userId: Number(addUserId) })
      setAddUserId('')
      await openTeam(selected)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Üye eklenemedi')
    }
  }

  const removeMember = async (userId: number) => {
    if (!selected) return
    setError(null)
    try {
      await api.delete(`/api/admin/teams/${selected.id}/members/${userId}`)
      await openTeam(selected)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Üye çıkarılamadı')
    }
  }

  const candidates = users.filter((u) => u.active && !members.some((m) => m.id === u.id))

  return (
    <section>
      <div className="page-head">
        <h2>Ekipler</h2>
        <p className="sub">Ekip oluştur ve üyeliklerini yönet.</p>
      </div>
      {error && <p className="alert error">{error}</p>}
      <form className="card panel" onSubmit={create} aria-label="create-team">
        <h3>Yeni ekip</h3>
        <label>Ad<input value={name} onChange={(e) => setName(e.target.value)} required /></label>
        <label>Açıklama<input value={description} onChange={(e) => setDescription(e.target.value)} /></label>
        <div className="form-actions">
          <button type="submit">Oluştur</button>
        </div>
      </form>
      {teams && (
        <div className="grid">
          {teams.map((t, i) => (
            <div
              key={t.id}
              className={`card team-card ${selected?.id === t.id ? 'selected' : ''}`}
              style={{ '--i': i } as React.CSSProperties}
            >
              <h3>{t.name}</h3>
              <p className="muted">{t.description}</p>
              <div style={{ marginTop: '0.75rem' }}>
                <button type="button" className="ghost sm" onClick={() => void openTeam(t)}>Üyeleri yönet</button>
              </div>
            </div>
          ))}
        </div>
      )}
      {selected && (
        <div className="card panel">
          <h3>{selected.name} — üyeler</h3>
          <form onSubmit={addMember} className="row" aria-label="add-member" style={{ marginBottom: '1rem' }}>
            <select value={addUserId} onChange={(e) => setAddUserId(e.target.value)} required>
              <option value="">Kullanıcı seç…</option>
              {candidates.map((u) => (
                <option key={u.id} value={u.id}>{u.fullName} — {u.email}</option>
              ))}
            </select>
            <button type="submit">Ekle</button>
          </form>
          {members.length === 0 ? (
            <p className="muted">Bu ekipte henüz üye yok. Yukarıdan ilk üyeyi ekle.</p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>Ad</th><th>E-posta</th><th></th></tr>
                </thead>
                <tbody>
                  {members.map((m, i) => (
                    <tr key={m.id} style={{ '--i': i } as React.CSSProperties}>
                      <td>{m.fullName}</td>
                      <td className="muted">{m.email}</td>
                      <td>
                        <span className="actions">
                          <button type="button" className="danger sm" onClick={() => void removeMember(m.id)}>Çıkar</button>
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </section>
  )
}
