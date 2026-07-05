import assert from 'node:assert/strict'
import {
  buildStatusExplanation,
  needsStatusExplanation,
  statusFromText,
} from './statusExplanation.js'

const urgent = buildStatusExplanation('긴급', {
  reason: '대상국이 DE이고 배터리 제품으로 분류되어 CE와 EU 배터리 규정 확인이 필요합니다.',
  sourceType: 'cache',
  evidenceCount: 5,
})

assert.equal(urgent.status, '긴급')
assert.equal(urgent.severity, 'high')
assert.match(urgent.plainSummary, /수출 전에/)
assert.match(urgent.reason, /CE/)
assert.equal(urgent.sourceType, 'cache')
assert.equal(urgent.evidenceCount, 5)
assert.ok(urgent.nextAction.length >= 2)
assert.match(urgent.dataStatus, /cache/)

const fallback = buildStatusExplanation('fallback', {
  reason: '',
  sourceType: 'fallback',
  evidenceCount: 1,
})

assert.equal(fallback.severity, 'medium')
assert.match(fallback.plainSummary, /실시간 데이터 대신/)
assert.match(fallback.nextAction[0], /live/)
assert.match(fallback.dataStatus, /fallback/)

const missing = buildStatusExplanation('KIPRIS 공공데이터 미연결 · 변리사 검토 필요', {
  sourceType: 'not_configured',
  evidenceCount: 0,
})

assert.equal(missing.status, '미연결')
assert.equal(missing.severity, 'high')
assert.match(missing.reason, /근거 데이터/)
assert.match(missing.nextAction.join(' '), /연결 상태/)

assert.equal(needsStatusExplanation('정상'), false)
assert.equal(needsStatusExplanation('5건 연결'), false)
assert.equal(needsStatusExplanation('보강'), true)
assert.equal(statusFromText('fallback 발생 시 담당자 근거 문서 지식화'), 'fallback')

console.log('status explanation template checks passed')
