export const label = (v: string | null | undefined) =>
  v ? v.charAt(0) + v.slice(1).toLowerCase().replace(/_/g, ' ') : '-';

export const dateTime = (iso: string | null | undefined) =>
  iso ? new Date(iso).toLocaleString('en-GB', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-';

export const time = (iso: string | null | undefined) =>
  iso ? new Date(iso).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '-';

export const priorityLevel = (p: number) => (p >= 80 ? 'high' : p >= 50 ? 'medium' : 'low');

export const TRANSPORT_STEPS = ['ASSIGNED', 'EN_ROUTE', 'ON_SCENE', 'TRANSPORTING', 'AT_HOSPITAL', 'HANDED_OVER'] as const;

export const INCIDENT_TYPES = ['MEDICAL', 'FIRE', 'TRAFFIC_ACCIDENT', 'BUILDING_COLLAPSE', 'FLOOD', 'OTHER'] as const;
export const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
