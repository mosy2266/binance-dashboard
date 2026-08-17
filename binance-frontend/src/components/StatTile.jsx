export default function StatTile({ label, value, delta, hero }) {
  return (
    <div className={`stat-tile${hero ? ' stat-tile-hero' : ''}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {delta != null && (
        <div className={`stat-delta ${delta.direction}`}>{delta.text}</div>
      )}
    </div>
  )
}
