import type { ReactNode } from 'react';
import { label, priorityLevel, TRANSPORT_STEPS } from './format';
import type { TransportStatus } from '../types';

export function PriorityBadge({ value }: { value: number }) {
  return <span className={`badge prio-${priorityLevel(value)}`} title="Priority score (0-100)">{value}</span>;
}

export function StatusBadge({ value }: { value: string | null | undefined }) {
  if (!value) return <span className="muted">-</span>;
  return <span className={`badge st-${value.toLowerCase()}`}>{label(value)}</span>;
}

export function ErrorBox({ error }: { error: string | null | undefined }) {
  return error ? <div className="alert error" role="alert">{error}</div> : null;
}

export function Card({ title, actions, children, className = '' }:
  { title?: ReactNode; actions?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="card-head">
          {title && <h2>{title}</h2>}
          {actions && <div className="card-actions">{actions}</div>}
        </header>
      )}
      {children}
    </section>
  );
}

export function Field({ label: text, error, children, hint }:
  { label: string; error?: string; children: ReactNode; hint?: string }) {
  return (
    <label className={`field ${error ? 'has-error' : ''}`}>
      <span className="field-label">{text}</span>
      {children}
      {hint && !error && <span className="field-hint">{hint}</span>}
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>;
}

/** Progress of the transport: Assigned -> ... -> Handed over */
export function TransportProgress({ status }: { status: TransportStatus | null }) {
  const idx = status ? TRANSPORT_STEPS.indexOf(status as typeof TRANSPORT_STEPS[number]) : -1;
  return (
    <ol className="progress">
      {TRANSPORT_STEPS.map((s, i) => (
        <li key={s} className={i < idx ? 'done' : i === idx ? 'current' : ''}>
          <span className="dot" />
          <span className="step-label">{label(s)}</span>
        </li>
      ))}
    </ol>
  );
}

export function KeyValue({ items }: { items: [string, ReactNode][] }) {
  return (
    <dl className="kv">
      {items.map(([k, v]) => (
        <div key={k}><dt>{k}</dt><dd>{v === null || v === undefined || v === '' ? <span className="muted">-</span> : v}</dd></div>
      ))}
    </dl>
  );
}
