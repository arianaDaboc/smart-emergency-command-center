package com.smartemergency.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.smartemergency.service.CrewService;
import com.smartemergency.service.CurrentUser;
import com.smartemergency.web.Dto.*;

@RestController
@RequestMapping("/api/crew")
public class CrewController {
    private final CrewService service;
    private final CurrentUser current;

    public CrewController(CrewService service, CurrentUser current) {
        this.service = service;
        this.current = current;
    }

    /** View Assigned Emergency (assignment is null when there is none). */
    @GetMapping("/assignment")
    public CrewHome home() { return service.home(current.get()); }

    @PostMapping("/assignment/status")
    public CrewHome status(@Valid @RequestBody StatusRequest req) { return service.updateStatus(current.get(), req); }

    @PostMapping("/assignment/no-transport")
    public CrewHome noTransport() { return service.noTransport(current.get()); }

    @PostMapping("/assignment/vitals")
    public CrewHome vitals(@Valid @RequestBody VitalRequest req) { return service.addVitals(current.get(), req); }

    @PostMapping("/assignment/handover")
    public CrewHome handover(@Valid @RequestBody HandoverRequest req) {
        return service.handOver(current.get(), req.note());
    }
}
