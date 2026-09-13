package edu.itba.fieldops.domain.shared;

import java.time.Instant;
import java.util.Objects;

public record TimePeriod(Instant start, Instant end) {
    public TimePeriod {
        Objects.requireNonNull(start, "period start");
        Objects.requireNonNull(end, "period end");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("period end must not be before start");
        }
    }

    public boolean contains(TimePeriod other) {
        Objects.requireNonNull(other, "other period");
        return !other.start.isBefore(start) && !other.end.isAfter(end);
    }

    public boolean overlaps(TimePeriod other) {
        Objects.requireNonNull(other, "other period");
        return !end.isBefore(other.start) && !other.end.isBefore(start);
    }

    public boolean finishesBeforeStartOf(TimePeriod other) {
        Objects.requireNonNull(other, "other period");
        return !end.isAfter(other.start);
    }
}
