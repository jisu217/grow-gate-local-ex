import { apiFetch } from './apiClient.js'

export function getPublicDataDashboard() {
  return apiFetch('/public-data/dashboard')
}

export function collectPublicData(payload) {
  return apiFetch('/public-data/collect', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
