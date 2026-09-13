package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.RiskLevel;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record MeasurementPolicy(UUID operatorCertification) implements ActivityPolicy {
    public MeasurementPolicy {
        Objects.requireNonNull(operatorCertification, "operator certification");
    }

    @Override
    public Duration estimatedDuration() {
        return Duration.ofHours(3);
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.HIGH;
    }

    @Override
    public ResourceRequirements requirements() {
        return new ResourceRequirements(Set.of(operatorCertification), false, true);
    }
}
