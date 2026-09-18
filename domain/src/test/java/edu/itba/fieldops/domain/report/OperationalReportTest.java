package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.MeasurementPolicy;
import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.tracking.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalReportTest {
    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void derivesDurationRiskAndConsumptionFromExpedition() {
        Expedition expedition = draftWithMeasurement();
        Activity activity = expedition.itinerary().getFirst();
        UUID vials = UUID.randomUUID();
        expedition.addAssignment(new ConsumableAssignment(activity.id(), vials, new Quantity(5)));
        expedition.addAssignment(new ConsumableAssignment(activity.id(), vials, new Quantity(2)));

        OperationalReport report = OperationalReport.of(expedition);

        assertAll(
                () -> assertEquals(ExpeditionStatus.DRAFT, report.status()),
                () -> assertEquals(1, report.plannedActivities()),
                () -> assertEquals(0, report.startedActivities()),
                () -> assertEquals(0, report.finishedActivities()),
                () -> assertEquals(Duration.ofHours(3), report.duration()),
                () -> assertEquals(RiskLevel.HIGH, report.risk()),
                () -> assertEquals(new Quantity(7), report.consumption().get(vials)),
                () -> assertEquals(List.of(), report.activityResults())
        );
    }

    @Test
    void includesFinishedActivityResults() {
        Expedition expedition = draftWithMeasurement();
        Activity activity = expedition.itinerary().getFirst();
        expedition.submitForReview();
        expedition.approve(ValidationResult.empty());
        expedition.start();
        expedition.startActivity(activity.id(), START);
        expedition.finishActivity(activity.id(), START.plus(Duration.ofHours(3)), "samples stored");

        OperationalReport report = OperationalReport.of(expedition);

        assertAll(
                () -> assertEquals(ExpeditionStatus.IN_PROGRESS, report.status()),
                () -> assertEquals(1, report.plannedActivities()),
                () -> assertEquals(1, report.startedActivities()),
                () -> assertEquals(1, report.finishedActivities()),
                () -> assertEquals(List.of(new ActivityResult(activity.id(), "samples stored")), report.activityResults())
        );
    }

    @Test
    void includesIncidents() {
        Expedition expedition = draftWithMeasurement();
        expedition.submitForReview();
        expedition.approve(ValidationResult.empty());
        expedition.start();
        Incident incident = Incident.of("ventisca en el frente", START);
        expedition.addIncident(incident);

        OperationalReport report = OperationalReport.of(expedition);

        assertEquals(List.of(incident), report.incidents());
    }

    private static Expedition draftWithMeasurement() {
        Expedition expedition = Expedition.draft(
                UUID.randomUUID(),
                List.of(new Objective("Measure water")),
                new TimePeriod(START, START.plus(Duration.ofDays(2))),
                List.of(DELTA),
                List.of(UUID.randomUUID()),
                List.of(new Restriction("Daylight only"))
        );
        expedition.addActivity(new Activity(
                UUID.randomUUID(),
                "measure",
                new MeasurementPolicy(UUID.randomUUID()),
                new TimePeriod(START, START.plus(Duration.ofHours(3))),
                Set.of(),
                DELTA
        ));
        return expedition;
    }
}
