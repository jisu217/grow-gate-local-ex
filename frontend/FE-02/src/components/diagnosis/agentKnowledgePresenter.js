import { humanProviderName, humanStatusLabel } from './sourceEvidencePresenter.js'

export function buildAgentKnowledgeActions(session = {}) {
  const productId = session?.productId ? encodeURIComponent(session.productId) : ''
  const diagnosisId = session?.id ? encodeURIComponent(session.id) : ''
  return [
    {
      label: '지식관리에서 검토',
      to: productId ? `/mypage/knowledge/${productId}` : '',
      enabled: Boolean(productId),
      disabledLabel: '제품 지식 연결 필요',
    },
    {
      label: '근거 데이터 보기',
      to: diagnosisId ? `/diagnosis/${diagnosisId}#evidence-data` : '',
      enabled: Boolean(diagnosisId),
      disabledLabel: '진단 근거 연결 필요',
    },
    {
      label: '실행보드 보기',
      to: diagnosisId ? `/diagnosis/${diagnosisId}#action-board` : '',
      enabled: Boolean(diagnosisId),
      disabledLabel: '실행보드 생성 전',
    },
  ]
}

export function humanAgentDataStatus(value) {
  if (!value || typeof value !== 'object') return '상태 확인 필요'
  const provider = value.provider ? humanProviderName(value.provider) : ''
  const mode = humanStatusLabel(value.mode ?? value.sourceType ?? value.status)
  return provider ? `${provider} / ${mode}` : mode
}
