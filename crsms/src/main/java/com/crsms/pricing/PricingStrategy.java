package com.crsms.pricing;

import java.math.BigDecimal;
import java.time.Instant;

public interface PricingStrategy {
    BigDecimal calculate(BigDecimal rate, Instant start, Instant end);
}
