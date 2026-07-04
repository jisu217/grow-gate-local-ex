# 발표장 리스크 점검표

작성일: 2026-07-01

| 리스크 | 현재 처리 | 검증 방법 |
| --- | --- | --- |
| API 연결 실패 | smoke가 `/health`, 인증 없는 401, signup/login, cookie auth, `/users/me`, 제품 생성, 진단 생성/조회까지 확인한다. `SMOKE_CORS_ORIGIN`을 주면 CORS preflight도 확인한다. | `BASE_URL=https://도메인 SMOKE_CORS_ORIGIN=https://도메인 ./scripts/smoke-prod.sh` |
| 공공데이터 fallback만 발생 | `PUBLIC_DATA_ENABLED=true`가 기본값이고 dashboard가 live/cache/fallback 비율을 반환한다. 발표 서버에서는 `SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true`로 live record가 없으면 실패시킨다. | `SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true BASE_URL=https://도메인 ./scripts/smoke-prod.sh` |
| S04~S07 mock 노출 | `diagnosisService.js`는 S04~S07 mock import/merge를 하지 않고 backend `stageCards`만 사용한다. risk gate가 회귀를 차단한다. | `./scripts/check-risk-gates.sh` |
| 리포트 다운로드 실패 | React 다운로드 핸들러가 try/catch로 화면 error state를 표시하고, smoke가 Markdown report API를 확인한다. PDF 보관은 브라우저 인쇄 또는 CI 변환으로만 설명한다. | `npm run build`, `./scripts/smoke-prod.sh` |
| 로그인 보안 질문 | access token은 메모리에만 두고, 서버가 `gg_access_token` HttpOnly cookie를 내려준다. filter는 Bearer와 cookie를 모두 지원한다. | `./scripts/check-risk-gates.sh`, smoke의 `/api/users/me with HttpOnly cookie` 단계 |
| 제품별 public data 누출 | public-data run/record 조회는 `requestedBy` scope로 제한된다. 개인은 본인, 회사 사용자는 같은 company membership 범위만 조회한다. | `PublicDataCollectionServiceTest`, `./scripts/check-risk-gates.sh` |
| 빌드 재현 실패 | GitHub Actions CI, `BUILD.md`, `scripts/verify-local.sh`로 Java 17/Node 20 기준 재현 절차를 고정했다. | `./scripts/verify-local.sh` |

## 발표 전 권장 순서

1. `./scripts/verify-local.sh`
2. 배포 서버 환경변수 확인: `PUBLIC_DATA_ENABLED=true`, `APP_CORS_ORIGINS`, `APP_AUTH_COOKIE_SECURE=true`, provider별 API key/endpoint
3. `BASE_URL=https://도메인 SMOKE_CORS_ORIGIN=https://도메인 ./scripts/smoke-prod.sh`
4. API key가 준비된 발표 서버에서는 `SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true`를 추가해 live 수집 증거를 강제한다.
