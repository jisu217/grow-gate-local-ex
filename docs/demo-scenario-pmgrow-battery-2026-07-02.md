# GrowGate 시연 시나리오: 독일 수출용 리튬이온 배터리팩

이 문서는 발표/심사 직전 서버에 실제 데모 데이터를 넣고, 화면에서 어떤 순서로 보여줄지 정리한 운영용 자료다. 공모전 전략 문서는 아니며 팀 내부 시연 준비 문서다.

## 한 번에 준비

```bash
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/setup-demo-scenario.sh
```

정식 도메인을 새로 붙이면 `BASE_URL=https://도메인`으로 바꿔 실행한다. 스크립트는 공개 API만 사용해서 회원가입, 로그인, 제품 등록, 제품 지식 등록, 공공데이터 수집 요청, 진단 생성/폴링, 출처 조회, Markdown 리포트 다운로드, 지식화 후보 승인까지 수행한다.

기본 데모 계정:

- 이메일: `demo-pmgrow@growgate.local`
- 비밀번호: `GrowGateDemo123!`

공개 서버에 오래 열어둘 때는 `DEMO_EMAIL`, `DEMO_PASSWORD`를 별도로 지정해 실행하고 발표 후 계정을 정리한다.

## 제품 시나리오

- 제품: `PMGrow 리튬이온 배터리팩`
- HS Code: `850760`
- 대상국: `DE`
- 수출 맥락: 독일/EU B2B 납품
- 핵심 리스크: UN38.3, IEC 62133-2, CE/EMC, RoHS, EU Battery Regulation 2023/1542, 배터리 여권, 위험물 운송, BMS/FTO

## 스크립트가 넣는 데이터

- 제품 지식: 담당자 검증 노트와 `manager_verified` 출처 유형
- 인증: UN38.3, IEC 62133-2 시험 계획, RoHS 자체 적합성 선언
- FTO: 독일 대상 `medium` 리스크와 BMS/열폭주/하우징 특허 검토 메모
- 바이어 요구사항: 독일 바이어와 물류 파트너 요구사항 2건
- 지식 출처: EU Battery Regulation, UN Manual of Tests and Criteria, 국가기술표준원 참고 URL
- 공공데이터: `/api/public-data/collect`로 HS 850760/DE 수집 요청
- 진단: `/api/diagnosis` 생성 후 완료까지 polling
- 리포트: `/report` Markdown 바이어 대응 메모 검증
- self-loop wiki: 진단 결과에서 생성된 지식화 후보 1건을 담당자 승인 처리

## 발표 화면 순서

1. 로그인 후 마이페이지에서 방금 생성된 제품을 보여준다.
2. 제품 지식 상세에서 인증, FTO, 바이어 요구사항, 출처가 담당자 입력 데이터로 저장된 것을 보여준다.
3. 진단 결과 화면에서 S04~S07이 backend `stageCards` 기반으로 채워지는지 확인한다.
4. S10 출처 화면에서 `live/cache/fallback/heuristic/user_input/agent`가 구분되는지 보여준다.
5. 공공데이터 dashboard에서 success/fallback 비율을 확인한다.
6. Markdown 바이어 대응 메모를 다운로드하고 UTF-8 한글 내용이 보존되는지 보여준다.
7. 마이페이지 지식화 후보에서 AI 판단 근거를 담당자가 승인해 지식 출처로 순환시키는 흐름을 설명한다.

## 약한 질문에 대한 방어 포인트

- “공공데이터가 실패하면?”: fallback이 화면과 sourceType에 명시되고 confidence가 낮아진다. 성공/실패 비율은 dashboard와 smoke 로그로 확인한다.
- “AI Agent가 실제로 뭘 하나?”: 진단 trace는 planning, tool execution, retry/fallback, verification 단계로 남고, 판단 근거는 담당자가 선택해 지식화할 수 있다.
- “RAG인가?”: 발표 표현은 RAG 과장 대신 담당자 통제형 self-loop wiki로 설명한다. 승인된 loop item은 다음 진단 context에 다시 들어간다.
- “PDF인가?”: 현재 제출 시연 기본 산출물은 Markdown이다. PDF는 한글 font embedding과 시각 검증이 끝난 뒤 후속 기능으로만 설명한다.

## 강제 live 검증

운영 서버에 API key와 operation endpoint가 설정된 뒤에는 다음 명령으로 live 성공이 없으면 실패하게 만든다.

```bash
DEMO_REQUIRE_LIVE_PUBLIC_DATA=true \
BASE_URL=https://도메인 \
./scripts/setup-demo-scenario.sh
```

시연 직전에는 `scripts/smoke-prod.sh`도 함께 실행해 인증, 진단, 리포트, public-data scope를 빠르게 점검한다.
