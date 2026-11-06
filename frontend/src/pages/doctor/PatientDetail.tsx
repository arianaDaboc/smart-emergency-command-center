import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../../api';
import { useAction, usePolling } from '../../components/hooks';
import { Card, ErrorBox, Field, KeyValue, PriorityBadge, StatusBadge } from '../../components/ui';
import VitalsTable from '../../components/VitalsTable';
import { dateTime, label, time } from '../../components/format';
import type { DoctorPatientDetail, PatientStatus } from '../../types';

const STATUSES: PatientStatus[] = ['ADMITTED', 'DISCHARGED', 'TRANSFERRED', 'DECEASED'];

/** View Emergency Data, Record Diagnosis and Treatment, Finalize Medical Case */
export default function PatientDetail() {
  const { id } = useParams();
  const nav = useNavigate();
  const detail = usePolling(() => api.get<DoctorPatientDetail>(`/api/doctor/patients/${id}`), 5000, [id]);
  const [form, setForm] = useState({ diagnosis: '', treatment: '', medication: '', procedures: '', notes: '', patientStatus: 'ADMITTED' as PatientStatus });
  const [cardForm, setCardForm] = useState({ allergies: '', chronicConditions: '', currentMedication: '' });
  const [loadedFor, setLoadedFor] = useState<string | null>(null);
  const [finalizing, setFinalizing] = useState(false);
  const [allergyWarn, setAllergyWarn] = useState<string | null>(null);
  const save = useAction();
  const fin = useAction();
  const d = detail.data;

  // fill the forms once per patient (polling must not overwrite what the doctor is typing)
  useEffect(() => {
    if (!d || loadedFor === id) return;
    const c = d.medicalCase;
    if (c) setForm({ diagnosis: c.diagnosis, treatment: c.treatment ?? '', medication: c.medication ?? '', procedures: c.procedures ?? '', notes: c.notes ?? '', patientStatus: c.patientStatus });
    if (d.card) setCardForm({ allergies: d.card.allergies ?? '', chronicConditions: d.card.chronicConditions ?? '', currentMedication: d.card.currentMedication ?? '' });
    setLoadedFor(id ?? null);
  }, [d, id, loadedFor]);

  if (!d) return <div className="page"><ErrorBox error={detail.error} /></div>;
  const i = d.incident;
  const arrived = d.transportStatus === 'HANDED_OVER';
  const finalized = d.medicalCase?.finalized ?? false;

  // allergies of the patient that appear in the prescribed medication / treatment
  const conflicts = () => {
    const allergies = (d.card?.allergies ?? '').split(/[,;]/).map((x) => x.trim().toLowerCase())
      .filter((x) => x.length > 2 && !['none', 'none known', 'no'].includes(x));
    const text = `${form.medication} ${form.treatment}`.toLowerCase();
    return allergies.filter((a) => text.includes(a));
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const c = conflicts();
    if (c.length > 0 && allergyWarn === null) {
      setAllergyWarn(`The patient is allergic to: ${c.join(', ')}. Press Save again to confirm.`);
      return;
    }
    setAllergyWarn(null);
    save.run(async () => detail.setData(await api.put<DoctorPatientDetail>(`/api/doctor/patients/${id}/case`, form)));
  };
  const finalize = () => fin.run(async () => {
    detail.setData(await api.post<DoctorPatientDetail>(`/api/doctor/patients/${id}/case/finalize`, d.card ? cardForm : {}));
    setFinalizing(false);
  });

  return (
    <div className="page">
      <div className="page-head">
        <button className="btn ghost small" onClick={() => nav('/doctor')}>← Incoming patients</button>
        <h1>{d.card?.fullName ?? 'Unknown patient'} <PriorityBadge value={i.priority} /> <StatusBadge value={d.transportStatus} /></h1>
      </div>
      <div className="two-col">
        <div className="stack">
          <Card title="Medical card (check before treatment)">
            {d.card ? (
              <KeyValue items={[
                ['Age', d.card.age], ['Blood type', d.card.bloodType && <span className="blood">{d.card.bloodType}</span>],
                ['Allergies', d.card.allergies && <b className="danger-text">{d.card.allergies}</b>],
                ['Chronic conditions', d.card.chronicConditions], ['Current medication', d.card.currentMedication],
                ['Medical notes', d.card.medicalNotes],
                ['Emergency contacts', d.card.contacts.map((c) => `${c.name} ${c.phone}`).join(', ')],
              ]} />
            ) : <p className="muted">The patient is not registered.</p>}
          </Card>
          <Card title="Medical history">
            {d.history.length === 0 ? <p className="muted">No previous events.</p> : (
              <ul className="timeline">{d.history.map((h) => (
                <li key={h.id}><div className="tl-date">{dateTime(h.date)}</div>
                  <div className="tl-body"><b>{h.diagnosis}</b>{h.treatment && <div>{h.treatment}</div>}<div className="muted small">{h.hospital}{h.doctor ? ` · ${h.doctor}` : ''}</div></div></li>
              ))}</ul>
            )}
          </Card>
          <Card title={`Emergency #${i.id}`}>
            <KeyValue items={[
              ['Type', label(i.type)], ['Severity', label(i.severity)], ['Description', i.description],
              ['Ambulance', d.ambulanceCode], ['Hand-over note', d.handoverNote],
              ['Timeline', <ul className="steps">{d.timeline.map((t, k) => <li key={k}><StatusBadge value={t.status} /> {time(t.at)}</li>)}</ul>],
            ]} />
          </Card>
        </div>
        <div className="stack">
          <Card title="Vital signs and interventions (transport)"><VitalsTable vitals={d.vitals} /></Card>
          <Card title={finalized ? 'Medical case (finalized)' : 'Diagnosis and treatment'}>
            {!arrived && <div className="alert info">The form is available after the crew hands the patient over.</div>}
            {finalized ? (
              <KeyValue items={[
                ['Diagnosis', d.medicalCase!.diagnosis], ['Treatment', d.medicalCase!.treatment], ['Medication', d.medicalCase!.medication],
                ['Procedures', d.medicalCase!.procedures], ['Notes', d.medicalCase!.notes], ['Patient status', label(d.medicalCase!.patientStatus)],
                ['Finalized', `${dateTime(d.medicalCase!.finalizedAt)} by ${d.medicalCase!.doctor}`],
              ]} />
            ) : (
              <form className="form" onSubmit={submit}>
                <ErrorBox error={save.error} />
                {allergyWarn && <div className="alert warn">⚠ {allergyWarn}</div>}
                <Field label="Diagnosis *" error={save.fields.diagnosis}><textarea rows={2} value={form.diagnosis} disabled={!arrived} onChange={(e) => setForm({ ...form, diagnosis: e.target.value })} /></Field>
                <Field label="Treatment"><textarea rows={2} value={form.treatment} disabled={!arrived} onChange={(e) => setForm({ ...form, treatment: e.target.value })} /></Field>
                <Field label="Medication prescribed"><input value={form.medication} disabled={!arrived} onChange={(e) => { setAllergyWarn(null); setForm({ ...form, medication: e.target.value }); }} /></Field>
                <Field label="Medical procedures"><input value={form.procedures} disabled={!arrived} onChange={(e) => setForm({ ...form, procedures: e.target.value })} /></Field>
                <Field label="Doctor's notes"><textarea rows={2} value={form.notes} disabled={!arrived} onChange={(e) => setForm({ ...form, notes: e.target.value })} /></Field>
                <Field label="Patient status">
                  <select value={form.patientStatus} disabled={!arrived} onChange={(e) => setForm({ ...form, patientStatus: e.target.value as PatientStatus })}>
                    {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
                  </select>
                </Field>
                <div className="row">
                  <button className="btn primary" disabled={!arrived || save.busy}>Save</button>
                  {d.medicalCase && <button type="button" className="btn success" onClick={() => setFinalizing(true)}>Finalize case…</button>}
                </div>
              </form>
            )}
          </Card>
          {finalizing && !finalized && (
            <Card title="Finalize medical case" className="confirm">
              <p className="muted">A new entry is added to the medical history. You can update the medical card below.</p>
              <ErrorBox error={fin.error} />
              {d.card && (
                <div className="form">
                  <Field label="Allergies"><input value={cardForm.allergies} onChange={(e) => setCardForm({ ...cardForm, allergies: e.target.value })} /></Field>
                  <Field label="Chronic conditions"><input value={cardForm.chronicConditions} onChange={(e) => setCardForm({ ...cardForm, chronicConditions: e.target.value })} /></Field>
                  <Field label="Current medication"><input value={cardForm.currentMedication} onChange={(e) => setCardForm({ ...cardForm, currentMedication: e.target.value })} /></Field>
                </div>
              )}
              <div className="row">
                <button className="btn success" disabled={fin.busy} onClick={finalize}>Finalize</button>
                <button className="btn ghost" onClick={() => setFinalizing(false)}>Cancel</button>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
