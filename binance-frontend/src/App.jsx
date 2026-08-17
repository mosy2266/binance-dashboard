import { useState } from 'react'
import ConnectionStatus from './components/ConnectionStatus'
import IntervalSelector from './components/IntervalSelector'
import SymbolPanel from './components/SymbolPanel'
import { SYMBOLS } from './constants'
import { useMarketData } from './hooks/useMarketData'

function App() {
  const [interval, setInterval] = useState('1m')
  const { tickers, klines, wsStatus, error } = useMarketData(interval)

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>Binance 운영 대시보드</h1>
        <ConnectionStatus status={wsStatus} />
      </header>

      {error && <div className="error-banner">데이터를 불러오지 못했습니다: {error}</div>}

      <IntervalSelector value={interval} onChange={setInterval} />

      <div className="panel-grid">
        {SYMBOLS.map((symbol) => (
          <SymbolPanel
            key={symbol}
            symbol={symbol}
            interval={interval}
            ticker={tickers[symbol]}
            points={klines[symbol]}
          />
        ))}
      </div>
    </div>
  )
}

export default App
