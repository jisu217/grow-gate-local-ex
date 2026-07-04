import { apiFetch } from './apiClient.js'

export function getPublicDataDashboard() {
  return apiFetch('/public-data/dashboard')
}

export function getPublicDataRuns() {
  return apiFetch('/public-data/runs')
}

export function getPublicDataRecords() {
  return apiFetch('/public-data/records')
}

export function collectPublicData(payload) {
  return apiFetch('/public-data/collect', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function refreshDailyPublicData() {
  return apiFetch('/public-data/daily-refresh', {
    method: 'POST',
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
