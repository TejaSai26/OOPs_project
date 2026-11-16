package com.crsms.penalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crsms.model.enums.PricingType;
import com.crsms.pricing.DailyPricingStrategy;
import com.crsms.pricing.HourlyPricingStrategy;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class PenaltyStrategyTest {
    @Test
    void hourlyLateFeeUsesRoundedHoursAndMultiplier() {
        BigDecimal penalty = new HourlyLatePenaltyStrategy(new HourlyPricingStrategy()).calculate(
                new BigDecimal("10.00"),
                Instant.parse("2026-04-01T10:00:00Z"),
                Instant.parse("2026-04-01T11:10:00Z"),
                new BigDecimal("1.50"));
        assertEquals(new BigDecimal("30.00"), penalty);
    }

    @Test
    void dailyLateFeeUsesRoundedDaysAndMultiplier() {
        BigDecimal penalty = new DailyLatePenaltyStrategy(new DailyPricingStrategy()).calculate(
                new BigDecimal("40"),
                Instant.parse("2026-04-01T10:00:00Z"),
                Instant.parse("2026-04-01T11:00:00Z"),
                new BigDecimal("2"));
        assertEquals(new BigDecimal("80.00"), penalty);
    }

    @Test
    void freeStrategyIgnoresLateness() {
        BigDecimal penalty = new FreePenaltyStrategy().calculate(
                new BigDecimal("10"),
                Instant.parse("2026-04-01T10:00:00Z"),
                Instant.parse("2026-04-03T10:00:00Z"),
                new BigDecimal("5"));
        assertEquals(new BigDecimal("0.00"), penalty);
    }

    @Test
    void onTimeReturnIsZero() {
        Instant due = Instant.parse("2026-04-01T12:00:00Z");
        BigDecimal penalty = new HourlyLatePenaltyStrategy(new HourlyPricingStrategy()).calculate(
                new BigDecimal("10"), due, due, new BigDecimal("1.5"));
        assertEquals(new BigDecimal("0.00"), penalty);
    }
}

@ExtendWith(MockitoExtension.class)
class PenaltyStrategyFactoryTest {
    @Mock FreePenaltyStrategy free;
    @Mock HourlyLatePenaltyStrategy hourly;
    @Mock DailyLatePenaltyStrategy daily;

    @Test
    void factorySelectsHourlyImplementation() {
        when(hourly.calculate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BigDecimal("12.00"));
        PenaltyStrategyFactory factory = new PenaltyStrategyFactory(free, hourly, daily);
        BigDecimal result = factory.forType(PricingType.HOURLY).calculate(
                BigDecimal.TEN, Instant.EPOCH, Instant.EPOCH.plusSeconds(10), BigDecimal.ONE);
        assertEquals(new BigDecimal("12.00"), result);
        verify(hourly).calculate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
