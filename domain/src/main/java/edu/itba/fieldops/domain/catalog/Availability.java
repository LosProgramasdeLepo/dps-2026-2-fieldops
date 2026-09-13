package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.TimePeriod;

import java.time.Instant;
import java.util.List;

public record Availability(List<TimePeriod> periods) {
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T00:00:00Z");

    public Availability {
        periods = List.copyOf(periods);
    }

    public static Availability always() {
        return new Availability(List.of(new TimePeriod(Instant.EPOCH, FAR_FUTURE)));
    }

    public boolean covers(TimePeriod needed) {
        return periods.stream().anyMatch(period -> period.contains(needed));
    }
}
