# Evidence Workflow Redesign - 2026-07-02

## 목적

첨부 평가의 핵심은 GROW-GATE를 구현 범위보다 큰 AI/ML 서비스로 보이게 하지 말고, 실제 구현 범위에 맞는 “근거 지식화 기반 수출 의사결정 workflow trace 서비스”로 재정렬하라는 것이다.

이번 수정은 그 방향에 맞춰 진단 결과 payload, S10 화면, 마이페이지 지식화 표현, README/리스크 게이트를 보강했다.

## 변경한 구현

| 영역 | 변경 내용 | 방어 포인트 |
| --- | --- | --- |
| Backend result payload | `evidenceRetrieval`, `exportKnowledgeTimeline`, `evidenceGuardrails` 추가 | top-k 근거, 시계열 source event, fallback 정책을 API 결과로 설명 가능 |
| Evidence retrieval | `retrievalMode`를 `source-bound-evidence-search`로 변경하고 `claimBoundary`, `sourceBound`, `displayRole` 추가 | pgvector 기반 대규모 문서 검색이 아니라 DB/source trace 범위의 bounded retrieval임을 명시 |
| Timeline compiler | `sourceId`, `sourceType`, `usedFor`, `fetchedAt`, `publishedAt`, `sourcePeriodStart/End`, `scoreImpact` 기반 이벤트 생성 | “언제 수집한 어떤 근거가 점수에 들어갔는가?” 질문에 대응 |
| Guardrails | fallback positive contribution count, unsupported claim count, official live/cache record count 계산 | fallback으로 점수를 올리지 않는 정책 검증 |
| S10 화면 | Public Data Pipeline에서 Evidence Workflow로 확장 | 공공데이터 카드, top-k evidence chunk, guardrail, export knowledge timeline을 한 화면에서 시연 |
| 마이페이지 | `AI 지식화`를 `근거 지식화`로 변경 | 담당자 승인형 self-loop wiki 구조를 강조 |
| 문서 | README와 risk gate에 evidence workflow 항목 추가 | 제출/팀 공유 문서와 코드 표현 일치 |

## 새 결과 JSON 핵심 필드

```json
{
  "evidenceRetrieval": {
    "mode": "source-bound-evidence-search",
    "claimBoundary": "vector DB 검색이 아니라 진단 DB와 source trace 안에서만 근거를 찾는 bounded evidence retrieval",
    "topK": [
      {
        "chunkId": "public-data:...",
        "sourceId": "KCS_TRADE-...",
        "sourceType": "live",
        "sourceBound": true,
        "displayRole": "공공데이터 근거 chunk",
        "usedSentence": "..."
      }
    ]
  },
  "exportKnowledgeTimeline": [
    {
      "eventType": "EXPORT_METRIC_SIGNAL",
      "provider": "KCS_TRADE",
      "sourceType": "live",
      "usedFor": "market",
      "periodLabel": "2026-06-01 ~ 2026-06-30",
      "scoreImpact": 9
    }
  ],
  "evidenceGuardrails": {
    "unsupportedClaimCount": 0,
    "fallbackPositiveContributionCount": 0,
    "officialLiveOrCacheRecordCount": 1,
    "verdict": "pass"
  }
}
```

## 발표 문구

사용 가능한 표현:

- “공공데이터와 담당자 지식을 source trace로 묶어 수출 Gate 판단을 돕습니다.”
- “live/cache/fallback과 rule heuristic을 구분하고, fallback은 점수를 올리지 않습니다.”
- “S10에서 top-k evidence chunk와 export knowledge timeline을 보여줍니다.”
- “담당자가 승인한 근거만 self-loop wiki로 재사용합니다.”

피해야 할 표현:

- “대규모 문서 검색 기반 자동 의사결정”
- “AI가 수출 성과 확률을 예측”
- “XGBoost/LLM이 점수를 직접 산출”
- “공공데이터 API가 항상 live로 완전 연동”

## 검증 기준

- `scripts/check-risk-gates.sh`가 `evidenceRetrieval`, `exportKnowledgeTimeline`, `evidenceGuardrails` wiring을 확인한다.
- frontend mapper test가 S10 `pipes`, `timeline`, `evidenceRetrieval` 매핑을 확인한다.
- backend agent context test가 `source-bound-evidence-search`와 source-bound chunk 필드를 확인한다.

## 2026-07-02 검증 결과

| 검증 | 결과 |
| --- | --- |
| `./scripts/check-risk-gates.sh` | 통과 |
| `cd frontend/FE-02 && npm run test:mappers` | 통과 |
| `cd frontend/FE-02 && npm run build` | 통과 |
| `VITE_APP_MODE=presentation VITE_PRESENTATION_MODE=true npm run build` | 통과 |
| presentation build mock/과장 표현 grep | mock key 및 `AI 지식화`, `추론 Agent` 미검출 |
| `cd backend/BE-02 && ./gradlew test --no-daemon` | 통과 |
| dev server HTTP smoke | `http://127.0.0.1:5174/diagnosis/diag-empty` 200 |

당시 Playwright Chromium headless 실행은 macOS sandbox의 Mach port 권한으로 차단되어 스크린샷 검증까지는 수행하지 못했다. 이후 2026-07-05에 `frontend/FE-02` dev dependency로 Playwright를 추가하고 Chromium runtime 실행을 sandbox 밖 권한으로 확인했다. 발표 핵심 화면은 mapper/build 검증과 함께 Playwright screenshot/assertion을 우선 적용한다.
