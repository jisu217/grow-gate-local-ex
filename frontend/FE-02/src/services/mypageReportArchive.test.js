import assert from 'node:assert/strict'
import {
  buildReportArchive,
  buildRerunDiagnosisPath,
  buildRerunDiagnosisPayload,
} from './mypageService.js'

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

assert.equal(
  buildRerunDiagnosisPath({ id: 'prod-1' }),
  '/diagnosis/new?productId=prod-1&mode=rerun'
)
assert.equal(
  buildRerunDiagnosisPath({ id: 'prod 1/특수' }),
  '/diagnosis/new?productId=prod+1%2F%ED%8A%B9%EC%88%98&mode=rerun'
)
assert.equal(buildRerunDiagnosisPath(null), '/diagnosis/new')

const rerunPayload = buildRerunDiagnosisPayload({
  id: 'prod-1',
  name: '배터리팩',
  hs: '850760',
  country: 'DE',
  category: '전기전자',
  description: '산업용 배터리팩',
  buyerCountry: 'VN',
  buyerRequest: '베트남 바이어가 UN38.3과 영문 라벨을 요구함',
})

assert.equal(rerunPayload.productId, 'prod-1')
assert.equal(rerunPayload.targetCountry, 'VN')
assert.equal(rerunPayload.inputData.productName, '배터리팩')
assert.equal(rerunPayload.inputData.hsCode, '850760')
assert.equal(rerunPayload.inputData.industry, '전기전자')
assert.equal(rerunPayload.inputData.buyerRequest, '베트남 바이어가 UN38.3과 영문 라벨을 요구함')
assert.equal(buildRerunDiagnosisPayload(null), null)

console.log('mypage report archive regression checks passed')
