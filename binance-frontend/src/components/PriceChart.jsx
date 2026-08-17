import { useMemo, useState } from 'react'
import { formatPrice, formatTime } from '../utils/format'

const WIDTH = 640
const HEIGHT = 260
const PAD = { top: 16, right: 16, bottom: 28, left: 60 }
const PLOT_W = WIDTH - PAD.left - PAD.right
const PLOT_H = HEIGHT - PAD.top - PAD.bottom
const Y_TICKS = 4

export default function PriceChart({ symbol, interval, color, points }) {
  const [hoverIndex, setHoverIndex] = useState(null)
  const [showTable, setShowTable] = useState(false)

  const scale = useMemo(() => {
    if (points.length === 0) return null
    const closes = points.map((p) => p.close)
    const rawMin = Math.min(...closes)
    const rawMax = Math.max(...closes)
    const pad = (rawMax - rawMin) * 0.1 || rawMax * 0.01 || 1
    const min = rawMin - pad
    const max = rawMax + pad

    const x = (i) => PAD.left + (points.length === 1 ? PLOT_W / 2 : (i / (points.length - 1)) * PLOT_W)
    const y = (v) => PAD.top + PLOT_H - ((v - min) / (max - min)) * PLOT_H

    return { min, max, x, y }
  }, [points])

  if (!points || points.length === 0) {
    return <div className="chart-empty">차트 데이터 불러오는 중...</div>
  }

  const linePath = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${scale.x(i)} ${scale.y(p.close)}`).join(' ')
  const areaPath = `${linePath} L ${scale.x(points.length - 1)} ${PAD.top + PLOT_H} L ${scale.x(0)} ${PAD.top + PLOT_H} Z`

  const yTicks = Array.from({ length: Y_TICKS + 1 }, (_, i) => scale.min + ((scale.max - scale.min) * i) / Y_TICKS)
  const xTickIndexes = [...new Set([0, Math.floor((points.length - 1) / 2), points.length - 1])]

  const last = points[points.length - 1]
  const hovered = hoverIndex != null ? points[hoverIndex] : null

  function handlePointerMove(e) {
    const svg = e.currentTarget
    const rect = svg.getBoundingClientRect()
    const px = ((e.clientX - rect.left) / rect.width) * WIDTH
    const ratio = Math.min(1, Math.max(0, (px - PAD.left) / PLOT_W))
    const index = Math.round(ratio * (points.length - 1))
    setHoverIndex(Math.min(points.length - 1, Math.max(0, index)))
  }

  return (
    <div className="chart-card">
      <div className="chart-header">
        <h3>{symbol} 가격 추이</h3>
        <button type="button" className="table-toggle" onClick={() => setShowTable((v) => !v)}>
          {showTable ? '차트로 보기' : '표로 보기'}
        </button>
      </div>

      {showTable ? (
        <div className="chart-table-wrap">
          <table className="chart-table">
            <thead>
              <tr>
                <th>시간</th>
                <th>종가</th>
                <th>고가</th>
                <th>저가</th>
                <th>거래량</th>
              </tr>
            </thead>
            <tbody>
              {[...points].reverse().map((p) => (
                <tr key={p.time.getTime()}>
                  <td>{formatTime(p.time, interval)}</td>
                  <td>{formatPrice(p.close)}</td>
                  <td>{formatPrice(p.high)}</td>
                  <td>{formatPrice(p.low)}</td>
                  <td>{formatPrice(p.volume)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="chart-svg-wrap">
          <svg
            viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
            role="img"
            aria-label={`${symbol} ${interval} 종가 추이 차트`}
            onPointerMove={handlePointerMove}
            onPointerLeave={() => setHoverIndex(null)}
          >
            {yTicks.map((tick) => (
              <g key={tick}>
                <line
                  x1={PAD.left}
                  x2={WIDTH - PAD.right}
                  y1={scale.y(tick)}
                  y2={scale.y(tick)}
                  className="grid-line"
                />
                <text x={PAD.left - 8} y={scale.y(tick)} className="axis-label" textAnchor="end" dominantBaseline="middle">
                  {formatPrice(tick)}
                </text>
              </g>
            ))}

            {xTickIndexes.map((i, tickPos) => (
              <text
                key={i}
                x={scale.x(i)}
                y={HEIGHT - 8}
                className="axis-label"
                textAnchor={tickPos === 0 ? 'start' : tickPos === xTickIndexes.length - 1 ? 'end' : 'middle'}
              >
                {formatTime(points[i].time, interval)}
              </text>
            ))}

            <path d={areaPath} fill={color} opacity="0.1" stroke="none" />
            <path d={linePath} fill="none" stroke={color} strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />

            <circle cx={scale.x(points.length - 1)} cy={scale.y(last.close)} r="4" fill={color} stroke="var(--chart-surface)" strokeWidth="2" />
            <text x={scale.x(points.length - 1)} y={scale.y(last.close) - 10} className="end-label" textAnchor="end">
              {formatPrice(last.close)}
            </text>

            {hovered && (
              <g>
                <line x1={scale.x(hoverIndex)} x2={scale.x(hoverIndex)} y1={PAD.top} y2={PAD.top + PLOT_H} className="crosshair" />
                <circle cx={scale.x(hoverIndex)} cy={scale.y(hovered.close)} r="4" fill={color} stroke="var(--chart-surface)" strokeWidth="2" />
              </g>
            )}
          </svg>

          {hovered && (
            <div
              className="chart-tooltip"
              style={{
                left: `${(scale.x(hoverIndex) / WIDTH) * 100}%`,
                top: `${(scale.y(hovered.close) / HEIGHT) * 100}%`,
              }}
            >
              <div className="tooltip-value">${formatPrice(hovered.close)}</div>
              <div className="tooltip-time">{formatTime(hovered.time, interval)}</div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
