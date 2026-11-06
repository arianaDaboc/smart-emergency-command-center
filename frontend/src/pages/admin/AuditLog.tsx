import { useState } from 'react';
import { api } from '../../api';
import { useAction, usePolling } from '../../components/hooks';
import { Card, Empty, ErrorBox } from '../../components/ui';
import { dateTime } from '../../components/format';
import type { AuditView, PageView } from '../../types';

/** View Audit Log, Export Audit Log */
export default function AuditLog() {
  const [username, setUsername] = useState('');
  const [action, setAction] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const exp = useAction();

  const query = () => {
    const p = new URLSearchParams();
    if (username) p.set('username', username);
    if (action) p.set('action', action);
    if (from) p.set('from', new Date(from).toISOString());
    if (to) p.set('to', new Date(to).toISOString());
    return p;
  };
  const log = usePolling(() => {
    const p = query(); p.set('page', String(page));
    return api.get<PageView<AuditView>>(`/api/admin/audit?${p}`);
  }, 0, [username, action, from, to, page]);

  const exportCsv = () => exp.run(() => api.download(`/api/admin/audit/export?${query()}`, 'audit-log.csv'));
  const d = log.data;

  return (
    <div className="page">
      <Card title="Audit log" actions={<button className="btn primary" disabled={exp.busy} onClick={exportCsv}>Export CSV</button>}>
        <div className="filters wrap">
          <input placeholder="Username" value={username} onChange={(e) => { setUsername(e.target.value); setPage(0); }} />
          <select value={action} onChange={(e) => { setAction(e.target.value); setPage(0); }}>
            <option value="">All actions</option><option>LOGIN</option><option>LOGIN_FAILED</option><option>REQUEST</option>
          </select>
          <label className="inline">From <input type="datetime-local" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} /></label>
          <label className="inline">To <input type="datetime-local" value={to} onChange={(e) => { setTo(e.target.value); setPage(0); }} /></label>
          <button className="btn ghost small" onClick={() => log.reload()}>Refresh</button>
        </div>
        <ErrorBox error={log.error ?? exp.error} />
        {!d || d.items.length === 0 ? <Empty>No entries.</Empty> : (
          <>
            <div className="table-scroll">
              <table className="table compact">
                <thead><tr><th>Time</th><th>User</th><th>IP</th><th>Action</th><th>Request</th><th>Status</th><th>ms</th></tr></thead>
                <tbody>
                  {d.items.map((a) => (
                    <tr key={a.id}>
                      <td>{dateTime(a.at)}</td><td>{a.username ?? '-'}</td><td>{a.ip}</td>
                      <td><span className={`badge act-${a.action.toLowerCase()}`}>{a.action}</span></td>
                      <td className="mono">{a.method} {a.path}</td>
                      <td className={a.status && a.status >= 400 ? 'abnormal' : ''}>{a.status}</td><td>{a.durationMs ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="pager">
              <button className="btn ghost small" disabled={page === 0} onClick={() => setPage(page - 1)}>← Newer</button>
              <span>Page {d.page + 1} / {Math.max(1, d.totalPages)} · {d.totalItems} entries</span>
              <button className="btn ghost small" disabled={page + 1 >= d.totalPages} onClick={() => setPage(page + 1)}>Older →</button>
            </div>
          </>
        )}
      </Card>
    </div>
  );
}
