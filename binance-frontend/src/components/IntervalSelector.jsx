import { INTERVALS } from '../constants'

export default function IntervalSelector({ value, onChange }) {
  return (
    <div className="filter-row" role="radiogroup" aria-label="캔들 간격">
      {INTERVALS.map((option) => (
        <button
          key={option.value}
          type="button"
          role="radio"
          aria-checked={option.value === value}
          className={`interval-button${option.value === value ? ' active' : ''}`}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
