import assert from 'node:assert/strict'
import { getDiagnosisStatusMeta, isDiagnosisRunning, nextDiagnosisPollMs } from './diagnosisStatusPresenter.js'

const now = Date.parse('2026-07-06T12:00:00.000Z')

assert.equal(getDiagnosisStatusMeta('done', null, now).label, '완료')
assert.equal(getDiagnosisStatusMeta('failed', null, now).label, '확인 필요')
assert.equal(getDiagnosisStatusMeta('timeout', null, now).message.includes('초과'), true)
assert.equal(getDiagnosisStatusMeta('partial', null, now).label, '부분 완료')

assert.equal(isDiagnosisRunning('pending'), true)
assert.equal(isDiagnosisRunning('running'), true)
assert.equal(isDiagnosisRunning('done'), false)

const fresh = getDiagnosisStatusMeta('running', '2026-07-06T11:59:45.000Z', now)
assert.equal(fresh.label, '분석 중')
assert.equal(fresh.delayed, false)

const delayed = getDiagnosisStatusMeta('running', '2026-07-06T11:58:30.000Z', now)
assert.equal(delayed.label, '지연 중')
assert.equal(delayed.delayed, true)

assert.equal(nextDiagnosisPollMs([{ status: 'running', requestedAt: '2026-07-06T11:59:50.000Z' }], now), 2000)
assert.equal(nextDiagnosisPollMs([{ status: 'running', requestedAt: '2026-07-06T11:59:30.000Z' }], now), 4000)
assert.equal(nextDiagnosisPollMs([{ status: 'running', requestedAt: '2026-07-06T11:58:00.000Z' }], now), 10000)
assert.equal(nextDiagnosisPollMs([{ status: 'done', requestedAt: '2026-07-06T11:58:00.000Z' }], now), null)

console.log('diagnosisStatusPresenter tests passed')
