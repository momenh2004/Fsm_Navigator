import { useState, Component } from 'react'
import Layout from '../components/layout/Layout'
import { useFetch } from '../hooks/useFetch'
import { usersAPI } from '../services/api'
import { useToast } from '../components/ui/Toast'
import Spinner from '../components/ui/Spinner'

class ErrorBoundary extends Component {
  state = { error: null }
  static getDerivedStateFromError(e) { return { error: e } }
  render() {
    if (this.state.error) return (
      <div className="p-8 text-red-400 text-sm space-y-2">
        <p className="font-bold">Erreur dans la page Utilisateurs :</p>
        <pre className="p-3 rounded text-xs overflow-auto whitespace-pre-wrap"
          style={{ background: '#0D1530' }}>
          {this.state.error.message}
          {'\n\n'}
          {this.state.error.stack}
        </pre>
      </div>
    )
    return this.props.children
  }
}

function exportCSV(rows, filename) {
  if (!rows?.length) return
  const keys = Object.keys(rows[0])
  const csv = [
    keys.join(','),
    ...rows.map(r => keys.map(k => JSON.stringify(r[k] ?? '')).join(','))
  ].join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename; a.click()
  URL.revokeObjectURL(url)
}

const EMPTY_FORM = { email: '', password: '', role: 'MEMBRE', nom: '', prenom: '' }

export default function Users() {
  const { data: users, loading, error, refetch } = useFetch(usersAPI.list)
  const [search, setSearch]     = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm]         = useState(EMPTY_FORM)
  const [saving, setSaving]     = useState(false)
  const [deletingId, setDeletingId] = useState(null)
  const toast = useToast()

  const filtered = Array.isArray(users)
    ? users.filter(u => u.email?.toLowerCase().includes(search.toLowerCase()))
    : []

  const handleDelete = async (id, email) => {
    if (!confirm(`Supprimer ${email} ?`)) return
    setDeletingId(id)
    await new Promise(r => setTimeout(r, 400))
    try {
      await usersAPI.delete(id)
      toast('Utilisateur supprimé', 'success')
      refetch()
    } catch {
      toast('Erreur lors de la suppression', 'error')
    } finally {
      setDeletingId(null)
    }
  }

  const handleAdd = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await usersAPI.create(form)
      toast('Utilisateur créé', 'success')
      setForm(EMPTY_FORM)
      setShowForm(false)
      refetch()
    } catch (err) {
      toast(err?.response?.data?.message || 'Erreur lors de la création', 'error')
    } finally {
      setSaving(false)
    }
  }

  const handleExport = () => {
    const rows = filtered.map(u => ({
      email: u.email,
      role: u.roleAsString || 'MEMBRE',
      créé_le: u.createdAt ? new Date(u.createdAt).toLocaleDateString('fr-FR') : '',
    }))
    exportCSV(rows, 'utilisateurs.csv')
  }

  return (
    <ErrorBoundary>
    <Layout>
      <div className="max-w-5xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">Utilisateurs</h1>
            <p className="text-sm mt-0.5" style={{ color: '#3b6cbd' }}>{users?.length || 0} utilisateurs inscrits</p>
          </div>
          <button onClick={() => setShowForm(v => !v)}
            className="btn-primary text-sm">
            {showForm ? 'Annuler' : '+ Ajouter'}
          </button>
        </div>

        {/* Add user form */}
        {showForm && (
          <div className="card">
            <h2 className="text-white font-semibold mb-4">Nouvel utilisateur</h2>
            <form onSubmit={handleAdd} className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="sm:col-span-2">
                <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Email *</label>
                <input className="input" type="email" required placeholder="email@example.com"
                  value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))}/>
              </div>
              <div>
                <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Mot de passe *</label>
                <input className="input" type="password" required placeholder="••••••••"
                  value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))}/>
              </div>
              <div>
                <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Rôle</label>
                <select className="input" value={form.role}
                  onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
                  <option value="MEMBRE">Membre</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
              {form.role === 'MEMBRE' && (
                <>
                  <div>
                    <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Nom</label>
                    <input className="input" placeholder="Nom"
                      value={form.nom} onChange={e => setForm(f => ({ ...f, nom: e.target.value }))}/>
                  </div>
                  <div>
                    <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Prénom</label>
                    <input className="input" placeholder="Prénom"
                      value={form.prenom} onChange={e => setForm(f => ({ ...f, prenom: e.target.value }))}/>
                  </div>
                </>
              )}
              <div className="sm:col-span-2 flex justify-end">
                <button type="submit" disabled={saving} className="btn-primary text-sm">
                  {saving ? 'Création...' : 'Créer'}
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="card">
          <div className="flex items-center gap-3 mb-5">
            <input className="input max-w-sm" placeholder="🔍 Rechercher par email..."
              value={search} onChange={e => setSearch(e.target.value)}/>
            <span className="text-sm" style={{ color: '#3b6cbd' }}>{filtered.length} résultats</span>
            <button onClick={handleExport} disabled={!filtered.length}
              className="ml-auto hover:text-white text-xs px-3 py-1.5 rounded-lg border transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
              style={{ color: '#3b6cbd', borderColor: '#1a2845' }}>
              ↓ Exporter CSV
            </button>
          </div>

          {loading ? (
            <div className="flex justify-center py-16"><Spinner size="lg"/></div>
          ) : error ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <p className="text-red-400 text-sm font-medium">Erreur : {error}</p>
              <button onClick={refetch} className="text-xs underline" style={{ color: '#3b6cbd' }}>Réessayer</button>
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left" style={{ color: '#3b6cbd', borderBottom: '1px solid #1a2845' }}>
                  <th className="pb-3 pr-4">Email</th>
                  <th className="pb-3 pr-4">Rôle</th>
                  <th className="pb-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1a2845]">
                {filtered.map((u) => (
                  <tr key={u.id}
                    className={`transition-all duration-500 ${
                      deletingId === u.id
                        ? 'bg-red-900/30 opacity-0'
                        : 'hover:bg-[#162040]'
                    }`}
                    style={deletingId !== u.id ? { color: '#93c5fd' } : {}}>
                    <td className="py-3 pr-4">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-full flex items-center justify-center text-xs text-blue-300"
                          style={{ background: '#162040' }}>
                          {u.email?.[0]?.toUpperCase()}
                        </div>
                        {u.email}
                      </div>
                    </td>
                    <td className="py-3 pr-4">
                      <span className={u.roleAsString === 'ADMIN' ? 'badge-red' : 'badge-green'}>
                        {u.roleAsString || 'MEMBRE'}
                      </span>
                    </td>
                    <td className="py-3 text-right">
                      {u.roleAsString !== 'ADMIN' && (
                        <button onClick={() => handleDelete(u.id, u.email)}
                          className="text-red-400 hover:text-red-300 text-xs hover:bg-red-900/20 px-2 py-1 rounded transition-colors">
                          Supprimer
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </Layout>
    </ErrorBoundary>
  )
}
