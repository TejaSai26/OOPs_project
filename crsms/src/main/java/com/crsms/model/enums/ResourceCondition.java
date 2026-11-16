package com.crsms.model.enums;

public enum ResourceCondition {
    NEW,
    GOOD,
    FAIR,
    DAMAGED,
    UNDER_MAINTENANCE;

    public boolean blocksReservation() {
        return this == DAMAGED || this == UNDER_MAINTENANCE;
    }
}
