import assert from 'node:assert/strict'
import { buildReportArchive } from './mypageService.js'

const diagnoses = [
  {
    id: 'diag-new',
    productId: 'prod-1',
    productName: '배터리팩',
    targetMarket: 'DE',
    status: 'done',
    requestedAt: '2026-07-04T01:20:00Z',
    diagnosisScore: 82,
  },
  {
    id: 'diag-running',
    productId: 'prod-1',
    productName: '배터리팩',
    targetMarket: 'DE',
    status: 'running',
    requestedAt: '2026-07-04T02:00:00Z',
    diagnosisScore: null,
  },
  {
    id: 'diag-other',
    productId: 'prod-2',
    productName: '센서',
    targetMarket: 'JP',
    status: 'done',
    requestedAt: '2026-07-03T00:00:00Z',
    diagnosisScore: 75,
  },
]

const archive = buildReportArchive(diagnoses, 'prod-1')

assert.equal(archive.length, 1)
assert.equal(archive[0].diagnosisId, 'diag-new')
assert.equal(archive[0].file, 'grow-gate-diagnosis-diag-new.md')
assert.equal(archive[0].date, '2026-07-04')
assert.equal(archive[0].format, 'Markdown')
assert.equal(archive[0].score, 82)
assert.equal(archive[0].downloadLabel, 'Markdown 다운로드')

console.log('mypage report archive regression checks passed')
