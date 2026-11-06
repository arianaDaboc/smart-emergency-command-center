import { useState, type FormEvent } from 'react';
import { api } from '../../api';
import { useAction, usePolling } from '../../components/hooks';
import { Card, Empty, ErrorBox, Field } from '../../components/ui';
import type { ContactView } from '../../types';

const MAX = 5;
const EMPTY = { name: '', relationship: '', phone: '' };

/** Manage Emergency Contacts */
export default function Contacts() {
  const list = usePolling(() => api.get<ContactView[]>('/api/patient/contacts'), 0);
  const [form, setForm] = useState(EMPTY);
  const [editId, setEditId] = useState<number | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const act = useAction();
  const contacts = list.data ?? [];
  const full = contacts.length >= MAX && editId === null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    act.run(async () => {
      if (editId) await api.put(`/api/patient/contacts/${editId}`, form);
      else await api.post('/api/patient/contacts', form);
      setForm(EMPTY);
      setEditId(null);
      list.reload();
    });
  };

  const remove = (id: number) => act.run(async () => {
    await api.del(`/api/patient/contacts/${id}`);
    setConfirmId(null);
    list.reload();
  });

  return (
    <div className="page two-col">
      <Card title={`Emergency contacts (${contacts.length}/${MAX})`}>
        {contacts.length === 0 ? <Empty>No emergency contacts yet.</Empty> : (
          <table className="table">
            <thead><tr><th>Name</th><th>Relationship</th><th>Phone</th><th /></tr></thead>
            <tbody>
              {contacts.map((c) => (
                <tr key={c.id}>
                  <td>{c.name}</td><td>{c.relationship ?? '-'}</td><td>{c.phone}</td>
                  <td className="actions">
                    {confirmId === c.id ? (
                      <>
                        <button className="btn danger small" onClick={() => remove(c.id)}>Confirm</button>
                        <button className="btn ghost small" onClick={() => setConfirmId(null)}>No</button>
                      </>
                    ) : (
                      <>
                        <button className="btn ghost small" onClick={() => { setEditId(c.id); setForm({ name: c.name, relationship: c.relationship ?? '', phone: c.phone }); }}>Edit</button>
                        <button className="btn ghost small danger-text" onClick={() => setConfirmId(c.id)}>Delete</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
      <Card title={editId ? 'Edit contact' : 'Add contact'}>
        <form onSubmit={submit} className="form">
          <ErrorBox error={act.error} />
          <Field label="Name" error={act.fields.name}>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} disabled={full} />
          </Field>
          <Field label="Relationship">
            <input value={form.relationship} onChange={(e) => setForm({ ...form, relationship: e.target.value })} disabled={full} />
          </Field>
          <Field label="Phone" error={act.fields.phone} hint="8-15 digits, optional leading +">
            <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} disabled={full} />
          </Field>
          <div className="row">
            <button className="btn primary" disabled={act.busy || full}>Save</button>
            {editId && <button type="button" className="btn ghost" onClick={() => { setEditId(null); setForm(EMPTY); }}>Cancel</button>}
          </div>
          {full && <p className="muted small">The maximum of {MAX} contacts is reached.</p>}
        </form>
      </Card>
    </div>
  );
}
