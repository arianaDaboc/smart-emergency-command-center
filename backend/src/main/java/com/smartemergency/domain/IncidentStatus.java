package com.smartemergency.domain;

public enum IncidentStatus {
    NEW, DISPATCHED, IN_PROGRESS, AT_HOSPITAL, CLOSED, CANCELLED;

    public boolean isOpen() {
        return this == NEW || this == DISPATCHED || this == IN_PROGRESS || this == AT_HOSPITAL;
    }
}
