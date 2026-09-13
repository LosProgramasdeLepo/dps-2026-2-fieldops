package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityPolicyConstructionTest {
    @Test
    void eachPolicyCanBuildAnActivity() {
        Activity sampling = activity("sample", new SamplingPolicy(UUID.randomUUID()), Duration.ofHours(4));
        Activity transit = activity("move", new TransitPolicy(), Duration.ofHours(2));
        Activity measurement = activity("measure", new MeasurementPolicy(UUID.randomUUID()), Duration.ofHours(3));

        assertFalse(sampling.requirements().needsVehicle());
        assertTrue(transit.requirements().needsVehicle());
        assertTrue(measurement.requirements().needsInstrument());
        assertEquals(RiskLevel.HIGH, measurement.risk());
        assertEquals(Duration.ofHours(2), transit.estimatedDuration());
    }

    @Test
    void rejectsSelfAsPredecessor() {
        UUID id = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new Activity(
                id,
                "loop",
                new TransitPolicy(),
                new TimePeriod(
                        Instant.parse("2026-11-01T08:00:00Z"),
                        Instant.parse("2026-11-01T10:00:00Z")
                ),
                Set.of(id),
                new WorkZone("Delta")
        ));
    }

    @Test
    void rejectsWindowShorterThanEstimate() {
        Instant start = Instant.parse("2026-11-01T08:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> new Activity(
                UUID.randomUUID(),
                "short",
                new TransitPolicy(),
                new TimePeriod(start, start.plus(Duration.ofHours(1))),
                Set.of(),
                new WorkZone("Delta")
        ));
    }

    private static Activity activity(String name, ActivityPolicy policy, Duration window) {
        Instant start = Instant.parse("2026-11-01T08:00:00Z");
        return new Activity(
                UUID.randomUUID(),
                name,
                policy,
                new TimePeriod(start, start.plus(window)),
                Set.of(),
                new WorkZone("Delta")
        );
    }
}
