package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.RiskLevel;

import java.time.Duration;

public sealed interface ActivityPolicy permits SamplingPolicy, MeasurementPolicy, TransitPolicy {
    Duration estimatedDuration();

    RiskLevel risk();

    ResourceRequirements requirements();
}
