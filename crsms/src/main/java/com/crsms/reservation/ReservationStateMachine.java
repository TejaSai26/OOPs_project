package com.crsms.reservation;

import com.crsms.model.enums.ReservationStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Legal reservation edges. Callers still live on {@code Reservation};
 * this table is the single place that decides whether a move is valid.
 */
public final class ReservationStateMachine {
    private static final Map<ReservationStatus, Set<ReservationStatus>> EDGES = new EnumMap<>(ReservationStatus.class);

    static {
        EDGES.put(ReservationStatus.REQUESTED, EnumSet.of(
                ReservationStatus.APPROVED, ReservationStatus.REJECTED, ReservationStatus.CANCELLED,
                ReservationStatus.EXPIRED, ReservationStatus.WAITLISTED));
        EDGES.put(ReservationStatus.WAITLISTED, EnumSet.of(
                ReservationStatus.REQUESTED, ReservationStatus.CANCELLED, ReservationStatus.EXPIRED));
        EDGES.put(ReservationStatus.APPROVED, EnumSet.of(
                ReservationStatus.ACTIVE, ReservationStatus.CANCELLED, ReservationStatus.EXPIRED));
        EDGES.put(ReservationStatus.ACTIVE, EnumSet.of(ReservationStatus.RETURNED));
        EDGES.put(ReservationStatus.REJECTED, EnumSet.noneOf(ReservationStatus.class));
        EDGES.put(ReservationStatus.CANCELLED, EnumSet.noneOf(ReservationStatus.class));
        EDGES.put(ReservationStatus.EXPIRED, EnumSet.noneOf(ReservationStatus.class));
        EDGES.put(ReservationStatus.RETURNED, EnumSet.noneOf(ReservationStatus.class));
    }

    private ReservationStateMachine() {
    }

    public static void move(ReservationStatus from, ReservationStatus to) {
        if (!EDGES.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("Cannot move reservation from " + from + " to " + to);
        }
    }
}
