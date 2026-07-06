const RUNNING_STATUSES = new Set(['pending', 'running'])
const DELAYED_AFTER_MS = 60 * 1000

export function isDiagnosisRunning(status) {
  return RUNNING_STATUSES.has(normalizeStatus(status))
}

export function getDiagnosisStatusMeta(status, requestedAt, now = Date.now()) {
  const code = normalizeStatus(status)
  const elapsedMs = elapsedFrom(requestedAt, now)
  const delayed = isDiagnosisRunning(code) && elapsedMs >= DELAYED_AFTER_MS

  if (code === 'done') {
    return {
      code,
      label: '완료',
      tone: 'done',
      message: 'Gate 판단이 완료되었습니다.',
      isRunning: false,
      delayed: false,
    }
  }

  if (code === 'failed') {
    return {
      code,
      label: '확인 필요',
      tone: 'failed',
      message: 'Gate 판단이 완료되지 않았습니다. 입력값을 확인하고 다시 실행해주세요.',
      isRunning: false,
      delayed: false,
    }
  }

  if (code === 'timeout') {
    return {
      code,
      label: '시간 초과',
      tone: 'timeout',
      message: '분석 시간이 초과되었습니다. 저장된 근거로 다시 실행하거나 입력값을 확인하세요.',
      isRunning: false,
      delayed: false,
    }
  }

  if (code === 'partial') {
    return {
      code,
      label: '부분 완료',
      tone: 'partial',
      message: '일부 공공데이터는 후속 확인 대상으로 분리하고 기본 Gate 판단은 완료했습니다.',
      isRunning: false,
      delayed: false,
    }
  }

  if (isDiagnosisRunning(code)) {
    return {
      code,
      label: delayed ? '지연 중' : '분석 중',
      tone: delayed ? 'delayed' : 'pending',
      message: delayed
        ? '분석이 지연되고 있습니다. 일부 공공데이터 응답 대기 중입니다.'
        : '공공데이터와 제품 정보를 대조 중입니다. 보통 10~30초 안에 완료됩니다.',
      isRunning: true,
      delayed,
    }
  }

  return {
    code,
    label: '확인 필요',
    tone: 'failed',
    message: 'Gate 판단 상태를 확인해야 합니다. 잠시 후 다시 조회해주세요.',
    isRunning: false,
    delayed: false,
  }
}

export function nextDiagnosisPollMs(items, now = Date.now()) {
  const runningItems = Array.isArray(items)
    ? items.filter((item) => isDiagnosisRunning(item?.status))
    : []
  if (runningItems.length === 0) return null

  const newestElapsed = Math.min(...runningItems.map((item) => elapsedFrom(item?.requestedAt, now)))
  if (newestElapsed < 15 * 1000) return 2000
  if (newestElapsed < 60 * 1000) return 4000
  return 10000
}

function normalizeStatus(status) {
  return String(status ?? '').trim().toLowerCase()
}

function elapsedFrom(value, now) {
  const time = Date.parse(value ?? '')
  if (Number.isNaN(time)) return 0
  return Math.max(0, now - time)
}
