import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api';
import { usePolling } from '../../components/hooks';
import { Card, Empty, ErrorBox, PriorityBadge, StatusBadge } from '../../components/ui';
import MapView from '../../components/MapView';
import { INCIDENT_TYPES, label } from '../../components/format';
import type { HospitalView, IncidentView, ResourceView } from '../../types';

/** Monitor Incidents: table ordered by priority + map, refreshed every 5 seconds. */
export default function Dashboard() {
  const nav = useNavigate();
  const [scope, setScope] = useState<'open' | 'all'>('open');
  const [type, setType] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const incidents = usePolling(
    () => api.get<IncidentView[]>(`/api/dispatcher/incidents?scope=${scope}${type ? `&type=${type}` : ''}`), 5000, [scope, type]);
  const resources = usePolling(() => api.get<ResourceView[]>('/api/dispatcher/resources'));
  const hospitals = usePolling(() => api.get<HospitalView[]>('/api/common/hospitals'), 0);

  const list = (incidents.data ?? []).filter((i) => !statusFilter || i.status === statusFilter);
  const open = list.filter((i) => !['CLOSED', 'CANCELLED'].includes(i.status));
  const available = (resources.data ?? []).filter((r) => r.status === 'AVAILABLE').length;

  return (
    <div className="page">
      <div className="stats">
        <div className="stat"><span>Open incidents</span><b>{open.length}</b></div>
        <div className="stat warn"><span>Without resources</span><b>{open.filter((i) => i.noResources).length}</b></div>
        <div className="stat"><span>Critical priority (≥ 80)</span><b>{open.filter((i) => i.priority >= 80).length}</b></div>
        <div className="stat ok"><span>Available resources</span><b>{available}</b></div>
      </div>
      <ErrorBox error={incidents.error} />
      <div className="dash">
        <Card title="Incidents" className="dash-list"
          actions={
            <div className="filters">
              <select value={scope} onChange={(e) => setScope(e.target.value as 'open' | 'all')}>
                <option value="open">Open</option><option value="all">All</option>
              </select>
              <select value={type} onChange={(e) => setType(e.target.value)}>
                <option value="">All types</option>
                {INCIDENT_TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
              </select>
              <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                <option value="">All statuses</option>
                {['NEW', 'DISPATCHED', 'IN_PROGRESS', 'AT_HOSPITAL', 'CLOSED', 'CANCELLED'].map((s) => <option key={s} value={s}>{label(s)}</option>)}
              </select>
            </div>
          }>
          {list.length === 0 ? <Empty>No incidents.</Empty> : (
            <div className="table-scroll">
              <table className="table clickable">
                <thead>
                  <tr><th>#</th><th>Prio</th><th>Type</th><th>Severity</th><th>People</th><th>Status</th><th>Resources</th><th>Waiting</th></tr>
                </thead>
                <tbody>
                  {list.map((i) => (
                    <tr key={i.id} onClick={() => nav(`/dispatcher/incidents/${i.id}`)} className={i.noResources ? 'row-alert' : ''}>
                      <td>{i.id}{i.source === 'SOS' && <span className="tag tag-sos">SOS</span>}</td>
                      <td><PriorityBadge value={i.priority} /></td>
                      <td>{label(i.type)}</td>
                      <td>{label(i.severity)}</td>
                      <td>{i.affectedPeople}</td>
                      <td><StatusBadge value={i.status} />{i.readyToClose && <span className="tag ready">Ready to close</span>}</td>
                      <td>{i.noResources ? <span className="tag danger">No resources</span> : i.resources.filter((r) => r.active).map((r) => r.resourceCode).join(', ') || '-'}</td>
                      <td>{i.waitingMinutes} min</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
        <Card title="Map" className="dash-map">
          <MapView incidents={open} resources={resources.data ?? []} hospitals={hospitals.data ?? []}
            onSelectIncident={(id) => nav(`/dispatcher/incidents/${id}`)} height={520} />
          <div className="legend">
            <span><i className="lg prio-high" />Priority ≥ 80</span>
            <span><i className="lg prio-medium" />50-79</span>
            <span><i className="lg prio-low" />&lt; 50</span>
            <span><i className="lg res-av" />Available resource</span>
            <span><i className="lg res-busy" />Busy resource</span>
            <span><i className="lg hosp" />Hospital</span>
          </div>
        </Card>
      </div>
    </div>
  );
}
