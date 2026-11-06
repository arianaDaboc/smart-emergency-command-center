import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '../api';

/** Loads data immediately and then every `intervalMs` (default 5 s, as required for the operational screens). */
export function usePolling<T>(loader: () => Promise<T>, intervalMs = 5000, deps: unknown[] = []) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  const reload = useCallback(async () => {
    try {
      const d = await loaderRef.current();
      setData(d);
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Server unreachable');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    reload();
    if (intervalMs <= 0) return;
    const t = setInterval(reload, intervalMs);
    return () => clearInterval(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reload, intervalMs, ...deps]);

  return { data, error, loading, reload, setData };
}

/** Runs an action and keeps its error message / field errors / busy flag. */
export function useAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});

  const run = useCallback(async <T,>(fn: () => Promise<T>): Promise<T | undefined> => {
    setBusy(true);
    setError(null);
    setFields({});
    try {
      return await fn();
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.message);
        setFields(e.fields ?? {});
      } else {
        setError('Server unreachable');
      }
      return undefined;
    } finally {
      setBusy(false);
    }
  }, []);

  return { busy, error, fields, run, setError };
}
