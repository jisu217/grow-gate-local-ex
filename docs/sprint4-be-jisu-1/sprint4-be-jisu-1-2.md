## BE-02 구현 상태 분류

### 피드백 요청 사항

- 주신 과제: `2) 구현 완료, 부분 구현, 미구현, mock 의존 항목을 표로 분류합니다.`
- 진행 내용: BE-02 구현 상태를 구현 완료, 부분 구현, 미구현, mock 의존 항목으로 분류

### 확인 기준

- 대상 코드: `backend/BE-02/src/main/java/com/growgate`
- 확인 범위: Controller, Service, Repository, DTO, Entity
- 분류 기준: 구현 완료, 부분 구현, 미구현, mock 의존

### 구현 상태 분류 기준

| 상태 | 기준 |
| :--- | :--- |
| 구현 완료 | Controller-Service-Repository 흐름 존재, DB 저장/조회/수정/삭제 연동, 계약 응답 구성 가능 |
| 부분 구현 | 엔드포인트와 기본 흐름 존재, 단 권한/상태 처리/운영 요구 일부 제한 |
| 미구현 | 계약서 또는 기대 기능 기준 Controller/Service 흐름 부재 |
| mock 의존 | 실제 외부 연동/AI 산출 대신 하드코딩 또는 샘플 데이터 기반 응답 |

### 부분 구현 항목

| 항목 | API | 제한 사항 |
| :--- | :--- | :--- |
| 로그아웃 | `POST /api/auth/logout` | 응답 메시지만 반환, JWT 블랙리스트/리프레시 토큰 폐기 처리 없음 |
| 권한 판정 | 제품/지식/진단 쓰기 API | 멤버십 미존재 계정의 기본 역할을 `admin`으로 처리, 개인 계정 권한 정책 명확화 필요 |
| 진단 생성 | `POST /api/diagnosis` | 진단 실행 레코드와 지식 스냅샷 저장은 구현, 실제 AI/규제 데이터 분석 미연동 |
| 진단 목록 조회 | `GET /api/diagnosis` | DB 조회는 구현, 진단 결과 품질은 mock 산출물에 의존 |
| 진단 상세 조회 | `GET /api/diagnosis/{diagnosisId}` | 저장 결과 파싱은 구현, 진단 점수/리스크/로드맵은 mock 산출물 기반 |
| 진단 출처 조회 | `GET /api/diagnosis/{diagnosisId}/sources` | 저장 결과 파싱은 구현, 출처 목록은 mock 산출물 기반 |

### 미구현 항목

| 항목 | API | 비고 |
| :--- | :--- | :--- |
| 실제 AI 진단 엔진 연동 | 진단 API 전체 | 외부 AI/규제/시장/리콜/FTO/지원사업 데이터 연동 부재 |
| 토큰 무효화 저장소 | `POST /api/auth/logout` | 로그아웃 후 기존 JWT 사용 차단 기능 부재 |

### mock 의존 항목

| 항목 | API | mock 의존 내용 |
| :--- | :--- | :--- |
| 진단 비동기 처리 | `POST /api/diagnosis` | `DiagnosisAsyncProcessor`에서 고정 점수, 요약, 리스크, 로드맵, 출처 생성 |
| 진단 결과 상세 | `GET /api/diagnosis/{diagnosisId}` | DB에 저장된 mock 진단 결과 JSON 파싱 후 응답 |
| 진단 출처 조회 | `GET /api/diagnosis/{diagnosisId}/sources` | DB에 저장된 mock 출처 목록 파싱 후 응답 |
