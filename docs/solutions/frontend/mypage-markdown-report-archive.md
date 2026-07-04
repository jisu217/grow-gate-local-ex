---
title: Mypage Markdown Report Archive
date: 2026-07-04
category: frontend
module: FE-02 mypage and diagnosis report
problem_type: best_practice
component: frontend_stimulus
severity: medium
applies_when:
  - 마이페이지에서 제품별 진단 산출물을 보여줘야 할 때
  - Markdown report를 공식 산출물로 유지하고 PDF 과장을 피해야 할 때
tags: [mypage, markdown-report, judge-defense]
---

# Mypage Markdown Report Archive

## Context

마이페이지의 “이 제품 리포트 보관함”은 카드만 있고 실제 생성된 진단 Markdown report와 연결되지 않았다. 심사 시 버튼을 눌렀을 때 API가 호출되지 않으면 화면 연출로 보일 수 있고, PDF 표현이 섞이면 한국어 PDF renderer 검증 질문을 받을 수 있다.

## Guidance

제품별 리포트 보관함은 `/api/diagnosis` 목록에서 `status=done`인 진단만 archive item으로 만들고, 각 항목의 버튼은 `/api/diagnosis/{id}/report`를 호출해야 한다.

지원하는 동작은 세 가지로 제한한다.

- `Markdown 다운로드`: 공식 산출물인 UTF-8 Markdown 파일을 저장한다.
- `HTML 미리보기`: Markdown을 브라우저에서 읽기 쉬운 HTML로 표시한다.
- `브라우저에서 PDF로 인쇄`: 서버 PDF 생성이 아니라 브라우저 인쇄/저장 흐름으로 안내한다.

백엔드 report renderer는 front matter에 `publicDataProofRunId`, `modelVersion`, `reportFormat`, `limitation`을 넣고, 본문에 `Source Appendix`, `Claim Verification`, `Synthetic Validation Disclaimer`를 포함해야 한다.

## Why This Matters

리포트가 실제 API에 연결되어야 마이페이지가 “진단 후 산출물 보관함” 역할을 한다. Markdown을 공식 산출물로 고정하면 PDF 한글 렌더링이나 자동 PDF 생성 과장 질문을 피하면서도 제출 가능한 결과물을 제공할 수 있다.

## When to Apply

- 진단 결과가 완료된 뒤 제품 단위 산출물을 다시 열어야 할 때
- 보고서 미리보기와 다운로드를 같은 API payload에서 유지해야 할 때
- PDF는 필요하지만 서버 렌더러 검증이 끝나지 않았을 때

## Examples

Before:

```js
reports: []
```

After:

```js
reports: buildReportArchive(diagnoses, product.productId)
```

UI 버튼은 `ReportDownloadButtons`를 사용하고, Markdown 미리보기는 `MarkdownReportPreview`를 사용한다.

## Related

- [Judge Defense Decision Brief](judge-defense-decision-brief.md)
- [Report Format Policy](../../report/REPORT_FORMAT_POLICY.md)
