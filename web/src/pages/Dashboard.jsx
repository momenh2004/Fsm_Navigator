import { useFetch } from '../hooks/useFetch'
import { statsAPI } from '../services/api'
import KpiCard from '../components/cards/KpiCard'
import Layout from '../components/layout/Layout'
import Spinner from '../components/ui/Spinner'
import { usersIcon, navIcon, wifiIcon, interditIcon, pmrIcon, structureIcon, mapIcon, tropheyIcon, mailboxIcon } from '../assets/icons'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  LineChart, Line, Cell,
} from 'recharts'

const RSSI_COLORS = {
  impossible: '#a78bfa',
  instable:   '#818cf8',
  correct:    '#3b82f6',
  optimal:    '#0ea5e9',
  fort:       '#6366f1',
}

const RSSI_LABELS = {
  '< -80':  { main: 'Nav. impossible', sub: '< -80 dBm' },
  '-80~-70': { main: 'Nav. instable',  sub: '-80 ~ -70' },
  '-70~-60': { main: 'Nav. correcte',  sub: '-70 ~ -60' },
  '-60~-50': { main: 'Nav. optimale',  sub: '-60 ~ -50' },
  '> -50':  { main: 'Signal fort',     sub: '> -50 dBm' },
}

const rssiLabel = (raw) => RSSI_LABELS[raw] || { main: raw, sub: '' }

const rssiBarColor = (label) => {
  if (label === '-60~-50') return RSSI_COLORS.optimal
  if (label === '-70~-60') return RSSI_COLORS.correct
  if (label === '-80~-70') return RSSI_COLORS.instable
  if (label === '> -50')   return RSSI_COLORS.fort
  return RSSI_COLORS.impossible
}

const RssiXTick = ({ x, y, payload }) => {
  const { main, sub } = rssiLabel(payload.value)
  return (
    <g transform={`translate(${x},${y})`}>
      <text x={0} y={0} dy={13} textAnchor="middle" fill="#93c5fd" fontSize={9.5} fontWeight="500">{main}</text>
      <text x={0} y={0} dy={25} textAnchor="middle" fill="#3b6cbd" fontSize={8.5}>{sub}</text>
    </g>
  )
}

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  const { main, sub } = rssiLabel(label)
  return (
    <div className="rounded-lg p-3 text-sm" style={{ background: '#0D1530', border: '1px solid #1a2845' }}>
      <p className="font-medium mb-0.5" style={{ color: '#93c5fd' }}>{main}</p>
      <p className="text-xs mb-1" style={{ color: '#3b6cbd' }}>{sub}</p>
      {payload.map((p, i) => (
        <p key={i} style={{ color: p.color }}>{p.name}: <strong>{p.value}</strong></p>
      ))}
    </div>
  )
}

function ChartEmpty({ message = 'Aucune donnée disponible' }) {
  return (
    <div className="h-48 flex flex-col items-center justify-center text-sm gap-2"
      style={{ color: '#3b6cbd' }}>
      <img src={mailboxIcon} alt="" className="w-10 h-10 object-contain icon"/>
      {message}
    </div>
  )
}

function DistribCard({ icon, value, label, color }) {
  const iconColors = {
    green:  { text: '#0ea5e9', bg: 'rgba(14,165,233,0.15)'  },
    blue:   { text: '#60a5fa', bg: 'rgba(59,130,246,0.15)'  },
    amber:  { text: '#818cf8', bg: 'rgba(99,102,241,0.15)'  },
    red:    { text: '#22d3ee', bg: 'rgba(6,182,212,0.15)'   },
  }
  const c = iconColors[color] || iconColors.blue
  return (
    <div className="rounded-xl p-4 flex flex-col items-center gap-2 flex-1"
      style={{ background: '#0A1020', border: '1px solid #1a2845' }}>
      <div className="w-10 h-10 rounded-full flex items-center justify-center text-xl"
        style={{ color: c.text, background: c.bg }}>
        {icon}
      </div>
      <p className="text-3xl font-bold text-white">{value ?? '—'}</p>
      <p className="text-xs text-center" style={{ color: '#3b6cbd' }}>{label}</p>
    </div>
  )
}

export default function Dashboard() {
  const { data: overview,  loading: l1 } = useFetch(statsAPI.overview)
  const { data: activity,  loading: l2 } = useFetch(statsAPI.activity)
  const { data: topNav,    loading: l3 } = useFetch(statsAPI.topNavigated)
  const { data: rssi,      loading: l4 } = useFetch(statsAPI.rssi)
  const { data: wifi,      loading: l5 } = useFetch(statsAPI.wifiCoverage)
  const { data: uncovered, loading: l6 } = useFetch(statsAPI.uncovered)
  const { data: pmr,       loading: l7 } = useFetch(statsAPI.pmrCoverage)

  const rssiData = rssi ? rssi.labels.map((l, i) => ({
    label: l, value: rssi.values[i],
    fill: rssiBarColor(l),
  })) : []

  const activityData = activity?.map(d => ({ day: d.day?.slice(5), count: d.count })) || []
  const topNavData   = topNav?.slice(0, 5).map(d => ({
    name: d.salleNom?.replace('Salle ', ''), count: d.count,
  })) || []

  // Navigations cette semaine
  const nav7j = activityData.reduce((s, d) => s + (d.count || 0), 0)

  // Qualité WiFi — % fingerprints en zone correcte + optimale
  const rssiTotal   = rssi ? rssi.values.reduce((s, v) => s + v, 0) : 0
  const rssiGood    = rssi ? rssi.values[2] + rssi.values[3] : 0
  const wifiQuality = rssiTotal > 0 ? Math.round(rssiGood / rssiTotal * 100) : null

  // Accessibilité PMR — % global toutes salles PMR / total
  const pmrTotalSalles = pmr?.reduce((s, b) => s + b.total, 0) || 0
  const pmrOk          = pmr?.reduce((s, b) => s + b.pmr,   0) || 0
  const pmrPct         = pmrTotalSalles > 0 ? Math.round(pmrOk / pmrTotalSalles * 100) : null

  // WiFi coverage distribution
  const coverageDistrib = wifi ? {
    optimal:    wifi.filter(b => b.fingerprints >= 20).length,
    sufficient: wifi.filter(b => b.fingerprints >= 10 && b.fingerprints < 20).length,
    weak:       wifi.filter(b => b.fingerprints >= 5  && b.fingerprints < 10).length,
    uncovered:  wifi.filter(b => b.fingerprints < 5).length,
  } : null

  return (
    <Layout>
      <div className="max-w-7xl mx-auto space-y-6">

        {/* ── Header ─────────────────────────────────────────── */}
        <div>
          <h1 className="text-2xl font-bold text-white">Tableau de bord</h1>
          <p className="text-sm mt-1" style={{ color: '#3b6cbd' }}>Surveillance de l'activité FSM Navigator</p>
        </div>

        {/* ── Row 1 — 4 KPI cards ────────────────────────────── */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiCard icon={usersIcon}   label="Utilisateurs"       color="blue"   loading={l1}
            value={overview?.totalUsers}
            sub="Inscrits sur l'application"/>
          <KpiCard icon={navIcon}     label="Navigations (7j)"   color="green"  loading={l2}
            value={l2 ? null : nav7j}
            sub="Itinéraires calculés cette semaine"/>
          <KpiCard icon={wifiIcon}    label="Fingerprints"       color="purple" loading={l1}
            value={overview?.totalFingerprints}
            sub="Empreintes WiFi collectées"/>
          <KpiCard icon={interditIcon} label="Zones aveugles"    color="red"    loading={l6}
            value={l6 ? null : (uncovered?.length ?? '—')}
            sub={uncovered?.length === 0 ? "Toutes les salles sont couvertes" : "Salles sans fingerprint"}/>
        </div>

        {/* ── Row 2 — 4 KPI cards ────────────────────────────── */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiCard icon={wifiIcon}      label="Qualité WiFi"       color="cyan"   loading={l4}
            value={wifiQuality !== null ? `${wifiQuality}%` : '—'}
            sub="Fingerprints en zone correcte / optimale"
            progress={wifiQuality ?? undefined}/>
          <KpiCard icon={pmrIcon}       label="Accessibilité PMR"  color="amber"  loading={l7}
            value={pmrPct !== null ? `${pmrPct}%` : '—'}
            sub="Salles accessibles PMR"
            progress={pmrPct ?? undefined}/>
          <KpiCard icon={structureIcon} label="Blocs"              color="green"  loading={l1}
            value={overview?.totalBlocs}
            sub="Bâtiments couverts"/>
          <KpiCard icon={mapIcon}       label="Couverture"         color="purple" loading={l5}
            value={wifi ? `${wifi.filter(b => b.fingerprints >= 10).length}/${wifi.length}` : '—'}
            sub="Blocs bien couverts (≥10 fp)"
            progress={wifi ? (wifi.filter(b => b.fingerprints >= 10).length / Math.max(wifi.length, 1)) * 100 : undefined}/>
        </div>


        {/* ── Charts row 1 ───────────────────────────────────── */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">

          <div className="card">
            <h3 className="text-white font-semibold mb-1 flex items-center gap-2 text-sm">
              <img src={navIcon} alt="" className="w-4 h-4 object-contain icon"/> Navigations — 7 derniers jours
            </h3>
            <p className="text-xs mb-4" style={{ color: '#3b6cbd' }}>Itinéraires calculés par jour</p>
            {l2
              ? <div className="h-48 flex items-center justify-center"><Spinner/></div>
              : !activityData.length
                ? <ChartEmpty message="Aucune navigation ces 7 derniers jours"/>
                : (
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={activityData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1a2845"/>
                    <XAxis dataKey="day" stroke="#2d4a7a" tick={{ fontSize: 11 }}/>
                    <YAxis stroke="#2d4a7a" tick={{ fontSize: 11 }}/>
                    <Tooltip content={<CustomTooltip/>}/>
                    <Line type="monotone" dataKey="count" stroke="#3b82f6" strokeWidth={2}
                      dot={{ fill: '#3b82f6', r: 3 }} name="Activités"/>
                  </LineChart>
                </ResponsiveContainer>
              )
            }
          </div>

          <div className="card">
            <h3 className="text-white font-semibold mb-1 flex items-center gap-2 text-sm">
              <img src={wifiIcon} alt="" className="w-4 h-4 object-contain icon"/> Distribution RSSI
            </h3>
            <p className="text-xs mb-4" style={{ color: '#3b6cbd' }}>Qualité des fingerprints WiFi · Impact direct sur la précision de navigation</p>
            {l4
              ? <div className="h-48 flex items-center justify-center"><Spinner/></div>
              : rssiData.every(d => d.value === 0)
                ? <ChartEmpty message="Aucun fingerprint enregistré"/>
                : (
                <ResponsiveContainer width="100%" height={230}>
                  <BarChart data={rssiData} margin={{ bottom: 10 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1a2845"/>
                    <XAxis dataKey="label" stroke="#2d4a7a" tick={<RssiXTick/>} interval={0} height={45}/>
                    <YAxis stroke="#2d4a7a" tick={{ fontSize: 11 }}/>
                    <Tooltip content={<CustomTooltip/>}/>
                    <Bar dataKey="value" name="Fingerprints" radius={[4, 4, 0, 0]}>
                      {rssiData.map((e, i) => <Cell key={i} fill={e.fill}/>)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )
            }
          </div>
        </div>

        {/* ── Charts row 2 ───────────────────────────────────── */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">

          <div className="card">
            <h3 className="text-white font-semibold mb-1 flex items-center gap-2 text-sm">
              <img src={tropheyIcon} alt="" className="w-4 h-4 object-contain icon"/> Salles les plus naviguées
            </h3>
            <p className="text-xs mb-4" style={{ color: '#3b6cbd' }}>Top 5 destinations</p>
            {l3
              ? <div className="h-48 flex items-center justify-center"><Spinner/></div>
              : !topNavData.length
                ? <ChartEmpty message="Aucune navigation enregistrée"/>
                : (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={topNavData} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" stroke="#1a2845"/>
                    <XAxis type="number" stroke="#2d4a7a" tick={{ fontSize: 11 }}/>
                    <YAxis dataKey="name" type="category" stroke="#2d4a7a" tick={{ fontSize: 11 }} width={60}/>
                    <Tooltip content={<CustomTooltip/>}/>
                    <Bar dataKey="count" fill="#0ea5e9" name="Navigations" radius={[0, 4, 4, 0]}/>
                  </BarChart>
                </ResponsiveContainer>
              )
            }
          </div>

          <div className="card">
            <h3 className="text-white font-semibold mb-1 flex items-center gap-2 text-sm">
              <img src={wifiIcon} alt="" className="w-4 h-4 object-contain icon"/> Couverture WiFi par bloc
            </h3>
            <p className="text-blue-900/60 text-xs mb-4">Fingerprints collectés par bâtiment</p>
            {l5
              ? <div className="h-48 flex items-center justify-center"><Spinner/></div>
              : (
              <div className="space-y-2.5 max-h-52 overflow-y-auto pr-1">
                {wifi?.map((b, i) => {
                  const pct = Math.min(100, Math.round((b.fingerprints / 20) * 100))
                  const ok  = b.fingerprints >= 10
                  return (
                    <div key={i}>
                      <div className="flex justify-between text-xs mb-1.5">
                        <span style={{ color: '#93c5fd' }}>{b.blocCode} — {b.blocNom}</span>
                        <span className={ok ? 'text-blue-400' : 'text-indigo-400'}>
                          {b.fingerprints} fp
                        </span>
                      </div>
                      <div className="h-1 rounded-full" style={{ background: '#1a2845' }}>
                        <div
                          className={`h-1 rounded-full transition-all ${ok ? 'bg-blue-500' : 'bg-indigo-500'}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

        </div>
      </div>
    </Layout>
  )
}
