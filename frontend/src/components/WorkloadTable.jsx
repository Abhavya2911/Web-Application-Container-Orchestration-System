import { api } from '../api.js'

export default function WorkloadTable({ workloads, act }) {
  return (
    <div className="panel">
      <h2>Workloads</h2>
      {workloads.length === 0 ? <p className="empty">No workloads yet. Submit one, or add the sample set.</p> : (
        <div className="scroll">
          <table>
            <thead>
              <tr><th>Name</th><th>Arrival</th><th>Burst</th><th>CPU</th><th>Memory</th><th>Status</th><th>Node</th><th>Container</th><th></th></tr>
            </thead>
            <tbody>
              {workloads.map((w) => (
                <tr key={w.id}>
                  <td>{w.name}</td><td>{w.arrivalTime}</td><td>{w.burstTime}</td>
                  <td>{w.cpuRequired}</td><td>{w.memoryRequired} MB</td>
                  <td><span className={`badge s-${w.status.toLowerCase()}`}>{w.status.toLowerCase()}</span></td>
                  <td>{w.assignedNode?.name ?? '–'}</td>
                  <td>{w.hostPort
                    ? <a href={`http://localhost:${w.hostPort}`} target="_blank" rel="noreferrer">{w.containerId} :{w.hostPort}</a>
                    : '–'}</td>
                  <td className="rowact">
                    {w.status === 'SCHEDULED' && <button className="small" onClick={() => act(() => api.startWorkload(w.id), `${w.name} started`)}>Start</button>}
                    {(w.status === 'SCHEDULED' || w.status === 'RUNNING') && <button className="small ghost" onClick={() => act(() => api.stopWorkload(w.id), `${w.name} stopped`)}>Stop</button>}
                    <button className="small ghost danger" onClick={() => act(() => api.deleteWorkload(w.id), `${w.name} deleted`)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
