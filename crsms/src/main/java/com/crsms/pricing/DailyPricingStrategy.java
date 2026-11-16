package com.crsms.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class DailyPricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculate(BigDecimal rate, Instant start, Instant end) {
        long minutes = Math.max(1, Duration.between(start, end).toMinutes());
        long days = (minutes + (24 * 60) - 1) / (24 * 60);
        return rate.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }
}
