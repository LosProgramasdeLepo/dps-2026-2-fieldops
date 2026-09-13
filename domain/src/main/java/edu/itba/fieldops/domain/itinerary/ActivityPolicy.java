package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.RiskLevel;

import java.time.Duration;

public interface ActivityPolicy {
    Duration estimatedDuration();

    RiskLevel risk();

    ResourceRequirements requirements();
}
