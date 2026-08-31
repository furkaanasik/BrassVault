import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import type { AuditLog, PageOf } from '../types'

const ACTIONS = ['', 'LOGIN', 'VIEW_SECRET', 'CREATE_ITEM', 'UPDATE_ITEM', 'DELETE_ITEM', 'ADD_MEMBER', 'REMOVE_MEMBER']

export function AuditLogs() {
  const [page, setPage] = useState(0)
  const [action, setAction] = useState('')
  const [userEmail, setUserEmail] = useState('')
  const [data, setData] = useState<PageOf<AuditLog> | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    const params = new URLSearchParams({ page: String(page), size: '20' })
    if (action) params.set('action', action)
    if (userEmail) params.set('userEmail', userEmail)
    api.get<PageOf<AuditLog>>(`/api/admin/audit-logs?${params}`)
      .then(setData)
      .catch((e) => setError(e.message))
  }, [page, action, userEmail])

  useEffect(() => {
    load()
  }, [load])

  return (
    <section>
      <div className="page-head">
        <h2>Denetim kayıtları</h2>
        <p className="sub">Kim, ne zaman, hangi kayda erişti — tamamı burada.</p>
      </div>
      {error && <p className="alert error">{error}</p>}
      <div className="row filters">
        <select value={action} onChange={(e) => { setAction(e.target.value); setPage(0) }}>
          {ACTIONS.map((a) => <option key={a} value={a}>{a || 'Tüm işlemler'}</option>)}
        </select>
        <input placeholder="E-posta filtrele" value={userEmail}
               onChange={(e) => { setUserEmail(e.target.value); setPage(0) }} />
      </div>
      {data && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Zaman</th><th>Kullanıcı</th><th>İşlem</th><th>Kayıt</th><th>Ekip</th><th>IP</th></tr>
              </thead>
              <tbody>
                {data.content.map((a, i) => (
                  <tr key={a.id} style={{ '--i': i } as React.CSSProperties}>
                    <td className="muted">{new Date(a.createdAt).toLocaleString('tr-TR')}</td>
                    <td>{a.userEmail}</td>
                    <td><span className={`badge badge-${a.action.toLowerCase()}`}>{a.action}</span></td>
                    <td>{a.itemTitle}</td>
                    <td>{a.teamName ?? (a.itemTitle != null ? <span className="muted">Kişisel</span> : null)}</td>
                    <td className="muted">{a.ipAddress}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="row pagination">
            <button type="button" className="ghost sm" disabled={page === 0} onClick={() => setPage(page - 1)}>← Önceki</button>
            <span className="muted">Sayfa {data.number + 1} / {Math.max(1, data.totalPages)}</span>
            <button type="button" className="ghost sm" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Sonraki →</button>
          </div>
        </>
      )}
    </section>
  )
}
