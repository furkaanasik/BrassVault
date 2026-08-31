import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import type { Team } from '../types'
import { VaultIcon } from '../components/Icons'

export function Teams() {
  const [teams, setTeams] = useState<Team[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.get<Team[]>('/api/teams').then(setTeams).catch((e) => setError(e.message))
  }, [])

  if (error) return <p className="alert error">{error}</p>
  if (!teams) return <p className="loading">Yükleniyor…</p>

  return (
    <section>
      <div className="page-head">
        <h2>Ekiplerim</h2>
        <p className="sub">Üyesi olduğun ekiplerin kasalarına buradan erişirsin.</p>
      </div>
      {teams.length === 0 ? (
        <div className="empty-state">
          <VaultIcon size={36} />
          <p>Henüz bir ekibe eklenmedin.</p>
          <p className="hint">Bir kasaya erişmek için yöneticinle iletişime geç.</p>
        </div>
      ) : (
        <div className="grid">
          {teams.map((t, i) => (
            <Link
              key={t.id}
              className="card team-card"
              to={`/teams/${t.id}`}
              style={{ '--i': i } as React.CSSProperties}
            >
              <h3>{t.name}</h3>
              <p className="muted">{t.description ?? ''}</p>
              <span className="card-cta">Kasayı aç →</span>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}
