import { useCallback, useEffect, useState } from 'react'
import { api } from './api.js'
import NodePanel from './components/NodePanel.jsx'
import WorkloadForm from './components/WorkloadForm.jsx'
import WorkloadTable from './components/WorkloadTable.jsx'
import SchedulerPanel from './components/SchedulerPanel.jsx'

export default function App() {
  const [nodes, setNodes] = useState([])
  const [workloads, setWorkloads] = useState([])
  const [runs, setRuns] = useState([])
  const [logs, setLogs] = useState([])
  const [selectedRunId, setSelectedRunId] = useState(null)
  const [notice, setNotice] = useState(null) // { type: 'ok' | 'error', text }

  const refresh = useCallback(async () => {
    try {
      const [n, w, r, l] = await Promise.all([api.nodes(), api.workloads(), api.runs(), api.logs()])
      setNodes(n); setWorkloads(w); setRuns(r); setLogs(l)
    } catch (e) {
      setNotice({ type: 'error', text: e.message })
    }
  }, [])

  useEffect(() => {
    refresh()
    const t = setInterval(refresh, 5000)
    return () => clearInterval(t)
  }, [refresh])

  // runs an API action, shows the result, then reloads everything
  const act = useCallback(async (fn, okText) => {
    try {
      const out = await fn()
      if (okText) setNotice({ type: 'ok', text: okText })
      await refresh()
      return out
    } catch (e) {
      setNotice({ type: 'error', text: e.message })
    }
  }, [refresh])

  const runScheduler = async (algorithm, quantum) => {
    const run = await act(() => api.schedule(algorithm, quantum), `${algorithm} schedule created`)
    if (run) setSelectedRunId(run.id)
  }

  const selectedRun = runs.find((r) => r.id === selectedRunId) || runs[0] || null

  return (
    <div className="app">
      <header className="top">
        <div>
          <h1>Container Orchestrator</h1>
          <p>Team Elite · Schedule web-app containers across worker nodes</p>
        </div>
      </header>

      {notice && (
        <div className={`notice ${notice.type}`} role="status">
          <span>{notice.text}</span>
          <button onClick={() => setNotice(null)} aria-label="Dismiss">×</button>
        </div>
      )}

      <main className="grid">
        <section className="col">
          <NodePanel nodes={nodes} act={act} />
          <WorkloadForm act={act} />
        </section>
        <section className="col wide">
          <SchedulerPanel run={selectedRun} runs={runs} onRun={runScheduler} onSelect={setSelectedRunId}
                          pending={workloads.filter((w) => w.status === 'PENDING' || w.status === 'REJECTED').length} />
          <WorkloadTable workloads={workloads} act={act} />
          <div className="panel">
            <h2>Activity</h2>
            {logs.length === 0 ? <p className="empty">Nothing has happened yet.</p> : (
              <ul className="log">
                {logs.map((l) => (
                  <li key={l.id} className={l.level.toLowerCase()}>
                    <time>{new Date(l.createdAt).toLocaleTimeString()}</time> {l.message}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>
      </main>
    </div>
  )
}
