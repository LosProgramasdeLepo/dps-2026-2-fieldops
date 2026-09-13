package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.Quantity;

import java.util.Map;
import java.util.UUID;

public sealed interface Assignment permits PersonAssignment, VehicleAssignment, InstrumentAssignment, ConsumableAssignment {
    UUID activityId();

    default Map<UUID, Quantity> consumption() {
        return Map.of();
    }
}
