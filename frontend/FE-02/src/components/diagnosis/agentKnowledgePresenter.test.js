import assert from 'node:assert/strict'
import {
  buildAgentKnowledgeActions,
  humanAgentDataStatus,
} from './agentKnowledgePresenter.js'

const actions = buildAgentKnowledgeActions({
  id: 'diag-agent',
  productId: 'prod-1',
})

assert.deepEqual(actions.map((action) => action.label), [
  '지식관리에서 검토',
  '근거 데이터 보기',
  '실행보드 보기',
])
assert.equal(actions[0].to, '/mypage/knowledge/prod-1')
assert.equal(actions[1].to, '/diagnosis/diag-agent#evidence-data')
assert.equal(actions[2].to, '/diagnosis/diag-agent#action-board')
assert.ok(actions.every((action) => action.enabled))

const missingActions = buildAgentKnowledgeActions({})
assert.equal(missingActions[0].enabled, false)
assert.equal(missingActions[1].enabled, false)
assert.equal(missingActions[2].enabled, false)

assert.equal(
  humanAgentDataStatus({ provider: 'RULE_ENGINE', mode: 'cache' }),
  '규칙 엔진 판단 / 저장된 최신 데이터 사용'
)
assert.equal(humanAgentDataStatus(null), '상태 확인 필요')

console.log('agent knowledge presenter regression checks passed')
