import { useRef } from 'react'

// Source: FsmMapView.java — canvas 1024×1024 → converted to %
export const POSITIONS = {
  'BP2':   { x: 84.96, y: 67.48 },
  'BP1':   { x: 60.94, y: 58.59 },
  'BM':    { x: 26.86, y: 61.43 },
  'B4':    { x: 92.28, y: 22.46 },
  'A1-6':  { x: 42.19, y: 57.23 },
  'B2':    { x: 42.19, y: 47.85 },
  'PCOUR': { x: 46.09, y: 86.04 },
  'B1':    { x: 74.71, y: 84.77 },
  'BC':    { x: 19.43, y: 71.48 },
  'BC2':   { x: 81.74, y: 48.34 },
  'BIB':   { x: 27.05, y: 71.29 },
  'BC1':   { x: 60.84, y: 49.32 },
  'ADM':   { x: 33.30, y: 86.33 },
  'INF':   { x: 16.50, y: 89.06 },
  'B3':    { x: 78.81, y: 26.46 },
  'STH':   { x: 9.18,  y: 80.47 },
  'COUR':  { x: 42.77, y: 71.00 },
  'D2':    { x: 7.62,  y: 74.22 },
  'D1':    { x: 7.42,  y: 71.88 },
}

function Marker({ label, isSelected, isNew, onClick, style }) {
  const base = isNew
    ? 'bg-emerald-400 text-[#060A14]'
    : isSelected
      ? 'bg-cyan-400 text-[#060A14]'
      : 'bg-blue-600 text-white group-hover:bg-blue-400'

  const dot = isNew
    ? 'bg-emerald-400'
    : isSelected
      ? 'bg-cyan-400'
      : 'bg-blue-500 group-hover:bg-blue-400'

  const stem = isNew ? 'bg-emerald-400' : isSelected ? 'bg-cyan-400' : 'bg-blue-400'

  return (
    <div
      className={`absolute flex flex-col items-center ${onClick ? 'cursor-pointer group' : 'pointer-events-none'}`}
      style={{ transform: 'translate(-50%, -100%)', ...style }}
      onClick={onClick}>
      <div className={`text-xs font-bold px-2 py-0.5 rounded-full shadow-lg whitespace-nowrap transition-all ${base}`}>
        {label}
      </div>
      <div className={`w-px h-2 ${stem}`} />
      <div className="relative flex items-center justify-center">
        <div className={`w-3 h-3 rounded-full border-2 border-white shadow-lg transition-all ${dot}`} />
        {isNew && (
          <div className="absolute w-3 h-3 rounded-full bg-emerald-400 animate-ping opacity-75" />
        )}
      </div>
    </div>
  )
}

export default function CampusMap({ blocs = [], extraPositions = {}, selected, onSelect, newPosition, newLabel }) {
  const imgRef = useRef()
  const allPositions = { ...extraPositions, ...POSITIONS }
  const positioned = blocs.filter(b => allPositions[b.code])
  const unpositioned = blocs.filter(b => !allPositions[b.code])

  return (
    <div className="rounded-2xl overflow-hidden" style={{ border: '1px solid #1a2845', background: '#0D1530' }}>

      {/* Map */}
      <div className="relative select-none">
        <img
          ref={imgRef}
          src="/campus.png"
          alt="Campus FSM"
          className="w-full block"
          draggable={false}
        />

        {/* Existing bloc markers */}
        {positioned.map(bloc => {
          const pos = allPositions[bloc.code]
          const isSelected = selected?.id === bloc.id
          return (
            <Marker
              key={bloc.id}
              label={bloc.code}
              isSelected={isSelected}
              onClick={e => { e.stopPropagation(); onSelect?.(bloc) }}
              style={{ left: `${pos.x}%`, top: `${pos.y}%`, zIndex: isSelected ? 20 : 10 }}
            />
          )
        })}

        {/* New bloc position (wizard) */}
        {newPosition && (
          <Marker
            label={newLabel || 'Nouveau bloc'}
            isNew
            style={{ left: `${newPosition.x}%`, top: `${newPosition.y}%`, zIndex: 30 }}
          />
        )}
      </div>

      {/* Footer */}
      <div className="flex flex-wrap items-center gap-x-5 gap-y-1 px-4 py-2.5 border-t text-xs"
        style={{ borderColor: '#1a2845', color: '#3b6cbd' }}>

        <div className="flex items-center gap-1.5">
          <div className="w-2.5 h-2.5 rounded-full bg-blue-500 border border-white/40 flex-shrink-0"/>
          <span>Bloc existant</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="w-2.5 h-2.5 rounded-full bg-cyan-400 border border-white/40 flex-shrink-0"/>
          <span>Sélectionné</span>
        </div>
        {newPosition && (
          <div className="flex items-center gap-1.5">
            <div className="w-2.5 h-2.5 rounded-full bg-emerald-400 border border-white/40 flex-shrink-0"/>
            <span>Nouvelle position</span>
          </div>
        )}

        <span className="ml-auto">
          {positioned.length}/{blocs.length} blocs positionnés
          {unpositioned.length > 0 && (
            <span className="ml-2" style={{ color: '#2d4a7a' }}>
              · sans position : {unpositioned.map(b => b.code).join(', ')}
            </span>
          )}
        </span>
      </div>
    </div>
  )
}
