### 문제 상황

특정 제품을 입력했을 때, 관련 공공데이터가 없으면 경고와 건수 표시 및 어떤 provider가 실패했는지 또는 어떤 근거가 fallback/empty인지 나타내며 담당자 검토 및 재수집 필요 상태를 표시함.

하지만 이 동작이 특정 분야에서만 잘 보이는 우연성을 가정하여 모든 진단 분야에서도 동일하게 작동하는지 확인함

### 검증 기준

| 확인 대상 | 기대 동작 |
|---|---|
| 시장성 | 관련 market public data가 없으면 live/cache 0건, fallback/empty 사유 노출 |
| 규제·인증 | 인증/규제 public data가 없으면 fallback 또는 empty trace 노출 |
| 리콜 위험 | recall 데이터가 없거나 호출 실패하면 source count와 오류 사유 노출 |
| FTO/특허 | KIPRIS 등 FTO 근거가 없으면 fallback/empty/0건 상태 노출 |
| 지원사업 | Bizinfo/support 데이터가 없으면 fallback/empty 사유 노출 |
| 모든 제품군 | 배터리뿐 아니라 의료, 해양, 일반 제조 등에서도 동일 UI 동작 |
| 모든 대상국 | EU뿐 아니라 Japan, US, Vietnam 등에서도 동일 UI 동작 |
| 모든 sourceType | `live`, `cache`, `fallback`, `empty`, `crawl`, `external_feed`가 숨겨지지 않고 구분 표시 |
| Gate | 데이터 없음이 있을 때 LLM이 덮어쓰지 않고 `fallbackCount`, `riskLevel`, 담당자 검토 필요로 반영 |

### 검증 결과 요약

| 항목 | 판정 | 확인 결과 | 조치 |
|---|---|---|---|
| 공통 경고 조건 | 문제 없음 | `SourceTrustBar`, `S10Sources` 모두 `liveCount + cacheCount === 0` 기준으로 경고 표시 | 분야별 조건이 아니라 source count 기반이라 전 분야 공통 동작 확인 |
| Backend source 전달 | 문제 없음 | `stageCards.s10 = sources`로 market/regulation/recall/fto/support source가 모두 전달됨 | 특정 분야만 필터링하는 구조 아님 |
| `/sources` 복원 경로 | 문제 없음 | 상세 결과에 `sourceTrustSummary`가 없을 때 source entries로 summary/ledger 복원 | 모든 source entry 기반으로 count 계산 확인 |
| `empty` source 표시 | 문제 발견 후 보강 완료 | `empty`라는 용어만으로는 API 미설정, catalog URL, 0건 응답 중 어떤 상태인지 모호하며, provider별 empty 상태를 확인하기 어려움 | `emptyCount` 추가 |
| Gate 소유권 | 문제 없음 | 테스트 케이스에서 `llmInfluencedGate=false` 유지 확인 | LLM output이 Gate를 덮어쓰지 않음 |

### 수정 사항

| 파일 | 수정 내용 | 효과 |
|---|---|---|
| `backend/BE-02/src/main/java/com/growgate/diagnosis/service/DiagnosisAsyncProcessor.java` | `sourceTrustSummary.emptyCount` 추가, provider health에 `emptyCount`와 `status=empty` 추가 | Backend 응답에서 `empty`가 `no_record`처럼 보이지 않음 |
| `frontend/FE-02/src/services/diagnosisService.js` | `/sources` 복원 경로에서도 `emptyCount` 계산, provider status에 `empty` 반영 | 상세 응답 또는 보조 sources API 어느 경로든 동일 표시 |
| `frontend/FE-02/src/components/diagnosis/SourceTrustBar.jsx` | 상단 Source Trust에 `Empty` 건수 표시, provider health 축약 표기에 `E` 추가 | 화면 첫 영역에서 데이터 없음 건수 확인 가능 |
| `frontend/FE-02/src/components/diagnosis/S10Sources.jsx` | S10에 `empty source N건` 확인 문구 추가, provider row에 `E` 추가 | 어떤 provider가 empty인지 상세 확인 가능 |
| `frontend/FE-02/src/styles/global.css` | `empty` provider 상태를 경고 톤으로 표시 | fallback과 구분되면서도 담당자 확인 대상으로 보임 |
| `frontend/FE-02/src/services/diagnosisService.mapper.test.js` | market/regulation/recall/fto/support 모든 분야가 `empty`/`fallback`인 회귀 테스트 추가 | 특정 분야에만 동작하는 우연성 방지 |

### 실행한 검증

| 명령 | 결과 | 비고 |
|---|---|---|
| `npm run test:mappers` | 통과 | 모든 분야 `empty`/`fallback` mapper 회귀 테스트 포함 |
| `npm run build` | 통과 | JSX/CSS 변경 번들링 확인 |
| `./gradlew test --tests com.growgate.diagnosis.service.DiagnosisServiceTest` | 통과 | Backend Java 컴파일 및 진단 서비스 테스트 확인 |
