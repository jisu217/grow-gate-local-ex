USE growgate;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- synthetic, not real customer data, persona-based validation
-- This seed is not real customer interview evidence.

CREATE TABLE IF NOT EXISTS synthetic_validation_personas (
  id VARCHAR(40) NOT NULL PRIMARY KEY,
  company_scenario VARCHAR(120) NOT NULL,
  role_name VARCHAR(120) NOT NULL,
  core_problem TEXT NOT NULL,
  validation_focus TEXT NOT NULL,
  synthetic_disclaimer BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS synthetic_validation_sessions (
  id VARCHAR(40) NOT NULL PRIMARY KEY,
  persona_id VARCHAR(40) NOT NULL,
  company_scenario VARCHAR(120) NOT NULL,
  task_id VARCHAR(40) NOT NULL,
  scenario_id VARCHAR(80) NOT NULL,
  baseline_manual_time_min INT NOT NULL,
  growgate_time_min INT NOT NULL,
  task_success BOOLEAN NOT NULL,
  confidence_before INT NOT NULL,
  confidence_after INT NOT NULL,
  source_trust_score INT NOT NULL,
  report_usefulness INT NOT NULL,
  willingness_to_pay_monthly_krw INT NOT NULL,
  top_objection TEXT,
  synthetic_disclaimer BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_validation_persona (persona_id),
  KEY idx_validation_scenario (scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS synthetic_validation_findings (
  id VARCHAR(40) NOT NULL PRIMARY KEY,
  persona_id VARCHAR(40) NOT NULL,
  severity VARCHAR(40) NOT NULL,
  finding TEXT NOT NULL,
  recommended_fix TEXT NOT NULL,
  synthetic_disclaimer BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_validation_finding_persona (persona_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO synthetic_validation_personas
(id, company_scenario, role_name, core_problem, validation_focus, synthetic_disclaimer)
VALUES
('PV-01','창원에너지팩','해외영업팀장 김민재','독일 바이어가 배터리팩 견적을 요청했지만 CE/UN38.3/리콜/FTO 확인에 시간이 오래 걸림','synthetic, not real customer data, persona-based validation; Gate 판단, Safety/KOTRA/KIPRIS 출처, buyer reply draft',TRUE),
('PV-02','창원에너지팩','품질인증 담당 오수진','인증서와 시험성적서가 여러 폴더에 흩어져 있고 최신성 관리가 안 됨','synthetic, not real customer data, persona-based validation; 문서 분류 Agent, 인증 누락 risk, Markdown report',TRUE),
('PV-03','부산마린컨트롤','대표 박도현','미국 조선 기자재 납품 기회를 놓치고 싶지 않지만 납기·인증·지원사업 확인 인력이 부족','synthetic, not real customer data, persona-based validation; 30/60/90 roadmap, Bizinfo 지원사업, time saved estimate',TRUE),
('PV-04','대구메디센서','R&D/IP 책임 이준호','일본 의료·헬스케어 시장 진입 전 특허/FTO red flag가 걱정됨','synthetic, not real customer data, persona-based validation; KIPRIS FTO, source-bound risk explanation, STOP/HOLD 방어',TRUE),
('PV-05','지역 수출지원센터','컨설턴트 정하윤','기업마다 KOTRA/KIPRIS/Bizinfo/SafetyKorea를 따로 확인해 상담 시간이 길어짐','synthetic, not real customer data, persona-based validation; 여러 기업 비교, public-data proof, consultant mode',TRUE)
ON DUPLICATE KEY UPDATE
  company_scenario=VALUES(company_scenario),
  role_name=VALUES(role_name),
  core_problem=VALUES(core_problem),
  validation_focus=VALUES(validation_focus),
  synthetic_disclaimer=TRUE;

INSERT INTO synthetic_validation_sessions
(id, persona_id, company_scenario, task_id, scenario_id, baseline_manual_time_min, growgate_time_min,
 task_success, confidence_before, confidence_after, source_trust_score, report_usefulness,
 willingness_to_pay_monthly_krw, top_objection, synthetic_disclaimer)
VALUES
('SV-001','PV-01','COMP-BATTERY','TASK-A','DE-BATTERY-850760',180,12,TRUE,2,4,4,5,150000,'synthetic, not real customer data, persona-based validation; KIPRIS 결과가 실제 특허 법률검토를 대체하지 않는다는 안내가 더 명확해야 함',TRUE),
('SV-002','PV-01','COMP-BATTERY','TASK-B','DE-BATTERY-850760',70,8,TRUE,2,4,4,4,150000,'synthetic, not real customer data, persona-based validation; raw hash가 무엇인지 도움말 필요',TRUE),
('SV-003','PV-01','COMP-BATTERY','TASK-C','DE-BATTERY-850760',120,18,TRUE,3,4,3,5,150000,'synthetic, not real customer data, persona-based validation; 바이어 회신 문구를 더 짧게',TRUE),
('SV-004','PV-02','COMP-BATTERY','TASK-A','DE-BATTERY-850760',160,14,TRUE,2,4,3,4,120000,'synthetic, not real customer data, persona-based validation; 인증서 만료일 입력 UX 보강 필요',TRUE),
('SV-005','PV-02','COMP-BATTERY','TASK-B','DE-BATTERY-850760',90,10,TRUE,2,4,4,4,120000,'synthetic, not real customer data, persona-based validation; fallback 영향이 점수에 어떻게 반영되는지 더 명확히',TRUE),
('SV-006','PV-02','COMP-BATTERY','TASK-C','DE-BATTERY-850760',150,22,TRUE,3,4,3,5,120000,'synthetic, not real customer data, persona-based validation; Markdown 편집 미리보기 필요',TRUE),
('SV-007','PV-03','COMP-MARINE','TASK-A','US-MARINE-848180',210,15,TRUE,2,5,4,5,200000,'synthetic, not real customer data, persona-based validation; 지원사업 추천의 신청 마감일 강조 필요',TRUE),
('SV-008','PV-03','COMP-MARINE','TASK-B','US-MARINE-848180',100,9,TRUE,2,4,4,4,200000,'synthetic, not real customer data, persona-based validation; KOTRA live proof가 없으면 경고가 더 커야 함',TRUE),
('SV-009','PV-03','COMP-MARINE','TASK-C','US-MARINE-848180',130,19,TRUE,3,5,4,5,200000,'synthetic, not real customer data, persona-based validation; 대표용 요약 1페이지 필요',TRUE),
('SV-010','PV-04','COMP-MEDICAL','TASK-A','JP-MEDICAL-902519',240,15,TRUE,1,4,3,4,180000,'synthetic, not real customer data, persona-based validation; PMDA와 FTO 전문가 검토 필요 문구가 더 앞에 와야 함',TRUE),
('SV-011','PV-04','COMP-MEDICAL','TASK-B','JP-MEDICAL-902519',130,12,TRUE,1,4,4,4,180000,'synthetic, not real customer data, persona-based validation; 특허 검색 결과의 한계 설명 필요',TRUE),
('SV-012','PV-04','COMP-MEDICAL','TASK-C','JP-MEDICAL-902519',160,25,TRUE,2,4,3,4,180000,'synthetic, not real customer data, persona-based validation; STOP일 때 바이어 대응 문구가 더 조심스러워야 함',TRUE),
('SV-013','PV-05','CONSULTANT','TASK-A','MULTI-COMPANY',180,16,TRUE,2,4,4,4,250000,'synthetic, not real customer data, persona-based validation; 기업별 비교 화면 필요',TRUE),
('SV-014','PV-05','CONSULTANT','TASK-B','MULTI-COMPANY',150,14,TRUE,2,5,5,4,250000,'synthetic, not real customer data, persona-based validation; public-data proof pack export가 필요',TRUE),
('SV-015','PV-05','CONSULTANT','TASK-C','MULTI-COMPANY',120,20,TRUE,3,4,4,5,250000,'synthetic, not real customer data, persona-based validation; 상담 리포트 템플릿 커스터마이징 필요',TRUE)
ON DUPLICATE KEY UPDATE
  baseline_manual_time_min=VALUES(baseline_manual_time_min),
  growgate_time_min=VALUES(growgate_time_min),
  task_success=VALUES(task_success),
  confidence_before=VALUES(confidence_before),
  confidence_after=VALUES(confidence_after),
  source_trust_score=VALUES(source_trust_score),
  report_usefulness=VALUES(report_usefulness),
  willingness_to_pay_monthly_krw=VALUES(willingness_to_pay_monthly_krw),
  top_objection=VALUES(top_objection),
  synthetic_disclaimer=TRUE;

INSERT INTO synthetic_validation_findings
(id, persona_id, severity, finding, recommended_fix, synthetic_disclaimer)
VALUES
('VF-001','PV-01','medium','synthetic, not real customer data, persona-based validation; KIPRIS/FTO 설명이 법률검토 대체처럼 보일 수 있음','FTO expert review limitation을 Gate 상단과 보고서 앞부분에 표시',TRUE),
('VF-002','PV-03','medium','synthetic, not real customer data, persona-based validation; 지원사업 추천은 마감일과 담당기관 연결이 부족하면 실행력이 떨어짐','Bizinfo source row에 due date, agency, action owner를 함께 표시',TRUE),
('VF-003','PV-05','high','synthetic, not real customer data, persona-based validation; 심사위원과 컨설턴트는 live/cache/fallback 차이를 바로 확인하려 함','S10 Public Data Proof tab과 proof pack export를 우선 시연',TRUE)
ON DUPLICATE KEY UPDATE
  finding=VALUES(finding),
  recommended_fix=VALUES(recommended_fix),
  synthetic_disclaimer=TRUE;
