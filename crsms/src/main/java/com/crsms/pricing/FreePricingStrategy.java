package com.crsms.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class FreePricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculate(BigDecimal rate, Instant start, Instant end) {
        return BigDecimal.ZERO.setScale(2);
    }
}
