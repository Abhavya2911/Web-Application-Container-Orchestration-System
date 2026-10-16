
import { useState } from 'react'

const hue = (id) => (id * 67) % 360

function Gantt({ run }) {
  const timeline = JSON.parse(run.timelineJson || '[]')
  const names = Object.fromEntries(
    run.results.map((r) => [r.workloadId, r.workloadName])
  )
  const nodeNames = Object.fromEntries(
    run.results.map((r) => [r.nodeId, r.nodeName])
  )
  const end = Math.max(1, ...timeline.map((s) => s.end))
  const nodeIds = [...new Set(timeline.map((s) => s.node))]
  const ticks = Array.from(
    { length: Math.min(end, 40) + 1 },
    (_, i) => Math.round((i * end) / Math.min(end, 40))
  )

  return (
    <div className="gantt" role="img" aria-label="Execution timeline per node">
      {nodeIds.map((nid) => (
        <div className="lane" key={nid}>
          <span className="lane-name">
            {nodeNames[nid] ?? `node ${nid}`}
          </span>
          <div className="lane-track">
            {timeline
              .filter((s) => s.node === nid)
              .map((s, i) => (
                <div
                  key={i}
                  className="seg"
                  title={`${names[s.id] ?? 'Workload'}: ${s.start} → ${s.end}`}
                  style={{
                    left: (s.start / end) * 100 + '%',
                    width: ((s.end - s.start) / end) * 100 + '%',
                    background: `hsl(${hue(s.id)} 55% 42%)`,
                  }}
                >
                  <span>{names[s.id] ?? 'Workload'}</span>
                </div>
              ))}
          </div>
        </div>
      ))}
      <div className="lane">
        <span className="lane-name" />
        <div className="axis">
          {[...new Set(ticks)].map((t) => (
            <span key={t} style={{ left: (t / end) * 100 + '%' }}>
              {t}
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}

export default function SchedulerPanel({
  run,
  runs,
  onRun,
  onSelect,
  pending,
}) {
  const [algo, setAlgo] = useState('FCFS')
  const [quantum, setQuantum] = useState(2)
  const [historyPeriod, setHistoryPeriod] = useState('7')

  const filteredRuns = runs.filter((r) => {
    if (historyPeriod === 'all') return true

    const runDate = new Date(r.createdAt)
    const now = new Date()

    if (Number.isNaN(runDate.getTime())) return false

    if (historyPeriod === 'today') {
      return runDate.toDateString() === now.toDateString()
    }

    const days = Number(historyPeriod)
    const cutoff = new Date(now)
    cutoff.setDate(cutoff.getDate() - days)

    return runDate >= cutoff && runDate <= now
  })

  return (
    <div className="panel">
      <h2>CPU scheduler</h2>

      <div className="controls">
        <label>
          Algorithm
          <select
            value={algo}
            onChange={(e) => setAlgo(e.target.value)}
          >
            <option value="FCFS">First Come First Served</option>
            <option value="RR">Round Robin</option>
          </select>
        </label>

        {algo === 'RR' && (
          <label>
            Time quantum
            <input
              type="number"
              min="1"
              value={quantum}
              onChange={(e) => setQuantum(+e.target.value)}
            />
          </label>
        )}

        <button
          onClick={() => onRun(algo, quantum)}
          disabled={pending === 0}
        >
          Schedule {pending} pending workload
          {pending === 1 ? '' : 's'}
        </button>
      </div>

      {!run ? (
        <p className="empty">
          Run the scheduler to see the execution timeline and waiting,
          turnaround and response times.
        </p>
      ) : (
        <>
          <h3>
            Run #{run.id}:{' '}
            {run.algorithm === 'RR'
              ? `Round Robin (quantum ${run.quantum})`
              : 'FCFS'}
          </h3>

          <Gantt run={run} />

          <div className="averages">
            <div>
              <b>{run.avgWaiting.toFixed(2)}</b>
              <span>avg waiting</span>
            </div>
            <div>
              <b>{run.avgTurnaround.toFixed(2)}</b>
              <span>avg turnaround</span>
            </div>
            <div>
              <b>{run.avgResponse.toFixed(2)}</b>
              <span>avg response</span>
            </div>
            {run.rejectedCount > 0 && (
              <div className="warn">
                <b>{run.rejectedCount}</b>
                <span>rejected (not enough CPU/memory)</span>
              </div>
            )}
          </div>

          <div className="scroll">
            <table>
              <thead>
                <tr>
                  <th>Workload</th>
                  <th>Node</th>
                  <th>Arrival</th>
                  <th>Burst</th>
                  <th>Start</th>
                  <th>Done</th>
                  <th>Waiting</th>
                  <th>Turnaround</th>
                  <th>Response</th>
                </tr>
              </thead>
              <tbody>
                {run.results.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <i
                        className="dot"
                        style={{
                          background: `hsl(${hue(r.workloadId)} 55% 42%)`,
                        }}
                      />
                      {r.workloadName}
                    </td>
                    <td>{r.nodeName}</td>
                    <td>{r.arrival}</td>
                    <td>{r.burst}</td>
                    <td>{r.start}</td>
                    <td>{r.completion}</td>
                    <td>{r.waiting}</td>
                    <td>{r.turnaround}</td>
                    <td>{r.response}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {runs.length > 0 && (
        <>
          <h3>Compare runs</h3>

          <div className="controls">
            <label>
              Show history
              <select
                value={historyPeriod}
                onChange={(e) => setHistoryPeriod(e.target.value)}
              >
                <option value="today">Today</option>
                <option value="7">Last 7 days</option>
                <option value="10">Last 10 days</option>
                <option value="all">All time</option>
              </select>
            </label>
          </div>

          {filteredRuns.length === 0 ? (
            <p className="empty">
              No scheduling runs found for this period.
            </p>
          ) : (
            <div className="scroll">
              <table className="history">
                <thead>
                  <tr>
                    <th>Run</th>
                    <th>Algorithm</th>
                    <th>Avg waiting</th>
                    <th>Avg turnaround</th>
                    <th>Avg response</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredRuns.map((r) => (
                    <tr
                      key={r.id}
                      className={run && r.id === run.id ? 'sel' : ''}
                      onClick={() => onSelect(r.id)}
                    >
                      <td>#{r.id}</td>
                      <td>
                        {r.algorithm === 'RR'
                          ? `RR (q=${r.quantum})`
                          : 'FCFS'}
                      </td>
                      <td>{r.avgWaiting.toFixed(2)}</td>
                      <td>{r.avgTurnaround.toFixed(2)}</td>
                      <td>{r.avgResponse.toFixed(2)}</td>
                      <td>{new Date(r.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  )
}
