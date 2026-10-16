async function req(path, opts = {}) {
  const res = await fetch('/api' + path, { headers: { 'Content-Type': 'application/json' }, ...opts })
  if (!res.ok) {
    let msg = `Request failed (${res.status}). Is the backend running on port 8080?`
    try { msg = (await res.json()).message || msg } catch { /* keep default */ }
    throw new Error(msg)
  }
  const text = await res.text()
  return text ? JSON.parse(text) : null
}
const post = (path, body) => req(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined })

export const api = {
  nodes: () => req('/nodes'),
  failNode: (id) => post(`/nodes/${id}/fail`),
  recoverNode: (id) => post(`/nodes/${id}/recover`),
  workloads: () => req('/workloads'),
  addWorkload: (w) => post('/workloads', w),
  startWorkload: (id) => post(`/workloads/${id}/start`),
  stopWorkload: (id) => post(`/workloads/${id}/stop`),
  deleteWorkload: (id) => req(`/workloads/${id}`, { method: 'DELETE' }),
  schedule: (algorithm, quantum) => post('/schedule', { algorithm, quantum }),
  runs: () => req('/schedule/runs'),
  logs: () => req('/logs'),
}
