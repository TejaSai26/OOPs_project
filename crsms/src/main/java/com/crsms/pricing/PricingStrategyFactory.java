package com.crsms.pricing;

import com.crsms.model.enums.PricingType;
import org.springframework.stereotype.Component;

@Component
public class PricingStrategyFactory {
    private final FreePricingStrategy free;
    private final HourlyPricingStrategy hourly;
    private final DailyPricingStrategy daily;

    public PricingStrategyFactory(FreePricingStrategy free, HourlyPricingStrategy hourly, DailyPricingStrategy daily) {
        this.free = free;
        this.hourly = hourly;
        this.daily = daily;
    }

    public PricingStrategy forType(PricingType type) {
        return switch (type) {
            case FREE -> free;
            case HOURLY -> hourly;
            case DAILY -> daily;
        };
    }
}
