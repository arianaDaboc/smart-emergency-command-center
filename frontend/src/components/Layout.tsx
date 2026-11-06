import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth';
import type { Role } from '../types';
import { label } from './format';

const NAV: Record<Role, { to: string; text: string }[]> = {
  PATIENT: [
    { to: '/patient', text: 'My emergency' },
    { to: '/patient/card', text: 'Medical card' },
    { to: '/patient/contacts', text: 'Emergency contacts' },
  ],
  DISPATCHER: [
    { to: '/dispatcher', text: 'Dashboard' },
    { to: '/dispatcher/new', text: 'New incident' },
    { to: '/dispatcher/resources', text: 'Resources' },
  ],
  CREW: [{ to: '/crew', text: 'Assigned emergency' }],
  DOCTOR: [{ to: '/doctor', text: 'Incoming patients' }],
  ADMIN: [
    { to: '/admin', text: 'System status' },
    { to: '/admin/users', text: 'Users' },
    { to: '/admin/audit', text: 'Audit log' },
  ],
};

export default function Layout() {
  const { user, logout } = useAuth();
  if (!user) return null;
  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo" aria-hidden>✚</span>
          <span>Smart Emergency <b>Command Center</b></span>
        </div>
        <nav>
          {NAV[user.role].map((n) => (
            <NavLink key={n.to} to={n.to} end>{n.text}</NavLink>
          ))}
        </nav>
        <div className="userbox">
          <span className="user-name">{user.fullName}</span>
          <span className="role-chip">{label(user.role)}</span>
          <button className="btn ghost small" onClick={logout}>Log out</button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
