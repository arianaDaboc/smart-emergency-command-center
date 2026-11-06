package com.smartemergency.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.smartemergency.domain.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistoryEntry, Long> {
    List<MedicalHistoryEntry> findByPatientIdOrderByEventDateDesc(Long patientId);
}
