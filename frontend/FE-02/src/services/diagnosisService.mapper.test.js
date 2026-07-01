import assert from 'node:assert/strict'
import { mapDiagnosisDetail } from './diagnosisService.js'

const detail = {
  id: 'diag-1',
  productId: 'prod-1',
  productName: '리튬이온 배터리팩',
  targetCountry: 'DE',
  status: 'done',
  knowledgeSnapshot: {
    trustGrade: 'high',
    missingFields: [],
  },
  result: {
    diagnosisScore: {
      overall: 82,
      market: 78,
      regulation: 81,
      recall: 69,
      fto: 74,
      support: 88,
    },
    summary: 'source-traced readiness scoring result',
    stageCards: {
      s04: [{ flag: 'DE', score: 78, publicSourceCount: 2, evidenceStrength: 4 }],
      s05: [{ name: 'EU Battery Regulation', targetCountry: 'DE', priorityLabel: '높음' }],
      s06: [{ title: 'Battery recall signal', similarity: 0.42, checks: ['SDS 확인'] }],
      s07: {
        patents: [{ name: 'BMS FTO', riskClass: 'm', badge: '검토' }],
        supports: [{ name: '수출바우처', org: '기업마당', dday: 'D-20', isGreen: true }],
      },
      s10: [{ provider: 'KCS_TRADE', sourceType: 'live', scoreContribution: 9, confidence: 0.82 }],
    },
  },
}

const mapped = mapDiagnosisDetail(detail)

assert.equal(mapped.session.id, 'diag-1')
assert.equal(mapped.session.productName, '리튬이온 배터리팩')
assert.equal(mapped.stages.s04[0].flag, 'DE')
assert.equal(mapped.stages.s04[0].score, 78)
assert.equal(mapped.stages.s05[0].nm, 'EU Battery Regulation')
assert.equal(mapped.stages.s06[0].title, 'Battery recall signal')
assert.equal(mapped.stages.s07.patents[0].name, 'BMS FTO')
assert.equal(mapped.stages.s07.supports[0].name, '수출바우처')
assert.equal(mapped.stages.s10[0].source, 'KCS_TRADE')
assert.equal(mapped.stages.s10[0].status, 'live')
