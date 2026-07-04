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

export function getLatestPublicDataProofRun() {
  return apiFetch('/public-data/proof-runs/latest')
}

export function createPublicDataProofRun(triggerType = 'manual') {
  return apiFetch(`/admin/public-data/proof-runs?triggerType=${encodeURIComponent(triggerType)}`, {
    method: 'POST',
  })
}
