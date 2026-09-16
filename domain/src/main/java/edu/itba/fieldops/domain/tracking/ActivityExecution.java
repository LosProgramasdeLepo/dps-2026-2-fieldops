package edu.itba.fieldops.domain.tracking;

import edu.itba.fieldops.domain.shared.Texts;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ActivityExecution {
    private final UUID activityId;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final String result;

    public ActivityExecution(UUID activityId, Instant startedAt) {
        this(activityId, startedAt, null, null);
    }

    private ActivityExecution(UUID activityId, Instant startedAt, Instant finishedAt, String result) {
        this.activityId = Objects.requireNonNull(activityId, "activity id");
        this.startedAt = Objects.requireNonNull(startedAt, "started at");
        this.finishedAt = finishedAt;
        this.result = result;
    }

    public ActivityExecution finish(Instant finishedAt, String result) {
        if (this.finishedAt != null) {
            throw new InvalidActivityExecution("activity already finished: " + activityId);
        }
        Objects.requireNonNull(finishedAt, "finished at");
        if (finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("finish must not be before start");
        }
        return new ActivityExecution(activityId, startedAt, finishedAt, Texts.required(result, "result"));
    }

    public UUID activityId() {
        return activityId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public String result() {
        return result;
    }

    public boolean isFinished() {
        return finishedAt != null;
    }
}
