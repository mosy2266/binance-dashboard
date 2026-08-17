// Backend sends java.time.Instant as an ISO-8601 string, but this defends
// against the epoch-millis / [seconds, nanos] shapes some Jackson configs emit.
export function parseInstant(value) {
  if (value == null) return null
  if (Array.isArray(value)) return new Date(value[0] * 1000)
  if (typeof value === 'number') return new Date(value)
  return new Date(value)
}

export function formatCompact(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return '-'
  return new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 2 }).format(n)
}

export function formatPrice(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return '-'
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: n < 100 ? 4 : 2,
  }).format(n)
}

export function formatPercent(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return '-'
  const sign = n > 0 ? '+' : ''
  return `${sign}${n.toFixed(2)}%`
}

export function formatTime(date, interval) {
  if (!date) return '-'
  const showDate = interval === '4h' || interval === '1d'
  return date.toLocaleString('ko-KR', {
    month: showDate ? 'numeric' : undefined,
    day: showDate ? 'numeric' : undefined,
    hour: '2-digit',
    minute: '2-digit',
  })
}
