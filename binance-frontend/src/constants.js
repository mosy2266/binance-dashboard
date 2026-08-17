export const SYMBOLS = ['BTCUSDT', 'ETHUSDT']

// Series color per symbol, from the categorical palette's slots 1 and 2
// (blue, orange) — validated as CVD-safe when read separately or together.
export const SYMBOL_COLOR = {
  BTCUSDT: 'var(--series-1)',
  ETHUSDT: 'var(--series-2)',
}

export const INTERVALS = [
  { value: '1m', label: '1분' },
  { value: '5m', label: '5분' },
  { value: '15m', label: '15분' },
  { value: '1h', label: '1시간' },
  { value: '4h', label: '4시간' },
  { value: '1d', label: '1일' },
]

export const INTERVAL_MS = {
  '1m': 60_000,
  '5m': 5 * 60_000,
  '15m': 15 * 60_000,
  '1h': 60 * 60_000,
  '4h': 4 * 60 * 60_000,
  '1d': 24 * 60 * 60_000,
}

// How many candles to show on the chart for any interval.
export const CHART_POINTS = 60
