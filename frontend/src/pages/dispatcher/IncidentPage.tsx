import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../../api';
import { useAction, usePolling } from '../../components/hooks';
import { Card, ErrorBox, KeyValue, PriorityBadge, StatusBadge } from '../../components/ui';
import MapView from '../../components/MapView';
import { dateTime, label } from '../../components/format';
import type { IncidentDetail, ResourceView } from '../../types';

/** Incident details: Assign Resources, Close Incident, link to Update Incident. */
export default function IncidentPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const detail = usePolling(() => api.get<IncidentDetail>(`/api/dispatcher/incidents/${id}`), 5000, [id]);
  const resources = usePolling(() => api.get<ResourceView[]>('/api/dispatcher/resources'));
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [manual, setManual] = useState(false);
  const [closeNote, setCloseNote] = useState('');
  const [closing, setClosing] = useState(false);
  const act = useAction();

  const d = detail.data;
  const rec = d?.recommendation;
  const recommendedIds = rec ? rec.needs.flatMap((n) => n.candidates.map((c) => c.id)) : [];

  useEffect(() => { setSelected(new Set()); setManual(false); }, [id]);

  if (!d) return <div className="page"><ErrorBox error={detail.error} /></div>;
  const i = d.incident;
  const open = !['CLOSED', 'CANCELLED'].includes(i.status);
  const available = (resources.data ?? []).filter((r) => r.status === 'AVAILABLE');

  const assign = (ids: number[]) => act.run(async () => {
    const res = await api.post<IncidentDetail>(`/api/dispatcher/incidents/${i.id}/assign`, { resourceIds: ids });
    detail.setData(res);
    setSelected(new Set());
    setManual(false);
    resources.reload();
  }).then((r) => { if (!r) detail.reload(); });

  const close = () => act.run(async () => {
    const res = await api.post<IncidentDetail>(`/api/dispatcher/incidents/${i.id}/close`, { note: closeNote || null });
    detail.setData(res);
    setClosing(false);
  });

  const toggle = (rid: number) => {
    const s = new Set(selected);
    if (s.has(rid)) s.delete(rid); else s.add(rid);
    setSelected(s);
  };

  return (
    <div className="page">
      <div className="page-head">
        <button className="btn ghost small" onClick={() => nav('/dispatcher')}>← Dashboard</button>
        <h1>Incident #{i.id} <StatusBadge value={i.status} /> <PriorityBadge value={i.priority} /></h1>
        <div className="row">
          {open && <Link className="btn ghost" to={`/dispatcher/incidents/${i.id}/edit`}>Edit</Link>}
          {open && <button className="btn danger-outline" onClick={() => setClosing(true)}>Close incident</button>}
        </div>
      </div>
      <ErrorBox error={act.error} />
      {closing && (
        <Card className="confirm">
          <p><b>Close incident #{i.id}?</b> All resources still assigned will become available.</p>
          <textarea rows={2} placeholder="Closing note (optional)" value={closeNote} onChange={(e) => setCloseNote(e.target.value)} />
          <div className="row">
            <button className="btn danger" disabled={act.busy} onClick={close}>Close incident</button>
            <button className="btn ghost" onClick={() => setClosing(false)}>Cancel</button>
          </div>
        </Card>
      )}
      <div className="two-col">
        <div className="stack">
          <Card title="Details">
            <KeyValue items={[
              ['Type', label(i.type)],
              ['Severity', label(i.severity)],
              ['Affected people', i.affectedPeople],
              ['Source', label(i.source)],
              ['Patient', i.patientName],
              ['Description', i.description],
              ['Created', dateTime(i.createdAt)],
              ['Waiting time', `${i.waitingMinutes} min`],
              ['Closed', i.closedAt && `${dateTime(i.closedAt)} ${i.closingNote ? `- ${i.closingNote}` : ''}`],
              ['Cancel reason', i.cancelReason],
            ]} />
          </Card>
          <Card title="Assigned resources">
            {i.resources.length === 0 ? <p className="muted">No resources assigned.</p> : (
              <table className="table">
                <thead><tr><th>Code</th><th>Type</th><th>Transport</th><th>Hospital</th><th>Distance</th></tr></thead>
                <tbody>
                  {i.resources.map((a) => (
                    <tr key={a.id}><td>{a.resourceCode}</td><td>{label(a.resourceType)}</td>
                      <td><StatusBadge value={a.transportStatus} /></td><td>{a.destinationHospital ?? '-'}</td><td>{a.distanceKm} km</td></tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>
        </div>
        <div className="stack">
          {open && rec && (
            <Card title="Recommended resources (Smart Dispatch)"
              actions={<button className="btn ghost small" onClick={() => setManual(!manual)}>{manual ? 'Show recommendation' : 'Choose manually'}</button>}>
              {!manual ? (
                <>
                  <table className="table">
                    <thead><tr><th>Type</th><th>Needed</th><th>Assigned</th><th>Recommended</th></tr></thead>
                    <tbody>
                      {rec.needs.map((n) => (
                        <tr key={n.type}>
                          <td>{label(n.type)}</td><td>{n.needed}</td><td>{n.alreadyAssigned}</td>
                          <td>
                            {n.candidates.map((c) => <span key={c.id} className="chip">{c.code} · {c.distanceKm} km</span>)}
                            {n.missing > 0 && <span className="tag danger">missing: {n.missing}</span>}
                            {n.candidates.length === 0 && n.missing === 0 && <span className="muted">complete</span>}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <button className="btn primary" disabled={act.busy || recommendedIds.length === 0}
                    onClick={() => assign(recommendedIds)}>Accept recommendation</button>
                </>
              ) : (
                <>
                  <div className="table-scroll small-h">
                    <table className="table">
                      <thead><tr><th /><th>Code</th><th>Type</th><th>Distance</th></tr></thead>
                      <tbody>
                        {available.map((r) => (
                          <tr key={r.id}>
                            <td><input type="checkbox" checked={selected.has(r.id)} onChange={() => toggle(r.id)} /></td>
                            <td>{r.code}</td><td>{label(r.type)}</td>
                            <td>{(Math.round(haversine(r.lat, r.lng, i.lat, i.lng) * 10) / 10)} km</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  <button className="btn primary" disabled={act.busy || selected.size === 0}
                    onClick={() => assign([...selected])}>Assign {selected.size || ''} selected</button>
                </>
              )}
            </Card>
          )}
          <Card title="Location">
            <MapView incidents={[i]} selectedId={i.id} resources={resources.data ?? []} center={[i.lat, i.lng]} zoom={14} height={320} />
          </Card>
        </div>
      </div>
    </div>
  );
}

function haversine(lat1: number, lng1: number, lat2: number, lng2: number) {
  const R = 6371, toRad = (x: number) => (x * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1), dLng = toRad(lng2 - lng1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(a));
}
