import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import type { Role } from './types';
import Layout from './components/Layout';
import Login from './pages/Login';
import PatientHome from './pages/patient/PatientHome';
import MedicalCard from './pages/patient/MedicalCard';
import Contacts from './pages/patient/Contacts';
import Dashboard from './pages/dispatcher/Dashboard';
import IncidentPage from './pages/dispatcher/IncidentPage';
import IncidentForm from './pages/dispatcher/IncidentForm';
import Resources from './pages/dispatcher/Resources';
import CrewHome from './pages/crew/CrewHome';
import Incoming from './pages/doctor/Incoming';
import PatientDetail from './pages/doctor/PatientDetail';
import SystemStatusPage from './pages/admin/SystemStatus';
import Users from './pages/admin/Users';
import AuditLog from './pages/admin/AuditLog';

export function homeFor(role: Role): string {
  return { PATIENT: '/patient', DISPATCHER: '/dispatcher', CREW: '/crew', DOCTOR: '/doctor', ADMIN: '/admin' }[role];
}

function RequireRole({ role, children }: { role: Role; children: JSX.Element }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) return <Navigate to={homeFor(user.role)} replace />;
  return children;
}

export default function App() {
  const { user, loading } = useAuth();
  if (loading) return <div className="center-screen">Loading…</div>;

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={homeFor(user.role)} replace /> : <Login />} />
      <Route element={user ? <Layout /> : <Navigate to="/login" replace />}>
        <Route path="/patient" element={<RequireRole role="PATIENT"><PatientHome /></RequireRole>} />
        <Route path="/patient/card" element={<RequireRole role="PATIENT"><MedicalCard /></RequireRole>} />
        <Route path="/patient/contacts" element={<RequireRole role="PATIENT"><Contacts /></RequireRole>} />

        <Route path="/dispatcher" element={<RequireRole role="DISPATCHER"><Dashboard /></RequireRole>} />
        <Route path="/dispatcher/new" element={<RequireRole role="DISPATCHER"><IncidentForm /></RequireRole>} />
        <Route path="/dispatcher/incidents/:id" element={<RequireRole role="DISPATCHER"><IncidentPage /></RequireRole>} />
        <Route path="/dispatcher/incidents/:id/edit" element={<RequireRole role="DISPATCHER"><IncidentForm /></RequireRole>} />
        <Route path="/dispatcher/resources" element={<RequireRole role="DISPATCHER"><Resources /></RequireRole>} />

        <Route path="/crew" element={<RequireRole role="CREW"><CrewHome /></RequireRole>} />

        <Route path="/doctor" element={<RequireRole role="DOCTOR"><Incoming /></RequireRole>} />
        <Route path="/doctor/patients/:id" element={<RequireRole role="DOCTOR"><PatientDetail /></RequireRole>} />

        <Route path="/admin" element={<RequireRole role="ADMIN"><SystemStatusPage /></RequireRole>} />
        <Route path="/admin/users" element={<RequireRole role="ADMIN"><Users /></RequireRole>} />
        <Route path="/admin/audit" element={<RequireRole role="ADMIN"><AuditLog /></RequireRole>} />
      </Route>
      <Route path="*" element={<Navigate to={user ? homeFor(user.role) : '/login'} replace />} />
    </Routes>
  );
}
