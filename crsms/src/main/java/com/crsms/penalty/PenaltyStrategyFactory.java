package com.crsms.penalty;

import com.crsms.model.enums.PricingType;
import org.springframework.stereotype.Component;

@Component
public class PenaltyStrategyFactory {
    private final FreePenaltyStrategy free;
    private final HourlyLatePenaltyStrategy hourly;
    private final DailyLatePenaltyStrategy daily;

    public PenaltyStrategyFactory(FreePenaltyStrategy free, HourlyLatePenaltyStrategy hourly, DailyLatePenaltyStrategy daily) {
        this.free = free;
        this.hourly = hourly;
        this.daily = daily;
    }

    public PenaltyStrategy forType(PricingType type) {
        return switch (type) {
            case FREE -> free;
            case HOURLY -> hourly;
            case DAILY -> daily;
        };
    }
}
