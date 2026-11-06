package com.smartemergency.domain;

import java.util.List;

/** Phases of an ambulance assignment. The first six are the normal order. */
public enum TransportStatus {
    ASSIGNED, EN_ROUTE, ON_SCENE, TRANSPORTING, AT_HOSPITAL, HANDED_OVER,
    CANCELLED, NO_TRANSPORT;

    public static final List<TransportStatus> ORDER =
            List.of(ASSIGNED, EN_ROUTE, ON_SCENE, TRANSPORTING, AT_HOSPITAL, HANDED_OVER);

    /** The status the crew may move to next, or null if none. */
    public TransportStatus next() {
        int i = ORDER.indexOf(this);
        return (i < 0 || i >= ORDER.size() - 1) ? null : ORDER.get(i + 1);
    }

    /** Ambulance is physically engaged with the patient (blocks closing the incident). */
    public boolean isActiveTransport() {
        return this == EN_ROUTE || this == ON_SCENE || this == TRANSPORTING || this == AT_HOSPITAL;
    }

    public boolean allowsVitalSigns() {
        return this == ON_SCENE || this == TRANSPORTING || this == AT_HOSPITAL;
    }
}
