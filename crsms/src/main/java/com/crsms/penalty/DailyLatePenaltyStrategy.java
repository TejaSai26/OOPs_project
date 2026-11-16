package com.crsms.penalty;

import com.crsms.pricing.DailyPricingStrategy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class DailyLatePenaltyStrategy implements PenaltyStrategy {
    private final DailyPricingStrategy daily;

    public DailyLatePenaltyStrategy(DailyPricingStrategy daily) {
        this.daily = daily;
    }

    @Override
    public BigDecimal calculate(BigDecimal rate, Instant dueAt, Instant returnedAt, BigDecimal multiplier) {
        if (rate == null || dueAt == null || returnedAt == null || !returnedAt.isAfter(dueAt)) {
            return new BigDecimal("0.00");
        }
        BigDecimal factor = multiplier == null ? BigDecimal.ONE : multiplier;
        return daily.calculate(rate, dueAt, returnedAt).multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
