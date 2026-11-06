import { useState, type FormEvent } from 'react';
import { api } from '../../api';
import { useAction, usePolling } from '../../components/hooks';
import { Card, ErrorBox, Field, StatusBadge } from '../../components/ui';
import MapView from '../../components/MapView';
import { label } from '../../components/format';
import type { ResourceType, ResourceView } from '../../types';

const TYPES: ResourceType[] = ['AMBULANCE', 'FIRE_TEAM', 'FIRE_TRUCK', 'POLICE_UNIT'];

/** Manage Resources */
export default function Resources() {
  const list = usePolling(() => api.get<ResourceView[]>('/api/dispatcher/resources'));
  const [code, setCode] = useState('');
  const [type, setType] = useState<ResourceType>('AMBULANCE');
  const [pos, setPos] = useState<{ lat: number; lng: number } | null>(null);
  const [filter, setFilter] = useState('');
  const act = useAction();

  const add = (e: FormEvent) => {
    e.preventDefault();
    if (!pos) { act.setError('Select the base location on the map'); return; }
    act.run(async () => {
      await api.post('/api/dispatcher/resources', { code, type, lat: pos.lat, lng: pos.lng });
      setCode(''); setPos(null);
      list.reload();
    });
  };

  const setStatus = (r: ResourceView, status: string) => act.run(async () => {
    await api.patch(`/api/dispatcher/resources/${r.id}/status`, { status });
    list.reload();
  });

  const rows = (list.data ?? []).filter((r) => !filter || r.type === filter);

  return (
    <div className="page">
      <ErrorBox error={act.error} />
      <div className="two-col wide-left">
        <Card title="Resources" actions={
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">All types</option>
            {TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
          </select>}>
          <div className="table-scroll">
            <table className="table">
              <thead><tr><th>Code</th><th>Type</th><th>Status</th><th>Crew</th><th>Incident</th><th /></tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td><b>{r.code}</b></td><td>{label(r.type)}</td><td><StatusBadge value={r.status} /></td>
                    <td>{r.crewName ?? '-'}</td><td>{r.currentIncidentId ? `#${r.currentIncidentId}` : '-'}</td>
                    <td className="actions">
                      {r.status === 'AVAILABLE' && <button className="btn ghost small" onClick={() => setStatus(r, 'OUT_OF_SERVICE')}>Out of service</button>}
                      {r.status === 'OUT_OF_SERVICE' && <button className="btn ghost small" onClick={() => setStatus(r, 'AVAILABLE')}>Set available</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
        <Card title="Add resource">
          <form className="form" onSubmit={add}>
            <Field label="Code" error={act.fields.code} hint="Unique, e.g. AMB-09">
              <input value={code} onChange={(e) => setCode(e.target.value)} required maxLength={20} />
            </Field>
            <Field label="Type">
              <select value={type} onChange={(e) => setType(e.target.value as ResourceType)}>
                {TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
              </select>
            </Field>
            <Field label="Base location">
              <span className={pos ? '' : 'muted'}>{pos ? `${pos.lat.toFixed(5)}, ${pos.lng.toFixed(5)}` : 'Click on the map'}</span>
            </Field>
            <MapView onPick={(lat, lng) => setPos({ lat, lng })} picked={pos} resources={list.data ?? []} height={260} />
            <button className="btn primary" disabled={act.busy}>Add resource</button>
          </form>
        </Card>
      </div>
    </div>
  );
}
