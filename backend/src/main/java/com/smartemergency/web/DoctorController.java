package com.smartemergency.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.smartemergency.service.CurrentUser;
import com.smartemergency.service.DoctorService;
import com.smartemergency.web.Dto.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {
    private final DoctorService service;
    private final CurrentUser current;

    public DoctorController(DoctorService service, CurrentUser current) {
        this.service = service;
        this.current = current;
    }

    @GetMapping("/incoming")
    public List<IncomingPatientView> incoming() { return service.incoming(current.get()); }

    @GetMapping("/patients/{assignmentId}")
    public DoctorPatientDetail detail(@PathVariable Long assignmentId) {
        return service.detail(current.get(), assignmentId);
    }

    @PutMapping("/patients/{assignmentId}/case")
    public DoctorPatientDetail saveCase(@PathVariable Long assignmentId, @Valid @RequestBody CaseRequest req) {
        return service.saveCase(current.get(), assignmentId, req);
    }

    @PostMapping("/patients/{assignmentId}/case/finalize")
    public DoctorPatientDetail finalizeCase(@PathVariable Long assignmentId, @RequestBody(required = false) FinalizeRequest req) {
        return service.finalizeCase(current.get(), assignmentId, req);
    }
}
