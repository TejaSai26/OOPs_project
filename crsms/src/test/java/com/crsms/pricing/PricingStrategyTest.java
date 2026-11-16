package com.crsms.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PricingStrategyTest {
    @Test
    void freeIsZero() {
        BigDecimal charge = new FreePricingStrategy().calculate(new BigDecimal("10"),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"));
        assertEquals(new BigDecimal("0.00"), charge);
    }

    @Test
    void hourlyRoundsUpPartialHours() {
        BigDecimal charge = new HourlyPricingStrategy().calculate(new BigDecimal("15.00"),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T01:05:00Z"));
        assertEquals(new BigDecimal("30.00"), charge);
    }

    @Test
    void dailyRoundsUpPartialDays() {
        BigDecimal charge = new DailyPricingStrategy().calculate(new BigDecimal("40"),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T01:00:00Z"));
        assertEquals(new BigDecimal("40.00"), charge);
    }
}
