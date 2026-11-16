package com.crsms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "crsms.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class CampusScheduler {
    private static final Logger log = LoggerFactory.getLogger(CampusScheduler.class);
    private final ReservationService reservations;
    private final BorrowingService borrowings;

    public CampusScheduler(ReservationService reservations, BorrowingService borrowings) {
        this.reservations = reservations;
        this.borrowings = borrowings;
    }

    @Scheduled(fixedDelayString = "${crsms.jobs.delay-ms:60000}")
    public void tick() {
        try {
            int expired = reservations.expireDue();
            if (expired > 0) {
                log.info("Expired {} reservations", expired);
            }
        } catch (RuntimeException ex) {
            log.warn("Reservation expiry failed: {}", ex.getMessage());
        }
        try {
            borrowings.flagOverdueAndRemind();
        } catch (RuntimeException ex) {
            log.warn("Overdue scan failed: {}", ex.getMessage());
        }
    }
}
