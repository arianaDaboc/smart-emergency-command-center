import { useState, type FormEvent } from 'react';
import { api } from '../../api';
import { useAuth } from '../../auth';
import { useAction, usePolling } from '../../components/hooks';
import { Card, ErrorBox, Field } from '../../components/ui';
import { dateTime, label } from '../../components/format';
import type { HospitalView, ResourceView, Role, UserView } from '../../types';

const ROLES: Role[] = ['PATIENT', 'DISPATCHER', 'CREW', 'DOCTOR', 'ADMIN'];
const EMPTY = { username: '', fullName: '', password: '', role: 'PATIENT' as Role, hospitalId: '', ambulanceId: '', dateOfBirth: '', bloodType: '' };

/** Manage User Accounts */
export default function Users() {
  const { user: me } = useAuth();
  const users = usePolling(() => api.get<UserView[]>('/api/admin/users'), 0);
  const hospitals = usePolling(() => api.get<HospitalView[]>('/api/common/hospitals'), 0);
  const ambulances = usePolling(() => api.get<ResourceView[]>('/api/admin/ambulances'), 0);
  const [form, setForm] = useState(EMPTY);
  const [edit, setEdit] = useState<UserView | null>(null);
  const [roleFilter, setRoleFilter] = useState('');
  const act = useAction();

  const startEdit = (u: UserView) => {
    setEdit(u);
    setForm({ ...EMPTY, username: u.username, fullName: u.fullName, role: u.role,
      hospitalId: u.hospitalId ? String(u.hospitalId) : '', ambulanceId: u.ambulanceId ? String(u.ambulanceId) : '' });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const id = (x: string) => (x ? Number(x) : null);
    act.run(async () => {
      if (edit) {
        await api.put(`/api/admin/users/${edit.id}`, { fullName: form.fullName, hospitalId: id(form.hospitalId),
          ambulanceId: id(form.ambulanceId), newPassword: form.password || null });
      } else {
        await api.post('/api/admin/users', { username: form.username, fullName: form.fullName, password: form.password,
          role: form.role, hospitalId: id(form.hospitalId), ambulanceId: id(form.ambulanceId),
          dateOfBirth: form.dateOfBirth || null, bloodType: form.bloodType || null });
      }
      setForm(EMPTY); setEdit(null); users.reload(); ambulances.reload();
    });
  };

  const toggle = (u: UserView) => act.run(async () => {
    await api.post(`/api/admin/users/${u.id}/enabled`, { enabled: !u.enabled });
    users.reload();
  });

  const list = (users.data ?? []).filter((u) => !roleFilter || u.role === roleFilter);

  return (
    <div className="page two-col wide-left">
      <Card title="Users" actions={
        <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
          <option value="">All roles</option>{ROLES.map((r) => <option key={r} value={r}>{label(r)}</option>)}
        </select>}>
        <div className="table-scroll">
          <table className="table">
            <thead><tr><th>Username</th><th>Full name</th><th>Role</th><th>Link</th><th>Status</th><th>Last login</th><th /></tr></thead>
            <tbody>
              {list.map((u) => (
                <tr key={u.id} className={u.enabled ? '' : 'row-disabled'}>
                  <td><b>{u.username}</b></td><td>{u.fullName}</td><td>{label(u.role)}</td>
                  <td>{u.hospitalName ?? u.ambulanceCode ?? '-'}</td>
                  <td>{u.enabled ? <span className="badge st-available">Active</span> : <span className="badge st-cancelled">Disabled</span>}</td>
                  <td>{dateTime(u.lastLogin)}</td>
                  <td className="actions">
                    <button className="btn ghost small" onClick={() => startEdit(u)}>Edit</button>
                    {u.id !== me?.userId && <button className="btn ghost small" onClick={() => toggle(u)}>{u.enabled ? 'Disable' : 'Enable'}</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
      <Card title={edit ? `Edit ${edit.username}` : 'Create user'}>
        <form className="form" onSubmit={submit}>
          <ErrorBox error={act.error} />
          {!edit && (
            <>
              <Field label="Username" error={act.fields.username}><input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></Field>
              <Field label="Role">
                <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
                  {ROLES.map((r) => <option key={r} value={r}>{label(r)}</option>)}
                </select>
              </Field>
            </>
          )}
          <Field label="Full name" error={act.fields.fullName}><input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></Field>
          <Field label={edit ? 'New password (optional)' : 'Password'} error={act.fields.password ?? act.fields.newPassword} hint="min. 8 characters">
            <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </Field>
          {form.role === 'DOCTOR' && (
            <Field label="Hospital">
              <select value={form.hospitalId} onChange={(e) => setForm({ ...form, hospitalId: e.target.value })}>
                <option value="">Select…</option>{(hospitals.data ?? []).map((h) => <option key={h.id} value={h.id}>{h.name}</option>)}
              </select>
            </Field>
          )}
          {form.role === 'CREW' && (
            <Field label="Ambulance">
              <select value={form.ambulanceId} onChange={(e) => setForm({ ...form, ambulanceId: e.target.value })}>
                <option value="">Select…</option>
                {(ambulances.data ?? []).map((a) => <option key={a.id} value={a.id}>{a.code}{a.crewName ? ` (${a.crewName})` : ''}</option>)}
              </select>
            </Field>
          )}
          {form.role === 'PATIENT' && !edit && (
            <div className="row">
              <Field label="Date of birth"><input type="date" value={form.dateOfBirth} onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} /></Field>
              <Field label="Blood type"><input value={form.bloodType} maxLength={5} onChange={(e) => setForm({ ...form, bloodType: e.target.value })} /></Field>
            </div>
          )}
          <div className="row">
            <button className="btn primary" disabled={act.busy}>{edit ? 'Save' : 'Create'}</button>
            {edit && <button type="button" className="btn ghost" onClick={() => { setEdit(null); setForm(EMPTY); }}>Cancel</button>}
          </div>
        </form>
      </Card>
    </div>
  );
}
