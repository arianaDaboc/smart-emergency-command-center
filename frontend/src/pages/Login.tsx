import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { ApiError } from '../api';
import { homeFor } from '../App';

const DEMO = ['patient1', 'dispatcher', 'crew1', 'doctor1', 'admin'];

export default function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const u = await login(username, password);
      nav(homeFor(u.role), { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Server unreachable');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit}>
        <div className="login-logo">✚</div>
        <h1>Smart Emergency Command Center</h1>
        <p className="muted">Sign in with your account</p>
        {error && <div className="alert error">{error}</div>}
        <label className="field">
          <span className="field-label">Username</span>
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
        </label>
        <label className="field">
          <span className="field-label">Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="btn primary block" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        <div className="demo">
          <span className="muted">Demo accounts (password <code>password123</code>):</span>
          <div className="demo-list">
            {DEMO.map((d) => (
              <button type="button" key={d} className="chip" onClick={() => { setUsername(d); setPassword('password123'); }}>{d}</button>
            ))}
          </div>
        </div>
      </form>
    </div>
  );
}
