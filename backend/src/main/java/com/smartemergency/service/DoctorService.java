package com.smartemergency.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartemergency.domain.*;
import com.smartemergency.repo.*;
import com.smartemergency.web.ApiException;
import com.smartemergency.web.Dto.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/** Use cases of the hospital doctor. */
@Service
public class DoctorService {
    private static final double AVERAGE_SPEED_KMH = 50.0;

    private final AssignmentRepository assignments;
    private final MedicalCaseRepository cases;
    private final MedicalCardRepository cards;
    private final MedicalHistoryRepository history;
    private final VitalSignRepository vitals;
    private final TransportEventRepository events;
    private final Mapper mapper;

    public DoctorService(AssignmentRepository assignments, MedicalCaseRepository cases, MedicalCardRepository cards,
                         MedicalHistoryRepository history, VitalSignRepository vitals,
                         TransportEventRepository events, Mapper mapper) {
        this.assignments = assignments;
        this.cases = cases;
        this.cards = cards;
        this.history = history;
        this.vitals = vitals;
        this.events = events;
        this.mapper = mapper;
    }

    private Hospital hospitalOf(User doctor) {
        if (doctor.getHospital() == null) throw ApiException.conflict("Your account is not linked to a hospital");
        return doctor.getHospital();
    }

    @Transactional(readOnly = true)
    public List<IncomingPatientView> incoming(User doctor) {
        Hospital h = hospitalOf(doctor);
        return assignments.findForHospital(h.getId(), EnumSet.of(TransportStatus.TRANSPORTING,
                        TransportStatus.AT_HOSPITAL, TransportStatus.HANDED_OVER)).stream()
                .filter(a -> {
                    Optional<MedicalCase> c = cases.findByAssignmentId(a.getId());
                    return c.isEmpty() || !c.get().isFinalized();
                })
                .map(a -> {
                    Incident i = a.getIncident();
                    Integer eta = null;
                    if (a.getTransportStatus() == TransportStatus.TRANSPORTING) {
                        double km = Geo.distanceKm(a.getResource().getLat(), a.getResource().getLng(), h.getLat(), h.getLng());
                        eta = (int) Math.ceil(km / AVERAGE_SPEED_KMH * 60);
                    }
                    PatientCondition last = vitals.findFirstByAssignmentIdOrderByRecordedAtDesc(a.getId())
                            .map(VitalSign::getCondition).orElse(null);
                    String caseState = cases.findByAssignmentId(a.getId()).map(c -> "DRAFT").orElse("NONE");
                    return new IncomingPatientView(a.getId(), i.getId(),
                            i.getPatient() == null ? "Unknown patient" : i.getPatient().getFullName(),
                            i.getType(), mapper.priority(i), a.getResource().getCode(), a.getTransportStatus(),
                            eta, last, caseState);
                }).toList();
    }

    private Assignment ownAssignment(User doctor, Long assignmentId) {
        Hospital h = hospitalOf(doctor);
        Assignment a = assignments.findById(assignmentId).orElseThrow(() -> ApiException.notFound("Patient"));
        if (a.getDestinationHospital() == null || !a.getDestinationHospital().getId().equals(h.getId())) {
            throw ApiException.forbidden("This patient is not transported to your hospital");
        }
        return a;
    }

    @Transactional(readOnly = true)
    public DoctorPatientDetail detail(User doctor, Long assignmentId) {
        Assignment a = ownAssignment(doctor, assignmentId);
        Incident i = a.getIncident();
        MedicalCardView card = null;
        List<HistoryView> hist = List.of();
        if (i.getPatient() != null) {
            card = mapper.card(i.getPatient(), cards.findByPatientId(i.getPatient().getId()).orElse(null));
            hist = history.findByPatientIdOrderByEventDateDesc(i.getPatient().getId()).stream().map(mapper::history).toList();
        }
        return new DoctorPatientDetail(a.getId(), mapper.incident(i), a.getResource().getCode(), a.getTransportStatus(),
                a.getHandoverNote(), card, hist,
                events.findByAssignmentIdOrderByAt(a.getId()).stream().map(mapper::timeline).toList(),
                vitals.findByAssignmentIdOrderByRecordedAt(a.getId()).stream().map(mapper::vital).toList(),
                mapper.medicalCase(cases.findByAssignmentId(a.getId()).orElse(null)));
    }

    @Transactional
    public DoctorPatientDetail saveCase(User doctor, Long assignmentId, CaseRequest r) {
        Assignment a = ownAssignment(doctor, assignmentId);
        if (a.getTransportStatus() != TransportStatus.HANDED_OVER) {
            throw ApiException.conflict("The patient has not been handed over yet");
        }
        MedicalCase c = cases.findByAssignmentId(a.getId()).orElseGet(() -> {
            MedicalCase n = new MedicalCase();
            n.setAssignmentId(a.getId());
            n.setIncident(a.getIncident());
            n.setPatient(a.getIncident().getPatient());
            n.setHospital(a.getDestinationHospital());
            return n;
        });
        if (c.isFinalized()) throw ApiException.conflict("The medical case is finalized");
        c.setDoctor(doctor);
        c.setDiagnosis(r.diagnosis());
        c.setTreatment(r.treatment());
        c.setMedication(r.medication());
        c.setProcedures(r.procedures());
        c.setNotes(r.notes());
        c.setPatientStatus(r.patientStatus());
        c.setUpdatedAt(Instant.now());
        cases.save(c);
        return detail(doctor, assignmentId);
    }

    @Transactional
    public DoctorPatientDetail finalizeCase(User doctor, Long assignmentId, FinalizeRequest r) {
        Assignment a = ownAssignment(doctor, assignmentId);
        MedicalCase c = cases.findByAssignmentId(a.getId())
                .orElseThrow(() -> ApiException.conflict("Record the diagnosis before finalizing"));
        if (c.isFinalized()) throw ApiException.conflict("The medical case is already finalized");
        c.setFinalized(true);
        c.setFinalizedAt(Instant.now());
        c.setDoctor(doctor);
        cases.save(c);

        User patient = a.getIncident().getPatient();
        if (patient != null) {
            MedicalHistoryEntry e = new MedicalHistoryEntry();
            e.setPatientId(patient.getId());
            e.setHospital(c.getHospital());
            e.setDoctor(doctor);
            e.setIncidentId(a.getIncident().getId());
            e.setDiagnosis(c.getDiagnosis());
            e.setTreatment(c.getTreatment());
            history.save(e);
            if (r != null) {
                cards.findByPatientId(patient.getId()).ifPresent(card -> {
                    if (r.allergies() != null) card.setAllergies(r.allergies());
                    if (r.chronicConditions() != null) card.setChronicConditions(r.chronicConditions());
                    if (r.currentMedication() != null) card.setCurrentMedication(r.currentMedication());
                    cards.save(card);
                });
            }
        }
        return detail(doctor, assignmentId);
    }
}
