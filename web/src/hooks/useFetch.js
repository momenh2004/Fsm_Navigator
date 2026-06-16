import { useState, useEffect, useCallback } from 'react'

export function useFetch(fetchFn, deps = []) {
  const [data, setData]       = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetchFn()
      setData(res.data)
    } catch (e) {
      setError(e?.response?.data?.message || 'Erreur serveur')
    } finally {
      setLoading(false)
    }
  }, deps)

  useEffect(() => { load() }, [load])

  return { data, loading, error, refetch: load }
}
