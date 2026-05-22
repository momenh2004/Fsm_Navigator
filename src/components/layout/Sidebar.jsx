import { NavLink } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import logo from '../../assets/logo.svg'
import { dashboardIcon, wifiIcon, usersIcon, blocIcon, decIcon } from '../../assets/icons'

const nav = [
  { to: '/',         icon: dashboardIcon, label: 'Tableau de bord' },
  { to: '/coverage', icon: wifiIcon,      label: 'Couverture' },
  { to: '/users',    icon: usersIcon,     label: 'Utilisateurs'   },
  { to: '/blocs',    icon: blocIcon,      label: 'Blocs & Salles' },
]

export default function Sidebar() {
  const { admin, logout } = useAuth()

  return (
    <aside className="w-60 flex flex-col h-screen fixed left-0 top-0 z-30"
      style={{ background: '#060A14', borderRight: '1px solid #1a2845' }}>

      {/* Logo */}
      <div className="p-5" style={{ borderBottom: '1px solid #1a2845' }}>
        <div className="flex items-center gap-3">
          <img src={logo} alt="FSM Navigator" className="w-9 h-9 rounded-lg" />
          <div>
            <p className="font-bold text-white text-sm">FSM Navigator</p>
            <p className="text-xs" style={{ color: '#3b6cbd' }}>Admin Dashboard</p>
          </div>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 p-3 space-y-1">
        {nav.map(({ to, icon, label }) => (
          <NavLink key={to} to={to} end={to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors ${
                isActive
                  ? 'bg-blue-600/20 text-blue-400 font-medium'
                  : 'text-blue-200/50 hover:text-blue-200'
              }`
            }
            style={({ isActive }) => isActive ? {} : {}}
            onMouseEnter={e => { if (!e.currentTarget.classList.contains('text-blue-400')) e.currentTarget.style.background = '#0D1838' }}
            onMouseLeave={e => { if (!e.currentTarget.classList.contains('text-blue-400')) e.currentTarget.style.background = '' }}
          >
            <img src={icon} alt="" className="w-5 h-5 object-contain icon"/>
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Admin info */}
      <div className="p-4" style={{ borderTop: '1px solid #1a2845' }}>
        <div className="flex items-center gap-3 mb-3">
          <div className="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold text-blue-300"
            style={{ background: '#162040' }}>
            {admin?.email?.[0]?.toUpperCase() || 'A'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-white text-xs font-medium truncate">{admin?.email || 'Admin'}</p>
            <p className="text-xs" style={{ color: '#3b6cbd' }}>Administrateur</p>
          </div>
        </div>
        <button onClick={logout}
          className="w-full text-left text-xs flex items-center gap-2 px-2 py-1.5 rounded transition-colors"
          style={{ color: '#3b6cbd' }}
          onMouseEnter={e => { e.currentTarget.style.color = '#ef4444'; e.currentTarget.style.background = '#1a0a0a' }}
          onMouseLeave={e => { e.currentTarget.style.color = '#3b6cbd'; e.currentTarget.style.background = '' }}>
          <img src={decIcon} alt="" className="w-4 h-4 object-contain icon"/> Déconnexion
        </button>
      </div>
    </aside>
  )
}
