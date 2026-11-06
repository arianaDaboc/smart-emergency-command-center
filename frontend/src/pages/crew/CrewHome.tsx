import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../../api';
import { useAction, usePolling } from '../../components/hooks';
import { Card, Empty, ErrorBox, Field, KeyValue, PriorityBadge, StatusBadge, TransportProgress } from '../../components/ui';
import MapView from '../../components/MapView';
import VitalsTable from '../../components/VitalsTable';
import { label, time } from '../../components/format';
import type { CrewHome as CrewHomeT, HospitalView, PatientCondition } from '../../types';

const EMPTY_VITALS = {
  pulse: '', spo2: '', systolic: '', diastolic: '', temperature: '', oxygenLpm: '', iv: false,
  medication: '', interventions: '', condition: 'STABLE' as PatientCondition, observations: '',
};

/** View Assigned Emergency, Update Transport Status, Record Vital Signs and Interventions, Hand Over Patient */
export default function CrewHome() {
  const home = usePolling(() => api.get<CrewHomeT>('/api/crew/assignment'));
  const hospitals = usePolling(() => api.get<HospitalView[]>('/api/common/hospitals'), 0);
  const [hospitalId, setHospitalId] = useState<number | ''>('');
  const [v, setV] = useState(EMPTY_VITALS);
  const [note, setNote] = useState('');
  const [tab, setTab] = useState<'emergency' | 'record'>('emergency');
  const act = useAction();
  const vit = useAction();
  const [lastSeen, setLastSeen] = useState<number | null>(null);

  const a = home.data?.assignment ?? null;
  useEffect(() => {
    if (a && hospitalId === '' && a.nearestHospitalId) setHospitalId(a.nearestHospitalId);
  }, [a, hospitalId]);

  const isNew = a && a.transportStatus === 'ASSIGNED' && lastSeen !== a.assignmentId;

  const advance = () => act.run(async () => {
    if (!a?.nextStatus) return;
    const res = await api.post<CrewHomeT>('/api/crew/assignment/status', {
      status: a.nextStatus, hospitalId: a.nextStatus === 'TRANSPORTING' ? hospitalId || null : null,
    });
    setLastSeen(a.assignmentId);
    home.setData(res);
    if (a.nextStatus === 'ON_SCENE') setTab('record');
  });

  const noTransport = () => act.run(async () => home.setData(await api.post<CrewHomeT>('/api/crew/assignment/no-transport')));
  const handover = () => act.run(async () => {
    home.setData(await api.post<CrewHomeT>('/api/crew/assignment/handover', { note: note || null }));
    setNote(''); setTab('emergency'); setHospitalId('');
  });

  const saveVitals = (e: FormEvent) => {
    e.preventDefault();
    vit.run(async () => {
      const num = (x: string) => (x === '' ? null : Number(x));
      const res = await api.post<CrewHomeT>('/api/crew/assignment/vitals', {
        pulse: num(v.pulse), spo2: num(v.spo2), systolic: num(v.systolic), diastolic: num(v.diastolic),
        temperature: num(v.temperature), oxygenLpm: num(v.oxygenLpm) ?? 0, iv: v.iv,
        medication: v.medication || null, interventions: v.interventions || null,
        condition: v.condition, observations: v.observations || null,
      });
      home.setData(res);
      setV({ ...EMPTY_VITALS, condition: v.condition });
    });
  };

  if (!home.data) return <div className="page"><ErrorBox error={home.error} /></div>;
  const amb = home.data.ambulance;

  if (!a) {
    return (
      <div className="page">
        <Card title={`Ambulance ${amb.code}`}>
          <Empty>
            <div className="big-icon">🚑</div>
            No active emergency. Ambulance status: <StatusBadge value={amb.status} />
            <div className="muted small">This screen refreshes automatically every 5 seconds.</div>
          </Empty>
        </Card>
      </div>
    );
  }

  const i = a.incident;
  const canVitals = ['ON_SCENE', 'TRANSPORTING', 'AT_HOSPITAL'].includes(a.transportStatus);
  const actionLabel: Record<string, string> = {
    EN_ROUTE: 'Start: en route', ON_SCENE: 'Arrived on scene', TRANSPORTING: 'Start transport', AT_HOSPITAL: 'Arrived at hospital',
  };

  return (
    <div className="page">
      {isNew && <div className="alert new-emergency" onClick={() => setLastSeen(a.assignmentId)}>🚨 NEW EMERGENCY – incident #{i.id}, {label(i.type)}, priority {i.priority}</div>}
      <Card title={<>Ambulance {amb.code} · Incident #{i.id} <PriorityBadge value={i.priority} /></>}
        actions={<div className="seg">
          <button className={tab === 'emergency' ? 'on' : ''} onClick={() => setTab('emergency')}>Emergency</button>
          <button className={tab === 'record' ? 'on' : ''} onClick={() => setTab('record')}>Transport record ({a.vitals.length})</button>
        </div>}>
        <TransportProgress status={a.transportStatus} />
        <ErrorBox error={act.error} />
        <div className="crew-actions">
          {a.nextStatus === 'TRANSPORTING' && (
            <label className="inline">Destination hospital:&nbsp;
              <select value={hospitalId} onChange={(e) => setHospitalId(Number(e.target.value))}>
                {(hospitals.data ?? []).map((h) => <option key={h.id} value={h.id}>{h.name}{h.id === a.nearestHospitalId ? ' (nearest)' : ''}</option>)}
              </select>
            </label>
          )}
          {a.nextStatus && <button className="btn primary big" disabled={act.busy} onClick={advance}>{actionLabel[a.nextStatus]}</button>}
          {a.transportStatus === 'ON_SCENE' && <button className="btn ghost" disabled={act.busy} onClick={noTransport}>No transport needed</button>}
        </div>
        {a.transportStatus === 'AT_HOSPITAL' && (
          <div className="handover">
            <h3>Hand over to {a.destinationHospital?.name}</h3>
            <textarea rows={2} placeholder="Hand-over note for the doctor" value={note} onChange={(e) => setNote(e.target.value)} />
            <button className="btn success" disabled={act.busy} onClick={handover}>Hand over to hospital</button>
          </div>
        )}
      </Card>

      {tab === 'emergency' ? (
        <div className="two-col">
          <div className="stack">
            <Card title="Emergency">
              <KeyValue items={[
                ['Type', label(i.type)], ['Severity', label(i.severity)], ['Affected people', i.affectedPeople],
                ['Description', i.description], ['Distance', `${a.distanceKm} km`],
                ['Location', `${i.lat.toFixed(5)}, ${i.lng.toFixed(5)}${i.locationApprox ? ' (approximate)' : ''}`],
                ['Destination', a.destinationHospital?.name],
              ]} />
            </Card>
            <Card title="Patient medical card">
              {a.patient ? (
                <KeyValue items={[
                  ['Name', a.patient.fullName], ['Age', a.patient.age], ['Blood type', a.patient.bloodType && <span className="blood">{a.patient.bloodType}</span>],
                  ['Allergies', a.patient.allergies && <b className="danger-text">{a.patient.allergies}</b>],
                  ['Chronic conditions', a.patient.chronicConditions], ['Current medication', a.patient.currentMedication],
                  ['Medical notes', a.patient.medicalNotes],
                  ['Emergency contacts', a.patient.contacts.map((c) => `${c.name} (${c.relationship ?? '-'}) ${c.phone}`).join(', ')],
                ]} />
              ) : <p className="muted">No medical card - unknown patient.</p>}
              {a.patient?.allergies && <div className="alert error small">Check the allergies before administering any medication.</div>}
            </Card>
            <Card title="Timeline">
              <ul className="steps">{a.timeline.map((t, k) => <li key={k}><StatusBadge value={t.status} /> {time(t.at)}</li>)}</ul>
            </Card>
          </div>
          <Card title="Map">
            <MapView incidents={[i]} selectedId={i.id} resources={[a.ambulance]} hospitals={hospitals.data ?? []}
              center={[i.lat, i.lng]} zoom={14} height={480} />
          </Card>
        </div>
      ) : (
        <div className="stack">
          {canVitals && !a.recordLocked && (
            <Card title="Record vital signs and interventions">
              <form className="form grid-form" onSubmit={saveVitals}>
                <ErrorBox error={vit.error} />
                <Field label="Pulse (bpm)" error={vit.fields.pulse} hint="20-250"><input type="number" value={v.pulse} onChange={(e) => setV({ ...v, pulse: e.target.value })} /></Field>
                <Field label="SpO2 (%)" error={vit.fields.spo2} hint="50-100"><input type="number" value={v.spo2} onChange={(e) => setV({ ...v, spo2: e.target.value })} /></Field>
                <Field label="Systolic (mmHg)" error={vit.fields.systolic} hint="50-260"><input type="number" value={v.systolic} onChange={(e) => setV({ ...v, systolic: e.target.value })} /></Field>
                <Field label="Diastolic (mmHg)" error={vit.fields.diastolic} hint="30-160"><input type="number" value={v.diastolic} onChange={(e) => setV({ ...v, diastolic: e.target.value })} /></Field>
                <Field label="Temperature (°C)" error={vit.fields.temperature} hint="30-45"><input type="number" step="0.1" value={v.temperature} onChange={(e) => setV({ ...v, temperature: e.target.value })} /></Field>
                <Field label="Oxygen (l/min)" error={vit.fields.oxygenLpm} hint="0 if none"><input type="number" step="0.5" value={v.oxygenLpm} onChange={(e) => setV({ ...v, oxygenLpm: e.target.value })} /></Field>
                <Field label="Condition">
                  <select value={v.condition} onChange={(e) => setV({ ...v, condition: e.target.value as PatientCondition })}>
                    <option value="STABLE">Stable</option><option value="SERIOUS">Serious</option><option value="CRITICAL">Critical</option>
                  </select>
                </Field>
                <Field label="IV administered"><label className="check"><input type="checkbox" checked={v.iv} onChange={(e) => setV({ ...v, iv: e.target.checked })} /> Yes</label></Field>
                <Field label="Medication administered"><input value={v.medication} onChange={(e) => setV({ ...v, medication: e.target.value })} /></Field>
                <Field label="Other interventions"><input value={v.interventions} onChange={(e) => setV({ ...v, interventions: e.target.value })} /></Field>
                <Field label="Observations"><input value={v.observations} onChange={(e) => setV({ ...v, observations: e.target.value })} /></Field>
                <div className="form-end"><button className="btn primary" disabled={vit.busy}>Save measurement</button></div>
              </form>
            </Card>
          )}
          {!canVitals && <div className="alert info">Vital signs can be recorded from the moment the crew is on scene.</div>}
          <Card title="Emergency Transport Record"><VitalsTable vitals={a.vitals} /></Card>
        </div>
      )}
    </div>
  );
}
