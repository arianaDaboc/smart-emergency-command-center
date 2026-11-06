package com.smartemergency.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.smartemergency.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface MedicalCardRepository extends JpaRepository<MedicalCard, Long> {
    Optional<MedicalCard> findByPatientId(Long patientId);
}
