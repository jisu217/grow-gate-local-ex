(function () {
  const products = [
    {
      productId: 'prod-full',
      name: '리튬이온 배터리팩',
      category: '전기전자',
      hsCode: '850760',
      targetCountry: 'DE',
      description: '산업용 리튬이온 배터리팩',
      trustGrade: 'high',
      knowledgeStatus: 'valid',
      certificationCount: 1,
      ftoCount: 1,
      buyerRequirementCount: 1,
      expiringCertCount: 0,
      missingFieldCount: 0,
      updatedAt: '2026-07-04T09:00:00Z',
    },
    {
      productId: 'prod-mid',
      name: '웨어러블 의료 패치',
      category: '식품·의료',
      hsCode: '901890',
      targetCountry: 'JP',
      description: '일본 수출용 웨어러블 의료 패치',
      trustGrade: 'medium',
      knowledgeStatus: 'needs_review',
      certificationCount: 1,
      ftoCount: 1,
      buyerRequirementCount: 0,
      expiringCertCount: 0,
      missingFieldCount: 1,
      buyerName: 'Tokyo Med',
      buyerCountry: 'JP',
      buyerRequest: 'PMDA 분류와 QMS 증빙 요청',
      updatedAt: '2026-07-04T08:30:00Z',
    },
    {
      productId: 'prod-low',
      name: '스마트 전동밸브',
      category: '기계·장비',
      hsCode: '848180',
      targetCountry: 'US',
      description: '',
      trustGrade: null,
      knowledgeStatus: 'incomplete',
      certificationCount: 0,
      ftoCount: 0,
      buyerRequirementCount: 0,
      expiringCertCount: 0,
      missingFieldCount: 2,
      updatedAt: '2026-07-04T07:20:00Z',
    },
  ]

  const responses = {
    '/api/users/me': {
      userId: 'user-visual',
      userType: 'company',
      role: 'owner',
      companyId: 'company-visual',
      companyName: '창원에너지팩',
      name: '김담당',
      email: 'demo@growgate.local',
    },
    '/api/companies/me': {
      name: '창원에너지팩',
      industry: '이차전지 부품',
      businessNumber: '123-45-67890',
    },
    '/api/companies/me/members': {
      members: [
        { name: '김담당', role: 'owner' },
        { name: '박검토', role: 'editor' },
      ],
    },
    '/api/products': { products },
    '/api/diagnosis': {
      diagnoses: [
        {
          id: 'diag-1',
          productId: 'prod-full',
          productName: '리튬이온 배터리팩',
          targetCountry: 'DE',
          status: 'done',
          createdAt: '2026-07-04T09:10:00Z',
          overallScore: 82,
        },
        {
          id: 'diag-2',
          productId: 'prod-mid',
          productName: '웨어러블 의료 패치',
          targetCountry: 'JP',
          status: 'done',
          createdAt: '2026-07-04T08:40:00Z',
          overallScore: 71,
        },
      ],
    },
    '/api/products/prod-mid/knowledge': {
      productId: 'prod-mid',
      name: '웨어러블 의료 패치',
      category: '식품·의료',
      hsCode: '901890',
      targetCountry: 'JP',
      description: '일본 수출용 웨어러블 의료 패치',
      knowledge: { trustGrade: 'medium', sourceType: 'official' },
      certifications: [
        {
          id: 'cert-1',
          name: 'ISO 13485',
          expiresAt: '2027-02-01',
          renewalCycle: 12,
          daysUntilExpiry: 212,
        },
      ],
      fto: [
        {
          id: 'fto-1',
          targetCountry: 'JP',
          riskLevel: 'medium',
          summary: '유사 특허 검토 필요',
        },
      ],
      buyerRequirements: [],
      updatedAt: '2026-07-04T08:30:00Z',
    },
    '/api/products/prod-mid/audit-logs': {
      logs: [
        {
          changedAt: '2026-07-04T08:30:00Z',
          changedBy: '김담당',
          changedFields: ['buyerRequirements'],
          previousValue: '',
          newValue: 'PMDA 요구사항 추가 예정',
          reason: '바이어 요구사항 보완',
        },
      ],
    },
    '/api/products/prod-mid/knowledge/loop-items': { items: [] },
  }

  const originalFetch = window.fetch.bind(window)
  window.fetch = function mockedFetch(input, init) {
    const url = typeof input === 'string' ? input : input && input.url
    const match = Object.keys(responses).find((path) => url && url.endsWith(path))
    if (!match) return originalFetch(input, init)

    return Promise.resolve(new Response(JSON.stringify(responses[match]), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }))
  }
})()
