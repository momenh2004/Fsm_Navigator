import Spinner from '../ui/Spinner'

// All shades stay in the blue/indigo/cyan family
const palette = {
  blue:   { label: '#60a5fa', iconBg: 'rgba(59,130,246,0.15)',  bar: '#3b82f6' },
  green:  { label: '#38bdf8', iconBg: 'rgba(14,165,233,0.15)',  bar: '#0ea5e9' },
  purple: { label: '#818cf8', iconBg: 'rgba(99,102,241,0.15)',  bar: '#6366f1' },
  amber:  { label: '#93c5fd', iconBg: 'rgba(147,197,253,0.12)', bar: '#60a5fa' },
  cyan:   { label: '#22d3ee', iconBg: 'rgba(6,182,212,0.15)',   bar: '#06b6d4' },
  red:    { label: '#a78bfa', iconBg: 'rgba(139,92,246,0.15)',  bar: '#8b5cf6' },
}

export default function KpiCard({ icon, label, value, sub, color = 'blue', loading, progress }) {
  const p = palette[color] || palette.blue

  return (
    <div className="rounded-xl p-5 flex flex-col gap-2"
      style={{ background: '#0D1530', border: '1px solid #1a2845', transition: 'border-color 0.2s ease, box-shadow 0.2s ease' }}
      onMouseEnter={e => { e.currentTarget.style.borderColor = 'rgba(255,255,255,0.2)'; e.currentTarget.style.boxShadow = '0 0 0 1px rgba(255,255,255,0.05)' }}
      onMouseLeave={e => { e.currentTarget.style.borderColor = '#1a2845'; e.currentTarget.style.boxShadow = '' }}>

      <div className="flex items-start justify-between">
        <span className="text-xs font-semibold tracking-widest uppercase"
          style={{ color: p.label }}>{label}</span>
        <div className="w-9 h-9 rounded-lg flex items-center justify-center"
          style={{ background: p.iconBg }}>
          <img src={icon} alt="" className="w-5 h-5 object-contain icon"/>
        </div>
      </div>

      {loading
        ? <div className="h-10 rounded-lg animate-pulse w-28 mt-1" style={{ background: '#162040' }} />
        : <p className="text-4xl font-bold text-white leading-none mt-1">{value ?? '—'}</p>
      }

      {sub && <p className="text-xs" style={{ color: '#3b6cbd' }}>{sub}</p>}

      {progress !== undefined && (
        <div className="h-[3px] rounded-full mt-1" style={{ background: '#1a2845' }}>
          <div className="h-[3px] rounded-full transition-all duration-500"
            style={{ width: `${Math.min(progress, 100)}%`, background: p.bar }} />
        </div>
      )}
    </div>
  )
}
