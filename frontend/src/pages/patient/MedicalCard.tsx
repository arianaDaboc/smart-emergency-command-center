import { api } from '../../api';
import { usePolling } from '../../components/hooks';
import { Card, Empty, ErrorBox, KeyValue } from '../../components/ui';
import { dateTime } from '../../components/format';
import type { MedicalCardAndHistory } from '../../types';

/** View Medical Card and History (read-only for the patient) */
export default function MedicalCard() {
  const { data, error } = usePolling(() => api.get<MedicalCardAndHistory>('/api/patient/medical-card'), 0);
  if (!data) return <ErrorBox error={error} />;
  const c = data.card;
  return (
    <div className="page two-col">
      <Card title="Medical card">
        <KeyValue items={[
          ['Full name', c.fullName],
          ['Date of birth', c.dateOfBirth ? `${c.dateOfBirth} (${c.age} years)` : null],
          ['Blood type', c.bloodType && <span className="blood">{c.bloodType}</span>],
          ['Allergies', c.allergies],
          ['Chronic conditions', c.chronicConditions],
          ['Current medication', c.currentMedication],
          ['Medical notes', c.medicalNotes],
          ['Emergency contacts', c.contacts.map((x) => `${x.name} (${x.relationship ?? '-'}) ${x.phone}`).join(', ')],
        ]} />
        <p className="muted small">Medical fields are updated only by doctors.</p>
      </Card>
      <Card title="Medical history">
        {data.history.length === 0 ? <Empty>No medical events recorded.</Empty> : (
          <ul className="timeline">
            {data.history.map((h) => (
              <li key={h.id}>
                <div className="tl-date">{dateTime(h.date)}</div>
                <div className="tl-body">
                  <b>{h.diagnosis}</b>
                  {h.treatment && <div>Treatment: {h.treatment}</div>}
                  <div className="muted small">
                    {h.hospital ?? 'Unknown hospital'}{h.doctor ? ` · ${h.doctor}` : ''}{h.incidentId ? ` · incident #${h.incidentId}` : ''}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
