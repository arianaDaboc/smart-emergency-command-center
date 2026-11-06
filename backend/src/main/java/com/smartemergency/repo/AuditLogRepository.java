package com.smartemergency.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.smartemergency.domain.AuditLog;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    @Query("select count(distinct a.username) from AuditLog a where a.at > :since and a.username is not null")
    long countActiveUsersSince(@Param("since") Instant since);
}
