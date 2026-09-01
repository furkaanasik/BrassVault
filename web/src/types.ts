export type Role = 'ADMIN' | 'USER'

export interface Team {
  id: number
  name: string
  description: string | null
}

export interface Me {
  id: number
  email: string
  fullName: string | null
  role: Role
  mustChangePassword: boolean
  teams: Team[]
}

export interface UserRow {
  id: number
  email: string
  fullName: string
  role: Role
  active: boolean
  mustChangePassword: boolean
}

export interface CreatedUser extends Omit<UserRow, 'active' | 'mustChangePassword'> {
  tempPassword: string
}

export type ItemType = 'LOGIN' | 'API_KEY' | 'SSH_KEY' | 'SECURE_NOTE' | 'DB_CONNECTION'

export interface Item {
  id: number
  teamId: number | null
  title: string
  username: string | null
  url: string | null
  notes: string | null
  type: ItemType
  createdAt: string
  updatedAt: string
}

export interface SecretPayload {
  password: string
  fields: Record<string, string>
}

export interface AuditLog {
  id: number
  userEmail: string
  itemTitle: string | null
  teamName: string | null
  action: string
  ipAddress: string | null
  createdAt: string
}

export interface PageOf<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
}
