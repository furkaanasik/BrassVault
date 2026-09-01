export interface FieldRow {
  key: string
  value: string
}

export function CustomFieldsEditor({ rows, onChange }: {
  rows: FieldRow[]
  onChange: (rows: FieldRow[]) => void
}) {
  const setRow = (i: number, patch: Partial<FieldRow>) =>
    onChange(rows.map((r, idx) => (idx === i ? { ...r, ...patch } : r)))

  return (
    <div className="custom-fields">
      {rows.map((row, i) => (
        <div key={i} className="custom-field-row">
          <input
            placeholder="Alan adı"
            value={row.key}
            maxLength={64}
            onChange={(e) => setRow(i, { key: e.target.value })}
          />
          <input
            type="password"
            placeholder="Değer"
            value={row.value}
            onChange={(e) => setRow(i, { value: e.target.value })}
          />
          <button
            type="button"
            className="ghost sm"
            aria-label="Alanı sil"
            onClick={() => onChange(rows.filter((_, idx) => idx !== i))}
          >
            ×
          </button>
        </div>
      ))}
      <button
        type="button"
        className="ghost sm"
        onClick={() => onChange([...rows, { key: '', value: '' }])}
      >
        + Alan ekle
      </button>
    </div>
  )
}
