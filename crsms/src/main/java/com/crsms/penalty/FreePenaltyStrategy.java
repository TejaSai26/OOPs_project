package com.crsms.penalty;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class FreePenaltyStrategy implements PenaltyStrategy {
    @Override
    public BigDecimal calculate(BigDecimal rate, Instant dueAt, Instant returnedAt, BigDecimal multiplier) {
        return new BigDecimal("0.00");
    }
}
