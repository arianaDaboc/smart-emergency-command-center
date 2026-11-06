package com.smartemergency.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.smartemergency.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRoleOrderByFullName(Role role);
    @Query("select u from User u where u.role = com.smartemergency.domain.Role.PATIENT and lower(u.fullName) like lower(concat('%', :q, '%')) order by u.fullName")
    List<User> searchPatients(@Param("q") String q);
}
