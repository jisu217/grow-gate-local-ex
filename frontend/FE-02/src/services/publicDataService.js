import { apiFetch } from './apiClient.js'

export function getPublicDataDashboard() {
  return apiFetch('/public-data/dashboard')
}
