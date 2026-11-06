package com.smartemergency.service;

import com.smartemergency.domain.IncidentType;
import com.smartemergency.domain.Severity;

import java.time.Duration;
import java.time.Instant;

/**
 * Priority score (0-100), as defined in the Glossary of the requirements document:
 * severity points + affected people points + type points + waiting points.
 */
public final class PriorityCalculator {
    private PriorityCalculator() {}

    public static int severityPoints(Severity s) {
        return switch (s) {
            case LOW -> 15;
            case MEDIUM -> 30;
            case HIGH -> 45;
            case CRITICAL -> 60;
        };
    }

    /** 2 points per affected person, max. 20. */
    public static int peoplePoints(int affectedPeople) {
        return Math.min(20, 2 * Math.max(0, affectedPeople));
    }

    public static int typePoints(IncidentType t) {
        return switch (t) {
            case FIRE, BUILDING_COLLAPSE -> 10;
            case TRAFFIC_ACCIDENT, FLOOD -> 8;
            case MEDICAL -> 6;
            case OTHER -> 4;
        };
    }

    /** 1 point per full minute without resources, max. 10. */
    public static int waitingPoints(Instant createdAt, Instant firstDispatchedAt, Instant now) {
        Instant end = firstDispatchedAt != null ? firstDispatchedAt : now;
        long minutes = Math.max(0, Duration.between(createdAt, end).toMinutes());
        return (int) Math.min(10, minutes);
    }

    public static int score(IncidentType type, Severity severity, int affectedPeople,
                            Instant createdAt, Instant firstDispatchedAt, Instant now) {
        int total = severityPoints(severity) + peoplePoints(affectedPeople) + typePoints(type)
                + waitingPoints(createdAt, firstDispatchedAt, now);
        return Math.min(100, total);
    }
}
