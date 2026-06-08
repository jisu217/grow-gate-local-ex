### 1. 인증
| # | 메서드 | 엔드포인트 | 설명 |
| :--- | :--- | :--- | :--- | 
| 1 | POST | /api/auth/login | 로그인 |
| 2 | POST | /api/auth/logout | 로그아웃 |
| 3 | POST | /api/auth/signup/personal | 개인 회원가입 |
| 4 | POST | /api/auth/signup/company | 기업 회원가입 |

### 2. 사용자 및 회사
| # | 메서드 | 엔드포인트 | 설명 |
| :--- | :--- | :--- | :--- | 
| 5 | GET | /api/users/me | 내 계정 정보 조회 |
| 6 | PATCH | /api/users/me | 내 계정 정보 수정 |
| 7 | GET | /api/companies/me | 내 회사 정보 조회 |
| 8 | PATCH | /api/companies/me | 내 회사 정보 수정 |

### 3. AI 진단 
| # | 메서드 | 엔드포인트 | 설명 |
| :--- | :--- | :--- | :--- |
| 9 | GET | /api/diagnosis | 내 AI 진단 이력 목록 조회 |
| 10 | POST | /api/diagnosis | AI 진단 신규 요청 |
| 11 | GET | /api/diagnosis/:id | 특정 진단 결과 조회 |
| 12 | GET | /api/diagnosis/:id/sources | 진단 결과 근거 / 출처 조회 |

### 3-1. 진단 세부 항목 (수정 중)
| # | 메서드 | 엔드포인트 | 설명 |
| :--- | :--- | :--- | :--- |
| 13 | GET | /api/diagnosis/:id/readiness | 수출 준비도 종합 점수 |
| 14 | GET | /api/diagnosis/:id/regulations | 규제·인증 분석 결과 |
| 15 | GET | /api/diagnosis/:id/market | 시장 분석 결과 |
| 16 | GET | /api/diagnosis/:id/recall | 리콜 위험 분석 결과 |
| 17 | GET | /api/diagnosis/:id/fto | 특허(FTO) 위험 분석 결과 |
| 18 | GET | /api/diagnosis/:id/support | 매칭 지원사업 목록 |
| 19 | GET | /api/diagnosis/:id/roadmap | 실행 로드맵 (30/60/90일) |

### 4. 마이페이지 
| # | 메서드 | 엔드포인트 | 설명 |
| :--- | :--- | :--- | :--- |
| 20 | GET | /api/mypage | 마이페이지 기본 정보 |
| 21 | PATCH | /api/mypage | 마이페이지 수정 |

### 4-1. 제품 지식 관리 
| # | 메서드 | 엔드포인트 | 설명 |
| :--- | :--- | :--- | :--- |
| 22 | GET | /api/mypage/knowledge | 등록 제품 목록 조회 |
| 23 | POST | /api/mypage/knowledge | 신규 제품 등록 |
| 24 | GET | /api/mypage/knowledge/:productId | 제품 상세 조회 |
| 25 | PATCH | /api/mypage/knowledge/:productId | 제품 정보 수정 |
| 26 | DELETE | /api/mypage/knowledge/:productId | 제품 삭제 |
| 27 | GET | /api/mypage/knowledge/:productId/logs | 제품 수정 이력 조회 |