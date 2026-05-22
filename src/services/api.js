import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('fsm_token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

api.interceptors.response.use(
  r => r,
  err => {
    if (err.response?.status === 401) {
      const path = window.location.pathname
      // Don't redirect if already on login/otp — let the page handle the error itself
      if (path !== '/login' && path !== '/otp') {
        localStorage.removeItem('fsm_token')
        localStorage.removeItem('fsm_admin')
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export const authAPI = {
  login:      (email, password) => api.post('/auth/login', { email, password }),
  verifyOtp:  (email, otp)      => api.post('/auth/verify-otp', { email, otp }),
  logout:     ()                => api.post('/auth/logout'),
}

export const statsAPI = {
  overview:       ()           => api.get('/admin/stats/overview'),
  wifiCoverage:   ()           => api.get('/admin/stats/wifi-coverage'),
  rssi:           ()           => api.get('/admin/stats/rssi-distribution'),
  salleTypes:     ()           => api.get('/admin/stats/salle-types'),
  topNavigated:   (days = 30)  => api.get('/admin/stats/top-navigated', { params: { days } }),
  topViewed:      (days = 30)  => api.get('/admin/stats/top-viewed',    { params: { days } }),
  activity:       (days = 7)   => api.get('/admin/stats/activity',      { params: { days } }),
  topUsers:       (days = 30)  => api.get('/admin/stats/top-users',     { params: { days } }),
  uncovered:      ()           => api.get('/admin/stats/uncovered-salles'),
  pmrCoverage:    ()           => api.get('/admin/stats/pmr-coverage'),
}

export const usersAPI = {
  list:   ()       => api.get('/admin/users'),
  create: (data)   => api.post('/admin/users', data),
  delete: (id)     => api.delete(`/admin/users/${id}`),
}

export const blocsAPI = {
  list:               ()        => api.get('/blocs'),
  get:                (id)      => api.get(`/blocs/${id}`),
  create:             (data)    => api.post('/admin/blocs', data),
  createEtage:        (data)    => api.post('/admin/etages', data),
  createSalle:        (data)    => api.post('/admin/salles', data),
  updateBloc:         (id, d)   => api.put(`/admin/blocs/${id}`, d),
  updateSalle:        (id, d)   => api.put(`/admin/salles/${id}`, d),
  deleteBloc:         (id)      => api.delete(`/admin/blocs/${id}`),
  deleteSalle:        (id)      => api.delete(`/admin/salles/${id}`),
  getPoiByBloc:           (id)  => api.get(`/admin/poi/bloc/${id}`),
  getFingerprintsByBloc:  (id)  => api.get(`/admin/fingerprints/bloc/${id}`),
  createFingerprint:  (data)    => api.post('/admin/fingerprints', data),
  deleteFingerprint:  (id)      => api.delete(`/admin/fingerprints/${id}`),
  exportBloc:         (id)      => api.get(`/admin/blocs/${id}/export`),
  importBloc:         (data)    => api.post('/admin/blocs/import', data),
  createPoi:          (data)    => api.post('/admin/poi', data),
}

export default api
