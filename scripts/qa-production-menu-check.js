const fs = require('fs');
const path = require('path');
let playwright;
try {
  playwright = require('playwright');
} catch {
  playwright = require('../frontend/FE-02/node_modules/playwright');
}
const { chromium } = playwright;

const baseUrl = process.env.BASE_URL || 'https://growgate.54.180.56.24.sslip.io';
const outDir = process.env.QA_OUT_DIR || path.resolve(process.cwd(), 'artifacts/qa/production-menu-qa-20260706');
const email = process.env.QA_EMAIL || 'admin@growgate.com';
const password = process.env.QA_PASSWORD || 'growgateadmin';

const scenarios = [
  { name: '창원 배터리팩 완료 결과', id: '2b52340c-d51d-50dc-b6cb-592627abb62d', expectedGate: '보류' },
  { name: '부산 마린 액추에이터 완료 결과', id: '6bc9800e-755c-55f9-b9ce-87a69dc2c450', expectedGate: '조건부 진행' },
  { name: '대구 메디센서 완료 결과', id: 'cf7d23b4-51d8-53f7-be26-a92bf6682a6d', expectedGate: '중단 후 보완' },
];

const routes = [
  { name: '홈', path: '/', auth: false },
  { name: '로그인', path: '/login', auth: false },
  { name: '회원가입', path: '/signup', auth: false },
  { name: '개인 회원가입', path: '/signup/personal', auth: false },
  { name: '기업 회원가입', path: '/signup/company', auth: false },
  { name: '판단 이력', path: '/diagnosis', auth: true },
  { name: 'Gate 판단 입력', path: '/diagnosis/new', auth: true },
  { name: '데이터 수집', path: '/public-data/daily-refresh', auth: true },
  { name: '마이페이지', path: '/mypage', auth: true },
  { name: '진단 결과 배터리', path: `/diagnosis/${scenarios[0].id}`, auth: true },
  { name: '진단 근거 배터리', path: `/diagnosis/${scenarios[0].id}/sources`, auth: true },
];

const hardErrorPatterns = [
  '진단 결과를 불러오지 못했습니다',
  '수집 로그를 불러오지 못했습니다',
  '네트워크를 확인하고 다시 시도해주세요',
  'Cannot read properties',
  'Unexpected Application Error',
  '404',
];

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function slug(input) {
  return input.replace(/[^a-zA-Z0-9가-힣_-]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 80);
}

async function snapshot(page, label) {
  const file = path.join(outDir, `${slug(label)}.png`);
  await page.screenshot({ path: file, fullPage: true });
  return file;
}

function classifyPage(text, failedRequests, consoleErrors) {
  const matchedErrors = hardErrorPatterns.filter((pattern) => text.includes(pattern));
  const apiFailures = failedRequests.filter((request) => request.status >= 400);
  const severeConsole = consoleErrors.filter((line) => !line.includes('favicon'));
  const status = matchedErrors.length || apiFailures.length || severeConsole.length ? 'CHECK' : 'PASS';
  return { status, matchedErrors, apiFailures, severeConsole };
}

async function waitForSettled(page) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(800);
}

async function main() {
  ensureDir(outDir);
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1000 },
    locale: 'ko-KR',
  });
  const page = await context.newPage();
  const records = [];
  const failedRequests = [];
  const consoleErrors = [];

  page.on('response', async (response) => {
    const status = response.status();
    if (status >= 400) {
      failedRequests.push({ url: response.url(), status });
    }
  });
  page.on('console', (message) => {
    if (['error', 'warning'].includes(message.type())) {
      consoleErrors.push(`${message.type()}: ${message.text()}`);
    }
  });
  page.on('pageerror', (error) => {
    consoleErrors.push(`pageerror: ${error.message}`);
  });

  async function recordRoute(name, urlPath, options = {}) {
    const beforeFailures = failedRequests.length;
    const beforeConsole = consoleErrors.length;
    const response = await page.goto(`${baseUrl}${urlPath}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
    await waitForSettled(page);
    const expectedMissing = [];
    if (options.expectText) {
      try {
        await page.getByText(options.expectText).first().waitFor({ timeout: 10000 });
      } catch {
        expectedMissing.push(`expected text not visible: ${options.expectText}`);
      }
    }
    const text = await page.locator('body').innerText({ timeout: 10000 });
    const screenshot = await snapshot(page, `${records.length + 1}-${name}`);
    const sliceFailures = failedRequests.slice(beforeFailures);
    const sliceConsole = consoleErrors.slice(beforeConsole);
    const classified = classifyPage(text, sliceFailures, sliceConsole);
    classified.matchedErrors.push(...expectedMissing);
    if (expectedMissing.length > 0) {
      classified.status = 'CHECK';
    }
    records.push({
      name,
      path: urlPath,
      finalUrl: page.url(),
      httpStatus: response ? response.status() : null,
      title: await page.title(),
      visibleTextSample: text.slice(0, 220),
      screenshot,
      ...classified,
    });
  }

  await recordRoute('홈', '/', { expectText: 'GROW-GATE' });
  await recordRoute('로그인', '/login', { expectText: '로그인' });
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', password);
  await page.getByRole('button', { name: '로그인' }).click();
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 });
  await waitForSettled(page);
  records.push({
    name: '로그인 실행',
    path: '/login',
    finalUrl: page.url(),
    status: 'PASS',
    matchedErrors: [],
    apiFailures: [],
    severeConsole: [],
    screenshot: await snapshot(page, 'login-success'),
  });

  for (const route of routes.filter((route) => route.name !== '홈' && route.name !== '로그인')) {
    await recordRoute(route.name, route.path);
  }

  await recordRoute('Gate 판단 입력 시연 카드 목록', '/diagnosis/new', { expectText: '합성 시연 시나리오' });
  for (const scenario of scenarios) {
    await recordRoute(`${scenario.name} 직접 URL`, `/diagnosis/${scenario.id}`, { expectText: '수출 GATE 판단' });
    const text = await page.locator('body').innerText();
    const hasExpected = text.includes(scenario.expectedGate);
    records.push({
      name: `${scenario.name} Gate 상태 확인`,
      path: `/diagnosis/${scenario.id}`,
      finalUrl: page.url(),
      status: hasExpected ? 'PASS' : 'CHECK',
      matchedErrors: hasExpected ? [] : [`expected gate ${scenario.expectedGate} not visible`],
      apiFailures: [],
      severeConsole: [],
      screenshot: await snapshot(page, `${scenario.name}-gate-check`),
    });
  }

  await recordRoute('진단 00 개요', `/diagnosis/${scenarios[0].id}?step=0`, { expectText: '진단 개요' });
  const stepNames = ['제품 입력', '데이터 호출', '준비도 추정', '시장성', '규제·인증', '리콜 위험', 'FTO·지원', '실행 보드', '리포트', '근거 데이터'];
  for (const stepName of stepNames) {
    const beforeFailures = failedRequests.length;
    const beforeConsole = consoleErrors.length;
    await page.getByText(stepName, { exact: true }).first().click();
    await waitForSettled(page);
    const text = await page.locator('body').innerText();
    const screenshot = await snapshot(page, `diagnosis-step-${stepName}`);
    const classified = classifyPage(text, failedRequests.slice(beforeFailures), consoleErrors.slice(beforeConsole));
    records.push({
      name: `진단 단계 ${stepName}`,
      path: `/diagnosis/${scenarios[0].id}`,
      finalUrl: page.url(),
      httpStatus: 200,
      visibleTextSample: text.slice(0, 220),
      screenshot,
      ...classified,
    });
  }

  await recordRoute('S10 근거 전용 페이지', `/diagnosis/${scenarios[0].id}/sources`, { expectText: '진단 근거 및 출처' });
  await recordRoute('공공데이터 수집 화면', '/public-data/daily-refresh', { expectText: '아침 자동 수집' });
  await recordRoute('마이페이지 화면', '/mypage', { expectText: '마이페이지' });

  const summary = {
    baseUrl,
    checkedAt: new Date().toISOString(),
    total: records.length,
    pass: records.filter((record) => record.status === 'PASS').length,
    check: records.filter((record) => record.status !== 'PASS').length,
    records,
  };

  fs.writeFileSync(path.join(outDir, 'qa-menu-results.json'), JSON.stringify(summary, null, 2));
  const md = [
    '# Production Menu QA - 2026-07-06',
    '',
    `- Target: ${baseUrl}`,
    `- Checked at: ${summary.checkedAt}`,
    `- Result: ${summary.pass}/${summary.total} PASS, ${summary.check} CHECK`,
    '',
    '| Menu / Flow | Path | Status | Notes | Screenshot |',
    '| :--- | :--- | :--- | :--- | :--- |',
    ...records.map((record) => {
      const notes = [
        ...(record.matchedErrors || []),
        ...(record.apiFailures || []).map((failure) => `${failure.status} ${failure.url}`),
        ...(record.severeConsole || []),
      ].join('<br>') || '정상 표시';
      return `| ${record.name} | \`${record.path}\` | ${record.status} | ${notes.replace(/\|/g, '\\|')} | \`${record.screenshot}\` |`;
    }),
    '',
  ].join('\n');
  fs.writeFileSync(path.join(outDir, 'production-menu-qa-2026-07-06.md'), md);
  await browser.close();
  console.log(JSON.stringify({ outDir, total: summary.total, pass: summary.pass, check: summary.check }, null, 2));
  if (summary.check > 0) {
    process.exitCode = 2;
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
