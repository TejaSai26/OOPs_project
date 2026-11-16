package com.crsms.model.enums;

public enum UnitStatus {
    AVAILABLE,
    RESERVED,
    BORROWED,
    MAINTENANCE_REQUIRED,
    UNDER_REPAIR,
    INACTIVE,
    LOST;

    public boolean isReservable() {
        return this == AVAILABLE || this == RESERVED || this == BORROWED;
    }
}
