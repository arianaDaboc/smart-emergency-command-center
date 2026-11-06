package com.smartemergency.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.smartemergency.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface VitalSignRepository extends JpaRepository<VitalSign, Long> {
    List<VitalSign> findByAssignmentIdOrderByRecordedAt(Long assignmentId);
    long countByAssignmentId(Long assignmentId);
    Optional<VitalSign> findFirstByAssignmentIdOrderByRecordedAtDesc(Long assignmentId);
}
