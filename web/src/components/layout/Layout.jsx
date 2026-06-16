import Sidebar from './Sidebar'

export default function Layout({ children }) {
  return (
    <div className="flex min-h-screen" style={{ background: '#070C18' }}>
      <Sidebar />
      <main className="flex-1 ml-60 p-6 flex flex-col min-h-screen">
        <div className="flex-1">
          {children}
        </div>
        <footer className="mt-8 pt-4 text-center text-xs" style={{ borderTop: '1px solid #1a2845', color: '#2d4a7a' }}>
          Développé par MH · Étudiant à la FSM
        </footer>
      </main>
    </div>
  )
}
