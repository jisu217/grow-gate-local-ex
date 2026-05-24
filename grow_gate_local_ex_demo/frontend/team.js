const dataSources = [
  {
    id: "kcs_trade_gw",
    name: "관세청 수출입무역통계 GW",
    provider: "Korea Customs Service",
    table: "trade_signal",
    screens: "S04, S09",
    status: "활용 중",
    reliability: 86,
    insight: "HS 850760과 대상국 기준 시장성 점수의 핵심 근거입니다. 월 단위 업데이트라 실시간 수요 급변은 보조 데이터가 필요합니다.",
    usage: ["시장성 점수와 수출기회 카드의 수입 증가율 근거", "Germany/EU 기회 점수 91의 seed 기준값", "리포트 출력 시 evidence_source로 추적"],
    codeRefs: ["backend/public_data_pipeline.py: trade_signal 정규화", "backend/agents.py: MarketScoringAgent", "frontend/app.js: 시장성 카드 렌더링"],
    dataShape: ["country_code: DE", "hs_code: 850760", "import_growth_yoy: 14.7", "tariff: 0.0", "opportunity_score: 91"],
    stats: { records: "1 seed", updateCycle: "monthly", targetTable: "trade_signal", screens: "S04, S09" }
  },
  {
    id: "kotra_supply_chain",
    name: "KOTRA 글로벌 공급망 인사이트",
    provider: "KOTRA",
    table: "country_profile",
    screens: "S04, S08, S09",
    status: "활용 중",
    reliability: 78,
    insight: "EU 배터리 규제와 바이어 요구를 로드맵 메모로 전환합니다. 정량 수치보다 정책·시장 설명력에 강합니다.",
    usage: ["공급망·정책 압력을 국가 프로필로 반영", "바이어가 선호하는 인증 문서 패키지 설명", "30·60·90일 로드맵의 시장 메모 근거"],
    codeRefs: ["backend/public_data_pipeline.py: country_profile 정규화", "backend/data_connectors.py: public data facade", "docs/architecture.md: 데이터 파이프라인 설명"],
    dataShape: ["country_code: DE", "region: EU", "market_note: EU 배터리 규제 증가", "regulatory_pressure: 0.92", "buyer_note: CE/UN38.3 선호"],
    stats: { records: "1 seed", updateCycle: "ad-hoc", targetTable: "country_profile", screens: "S04, S08, S09" }
  },
  {
    id: "safety_korea_cert_recall",
    name: "Safety Korea 제품 안전인증 및 리콜 정보",
    provider: "KATS Safety Korea",
    table: "recall_case",
    screens: "S05, S06, S08",
    status: "활용 중",
    reliability: 88,
    insight: "유사 리콜을 시험 전 체크리스트로 바꾸는 차별화 근거입니다. 해외 리콜 데이터와 결합하면 설득력이 더 커집니다.",
    usage: ["배터리팩 유사 리콜 위험 Copilot의 핵심 사례", "낙하·진동·충격·충방전 시험 체크리스트 생성", "규제·인증 패스포트와 실행 보드의 안전 리스크 근거"],
    codeRefs: ["backend/public_data_pipeline.py: recall_case 정규화", "backend/ml_models.py: 리콜 유사도 및 위험도 계산", "frontend/app.js: 리콜/FMEA 카드 렌더링"],
    dataShape: ["product: 전동공구 배터리팩", "category: battery", "hazard: 내부 단락 및 온도상승", "severity: 0.88", "recommended_tests: 4개"],
    stats: { records: "1 seed", updateCycle: "near-real-time", targetTable: "recall_case", screens: "S05, S06, S08, S09" }
  },
  {
    id: "kipris_plus_patent",
    name: "KIPRIS Plus Open API",
    provider: "KIPRIS Plus",
    table: "patent_signal",
    screens: "S07, S09",
    status: "활용 중",
    reliability: 80,
    insight: "BMS, 보호회로, 열관리 키워드의 FTO 1차 위험도를 설명합니다. 법률 의견이 아니라 선행 위험 탐색으로 표현해야 안전합니다.",
    usage: ["BMS·보호회로·열관리 특허 밀집도 산출", "FTO 위험지도에서 1차 위험 키워드 제시", "지원사업·로드맵에서 선행기술 검토 태스크 생성"],
    codeRefs: ["backend/public_data_pipeline.py: patent_signal 정규화", "backend/ml_models.py: FTO 위험 산정", "backend/agents.py: FTORiskAgent"],
    dataShape: ["keyword: battery management system", "ipc: H02J/H01M", "filings: 9630", "recent_growth: 0.22", "risk: 0.81"],
    stats: { records: "1 seed", updateCycle: "daily", targetTable: "patent_signal", screens: "S07, S08, S09" }
  },
  {
    id: "export_voucher_support",
    name: "수출바우처·해외규격인증·지역TP 공고",
    provider: "MOTIE/MSS/KOTRA/Regional TP",
    table: "support_program",
    screens: "S07, S08",
    status: "활용 중",
    reliability: 74,
    insight: "기업 지역과 인증 갭을 지원사업 우선순위로 바꿉니다. 공고 마감일 자동 동기화가 다음 보강 포인트입니다.",
    usage: ["인증 갭과 지역 정보를 지원사업 적합도 점수로 변환", "해외규격인증획득지원사업을 우선 추천", "30일 로드맵의 신청 가능성 확인 태스크 생성"],
    codeRefs: ["backend/public_data_pipeline.py: support_program 정규화", "backend/agents.py: SupportMatchingAgent", "frontend/app.js: 지원사업 매칭 카드"],
    dataShape: ["name: 해외규격인증획득지원사업", "agency: 중기부/유관기관", "benefit: 인증 비용 일부 지원", "deadline: 공고별 상이", "fit_tags: CE/IEC/SME"],
    stats: { records: "1 seed", updateCycle: "weekly", targetTable: "support_program", screens: "S07, S08, S09" }
  }
];

const missingData = [
  { title: "해외 리콜·사고 데이터", impact: "EU/US 실제 리콜까지 연결하면 심사위원 설득력이 상승합니다.", priority: "High", owner: "데이터" },
  { title: "시험기관 견적·리드타임", impact: "로드맵이 예측이 아니라 실행 일정으로 보입니다.", priority: "High", owner: "인증" },
  { title: "지역기업 인터뷰 3건", impact: "현장 문제 정의와 UX 타당성을 보강합니다.", priority: "High", owner: "기획" },
  { title: "지원사업 마감 캘린더", impact: "추천 기능이 실제 신청 행동으로 이어집니다.", priority: "Medium", owner: "운영" },
  { title: "심사 기준표 매핑", impact: "공모전 점수 상승 행동을 더 정확히 계산할 수 있습니다.", priority: "Medium", owner: "발표" }
];

const stakeholderScores = [
  { role: "공모전 심사위원", group: "평가", score: 86, confidence: "중상", evidence: "공공데이터 출처와 화면별 활용 흐름이 명확합니다.", concern: "실제 사용자 검증 수치가 아직 약합니다.", improvement: "기업 인터뷰와 전후 비교 지표를 추가하세요." },
  { role: "박사급 실무자", group: "전문", score: 82, confidence: "중", evidence: "규제·리콜·특허·지원사업을 제품 단위로 묶은 구조가 타당합니다.", concern: "예측 모델 설명과 검증 데이터가 제한적입니다.", improvement: "모델 입력 특성과 fallback 기준을 한 장으로 설명하세요." },
  { role: "수출지원 업무 담당자", group: "실무", score: 88, confidence: "중상", evidence: "30·60·90일 산출물 중심 로드맵이 업무 방식과 맞습니다.", concern: "기관 상담 연결과 문서 첨부 흐름이 빠져 있습니다.", improvement: "상담 요청 버튼과 필요 서류 체크리스트를 보강하세요." },
  { role: "중소 CEO", group: "경영", score: 84, confidence: "중", evidence: "무엇을 먼저 해야 하는지 비용·일정 관점으로 보입니다.", concern: "예상 비용과 지원금 규모가 더 필요합니다.", improvement: "시험·인증 예상 비용 범위를 카드로 추가하세요." },
  { role: "대기업 CEO", group: "경영", score: 76, confidence: "중", evidence: "공급망·규제 리스크 통합 관점은 의미가 있습니다.", concern: "엔터프라이즈 권한, 감사, 다국가 운영 관점은 부족합니다.", improvement: "확장 아키텍처와 감사 로그 로드맵을 별도로 제시하세요." },
  { role: "장관·정책 의사결정자", group: "정책", score: 85, confidence: "중", evidence: "지역 제조기업 수출 병목을 공공데이터 활용 사례로 잘 보여줍니다.", concern: "정책 효과 지표가 정량화되지 않았습니다.", improvement: "지역별 인증 리드타임 단축 KPI를 추가하세요." },
  { role: "학부생", group: "학습", score: 90, confidence: "상", evidence: "데이터 흐름과 화면 구조가 직관적이라 발표 이해가 쉽습니다.", concern: "전문 용어가 많아 초반 설명이 필요합니다.", improvement: "첫 화면에 용어 도움말을 넣으세요." },
  { role: "대학원생", group: "연구", score: 80, confidence: "중", evidence: "문제 정의와 에이전트 분해가 연구 주제로 확장 가능합니다.", concern: "실험 설계와 비교군이 부족합니다.", improvement: "기존 서비스 대비 의사결정 시간 단축 실험을 설계하세요." },
  { role: "수출업자", group: "사용자", score: 87, confidence: "중상", evidence: "HS Code, 인증, 물류, 지원사업이 한 화면에서 연결됩니다.", concern: "국가별 바이어 발굴 데이터가 더 있으면 좋습니다.", improvement: "KOTRA 무역관 상담 연결을 액션으로 추가하세요." },
  { role: "수입업자", group: "사용자", score: 73, confidence: "중", evidence: "공급업체의 인증 준비도를 확인하는 용도는 있습니다.", concern: "수입자 관점의 통관·책임소재 정보는 부족합니다.", improvement: "수입자 체크리스트와 책임 문서 항목을 추가하세요." },
  { role: "인증 시험기관", group: "전문", score: 79, confidence: "중", evidence: "시험 항목과 준비 문서를 먼저 정리하는 흐름이 좋습니다.", concern: "시험 규격 버전과 샘플 조건이 더 구체적이어야 합니다.", improvement: "규격 버전, 샘플 수량, 전처리 조건 필드를 추가하세요." },
  { role: "투자자", group: "사업", score: 78, confidence: "중", evidence: "공공데이터 기반 B2B SaaS 확장 가능성이 보입니다.", concern: "반복 사용과 과금 지점이 아직 약합니다.", improvement: "기관 관리자 대시보드와 기업별 이력 저장 로드맵을 제시하세요." },
  { role: "지역 지자체 담당자", group: "정책", score: 89, confidence: "중상", evidence: "지역 제조기업 지원사업과 실제 수출 준비 업무가 직접 연결됩니다.", concern: "지역별 산업군 템플릿이 더 있으면 도입 설득이 쉽습니다.", improvement: "배터리 외 기계·소재·생활용품 데모 케이스를 추가하세요." }
];

const awardFactors = [
  { label: "공공데이터 활용성", value: 88, type: "positive", text: "5개 출처가 화면별 기능과 연결되어 있습니다." },
  { label: "문제 적합성", value: 86, type: "positive", text: "지역 제조기업의 수출 규제·인증 병목을 직접 겨냥합니다." },
  { label: "구현 완성도", value: 82, type: "positive", text: "정적 프론트와 FastAPI 백엔드가 함께 준비되어 있습니다." },
  { label: "실증 근거", value: 58, type: "risk", text: "실제 기업 인터뷰, 테스트 로그, 효과 지표가 더 필요합니다." },
  { label: "발표 차별성", value: 78, type: "positive", text: "AI 에이전트와 30·60·90일 실행 로드맵이 강한 스토리입니다." }
];

const actionSimulations = [
  { id: "interview", title: "지역기업 인터뷰 3건 추가", effect: "도움", delta: 7, type: "positive", effort: "반나절", owner: "기획", reason: "심사에서 가장 약한 실증 근거를 직접 보강합니다. 문제 정의가 실제 현장 기반이라는 증거가 생깁니다." },
  { id: "foreign-recall", title: "EU/US 리콜 데이터 연결", effect: "도움", delta: 5, type: "positive", effort: "1일", owner: "데이터", reason: "국내 Safety Korea 근거를 해외 시장 리스크와 연결해 수출 서비스로서의 신뢰도를 높입니다." },
  { id: "more-animation", title: "화려한 애니메이션 추가", effect: "감점 가능", delta: -3, type: "negative", effort: "반나절", owner: "프론트", reason: "운영형 B2B 대시보드에서는 장식보다 근거와 실행성이 중요합니다. 발표 집중도를 분산시킬 수 있습니다." },
  { id: "cost-range", title: "시험·인증 비용 범위 추가", effect: "도움", delta: 4, type: "positive", effort: "반나절", owner: "인증", reason: "중소 CEO와 실무자가 바로 판단할 수 있는 비용 감각을 제공합니다." },
  { id: "llm-copy", title: "모든 설명을 AI 문장으로 길게 확장", effect: "감점 가능", delta: -4, type: "negative", effort: "반나절", owner: "발표", reason: "심사위원은 긴 설명보다 근거, 점수, 행동을 빠르게 봅니다. 장황한 문장은 완성도를 낮춰 보이게 합니다." },
  { id: "judge-map", title: "심사 기준표와 기능 매핑", effect: "도움", delta: 6, type: "positive", effort: "2시간", owner: "PM", reason: "어떤 기능이 어떤 평가 항목 점수를 올리는지 명확해져 발표 전략과 우선순위가 정리됩니다." }
];

const codeGate = {
  reviewFocus: [
    { title: "데이터 근거 표현", text: "공공데이터 출처, 정규화 테이블, 화면 연결이 과장 없이 보이는지 확인합니다." },
    { title: "Git 동작 안전성", text: "정적 페이지 버튼이 실제 커밋·철회를 실행하는 것처럼 보이지 않는지 확인합니다." },
    { title: "반응형 가독성", text: "역할자 점수 카드와 체크리스트가 모바일에서 겹치지 않는지 확인합니다." }
  ],
  reviewChecklist: [
    "CSP를 깨는 inline script가 없는가",
    "수상 확률이 확정 예측처럼 표현되지 않는가",
    "공공데이터 출처와 활용 화면이 연결되는가",
    "코드 게이트 버튼이 UI 상태 표시임을 명확히 말하는가",
    "모바일 폭에서 긴 한국어 문장이 카드 밖으로 넘치지 않는가",
    "발표에 불필요한 장식보다 판단 정보가 우선되는가"
  ],
  preCommitChecklist: [
    "team.html 직접 열기 확인",
    "code-gate.html 직접 열기 확인",
    "모든 액션 카드 클릭 확인",
    "코드 게이트 버튼과 체크박스 동작 확인",
    "미완성 표식과 오해 소지 문구 검색",
    "변경 파일 목록과 커밋 메시지 확인"
  ],
  commitCommands: [
    "git status --short",
    "git diff -- grow_gate_local_ex_demo/frontend",
    "git add grow_gate_local_ex_demo/frontend/team.html grow_gate_local_ex_demo/frontend/code-gate.html grow_gate_local_ex_demo/frontend/team.js grow_gate_local_ex_demo/frontend/styles.css",
    "git commit -m \"feat: add team operations dashboards\""
  ],
  rollbackCommands: [
    "git log --oneline -5",
    "git revert <commit-sha>",
    "git status --short"
  ],
  rollbackVersions: [
    { name: "설계 승인 전", risk: "낮음", use: "대시보드 방향 자체를 바꿀 때 사용합니다." },
    { name: "팀 운영실 추가 전", risk: "중간", use: "전략 대시보드만 문제가 있을 때 사용합니다." },
    { name: "최종 커밋 전", risk: "낮음", use: "시각 QA 후 마지막 수정만 되돌릴 때 사용합니다." }
  ]
};

const gateState = {
  reviewRequested: false,
  reviewCompleted: false,
  preCommitChecked: false,
  commitReady: false,
  rollbackOpen: false
};

function qs(selector) {
  return document.querySelector(selector);
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function renderTeamDashboard() {
  renderDataSources();
  renderMissingData();
  renderStakeholderScores();
  renderAwardFactors();
  renderActionSimulator();
  selectDataSource(dataSources[0].id);
}

function renderDataSources() {
  const target = qs("#teamDataSources");
  if (!target) return;
  target.innerHTML = dataSources.map((source) => `
    <article class="source-row" role="button" tabindex="0" data-source-id="${escapeHtml(source.id)}" aria-label="${escapeHtml(source.name)} 상세 보기">
      <div>
        <span class="source-status">${escapeHtml(source.status)}</span>
        <h3>${escapeHtml(source.name)}</h3>
        <p>${escapeHtml(source.insight)}</p>
      </div>
      <dl>
        <dt>제공</dt><dd>${escapeHtml(source.provider)}</dd>
        <dt>테이블</dt><dd>${escapeHtml(source.table)}</dd>
        <dt>화면</dt><dd>${escapeHtml(source.screens)}</dd>
      </dl>
      <div class="mini-score" style="--value:${source.reliability}%"><b>${source.reliability}</b><span>신뢰</span></div>
    </article>
  `).join("");
  target.querySelectorAll("[data-source-id]").forEach((row) => {
    row.addEventListener("click", () => selectDataSource(row.dataset.sourceId));
    row.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        selectDataSource(row.dataset.sourceId);
      }
    });
  });
}

function selectDataSource(id) {
  const source = dataSources.find((item) => item.id === id) || dataSources[0];
  const target = qs("#sourceDetail");
  if (!target) return;
  qs("#teamDataSources")?.querySelectorAll("[data-source-id]").forEach((row) => {
    row.classList.toggle("selected", row.dataset.sourceId === source.id);
  });
  target.innerHTML = `
    <div class="panel-heading horizontal">
      <div>
        <p class="eyebrow">SOURCE DETAIL</p>
        <h2>${escapeHtml(source.name)}</h2>
      </div>
      <span class="badge">${escapeHtml(source.table)}</span>
    </div>
    <div class="source-detail-summary">
      <article><span>신뢰도</span><strong>${source.reliability}</strong><p>seed와 공식 출처 매핑 기준</p></article>
      <article><span>정규화</span><strong>${escapeHtml(source.stats.targetTable)}</strong><p>Agent 입력 계약</p></article>
      <article><span>연결 화면</span><strong>${escapeHtml(source.stats.screens)}</strong><p>근거 표시 위치</p></article>
    </div>
    <div class="source-detail-grid">
      <article class="source-detail-card">
        <h3>활용 내역 요약</h3>
        <ul>${source.usage.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>
      </article>
      <article class="source-detail-card">
        <h3>실제 코드 연결</h3>
        <ul>${source.codeRefs.map((item) => `<li><code>${escapeHtml(item)}</code></li>`).join("")}</ul>
      </article>
      <article class="source-detail-card">
        <h3>데이터 형태</h3>
        <pre><code>${escapeHtml(source.dataShape.join("\n"))}</code></pre>
      </article>
      <article class="source-detail-card">
        <h3>간단 통계</h3>
        <dl>
          <dt>레코드</dt><dd>${escapeHtml(source.stats.records)}</dd>
          <dt>갱신</dt><dd>${escapeHtml(source.stats.updateCycle)}</dd>
          <dt>테이블</dt><dd>${escapeHtml(source.stats.targetTable)}</dd>
          <dt>화면</dt><dd>${escapeHtml(source.stats.screens)}</dd>
        </dl>
      </article>
    </div>
  `;
}

function renderMissingData() {
  const target = qs("#missingDataList");
  if (!target) return;
  target.innerHTML = missingData.map((item) => `
    <article>
      <div>
        <strong>${escapeHtml(item.title)}</strong>
        <p>${escapeHtml(item.impact)}</p>
      </div>
      <span class="priority ${item.priority.toLowerCase()}">${escapeHtml(item.priority)} · ${escapeHtml(item.owner)}</span>
    </article>
  `).join("");
}

function renderStakeholderScores() {
  const target = qs("#stakeholderScores");
  if (!target) return;
  target.innerHTML = stakeholderScores.map((item) => `
    <article class="role-card">
      <div class="role-card-top">
        <span>${escapeHtml(item.group)}</span>
        <strong>${item.score}</strong>
      </div>
      <h3>${escapeHtml(item.role)}</h3>
      <div class="score-track" aria-hidden="true"><i style="width:${item.score}%"></i></div>
      <p><b>근거</b>${escapeHtml(item.evidence)}</p>
      <p><b>부족</b>${escapeHtml(item.concern)}</p>
      <p><b>보강</b>${escapeHtml(item.improvement)}</p>
      <small>신뢰도 ${escapeHtml(item.confidence)}</small>
    </article>
  `).join("");
}

function renderAwardFactors() {
  const target = qs("#awardFactors");
  if (!target) return;
  target.innerHTML = awardFactors.map((factor) => `
    <article class="factor-row ${escapeHtml(factor.type)}">
      <div>
        <strong>${escapeHtml(factor.label)}</strong>
        <p>${escapeHtml(factor.text)}</p>
      </div>
      <span>${factor.value}</span>
    </article>
  `).join("");
}

function renderActionSimulator() {
  const target = qs("#actionSimulator");
  if (!target) return;
  target.innerHTML = actionSimulations.map((action) => `
    <button class="action-card ${escapeHtml(action.type)}" type="button" data-action-id="${escapeHtml(action.id)}">
      <span>${escapeHtml(action.effect)}</span>
      <strong>${action.delta > 0 ? "+" : ""}${action.delta}</strong>
      <p>${escapeHtml(action.title)}</p>
    </button>
  `).join("");
  target.querySelectorAll("[data-action-id]").forEach((button) => {
    button.addEventListener("click", () => selectAction(button.dataset.actionId));
  });
  selectAction(actionSimulations[0].id);
}

function selectAction(id) {
  const action = actionSimulations.find((item) => item.id === id) || actionSimulations[0];
  const target = qs("#selectedActionInsight");
  if (!target) return;
  qs("#actionSimulator")?.querySelectorAll(".action-card").forEach((button) => {
    button.classList.toggle("selected", button.dataset.actionId === action.id);
  });
  target.className = `insight-box ${action.type}`;
  target.innerHTML = `
    <span>${escapeHtml(action.effect)}</span>
    <strong>${action.delta > 0 ? "+" : ""}${action.delta}점 예상</strong>
    <h3>${escapeHtml(action.title)}</h3>
    <p>${escapeHtml(action.reason)}</p>
    <dl>
      <dt>노력</dt><dd>${escapeHtml(action.effort)}</dd>
      <dt>담당</dt><dd>${escapeHtml(action.owner)}</dd>
    </dl>
  `;
}

function renderCodeGate() {
  renderReviewFocus();
  renderReviewChecklist();
  renderPreCommitChecklist();
  renderCommitGuidance("review");
  renderRollbackVersions();
  updateCodeGateStatus();
}

function renderReviewFocus() {
  const target = qs("#reviewFocus");
  if (!target) return;
  target.innerHTML = codeGate.reviewFocus.map((item) => `
    <article>
      <h3>${escapeHtml(item.title)}</h3>
      <p>${escapeHtml(item.text)}</p>
    </article>
  `).join("");
}

function renderChecklist(targetId, items) {
  const target = qs(targetId);
  if (!target) return;
  target.innerHTML = items.map((item, index) => `
    <label class="gate-check">
      <input type="checkbox" data-check-index="${index}" />
      <span>${escapeHtml(item)}</span>
    </label>
  `).join("");
  target.querySelectorAll("input").forEach((input) => {
    input.addEventListener("change", updateCodeGateStatus);
  });
}

function renderReviewChecklist() {
  renderChecklist("#reviewChecklist", codeGate.reviewChecklist);
}

function renderPreCommitChecklist() {
  renderChecklist("#preCommitChecklist", codeGate.preCommitChecklist);
}

function renderCommitGuidance(mode) {
  const target = qs("#commitGuidance");
  if (!target) return;
  const commands = mode === "rollback" ? codeGate.rollbackCommands : codeGate.commitCommands;
  const title = mode === "rollback" ? "철회 가이드" : "최종 커밋 명령 가이드";
  const note = mode === "rollback"
    ? "정적 페이지는 Git 철회를 실행하지 않습니다. 필요한 경우 터미널에서 안전한 revert를 사용하세요."
    : "아래 명령은 브라우저에서 실행되지 않는 안내입니다. 실제 실행은 터미널에서 진행하세요.";
  target.innerHTML = `
    <h3>${title}</h3>
    <p>${note}</p>
    <pre><code>${escapeHtml(commands.join("\n"))}</code></pre>
  `;
}

function renderRollbackVersions() {
  const target = qs("#rollbackVersions");
  if (!target) return;
  target.innerHTML = codeGate.rollbackVersions.map((version) => `
    <article>
      <span>위험 ${escapeHtml(version.risk)}</span>
      <h3>${escapeHtml(version.name)}</h3>
      <p>${escapeHtml(version.use)}</p>
    </article>
  `).join("");
}

function setGateState(key, value) {
  gateState[key] = value;
  if (key === "commitReady") renderCommitGuidance("commit");
  if (key === "rollbackOpen") renderCommitGuidance("rollback");
  updateCodeGateStatus();
}

function updateCodeGateStatus() {
  const target = qs("#codeGateStatus");
  if (!target) return;
  const reviewChecked = checkedCount("#reviewChecklist");
  const preCommitChecked = checkedCount("#preCommitChecklist");
  gateState.preCommitChecked = preCommitChecked === codeGate.preCommitChecklist.length;
  target.innerHTML = `
    <article class="${gateState.reviewRequested ? "done" : ""}">
      <strong>${gateState.reviewRequested ? "요청됨" : "대기"}</strong>
      <span>리뷰 요청</span>
    </article>
    <article class="${gateState.reviewCompleted ? "done" : ""}">
      <strong>${reviewChecked}/${codeGate.reviewChecklist.length}</strong>
      <span>리뷰 체크</span>
    </article>
    <article class="${gateState.preCommitChecked ? "done" : ""}">
      <strong>${preCommitChecked}/${codeGate.preCommitChecklist.length}</strong>
      <span>커밋 전 확인</span>
    </article>
    <article class="${gateState.commitReady ? "done" : ""}">
      <strong>${gateState.commitReady ? "준비" : "검토"}</strong>
      <span>최종 커밋</span>
    </article>
  `;
}

function checkedCount(selector) {
  const target = qs(selector);
  if (!target) return 0;
  return target.querySelectorAll("input:checked").length;
}

function bindCodeGateButtons() {
  document.querySelectorAll("[data-gate-action]").forEach((button) => {
    button.addEventListener("click", () => {
      const action = button.dataset.gateAction;
      if (action === "review-requested") setGateState("reviewRequested", true);
      if (action === "review-completed") setGateState("reviewCompleted", true);
      if (action === "commit-ready") setGateState("commitReady", true);
      if (action === "rollback-open") setGateState("rollbackOpen", true);
    });
  });
  document.querySelectorAll("[data-git-action]").forEach((button) => {
    button.addEventListener("click", () => runGitAction(button.dataset.gitAction));
  });
}

async function runGitAction(action) {
  const panel = qs("#gitBackendPanel");
  if (!panel) return;
  const remoteUrl = qs("#gitRemoteUrl")?.value || "https://github.com/jiyongKim615/grow_gate_local_ex.git";
  const message = qs("#gitCommitMessage")?.value || "feat: add team operations dashboards";
  panel.className = "git-backend-panel loading";
  panel.innerHTML = `<strong>실행 중</strong><p>${escapeHtml(action)} 요청을 백엔드로 보내는 중입니다.</p>`;
  try {
    const response = await fetch(buildGitUrl(action), buildGitOptions(action, remoteUrl, message));
    const payload = await response.json();
    renderGitResult(action, response.ok, payload);
  } catch (error) {
    panel.className = "git-backend-panel error";
    panel.innerHTML = `
      <strong>백엔드 연결 실패</strong>
      <p>FastAPI 서버가 실행 중인지 확인하세요. 기본 주소는 <code>http://127.0.0.1:8000</code> 입니다.</p>
      <pre><code>${escapeHtml(error.message)}</code></pre>
    `;
  }
}

function buildGitUrl(action) {
  if (action === "status") return "http://127.0.0.1:8000/git/status";
  if (action === "init") return "http://127.0.0.1:8000/git/init";
  if (action === "commit") return "http://127.0.0.1:8000/git/commit";
  if (action === "push") return "http://127.0.0.1:8000/git/push?branch=main";
  return "http://127.0.0.1:8000/git/status";
}

function buildGitOptions(action, remoteUrl, message) {
  const token = qs("#gitActionToken")?.value || "";
  const headers = { "Accept": "application/json" };
  if (token) headers["X-GrowGate-Git-Token"] = token;
  if (action === "status") return { method: "GET", headers: { "Accept": "application/json" } };
  if (action === "init") {
    return {
      method: "POST",
      headers: { ...headers, "Content-Type": "application/json" },
      body: JSON.stringify({ remote_url: remoteUrl, branch: "main" })
    };
  }
  if (action === "commit") {
    return {
      method: "POST",
      headers: { ...headers, "Content-Type": "application/json" },
      body: JSON.stringify({ message })
    };
  }
  return { method: "POST", headers };
}

function renderGitResult(action, httpOk, payload) {
  const panel = qs("#gitBackendPanel");
  if (!panel) return;
  const ok = Boolean(httpOk && payload.ok !== false);
  const steps = Array.isArray(payload.steps)
    ? payload.steps
    : [payload.status, payload.remote, payload].filter(Boolean);
  const statusText = ok ? "완료" : "확인 필요";
  const summary = payload.is_repo
    ? `브랜치 ${payload.branch || "unknown"} · ${payload.project_root || ""}`
    : (payload.hint || payload.detail || "백엔드 Git 요청 결과입니다.");
  const gitOps = payload.git_ops
    ? `<p class="git-ops-state">${payload.git_ops.enabled && payload.git_ops.token_configured ? "Git 실행 가능" : "Git 쓰기 잠김"} · ${escapeHtml(payload.git_ops.hint)}</p>`
    : "";
  panel.className = `git-backend-panel ${ok ? "success" : "error"}`;
  panel.innerHTML = `
    <strong>${escapeHtml(statusText)} · ${escapeHtml(action)}</strong>
    <p>${escapeHtml(summary)}</p>
    ${gitOps}
    <div class="git-step-list">
      ${steps.map(renderGitStep).join("")}
    </div>
  `;
}

function renderGitStep(step) {
  const command = Array.isArray(step.command) ? step.command.join(" ") : "";
  const output = [step.stdout, step.stderr].filter(Boolean).join("\n");
  return `
    <article class="${step.ok ? "success" : "error"}">
      <span>${step.ok ? "OK" : `ERR ${step.returncode || ""}`}</span>
      <code>${escapeHtml(command || step.action || "git")}</code>
      ${output ? `<pre><code>${escapeHtml(output)}</code></pre>` : ""}
    </article>
  `;
}

document.addEventListener("DOMContentLoaded", () => {
  const dashboard = document.body.dataset.dashboard;
  if (dashboard === "team") renderTeamDashboard();
  if (dashboard === "code-gate") {
    renderCodeGate();
    bindCodeGateButtons();
  }
});
