import { useState, useEffect } from 'react'
import Layout from '../components/layout/Layout'
import { useFetch } from '../hooks/useFetch'
import { blocsAPI } from '../services/api'
import Spinner from '../components/ui/Spinner'
import { useToast } from '../components/ui/Toast'
import { blocIcon, structureIcon, wifiIcon, mapIcon, pmrIcon, dangerIcon } from '../assets/icons'
import CampusMap, { POSITIONS } from '../components/layout/CampusMap'

const CATEGORIES = [
  { value: 'SALLE_ETUDE', label: "Salle d'étude" },
  { value: 'BUREAU',      label: 'Bureau / Département' },
]
const ENTREES = ['DROITE', 'GAUCHE', 'CENTRE']

const mkEtage = (i) => ({
  numero: i,
  label: i === 0 ? 'Rez-de-chaussée' : `${i}${i === 1 ? 'er' : 'e'} étage`,
  accessiblePmr: i === 0,
  sallesCount: 1,
  salles: [],
})

const mkSalle = (ordre) => ({
  nom: '', categorie: 'SALLE_ETUDE',
  ordreDepuisEntree: ordre, entreeReference: 'DROITE', accessiblePmr: true,
})

const INIT_WIZ = {
  step: 'bloc',
  bloc: { code: '', nom: '', description: '', accessiblePmr: false },
  position: null,
  etagesCount: 1,
  currentEtage: 0,
  etages: [],
}

// ─── Wizard sub-components ────────────────────────────────────────────────────

function WizHeader({ title, sub }) {
  return (
    <div className="mb-4">
      <h3 className="text-white font-semibold text-base">{title}</h3>
      {sub && <p className="text-xs mt-0.5" style={{ color: '#3b6cbd' }}>{sub}</p>}
    </div>
  )
}

function WizFooter({ onBack, onNext, nextLabel = 'Suivant →', disabled = false }) {
  return (
    <div className="flex justify-between pt-4 border-t mt-4" style={{ borderColor: '#1a2845' }}>
      {onBack
        ? <button onClick={onBack}
            className="text-sm px-3 py-2 rounded-lg hover:text-white hover:bg-[#162040] transition-colors"
            style={{ color: '#3b6cbd' }}>
            ← Retour
          </button>
        : <div />}
      <button onClick={onNext} disabled={disabled}
        className="btn-primary text-sm disabled:opacity-50 disabled:cursor-not-allowed">
        {nextLabel}
      </button>
    </div>
  )
}

// ─── RSSI color helper ────────────────────────────────────────────────────────

const rssiColor = (v) => v > -60 ? '#60a5fa' : v > -75 ? '#818cf8' : '#a78bfa'

// ─── Main page ────────────────────────────────────────────────────────────────

export default function Blocs() {
  const { data: blocs, loading, refetch } = useFetch(blocsAPI.list)

  const [selected, setSelected]           = useState(null)
  const [search, setSearch]               = useState('')
  const [wiz, setWiz]                     = useState(null)
  const [saving, setSaving]               = useState(false)
  const [tab, setTab]                     = useState('structure')
  const [pageTab, setPageTab]             = useState('blocs')

  const LS_KEY = 'fsm_bloc_positions'
  const [customPositions, setCustomPositions] = useState(() => {
    try { return JSON.parse(localStorage.getItem(LS_KEY) || '{}') }
    catch { return {} }
  })
  const saveCustomPositions = (next) => {
    setCustomPositions(next)
    localStorage.setItem(LS_KEY, JSON.stringify(next))
  }

  // Edit states
  const [editBloc, setEditBloc]           = useState(null)
  const [editSalle, setEditSalle]         = useState(null)

  // Add salle to existing étage
  const EMPTY_NEW_SALLE = { nom: '', categorie: 'SALLE_ETUDE', entreeReference: 'DROITE', ordreDepuisEntree: 1, accessiblePmr: true, disponible: true }
  const [addSalleEtageId, setAddSalleEtageId] = useState(null)
  const [newSalle, setNewSalle]               = useState(EMPTY_NEW_SALLE)
  const [addingSalle, setAddingSalle]         = useState(false)

  // Add étage to existing bloc
  const EMPTY_NEW_ETAGE = { numero: 1, label: '', accessiblePmr: false }
  const [showAddEtage, setShowAddEtage] = useState(false)
  const [newEtage, setNewEtage]         = useState(EMPTY_NEW_ETAGE)
  const [addingEtage, setAddingEtage]   = useState(false)

  // Delete animation
  const [deletingBlocId, setDeletingBlocId]   = useState(null)
  const [deletingSalleId, setDeletingSalleId] = useState(null)

  // WiFi / POI state
  const [pois, setPois]               = useState([])
  const [fingerprints, setFingerprints] = useState([])
  const [loadingWifi, setLoadingWifi] = useState(false)
  const [fpForm, setFpForm]           = useState(null)
  const [addingFp, setAddingFp]       = useState(false)

  const toast = useToast()

  // Keep selected in sync after refetch
  useEffect(() => {
    if (selected && blocs) {
      const updated = blocs.find(b => b.id === selected.id)
      setSelected(updated || null)
    }
  }, [blocs])

  // Load WiFi data when WiFi tab is opened
  useEffect(() => {
    if (tab === 'wifi' && selected) loadWifi()
  }, [tab, selected?.id])

  const loadWifi = async () => {
    setLoadingWifi(true)
    try {
      const [poisRes, fpRes] = await Promise.all([
        blocsAPI.getPoiByBloc(selected.id),
        blocsAPI.getFingerprintsByBloc(selected.id),
      ])
      setPois(Array.isArray(poisRes.data) ? poisRes.data : [])
      setFingerprints(Array.isArray(fpRes.data) ? fpRes.data : [])
    } catch {
      toast('Erreur lors du chargement des POI', 'error')
    } finally {
      setLoadingWifi(false)
    }
  }

  const filtered = blocs?.filter(b =>
    b.nom?.toLowerCase().includes(search.toLowerCase()) ||
    b.code?.toLowerCase().includes(search.toLowerCase())
  ) || []

  // ── Delete with animation ───────────────────────────────────────────────────

  const handleDeleteBloc = async (b) => {
    if (!confirm(`Supprimer "${b.nom}" et toutes ses salles ?`)) return
    setDeletingBlocId(b.id)
    await new Promise(r => setTimeout(r, 400))
    try {
      await blocsAPI.deleteBloc(b.id)
      const { [b.code]: _, ...rest } = customPositions
      saveCustomPositions(rest)
      toast('Bloc supprimé', 'success')
      if (selected?.id === b.id) setSelected(null)
      refetch()
    } catch {
      toast('Erreur lors de la suppression', 'error')
    } finally {
      setDeletingBlocId(null)
    }
  }

  const handleDeleteSalle = async (salle) => {
    if (!confirm(`Supprimer "${salle.nom}" ?`)) return
    setDeletingSalleId(salle.id)
    await new Promise(r => setTimeout(r, 400))
    try {
      await blocsAPI.deleteSalle(salle.id)
      toast('Salle supprimée', 'success')
      refetch()
    } catch {
      toast('Erreur lors de la suppression', 'error')
    } finally {
      setDeletingSalleId(null)
    }
  }

  // ── Edit bloc ───────────────────────────────────────────────────────────────

  const openEditBloc = () => setEditBloc({
    code: selected.code,
    nom: selected.nom,
    description: selected.description || '',
    accessiblePmr: selected.accessiblePmr || false,
  })

  const saveEditBloc = async () => {
    if (!editBloc.nom.trim() || !editBloc.code.trim()) {
      toast('Code et nom requis', 'error'); return
    }
    setSaving(true)
    try {
      await blocsAPI.updateBloc(selected.id, editBloc)
      toast('Bloc mis à jour', 'success')
      setEditBloc(null)
      refetch()
    } catch (err) {
      toast(err?.response?.data?.message || 'Erreur', 'error')
    } finally {
      setSaving(false)
    }
  }

  // ── Edit salle ──────────────────────────────────────────────────────────────

  const saveEditSalle = async () => {
    if (!editSalle.nom.trim()) { toast('Nom requis', 'error'); return }
    setSaving(true)
    try {
      await blocsAPI.updateSalle(editSalle.id, {
        nom: editSalle.nom,
        categorie: editSalle.categorie,
        accessiblePmr: editSalle.accessiblePmr,
        disponible: editSalle.disponible,
      })
      toast('Salle mise à jour', 'success')
      setEditSalle(null)
      refetch()
    } catch {
      toast('Erreur lors de la mise à jour', 'error')
    } finally {
      setSaving(false)
    }
  }

  // ── Add salle to existing étage ────────────────────────────────────────────

  const openAddSalle = (etageId) => {
    setAddSalleEtageId(etageId)
    setNewSalle(EMPTY_NEW_SALLE)
  }

  const handleAddSalle = async (etageId) => {
    if (!newSalle.nom.trim()) { toast('Nom de la salle requis', 'error'); return }
    setAddingSalle(true)
    try {
      await blocsAPI.createSalle({ ...newSalle, etageId })
      toast(`Salle "${newSalle.nom}" ajoutée`, 'success')
      setAddSalleEtageId(null)
      setNewSalle(EMPTY_NEW_SALLE)
      refetch()
    } catch (err) {
      toast(err?.response?.data?.message || 'Erreur lors de l\'ajout', 'error')
    } finally {
      setAddingSalle(false)
    }
  }

  // ── Add étage to existing bloc ─────────────────────────────────────────────

  const handleAddEtage = async () => {
    if (!newEtage.label.trim()) { toast('Label de l\'étage requis', 'error'); return }
    setAddingEtage(true)
    try {
      await blocsAPI.createEtage({ ...newEtage, blocId: selected.id })
      toast(`Étage "${newEtage.label}" ajouté`, 'success')
      setShowAddEtage(false)
      setNewEtage(EMPTY_NEW_ETAGE)
      refetch()
    } catch (err) {
      toast(err?.response?.data?.message || 'Erreur lors de l\'ajout', 'error')
    } finally {
      setAddingEtage(false)
    }
  }

  // ── Fingerprints ────────────────────────────────────────────────────────────

  const handleAddFingerprint = async () => {
    if (!fpForm.bssid.trim() || fpForm.rssiMoyen === '') {
      toast('BSSID et RSSI requis', 'error'); return
    }
    setAddingFp(true)
    try {
      await blocsAPI.createFingerprint({
        bssid: fpForm.bssid.trim(),
        ssid: fpForm.ssid.trim(),
        rssiMoyen: parseFloat(fpForm.rssiMoyen),
        poiId: fpForm.poiId,
      })
      toast('Fingerprint ajouté', 'success')
      setFpForm(null)
      loadWifi()
    } catch (err) {
      toast(err?.response?.data?.message || 'Erreur', 'error')
    } finally {
      setAddingFp(false)
    }
  }

  const handleDeleteFingerprint = async (id) => {
    try {
      await blocsAPI.deleteFingerprint(id)
      setFingerprints(prev => prev.filter(f => f.id !== id))
      toast('Fingerprint supprimé', 'success')
    } catch {
      toast('Erreur lors de la suppression', 'error')
    }
  }

  // ── Export / Import ─────────────────────────────────────────────────────────

  const handleExportBloc = async () => {
    try {
      const { data } = await blocsAPI.exportBloc(selected.id)
      const json = JSON.stringify(data, null, 2)
      const blob = new Blob([json], { type: 'application/json' })
      const url  = URL.createObjectURL(blob)
      const a    = document.createElement('a')
      a.href = url; a.download = `bloc-${selected.code}.json`; a.click()
      URL.revokeObjectURL(url)
      toast('Export téléchargé', 'success')
    } catch {
      toast('Erreur lors de l\'export', 'error')
    }
  }

  const handleImportBloc = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    e.target.value = ''
    try {
      const text = await file.text()
      const data = JSON.parse(text)
      await blocsAPI.importBloc(data)
      toast(`Bloc "${data.nom}" importé`, 'success')
      refetch()
    } catch (err) {
      toast(err?.response?.data?.message || 'Fichier invalide', 'error')
    }
  }

  // ── Wizard helpers ──────────────────────────────────────────────────────────

  const patchWiz   = (patch) => setWiz(w => ({ ...w, ...patch }))
  const patchBloc  = (patch) => setWiz(w => ({ ...w, bloc: { ...w.bloc, ...patch } }))
  const patchEtage = (i, patch) => setWiz(w => ({
    ...w, etages: w.etages.map((e, idx) => idx === i ? { ...e, ...patch } : e),
  }))
  const patchSalle = (ei, si, patch) => setWiz(w => ({
    ...w,
    etages: w.etages.map((e, i) => i !== ei ? e : {
      ...e, salles: e.salles.map((s, j) => j !== si ? s : { ...s, ...patch }),
    }),
  }))

  const goLocalisation = () => {
    if (!wiz.bloc.code.trim() || !wiz.bloc.nom.trim()) {
      toast('Code et nom du bloc requis', 'error'); return
    }
    if (blocs?.some(b => b.code === wiz.bloc.code.trim())) {
      toast(`Le code "${wiz.bloc.code}" est déjà utilisé par un autre bloc`, 'error'); return
    }
    patchWiz({ step: 'localisation' })
  }

  const goEtagesCount = () => patchWiz({ step: 'etages_count' })

  const handleMapClick = (e) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const x = parseFloat(((e.clientX - rect.left) / rect.width * 100).toFixed(2))
    const y = parseFloat(((e.clientY - rect.top) / rect.height * 100).toFixed(2))
    patchWiz({ position: { x, y } })
  }

  const goFirstEtage = () => {
    const n = Math.max(1, parseInt(wiz.etagesCount) || 1)
    setWiz(w => ({
      ...w, etagesCount: n,
      etages: Array.from({ length: n }, (_, i) => mkEtage(i)),
      currentEtage: 0, step: 'etage',
    }))
  }

  const goSalles = () => {
    const ei    = wiz.currentEtage
    const count = Math.max(0, parseInt(wiz.etages[ei].sallesCount) || 0)
    const salles = Array.from({ length: count }, (_, i) => mkSalle(i + 1))
    setWiz(w => {
      const etages = w.etages.map((e, i) => i === ei ? { ...e, salles, sallesCount: count } : e)
      if (count === 0) {
        const next = ei + 1
        return { ...w, etages, currentEtage: next < w.etagesCount ? next : ei,
          step: next < w.etagesCount ? 'etage' : 'confirm' }
      }
      return { ...w, etages, step: 'salles' }
    })
  }

  const goNextEtageOrConfirm = () => {
    const etage = wiz.etages[wiz.currentEtage]
    const empty = etage.salles.filter(s => !s.nom.trim())
    if (empty.length > 0) {
      toast(`${empty.length} salle(s) sans nom — veuillez remplir tous les noms`, 'error')
      return
    }
    const next = wiz.currentEtage + 1
    if (next < wiz.etagesCount) patchWiz({ currentEtage: next, step: 'etage' })
    else patchWiz({ step: 'confirm' })
  }

  const handleSubmit = async () => {
    setSaving(true)
    try {
      const { data: bloc } = await blocsAPI.create(wiz.bloc)
      if (wiz.position) {
        await blocsAPI.createPoi({
          nom: `Entrée ${wiz.bloc.nom}`,
          type: 'ENTREE',
          x: wiz.position.x,
          y: wiz.position.y,
          accessiblePmr: wiz.bloc.accessiblePmr,
          blocId: bloc.id,
        })
      }
      for (const etage of wiz.etages) {
        const { data: savedEtage } = await blocsAPI.createEtage({
          numero: etage.numero, label: etage.label,
          accessiblePmr: etage.accessiblePmr, blocId: bloc.id,
        })
        for (const salle of etage.salles) {
          await blocsAPI.createSalle({ ...salle, etageId: savedEtage.id })
        }
      }
      if (wiz.position) {
        saveCustomPositions({ ...customPositions, [wiz.bloc.code]: wiz.position })
      }
      toast(`Bloc "${wiz.bloc.nom}" créé`, 'success')
      setWiz(null)
      refetch()
    } catch (err) {
      toast(err?.response?.data?.message || 'Erreur lors de la création', 'error')
    } finally {
      setSaving(false)
    }
  }

  // ── Wizard render ───────────────────────────────────────────────────────────

  const renderWiz = () => {
    const { step, bloc, etagesCount, etages, currentEtage } = wiz

    if (step === 'bloc') return (
      <>
        <WizHeader title="Informations du bloc" sub="Étape 1 — Détails généraux"/>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Code *</label>
            <input className="input" placeholder="ex: B5" value={bloc.code}
              onChange={e => patchBloc({ code: e.target.value.toUpperCase() })}/>
            {bloc.code && blocs?.some(b => b.code === bloc.code) && (
              <p className="text-xs mt-1 text-red-400">⚠ Ce code est déjà utilisé</p>
            )}
          </div>
          <div>
            <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Nom *</label>
            <input className="input" placeholder="ex: Bloc 5" value={bloc.nom}
              onChange={e => patchBloc({ nom: e.target.value })}/>
          </div>
          <div className="col-span-2">
            <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Description</label>
            <input className="input" placeholder="ex: Salles 501→520" value={bloc.description}
              onChange={e => patchBloc({ description: e.target.value })}/>
          </div>
          <label className="col-span-2 flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={bloc.accessiblePmr}
              onChange={e => patchBloc({ accessiblePmr: e.target.checked })} className="rounded"/>
            <span className="text-sm" style={{ color: '#93c5fd' }}>Accessible PMR</span>
          </label>
        </div>
        <WizFooter onNext={goLocalisation}/>
      </>
    )

    if (step === 'localisation') return (
      <>
        <WizHeader title="Localisation sur le campus" sub="Étape 2 — Cliquez pour placer le bloc"/>
        <div className="relative rounded-lg overflow-hidden select-none"
          style={{ border: '1px solid #1a2845', cursor: 'crosshair' }}>
          <img
            src="/campus.png"
            alt="Campus FSM"
            className="w-full block"
            draggable={false}
            onClick={handleMapClick}
          />
          {/* Existing bloc markers */}
          {blocs?.map(b => {
            const pos = POSITIONS[b.code] || customPositions[b.code]
            if (!pos) return null
            return (
              <div key={b.id} className="absolute pointer-events-none flex flex-col items-center"
                style={{ left: `${pos.x}%`, top: `${pos.y}%`, transform: 'translate(-50%, -100%)', zIndex: 10 }}>
                <div className="text-xs font-bold px-2 py-0.5 rounded-full shadow-lg whitespace-nowrap bg-blue-600 text-white">
                  {b.code}
                </div>
                <div className="w-px h-2 bg-blue-400"/>
                <div className="w-3 h-3 rounded-full border-2 border-white shadow-lg bg-blue-500"/>
              </div>
            )
          })}
          {/* New bloc position */}
          {wiz.position && (
            <div className="absolute pointer-events-none flex flex-col items-center"
              style={{ left: `${wiz.position.x}%`, top: `${wiz.position.y}%`, transform: 'translate(-50%, -100%)', zIndex: 20 }}>
              <div className="bg-emerald-400 text-[#060A14] text-xs font-bold px-2 py-0.5 rounded-full shadow-lg whitespace-nowrap">
                {wiz.bloc.nom || wiz.bloc.code || 'Nouveau bloc'}
              </div>
              <div className="w-px h-2 bg-emerald-400"/>
              <div className="relative flex items-center justify-center">
                <div className="w-3 h-3 bg-emerald-400 rounded-full border-2 border-white shadow-lg"/>
                <div className="absolute w-3 h-3 rounded-full bg-emerald-400 animate-ping opacity-75"/>
              </div>
            </div>
          )}
        </div>
        <p className="text-xs text-center mt-2"
          style={{ color: wiz.position ? '#60a5fa' : '#3b6cbd' }}>
          {wiz.position
            ? '✓ Position marquée — recliquez pour ajuster'
            : "Cliquez sur la carte pour marquer l'emplacement du bloc"}
        </p>
        <WizFooter
          onBack={() => patchWiz({ step: 'bloc' })}
          onNext={goEtagesCount}
          disabled={!wiz.position}
        />
      </>
    )

    if (step === 'etages_count') return (
      <>
        <WizHeader title="Combien d'étages ?" sub="Étape 3 — Structure du bloc"/>
        <div className="flex flex-col items-center gap-2 py-6">
          <input type="number" min="1" max="10" className="input w-28 text-center text-2xl font-bold"
            value={etagesCount} onChange={e => patchWiz({ etagesCount: e.target.value })}/>
          <p className="text-xs" style={{ color: '#3b6cbd' }}>rez-de-chaussée compris</p>
        </div>
        <WizFooter onBack={() => patchWiz({ step: 'localisation' })} onNext={goFirstEtage}/>
      </>
    )

    if (step === 'etage') {
      const etage = etages[currentEtage]
      return (
        <>
          <WizHeader title={`Étage ${currentEtage + 1} / ${etagesCount}`} sub="Informations et nombre de salles"/>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Numéro</label>
              <input type="number" className="input" value={etage.numero}
                onChange={e => patchEtage(currentEtage, { numero: parseInt(e.target.value) || 0 })}/>
            </div>
            <div>
              <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Label</label>
              <input className="input" placeholder="ex: Rez-de-chaussée" value={etage.label}
                onChange={e => patchEtage(currentEtage, { label: e.target.value })}/>
            </div>
            <label className="col-span-2 flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={etage.accessiblePmr}
                onChange={e => patchEtage(currentEtage, { accessiblePmr: e.target.checked })} className="rounded"/>
              <span className="text-sm" style={{ color: '#93c5fd' }}>Étage accessible PMR</span>
            </label>
            <div className="col-span-2 border-t pt-3" style={{ borderColor: '#1a2845' }}>
              <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Combien de salles ?</label>
              <input type="number" min="0" max="50" className="input w-32" value={etage.sallesCount}
                onChange={e => patchEtage(currentEtage, { sallesCount: e.target.value })}/>
            </div>
          </div>
          <WizFooter
            onBack={() => currentEtage === 0
              ? patchWiz({ step: 'etages_count' })
              : patchWiz({ currentEtage: currentEtage - 1, step: 'salles' })}
            onNext={goSalles}
          />
        </>
      )
    }

    if (step === 'salles') {
      const etage = etages[currentEtage]
      return (
        <>
          <WizHeader title={`Salles — ${etage.label}`} sub={`${etage.salles.length} salle(s) à configurer`}/>
          <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
            {etage.salles.map((salle, si) => (
              <div key={si} className="rounded-lg p-3 space-y-2" style={{ background: '#0A1020' }}>
                <p className="text-xs font-medium" style={{ color: '#3b6cbd' }}>Salle {si + 1}</p>
                <div className="space-y-2">
                  <div>
                    <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Nom *</label>
                    <input className="input text-sm" placeholder="ex: Salle 501" value={salle.nom}
                      onChange={e => patchSalle(currentEtage, si, { nom: e.target.value })}
                      style={salle.nom === '' ? { borderColor: '#ef4444' } : {}}/>
                  </div>
                  <div>
                    <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Catégorie</label>
                    <select className="input text-sm" value={salle.categorie}
                      onChange={e => patchSalle(currentEtage, si, { categorie: e.target.value })}>
                      {CATEGORIES.map(c => <option key={c.value} value={c.value}>{c.label}</option>)}
                    </select>
                  </div>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={salle.accessiblePmr}
                      onChange={e => patchSalle(currentEtage, si, { accessiblePmr: e.target.checked })} className="rounded"/>
                    <span className="text-xs" style={{ color: '#93c5fd' }}>PMR</span>
                  </label>
                </div>
              </div>
            ))}
          </div>
          <WizFooter
            onBack={() => patchWiz({ step: 'etage' })}
            onNext={goNextEtageOrConfirm}
            nextLabel={currentEtage + 1 < etagesCount ? 'Étage suivant →' : 'Récapitulatif →'}
          />
        </>
      )
    }

    if (step === 'confirm') {
      const totalSalles = etages.reduce((s, e) => s + e.salles.length, 0)
      return (
        <>
          <WizHeader title="Récapitulatif" sub="Vérifiez avant de créer"/>
          <div className="rounded-lg p-4 space-y-3" style={{ background: '#0A1020' }}>
            <div className="flex items-center gap-3">
              <img src={blocIcon} alt="" className="w-9 h-9 object-contain icon"/>
              <div>
                <p className="text-white font-bold">{bloc.nom}</p>
                <p className="font-mono text-sm" style={{ color: '#3b6cbd' }}>{bloc.code}</p>
              </div>
              {bloc.accessiblePmr && <span className="ml-auto text-blue-400 text-sm flex items-center gap-1"><img src={pmrIcon} alt="" className="w-4 h-4 object-contain icon"/> PMR</span>}
            </div>
            {bloc.description && <p className="text-sm" style={{ color: '#3b6cbd' }}>{bloc.description}</p>}
            {wiz.position && (
              <div className="flex items-center gap-2 text-xs" style={{ color: '#60a5fa' }}>
                <img src={mapIcon} alt="" className="w-3.5 h-3.5 object-contain icon"/>
                <span>Positionné sur le campus</span>
              </div>
            )}
            <div className="border-t pt-3 space-y-1.5" style={{ borderColor: '#1a2845' }}>
              {etages.map((e, i) => (
                <div key={i} className="flex justify-between text-sm">
                  <span style={{ color: '#93c5fd' }}>{e.label}</span>
                  <span style={{ color: '#3b6cbd' }}>{e.salles.length} salle(s)</span>
                </div>
              ))}
            </div>
            <p className="text-xs border-t pt-2" style={{ color: '#3b6cbd', borderColor: '#1a2845' }}>
              Total : {etagesCount} étage(s) · {totalSalles} salle(s)
            </p>
          </div>
          <WizFooter
            onBack={() => patchWiz({ step: 'salles', currentEtage: etagesCount - 1 })}
            onNext={handleSubmit}
            nextLabel={saving ? 'Création en cours...' : 'Créer le bloc'}
            disabled={saving}
          />
        </>
      )
    }
  }

  // ── WiFi tab render ─────────────────────────────────────────────────────────

  const renderWifi = () => {
    if (loadingWifi) return <div className="flex justify-center py-10"><Spinner/></div>

    if (!pois.length) return (
      <div className="flex flex-col items-center justify-center py-12 text-sm gap-2"
        style={{ color: '#3b6cbd' }}>
        <img src={wifiIcon} alt="" className="w-10 h-10 object-contain icon opacity-50"/>
        <p>Aucun point de localisation dans ce bloc</p>
        <p className="text-xs" style={{ color: '#2d4a7a' }}>Les POI sont créés automatiquement avec les salles</p>
      </div>
    )

    return (
      <div className="space-y-3 max-h-[480px] overflow-y-auto pr-1">
        {pois.map(poi => {
          const poiFps = fingerprints.filter(f => f.poiId === poi.id)
          const isAddingHere = fpForm?.poiId === poi.id
          return (
            <div key={poi.id} className="rounded-xl p-4 space-y-3" style={{ background: '#0A1020' }}>
              {/* POI header */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <p className="text-white font-medium text-sm">{poi.nom}</p>
                  <span className="text-xs font-mono px-1.5 py-0.5 rounded"
                    style={{ color: '#3b6cbd', background: '#162040' }}>
                    {poi.type}
                  </span>
                  {poi.accessiblePmr && <img src={pmrIcon} alt="PMR" className="w-3.5 h-3.5 object-contain icon"/>}
                </div>
                <button
                  onClick={() => setFpForm(isAddingHere ? null : { poiId: poi.id, bssid: '', ssid: '', rssiMoyen: '' })}
                  className="text-xs text-blue-400 hover:text-blue-300 px-2 py-1 rounded hover:bg-blue-900/20 transition-colors flex-shrink-0">
                  {isAddingHere ? '✕ Annuler' : '+ Fingerprint'}
                </button>
              </div>

              {/* Add fingerprint form */}
              {isAddingHere && (
                <div className="rounded-lg p-3 space-y-2 border" style={{ background: '#162040', borderColor: '#1a2845' }}>
                  <div className="grid grid-cols-3 gap-2">
                    <div className="col-span-3 sm:col-span-1">
                      <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>BSSID *</label>
                      <input className="input text-xs" placeholder="aa:bb:cc:dd:ee:ff"
                        value={fpForm.bssid}
                        onChange={e => setFpForm(f => ({ ...f, bssid: e.target.value }))}/>
                    </div>
                    <div>
                      <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>SSID</label>
                      <input className="input text-xs" placeholder="WiFi-Campus"
                        value={fpForm.ssid}
                        onChange={e => setFpForm(f => ({ ...f, ssid: e.target.value }))}/>
                    </div>
                    <div>
                      <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>RSSI (dBm) *</label>
                      <input type="number" className="input text-xs" placeholder="-65"
                        value={fpForm.rssiMoyen}
                        onChange={e => setFpForm(f => ({ ...f, rssiMoyen: e.target.value }))}/>
                    </div>
                  </div>
                  <button onClick={handleAddFingerprint} disabled={addingFp}
                    className="btn-primary text-xs w-full disabled:opacity-50">
                    {addingFp ? 'Ajout...' : 'Ajouter le fingerprint'}
                  </button>
                </div>
              )}

              {/* Fingerprints list */}
              {poiFps.length === 0 ? (
                <p className="text-xs italic" style={{ color: '#2d4a7a' }}>Aucun fingerprint WiFi enregistré</p>
              ) : (
                <div className="space-y-1">
                  <p className="text-xs mb-1" style={{ color: '#3b6cbd' }}>{poiFps.length} fingerprint(s)</p>
                  {poiFps.map(fp => (
                    <div key={fp.id} className="group flex items-center gap-2 rounded px-2.5 py-1.5"
                      style={{ background: '#162040' }}>
                      <span className="font-mono text-xs text-blue-400 flex-shrink-0 w-36 truncate">
                        {fp.bssid}
                      </span>
                      {fp.ssid && (
                        <span className="text-xs truncate flex-1" style={{ color: '#3b6cbd' }}>{fp.ssid}</span>
                      )}
                      <span className="text-xs font-semibold ml-auto flex-shrink-0"
                        style={{ color: rssiColor(fp.rssiMoyen) }}>
                        {fp.rssiMoyen} dBm
                      </span>
                      <button onClick={() => handleDeleteFingerprint(fp.id)}
                        className="opacity-0 group-hover:opacity-100 text-red-400 hover:text-red-300 text-xs transition-all ml-1 flex-shrink-0">
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </div>
    )
  }

  // ── Main render ─────────────────────────────────────────────────────────────

  return (
    <Layout>
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">Blocs & Salles</h1>
            <p className="text-sm mt-0.5" style={{ color: '#3b6cbd' }}>Vue d'ensemble des bâtiments et des salles</p>
          </div>
          <div className="flex gap-2">
            <label className="cursor-pointer hover:text-white text-sm px-3 py-2 rounded-lg border transition-colors"
              style={{ color: '#3b6cbd', borderColor: '#1a2845' }}>
              ↑ Importer JSON
              <input type="file" accept=".json" className="hidden" onChange={handleImportBloc}/>
            </label>
            <button onClick={() => setWiz({ ...INIT_WIZ })} className="btn-primary text-sm">
              + Créer un bloc
            </button>
          </div>
        </div>

        {/* Page tabs */}
        <div className="flex gap-0 border-b" style={{ borderColor: '#1a2845' }}>
          {[
            ['blocs', 'Blocs & Salles', blocIcon],
            ['carte', 'Carte du campus', mapIcon],
          ].map(([key, label, icon]) => (
            <button key={key} onClick={() => setPageTab(key)}
              className={`flex items-center gap-2 px-5 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
                pageTab === key
                  ? 'text-blue-400 border-blue-400'
                  : 'border-transparent hover:text-white'
              }`}
              style={pageTab !== key ? { color: '#3b6cbd' } : {}}>
              <img src={icon} alt="" className="w-4 h-4 object-contain icon"/>
              {label}
            </button>
          ))}
        </div>

        {/* ── Carte tab ── */}
        {pageTab === 'carte' && (
          <CampusMap
            blocs={blocs || []}
            extraPositions={customPositions}
            selected={selected}
            onSelect={b => { setSelected(b); setPageTab('blocs'); setTab('structure'); setEditBloc(null); setEditSalle(null); setAddSalleEtageId(null); setShowAddEtage(false) }}
            newPosition={wiz?.position}
            newLabel={wiz?.bloc?.code || wiz?.bloc?.nom || 'Nouveau bloc'}
          />
        )}

        {pageTab === 'blocs' && <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">

          {/* ── Liste des blocs ── */}
          <div className="card">
            <div className="mb-3">
              <input className="input text-sm" placeholder="🔍 Rechercher..."
                value={search} onChange={e => setSearch(e.target.value)}/>
            </div>
            {loading ? (
              <div className="flex justify-center py-10"><Spinner/></div>
            ) : (
              <div className="space-y-1 max-h-96 overflow-y-auto">
                {filtered.map((b) => (
                  <div key={b.id}
                    className={`group flex items-center gap-1 transition-all duration-500 ${
                      deletingBlocId === b.id ? 'opacity-0 -translate-x-2' : ''
                    }`}>
                    <button
                      onClick={() => { setSelected(b); setTab('structure'); setEditBloc(null); setEditSalle(null); setAddSalleEtageId(null); setShowAddEtage(false) }}
                      className={`flex-1 text-left px-3 py-2.5 rounded-lg transition-colors text-sm ${
                        selected?.id === b.id
                          ? 'bg-blue-600/20 text-blue-400'
                          : 'hover:bg-[#162040]'
                      }`}
                      style={selected?.id !== b.id ? { color: '#93c5fd' } : {}}>
                      <span className="font-mono text-xs mr-2" style={{ color: '#3b6cbd' }}>{b.code}</span>
                      {b.nom}
                      {b.accessiblePmr && <img src={pmrIcon} alt="PMR" className="ml-2 w-3.5 h-3.5 object-contain icon inline"/>}
                    </button>
                    <button onClick={() => handleDeleteBloc(b)}
                      className="opacity-0 group-hover:opacity-100 p-1.5 hover:text-red-400 hover:bg-red-900/20 rounded-lg transition-all flex-shrink-0"
                      style={{ color: '#3b6cbd' }}
                      title="Supprimer ce bloc">
                      <img src={dangerIcon} alt="Supprimer" className="w-4 h-4 object-contain icon"/>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* ── Détail bloc ── */}
          <div className="card lg:col-span-2">
            {!selected ? (
              <div className="flex flex-col items-center justify-center h-64" style={{ color: '#3b6cbd' }}>
                <img src={blocIcon} alt="" className="w-12 h-12 object-contain icon mb-3 opacity-50"/>
                <p>Sélectionnez un bloc pour voir ses détails</p>
              </div>

            ) : editBloc ? (
              /* Edit bloc inline form */
              <div className="space-y-4">
                <h2 className="text-white font-bold text-lg">Modifier le bloc</h2>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Code *</label>
                    <input className="input" value={editBloc.code}
                      onChange={e => setEditBloc(f => ({ ...f, code: e.target.value.toUpperCase() }))}/>
                  </div>
                  <div>
                    <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Nom *</label>
                    <input className="input" value={editBloc.nom}
                      onChange={e => setEditBloc(f => ({ ...f, nom: e.target.value }))}/>
                  </div>
                  <div className="col-span-2">
                    <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Description</label>
                    <input className="input" value={editBloc.description}
                      onChange={e => setEditBloc(f => ({ ...f, description: e.target.value }))}/>
                  </div>
                  <label className="col-span-2 flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={editBloc.accessiblePmr}
                      onChange={e => setEditBloc(f => ({ ...f, accessiblePmr: e.target.checked }))} className="rounded"/>
                    <span className="text-sm" style={{ color: '#93c5fd' }}>Accessible PMR</span>
                  </label>
                </div>
                <div className="flex gap-2 pt-1">
                  <button onClick={saveEditBloc} disabled={saving}
                    className="btn-primary text-sm disabled:opacity-50">
                    {saving ? 'Sauvegarde...' : 'Sauvegarder'}
                  </button>
                  <button onClick={() => setEditBloc(null)}
                    className="text-sm px-3 py-2 rounded-lg hover:text-white hover:bg-[#162040] transition-colors"
                    style={{ color: '#3b6cbd' }}>
                    Annuler
                  </button>
                </div>
              </div>

            ) : (
              <div>
                {/* Bloc header */}
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h2 className="text-xl font-bold text-white">{selected.nom}</h2>
                    <p className="text-sm" style={{ color: '#3b6cbd' }}>
                      Code : <span className="font-mono text-blue-400">{selected.code}</span>
                    </p>
                    {selected.description && (
                      <p className="text-xs mt-0.5" style={{ color: '#2d4a7a' }}>{selected.description}</p>
                    )}
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    {selected.accessiblePmr && <span className="badge-green flex items-center gap-1"><img src={pmrIcon} alt="" className="w-3.5 h-3.5 object-contain icon"/> PMR</span>}
                    <button onClick={handleExportBloc}
                      className="hover:text-white text-xs px-2.5 py-1.5 rounded-lg border transition-colors"
                      style={{ color: '#3b6cbd', borderColor: '#1a2845' }}>
                      ↓ JSON
                    </button>
                    <button onClick={openEditBloc}
                      className="hover:text-white text-xs px-2.5 py-1.5 rounded-lg border transition-colors"
                      style={{ color: '#3b6cbd', borderColor: '#1a2845' }}>
                      ✏️ Éditer
                    </button>
                  </div>
                </div>

                {/* Tabs */}
                <div className="flex gap-0 mb-4 border-b" style={{ borderColor: '#1a2845' }}>
                  {[['structure', 'Structure', structureIcon], ['wifi', 'WiFi & POI', wifiIcon]].map(([key, label, icon]) => (
                    <button key={key} onClick={() => setTab(key)}
                      className={`flex items-center gap-2 px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
                        tab === key
                          ? 'text-blue-400 border-blue-400'
                          : 'border-transparent hover:text-white'
                      }`}
                      style={tab !== key ? { color: '#3b6cbd' } : {}}>
                      <img src={icon} alt="" className="w-4 h-4 object-contain icon"/>
                      {label}
                    </button>
                  ))}
                </div>

                {/* Structure tab */}
                {tab === 'structure' && (
                  selected.etages?.map((etage, ei) => (
                    <div key={etage.id ?? ei} className="mb-5">
                      <div className="flex items-center justify-between mb-2">
                        <h3 className="font-medium text-sm flex items-center gap-2"
                          style={{ color: '#93c5fd' }}>
                          <img src={structureIcon} alt="" className="w-4 h-4 object-contain icon"/> {etage.label}
                          <span className="text-xs" style={{ color: '#3b6cbd' }}>({etage.salles?.length || 0} salles)</span>
                        </h3>
                        <button
                          onClick={() => addSalleEtageId === etage.id
                            ? setAddSalleEtageId(null)
                            : openAddSalle(etage.id)}
                          className="text-xs text-blue-400 hover:text-blue-300 px-2 py-1 rounded hover:bg-blue-900/20 transition-colors">
                          {addSalleEtageId === etage.id ? '✕ Annuler' : '+ Salle'}
                        </button>
                      </div>

                      {/* Inline add-salle form */}
                      {addSalleEtageId === etage.id && (
                        <div className="mb-3 rounded-xl p-4 space-y-3 border"
                          style={{ background: '#0A1020', borderColor: '#1a2845' }}>
                          <p className="text-xs font-medium" style={{ color: '#93c5fd' }}>Nouvelle salle — {etage.label}</p>
                          <div className="space-y-2">
                            <div>
                              <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Nom *</label>
                              <input className="input text-sm" placeholder="ex: Salle 205" autoFocus
                                value={newSalle.nom}
                                onChange={e => setNewSalle(f => ({ ...f, nom: e.target.value }))}
                                onKeyDown={e => e.key === 'Enter' && handleAddSalle(etage.id)}/>
                            </div>
                            <div>
                              <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Catégorie</label>
                              <select className="input text-sm" value={newSalle.categorie}
                                onChange={e => setNewSalle(f => ({ ...f, categorie: e.target.value }))}>
                                {CATEGORIES.map(c => <option key={c.value} value={c.value}>{c.label}</option>)}
                              </select>
                            </div>
                            <div className="flex gap-3">
                              <label className="flex items-center gap-2 cursor-pointer">
                                <input type="checkbox" checked={newSalle.accessiblePmr}
                                  onChange={e => setNewSalle(f => ({ ...f, accessiblePmr: e.target.checked }))}
                                  className="rounded"/>
                                <span className="text-xs" style={{ color: '#93c5fd' }}>PMR</span>
                              </label>
                              <label className="flex items-center gap-2 cursor-pointer">
                                <input type="checkbox" checked={newSalle.disponible}
                                  onChange={e => setNewSalle(f => ({ ...f, disponible: e.target.checked }))}
                                  className="rounded"/>
                                <span className="text-xs" style={{ color: '#93c5fd' }}>Disponible</span>
                              </label>
                            </div>
                          </div>
                          <div className="flex gap-2 pt-1">
                            <button onClick={() => handleAddSalle(etage.id)} disabled={addingSalle}
                              className="btn-primary text-sm disabled:opacity-50">
                              {addingSalle ? 'Ajout...' : 'Ajouter la salle'}
                            </button>
                            <button onClick={() => setAddSalleEtageId(null)}
                              className="text-sm px-3 py-2 rounded-lg hover:text-white hover:bg-[#162040] transition-colors"
                              style={{ color: '#3b6cbd' }}>
                              Annuler
                            </button>
                          </div>
                        </div>
                      )}
                      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                        {etage.salles?.map((s) => {
                          const isEditing  = editSalle?.id === s.id
                          const isDeleting = deletingSalleId === s.id
                          return (
                            <div key={s.id}
                              className={`group relative rounded-lg p-2.5 text-xs transition-all duration-500 ${
                                isDeleting ? 'opacity-0 scale-90 bg-red-900/30' : ''
                              }`}
                              style={!isDeleting ? { background: '#0A1020' } : {}}>
                              {isEditing ? (
                                <div className="space-y-1.5">
                                  <input className="input text-xs w-full" placeholder="Nom"
                                    value={editSalle.nom}
                                    onChange={e => setEditSalle(f => ({ ...f, nom: e.target.value }))}
                                    autoFocus/>
                                  <select className="input text-xs w-full" value={editSalle.categorie}
                                    onChange={e => setEditSalle(f => ({ ...f, categorie: e.target.value }))}>
                                    {CATEGORIES.map(c => <option key={c.value} value={c.value}>{c.label}</option>)}
                                  </select>
                                  <div className="flex gap-3">
                                    <label className="flex items-center gap-1.5 cursor-pointer">
                                      <input type="checkbox" checked={editSalle.accessiblePmr}
                                        onChange={e => setEditSalle(f => ({ ...f, accessiblePmr: e.target.checked }))}
                                        className="rounded"/>
                                      <span style={{ color: '#93c5fd' }}>PMR</span>
                                    </label>
                                    <label className="flex items-center gap-1.5 cursor-pointer">
                                      <input type="checkbox" checked={editSalle.disponible ?? true}
                                        onChange={e => setEditSalle(f => ({ ...f, disponible: e.target.checked }))}
                                        className="rounded"/>
                                      <span style={{ color: '#93c5fd' }}>Disponible</span>
                                    </label>
                                  </div>
                                  <div className="flex gap-1 pt-1">
                                    <button onClick={saveEditSalle} disabled={saving}
                                      className="flex-1 bg-blue-600 hover:bg-blue-500 text-white text-xs py-1.5 rounded transition-colors disabled:opacity-50">
                                      {saving ? '...' : '✓ Sauv.'}
                                    </button>
                                    <button onClick={() => setEditSalle(null)}
                                      className="flex-1 text-xs py-1.5 rounded transition-colors"
                                      style={{ background: '#162040', color: '#93c5fd' }}>
                                      Annuler
                                    </button>
                                  </div>
                                </div>
                              ) : (
                                <>
                                  <div className="flex items-center gap-1.5 pr-8">
                                    <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${s.disponible === false ? 'bg-red-500' : 'bg-blue-500'}`}/>
                                    <p className="text-white font-medium truncate">{s.nom}</p>
                                  </div>
                                  <p className="mt-0.5" style={{ color: '#3b6cbd' }}>
                                    {s.categorie === 'SALLE_ETUDE' ? "Salle d'étude" : 'Bureau'}
                                  </p>
                                  {s.accessiblePmr && <img src={pmrIcon} alt="PMR" className="w-3.5 h-3.5 object-contain icon"/>}
                                  <div className="absolute top-1.5 right-1.5 hidden group-hover:flex gap-1">
                                    <button onClick={() => setEditSalle({ ...s })}
                                      className="text-xs leading-none p-0.5 transition-colors"
                                      style={{ color: '#3b6cbd' }}
                                      title="Modifier">✏️</button>
                                    <button onClick={() => handleDeleteSalle(s)}
                                      className="text-red-400 hover:text-red-300 text-xs leading-none p-0.5 transition-colors"
                                      title="Supprimer">✕</button>
                                  </div>
                                </>
                              )}
                            </div>
                          )
                        })}
                      </div>
                    </div>
                  ))
                )}

                {/* Ajouter un étage */}
                {tab === 'structure' && (
                  <div className="mt-4 border-t pt-4" style={{ borderColor: '#1a2845' }}>
                    {!showAddEtage ? (
                      <button
                        onClick={() => { setShowAddEtage(true); setNewEtage({ ...EMPTY_NEW_ETAGE, numero: (selected.etages?.length || 0) }) }}
                        className="text-xs text-blue-400 hover:text-blue-300 px-3 py-1.5 rounded hover:bg-blue-900/20 transition-colors">
                        + Ajouter un étage
                      </button>
                    ) : (
                      <div className="rounded-xl p-4 space-y-3 border"
                        style={{ background: '#0A1020', borderColor: '#1a2845' }}>
                        <p className="text-xs font-medium" style={{ color: '#93c5fd' }}>Nouvel étage — {selected.nom}</p>
                        <div className="grid grid-cols-2 gap-2">
                          <div>
                            <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Numéro</label>
                            <input type="number" min="0" className="input text-sm"
                              value={newEtage.numero}
                              onChange={e => setNewEtage(f => ({ ...f, numero: parseInt(e.target.value) || 0 }))}/>
                          </div>
                          <div>
                            <label className="block text-xs mb-1" style={{ color: '#3b6cbd' }}>Label *</label>
                            <input className="input text-sm" placeholder="ex: 2e étage" autoFocus
                              value={newEtage.label}
                              onChange={e => setNewEtage(f => ({ ...f, label: e.target.value }))}
                              onKeyDown={e => e.key === 'Enter' && handleAddEtage()}/>
                          </div>
                          <label className="col-span-2 flex items-center gap-2 cursor-pointer mt-1">
                            <input type="checkbox" checked={newEtage.accessiblePmr}
                              onChange={e => setNewEtage(f => ({ ...f, accessiblePmr: e.target.checked }))}
                              className="rounded"/>
                            <span className="text-xs" style={{ color: '#93c5fd' }}>Accessible PMR</span>
                          </label>
                        </div>
                        <div className="flex gap-2 pt-1">
                          <button onClick={handleAddEtage} disabled={addingEtage}
                            className="btn-primary text-sm disabled:opacity-50">
                            {addingEtage ? 'Ajout...' : 'Ajouter l\'étage'}
                          </button>
                          <button onClick={() => setShowAddEtage(false)}
                            className="text-sm px-3 py-2 rounded-lg hover:text-white hover:bg-[#162040] transition-colors"
                            style={{ color: '#3b6cbd' }}>
                            Annuler
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* WiFi tab */}
                {tab === 'wifi' && renderWifi()}
              </div>
            )}
          </div>
        </div>}
      </div>

      {/* Wizard modal */}
      {wiz && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="rounded-2xl w-full max-w-lg shadow-2xl"
            style={{ background: '#0D1530', border: '1px solid #1a2845' }}>
            <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b"
              style={{ borderColor: '#1a2845' }}>
              <h2 className="text-white font-bold">Créer un bloc</h2>
              {!saving && (
                <button onClick={() => setWiz(null)}
                  className="hover:text-white w-7 h-7 flex items-center justify-center rounded-lg hover:bg-[#162040] transition-colors"
                  style={{ color: '#3b6cbd' }}>
                  ✕
                </button>
              )}
            </div>
            <div className="px-6 py-5">
              {renderWiz()}
            </div>
          </div>
        </div>
      )}
    </Layout>
  )
}
