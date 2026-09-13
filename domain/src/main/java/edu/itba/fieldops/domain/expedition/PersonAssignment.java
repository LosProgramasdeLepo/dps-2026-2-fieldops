package edu.itba.fieldops.domain.expedition;

import java.util.Objects;
import java.util.UUID;

public record PersonAssignment(UUID activityId, UUID personId) implements Assignment {
    public PersonAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(personId, "person id");
    }
}
