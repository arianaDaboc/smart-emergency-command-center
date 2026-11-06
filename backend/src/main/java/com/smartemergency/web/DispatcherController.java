package com.smartemergency.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.smartemergency.domain.Incident;
import com.smartemergency.domain.IncidentType;
import com.smartemergency.domain.Role;
import com.smartemergency.repo.UserRepository;
import com.smartemergency.service.DispatchService;
import com.smartemergency.service.Mapper;
import com.smartemergency.service.ResourceService;
import com.smartemergency.web.Dto.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispatcher")
public class DispatcherController {
    private final DispatchService dispatch;
    private final ResourceService resources;
    private final UserRepository users;
    private final Mapper mapper;

    public DispatcherController(DispatchService dispatch, ResourceService resources, UserRepository users, Mapper mapper) {
        this.dispatch = dispatch;
        this.resources = resources;
        this.users = users;
        this.mapper = mapper;
    }

    /** Monitor Incidents: open incidents (default) or all, ordered by priority. */
    @GetMapping("/incidents")
    public List<IncidentView> incidents(@RequestParam(defaultValue = "open") String scope,
                                        @RequestParam(required = false) IncidentType type) {
        return dispatch.list(!"all".equals(scope), type);
    }

    @GetMapping("/incidents/{id}")
    @Transactional(readOnly = true)
    public IncidentDetail incident(@PathVariable Long id) {
        Incident i = dispatch.get(id);
        return new IncidentDetail(mapper.incident(i), i.getStatus().isOpen() ? dispatch.recommend(i) : null);
    }

    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentDetail create(@Valid @RequestBody IncidentRequest req) {
        return incident(dispatch.createByDispatcher(req).getId());
    }

    @PutMapping("/incidents/{id}")
    public IncidentDetail update(@PathVariable Long id, @Valid @RequestBody IncidentRequest req) {
        dispatch.update(id, req);
        return incident(id);
    }

    @PostMapping("/incidents/{id}/assign")
    public IncidentDetail assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        dispatch.assign(id, req.resourceIds());
        return incident(id);
    }

    @PostMapping("/incidents/{id}/close")
    public IncidentDetail close(@PathVariable Long id, @Valid @RequestBody CloseRequest req) {
        dispatch.close(id, req.note());
        return incident(id);
    }

    @GetMapping("/resources")
    public List<ResourceView> resources() { return resources.list(); }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceView addResource(@Valid @RequestBody ResourceRequest req) { return resources.create(req); }

    @PatchMapping("/resources/{id}/status")
    public ResourceView setStatus(@PathVariable Long id, @Valid @RequestBody ResourceStatusRequest req) {
        return resources.setStatus(id, req.status());
    }

    @GetMapping("/patients")
    public List<PatientSearchView> patients(@RequestParam(defaultValue = "") String q) {
        return users.searchPatients(q.trim()).stream().limit(20)
                .map(u -> new PatientSearchView(u.getId(), u.getFullName(), u.getUsername())).toList();
    }
}
