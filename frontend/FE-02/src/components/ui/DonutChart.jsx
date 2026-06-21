// 도넛 차트 컴포넌트
export default function DonutChart({ score = 83, size = 62 }) {
  const CIRCUMFERENCE = 97.4

  const offset = CIRCUMFERENCE * (1 - score / 100)

  return (
    <svg
      className="donut"
      viewBox="0 0 36 36"
      width={size}
      height={size}
      style={{ flex: 'none' }}
    >

      <path
        d="M18 2.5a15.5 15.5 0 1 1 0 31 15.5 15.5 0 0 1 0-31"
        fill="none"
        stroke="#eef1f7"
        strokeWidth="4"
      />

      <path
        d="M18 2.5a15.5 15.5 0 1 1 0 31 15.5 15.5 0 0 1 0-31"
        fill="none"
        stroke="var(--blue)"
        strokeWidth="4"
        strokeLinecap="round"
        strokeDasharray={CIRCUMFERENCE}
        strokeDashoffset={offset}
        transform="rotate(-90 18 18)"
      />
    </svg>
  )
}
