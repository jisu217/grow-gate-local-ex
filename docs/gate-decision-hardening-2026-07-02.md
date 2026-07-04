# Gate Decision Hardening - 2026-07-02

## 목적

첨부 평가에서 지적된 핵심 문제는 기능 수가 아니라 "왜 이 서비스가 필요한가"가 화면과 코드에 충분히 드러나지 않는다는 점이었다. 이번 변경은 GrowGate를 단순 점수 화면이 아니라, 해외 바이어 문의에 대해 견적 전 진행 가능 여부를 판단하는 수출 Gate Decision 서비스로 정리한다.

## 적용한 변경

| 구분 | 변경 내용 | 코드 위치 |
| --- | --- | --- |
| 서비스 언어 | 핵심 UI의 AI 중심 표현을 `수출 Gate 판단`, `공공데이터와 제품 지식 대조`로 전환 | `frontend/FE-02/src/pages/auth/Home.jsx`, `TopNav.jsx`, `Topbar.jsx`, `DiagnosisList.jsx`, `DiagnosisResult.jsx` |
| Gate Decision | 결과 최상단에 `GO`, `CONDITIONAL_GO`, `HOLD`, `STOP` 판단과 다음 가능 단계, 막히는 단계, 견적 전 필수 확인을 표시 | `DiagnosisAsyncProcessor.java`, `DiagnosisDetailResponse.java`, `GateDecisionHero.jsx` |
| 바이어 문의 맥락 | 입력값에 바이어 문의 원문, 회신 마감일, 출하 예정일, 요구 인증, 보유 문서, 수출 경험, 예산을 추가 | `CreateDiagnosisRequest.java`, `DiagnosisNew.jsx`, `diagnosisService.js` |
| 업무 단위 Risk | RiskItem을 단순 점수 사유가 아니라 담당자, 산출물, 기한, 다음 행동, sourceId를 가진 업무 단위로 확장 | `DiagnosisAsyncProcessor.java`, `DiagnosisDetailResponse.java` |
| ActionPlan | 30/60/90 숫자 중심 로드맵을 `thisWeek`, `beforeQuote`, `beforeShipment` 업무 보드로 확장 | `DiagnosisAsyncProcessor.java`, `S08ExecBoard.jsx`, `diagnosisService.js` |
| 공공데이터 방어력 | source마다 `metricSummary`, `decisionImpact`, `sourceType`, `confidence`를 노출하고 fallback은 점수를 올리지 않게 유지 | `DiagnosisAsyncProcessor.java`, `S10Sources.jsx` |
| 바이어 대응 메모 | 다운로드 리포트 기본 문구를 PDF 주장 없이 Markdown 기반 `바이어 대응 메모`로 변경 | `DiagnosisAsyncProcessor.java`, `DiagnosisResult.jsx` |
| 홈 차별화 | Before/After 섹션을 추가해 기존 포털 조회·체크리스트와의 차이를 첫 화면에서 설명 | `Home.jsx`, `Home.module.css` |
| CI 리스크 게이트 | 실제 구현보다 앞서는 예측 모델 표현, GateDecision 누락, buyer context 누락, source impact 누락을 CI에서 차단 | `scripts/check-risk-gates.sh` |

## 심사 질문 대응

| 질문 | 방어 가능한 답변 |
| --- | --- |
| 이 서비스가 ChatGPT 질의와 다른 점은 무엇인가? | 제품·HS Code·바이어 문의를 구조화하고, 공공데이터 live/cache/fallback source와 로컬 규칙 근거를 분리해 Gate 판단과 담당자 업무로 남긴다. |
| 공공데이터가 실패하면 어떻게 되는가? | fallback은 `sourceType=fallback`으로 표시되고 점수를 올리지 않는다. 결과 상단 confidence panel과 S10 source card에서 담당자 확인 대상으로 표시된다. |
| AI 예측 모델인가? | 메인 제출 앱은 XGBoost/성과확률 예측을 주장하지 않는다. 현재는 source-traced rule scoring, agent loop, 담당자 승인형 self-loop wiki를 사용한다. |
| 결과가 실제 업무로 이어지는가? | 리스크마다 담당자, 필요 산출물, 기한, 다음 행동을 만들고 ActionPlan을 이번 주/견적 전/출하 전으로 나눈다. |
| 리포트가 PDF인가? | 현재 제출 경로는 Markdown 기반 바이어 대응 메모다. PDF 보관은 필요 시 브라우저 인쇄 또는 CI 변환으로만 설명한다. |

## 검증 체크리스트

- `scripts/check-risk-gates.sh`
- `cd frontend/FE-02 && npm run test:mappers && npm run build`
- `cd backend/BE-02 && ./gradlew test`
- 결과 화면에서 Gate Decision 패널이 KPI보다 먼저 보이는지 확인
- S10에서 live/cache/fallback/heuristic source와 metric/decision impact가 분리되어 보이는지 확인
