import { api } from '../api.js'

function Bar({ label, used, total, unit }) {
  const pct = total ? Math.min(100, (used / total) * 100) : 0
  return (
    <div className="meter">
      <div className="meter-head"><span>{label}</span><span>{Number(used.toFixed(2))} / {total} {unit}</span></div>
      <div className="track"><div className={`fill ${pct > 85 ? 'hot' : ''}`} style={{ width: pct + '%' }} /></div>
    </div>
  )
}

export default function NodePanel({ nodes, act }) {
  return (
    <div className="panel">
      <h2>Worker nodes</h2>
      <div className="nodes">
        {nodes.map((n) => (
          <article key={n.id} className={`node ${n.status === 'DOWN' ? 'down' : ''}`}>
            <header>
              <strong>{n.name}</strong>
              <span className={`badge ${n.status.toLowerCase()}`}>{n.status === 'UP' ? 'Healthy' : 'Down'}</span>
            </header>
            <Bar label="CPU" used={n.usedCpu} total={n.cpuCores} unit="cores" />
            <Bar label="Memory" used={n.usedMemory} total={n.memoryMb} unit="MB" />
            <footer>
              <span>{n.workloadCount} container{n.workloadCount === 1 ? '' : 's'}</span>
              {n.status === 'UP'
                ? <button className="ghost danger" onClick={() => act(() => api.failNode(n.id), `${n.name} marked down`)}>Simulate failure</button>
                : <button className="ghost" onClick={() => act(() => api.recoverNode(n.id), `${n.name} is back up`)}>Bring back up</button>}
            </footer>
          </article>
        ))}
        {nodes.length === 0 && <p className="empty">No nodes yet. Start the backend to create the default nodes.</p>}
      </div>
    </div>
  )
}
