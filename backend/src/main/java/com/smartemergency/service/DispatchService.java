package com.smartemergency.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartemergency.domain.*;
import com.smartemergency.repo.*;
import com.smartemergency.web.ApiException;
import com.smartemergency.web.Dto.*;

import java.time.Instant;
import java.util.*;

/**
 * Smart Dispatch and Priority System: incident life-cycle, resource recommendation and assignment.
 */
@Service
public class DispatchService {
    private static final Set<IncidentStatus> OPEN = EnumSet.of(
            IncidentStatus.NEW, IncidentStatus.DISPATCHED, IncidentStatus.IN_PROGRESS, IncidentStatus.AT_HOSPITAL);

    private final IncidentRepository incidents;
    private final ResourceRepository resources;
    private final AssignmentRepository assignments;
    private final TransportEventRepository events;
    private final UserRepository users;
    private final Mapper mapper;

    public DispatchService(IncidentRepository incidents, ResourceRepository resources, AssignmentRepository assignments,
                           TransportEventRepository events, UserRepository users, Mapper mapper) {
        this.incidents = incidents;
        this.resources = resources;
        this.assignments = assignments;
        this.events = events;
        this.users = users;
        this.mapper = mapper;
    }

    public Incident get(Long id) {
        return incidents.findById(id).orElseThrow(() -> ApiException.notFound("Incident"));
    }

    // ------------------------------------------------------------------ queries

    /** Open incidents (or all), ordered by priority descending. */
    @Transactional(readOnly = true)
    public List<IncidentView> list(boolean onlyOpen, IncidentType type) {
        List<Incident> list = onlyOpen ? incidents.findByStatusIn(OPEN) : incidents.findAll();
        return list.stream()
                .filter(i -> type == null || i.getType() == type)
                .map(mapper::incident)
                .sorted(Comparator.<IncidentView>comparingInt(IncidentView::priority).reversed()
                        .thenComparing(IncidentView::createdAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public Recommendation recommend(Incident incident) {
        Map<ResourceType, Integer> required = DispatchRules.requiredResources(incident.getType(), incident.getAffectedPeople());
        List<Assignment> active = assignments.findByIncidentIdAndActiveTrue(incident.getId());
        List<Need> needs = new ArrayList<>();
        for (Map.Entry<ResourceType, Integer> e : required.entrySet()) {
            ResourceType type = e.getKey();
            int already = (int) active.stream().filter(a -> a.getResource().getType() == type).count();
            int stillNeeded = Math.max(0, e.getValue() - already);
            List<Candidate> candidates = resources.findByTypeAndStatus(type, ResourceStatus.AVAILABLE).stream()
                    .map(r -> new Candidate(r.getId(), r.getCode(), r.getType(),
                            Geo.round1(Geo.distanceKm(r.getLat(), r.getLng(), incident.getLat(), incident.getLng()))))
                    .sorted(Comparator.comparingDouble(Candidate::distanceKm))
                    .limit(stillNeeded)
                    .toList();
            needs.add(new Need(type, e.getValue(), already, candidates, stillNeeded - candidates.size()));
        }
        return new Recommendation(incident.getId(), mapper.priority(incident), needs);
    }

    // ------------------------------------------------------------------ create / update

    @Transactional
    public Incident createFromSos(User patient, SosRequest req, MedicalCard card) {
        boolean hasOpen = incidents.findByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .anyMatch(i -> i.getStatus() == IncidentStatus.NEW || i.getStatus() == IncidentStatus.DISPATCHED
                        || i.getStatus() == IncidentStatus.IN_PROGRESS);
        if (hasOpen) {
            throw ApiException.conflict("You already have an open emergency");
        }
        Incident i = new Incident();
        i.setType(req.type() != null ? req.type() : IncidentType.MEDICAL);
        i.setSeverity(Severity.HIGH);
        i.setAffectedPeople(1);
        i.setDescription(req.note());
        i.setSource(IncidentSource.SOS);
        i.setPatient(patient);
        if (req.lat() != null && req.lng() != null) {
            i.setLat(req.lat());
            i.setLng(req.lng());
        } else if (card != null && card.getHomeLat() != null && card.getHomeLng() != null) {
            i.setLat(card.getHomeLat());
            i.setLng(card.getHomeLng());
            i.setLocationApprox(true);
        } else {
            throw ApiException.badRequest("Location unavailable: allow location access or ask the administrator to set your home location");
        }
        incidents.save(i);
        autoAssignNearestAmbulance(i);
        return i;
    }

    @Transactional
    public Incident createByDispatcher(IncidentRequest req) {
        Incident i = new Incident();
        apply(i, req);
        i.setSource(IncidentSource.PHONE);
        return incidents.save(i);
    }

    @Transactional
    public Incident update(Long id, IncidentRequest req) {
        Incident i = get(id);
        if (!i.getStatus().isOpen()) {
            throw ApiException.conflict("A closed or cancelled incident cannot be modified");
        }
        apply(i, req);
        return incidents.save(i);
    }

    private void apply(Incident i, IncidentRequest req) {
        i.setType(req.type());
        i.setSeverity(req.severity());
        i.setAffectedPeople(req.affectedPeople());
        i.setDescription(req.description());
        i.setLat(req.lat());
        i.setLng(req.lng());
        i.setLocationApprox(false);
        if (req.patientId() != null) {
            User p = users.findById(req.patientId()).filter(u -> u.getRole() == Role.PATIENT)
                    .orElseThrow(() -> ApiException.notFound("Patient"));
            i.setPatient(p);
        }
    }

    // ------------------------------------------------------------------ assignment

    /** Assigns the closest AVAILABLE ambulance, if any. Used for SOS requests. */
    @Transactional
    public Optional<Assignment> autoAssignNearestAmbulance(Incident incident) {
        Optional<Resource> nearest = resources.findByTypeAndStatus(ResourceType.AMBULANCE, ResourceStatus.AVAILABLE).stream()
                .min(Comparator.comparingDouble(r -> Geo.distanceKm(r.getLat(), r.getLng(), incident.getLat(), incident.getLng())));
        if (nearest.isEmpty()) return Optional.empty();
        List<Resource> locked = resources.lockByIds(List.of(nearest.get().getId()));
        if (locked.isEmpty() || locked.get(0).getStatus() != ResourceStatus.AVAILABLE) return Optional.empty();
        return Optional.of(createAssignment(incident, locked.get(0)));
    }

    @Transactional
    public Incident assign(Long incidentId, List<Long> resourceIds) {
        Incident incident = get(incidentId);
        if (!incident.getStatus().isOpen()) {
            throw ApiException.conflict("The incident is closed or cancelled");
        }
        List<Resource> locked = resources.lockByIds(new HashSet<>(resourceIds));
        if (locked.size() != new HashSet<>(resourceIds).size()) {
            throw ApiException.notFound("Resource");
        }
        for (Resource r : locked) {
            if (r.getStatus() != ResourceStatus.AVAILABLE) {
                throw ApiException.conflict("Resource " + r.getCode() + " is no longer available");
            }
        }
        locked.forEach(r -> createAssignment(incident, r));
        return incident;
    }

    private Assignment createAssignment(Incident incident, Resource r) {
        r.setStatus(ResourceStatus.BUSY);
        resources.save(r);
        Assignment a = new Assignment();
        a.setIncident(incident);
        a.setResource(r);
        if (r.getType() == ResourceType.AMBULANCE) {
            a.setTransportStatus(TransportStatus.ASSIGNED);
        }
        assignments.save(a);
        if (a.getTransportStatus() != null) {
            events.save(new TransportEvent(a.getId(), TransportStatus.ASSIGNED));
        }
        if (incident.getStatus() == IncidentStatus.NEW) {
            incident.setStatus(IncidentStatus.DISPATCHED);
        }
        if (incident.getFirstDispatchedAt() == null) {
            incident.setFirstDispatchedAt(Instant.now());
        }
        incidents.save(incident);
        return a;
    }

    /** Ends an assignment and makes the resource available again. */
    @Transactional
    public void release(Assignment a, TransportStatus finalTransportStatus) {
        a.setActive(false);
        a.setReleasedAt(Instant.now());
        if (a.getTransportStatus() != null && finalTransportStatus != null) {
            a.setTransportStatus(finalTransportStatus);
            events.save(new TransportEvent(a.getId(), finalTransportStatus));
        }
        assignments.save(a);
        Resource r = a.getResource();
        if (r.getStatus() == ResourceStatus.BUSY) {
            r.setStatus(ResourceStatus.AVAILABLE);
            resources.save(r);
        }
    }

    // ------------------------------------------------------------------ close / cancel

    @Transactional
    public Incident close(Long id, String note) {
        Incident i = get(id);
        if (!i.getStatus().isOpen()) {
            throw ApiException.conflict("The incident is already closed or cancelled");
        }
        List<Assignment> active = assignments.findByIncidentIdAndActiveTrue(id);
        if (active.stream().anyMatch(a -> a.getTransportStatus() != null && a.getTransportStatus().isActiveTransport())) {
            throw ApiException.conflict("Patient transport in progress");
        }
        active.forEach(a -> release(a, a.getTransportStatus() == TransportStatus.ASSIGNED ? TransportStatus.CANCELLED : null));
        i.setStatus(IncidentStatus.CLOSED);
        i.setClosedAt(Instant.now());
        i.setClosingNote(note);
        return incidents.save(i);
    }

    @Transactional
    public Incident cancel(Incident i, String reason) {
        for (Assignment a : assignments.findByIncidentIdAndActiveTrue(i.getId())) {
            release(a, a.getTransportStatus() != null ? TransportStatus.CANCELLED : null);
        }
        i.setStatus(IncidentStatus.CANCELLED);
        i.setCancelReason(reason);
        i.setClosedAt(Instant.now());
        return incidents.save(i);
    }

    /** The patient may cancel only before the crew is on scene. */
    public boolean cancellableByPatient(Incident i) {
        if (!(i.getStatus() == IncidentStatus.NEW || i.getStatus() == IncidentStatus.DISPATCHED)) return false;
        return assignments.findByIncidentIdAndActiveTrue(i.getId()).stream()
                .filter(a -> a.getTransportStatus() != null)
                .allMatch(a -> a.getTransportStatus() == TransportStatus.ASSIGNED
                        || a.getTransportStatus() == TransportStatus.EN_ROUTE);
    }
}
