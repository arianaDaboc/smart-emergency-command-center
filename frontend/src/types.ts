// Types mirroring the DTOs of the Spring Boot REST API (com.smartemergency.web.Dto)

export type Role = 'PATIENT' | 'DISPATCHER' | 'CREW' | 'DOCTOR' | 'ADMIN';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IncidentType = 'MEDICAL' | 'FIRE' | 'TRAFFIC_ACCIDENT' | 'BUILDING_COLLAPSE' | 'FLOOD' | 'OTHER';
export type IncidentStatus = 'NEW' | 'DISPATCHED' | 'IN_PROGRESS' | 'AT_HOSPITAL' | 'CLOSED' | 'CANCELLED';
export type IncidentSource = 'SOS' | 'PHONE' | 'SIMULATED';
export type ResourceType = 'AMBULANCE' | 'FIRE_TEAM' | 'FIRE_TRUCK' | 'POLICE_UNIT';
export type ResourceStatus = 'AVAILABLE' | 'BUSY' | 'OUT_OF_SERVICE';
export type TransportStatus =
  | 'ASSIGNED' | 'EN_ROUTE' | 'ON_SCENE' | 'TRANSPORTING' | 'AT_HOSPITAL' | 'HANDED_OVER'
  | 'CANCELLED' | 'NO_TRANSPORT';
export type PatientCondition = 'STABLE' | 'SERIOUS' | 'CRITICAL';
export type PatientStatus = 'ADMITTED' | 'DISCHARGED' | 'TRANSFERRED' | 'DECEASED';

export interface LoginResponse { token: string; userId: number; username: string; fullName: string; role: Role }
export interface HospitalView { id: number; name: string; lat: number; lng: number }

export interface ResourceView {
  id: number; code: string; type: ResourceType; status: ResourceStatus;
  lat: number; lng: number; crewName: string | null; currentIncidentId: number | null;
}
export interface AssignmentView {
  id: number; resourceId: number; resourceCode: string; resourceType: ResourceType; active: boolean;
  transportStatus: TransportStatus | null; destinationHospitalId: number | null; destinationHospital: string | null;
  assignedAt: string; distanceKm: number;
}
export interface IncidentView {
  id: number; type: IncidentType; severity: Severity; affectedPeople: number; description: string | null;
  lat: number; lng: number; locationApprox: boolean; source: IncidentSource; status: IncidentStatus;
  priority: number; patientId: number | null; patientName: string | null; createdAt: string;
  firstDispatchedAt: string | null; closedAt: string | null; closingNote: string | null; cancelReason: string | null;
  waitingMinutes: number; resources: AssignmentView[]; noResources: boolean; readyToClose: boolean;
}
export interface ContactView { id: number; name: string; relationship: string | null; phone: string }
export interface MedicalCardView {
  patientId: number; fullName: string; dateOfBirth: string | null; age: number | null; bloodType: string | null;
  allergies: string | null; chronicConditions: string | null; currentMedication: string | null;
  medicalNotes: string | null; contacts: ContactView[];
}
export interface HistoryView {
  id: number; date: string; hospital: string | null; doctor: string | null; incidentId: number | null;
  diagnosis: string; treatment: string | null;
}
export interface TimelineView { status: TransportStatus; at: string }
export interface VitalView {
  id: number; recordedAt: string; pulse: number; spo2: number; systolic: number; diastolic: number;
  temperature: number; oxygenLpm: number | null; iv: boolean; medication: string | null;
  interventions: string | null; condition: PatientCondition; observations: string | null; abnormal: string[];
}
export interface PatientEmergencyView {
  incident: IncidentView; ambulanceCode: string | null; transportStatus: TransportStatus | null;
  ambulanceDistanceKm: number | null; destinationHospital: string | null; cancellable: boolean;
}
export interface MedicalCardAndHistory { card: MedicalCardView; history: HistoryView[] }

export interface Candidate { id: number; code: string; type: ResourceType; distanceKm: number }
export interface Need { type: ResourceType; needed: number; alreadyAssigned: number; candidates: Candidate[]; missing: number }
export interface Recommendation { incidentId: number; priority: number; needs: Need[] }
export interface IncidentDetail { incident: IncidentView; recommendation: Recommendation | null }
export interface PatientSearchView { id: number; fullName: string; username: string }

export interface EmergencyPatientData {
  fullName: string; age: number | null; bloodType: string | null; allergies: string | null;
  chronicConditions: string | null; currentMedication: string | null; medicalNotes: string | null;
  contacts: ContactView[];
}
export interface CrewAssignmentView {
  assignmentId: number; ambulance: ResourceView; incident: IncidentView; patient: EmergencyPatientData | null;
  transportStatus: TransportStatus; nextStatus: TransportStatus | null; destinationHospital: HospitalView | null;
  nearestHospitalId: number | null; distanceKm: number; recordLocked: boolean;
  timeline: TimelineView[]; vitals: VitalView[];
}
export interface CrewHome { ambulance: ResourceView; assignment: CrewAssignmentView | null }

export interface IncomingPatientView {
  assignmentId: number; incidentId: number; patientName: string; incidentType: IncidentType; priority: number;
  ambulanceCode: string; transportStatus: TransportStatus; etaMinutes: number | null;
  lastCondition: PatientCondition | null; caseState: 'NONE' | 'DRAFT';
}
export interface CaseView {
  id: number; diagnosis: string; treatment: string | null; medication: string | null; procedures: string | null;
  notes: string | null; patientStatus: PatientStatus; finalized: boolean; finalizedAt: string | null; doctor: string | null;
}
export interface DoctorPatientDetail {
  assignmentId: number; incident: IncidentView; ambulanceCode: string; transportStatus: TransportStatus;
  handoverNote: string | null; card: MedicalCardView | null; history: HistoryView[];
  timeline: TimelineView[]; vitals: VitalView[]; medicalCase: CaseView | null;
}

export interface UserView {
  id: number; username: string; fullName: string; role: Role; enabled: boolean; lastLogin: string | null;
  hospitalId: number | null; hospitalName: string | null; ambulanceId: number | null; ambulanceCode: string | null;
}
export interface SystemStatus {
  uptimeSeconds: number; version: string; databaseStatus: 'OK' | 'ERROR'; databaseResponseMs: number | null;
  activeUsers: number; incidentsByStatus: Record<string, number>; resourcesByStatus: Record<string, number>;
}
export interface AuditView {
  id: number; at: string; username: string | null; ip: string | null; action: string; method: string | null;
  path: string | null; status: number | null; durationMs: number | null;
}
export interface PageView<T> { items: T[]; page: number; totalPages: number; totalItems: number }
