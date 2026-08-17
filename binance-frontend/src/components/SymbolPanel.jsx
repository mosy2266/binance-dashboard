import TickerPanel from './TickerPanel'
import PriceChart from './PriceChart'
import { SYMBOL_COLOR } from '../constants'

export default function SymbolPanel({ symbol, interval, ticker, points }) {
  return (
    <section className="symbol-panel" style={{ '--symbol-color': SYMBOL_COLOR[symbol] }}>
      <h2 className="symbol-title">
        <span className="symbol-dot" aria-hidden="true" />
        {symbol}
      </h2>
      <TickerPanel ticker={ticker} />
      <PriceChart symbol={symbol} interval={interval} color={SYMBOL_COLOR[symbol]} points={points ?? []} />
    </section>
  )
}
