package com.smartemergency.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartemergency.domain.*;
import com.smartemergency.repo.*;
import com.smartemergency.web.ApiException;
import com.smartemergency.web.Dto.*;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Use cases of the administrator. */
@Service
public class AdminService {
    public static final String VERSION = "1.1.0";

    private final UserRepository users;
    private final ResourceRepository resources;
    private final HospitalRepository hospitals;
    private final MedicalCardRepository cards;
    private final IncidentRepository incidents;
    private final AuditLogRepository audit;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    private final Mapper mapper;

    public AdminService(UserRepository users, ResourceRepository resources, HospitalRepository hospitals,
                        MedicalCardRepository cards, IncidentRepository incidents, AuditLogRepository audit,
                        PasswordEncoder encoder, JdbcTemplate jdbc, Mapper mapper) {
        this.users = users;
        this.resources = resources;
        this.hospitals = hospitals;
        this.cards = cards;
        this.incidents = incidents;
        this.audit = audit;
        this.encoder = encoder;
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ users

    @Transactional(readOnly = true)
    public List<UserView> listUsers() {
        return users.findAll(Sort.by("role", "username")).stream()
                .map(u -> mapper.user(u, u.getRole() == Role.CREW ? resources.findByCrewUserId(u.getId()).orElse(null) : null))
                .toList();
    }

    @Transactional
    public UserView create(CreateUserRequest r) {
        if (users.existsByUsername(r.username().trim())) {
            throw ApiException.conflict("The username already exists");
        }
        User u = new User();
        u.setUsername(r.username().trim());
        u.setFullName(r.fullName().trim());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(r.role());
        if (r.role() == Role.DOCTOR) {
            if (r.hospitalId() == null) throw ApiException.badRequest("Select the hospital of the doctor");
            u.setHospital(hospitals.findById(r.hospitalId()).orElseThrow(() -> ApiException.notFound("Hospital")));
        }
        users.save(u);
        if (r.role() == Role.CREW) {
            if (r.ambulanceId() == null) throw ApiException.badRequest("Select the ambulance of the crew");
            linkAmbulance(u, r.ambulanceId());
        }
        if (r.role() == Role.PATIENT) {
            MedicalCard c = new MedicalCard();
            c.setPatient(u);
            c.setDateOfBirth(r.dateOfBirth());
            c.setBloodType(r.bloodType());
            c.setHomeLat(r.homeLat());
            c.setHomeLng(r.homeLng());
            cards.save(c);
        }
        return mapper.user(u, r.role() == Role.CREW ? resources.findByCrewUserId(u.getId()).orElse(null) : null);
    }

    @Transactional
    public UserView update(Long id, UpdateUserRequest r) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setFullName(r.fullName().trim());
        if (r.newPassword() != null && !r.newPassword().isBlank()) {
            u.setPasswordHash(encoder.encode(r.newPassword()));
        }
        if (u.getRole() == Role.DOCTOR && r.hospitalId() != null) {
            u.setHospital(hospitals.findById(r.hospitalId()).orElseThrow(() -> ApiException.notFound("Hospital")));
        }
        users.save(u);
        if (u.getRole() == Role.CREW && r.ambulanceId() != null) {
            linkAmbulance(u, r.ambulanceId());
        }
        return mapper.user(u, u.getRole() == Role.CREW ? resources.findByCrewUserId(u.getId()).orElse(null) : null);
    }

    private void linkAmbulance(User crew, Long ambulanceId) {
        Resource amb = resources.findById(ambulanceId).filter(r -> r.getType() == ResourceType.AMBULANCE)
                .orElseThrow(() -> ApiException.notFound("Ambulance"));
        if (amb.getCrewUser() != null && !amb.getCrewUser().getId().equals(crew.getId())) {
            throw ApiException.conflict("Ambulance " + amb.getCode() + " already has a crew account");
        }
        resources.findByCrewUserId(crew.getId()).ifPresent(old -> {
            if (!old.getId().equals(amb.getId())) {
                old.setCrewUser(null);
                resources.saveAndFlush(old);
            }
        });
        amb.setCrewUser(crew);
        resources.save(amb);
    }

    @Transactional
    public UserView setEnabled(User admin, Long id, boolean enabled) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        if (u.getId().equals(admin.getId()) && !enabled) {
            throw ApiException.conflict("You cannot disable your own account");
        }
        u.setEnabled(enabled);
        users.save(u);
        return mapper.user(u, u.getRole() == Role.CREW ? resources.findByCrewUserId(u.getId()).orElse(null) : null);
    }

    // ------------------------------------------------------------------ status

    public SystemStatus status() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        String dbStatus;
        Long dbMs;
        Map<String, Long> inc = new LinkedHashMap<>();
        Map<String, Long> res = new LinkedHashMap<>();
        long active = 0;
        try {
            long t0 = System.nanoTime();
            jdbc.queryForObject("SELECT 1", Integer.class);
            dbMs = (System.nanoTime() - t0) / 1_000_000;
            dbStatus = "OK";
            for (IncidentStatus s : IncidentStatus.values()) inc.put(s.name(), incidents.countByStatus(s));
            for (ResourceStatus s : ResourceStatus.values()) res.put(s.name(), resources.countByStatus(s));
            active = audit.countActiveUsersSince(Instant.now().minus(5, ChronoUnit.MINUTES));
        } catch (Exception e) {
            dbStatus = "ERROR";
            dbMs = null;
        }
        return new SystemStatus(uptime, VERSION, dbStatus, dbMs, active, inc, res);
    }

    // ------------------------------------------------------------------ audit log

    private Specification<AuditLog> filter(String username, String action, Instant from, Instant to) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (username != null && !username.isBlank())
                p.add(cb.like(cb.lower(root.<String>get("username")), "%" + username.toLowerCase() + "%"));
            if (action != null && !action.isBlank()) p.add(cb.equal(root.<String>get("action"), action));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.<Instant>get("at"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.<Instant>get("at"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static AuditView view(AuditLog a) {
        return new AuditView(a.getId(), a.getAt(), a.getUsername(), a.getIp(), a.getAction(), a.getMethod(),
                a.getPath(), a.getStatus(), a.getDurationMs());
    }

    @Transactional(readOnly = true)
    public PageView<AuditView> audit(String username, String action, Instant from, Instant to, int page) {
        Page<AuditLog> p = audit.findAll(filter(username, action, from, to),
                PageRequest.of(Math.max(0, page), 50, Sort.by(Sort.Direction.DESC, "at")));
        return new PageView<>(p.getContent().stream().map(AdminService::view).toList(), p.getNumber(),
                p.getTotalPages(), p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public String exportCsv(String username, String action, Instant from, Instant to) {
        StringBuilder sb = new StringBuilder("id,time,username,ip,action,method,path,status,duration_ms\n");
        for (AuditLog a : audit.findAll(filter(username, action, from, to), Sort.by(Sort.Direction.DESC, "at"))) {
            sb.append(a.getId()).append(',')
              .append(a.getAt()).append(',')
              .append(csv(a.getUsername())).append(',')
              .append(csv(a.getIp())).append(',')
              .append(csv(a.getAction())).append(',')
              .append(csv(a.getMethod())).append(',')
              .append(csv(a.getPath())).append(',')
              .append(a.getStatus() == null ? "" : a.getStatus()).append(',')
              .append(a.getDurationMs() == null ? "" : a.getDurationMs()).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    public List<ResourceView> ambulances() {
        return resources.findAllByOrderByCode().stream().filter(r -> r.getType() == ResourceType.AMBULANCE)
                .map(mapper::resource).toList();
    }
}
