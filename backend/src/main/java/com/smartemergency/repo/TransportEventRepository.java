package com.smartemergency.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.smartemergency.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface TransportEventRepository extends JpaRepository<TransportEvent, Long> {
    List<TransportEvent> findByAssignmentIdOrderByAt(Long assignmentId);
}
