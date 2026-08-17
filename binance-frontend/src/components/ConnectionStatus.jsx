const STATUS = {
  open: { label: '실시간 연결됨', className: 'good' },
  connecting: { label: '연결 중...', className: 'warning' },
  reconnecting: { label: '재연결 중...', className: 'warning' },
}

export default function ConnectionStatus({ status }) {
  const info = STATUS[status] ?? { label: '연결 끊김', className: 'critical' }
  return (
    <span className={`status-badge status-${info.className}`}>
      <span className="status-dot" aria-hidden="true" />
      {info.label}
    </span>
  )
}
