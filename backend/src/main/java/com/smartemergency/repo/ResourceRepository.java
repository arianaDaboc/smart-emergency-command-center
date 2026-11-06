package com.smartemergency.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.smartemergency.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findAllByOrderByCode();
    List<Resource> findByTypeAndStatus(ResourceType type, ResourceStatus status);
    Optional<Resource> findByCrewUserId(Long userId);
    boolean existsByCode(String code);
    long countByStatus(ResourceStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resource r where r.id in :ids")
    List<Resource> lockByIds(@Param("ids") Collection<Long> ids);
}
