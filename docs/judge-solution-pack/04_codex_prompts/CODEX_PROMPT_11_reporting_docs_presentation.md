# Codex Prompt 11 — 보고서/문서/발표자료 문구 정리

목표:
AI/ML/RAG/실시간/실제 고객 데이터 과장 표현을 제거하고 심사 방어 문장으로 통일한다.

작업:
1. README, docs, 발표자료 초안을 검색한다: `AI Agent 자동 판단`, `XGBoost 기반 성공예측`, `RAG`, `실시간 공공데이터`, `실제 고객 데이터`.
2. 실제 구현 수준으로 바꾼다:
   - “규칙 기반 Gate + ML 보조 예측 + SHAP 해석 + source-grounded Agent 보고서”
   - “공공데이터 live/cache/fallback/seed 투명 표시”
   - “합성 PoC 데이터이며 실제 고객 검증은 인터뷰/로그로 별도 확보”
3. `docs/ATTACK_ANSWERS.md`에 7명 심사위원 공격 질문과 답변을 추가한다.
4. reportMarkdown에는 limitations 섹션을 필수 추가한다.

수용 조건:
- 위험 표현 grep 결과가 0 또는 안전 문맥으로만 존재한다.
- 발표 답변 10개가 docs에 정리된다.
