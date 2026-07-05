# Legal and Ethical Risk Register

Checked at: 2026-07-04

This register is for contest defense and implementation discipline. It is not legal advice.

| 리스크 | 심사위원 공격 질문 | 현재 대응 | 부족한 점 | 수정 조치 | 남은 리스크 |
| --- | --- | --- | --- | --- | --- |
| 공공데이터 재배포 | 재가공 데이터를 화면/보고서에 보여줘도 되나? | `sourceType`, `operationEndpoint`, `apiKeyConfigured`, raw/normalized count를 구분한다. KOTRA/SafetyKorea는 공공데이터포털 이용허락범위 제한 없음 확인. | KIPRIS Plus 상품별 재배포 조건과 Bizinfo 공고별 저작권/정책정보 API 조건은 미확인. | `docs/public-data-license-and-usage-matrix.md` 생성. `PublicDataProvider`, DTO, 화면에 license/storage/reprocessing/redistribution metadata와 usageWarning 추가. | Provider별 최신 약관과 operation endpoint 문서를 발표 전 다시 확인해야 한다. |
| FTO 법률 판단 오해 | 특허 권리 충돌 부재를 확정처럼 말하는가? | 기존 claim verifier는 `FTO 문제 없음`, `문제 없음`, `리스크 없음`, `확정`, `보장` 등을 차단한다. | 화면/보고서에 변리사 검토 필요 고지가 부족했다. | S07과 Markdown report에 공개 특허·상표 데이터 기반 사전 탐색, 법률 판단 아님, 변리사/법률 전문가 검토 필요 고지 추가. | 실제 사용자가 입력한 제품 설명이 불충분하면 FTO 후보 신호도 누락될 수 있다. |
| 영업비밀 | 기업 민감정보를 입력받는가? | API key는 환경변수/secret manager로만 주입하고 raw key 커밋을 risk gate에서 차단한다. | 입력 화면에서 도면/원가/거래처/미공개 인증문서 입력 금지 안내가 명시적이지 않았다. | S01과 report에 주민등록번호, 연락처, 계약서, 도면, 원가, 거래처명, 미공개 인증문서 입력 금지 문구 추가. | 운영 전 파일 업로드/첨부 기능이 생기면 DLP, 마스킹, 삭제 정책 테스트가 필요하다. |
| 개인정보 | 동의/보관/삭제 정책이 있는가? | 회원가입은 서비스 이용약관, 개인정보 수집·이용, 마케팅 선택 동의를 분리하고 `user_consent_records`에 기록한다. | 데모 입력 데이터 장기 보관 범위와 개인정보 포함 시 별도 처리방침 필요 고지가 약했다. | report와 docs에 개인정보 포함 금지, 포함 시 별도 동의/처리방침 필요, 로그에 개인정보/API key/영업비밀 미기록 정책 추가. | 실제 운영 전 개인정보 처리방침 URL, 보관기간, 삭제 요청 flow, 접근권한 감사가 필요하다. |
| AI 책임 | AI가 틀리면 누가 책임지는가? | AGENTS.md와 README는 final Gate가 deterministic rule score + risk/fallback 조건이며 LLM은 advisory라고 제한한다. | 화면/report에서 공공데이터 원문 근거, 정규화 결과, AI 요약/추론, 사람 확인 항목 구분이 약했다. | Markdown report와 S09에 `근거 데이터`, `AI 보조 해석`, `주의` 구조 추가. `llmInfluencedGate=false` 정책 유지. | AI 요약이 사용자의 확정 결정처럼 복사되지 않도록 다운로드/공유물 검수 문구 유지 필요. |
| SourceType 오인 | fallback/seed를 live처럼 보이게 하지 않는가? | S10 proof 화면과 source page가 sourceType, operation endpoint, API key 여부를 표시한다. | `empty`와 `seed` 설명 및 provider별 usageWarning이 화면에 부족했다. | S10의 `empty` 표시 추가. 출처 페이지에 sourceType legend와 usageWarning/license metadata 표시 추가. | 기존 저장된 diagnosis result에는 새 metadata가 없을 수 있으므로 새 진단 또는 재수집이 필요하다. |

## Defense Position

GrowGate is a rule-based Gate with LLM-assisted report text. Public data, normalized metrics, and AI interpretation are separated. FTO is a preliminary risk-signal search, not a legal opinion. Synthetic demo data is not real customer validation.
