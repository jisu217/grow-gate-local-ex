// 로딩·에러·빈 상태 메시지 UI
import styles from './StatusMessage.module.css'

const TYPE_CLASS = {
  empty:      styles.empty,
  loading:    styles.loading,
  error:      styles.error,
  permission: styles.permission,
}

// onAction/onSecondary가 주어지면 메시지 아래에 다음 행동 버튼을 노출한다.
// (운영 중 API 실패 시 "다시 시도"·"뒤로가기" 동선을 사용자에게 제공)
export default function StatusMessage({
  type = 'error',
  message,
  actionLabel = '다시 시도',
  onAction,
  secondaryLabel = '뒤로가기',
  onSecondary,
}) {
  return (
    <div className={`${styles.wrap} ${TYPE_CLASS[type] ?? styles.error}`}>
      <p className={styles.msg}>{message}</p>
      {(onAction || onSecondary) && (
        <div className={styles.actions}>
          {onAction && (
            <button type="button" className={styles.primaryBtn} onClick={onAction}>
              {actionLabel}
            </button>
          )}
          {onSecondary && (
            <button type="button" className={styles.secondaryBtn} onClick={onSecondary}>
              {secondaryLabel}
            </button>
          )}
        </div>
      )}
    </div>
  )
}
