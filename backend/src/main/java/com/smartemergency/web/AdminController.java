package com.smartemergency.web;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartemergency.service.AdminService;
import com.smartemergency.service.CurrentUser;
import com.smartemergency.web.Dto.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final CurrentUser current;

    public AdminController(AdminService admin, CurrentUser current) {
        this.admin = admin;
        this.current = current;
    }

    @GetMapping("/users")
    public List<UserView> users() { return admin.listUsers(); }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody CreateUserRequest req) { return admin.create(req); }

    @PutMapping("/users/{id}")
    public UserView update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) { return admin.update(id, req); }

    @PostMapping("/users/{id}/enabled")
    public UserView enabled(@PathVariable Long id, @RequestBody EnabledRequest req) {
        return admin.setEnabled(current.get(), id, req.enabled());
    }

    @GetMapping("/ambulances")
    public List<ResourceView> ambulances() { return admin.ambulances(); }

    @GetMapping("/status")
    public SystemStatus status() { return admin.status(); }

    @GetMapping("/audit")
    public PageView<AuditView> audit(@RequestParam(required = false) String username,
                                     @RequestParam(required = false) String action,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                     @RequestParam(defaultValue = "0") int page) {
        return admin.audit(username, action, from, to, page);
    }

    @GetMapping("/audit/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String username,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        byte[] csv = admin.exportCsv(username, action, from, to).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log_" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
