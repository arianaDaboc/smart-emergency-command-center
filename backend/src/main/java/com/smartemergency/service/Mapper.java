package com.smartemergency.service;

import org.springframework.stereotype.Component;
import com.smartemergency.domain.*;
import com.smartemergency.repo.*;
import com.smartemergency.web.Dto.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/** Converts entities to the views returned by the REST API. */
@Component
public class Mapper {
    private final AssignmentRepository assignments;
    private final EmergencyContactRepository contacts;
    private final MedicalCaseRepository cases;

    public Mapper(AssignmentRepository assignments, EmergencyContactRepository contacts, MedicalCaseRepository cases) {
        this.assignments = assignments;
        this.contacts = contacts;
        this.cases = cases;
    }

    public int priority(Incident i) {
        return PriorityCalculator.score(i.getType(), i.getSeverity(), i.getAffectedPeople(),
                i.getCreatedAt(), i.getFirstDispatchedAt(), Instant.now());
    }

    public HospitalView hospital(Hospital h) {
        return h == null ? null : new HospitalView(h.getId(), h.getName(), h.getLat(), h.getLng());
    }

    public ResourceView resource(Resource r) {
        Long incidentId = assignments.findFirstByResourceIdAndActiveTrue(r.getId())
                .map(a -> a.getIncident().getId()).orElse(null);
        return new ResourceView(r.getId(), r.getCode(), r.getType(), r.getStatus(), r.getLat(), r.getLng(),
                r.getCrewUser() == null ? null : r.getCrewUser().getFullName(), incidentId);
    }

    public AssignmentView assignment(Assignment a) {
        Incident i = a.getIncident();
        Resource r = a.getResource();
        double d = Geo.round1(Geo.distanceKm(r.getLat(), r.getLng(), i.getLat(), i.getLng()));
        Hospital h = a.getDestinationHospital();
        return new AssignmentView(a.getId(), r.getId(), r.getCode(), r.getType(), a.isActive(), a.getTransportStatus(),
                h == null ? null : h.getId(), h == null ? null : h.getName(), a.getAssignedAt(), d);
    }

    public IncidentView incident(Incident i) {
        List<Assignment> all = assignments.findByIncidentIdOrderById(i.getId());
        List<AssignmentView> views = all.stream().filter(Assignment::isActive).map(this::assignment).toList();
        // also show ambulances that already handed the patient over
        List<AssignmentView> finished = all.stream()
                .filter(a -> !a.isActive() && a.getTransportStatus() == TransportStatus.HANDED_OVER)
                .map(this::assignment).toList();
        List<AssignmentView> res = new ArrayList<>(views);
        res.addAll(finished);
        boolean noResources = i.getStatus().isOpen() && views.isEmpty() && i.getFirstDispatchedAt() == null;
        boolean anyActiveTransport = all.stream().anyMatch(a -> a.isActive()
                && a.getTransportStatus() != null && a.getTransportStatus().isActiveTransport());
        boolean readyToClose = i.getStatus().isOpen() && i.getStatus() != IncidentStatus.NEW && !anyActiveTransport
                && (i.getStatus() != IncidentStatus.AT_HOSPITAL
                    || cases.findByIncidentId(i.getId()).stream().allMatch(MedicalCase::isFinalized)
                       && !cases.findByIncidentId(i.getId()).isEmpty());
        Instant end = i.getFirstDispatchedAt() != null ? i.getFirstDispatchedAt()
                : (i.getStatus().isOpen() ? Instant.now() : (i.getClosedAt() != null ? i.getClosedAt() : Instant.now()));
        long waiting = Math.max(0, Duration.between(i.getCreatedAt(), end).toMinutes());
        return new IncidentView(i.getId(), i.getType(), i.getSeverity(), i.getAffectedPeople(), i.getDescription(),
                i.getLat(), i.getLng(), i.isLocationApprox(), i.getSource(), i.getStatus(), priority(i),
                i.getPatient() == null ? null : i.getPatient().getId(),
                i.getPatient() == null ? null : i.getPatient().getFullName(),
                i.getCreatedAt(), i.getFirstDispatchedAt(), i.getClosedAt(), i.getClosingNote(), i.getCancelReason(),
                waiting, res, noResources, readyToClose);
    }

    public ContactView contact(EmergencyContact c) {
        return new ContactView(c.getId(), c.getName(), c.getRelationship(), c.getPhone());
    }

    public static Integer age(LocalDate dob) {
        return dob == null ? null : Period.between(dob, LocalDate.now()).getYears();
    }

    public MedicalCardView card(User patient, MedicalCard c) {
        List<ContactView> cs = contacts.findByPatientIdOrderById(patient.getId()).stream().map(this::contact).toList();
        if (c == null) {
            return new MedicalCardView(patient.getId(), patient.getFullName(), null, null, null, null, null, null, null, cs);
        }
        return new MedicalCardView(patient.getId(), patient.getFullName(), c.getDateOfBirth(), age(c.getDateOfBirth()),
                c.getBloodType(), c.getAllergies(), c.getChronicConditions(), c.getCurrentMedication(),
                c.getMedicalNotes(), cs);
    }

    /** Medical Card as seen by the ambulance crew: all card fields, but no medical history. */
    public EmergencyPatientData emergencyData(User patient, MedicalCard c) {
        List<ContactView> cs = contacts.findByPatientIdOrderById(patient.getId()).stream().map(this::contact).toList();
        return new EmergencyPatientData(patient.getFullName(),
                c == null ? null : age(c.getDateOfBirth()),
                c == null ? null : c.getBloodType(),
                c == null ? null : c.getAllergies(),
                c == null ? null : c.getChronicConditions(),
                c == null ? null : c.getCurrentMedication(),
                c == null ? null : c.getMedicalNotes(), cs);
    }

    public HistoryView history(MedicalHistoryEntry h) {
        return new HistoryView(h.getId(), h.getEventDate(),
                h.getHospital() == null ? null : h.getHospital().getName(),
                h.getDoctor() == null ? null : h.getDoctor().getFullName(),
                h.getIncidentId(), h.getDiagnosis(), h.getTreatment());
    }

    public TimelineView timeline(TransportEvent e) {
        return new TimelineView(e.getStatus(), e.getAt());
    }

    /** Values outside the normal range are reported so that the client can highlight them. */
    public static List<String> abnormal(VitalSign v) {
        List<String> out = new ArrayList<>();
        if (v.getPulse() != null && (v.getPulse() < 50 || v.getPulse() > 120)) out.add("pulse");
        if (v.getSpo2() != null && v.getSpo2() < 92) out.add("spo2");
        if (v.getSystolic() != null && (v.getSystolic() < 90 || v.getSystolic() > 180)) out.add("systolic");
        if (v.getTemperature() != null && v.getTemperature().doubleValue() > 38.5) out.add("temperature");
        return out;
    }

    public VitalView vital(VitalSign v) {
        return new VitalView(v.getId(), v.getRecordedAt(), v.getPulse(), v.getSpo2(), v.getSystolic(), v.getDiastolic(),
                v.getTemperature(), v.getOxygenLpm(), v.isIv(), v.getMedication(), v.getInterventions(),
                v.getCondition(), v.getObservations(), abnormal(v));
    }

    public CaseView medicalCase(MedicalCase c) {
        if (c == null) return null;
        return new CaseView(c.getId(), c.getDiagnosis(), c.getTreatment(), c.getMedication(), c.getProcedures(),
                c.getNotes(), c.getPatientStatus(), c.isFinalized(), c.getFinalizedAt(),
                c.getDoctor() == null ? null : c.getDoctor().getFullName());
    }

    public UserView user(User u, Resource ambulance) {
        return new UserView(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isEnabled(), u.getLastLogin(),
                u.getHospital() == null ? null : u.getHospital().getId(),
                u.getHospital() == null ? null : u.getHospital().getName(),
                ambulance == null ? null : ambulance.getId(), ambulance == null ? null : ambulance.getCode());
    }
}
