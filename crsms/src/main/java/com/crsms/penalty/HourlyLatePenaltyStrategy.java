package com.crsms.penalty;

import com.crsms.pricing.HourlyPricingStrategy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class HourlyLatePenaltyStrategy implements PenaltyStrategy {
    private final HourlyPricingStrategy hourly;

    public HourlyLatePenaltyStrategy(HourlyPricingStrategy hourly) {
        this.hourly = hourly;
    }

    @Override
    public BigDecimal calculate(BigDecimal rate, Instant dueAt, Instant returnedAt, BigDecimal multiplier) {
        if (rate == null || dueAt == null || returnedAt == null || !returnedAt.isAfter(dueAt)) {
            return new BigDecimal("0.00");
        }
        BigDecimal factor = multiplier == null ? BigDecimal.ONE : multiplier;
        return hourly.calculate(rate, dueAt, returnedAt).multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
