package com.crsms.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.crsms.model.enums.PricingType;
import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.enums.UnitStatus;
import com.crsms.model.reservation.Reservation;
import com.crsms.model.resource.ResourceCategory;
import com.crsms.model.resource.ResourceListing;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.Student;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReservationTransitionTest {
    @Test
    void approveThenPickupThenReturn() {
        Student owner = new Student("a@campus.edu", "x", "A", "CS", "S1");
        Student borrower = new Student("b@campus.edu", "x", "B", "CS", "S2");
        ResourceListing listing = new ResourceListing(owner, new ResourceCategory("Bicycles"),
                "Bike", "A bike", PricingType.FREE, BigDecimal.ZERO);
        ResourceUnit unit = new ResourceUnit(listing, "B1", ResourceCondition.GOOD, "rack");
        Reservation reservation = new Reservation(borrower, unit,
                Instant.parse("2026-06-01T10:00:00Z"), Instant.parse("2026-06-01T12:00:00Z"));
        reservation.approve();
        reservation.markPickedUp();
        reservation.markReturned();
        assertEquals(ReservationStatus.RETURNED, reservation.getStatus());
    }

    @Test
    void pickupBeforeApprovalIsRejected() {
        Student owner = new Student("a@campus.edu", "x", "A", "CS", "S1");
        Student borrower = new Student("b@campus.edu", "x", "B", "CS", "S2");
        ResourceListing listing = new ResourceListing(owner, new ResourceCategory("Bicycles"),
                "Bike", "A bike", PricingType.FREE, BigDecimal.ZERO);
        ResourceUnit unit = new ResourceUnit(listing, "B1", ResourceCondition.GOOD, "rack");
        Reservation reservation = new Reservation(borrower, unit,
                Instant.parse("2026-06-01T10:00:00Z"), Instant.parse("2026-06-01T12:00:00Z"));
        assertThrows(IllegalStateException.class, reservation::markPickedUp);
        assertEquals(UnitStatus.AVAILABLE, unit.getStatus());
    }

    @Test
    void maintenanceStatusIsNotReservable() {
        assertEquals(false, UnitStatus.UNDER_REPAIR.isReservable());
        assertEquals(false, UnitStatus.MAINTENANCE_REQUIRED.isReservable());
    }
}
