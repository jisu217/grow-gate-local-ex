import './MarkdownReportPreview.css'

export default function ReportDownloadButtons({ report, onDownload, onPreview, onPrint, disabled }) {
  return (
    <div className="report-actions">
      <button type="button" onClick={() => onDownload(report)} disabled={disabled}>
        Markdown 다운로드
      </button>
      <button type="button" onClick={() => onPreview(report)} disabled={disabled}>
        HTML 미리보기
      </button>
      <button type="button" onClick={() => onPrint(report)} disabled={disabled}>
        브라우저에서 PDF로 인쇄
      </button>
    </div>
  )
}
