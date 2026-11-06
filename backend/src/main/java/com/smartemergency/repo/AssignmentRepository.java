package com.smartemergency.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.smartemergency.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByIncidentIdOrderById(Long incidentId);
    List<Assignment> findByIncidentIdAndActiveTrue(Long incidentId);
    Optional<Assignment> findFirstByResourceIdAndActiveTrue(Long resourceId);
    List<Assignment> findByActiveTrue();
    @Query("select a from Assignment a where a.destinationHospital.id = :hid and a.transportStatus in :st order by a.assignedAt desc")
    List<Assignment> findForHospital(@Param("hid") Long hospitalId, @Param("st") Collection<TransportStatus> statuses);
}
