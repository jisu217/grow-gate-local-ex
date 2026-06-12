export const PRODUCTS = [
  {
    id: 'prod-001', name: '리튬이온 배터리팩', category: '전기전자',
    grade: 'A', expiringCerts: 1, updatedAt: '2025-05-28',
    knowledge_status: 'expiring',
    source_basis: 'official',
    missing_fields: ['MSDS 최신본'],
  },
  {
    id: 'prod-002', name: '스마트 센서 모듈', category: '전자부품',
    grade: 'B+', expiringCerts: 0, updatedAt: '2025-04-10',
    knowledge_status: 'needs-check',
    source_basis: 'internal',
    missing_fields: ['CE 인증서', 'RoHS 적합선언'],
  },
]

export const PRODUCT_DETAILS = {
  'prod-001': {
    id: 'prod-001',
    name: '리튬이온 배터리팩',
    category: '전기전자',
    description: 'UN38.3 인증 추진 중인 리튬이온 배터리팩. 독일/EU 수출 목표.',
    grade: 'A',
    knowledge_status: 'expiring',
    source_basis: 'official',
    missing_fields: ['MSDS 최신본', 'CE DoC 초안'],
    updatedAt: '2025-05-28',
    certs: [
      { name: 'UN38.3', expiryDate: '2025-09-30', renewalCycle: '3년', status: '임박', source_type: 'official' },
      { name: 'CE/EMC', expiryDate: '2026-03-15', renewalCycle: '5년', status: '정상', source_type: 'official' },
      { name: 'RoHS',   expiryDate: '2027-01-10', renewalCycle: '5년', status: '정상', source_type: 'internal' },
    ],
    fto: [
      { target: 'EU', status: '검토 중', note: 'BMS 관련 삼성SDI 특허 위험 감지' },
    ],
    buyerRequirements: ['IEC 62133 시험 성적서', 'CE DoC(적합성 선언서)', 'MSDS 최신본'],
    auditLog: [
      {
        date: '2025-05-28', by: '홍길동',
        field: 'UN38.3 만료일',
        oldValue: '2024-09-30',
        newValue: '2025-09-30',
        reason: '인증서 갱신 완료 후 업데이트',
      },
      {
        date: '2025-04-10', by: '홍길동',
        field: '제품 설명',
        oldValue: '리튬이온 배터리팩 (EU 수출 검토 중)',
        newValue: 'UN38.3 인증 추진 중인 리튬이온 배터리팩. 독일/EU 수출 목표.',
        reason: '바이어 미팅 후 설명 구체화',
      },
      {
        date: '2025-03-01', by: '홍길동',
        field: '제품 등록',
        oldValue: null,
        newValue: '리튬이온 배터리팩',
        reason: '신규 제품 지식 등록',
      },
    ],
  },
}
