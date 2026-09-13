package edu.itba.fieldops.domain.itinerary;

import java.util.Set;
import java.util.UUID;

public record ResourceRequirements(
        Set<UUID> certifications,
        boolean needsVehicle,
        boolean needsInstrument
) {
    public ResourceRequirements {
        certifications = Set.copyOf(certifications);
    }
}
