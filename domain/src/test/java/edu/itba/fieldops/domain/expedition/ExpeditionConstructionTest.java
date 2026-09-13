package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.ActivityPolicy;
import edu.itba.fieldops.domain.itinerary.MeasurementPolicy;
import edu.itba.fieldops.domain.itinerary.SamplingPolicy;
import edu.itba.fieldops.domain.itinerary.TransitPolicy;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.validation.IssueSeverity;
import edu.itba.fieldops.domain.validation.ValidationIssue;
import edu.itba.fieldops.domain.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionConstructionTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta", "Paraná");

    @Test
    void draftHoldsObjectivesPeriodZonesAndRestrictions() {
        Expedition expedition = wetlandDraft();

        assertEquals(ExpeditionStatus.DRAFT, expedition.status());
        assertEquals(1, expedition.objectives().size());
        assertEquals(1, expedition.zones().size());
        assertEquals(1, expedition.responsibles().size());
        assertEquals(1, expedition.restrictions().size());
    }

    @Test
    void draftAcceptsOneActivityPerPolicy() {
        Expedition expedition = wetlandDraft();
        expedition.addActivity(sampling());
        expedition.addActivity(transit());
        expedition.addActivity(measurement());

        assertEquals(3, expedition.itinerary().size());
    }

    @Test
    void activityZoneMatchesByNameOnly() {
        Expedition expedition = wetlandDraft();

        expedition.addActivity(activity("sample", new SamplingPolicy(UUID.randomUUID()), 0, 4, new WorkZone("Delta")));

        assertEquals(1, expedition.itinerary().size());
    }

    @Test
    void rejectsActivityOutsidePeriod() {
        Expedition expedition = wetlandDraft();
        Activity late = activity("late", new TransitPolicy(), 200, 203, DELTA);

        assertThrows(IllegalArgumentException.class, () -> expedition.addActivity(late));
    }

    @Test
    void rejectsUnknownPredecessor() {
        Expedition expedition = wetlandDraft();
        Activity orphan = new Activity(
                UUID.randomUUID(),
                "dependent",
                new TransitPolicy(),
                window(0, 2),
                Set.of(UUID.randomUUID()),
                DELTA
        );

        assertThrows(IllegalArgumentException.class, () -> expedition.addActivity(orphan));
    }

    @Test
    void rejectsAssignmentToUnknownActivity() {
        Expedition expedition = wetlandDraft();

        assertThrows(
                IllegalArgumentException.class,
                () -> expedition.addAssignment(new PersonAssignment(UUID.randomUUID(), UUID.randomUUID()))
        );
    }

    @Test
    void simpleTransitionsReachFinished() {
        Expedition expedition = wetlandDraft();
        expedition.addActivity(sampling());
        expedition.submitForReview();
        expedition.approve(ValidationResult.empty());
        expedition.start();
        expedition.addIncident(Incident.of("rain delay", DAY.plusSeconds(3600)));
        expedition.finish();

        assertEquals(ExpeditionStatus.FINISHED, expedition.status());
        assertEquals(1, expedition.incidents().size());
    }

    @Test
    void rejectsDuplicateAssignmentAndPermit() {
        Expedition expedition = wetlandDraft();
        Activity activity = sampling();
        expedition.addActivity(activity);
        PersonAssignment assignment = new PersonAssignment(activity.id(), UUID.randomUUID());
        UUID permit = UUID.randomUUID();
        expedition.addAssignment(assignment);
        expedition.addPermit(permit);

        assertThrows(IllegalArgumentException.class, () -> expedition.addAssignment(assignment));
        assertThrows(IllegalArgumentException.class, () -> expedition.addPermit(permit));
    }

    @Test
    void rejectsDuplicateAcceptedWarning() {
        Expedition expedition = wetlandDraft();
        expedition.submitForReview();
        AcceptedWarning warning = acceptedCapacityWarning();
        expedition.acceptWarning(warning);

        assertThrows(IllegalArgumentException.class, () -> expedition.acceptWarning(warning));
    }

    @Test
    void returnToDraftClearsAcceptedWarnings() {
        Expedition expedition = wetlandDraft();
        expedition.submitForReview();
        expedition.acceptWarning(acceptedCapacityWarning());
        expedition.returnToDraft();

        assertTrue(expedition.acceptedWarnings().isEmpty());
    }

    @Test
    void cannotApproveFromDraft() {
        Expedition expedition = wetlandDraft();

        InvalidExpeditionTransition error = assertThrows(
                InvalidExpeditionTransition.class,
                () -> expedition.approve(ValidationResult.empty())
        );
        assertTrue(error.getMessage().contains("approve"));
        assertTrue(error.getMessage().contains("DRAFT"));
    }

    @Test
    void cannotApproveWithCriticalIssues() {
        Expedition expedition = wetlandDraft();
        expedition.submitForReview();
        ValidationIssue critical = new ValidationIssue(IssueSeverity.CRITICAL, "PERMIT", "missing permit");

        assertThrows(
                ExpeditionNotApprovable.class,
                () -> expedition.approve(new ValidationResult(List.of(critical)))
        );
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.status());
    }

    @Test
    void cannotApproveWarningWithoutJustification() {
        Expedition expedition = wetlandDraft();
        expedition.submitForReview();
        ValidationIssue warning = new ValidationIssue(IssueSeverity.WARNING, "RISK", "high risk");

        assertThrows(
                ExpeditionNotApprovable.class,
                () -> expedition.approve(new ValidationResult(List.of(warning)))
        );
    }

    @Test
    void approvesWhenWarningIsJustified() {
        Expedition expedition = wetlandDraft();
        expedition.submitForReview();
        ValidationIssue warning = new ValidationIssue(IssueSeverity.WARNING, "RISK", "high risk");
        expedition.acceptWarning(new AcceptedWarning(warning, "backup team on site", UUID.randomUUID()));

        expedition.approve(new ValidationResult(List.of(warning)));

        assertEquals(ExpeditionStatus.APPROVED, expedition.status());
    }

    @Test
    void suspendAndResumeFromInProgress() {
        Expedition expedition = approvedWithActivity();
        expedition.start();
        expedition.suspend();
        expedition.resume();

        assertEquals(ExpeditionStatus.IN_PROGRESS, expedition.status());
    }

    @Test
    void returnToDraftAllowsChangingItinerary() {
        Expedition expedition = wetlandDraft();
        Activity first = sampling();
        expedition.addActivity(first);
        expedition.submitForReview();
        expedition.returnToDraft();
        expedition.removeActivity(first.id());
        expedition.addActivity(transit());

        assertEquals(1, expedition.itinerary().size());
        assertEquals("Camp to site", expedition.itinerary().getFirst().name());
    }

    @Test
    void attachesPermitAndObservation() {
        Expedition expedition = wetlandDraft();
        expedition.addActivity(sampling());
        expedition.addPermit(UUID.randomUUID());
        expedition.submitForReview();
        expedition.approve(ValidationResult.empty());
        expedition.start();
        expedition.addObservation(new Observation("site wet", DAY));

        assertEquals(1, expedition.permits().size());
        assertEquals(1, expedition.observations().size());
    }

    @Test
    void tracksActivityExecution() {
        Expedition expedition = approvedWithActivity();
        Activity activity = expedition.itinerary().getFirst();
        expedition.start();
        expedition.startActivity(activity.id(), DAY);
        expedition.finishActivity(activity.id(), DAY.plusSeconds(3600), "samples stored");

        assertEquals(1, expedition.executions().size());
        assertTrue(expedition.executions().getFirst().isFinished());
    }

    private static Expedition approvedWithActivity() {
        Expedition expedition = wetlandDraft();
        expedition.addActivity(sampling());
        expedition.submitForReview();
        expedition.approve(ValidationResult.empty());
        return expedition;
    }

    private static Expedition wetlandDraft() {
        return Expedition.draft(
                UUID.randomUUID(),
                List.of(new Objective("Map wetland biodiversity")),
                new TimePeriod(DAY, DAY.plusSeconds(86_400 * 5)),
                List.of(DELTA),
                List.of(UUID.randomUUID()),
                List.of(new Restriction("No night work"))
        );
    }

    private static AcceptedWarning acceptedCapacityWarning() {
        ValidationIssue issue = new ValidationIssue(IssueSeverity.WARNING, "CAPACITY", "vehicle near capacity");
        return new AcceptedWarning(issue, "extra trailer available", UUID.randomUUID());
    }

    private static Activity sampling() {
        return activity("Soil sampling", new SamplingPolicy(UUID.randomUUID()), 0, 4, DELTA);
    }

    private static Activity transit() {
        return activity("Camp to site", new TransitPolicy(), 4, 6, DELTA);
    }

    private static Activity measurement() {
        return activity("Water measurement", new MeasurementPolicy(UUID.randomUUID()), 6, 9, DELTA);
    }

    private static Activity activity(String name, ActivityPolicy policy, int fromHour, int toHour, WorkZone zone) {
        return new Activity(UUID.randomUUID(), name, policy, window(fromHour, toHour), Set.of(), zone);
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }
}
