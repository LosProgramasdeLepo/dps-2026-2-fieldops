package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.shared.Texts;

import java.time.Instant;
import java.util.Objects;

public record Observation(String text, Instant at) {
    public Observation {
        text = Texts.required(text, "observation");
        Objects.requireNonNull(at, "observation time");
    }
}
