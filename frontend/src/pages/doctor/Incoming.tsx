import { useNavigate } from 'react-router-dom';
import { api } from '../../api';
import { usePolling } from '../../components/hooks';
import { Card, Empty, ErrorBox, PriorityBadge, StatusBadge } from '../../components/ui';
import { label } from '../../components/format';
import type { IncomingPatientView } from '../../types';

/** View Incoming Patients (refreshed every 5 seconds) */
export default function Incoming() {
  const nav = useNavigate();
  const { data, error } = usePolling(() => api.get<IncomingPatientView[]>('/api/doctor/incoming'));
  return (
    <div className="page">
      <ErrorBox error={error} />
      <Card title="Incoming patients">
        {!data || data.length === 0 ? <Empty>No incoming patients.</Empty> : (
          <table className="table clickable">
            <thead><tr><th>Patient</th><th>Incident</th><th>Priority</th><th>Ambulance</th><th>Transport</th><th>ETA</th><th>Last condition</th><th>Case</th></tr></thead>
            <tbody>
              {data.map((p) => (
                <tr key={p.assignmentId} onClick={() => nav(`/doctor/patients/${p.assignmentId}`)}>
                  <td><b>{p.patientName}</b></td>
                  <td>#{p.incidentId} {label(p.incidentType)}</td>
                  <td><PriorityBadge value={p.priority} /></td>
                  <td>{p.ambulanceCode}</td>
                  <td>{p.transportStatus === 'HANDED_OVER' ? <span className="badge st-arrived">Arrived</span> : <StatusBadge value={p.transportStatus} />}</td>
                  <td>{p.etaMinutes != null ? `${p.etaMinutes} min` : '-'}</td>
                  <td>{p.lastCondition ? <span className={`badge cond-${p.lastCondition.toLowerCase()}`}>{label(p.lastCondition)}</span> : '-'}</td>
                  <td>{p.caseState === 'DRAFT' ? 'Draft' : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}
