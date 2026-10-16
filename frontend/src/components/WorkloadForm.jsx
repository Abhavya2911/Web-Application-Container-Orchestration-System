import { useState } from 'react'
import { api } from '../api.js'

const blank = { name: '', arrivalTime: 0, burstTime: 4, priority: 1, cpuRequired: 0.5, memoryRequired: 256 }

// 5 workloads with staggered arrivals, handy for showing FCFS vs Round Robin
const SAMPLE = [
  { name: 'shop-frontend', arrivalTime: 0, burstTime: 6, priority: 1, cpuRequired: 0.5, memoryRequired: 256 },
  { name: 'shop-api', arrivalTime: 1, burstTime: 3, priority: 1, cpuRequired: 0.5, memoryRequired: 256 },
  { name: 'blog', arrivalTime: 2, burstTime: 4, priority: 1, cpuRequired: 0.25, memoryRequired: 128 },
  { name: 'auth-service', arrivalTime: 3, burstTime: 2, priority: 1, cpuRequired: 0.5, memoryRequired: 256 },
  { name: 'reports', arrivalTime: 4, burstTime: 5, priority: 1, cpuRequired: 1, memoryRequired: 512 },
]

export default function WorkloadForm({ act }) {
  const [f, setF] = useState(blank)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    const body = { ...f, arrivalTime: +f.arrivalTime, burstTime: +f.burstTime, priority: +f.priority,
                   cpuRequired: +f.cpuRequired, memoryRequired: +f.memoryRequired }
    const ok = await act(() => api.addWorkload(body), `${f.name} submitted`)
    if (ok) setF(blank)
  }

  const addSamples = () => act(async () => { for (const w of SAMPLE) await api.addWorkload(w) }, 'Sample workloads added')

  return (
    <form className="panel" onSubmit={submit}>
      <h2>Submit a workload</h2>
      <label>Name<input required value={f.name} onChange={set('name')} placeholder="e.g. shop-api" /></label>
      <div className="row2">
        <label>Arrival time<input type="number" min="0" required value={f.arrivalTime} onChange={set('arrivalTime')} /></label>
        <label>Burst time<input type="number" min="1" required value={f.burstTime} onChange={set('burstTime')} /></label>
      </div>
      <div className="row2">
        <label>CPU (cores)<input type="number" min="0.1" step="0.05" required value={f.cpuRequired} onChange={set('cpuRequired')} /></label>
        <label>Memory (MB)<input type="number" min="16" required value={f.memoryRequired} onChange={set('memoryRequired')} /></label>
      </div>
      <label>Priority<input type="number" min="1" value={f.priority} onChange={set('priority')} /></label>
      <div className="actions">
        <button type="submit">Submit workload</button>
        <button type="button" className="ghost" onClick={addSamples}>Add 5 sample workloads</button>
      </div>
    </form>
  )
}
