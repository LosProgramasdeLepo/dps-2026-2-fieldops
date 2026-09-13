package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.shared.Texts;

import java.util.Objects;
import java.util.UUID;

public record ActivityResult(UUID activityId, String result) {
    public ActivityResult {
        Objects.requireNonNull(activityId, "activity id");
        result = Texts.required(result, "result");
    }
}
