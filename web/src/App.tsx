import { BrowserRouter, Navigate, NavLink, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth'
import { Login } from './pages/Login'
import { ChangePassword } from './pages/ChangePassword'
import { Teams } from './pages/Teams'
import { TeamVault } from './pages/TeamVault'
import { PersonalVault } from './pages/PersonalVault'
import { AdminUsers } from './pages/AdminUsers'
import { AdminTeams } from './pages/AdminTeams'
import { AuditLogs } from './pages/AuditLogs'
import { AuditIcon, KeyIcon, LockIcon, LogoutIcon, ShieldIcon, TeamIcon, UsersIcon, VaultIcon } from './components/Icons'

function initials(email: string): string {
  const name = email.split('@')[0]
  const parts = name.split(/[._-]/).filter(Boolean)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return name.slice(0, 2).toUpperCase()
}

function Shell() {
  const { me, loading, logout } = useAuth()

  if (loading) return <div className="centered-card"><p className="loading">Yükleniyor…</p></div>
  if (!me) return <Login />
  if (me.mustChangePassword) return <ChangePassword forced />

  return (
    <BrowserRouter>
      <div className="shell">
        <aside className="sidebar">
          <NavLink to="/" className="brand">
            <ShieldIcon size={22} />
            <span className="brand-text">BrassVault</span>
          </NavLink>
          <nav>
            <div className="nav-section">Kasa</div>
            <NavLink to="/" end className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
              <VaultIcon />
              <span>Ekiplerim</span>
            </NavLink>
            <NavLink to="/vault" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
              <LockIcon />
              <span>Kişisel Kasam</span>
            </NavLink>
            {me.role === 'ADMIN' && (
              <>
                <div className="nav-section">Yönetim</div>
                <NavLink to="/admin/users" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
                  <UsersIcon />
                  <span>Kullanıcılar</span>
                </NavLink>
                <NavLink to="/admin/teams" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
                  <TeamIcon />
                  <span>Ekipler</span>
                </NavLink>
                <NavLink to="/admin/audit" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
                  <AuditIcon />
                  <span>Denetim</span>
                </NavLink>
              </>
            )}
            <div className="nav-section">Hesap</div>
            <NavLink to="/password" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
              <KeyIcon />
              <span>Şifre değiştir</span>
            </NavLink>
          </nav>
          <div className="sidebar-footer">
            <div className="user-chip">
              <span className="avatar">{initials(me.email)}</span>
              <span className="who">
                <span className="email">{me.email}</span>
                <span className="role">{me.role === 'ADMIN' ? 'Yönetici' : 'Üye'}</span>
              </span>
            </div>
            <button type="button" className="ghost sm" onClick={() => void logout()}
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', justifyContent: 'center' }}>
              <LogoutIcon size={14} />
              <span className="logout-text">Çıkış</span>
            </button>
          </div>
        </aside>
        <main className="content">
          <Routes>
            <Route path="/" element={<Teams />} />
            <Route path="/teams/:teamId" element={<TeamVault />} />
            <Route path="/vault" element={<PersonalVault />} />
            <Route path="/password" element={<ChangePassword />} />
            {me.role === 'ADMIN' && (
              <>
                <Route path="/admin/users" element={<AdminUsers />} />
                <Route path="/admin/teams" element={<AdminTeams />} />
                <Route path="/admin/audit" element={<AuditLogs />} />
              </>
            )}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  )
}
