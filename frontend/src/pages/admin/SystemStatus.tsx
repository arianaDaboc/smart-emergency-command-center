import { api } from '../../api';
import { usePolling } from '../../components/hooks';
import { Card, ErrorBox } from '../../components/ui';
import { label } from '../../components/format';
import type { SystemStatus } from '../../types';

function uptime(s: number) {
  const d = Math.floor(s / 86400), h = Math.floor((s % 86400) / 3600), m = Math.floor((s % 3600) / 60);
  return `${d ? `${d}d ` : ''}${h}h ${m}m`;
}

/** Monitor System Status (refreshed every 10 seconds) */
export default function SystemStatusPage() {
  const { data, error } = usePolling(() => api.get<SystemStatus>('/api/admin/status'), 10000);
  if (!data) return <div className="page"><ErrorBox error={error} /></div>;
  return (
    <div className="page">
      <ErrorBox error={error} />
      <div className="stats">
        <div className="stat"><span>Uptime</span><b>{uptime(data.uptimeSeconds)}</b></div>
        <div className="stat"><span>Version</span><b>{data.version}</b></div>
        <div className={`stat ${data.databaseStatus === 'OK' ? 'ok' : 'bad'}`}><span>Database</span>
          <b>{data.databaseStatus}{data.databaseResponseMs != null && <small> · {data.databaseResponseMs} ms</small>}</b></div>
        <div className="stat"><span>Active users (5 min)</span><b>{data.activeUsers}</b></div>
      </div>
      <div className="two-col">
        <Card title="Incidents by status">
          <div className="bars">
            {Object.entries(data.incidentsByStatus).map(([k, v]) => <Bar key={k} name={label(k)} value={v} max={Math.max(1, ...Object.values(data.incidentsByStatus))} />)}
          </div>
        </Card>
        <Card title="Resources by status">
          <div className="bars">
            {Object.entries(data.resourcesByStatus).map(([k, v]) => <Bar key={k} name={label(k)} value={v} max={Math.max(1, ...Object.values(data.resourcesByStatus))} />)}
          </div>
        </Card>
      </div>
    </div>
  );
}

function Bar({ name, value, max }: { name: string; value: number; max: number }) {
  return (
    <div className="bar-row">
      <span className="bar-name">{name}</span>
      <span className="bar-track"><span className="bar-fill" style={{ width: `${(value / max) * 100}%` }} /></span>
      <span className="bar-val">{value}</span>
    </div>
  );
}
