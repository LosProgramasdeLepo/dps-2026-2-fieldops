package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.RiskLevel;

import java.time.Duration;
import java.util.Set;

public record TransitPolicy() implements ActivityPolicy {
    @Override
    public Duration estimatedDuration() {
        return Duration.ofHours(2);
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.LOW;
    }

    @Override
    public ResourceRequirements requirements() {
        return new ResourceRequirements(Set.of(), true, false);
    }
}
