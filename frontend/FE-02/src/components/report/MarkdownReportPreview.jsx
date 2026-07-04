import './MarkdownReportPreview.css'

export function markdownToHtml(markdown = '') {
  const lines = markdown.split(/\r?\n/)
  const html = []
  let inList = false

  lines.forEach((line) => {
    if (line.startsWith('---')) {
      html.push('<hr />')
      return
    }
    if (line.startsWith('### ')) {
      closeList()
      html.push(`<h3>${escapeHtml(line.slice(4))}</h3>`)
      return
    }
    if (line.startsWith('## ')) {
      closeList()
      html.push(`<h2>${escapeHtml(line.slice(3))}</h2>`)
      return
    }
    if (line.startsWith('# ')) {
      closeList()
      html.push(`<h1>${escapeHtml(line.slice(2))}</h1>`)
      return
    }
    if (line.startsWith('- ')) {
      if (!inList) {
        html.push('<ul>')
        inList = true
      }
      html.push(`<li>${formatInline(line.slice(2))}</li>`)
      return
    }
    if (!line.trim()) {
      closeList()
      return
    }
    closeList()
    html.push(`<p>${formatInline(line)}</p>`)
  })

  closeList()
  return html.join('\n')

  function closeList() {
    if (!inList) return
    html.push('</ul>')
    inList = false
  }
}

export function reportPreviewDocument(markdown = '') {
  return `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8" />
  <title>GROW-GATE Markdown Report Preview</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; line-height: 1.65; max-width: 900px; margin: 40px auto; padding: 0 28px; color: #17213a; }
    h1 { font-size: 28px; margin: 22px 0 12px; }
    h2 { font-size: 20px; margin: 28px 0 10px; border-bottom: 1px solid #e7ebf3; padding-bottom: 8px; }
    h3 { font-size: 16px; margin: 20px 0 8px; }
    li { margin: 6px 0; }
    code { background: #f3f6fb; padding: 2px 5px; border-radius: 5px; }
    .print-note { color: #61708a; font-size: 13px; margin-bottom: 18px; }
    @media print { .print-note { display: none; } body { margin: 0 auto; } }
  </style>
</head>
<body>
  <div class="print-note">공식 원본은 Markdown입니다. PDF 보관이 필요하면 이 화면을 브라우저 인쇄로 저장합니다.</div>
  ${markdownToHtml(markdown)}
</body>
</html>`
}

export default function MarkdownReportPreview({ markdown }) {
  return (
    <div
      className="markdown-report-preview"
      dangerouslySetInnerHTML={{ __html: markdownToHtml(markdown) }}
    />
  )
}

function formatInline(value) {
  return escapeHtml(value)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}
