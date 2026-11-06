package com.smartemergency.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartemergency.domain.*;
import com.smartemergency.repo.*;
import com.smartemergency.web.ApiException;
import com.smartemergency.web.Dto.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PatientService {
    public static final int MAX_CONTACTS = 5;

    private final DispatchService dispatch;
    private final IncidentRepository incidents;
    private final AssignmentRepository assignments;
    private final MedicalCardRepository cards;
    private final MedicalHistoryRepository history;
    private final EmergencyContactRepository contacts;
    private final Mapper mapper;

    public PatientService(DispatchService dispatch, IncidentRepository incidents, AssignmentRepository assignments,
                          MedicalCardRepository cards, MedicalHistoryRepository history,
                          EmergencyContactRepository contacts, Mapper mapper) {
        this.dispatch = dispatch;
        this.incidents = incidents;
        this.assignments = assignments;
        this.cards = cards;
        this.history = history;
        this.contacts = contacts;
        this.mapper = mapper;
    }

    @Transactional
    public PatientEmergencyView sos(User patient, SosRequest req) {
        Incident i = dispatch.createFromSos(patient, req, cards.findByPatientId(patient.getId()).orElse(null));
        return emergencyView(i);
    }

    /** Current open emergency of the patient, if any. */
    @Transactional(readOnly = true)
    public Optional<PatientEmergencyView> current(User patient) {
        return incidents.findByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .filter(i -> i.getStatus().isOpen())
                .findFirst()
                .map(this::emergencyView);
    }

    @Transactional(readOnly = true)
    public List<IncidentView> past(User patient) {
        return incidents.findByPatientIdOrderByCreatedAtDesc(patient.getId()).stream().map(mapper::incident).toList();
    }

    @Transactional
    public PatientEmergencyView cancel(User patient, Long incidentId, String reason) {
        Incident i = dispatch.get(incidentId);
        if (i.getPatient() == null || !i.getPatient().getId().equals(patient.getId())) {
            throw ApiException.notFound("Emergency");
        }
        if (!dispatch.cancellableByPatient(i)) {
            throw ApiException.conflict("The emergency can no longer be cancelled");
        }
        return emergencyView(dispatch.cancel(i, reason));
    }

    private PatientEmergencyView emergencyView(Incident i) {
        Optional<Assignment> amb = assignments.findByIncidentIdOrderById(i.getId()).stream()
                .filter(a -> a.getTransportStatus() != null)
                .max(Comparator.comparing(Assignment::getAssignedAt));
        Double dist = amb.map(a -> Geo.round1(Geo.distanceKm(a.getResource().getLat(), a.getResource().getLng(),
                i.getLat(), i.getLng()))).orElse(null);
        return new PatientEmergencyView(mapper.incident(i),
                amb.map(a -> a.getResource().getCode()).orElse(null),
                amb.map(Assignment::getTransportStatus).orElse(null),
                dist,
                amb.map(a -> a.getDestinationHospital() == null ? null : a.getDestinationHospital().getName()).orElse(null),
                dispatch.cancellableByPatient(i));
    }

    @Transactional(readOnly = true)
    public MedicalCardAndHistory medicalCard(User patient) {
        MedicalCard c = cards.findByPatientId(patient.getId()).orElse(null);
        return new MedicalCardAndHistory(mapper.card(patient, c),
                history.findByPatientIdOrderByEventDateDesc(patient.getId()).stream().map(mapper::history).toList());
    }

    // ---------- emergency contacts ----------

    public List<ContactView> contacts(User patient) {
        return contacts.findByPatientIdOrderById(patient.getId()).stream().map(mapper::contact).toList();
    }

    @Transactional
    public ContactView addContact(User patient, ContactRequest req) {
        if (contacts.countByPatientId(patient.getId()) >= MAX_CONTACTS) {
            throw ApiException.conflict("At most " + MAX_CONTACTS + " emergency contacts are allowed");
        }
        EmergencyContact c = new EmergencyContact();
        c.setPatientId(patient.getId());
        fill(c, req);
        return mapper.contact(contacts.save(c));
    }

    @Transactional
    public ContactView updateContact(User patient, Long id, ContactRequest req) {
        EmergencyContact c = own(patient, id);
        fill(c, req);
        return mapper.contact(contacts.save(c));
    }

    @Transactional
    public void deleteContact(User patient, Long id) {
        contacts.delete(own(patient, id));
    }

    private EmergencyContact own(User patient, Long id) {
        return contacts.findById(id).filter(c -> c.getPatientId().equals(patient.getId()))
                .orElseThrow(() -> ApiException.notFound("Contact"));
    }

    private static void fill(EmergencyContact c, ContactRequest r) {
        c.setName(r.name().trim());
        c.setRelationship(r.relationship());
        c.setPhone(r.phone().trim());
    }
}
