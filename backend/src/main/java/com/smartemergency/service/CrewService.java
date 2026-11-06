package com.smartemergency.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartemergency.domain.*;
import com.smartemergency.repo.*;
import com.smartemergency.web.ApiException;
import com.smartemergency.web.Dto.*;

import java.util.Comparator;
import java.util.List;

/** Use cases of the ambulance crew. */
@Service
public class CrewService {
    private final ResourceRepository resources;
    private final AssignmentRepository assignments;
    private final TransportEventRepository events;
    private final VitalSignRepository vitals;
    private final HospitalRepository hospitals;
    private final IncidentRepository incidents;
    private final MedicalCardRepository cards;
    private final DispatchService dispatch;
    private final Mapper mapper;

    public CrewService(ResourceRepository resources, AssignmentRepository assignments, TransportEventRepository events,
                       VitalSignRepository vitals, HospitalRepository hospitals, IncidentRepository incidents,
                       MedicalCardRepository cards, DispatchService dispatch, Mapper mapper) {
        this.resources = resources;
        this.assignments = assignments;
        this.events = events;
        this.vitals = vitals;
        this.hospitals = hospitals;
        this.incidents = incidents;
        this.cards = cards;
        this.dispatch = dispatch;
        this.mapper = mapper;
    }

    private Resource ambulanceOf(User crew) {
        return resources.findByCrewUserId(crew.getId())
                .orElseThrow(() -> ApiException.conflict("Your account is not linked to an ambulance"));
    }

    private Assignment activeAssignment(Resource amb) {
        return assignments.findFirstByResourceIdAndActiveTrue(amb.getId())
                .orElseThrow(() -> ApiException.conflict("No active emergency"));
    }

    @Transactional(readOnly = true)
    public CrewHome home(User crew) {
        Resource amb = ambulanceOf(crew);
        CrewAssignmentView view = assignments.findFirstByResourceIdAndActiveTrue(amb.getId())
                .map(this::view).orElse(null);
        return new CrewHome(mapper.resource(amb), view);
    }

    private CrewAssignmentView view(Assignment a) {
        Incident i = a.getIncident();
        Resource amb = a.getResource();
        EmergencyPatientData patient = null;
        if (i.getPatient() != null) {
            patient = mapper.emergencyData(i.getPatient(), cards.findByPatientId(i.getPatient().getId()).orElse(null));
        }
        Long nearestHospital = hospitals.findAll().stream()
                .min(Comparator.comparingDouble(h -> Geo.distanceKm(h.getLat(), h.getLng(), i.getLat(), i.getLng())))
                .map(Hospital::getId).orElse(null);
        TransportStatus next = a.getTransportStatus() == null ? null : a.getTransportStatus().next();
        if (next == TransportStatus.HANDED_OVER) next = null; // only through hand over
        return new CrewAssignmentView(a.getId(), mapper.resource(amb), mapper.incident(i), patient,
                a.getTransportStatus(), next, mapper.hospital(a.getDestinationHospital()), nearestHospital,
                Geo.round1(Geo.distanceKm(amb.getLat(), amb.getLng(), i.getLat(), i.getLng())),
                a.getTransportStatus() == TransportStatus.HANDED_OVER,
                events.findByAssignmentIdOrderByAt(a.getId()).stream().map(mapper::timeline).toList(),
                vitals.findByAssignmentIdOrderByRecordedAt(a.getId()).stream().map(mapper::vital).toList());
    }

    @Transactional
    public CrewHome updateStatus(User crew, StatusRequest req) {
        Resource amb = ambulanceOf(crew);
        Assignment a = activeAssignment(amb);
        TransportStatus current = a.getTransportStatus();
        TransportStatus target = req.status();
        if (target == TransportStatus.HANDED_OVER || current == null || current.next() != target) {
            throw ApiException.conflict("Invalid status transition: " + current + " -> " + target);
        }
        Incident i = a.getIncident();
        switch (target) {
            case ON_SCENE -> {
                amb.setLat(i.getLat());
                amb.setLng(i.getLng());
                if (i.getStatus() == IncidentStatus.DISPATCHED) i.setStatus(IncidentStatus.IN_PROGRESS);
            }
            case TRANSPORTING -> {
                if (req.hospitalId() == null) throw ApiException.badRequest("Select the destination hospital");
                a.setDestinationHospital(hospitals.findById(req.hospitalId())
                        .orElseThrow(() -> ApiException.notFound("Hospital")));
            }
            case AT_HOSPITAL -> {
                Hospital h = a.getDestinationHospital();
                amb.setLat(h.getLat());
                amb.setLng(h.getLng());
            }
            default -> { }
        }
        a.setTransportStatus(target);
        assignments.save(a);
        resources.save(amb);
        incidents.save(i);
        events.save(new TransportEvent(a.getId(), target));
        return home(crew);
    }

    /** Patient treated on scene, no transport needed: assignment ends, ambulance is available. */
    @Transactional
    public CrewHome noTransport(User crew) {
        Resource amb = ambulanceOf(crew);
        Assignment a = activeAssignment(amb);
        if (a.getTransportStatus() != TransportStatus.ON_SCENE) {
            throw ApiException.conflict("Available only when the crew is on scene");
        }
        dispatch.release(a, TransportStatus.NO_TRANSPORT);
        return home(crew);
    }

    @Transactional
    public CrewHome addVitals(User crew, VitalRequest r) {
        Assignment a = activeAssignment(ambulanceOf(crew));
        if (a.getTransportStatus() == null || !a.getTransportStatus().allowsVitalSigns()) {
            throw ApiException.conflict("Vital signs can be recorded only from the moment the crew is on scene");
        }
        VitalSign v = new VitalSign();
        v.setAssignmentId(a.getId());
        v.setPulse(r.pulse());
        v.setSpo2(r.spo2());
        v.setSystolic(r.systolic());
        v.setDiastolic(r.diastolic());
        v.setTemperature(r.temperature());
        v.setOxygenLpm(r.oxygenLpm());
        v.setIv(r.iv());
        v.setMedication(r.medication());
        v.setInterventions(r.interventions());
        v.setCondition(r.condition());
        v.setObservations(r.observations());
        vitals.save(v);
        return home(crew);
    }

    @Transactional
    public CrewHome handOver(User crew, String note) {
        Resource amb = ambulanceOf(crew);
        Assignment a = activeAssignment(amb);
        if (a.getTransportStatus() != TransportStatus.AT_HOSPITAL) {
            throw ApiException.conflict("Hand over is possible only at the hospital");
        }
        if (vitals.countByAssignmentId(a.getId()) == 0) {
            throw ApiException.conflict("Record at least one set of vital signs");
        }
        a.setHandoverNote(note);
        dispatch.release(a, TransportStatus.HANDED_OVER);
        Incident i = a.getIncident();
        i.setStatus(IncidentStatus.AT_HOSPITAL);
        incidents.save(i);
        return home(crew);
    }

    public List<HospitalView> hospitals() {
        return hospitals.findAll().stream().map(mapper::hospital).toList();
    }
}
