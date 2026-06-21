// 로딩·에러·빈 상태 메시지 UI
import styles from './StatusMessage.module.css'

const TYPE_CLASS = {
  empty:      styles.empty,
  loading:    styles.loading,
  error:      styles.error,
  permission: styles.permission,
}

export default function StatusMessage({ type = 'error', message }) {
  return (
    <div className={`${styles.wrap} ${TYPE_CLASS[type] ?? styles.error}`}>
      <p className={styles.msg}>{message}</p>
    </div>
  )
}
