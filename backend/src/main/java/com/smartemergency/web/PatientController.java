package com.smartemergency.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartemergency.service.CurrentUser;
import com.smartemergency.service.PatientService;
import com.smartemergency.web.Dto.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
public class PatientController {
    private final PatientService service;
    private final CurrentUser current;

    public PatientController(PatientService service, CurrentUser current) {
        this.service = service;
        this.current = current;
    }

    /** Activate Emergency Request (SOS button). */
    @PostMapping("/emergencies")
    @ResponseStatus(HttpStatus.CREATED)
    public PatientEmergencyView sos(@Valid @RequestBody SosRequest req) {
        return service.sos(current.get(), req);
    }

    /** View Emergency Status: 204 when there is no open emergency. */
    @GetMapping("/emergencies/current")
    public ResponseEntity<PatientEmergencyView> current() {
        return service.current(current.get()).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/emergencies")
    public List<IncidentView> past() { return service.past(current.get()); }

    @PostMapping("/emergencies/{id}/cancel")
    public PatientEmergencyView cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest req) {
        return service.cancel(current.get(), id, req.reason());
    }

    @GetMapping("/medical-card")
    public MedicalCardAndHistory card() { return service.medicalCard(current.get()); }

    @GetMapping("/contacts")
    public List<ContactView> contacts() { return service.contacts(current.get()); }

    @PostMapping("/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactView add(@Valid @RequestBody ContactRequest req) { return service.addContact(current.get(), req); }

    @PutMapping("/contacts/{id}")
    public ContactView update(@PathVariable Long id, @Valid @RequestBody ContactRequest req) {
        return service.updateContact(current.get(), id, req);
    }

    @DeleteMapping("/contacts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.deleteContact(current.get(), id); }
}
