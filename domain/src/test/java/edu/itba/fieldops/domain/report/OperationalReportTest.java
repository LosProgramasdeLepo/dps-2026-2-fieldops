package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.MeasurementPolicy;
import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalReportTest {
    @Test
    void derivesDurationRiskAndConsumptionFromExpedition() {
        Instant start = Instant.parse("2026-11-01T08:00:00Z");
        WorkZone delta = new WorkZone("Delta");
        Expedition expedition = Expedition.draft(
                UUID.randomUUID(),
                List.of(new Objective("Measure water")),
                new TimePeriod(start, start.plus(Duration.ofDays(2))),
                List.of(delta),
                List.of(UUID.randomUUID()),
                List.of(new Restriction("Daylight only"))
        );
        Activity activity = new Activity(
                UUID.randomUUID(),
                "measure",
                new MeasurementPolicy(UUID.randomUUID()),
                new TimePeriod(start, start.plus(Duration.ofHours(3))),
                Set.of(),
                delta
        );
        expedition.addActivity(activity);
        UUID vials = UUID.randomUUID();
        expedition.addAssignment(new ConsumableAssignment(activity.id(), vials, new Quantity(5)));
        expedition.addAssignment(new ConsumableAssignment(activity.id(), vials, new Quantity(2)));

        OperationalReport report = OperationalReport.of(expedition);

        assertEquals(Duration.ofHours(3), report.duration());
        assertEquals(RiskLevel.HIGH, report.risk());
        assertEquals(new Quantity(7), report.consumption().get(vials));
    }
}
