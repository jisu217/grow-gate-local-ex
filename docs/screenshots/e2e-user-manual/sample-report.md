---
reportId: rpt_f70c509a-5e18-4d8f-a563-22511819b654
diagnosisId: f70c509a-5e18-4d8f-a563-22511819b654
publicDataProofRunId: b331d870-3454-441c-a5a8-85ffff6a29e9
modelVersion: growgate-gate-risk-v0.1.0
reportFormat: markdown
pdfStatus: optional_browser_print_or_ci_conversion
limitation:
  - 본 보고서는 법률·인증 최종 판단이 아닌 견적 전 risk screening 자료입니다.
  - LLM 보조 문장은 source trace와 claim verification을 통과한 설명에 한정합니다.
  - synthetic persona validation은 실제 고객 인터뷰가 아닙니다.
---

# GROW-GATE Local EX Markdown 리포트

- 제품: 산업용 리튬이온 배터리팩 48V 80Ah
- HS Code: 850760
- 대상국: GERMANY / EU
- Gate 판단 점수: 70/100
- 공식 산출물: Markdown 리포트 다운로드
- PDF 보관: 필요 시 브라우저 인쇄 또는 CI 변환으로 보관 가능

## 바이어에게 보낼 1차 회신 초안

현재 제품의 인증·리콜·FTO·공공데이터 근거를 확인 중입니다. 견적과 납기 확정 전 `배터리 품목군은 운송·안전 인증 증빙 강화 필요` 항목을 우선 점검하고, 확인 결과와 보완 자료를 함께 회신드리겠습니다.

## 담당자 즉시 할 일

- 이번 주: UN38.3/IEC62133 보유 여부와 시험기관 견적 확인
- 견적 전: UN38.3/IEC62133 보유 여부와 시험기관 견적 확인
- 출하 전: 출하 전 인증·시험성적서 패키지 확인

## 우선 보완 근거

배터리 품목군은 운송·안전 인증 증빙 강화 필요 때문에 `UN38.3/IEC62133 보유 여부와 시험기관 견적 확인` 조치가 필요합니다.

## Public Data Proof

- proofRunId: b331d870-3454-441c-a5a8-85ffff6a29e9
- source count: 12

## 근거 데이터와 AI 보조 해석 구분

### 근거 데이터

- Provider: Source Appendix의 provider/sourceId별 확인
- sourceType: live/cache/fallback/seed/empty를 구분해 확인
- 조회일: fetchedAt 또는 proofRun 생성 시각 기준
- 원문 링크: provider officialUrl 또는 record endpoint 기준
- rawCount/normalizedCount: public-data normalized payload 기준

### AI 보조 해석

- 요약: source trace와 규칙 점수 설명을 사람이 읽기 쉽게 정리
- 리스크 후보: 전문가 검토가 필요한 후보 항목 표시
- 확인 필요 사항: 담당자·인증기관·변리사 검토 전 확정 금지

주의: 본 내용은 자동 생성된 의사결정 보조 정보이며, 법률/인증/수출 실행 여부의 최종 판단이 아닙니다.

## FTO 사전 탐색 고지

본 결과는 공개 특허·상표 데이터와 입력 정보를 기반으로 한 사전 탐색 결과입니다. 특허 침해 여부, 실시 가능성, 권리 유효성에 대한 법률 판단이 아니며, 최종 의사결정 전 변리사 또는 법률 전문가 검토가 필요합니다.

## 개인정보·영업비밀 입력 제한

데모 환경에는 주민등록번호, 연락처, 계약서, 도면, 원가, 거래처명, 미공개 인증문서 등 개인정보 또는 영업비밀에 해당할 수 있는 정보를 입력하지 마십시오. 실제 개인정보가 포함될 경우 별도 동의와 처리방침이 필요하며, 로그에는 개인정보/API key/영업비밀을 남기지 않습니다.


## Claim Verification

- PASS [source_trace_present] 보고서 appendix에 sourceId가 연결되어 있습니다.

## Source Appendix

- RULE-LOCAL-INDUSTRY-경남창원
- USER-BUYER-REQUEST
- BIZINFO_SUPPORT-9cbdec72-6983-41e0-86d8-1a6b7f753e69
- BOK_KEY_STATISTICS-9c9d8b86-8fdb-44b4-9f60-5fa5e969531c
- CONSUMER_RECALL_CHEMICAL-b8ef978f-d1c1-46dc-9a52-f533d673e068
- CONSUMER_RECALL_OVERSEAS-c8aadd85-97cc-45da-a39f-6f1327bebf90
- CONSUMER_RECALL_SANITATION-bcd1ad18-8222-44f4-b1b5-7d1aa0a9a960
- EXIM_EXCHANGE_RATE-9da4dba5-a49e-482a-ba08-0a7e7ce0d420
- KATS_CERTIFIED_PRODUCT_INFO-b7c4eda1-c2dd-47ad-8d69-16f49997daa6
- KATS_DOMESTIC_RECALL_REPORT-d0701845-4318-44dc-843a-26d5d8a9433a
- KATS_PRODUCT_SAFETY_HAZARD_INFO-aa970e39-2c1b-4677-8640-82d6e7f38696
- KCS_TARIFF_LOGISTICS-39271e2c-7608-4c2d-9e6e-114a3bbe3ba8

## Synthetic Validation Disclaimer

synthetic persona validation은 실제 고객 인터뷰가 아닙니다. 합성 데이터는 시연 경로와 UX 검증용으로만 사용하며 live 공공데이터 proof와 구분합니다.
