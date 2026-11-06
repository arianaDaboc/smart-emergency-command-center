// Small fetch wrapper: adds the JWT token and turns error responses into ApiError.

const TOKEN_KEY = 'secc.token';

export class ApiError extends Error {
  constructor(public status: number, message: string, public fields?: Record<string, string>) {
    super(message);
  }
}

export function getToken(): string | null {
  try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
}
export function setToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch { /* storage unavailable */ }
}

let onUnauthorized: () => void = () => {};
export function setUnauthorizedHandler(fn: () => void) { onUnauthorized = fn; }

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  if (res.status === 401 && !url.endsWith('/auth/login')) {
    onUnauthorized();
  }
  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    let fields: Record<string, string> | undefined;
    try {
      const data = await res.json();
      if (data.message) msg = data.message;
      fields = data.fields;
    } catch { /* no json body */ }
    throw new ApiError(res.status, msg, fields);
  }
  if (res.status === 204) return null as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : null) as T;
}

export const api = {
  get: <T,>(url: string) => request<T>('GET', url),
  post: <T,>(url: string, body?: unknown) => request<T>('POST', url, body ?? {}),
  put: <T,>(url: string, body: unknown) => request<T>('PUT', url, body),
  patch: <T,>(url: string, body: unknown) => request<T>('PATCH', url, body),
  del: <T,>(url: string) => request<T>('DELETE', url),

  /** Downloads a file (e.g. CSV export) using the token. */
  async download(url: string, fallbackName: string) {
    const res = await fetch(url, { headers: { Authorization: `Bearer ${getToken()}` } });
    if (!res.ok) throw new ApiError(res.status, 'Download failed');
    const cd = res.headers.get('Content-Disposition') ?? '';
    const name = /filename="?([^"]+)"?/.exec(cd)?.[1] ?? fallbackName;
    const blob = await res.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = name;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(a.href);
  },
};
