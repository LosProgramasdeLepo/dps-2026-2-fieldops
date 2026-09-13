package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.shared.Texts;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Incident(String description, Instant at, UUID activityId) {
    public Incident {
        description = Texts.required(description, "incident");
        Objects.requireNonNull(at, "incident time");
    }

    public static Incident of(String description, Instant at) {
        return new Incident(description, at, null);
    }
}
