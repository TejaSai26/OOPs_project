package com.crsms.penalty;

import java.math.BigDecimal;
import java.time.Instant;

public interface PenaltyStrategy {
    BigDecimal calculate(BigDecimal rate, Instant dueAt, Instant returnedAt, BigDecimal multiplier);
}
