# Dev-only Fixtures

이 폴더의 데이터는 로컬 UI 개발과 mapper 테스트 보조용 fixture입니다.

- production build와 presentation mode에서는 `VITE_ENABLE_MOCK=true`가 아니면 사용되지 않습니다.
- API 실패 시 제출/시연 화면에서 mock 결과로 대체하지 않습니다.
- S04-S07 핵심 시장성, 규제, 리콜, FTO/지원 데이터는 backend `stageCards` 응답만 판단 기준으로 사용합니다.
- 이 fixture는 live 공공데이터 수집, 인증/권한, 보고서 검증 증거로 제시하지 않습니다.
