import { useState } from 'react'
import Layout from '../components/layout/Layout'
import { useFetch } from '../hooks/useFetch'
import { statsAPI } from '../services/api'
import Spinner from '../components/ui/Spinner'
import { wifiIcon, pmrIcon, checkIcon, dangerIcon, interditIcon } from '../assets/icons'

export default function Coverage() {
  const { data: wifi,      loading: l1 } = useFetch(statsAPI.wifiCoverage)
  const { data: uncovered, loading: l2 } = useFetch(statsAPI.uncovered)
  const { data: pmr,       loading: l3 } = useFetch(statsAPI.pmrCoverage)

  const [tab, setTab] = useState('wifi')

  // Global PMR stats
  const pmrTotalSalles = pmr?.reduce((s, b) => s + b.total, 0) || 0
  const pmrOk          = pmr?.reduce((s, b) => s + b.pmr,   0) || 0
  const pmrGlobalPct   = pmrTotalSalles > 0 ? Math.round(pmrOk / pmrTotalSalles * 100) : 0

  return (
    <Layout>
      <div className="max-w-6xl mx-auto space-y-6">

        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-white">Couverture</h1>
          <p className="text-sm mt-0.5" style={{ color: '#3b6cbd' }}>
            Analyse de la couverture WiFi et de l'accessibilité PMR
          </p>
        </div>

        {/* Tabs */}
        <div className="flex gap-0 border-b" style={{ borderColor: '#1a2845' }}>
          {[
            ['wifi', 'Couverture WiFi',   wifiIcon],
            ['pmr',  'Accessibilité PMR', pmrIcon],
          ].map(([key, label, icon]) => (
            <button key={key} onClick={() => setTab(key)}
              className={`flex items-center gap-2 px-5 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
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

        {/* ── WiFi tab ── */}
        {tab === 'wifi' && (
          <div className="space-y-5">

            {/* Fingerprints par bloc */}
            <div className="card">
              <h2 className="text-white font-semibold mb-4 text-sm flex items-center gap-2">
                <img src={wifiIcon} alt="" className="w-4 h-4 object-contain icon"/> Fingerprints par bloc
              </h2>
              {l1 ? <div className="flex justify-center py-10"><Spinner/></div> : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left" style={{ color: '#3b6cbd', borderBottom: '1px solid #1a2845' }}>
                      <th className="pb-3 pr-4">Bloc</th>
                      <th className="pb-3 pr-4">Nom</th>
                      <th className="pb-3 pr-4">Fingerprints</th>
                      <th className="pb-3 pr-4">Progression</th>
                      <th className="pb-3">Statut</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#1a2845]">
                    {wifi?.map((b, i) => {
                      const pct = Math.min(100, Math.round((b.fingerprints / 20) * 100))
                      const ok  = b.fingerprints >= 10
                      return (
                        <tr key={i} style={{ color: '#93c5fd' }}>
                          <td className="py-3 pr-4 font-mono text-blue-400">{b.blocCode}</td>
                          <td className="py-3 pr-4">{b.blocNom}</td>
                          <td className="py-3 pr-4 font-semibold">{b.fingerprints}</td>
                          <td className="py-3 pr-4 w-40">
                            <div className="h-2 rounded-full" style={{ background: '#1a2845' }}>
                              <div className={`h-2 rounded-full ${ok ? 'bg-blue-500' : 'bg-indigo-500'}`}
                                style={{ width: `${pct}%` }}/>
                            </div>
                            <p className="text-xs mt-0.5" style={{ color: '#3b6cbd' }}>{pct}%</p>
                          </td>
                          <td className="py-3">
                            {ok
                              ? <span className="badge-green flex items-center gap-1"><img src={checkIcon} alt="" className="w-3 h-3 object-contain icon"/> Couvert</span>
                              : <span className="badge-yellow flex items-center gap-1"><img src={dangerIcon} alt="" className="w-3 h-3 object-contain icon"/> Insuffisant</span>
                            }
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              )}
            </div>

            {/* Salles non couvertes */}
            <div className="card">
              <h2 className="text-white font-semibold mb-4 text-sm flex items-center gap-2">
                <img src={dangerIcon} alt="" className="w-4 h-4 object-contain icon"/> Salles sans fingerprint
              </h2>
              {l2 ? <div className="flex justify-center py-8"><Spinner/></div> : (
                uncovered?.length === 0
                  ? <p className="text-sm flex items-center gap-2" style={{ color: '#60a5fa' }}><img src={checkIcon} alt="" className="w-4 h-4 object-contain icon"/> Toutes les salles sont couvertes !</p>
                  : (
                    <div className="space-y-2 max-h-64 overflow-y-auto">
                      {uncovered?.map((s, i) => (
                        <div key={i} className="flex items-center justify-between p-2.5 rounded-lg"
                          style={{ background: '#0A1020' }}>
                          <div>
                            <p className="text-white text-sm font-medium">{s.salleNom}</p>
                            <p className="text-xs" style={{ color: '#3b6cbd' }}>{s.bloc} — {s.etage}</p>
                          </div>
                          <span className="badge-red">Non couvert</span>
                        </div>
                      ))}
                    </div>
                  )
              )}
            </div>
          </div>
        )}

        {/* ── PMR tab ── */}
        {tab === 'pmr' && (
          <div className="space-y-5">

            {/* Résumé global */}
            {l3
              ? <div className="card flex justify-center py-8"><Spinner/></div>
              : (
              <div className="card">
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <h2 className="text-white font-semibold text-sm">Accessibilité PMR globale</h2>
                    <p className="text-xs mt-0.5" style={{ color: '#3b6cbd' }}>
                      {pmrOk} salles PMR sur {pmrTotalSalles} au total
                    </p>
                  </div>
                  <span className="text-4xl font-bold text-white">{pmrGlobalPct}%</span>
                </div>

                {/* Barre globale */}
                <div className="h-3 rounded-full mb-2" style={{ background: '#1a2845' }}>
                  <div
                    className={`h-3 rounded-full transition-all duration-700 ${
                      pmrGlobalPct >= 70 ? 'bg-blue-500' :
                      pmrGlobalPct >= 40 ? 'bg-indigo-500' : 'bg-violet-500'
                    }`}
                    style={{ width: `${pmrGlobalPct}%` }}
                  />
                </div>

                <div className="flex justify-between text-xs mt-1" style={{ color: '#3b6cbd' }}>
                  <span>0%</span>
                  <span className={`flex items-center gap-1 ${pmrGlobalPct >= 70 ? 'text-blue-400' : pmrGlobalPct >= 40 ? 'text-indigo-400' : 'text-violet-400'}`}>
                    <img src={pmrGlobalPct >= 70 ? checkIcon : pmrGlobalPct >= 40 ? dangerIcon : interditIcon} alt="" className="w-3 h-3 object-contain icon"/>
                    {pmrGlobalPct >= 70 ? 'Bonne accessibilité' :
                     pmrGlobalPct >= 40 ? 'Accessibilité partielle' : 'Accessibilité insuffisante'}
                  </span>
                  <span>100%</span>
                </div>
              </div>
            )}

            {/* Détail par bloc */}
            <div className="card">
              <h2 className="text-white font-semibold mb-4 text-sm">Détail par bloc</h2>
              {l3 ? <div className="flex justify-center py-8"><Spinner/></div> : (
                <div className="space-y-4">
                  {pmr?.map((b, i) => (
                    <div key={i}>
                      <div className="flex items-center justify-between mb-1.5">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-sm text-blue-400">{b.blocCode}</span>
                          <span className="text-xs" style={{ color: '#3b6cbd' }}>
                            {b.pmr} / {b.total} salles accessibles PMR
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-sm text-white">{b.pct}%</span>
                          {b.pct >= 70
                            ? <span className="badge-green">Bon</span>
                            : b.pct >= 40
                              ? <span className="badge-blue">Partiel</span>
                              : <span className="badge-red">Insuffisant</span>
                          }
                        </div>
                      </div>
                      <div className="h-2 rounded-full" style={{ background: '#1a2845' }}>
                        <div
                          className={`h-2 rounded-full transition-all duration-500 ${
                            b.pct >= 70 ? 'bg-blue-500' :
                            b.pct >= 40 ? 'bg-indigo-500' : 'bg-violet-500'
                          }`}
                          style={{ width: `${b.pct}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

          </div>
        )}

      </div>
    </Layout>
  )
}
