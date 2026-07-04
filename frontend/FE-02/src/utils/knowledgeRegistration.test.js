import assert from 'node:assert/strict'
import {
  calculateKnowledgeRegistrationRate,
  describeKnowledgeRegistration,
} from './knowledgeRegistration.js'

assert.equal(
  calculateKnowledgeRegistrationRate({
    name: '배터리팩',
    hsCode: '850760',
    targetCountry: 'DE',
    description: '산업용 리튬이온 배터리팩',
    trustGrade: 'A',
    knowledgeStatus: 'valid',
    missingFieldCount: 0,
  }),
  100,
)

assert.equal(
  calculateKnowledgeRegistrationRate({
    name: '배터리팩',
    hsCode: '850760',
    targetCountry: 'DE',
    description: '산업용 리튬이온 배터리팩',
    trustGrade: 'B',
    knowledgeStatus: 'needs_review',
    certificationCount: 1,
    ftoCount: 1,
    buyerRequirementCount: 0,
    missingFieldCount: 1,
  }),
  85,
)

assert.equal(
  calculateKnowledgeRegistrationRate({
    name: '배터리팩',
    hsCode: '850760',
    targetCountry: 'DE',
    description: '',
    trustGrade: null,
    knowledgeStatus: 'incomplete',
    missingFieldCount: 2,
  }),
  20,
)

assert.equal(
  calculateKnowledgeRegistrationRate({
    name: '의료 패치',
    hsCode: '901890',
    targetCountry: 'JP',
    description: '웨어러블 의료 패치',
    trustGrade: 'C',
    certifications: [{ id: 'cert-1' }],
    fto: [{ id: 'fto-1' }],
    buyerRequirements: [{ id: 'buyer-1' }],
  }),
  100,
)

assert.equal(
  calculateKnowledgeRegistrationRate({
    name: '의료 패치',
    hsCode: '901890',
    targetCountry: 'JP',
    description: '웨어러블 의료 패치',
    trustGrade: 'C',
    certifications: [],
    fto: [{ id: 'fto-1' }],
    buyerRequirements: [],
  }),
  65,
)

assert.equal(
  describeKnowledgeRegistration({ registrationRate: 85, missingFieldCount: 1 }),
  '데이터 기준 85% · 보완 1건',
)
assert.equal(
  describeKnowledgeRegistration({ registrationRate: 100, missingFieldCount: 0 }),
  '데이터 기준 100% · 필수 항목 입력 완료',
)

console.log('knowledge registration rate checks passed')
