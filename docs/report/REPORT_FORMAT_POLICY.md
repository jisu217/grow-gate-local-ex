# Report Format Policy

Official report output is Markdown.

PDF is optional print/export output generated from Markdown or browser print. Do not present PDF generation as a core feature. 필요한 경우 브라우저 인쇄 또는 CI 변환으로 PDF 보관이 가능합니다.

## Allowed Wording

- Markdown report download
- HTML preview
- Browser print/export to PDF

## Wording To Avoid

- PDF auto-generation as a core feature
- PDF가 공식 원본
- PDF renderer가 검증 완료된 상용 기능

## Required Front Matter

Reports should include `diagnosisId`, `publicDataProofRunId`, `mlModelVersion`, `reportFormat=markdown`, and limitations.

Each Markdown report must include:

- Front matter with `publicDataProofRunId`, `modelVersion`, `reportFormat`, and `limitation`
- Source Appendix
- Claim Verification result
- Synthetic Validation Disclaimer
