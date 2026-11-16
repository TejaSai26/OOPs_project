package com.crsms.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class HourlyPricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculate(BigDecimal rate, Instant start, Instant end) {
        long minutes = Math.max(1, Duration.between(start, end).toMinutes());
        long hours = (minutes + 59) / 60;
        return rate.multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
    }
}
