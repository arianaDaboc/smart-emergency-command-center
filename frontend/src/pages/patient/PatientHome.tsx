import { useState } from 'react';
import { api } from '../../api';
import { useAction, usePolling } from '../../components/hooks';
import { Card, Empty, ErrorBox, KeyValue, PriorityBadge, StatusBadge, TransportProgress } from '../../components/ui';
import { dateTime, label } from '../../components/format';
import type { IncidentType, IncidentView, PatientEmergencyView } from '../../types';

const SOS_TYPES: IncidentType[] = ['MEDICAL', 'FIRE', 'TRAFFIC_ACCIDENT', 'OTHER'];

function getPosition(): Promise<{ lat: number; lng: number } | null> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) return resolve(null);
    navigator.geolocation.getCurrentPosition(
      (p) => resolve({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => resolve(null),
      { timeout: 8000, enableHighAccuracy: true },
    );
  });
}

/** Activate Emergency Request, View Emergency Status, Cancel Emergency Request */
export default function PatientHome() {
  const current = usePolling(() => api.get<PatientEmergencyView | null>('/api/patient/emergencies/current'));
  const past = usePolling(() => api.get<IncidentView[]>('/api/patient/emergencies'), 15000);
  const [type, setType] = useState<IncidentType>('MEDICAL');
  const [note, setNote] = useState('');
  const [cancelOpen, setCancelOpen] = useState(false);
  const [reason, setReason] = useState('False alarm');
  const sos = useAction();
  const cancel = useAction();

  const sendSos = () => sos.run(async () => {
    const pos = await getPosition();
    const res = await api.post<PatientEmergencyView>('/api/patient/emergencies', {
      type, note: note.trim() || null, lat: pos?.lat ?? null, lng: pos?.lng ?? null,
    });
    current.setData(res);
    setNote('');
    past.reload();
  });

  const doCancel = () => cancel.run(async () => {
    if (!current.data) return;
    await api.post(`/api/patient/emergencies/${current.data.incident.id}/cancel`, { reason });
    setCancelOpen(false);
    current.reload();
    past.reload();
  });

  const e = current.data;
  const history = (past.data ?? []).filter((i) => !e || i.id !== e.incident.id);

  return (
    <div className="page">
      <ErrorBox error={current.error} />
      {e ? (
        <Card title={<>Emergency #{e.incident.id} <StatusBadge value={e.incident.status} /></>}
          actions={e.cancellable && <button className="btn danger-outline" onClick={() => setCancelOpen(true)}>Cancel request</button>}>
          {e.incident.status === 'NEW' && e.incident.noResources && (
            <div className="alert warn">No ambulance is available right now. A dispatcher will handle your request as soon as possible.</div>
          )}
          <TransportProgress status={e.transportStatus} />
          <KeyValue items={[
            ['Type', label(e.incident.type)],
            ['Priority', <PriorityBadge value={e.incident.priority} />],
            ['Sent at', dateTime(e.incident.createdAt)],
            ['Ambulance', e.ambulanceCode],
            ['Ambulance distance', e.ambulanceDistanceKm != null ? `${e.ambulanceDistanceKm} km` : null],
            ['Destination hospital', e.destinationHospital],
            ['Location', e.incident.locationApprox ? 'Approximate (home address)' : 'GPS'],
          ]} />
          {cancelOpen && (
            <div className="confirm">
              <p><b>Cancel this emergency request?</b></p>
              <div className="row">
                <select value={reason} onChange={(ev) => setReason(ev.target.value)}>
                  <option>False alarm</option>
                  <option>Help no longer needed</option>
                </select>
                <button className="btn danger" disabled={cancel.busy} onClick={doCancel}>Yes, cancel</button>
                <button className="btn ghost" onClick={() => setCancelOpen(false)}>No</button>
              </div>
              <ErrorBox error={cancel.error} />
            </div>
          )}
        </Card>
      ) : (
        !current.loading && (
          <Card className="sos-card">
            <div className="sos-wrap">
              <button className="sos" disabled={sos.busy} onClick={sendSos}>
                {sos.busy ? 'Sending…' : 'SOS'}
              </button>
              <div className="sos-side">
                <h2>Need emergency help?</h2>
                <p className="muted">Press SOS. Your location is sent automatically and the nearest ambulance is dispatched.</p>
                <div className="seg">
                  {SOS_TYPES.map((t) => (
                    <button key={t} className={t === type ? 'on' : ''} onClick={() => setType(t)}>{label(t)}</button>
                  ))}
                </div>
                <textarea placeholder="Short note (optional)" maxLength={500} value={note}
                  onChange={(ev) => setNote(ev.target.value)} rows={2} />
                <ErrorBox error={sos.error} />
              </div>
            </div>
          </Card>
        )
      )}

      <Card title="Previous emergencies">
        {history.length === 0 ? <Empty>No previous emergencies.</Empty> : (
          <table className="table">
            <thead><tr><th>#</th><th>Date</th><th>Type</th><th>Final status</th></tr></thead>
            <tbody>
              {history.map((i) => (
                <tr key={i.id}><td>{i.id}</td><td>{dateTime(i.createdAt)}</td><td>{label(i.type)}</td><td><StatusBadge value={i.status} /></td></tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
