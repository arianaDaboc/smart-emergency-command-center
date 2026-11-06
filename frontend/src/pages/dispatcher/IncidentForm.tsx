import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../../api';
import { useAction } from '../../components/hooks';
import { Card, ErrorBox, Field } from '../../components/ui';
import MapView from '../../components/MapView';
import { INCIDENT_TYPES, SEVERITIES, label } from '../../components/format';
import type { IncidentDetail, IncidentType, PatientSearchView, Severity } from '../../types';

/** Create Incident / Update Incident */
export default function IncidentForm() {
  const { id } = useParams();
  const nav = useNavigate();
  const editing = !!id;
  const [type, setType] = useState<IncidentType>('MEDICAL');
  const [severity, setSeverity] = useState<Severity>('MEDIUM');
  const [people, setPeople] = useState('1');
  const [description, setDescription] = useState('');
  const [pos, setPos] = useState<{ lat: number; lng: number } | null>(null);
  const [patient, setPatient] = useState<PatientSearchView | null>(null);
  const [q, setQ] = useState('');
  const [results, setResults] = useState<PatientSearchView[]>([]);
  const act = useAction();

  useEffect(() => {
    if (!id) return;
    api.get<IncidentDetail>(`/api/dispatcher/incidents/${id}`).then(({ incident: i }) => {
      setType(i.type); setSeverity(i.severity); setPeople(String(i.affectedPeople));
      setDescription(i.description ?? ''); setPos({ lat: i.lat, lng: i.lng });
      if (i.patientId) setPatient({ id: i.patientId, fullName: i.patientName ?? '', username: '' });
    });
  }, [id]);

  useEffect(() => {
    if (q.trim().length < 2) { setResults([]); return; }
    const t = setTimeout(() => {
      api.get<PatientSearchView[]>(`/api/dispatcher/patients?q=${encodeURIComponent(q)}`).then(setResults).catch(() => {});
    }, 250);
    return () => clearTimeout(t);
  }, [q]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!pos) { act.setError('Select the location on the map'); return; }
    act.run(async () => {
      const body = {
        type, severity, affectedPeople: Number(people), description: description || null,
        lat: pos.lat, lng: pos.lng, patientId: patient?.id ?? null,
      };
      const res = editing
        ? await api.put<IncidentDetail>(`/api/dispatcher/incidents/${id}`, body)
        : await api.post<IncidentDetail>('/api/dispatcher/incidents', body);
      nav(`/dispatcher/incidents/${res.incident.id}`);
    });
  };

  return (
    <div className="page">
      <form onSubmit={submit} className="two-col wide-right">
        <Card title={editing ? `Edit incident #${id}` : 'New incident (reported by phone)'}>
          <div className="form">
            <ErrorBox error={act.error} />
            <Field label="Type" error={act.fields.type}>
              <select value={type} onChange={(e) => setType(e.target.value as IncidentType)}>
                {INCIDENT_TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
              </select>
            </Field>
            <Field label="Severity" error={act.fields.severity}>
              <div className="seg">
                {SEVERITIES.map((s) => (
                  <button type="button" key={s} className={s === severity ? `on sev-${s.toLowerCase()}` : ''} onClick={() => setSeverity(s)}>{label(s)}</button>
                ))}
              </div>
            </Field>
            <Field label="Affected people" error={act.fields.affectedPeople}>
              <input type="number" min={1} value={people} onChange={(e) => setPeople(e.target.value)} />
            </Field>
            <Field label="Description">
              <textarea rows={3} maxLength={1000} value={description} onChange={(e) => setDescription(e.target.value)} />
            </Field>
            <Field label="Registered patient (optional)">
              {patient ? (
                <div className="row"><span className="chip on">{patient.fullName}</span>
                  <button type="button" className="btn ghost small" onClick={() => setPatient(null)}>Remove</button></div>
              ) : (
                <div className="search">
                  <input placeholder="Search by name…" value={q} onChange={(e) => setQ(e.target.value)} />
                  {results.length > 0 && (
                    <ul className="search-results">
                      {results.map((r) => (
                        <li key={r.id}><button type="button" onClick={() => { setPatient(r); setQ(''); setResults([]); }}>{r.fullName} <span className="muted">({r.username})</span></button></li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </Field>
            <Field label="Location" error={act.fields.lat}>
              <span className={pos ? '' : 'muted'}>{pos ? `${pos.lat.toFixed(5)}, ${pos.lng.toFixed(5)}` : 'Click on the map'}</span>
            </Field>
            <div className="row">
              <button className="btn primary" disabled={act.busy}>{editing ? 'Save changes' : 'Create incident'}</button>
              <button type="button" className="btn ghost" onClick={() => nav(-1)}>Cancel</button>
            </div>
          </div>
        </Card>
        <Card title="Location (click on the map)">
          <MapView onPick={(lat, lng) => setPos({ lat, lng })} picked={pos} height={520} />
        </Card>
      </form>
    </div>
  );
}
