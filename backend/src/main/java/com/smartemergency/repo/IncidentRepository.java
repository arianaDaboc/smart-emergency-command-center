package com.smartemergency.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.smartemergency.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);
    List<Incident> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Incident> findBySourceAndStatusIn(IncidentSource source, Collection<IncidentStatus> statuses);
    long countByStatus(IncidentStatus status);
}
