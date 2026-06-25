PRAGMA foreign_keys = ON;

INSERT INTO companies (id, name, business_number, industry, created_at, updated_at)
VALUES
  ('11111111-1111-1111-1111-111111111111', '경남 배터리테크', '615-81-2026', '전기전자', datetime('now'), datetime('now'))
ON CONFLICT(id) DO UPDATE SET
  name = excluded.name,
  industry = excluded.industry,
  updated_at = datetime('now');

INSERT INTO users (id, email, password_hash, name, user_type, created_at, updated_at)
VALUES
  ('22222222-2222-2222-2222-222222222222', 'owner@growgate.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김대표', 'company', datetime('now'), datetime('now')),
  ('33333333-3333-3333-3333-333333333333', 'viewer@growgate.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '박담당', 'company', datetime('now'), datetime('now'))
ON CONFLICT(id) DO UPDATE SET
  name = excluded.name,
  user_type = excluded.user_type,
  updated_at = datetime('now');

INSERT INTO memberships (id, user_id, company_id, role, created_at)
VALUES
  ('44444444-4444-4444-4444-444444444441', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'owner', datetime('now')),
  ('44444444-4444-4444-4444-444444444442', '33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'viewer', datetime('now'))
ON CONFLICT(id) DO UPDATE SET
  role = excluded.role;

INSERT INTO products (id, owner_user_id, company_id, name, category, hs_code, target_country, description, created_at, updated_at)
VALUES
  (
    '55555555-5555-5555-5555-555555555555',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    '리튬이온 배터리팩',
    '전기전자',
    '850760',
    'Germany / EU',
    'EU 수출 준비 중인 산업용 리튬이온 배터리팩. HS Code 850760 기준 인증, 리콜, FTO, 지원사업 진단 테스트용 제품.',
    datetime('now'),
    datetime('now')
  )
ON CONFLICT(id) DO UPDATE SET
  name = excluded.name,
  category = excluded.category,
  hs_code = excluded.hs_code,
  target_country = excluded.target_country,
  description = excluded.description,
  updated_at = datetime('now');

INSERT INTO product_knowledge (id, product_id, trust_grade, source_type, notes, created_by, updated_by, created_at, updated_at)
VALUES
  (
    '66666666-6666-6666-6666-666666666666',
    '55555555-5555-5555-5555-555555555555',
    'high',
    'internal_test_report',
    'BOM, KC, UN38.3 시험성적서 일부 확보. CE/FCC/IEC 62133 추가 확인 필요.',
    '22222222-2222-2222-2222-222222222222',
    '22222222-2222-2222-2222-222222222222',
    datetime('now'),
    datetime('now')
  )
ON CONFLICT(id) DO UPDATE SET
  trust_grade = excluded.trust_grade,
  source_type = excluded.source_type,
  notes = excluded.notes,
  updated_by = excluded.updated_by,
  updated_at = datetime('now');

INSERT INTO certifications (id, product_id, name, issued_at, expires_at, renewal_cycle, created_at)
VALUES
  ('77777777-7777-7777-7777-777777777771', '55555555-5555-5555-5555-555555555555', 'KC 안전확인', '2025-03-01', '2027-03-01', 24, datetime('now')),
  ('77777777-7777-7777-7777-777777777772', '55555555-5555-5555-5555-555555555555', 'UN38.3 운송시험', '2025-04-10', '2027-04-10', 24, datetime('now'))
ON CONFLICT(id) DO UPDATE SET
  name = excluded.name,
  issued_at = excluded.issued_at,
  expires_at = excluded.expires_at,
  renewal_cycle = excluded.renewal_cycle;

INSERT INTO fto (id, product_id, target_country, risk_level, summary, expires_at, created_at)
VALUES
  (
    '88888888-8888-8888-8888-888888888888',
    '55555555-5555-5555-5555-555555555555',
    'Germany / EU',
    'medium',
    'BMS 보호회로, 열관리 구조, 팩 하우징 결합부의 선행특허 추가 검토 필요.',
    '2026-12-31',
    datetime('now')
  )
ON CONFLICT(id) DO UPDATE SET
  target_country = excluded.target_country,
  risk_level = excluded.risk_level,
  summary = excluded.summary,
  expires_at = excluded.expires_at;

INSERT INTO buyer_requirements (id, product_id, buyer_name, country, requirements, created_at)
VALUES
  (
    '99999999-9999-9999-9999-999999999999',
    '55555555-5555-5555-5555-555555555555',
    'EU Mobility Buyer',
    'Germany',
    'CE 적합성 문서, UN38.3, RoHS/REACH 선언, 영문 라벨링 초안, 리콜 대응 프로세스 제출 요청.',
    datetime('now')
  )
ON CONFLICT(id) DO UPDATE SET
  buyer_name = excluded.buyer_name,
  country = excluded.country,
  requirements = excluded.requirements;

INSERT INTO knowledge_sources (id, knowledge_id, url, document_name, retrieved_at)
VALUES
  (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '66666666-6666-6666-6666-666666666666',
    'https://www.safetykorea.kr',
    'Safety Korea 제품안전 리콜 확인',
    date('now')
  )
ON CONFLICT(id) DO UPDATE SET
  url = excluded.url,
  document_name = excluded.document_name,
  retrieved_at = excluded.retrieved_at;

INSERT INTO audit_logs (id, product_id, changed_by, change_type, changed_fields, previous_value, new_value, reason, changed_at)
VALUES
  (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '55555555-5555-5555-5555-555555555555',
    '22222222-2222-2222-2222-222222222222',
    'create',
    'name,category,description,knowledge',
    NULL,
    '{"name":"리튬이온 배터리팩","trustGrade":"high"}',
    'DBeaver 테스트용 초기 제품 지식 등록',
    datetime('now')
  )
ON CONFLICT(id) DO UPDATE SET
  new_value = excluded.new_value,
  reason = excluded.reason,
  changed_at = datetime('now');

INSERT INTO diagnosis_runs (
  id,
  user_id,
  company_id,
  product_id,
  input_data,
  knowledge_snapshot,
  result,
  status,
  target_country,
  created_at
)
VALUES
  (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    '55555555-5555-5555-5555-555555555555',
    NULL,
    '{"productName":"리튬이온 배터리팩","certifications":["KC 안전확인","UN38.3 운송시험"],"trustGrade":"high"}',
    '{"summary":"EU 수출 준비도는 양호하나 CE/RoHS/REACH 문서 보완이 필요합니다.","diagnosisScore":{"market":82,"regulation":74,"recall":69,"fto":63,"support":78,"overall":73},"riskItems":[{"type":"regulation","level":"medium","message":"CE 적합성 문서 보완 필요"}],"actionRoadmap":{"days30":["CE/RoHS/REACH 갭 분석"],"days60":["시험기관 견적 및 샘플 제출"],"days90":["영문 기술문서 패키지 제출"]},"evidenceSources":["Safety Korea","KOTRA","KIPRIS"]}',
    'done',
    'Germany / EU',
    datetime('now')
  )
ON CONFLICT(id) DO UPDATE SET
  knowledge_snapshot = excluded.knowledge_snapshot,
  result = excluded.result,
  status = excluded.status,
  target_country = excluded.target_country;
