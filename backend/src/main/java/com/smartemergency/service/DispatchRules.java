package com.smartemergency.service;

import com.smartemergency.domain.IncidentType;
import com.smartemergency.domain.ResourceType;

import java.util.EnumMap;
import java.util.Map;

/** Rules of the Smart Dispatch system: which resources an incident needs. */
public final class DispatchRules {
    private DispatchRules() {}

    /** Ambulances = affected people / 4 rounded up, min 1, max 3. */
    public static int ambulancesNeeded(int affectedPeople) {
        int n = (int) Math.ceil(Math.max(1, affectedPeople) / 4.0);
        return Math.max(1, Math.min(3, n));
    }

    public static Map<ResourceType, Integer> requiredResources(IncidentType type, int affectedPeople) {
        Map<ResourceType, Integer> req = new EnumMap<>(ResourceType.class);
        req.put(ResourceType.AMBULANCE, ambulancesNeeded(affectedPeople));
        switch (type) {
            case FIRE -> { req.put(ResourceType.FIRE_TEAM, 1); req.put(ResourceType.FIRE_TRUCK, 1); }
            case TRAFFIC_ACCIDENT -> req.put(ResourceType.POLICE_UNIT, 1);
            case BUILDING_COLLAPSE -> { req.put(ResourceType.FIRE_TEAM, 1); req.put(ResourceType.POLICE_UNIT, 1); }
            case FLOOD -> req.put(ResourceType.FIRE_TEAM, 1);
            default -> { }
        }
        return req;
    }
}
