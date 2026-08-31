import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import { SecretCell } from '../components/SecretCell'
import { CopyButton } from '../components/CopyButton'
import { LockIcon } from '../components/Icons'
import type { Item } from '../types'

interface ItemForm {
  title: string
  username: string
  password: string
  url: string
  notes: string
}

const emptyForm: ItemForm = { title: '', username: '', password: '', url: '', notes: '' }

export function TeamVault() {
  const { teamId } = useParams()
  const { me } = useAuth()
  const isAdmin = me?.role === 'ADMIN'
  const [items, setItems] = useState<Item[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<ItemForm>(emptyForm)
  const [editing, setEditing] = useState<Item | null>(null)
  const [showForm, setShowForm] = useState(false)

  const load = useCallback(() => {
    api.get<Item[]>(`/api/teams/${teamId}/items`).then(setItems).catch((e) => setError(e.message))
  }, [teamId])

  useEffect(() => {
    load()
  }, [load])

  const startEdit = (item: Item) => {
    setEditing(item)
    setForm({
      title: item.title,
      username: item.username ?? '',
      password: '',
      url: item.url ?? '',
      notes: item.notes ?? '',
    })
    setShowForm(true)
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    try {
      if (editing) {
        await api.put(`/api/admin/items/${editing.id}`, {
          title: form.title,
          username: form.username || null,
          password: form.password || null,
          url: form.url || null,
          notes: form.notes || null,
        })
      } else {
        await api.post('/api/admin/items', {
          teamId: Number(teamId),
          title: form.title,
          username: form.username || null,
          password: form.password,
          url: form.url || null,
          notes: form.notes || null,
        })
      }
      setForm(emptyForm)
      setEditing(null)
      setShowForm(false)
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kaydedilemedi')
    }
  }

  const remove = async (item: Item) => {
    if (!window.confirm(`"${item.title}" silinsin mi?`)) return
    try {
      await api.delete(`/api/admin/items/${item.id}`)
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Silinemedi')
    }
  }

  if (error && !items) return <p className="alert error">{error}</p>
  if (!items) return <p className="loading">Yükleniyor…</p>

  return (
    <section>
      <div className="page-head-row">
        <div className="page-head">
          <h2>Ekip kasası</h2>
          <p className="sub">Şifreler görüntülendiğinde denetim kaydına işlenir.</p>
        </div>
        {isAdmin && (
          <button
            type="button"
            className={showForm ? 'ghost' : ''}
            onClick={() => { setEditing(null); setForm(emptyForm); setShowForm(!showForm) }}
          >
            {showForm ? 'Vazgeç' : '+ Yeni kayıt'}
          </button>
        )}
      </div>
      {error && <p className="alert error">{error}</p>}
      {isAdmin && showForm && (
        <div className="expand"><div className="expand-inner">
        <form className="card panel" onSubmit={submit} aria-label="item-form">
          <h3>{editing ? `Düzenle: ${editing.title}` : 'Yeni kayıt'}</h3>
          <label>Başlık<input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></label>
          <label>Kullanıcı adı<input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></label>
          <label>
            Şifre{editing && <span className="muted"> (boş bırakılırsa değişmez)</span>}
            <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required={!editing} />
          </label>
          <label>URL<input value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} /></label>
          <label>Notlar<textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></label>
          <div className="form-actions">
            <button type="submit">Kaydet</button>
          </div>
        </form>
        </div></div>
      )}
      {items.length === 0 ? (
        <div className="empty-state">
          <LockIcon size={36} />
          <p>Bu kasada henüz kayıt yok.</p>
          {isAdmin && <p className="hint">&quot;Yeni kayıt&quot; ile ekibin ilk credential&apos;ını ekle.</p>}
        </div>
      ) : (
        <div className="table-wrap">
          <table className="table-fixed">
            <thead>
              <tr>
                <th>Başlık</th><th>Kullanıcı adı</th><th className="secret-col">Şifre</th><th>URL</th><th>Notlar</th>
                {isAdmin && <th className="actions-col"></th>}
              </tr>
            </thead>
            <tbody>
              {items.map((item, i) => (
                <tr key={item.id} style={{ '--i': i } as React.CSSProperties}>
                  <td>{item.title}</td>
                  <td>
                    {item.username && (
                      <span className="cell-copy">
                        <span>{item.username}</span>
                        <CopyButton getText={() => item.username ?? ''} />
                      </span>
                    )}
                  </td>
                  <td className="secret-col"><SecretCell itemId={item.id} /></td>
                  <td>
                    {item.url && (
                      <span className="cell-copy">
                        <a href={item.url} target="_blank" rel="noreferrer">{item.url}</a>
                        <CopyButton getText={() => item.url ?? ''} />
                      </span>
                    )}
                  </td>
                  <td className="muted wrap">{item.notes}</td>
                  {isAdmin && (
                    <td>
                      <span className="actions">
                        <button type="button" className="ghost sm" onClick={() => startEdit(item)}>Düzenle</button>
                        <button type="button" className="danger sm" onClick={() => void remove(item)}>Sil</button>
                      </span>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
