import StatTile from './StatTile'
import { formatCompact, formatPercent, formatPrice } from '../utils/format'

export default function TickerPanel({ ticker }) {
  if (!ticker) {
    return <div className="kpi-row kpi-row-empty">시세 불러오는 중...</div>
  }

  const direction = ticker.priceChangePercent > 0 ? 'good' : ticker.priceChangePercent < 0 ? 'critical' : ''

  return (
    <div className="kpi-row">
      <StatTile label="현재가" value={`$${formatPrice(ticker.lastPrice)}`} hero />
      <StatTile
        label="24시간 변동"
        value={`$${formatPrice(Math.abs(ticker.priceChange))}`}
        delta={{ direction, text: formatPercent(ticker.priceChangePercent) }}
      />
      <StatTile label="24시간 고가" value={`$${formatPrice(ticker.highPrice)}`} />
      <StatTile label="24시간 저가" value={`$${formatPrice(ticker.lowPrice)}`} />
      <StatTile label="거래량" value={formatCompact(ticker.volume)} />
      <StatTile label="거래대금" value={`$${formatCompact(ticker.quoteVolume)}`} />
    </div>
  )
}
