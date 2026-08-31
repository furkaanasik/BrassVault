import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api'
import { SecretCell } from '../components/SecretCell'
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

export function PersonalVault() {
  const [items, setItems] = useState<Item[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<ItemForm>(emptyForm)
  const [editing, setEditing] = useState<Item | null>(null)
  const [showForm, setShowForm] = useState(false)

  const load = useCallback(() => {
    api.get<Item[]>('/api/vault/items').then(setItems).catch((e) => setError(e.message))
  }, [])

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
        await api.put(`/api/vault/items/${editing.id}`, {
          title: form.title,
          username: form.username || null,
          password: form.password || null,
          url: form.url || null,
          notes: form.notes || null,
        })
      } else {
        await api.post('/api/vault/items', {
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
      await api.delete(`/api/vault/items/${item.id}`)
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
          <h2>Kişisel kasam</h2>
          <p className="sub">Sadece sen görebilirsin. Şifre görüntülemeleri denetim kaydına işlenir.</p>
        </div>
        <button
          type="button"
          className={showForm ? 'ghost' : ''}
          onClick={() => { setEditing(null); setForm(emptyForm); setShowForm(!showForm) }}
        >
          {showForm ? 'Vazgeç' : '+ Yeni kayıt'}
        </button>
      </div>
      {error && <p className="alert error">{error}</p>}
      {showForm && (
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
          <p>Kişisel kasanda henüz kayıt yok.</p>
          <p className="hint">&quot;Yeni kayıt&quot; ile ilk credential&apos;ını ekle.</p>
        </div>
      ) : (
        <div className="table-wrap">
          <table className="table-fixed">
            <thead>
              <tr>
                <th>Başlık</th><th>Kullanıcı adı</th><th className="secret-col">Şifre</th><th>URL</th><th>Notlar</th>
                <th className="actions-col"></th>
              </tr>
            </thead>
            <tbody>
              {items.map((item, i) => (
                <tr key={item.id} style={{ '--i': i } as React.CSSProperties}>
                  <td>{item.title}</td>
                  <td>{item.username}</td>
                  <td className="secret-col"><SecretCell itemId={item.id} /></td>
                  <td>{item.url && <a href={item.url} target="_blank" rel="noreferrer">{item.url}</a>}</td>
                  <td className="muted wrap">{item.notes}</td>
                  <td>
                    <span className="actions">
                      <button type="button" className="ghost sm" onClick={() => startEdit(item)}>Düzenle</button>
                      <button type="button" className="danger sm" onClick={() => void remove(item)}>Sil</button>
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
