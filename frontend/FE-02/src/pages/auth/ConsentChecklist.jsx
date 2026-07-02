import styles from './Auth.module.css'

export const CONSENT_POLICY_VERSION = '2026-07-privacy-v1'

export function initialConsentState() {
  return {
    termsAccepted: false,
    privacyAccepted: false,
    marketingAccepted: false,
  }
}

export function ConsentChecklist({ value, onChange }) {
  const update = (key) => (event) => {
    onChange({ ...value, [key]: event.target.checked })
  }

  return (
    <div className={styles.consentBox} aria-label="가입 동의 항목">
      <label className={styles.checkRow}>
        <input type="checkbox" checked={value.termsAccepted} onChange={update('termsAccepted')} />
        <span>
          <strong>[필수] 서비스 이용약관 동의</strong>
          <small>계정 생성, 로그인, 제품/진단 데이터 관리를 위한 기본 이용 조건입니다.</small>
        </span>
      </label>
      <label className={styles.checkRow}>
        <input type="checkbox" checked={value.privacyAccepted} onChange={update('privacyAccepted')} />
        <span>
          <strong>[필수] 개인정보 수집·이용 동의</strong>
          <small>이메일, 이름, 비밀번호 해시, 기업 가입 시 사업자번호와 회사 정보를 가입·보안·권한 관리 목적으로 처리합니다.</small>
        </span>
      </label>
      <label className={styles.checkRow}>
        <input type="checkbox" checked={value.marketingAccepted} onChange={update('marketingAccepted')} />
        <span>
          <strong>[선택] 안내·마케팅 수신 동의</strong>
          <small>공모전 제출 서버에서는 마케팅 발송을 하지 않으며, 선택하지 않아도 가입할 수 있습니다.</small>
        </span>
      </label>
    </div>
  )
}
