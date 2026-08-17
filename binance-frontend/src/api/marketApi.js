const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

export function wsMarketUrl() {
  if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/market`
}

export async function fetchTickers() {
  const res = await fetch(`${API_BASE}/tickers`)
  if (!res.ok) throw new Error(`Failed to fetch tickers: ${res.status}`)
  return res.json()
}

export async function fetchKlines(symbol, interval, from, to) {
  const params = new URLSearchParams({
    symbol,
    interval,
    from: from.toISOString(),
    to: to.toISOString(),
  })
  const res = await fetch(`${API_BASE}/klines?${params}`)
  if (!res.ok) throw new Error(`Failed to fetch klines: ${res.status}`)
  return res.json()
}
