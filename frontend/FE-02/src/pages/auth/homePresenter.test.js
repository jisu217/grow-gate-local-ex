import assert from 'node:assert/strict'
import {
  HOME_BEFORE_AFTER,
  HOME_FEATURES,
  HOME_READINESS,
  buildHomeStats,
  buildHomeStatsFallback,
} from './homePresenter.js'

assert.equal(
  HOME_READINESS.find((item) => item.k === '서비스 구현')?.v,
  'React 화면에서 Spring 진단 생성, 상태 갱신, 결과 리포트까지 연결'
)

assert.equal(
  HOME_FEATURES.find((item) => item.id === 'evidence')?.description,
  '공공데이터 상태와 제품 정보를 연결해, 기업이 바로 검토할 실행 후보로 정리합니다.'
)

assert.ok(HOME_FEATURES.every((item) => item.actionLabel === '자세히 보기'))
assert.ok(HOME_FEATURES.every((item) => !['Gate 판단', '데이터 보기', '업무 보드'].includes(item.actionLabel)))
assert.ok(HOME_FEATURES.every((item) => item.modal?.result && item.modal?.caution))

const afterOutputs = HOME_BEFORE_AFTER.flatMap((item) => item.afterOutputs)
assert.deepEqual(afterOutputs, ['출처 연결', 'Gate 판단', '업무 생성', '지식화 후보', '리포트 반영'])

const stats = buildHomeStats({
  runCount: 2,
  recordCount: 20,
  successCount: 8,
  cacheHitCount: 4,
  fallbackCount: 3,
  newestFetchedAt: '2026-07-06T08:00:00Z',
  providers: [
    { provider: 'KCS_TRADE', liveCount: 3, cacheCount: 0, fallbackCount: 0 },
    { provider: 'KOTRA_MARKET', liveCount: 0, cacheCount: 2, fallbackCount: 0 },
    { provider: 'KIPRIS_FTO', liveCount: 0, cacheCount: 0, fallbackCount: 1 },
  ],
})

assert.equal(stats[0].value, '3개')
assert.equal(stats[0].label, '공공데이터 provider 상태 추적')
assert.match(stats[0].helper, /최근 수집 기준/)
assert.equal(stats[2].value, '100%')
assert.match(stats[2].helper, /source trace/)

const emptyStats = buildHomeStats({ providers: [] })
assert.equal(emptyStats[0].value, '-')
assert.match(emptyStats[0].helper, /확인 필요/)

const fallbackStats = buildHomeStatsFallback()
assert.ok(fallbackStats.every((item) => item.value === '-'))
assert.ok(fallbackStats.every((item) => /최근 수집 상태 확인 필요/.test(item.helper)))

console.log('home presenter regression checks passed')
