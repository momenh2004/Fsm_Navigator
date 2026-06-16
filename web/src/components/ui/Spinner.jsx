export default function Spinner({ size = 'md' }) {
  const s = size === 'sm' ? 'w-4 h-4' : size === 'lg' ? 'w-10 h-10' : 'w-7 h-7'
  return (
    <div className={`${s} border-2 border-slate-600 border-t-blue-500 rounded-full animate-spin`}/>
  )
}
