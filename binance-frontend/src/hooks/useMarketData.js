import { useCallback, useEffect, useState } from 'react'
import { fetchKlines, fetchTickers } from '../api/marketApi'
import { CHART_POINTS, INTERVAL_MS, SYMBOLS } from '../constants'
import { parseInstant } from '../utils/format'
import { useMarketSocket } from './useMarketSocket'

function toPoint(raw) {
  return {
    time: parseInstant(raw.openTime),
    open: Number(raw.openPrice),
    high: Number(raw.highPrice),
    low: Number(raw.lowPrice),
    close: Number(raw.closePrice),
    volume: Number(raw.volume),
    quoteVolume: Number(raw.quoteVolume),
  }
}

function toTicker(raw) {
  return {
    symbol: raw.symbol,
    lastPrice: Number(raw.lastPrice),
    priceChange: Number(raw.priceChange),
    priceChangePercent: Number(raw.priceChangePercent),
    highPrice: Number(raw.highPrice),
    lowPrice: Number(raw.lowPrice),
    volume: Number(raw.volume),
    quoteVolume: Number(raw.quoteVolume),
    updatedAt: parseInstant(raw.updatedAt),
  }
}

// Appends a live point, replacing the last one when it shares the same
// open time (the still-forming candle keeps getting overwritten until close).
function appendPoint(points, point) {
  const last = points[points.length - 1]
  const next = last && last.time.getTime() === point.time.getTime()
    ? [...points.slice(0, -1), point]
    : [...points, point]
  return next.slice(-CHART_POINTS)
}

export function useMarketData(interval) {
  const [tickers, setTickers] = useState({})
  const [klines, setKlines] = useState({})
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    fetchTickers()
      .then((rows) => {
        if (cancelled) return
        setTickers(Object.fromEntries(rows.map((row) => [row.symbol, toTicker(row)])))
      })
      .catch((e) => !cancelled && setError(e.message))
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    const to = new Date()
    const from = new Date(to.getTime() - CHART_POINTS * INTERVAL_MS[interval])

    Promise.all(SYMBOLS.map((symbol) => fetchKlines(symbol, interval, from, to)))
      .then((results) => {
        if (cancelled) return
        setKlines(
          Object.fromEntries(results.map((rows, i) => [SYMBOLS[i], rows.map(toPoint)])),
        )
      })
      .catch((e) => !cancelled && setError(e.message))
    return () => {
      cancelled = true
    }
  }, [interval])

  const onMessage = useCallback(
    (message) => {
      if (message.type === 'ticker') {
        const ticker = toTicker(message.data)
        setTickers((prev) => ({ ...prev, [ticker.symbol]: ticker }))
      } else if (message.type === 'kline' && interval === '1m') {
        const point = toPoint(message.data)
        setKlines((prev) => ({
          ...prev,
          [message.symbol]: appendPoint(prev[message.symbol] ?? [], point),
        }))
      }
    },
    [interval],
  )

  const wsStatus = useMarketSocket(onMessage)

  return { tickers, klines, wsStatus, error }
}
