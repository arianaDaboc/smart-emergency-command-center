import type { VitalView } from '../types';
import { label, time } from './format';
import { Empty } from './ui';

/** Measurements of the Emergency Transport Record; abnormal values are shown in red. */
export default function VitalsTable({ vitals }: { vitals: VitalView[] }) {
  if (vitals.length === 0) return <Empty>No vital signs recorded yet.</Empty>;
  const cls = (v: VitalView, f: string) => (v.abnormal.includes(f) ? 'abnormal' : '');
  return (
    <div className="table-scroll">
      <table className="table compact">
        <thead>
          <tr><th>Time</th><th>Pulse</th><th>SpO2</th><th>BP</th><th>Temp</th><th>O₂</th><th>IV</th><th>Condition</th><th>Medication / interventions</th><th>Observations</th></tr>
        </thead>
        <tbody>
          {vitals.map((v) => (
            <tr key={v.id}>
              <td>{time(v.recordedAt)}</td>
              <td className={cls(v, 'pulse')}>{v.pulse}</td>
              <td className={cls(v, 'spo2')}>{v.spo2}%</td>
              <td className={cls(v, 'systolic')}>{v.systolic}/{v.diastolic}</td>
              <td className={cls(v, 'temperature')}>{v.temperature}°C</td>
              <td>{v.oxygenLpm ? `${v.oxygenLpm} l/min` : '-'}</td>
              <td>{v.iv ? 'Yes' : 'No'}</td>
              <td><span className={`badge cond-${v.condition.toLowerCase()}`}>{label(v.condition)}</span></td>
              <td>{[v.medication, v.interventions].filter(Boolean).join(' · ') || '-'}</td>
              <td>{v.observations ?? '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
