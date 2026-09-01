import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api'
import { SecretCell } from '../components/SecretCell'
import { CopyButton } from '../components/CopyButton'
import { CustomFieldsEditor } from '../components/CustomFieldsEditor'
import type { FieldRow } from '../components/CustomFieldsEditor'
import { LockIcon } from '../components/Icons'
import { ITEM_TYPES } from '../itemTypes'
import type { Item, ItemType } from '../types'

interface ItemForm {
  title: string
  username: string
  password: string
  url: string
  notes: string
  type: ItemType
  fields: FieldRow[]
}

const emptyForm: ItemForm = { title: '', username: '', password: '', url: '', notes: '', type: 'LOGIN', fields: [] }

function toCustomFields(rows: FieldRow[]): Record<string, string> {
  return Object.fromEntries(rows.filter((f) => f.key.trim()).map((f) => [f.key.trim(), f.value]))
}

export function PersonalVault() {
  const [items, setItems] = useState<Item[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState<ItemForm>(emptyForm)
  const [editing, setEditing] = useState<Item | null>(null)
  const [editFields, setEditFields] = useState(false)
  const [showForm, setShowForm] = useState(false)

  const load = useCallback(() => {
    api.get<Item[]>('/api/vault/items').then(setItems).catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const startEdit = (item: Item) => {
    setEditing(item)
    setEditFields(false)
    setForm({
      title: item.title,
      username: item.username ?? '',
      password: '',
      url: item.url ?? '',
      notes: item.notes ?? '',
      type: item.type,
      fields: [],
    })
    setShowForm(true)
  }

  const changeType = (type: ItemType) => {
    setForm((f) => ({
      ...f,
      type,
      // seed suggested keys only for a fresh create form the user hasn't touched
      fields: !editing && f.fields.every((r) => !r.key && !r.value)
        ? ITEM_TYPES[type].suggestedFields.map((key) => ({ key, value: '' }))
        : f.fields,
    }))
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
          type: form.type,
          customFields: editFields ? toCustomFields(form.fields) : null,
        })
      } else {
        await api.post('/api/vault/items', {
          title: form.title,
          username: form.username || null,
          password: form.password,
          url: form.url || null,
          notes: form.notes || null,
          type: form.type,
          customFields: toCustomFields(form.fields),
        })
      }
      setForm(emptyForm)
      setEditing(null)
      setEditFields(false)
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

  const meta = ITEM_TYPES[form.type]

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
          onClick={() => { setEditing(null); setEditFields(false); setForm(emptyForm); setShowForm(!showForm) }}
        >
          {showForm ? 'Vazgeç' : '+ Yeni kayıt'}
        </button>
      </div>
      {error && <p className="alert error">{error}</p>}
      {showForm && (
        <div className="expand"><div className="expand-inner">
        <form className="card panel" onSubmit={submit} aria-label="item-form">
          <h3>{editing ? `Düzenle: ${editing.title}` : 'Yeni kayıt'}</h3>
          <label>Tip
            <select value={form.type} onChange={(e) => changeType(e.target.value as ItemType)}>
              {(Object.keys(ITEM_TYPES) as ItemType[]).map((t) => (
                <option key={t} value={t}>{ITEM_TYPES[t].label}</option>
              ))}
            </select>
          </label>
          <label>Başlık<input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></label>
          {meta.showUsername && (
            <label>Kullanıcı adı<input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></label>
          )}
          <label>
            {meta.secretLabel}{editing && <span className="muted"> (boş bırakılırsa değişmez)</span>}
            <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required={!editing} />
          </label>
          {meta.showUrl && (
            <label>URL<input value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} /></label>
          )}
          <label>Notlar<textarea value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></label>
          {editing ? (
            <label className="checkbox-row">
              <input type="checkbox" checked={editFields} onChange={(e) => setEditFields(e.target.checked)} />
              Özel alanları değiştir (mevcutlar korunur)
            </label>
          ) : (
            <span className="muted">Özel alanlar</span>
          )}
          {(!editing || editFields) && (
            <CustomFieldsEditor rows={form.fields} onChange={(fields) => setForm({ ...form, fields })} />
          )}
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
                <th style={{ width: 110 }}>Tip</th>
                <th>Başlık</th><th>Kullanıcı adı</th><th className="secret-col">Şifre</th><th>URL</th><th>Notlar</th>
                <th className="actions-col"></th>
              </tr>
            </thead>
            <tbody>
              {items.map((item, i) => (
                <tr key={item.id} style={{ '--i': i } as React.CSSProperties}>
                  <td><span className="badge">{ITEM_TYPES[item.type].label}</span></td>
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
