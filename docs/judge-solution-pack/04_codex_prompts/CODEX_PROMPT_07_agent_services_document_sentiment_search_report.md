# Codex Prompt 07 — Agent 고도화: 보고서/초안/문서분류/감정분석/고급검색

목표:
AI 서비스는 “자동 판단”이 아니라 다음 5개 업무를 잘하게 만든다: 보고서, 초안 작성, 문서 분류, 감정분석, 고급 검색.

작업:
1. DocumentClassifierAgent
   - knowledge_documents_index.jsonl 또는 DB knowledge_sources를 읽어 제품 문서/정책/위키/RFQ/시험성적서로 분류한다.
   - output: docType, productId, evidenceSourceIds, missingFields.
2. SentimentAgent
   - buyerRequest에서 urgency, pressure, ambiguity, negotiationRisk를 0~100으로 산출한다.
   - 한국어/영어 키워드 기반 baseline + optional LLM summary.
3. AdvancedSearchAgent
   - source-bound topK 검색. 우선 BM25/keyword fallback, 후속으로 vector DB optional.
   - 출처 없는 chunk는 report claim에 사용 금지.
4. ReportAgent
   - Markdown 보고서, buyer-facing reply draft, internal action memo 생성.
   - source appendix와 limitations 필수.
5. ShapInterpretationAgent
   - ML shapTopFeatures를 업무 액션으로 번역한다.
   - 예: cert_gap_count↑ → “견적 전 인증 gap table을 바이어에게 공유하고 확정 납기는 보류”.

수용 조건:
- 동일 입력에 대해 deterministic fallback 결과가 나온다.
- LLM provider disabled여도 local baseline report가 생성된다.
- claim verification 결과가 S10에 표시된다.
