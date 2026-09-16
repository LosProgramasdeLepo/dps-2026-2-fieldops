package edu.itba.fieldops.domain.shared;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimePeriodTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");

    @Test
    void adjacentWindowsDoNotOverlap() {
        TimePeriod morning = hours(0, 4);
        TimePeriod afternoon = hours(4, 8);

        assertFalse(morning.overlaps(afternoon));
        assertFalse(afternoon.overlaps(morning));
        assertTrue(morning.finishesBeforeStartOf(afternoon));
    }

    @Test
    void interiorWindowsOverlap() {
        TimePeriod first = hours(0, 4);
        TimePeriod second = hours(3, 6);

        assertTrue(first.overlaps(second));
        assertTrue(second.overlaps(first));
    }

    @Test
    void containsClosedInterval() {
        TimePeriod week = hours(0, 24);
        TimePeriod morning = hours(0, 4);

        assertTrue(week.contains(morning));
        assertTrue(week.contains(week));
        assertFalse(morning.contains(week));
    }

    @Test
    void shiftedKeepsLength() {
        TimePeriod morning = hours(0, 4);

        TimePeriod delayed = morning.shifted(Duration.ofHours(2));

        assertEquals(hours(2, 6), delayed);
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () -> new TimePeriod(hours(4, 8).end(), hours(0, 4).start()));
    }

    private static TimePeriod hours(int from, int to) {
        return new TimePeriod(DAY.plusSeconds(from * 3600L), DAY.plusSeconds(to * 3600L));
    }
}
