package com.smartemergency.web;

import jakarta.validation.constraints.*;
import com.smartemergency.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response objects of the REST API. */
public final class Dto {
    private Dto() {}

    // ---------- auth ----------
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, Long userId, String username, String fullName, Role role) {}

    // ---------- common ----------
    public record HospitalView(Long id, String name, double lat, double lng) {}

    public record ResourceView(Long id, String code, ResourceType type, ResourceStatus status,
                               double lat, double lng, String crewName, Long currentIncidentId) {}

    public record AssignmentView(Long id, Long resourceId, String resourceCode, ResourceType resourceType,
                                 boolean active, TransportStatus transportStatus, Long destinationHospitalId,
                                 String destinationHospital, Instant assignedAt, double distanceKm) {}

    public record IncidentView(Long id, IncidentType type, Severity severity, int affectedPeople, String description,
                               double lat, double lng, boolean locationApprox, IncidentSource source,
                               IncidentStatus status, int priority, Long patientId, String patientName,
                               Instant createdAt, Instant firstDispatchedAt, Instant closedAt,
                               String closingNote, String cancelReason, long waitingMinutes,
                               List<AssignmentView> resources, boolean noResources, boolean readyToClose) {}

    public record ContactView(Long id, String name, String relationship, String phone) {}

    public record MedicalCardView(Long patientId, String fullName, LocalDate dateOfBirth, Integer age,
                                  String bloodType, String allergies, String chronicConditions,
                                  String currentMedication, String medicalNotes, List<ContactView> contacts) {}

    public record HistoryView(Long id, Instant date, String hospital, String doctor, Long incidentId,
                              String diagnosis, String treatment) {}

    public record TimelineView(TransportStatus status, Instant at) {}

    public record VitalView(Long id, Instant recordedAt, Integer pulse, Integer spo2, Integer systolic,
                            Integer diastolic, BigDecimal temperature, BigDecimal oxygenLpm, boolean iv,
                            String medication, String interventions, PatientCondition condition,
                            String observations, List<String> abnormal) {}

    // ---------- patient ----------
    public record SosRequest(IncidentType type, @Size(max = 500) String note, Double lat, Double lng) {}
    public record CancelRequest(@NotBlank String reason) {}
    public record ContactRequest(@NotBlank(message = "Name is required") @Size(max = 120) String name,
                                 @Size(max = 60) String relationship,
                                 @NotBlank(message = "Phone is required")
                                 @Pattern(regexp = "^\\+?[0-9 ]{8,15}$", message = "Phone must have 8-15 digits, optional leading +")
                                 String phone) {}
    public record PatientEmergencyView(IncidentView incident, String ambulanceCode, TransportStatus transportStatus,
                                       Double ambulanceDistanceKm, String destinationHospital, boolean cancellable) {}
    public record MedicalCardAndHistory(MedicalCardView card, List<HistoryView> history) {}

    // ---------- dispatcher ----------
    public record IncidentRequest(@NotNull(message = "Type is required") IncidentType type,
                                  @NotNull(message = "Severity is required") Severity severity,
                                  @NotNull @Min(value = 1, message = "At least 1 affected person") Integer affectedPeople,
                                  @Size(max = 1000) String description,
                                  @NotNull(message = "Location is required") Double lat,
                                  @NotNull(message = "Location is required") Double lng,
                                  Long patientId) {}
    public record Candidate(Long id, String code, ResourceType type, double distanceKm) {}
    public record Need(ResourceType type, int needed, int alreadyAssigned, List<Candidate> candidates, int missing) {}
    public record Recommendation(Long incidentId, int priority, List<Need> needs) {}
    public record AssignRequest(@NotEmpty List<Long> resourceIds) {}
    public record CloseRequest(@Size(max = 1000) String note) {}
    public record ResourceRequest(@NotBlank @Size(max = 20) String code, @NotNull ResourceType type,
                                  @NotNull Double lat, @NotNull Double lng) {}
    public record ResourceStatusRequest(@NotNull ResourceStatus status) {}
    public record PatientSearchView(Long id, String fullName, String username) {}
    public record IncidentDetail(IncidentView incident, Recommendation recommendation) {}

    // ---------- crew ----------
    /** Medical Card as seen by the ambulance crew (no medical history). */
    public record EmergencyPatientData(String fullName, Integer age, String bloodType, String allergies,
                                       String chronicConditions, String currentMedication, String medicalNotes,
                                       List<ContactView> contacts) {}
    public record CrewAssignmentView(Long assignmentId, ResourceView ambulance, IncidentView incident,
                                     EmergencyPatientData patient, TransportStatus transportStatus,
                                     TransportStatus nextStatus, HospitalView destinationHospital,
                                     Long nearestHospitalId, double distanceKm, boolean recordLocked,
                                     List<TimelineView> timeline, List<VitalView> vitals) {}
    public record CrewHome(ResourceView ambulance, CrewAssignmentView assignment) {}
    public record StatusRequest(@NotNull TransportStatus status, Long hospitalId) {}
    public record VitalRequest(@NotNull @Min(20) @Max(250) Integer pulse,
                               @NotNull @Min(50) @Max(100) Integer spo2,
                               @NotNull @Min(50) @Max(260) Integer systolic,
                               @NotNull @Min(30) @Max(160) Integer diastolic,
                               @NotNull @DecimalMin("30.0") @DecimalMax("45.0") BigDecimal temperature,
                               @DecimalMin("0.0") @DecimalMax("25.0") BigDecimal oxygenLpm,
                               boolean iv,
                               @Size(max = 500) String medication,
                               @Size(max = 500) String interventions,
                               @NotNull PatientCondition condition,
                               @Size(max = 1000) String observations) {}
    public record HandoverRequest(@Size(max = 1000) String note) {}

    // ---------- doctor ----------
    public record IncomingPatientView(Long assignmentId, Long incidentId, String patientName, IncidentType incidentType,
                                      int priority, String ambulanceCode, TransportStatus transportStatus,
                                      Integer etaMinutes, PatientCondition lastCondition, String caseState) {}
    public record CaseView(Long id, String diagnosis, String treatment, String medication, String procedures,
                           String notes, PatientStatus patientStatus, boolean finalized, Instant finalizedAt,
                           String doctor) {}
    public record DoctorPatientDetail(Long assignmentId, IncidentView incident, String ambulanceCode,
                                      TransportStatus transportStatus, String handoverNote,
                                      MedicalCardView card, List<HistoryView> history,
                                      List<TimelineView> timeline, List<VitalView> vitals, CaseView medicalCase) {}
    public record CaseRequest(@NotBlank(message = "Diagnosis is required") @Size(max = 1000) String diagnosis,
                              @Size(max = 1000) String treatment, @Size(max = 1000) String medication,
                              @Size(max = 1000) String procedures, @Size(max = 2000) String notes,
                              @NotNull PatientStatus patientStatus) {}
    public record FinalizeRequest(String allergies, String chronicConditions, String currentMedication) {}

    // ---------- admin ----------
    public record UserView(Long id, String username, String fullName, Role role, boolean enabled,
                           Instant lastLogin, Long hospitalId, String hospitalName,
                           Long ambulanceId, String ambulanceCode) {}
    public record CreateUserRequest(@NotBlank @Size(min = 3, max = 50) String username,
                                    @NotBlank @Size(max = 120) String fullName,
                                    @NotBlank @Size(min = 8, message = "Password must have at least 8 characters") String password,
                                    @NotNull Role role, Long hospitalId, Long ambulanceId,
                                    LocalDate dateOfBirth, String bloodType, Double homeLat, Double homeLng) {}
    public record UpdateUserRequest(@NotBlank @Size(max = 120) String fullName, Long hospitalId, Long ambulanceId,
                                    @Size(min = 8, message = "Password must have at least 8 characters") String newPassword) {}
    public record EnabledRequest(boolean enabled) {}
    public record SystemStatus(long uptimeSeconds, String version, String databaseStatus, Long databaseResponseMs,
                               long activeUsers, java.util.Map<String, Long> incidentsByStatus,
                               java.util.Map<String, Long> resourcesByStatus) {}
    public record AuditView(Long id, Instant at, String username, String ip, String action, String method,
                            String path, Integer status, Long durationMs) {}
    public record PageView<T>(List<T> items, int page, int totalPages, long totalItems) {}
}
