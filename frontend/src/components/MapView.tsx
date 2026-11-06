import { MapContainer, TileLayer, CircleMarker, Tooltip, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import type { HospitalView, IncidentView, ResourceView } from '../types';
import { label, priorityLevel } from './format';

const CLUJ: [number, number] = [46.7712, 23.6036];
const PRIO_COLORS = { high: '#d92d20', medium: '#f79009', low: '#12b76a' };

interface Props {
  incidents?: IncidentView[];
  resources?: ResourceView[];
  hospitals?: HospitalView[];
  selectedId?: number | null;
  onSelectIncident?: (id: number) => void;
  onPick?: (lat: number, lng: number) => void;
  picked?: { lat: number; lng: number } | null;
  height?: number | string;
  center?: [number, number];
  zoom?: number;
}

function ClickPicker({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  useMapEvents({ click: (e) => onPick(e.latlng.lat, e.latlng.lng) });
  return null;
}

/** OpenStreetMap map with incidents (colored by priority), resources and hospitals. */
export default function MapView({ incidents = [], resources = [], hospitals = [], selectedId, onSelectIncident,
  onPick, picked, height = 420, center, zoom = 13 }: Props) {
  return (
    <div className="map" style={{ height }}>
      <MapContainer center={center ?? CLUJ} zoom={zoom} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {onPick && <ClickPicker onPick={onPick} />}
        {hospitals.map((h) => (
          <CircleMarker key={`h${h.id}`} center={[h.lat, h.lng]} radius={9}
            pathOptions={{ color: '#1849a9', fillColor: '#ffffff', fillOpacity: 1, weight: 3 }}>
            <Tooltip>🏥 {h.name}</Tooltip>
          </CircleMarker>
        ))}
        {resources.map((r) => (
          <CircleMarker key={`r${r.id}`} center={[r.lat, r.lng]} radius={6}
            pathOptions={{
              color: '#fff', weight: 2, fillOpacity: 1,
              fillColor: r.status === 'AVAILABLE' ? '#12b76a' : r.status === 'BUSY' ? '#667085' : '#b42318',
            }}>
            <Tooltip>{r.code} · {label(r.type)} · {label(r.status)}</Tooltip>
          </CircleMarker>
        ))}
        {incidents.map((i) => (
          <CircleMarker key={`i${i.id}`} center={[i.lat, i.lng]} radius={i.id === selectedId ? 14 : 10}
            eventHandlers={{ click: () => onSelectIncident?.(i.id) }}
            pathOptions={{
              color: i.id === selectedId ? '#101828' : '#fff', weight: i.id === selectedId ? 3 : 2,
              fillColor: PRIO_COLORS[priorityLevel(i.priority)], fillOpacity: 0.95,
            }}>
            <Tooltip>#{i.id} {label(i.type)} · priority {i.priority} · {label(i.status)}</Tooltip>
          </CircleMarker>
        ))}
        {picked && (
          <CircleMarker center={[picked.lat, picked.lng]} radius={10}
            pathOptions={{ color: '#101828', fillColor: '#7a5af8', fillOpacity: 0.9, weight: 2 }}>
            <Tooltip permanent>Selected location</Tooltip>
          </CircleMarker>
        )}
      </MapContainer>
    </div>
  );
}
