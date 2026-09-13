package edu.itba.fieldops.domain.expedition;

import java.util.Objects;
import java.util.UUID;

public record InstrumentAssignment(UUID activityId, UUID instrumentId) implements Assignment {
    public InstrumentAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(instrumentId, "instrument id");
    }
}
