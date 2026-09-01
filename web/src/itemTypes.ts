import type { ItemType } from './types'

export interface ItemTypeMeta {
  label: string
  secretLabel: string
  showUsername: boolean
  showUrl: boolean
  suggestedFields: string[]
}

export const ITEM_TYPES: Record<ItemType, ItemTypeMeta> = {
  LOGIN:         { label: 'Giriş',        secretLabel: 'Şifre',        showUsername: true,  showUrl: true,  suggestedFields: [] },
  API_KEY:       { label: 'API Anahtarı', secretLabel: 'API Anahtarı', showUsername: false, showUrl: true,  suggestedFields: ['key_id'] },
  SSH_KEY:       { label: 'SSH Anahtarı', secretLabel: 'Private Key',  showUsername: true,  showUrl: false, suggestedFields: ['public_key', 'passphrase'] },
  SECURE_NOTE:   { label: 'Güvenli Not',  secretLabel: 'Not içeriği',  showUsername: false, showUrl: false, suggestedFields: [] },
  DB_CONNECTION: { label: 'Veritabanı',   secretLabel: 'Şifre',        showUsername: true,  showUrl: false, suggestedFields: ['host', 'port', 'database'] },
}
