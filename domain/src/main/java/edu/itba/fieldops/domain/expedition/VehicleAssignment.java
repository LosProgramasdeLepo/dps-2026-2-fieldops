package edu.itba.fieldops.domain.expedition;

import java.util.Objects;
import java.util.UUID;

public record VehicleAssignment(UUID activityId, UUID vehicleId) implements Assignment {
    public VehicleAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(vehicleId, "vehicle id");
    }
}
