package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.RiskLevel;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record SamplingPolicy(UUID samplingCertification) implements ActivityPolicy {
    public SamplingPolicy {
        Objects.requireNonNull(samplingCertification, "sampling certification");
    }

    @Override
    public Duration estimatedDuration() {
        return Duration.ofHours(4);
    }

    @Override
    public RiskLevel risk() {
        return RiskLevel.MEDIUM;
    }

    @Override
    public ResourceRequirements requirements() {
        return new ResourceRequirements(Set.of(samplingCertification), false, false);
    }
}
