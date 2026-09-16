package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.expedition.AcceptedWarning;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.MeasurementPolicy;
import edu.itba.fieldops.domain.itinerary.SamplingPolicy;
import edu.itba.fieldops.domain.itinerary.TransitPolicy;
import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionValidatorTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");
    private static final ExpeditionValidator VALIDATOR = new ExpeditionValidator();

    @Test
    void validSamplingPlanHasNoIssues() {
        SamplingPlan plan = samplingPlan(0, 4);

        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of());

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void samePersonCanJoinAdjacentExpeditions() {
        SamplingPlan morning = samplingPlan(0, 4);
        Expedition afternoon = samplingOn(morning, 4, 8);
        morning.expedition.submitForReview();

        ValidationResult result = VALIDATOR.validate(afternoon, morning.catalog, List.of(morning.expedition));

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void overlappingWindowsOnOccupyingExpeditionAreCritical() {
        SamplingPlan first = samplingPlan(0, 4);
        Expedition second = samplingOn(first, 0, 4);
        first.expedition.submitForReview();

        ValidationResult result = VALIDATOR.validate(second, first.catalog, List.of(first.expedition));

        assertIssue(result, IssueSeverity.CRITICAL, "OVERLAP");
    }

    @Test
    void draftPeerDoesNotOccupyThePerson() {
        SamplingPlan first = samplingPlan(0, 4);
        Expedition second = samplingOn(first, 0, 4);

        ValidationResult result = VALIDATOR.validate(second, first.catalog, List.of(first.expedition));

        assertNo(result, "OVERLAP");
    }

    @Test
    void finishedPeerDoesNotOccupyThePerson() {
        SamplingPlan first = samplingPlan(0, 4);
        Expedition second = samplingOn(first, 0, 4);
        finish(first.expedition, first.activity);

        ValidationResult result = VALIDATOR.validate(second, first.catalog, List.of(first.expedition));

        assertEquals(ExpeditionStatus.FINISHED, first.expedition.status());
        assertNo(result, "OVERLAP");
    }

    @Test
    void adjacentActivitiesInTheSameExpeditionDoNotOverlap() {
        SamplingPlan plan = samplingPlan(0, 4);
        Activity later = sampling(plan.certification.id(), 4, 8);
        plan.expedition.addActivity(later);
        plan.expedition.addAssignment(new PersonAssignment(later.id(), plan.person.id()));

        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of());

        assertNo(result, "OVERLAP");
    }

    @Test
    void overlappingActivitiesInTheSameExpeditionAreCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Activity later = sampling(plan.certification.id(), 2, 6);
        plan.expedition.addActivity(later);
        plan.expedition.addAssignment(new PersonAssignment(later.id(), plan.person.id()));

        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "OVERLAP");
    }

    @Test
    void catalogUnavailabilityIsCritical() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person person = new Person(
                UUID.randomUUID(),
                "Ada",
                List.of(certification),
                new Availability(List.of(window(0, 4)))
        );
        SamplingPlan plan = samplingPlan(person, certification, 4, 8);

        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "AVAILABILITY");
    }

    @Test
    void transitWithoutVehicleIsCritical() {
        Expedition expedition = draft();
        Activity activity = transit(4, 6);
        Permit permit = permitFor(activity);
        expedition.addActivity(activity);
        expedition.addPermit(permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(permit);

        ValidationResult result = VALIDATOR.validate(expedition, catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
    }

    @Test
    void measurementWithoutInstrumentIsCritical() {
        Certification operator = new Certification(UUID.randomUUID(), "Operator");
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(operator), Availability.always());
        Activity activity = measurement(operator.id(), 0, 3);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()));
        expedition.addPermit(permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(permit);

        ValidationResult result = VALIDATOR.validate(expedition, catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
        assertNo(result, "CERTIFICATION");
    }

    @Test
    void unknownAssignedPersonIsCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Activity extra = sampling(plan.certification.id(), 4, 8);
        plan.expedition.addActivity(extra);
        plan.expedition.addAssignment(new PersonAssignment(extra.id(), UUID.randomUUID()));

        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "RESOURCE");
    }

    @Test
    void missingCertificationIsCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Person unqualified = new Person(UUID.randomUUID(), "Bob", List.of(), Availability.always());
        plan.catalog.add(unqualified);
        Activity extra = sampling(plan.certification.id(), 4, 8);
        plan.expedition.addActivity(extra);
        plan.expedition.addAssignment(new PersonAssignment(extra.id(), unqualified.id()));

        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "CERTIFICATION");
    }

    @Test
    void stockShortfallIsCritical() {
        SamplingPlan plan = samplingPlan(0, 4);
        Consumable vials = new Consumable(UUID.randomUUID(), "vials", new Quantity(10));
        plan.catalog.add(vials);
        plan.expedition.addAssignment(new ConsumableAssignment(plan.activity.id(), vials.id(), new Quantity(15)));

        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "STOCK");
    }

    @Test
    void occupyingPeerConsumesStock() {
        SamplingPlan first = samplingPlan(0, 4);
        Consumable vials = new Consumable(UUID.randomUUID(), "vials", new Quantity(10));
        first.catalog.add(vials);
        first.expedition.addAssignment(new ConsumableAssignment(first.activity.id(), vials.id(), new Quantity(6)));
        first.expedition.submitForReview();
        Expedition second = samplingOn(first, 4, 8);
        second.addAssignment(new ConsumableAssignment(second.itinerary().getFirst().id(), vials.id(), new Quantity(5)));

        ValidationResult result = VALIDATOR.validate(second, first.catalog, List.of(first.expedition));

        assertIssue(result, IssueSeverity.CRITICAL, "STOCK");
    }

    @Test
    void selfPassedInOthersDoesNotDoubleCountStock() {
        SamplingPlan plan = samplingPlan(0, 4);
        Consumable vials = new Consumable(UUID.randomUUID(), "vials", new Quantity(10));
        plan.catalog.add(vials);
        plan.expedition.addAssignment(new ConsumableAssignment(plan.activity.id(), vials.id(), new Quantity(8)));

        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of(plan.expedition));

        assertNo(result, "STOCK");
    }

    @Test
    void excessCapacityIsAWarning() {
        TransitPlan plan = crowdedTransit();

        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of());

        assertIssue(result, IssueSeverity.WARNING, "CAPACITY");
        assertFalse(result.hasCritical());
    }

    @Test
    void missingPermitIsCritical() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draft();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);

        ValidationResult result = VALIDATOR.validate(expedition, catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
    }

    @Test
    void permitDoesNotCoverADifferentZone() {
        WorkZone coast = new WorkZone("Coast");
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity activity = new Activity(
                UUID.randomUUID(),
                "coast sample",
                new SamplingPolicy(certification.id()),
                window(0, 4),
                Set.of(),
                coast
        );
        Permit deltaPermit = new Permit(UUID.randomUUID(), DELTA, activity.window());
        Expedition expedition = Expedition.draft(
                UUID.randomUUID(),
                List.of(new Objective("Map wetland biodiversity")),
                week(),
                List.of(DELTA, coast),
                List.of(UUID.randomUUID()),
                List.of(new Restriction("No night work"))
        );
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()));
        expedition.addPermit(deltaPermit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(deltaPermit);

        ValidationResult result = VALIDATOR.validate(expedition, catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
    }

    @Test
    void permitThatDoesNotCoverTheWindowIsCritical() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 4, 8);
        Permit morningOnly = new Permit(UUID.randomUUID(), DELTA, window(0, 4));
        Expedition expedition = draft();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()));
        expedition.addPermit(morningOnly.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(morningOnly);

        ValidationResult result = VALIDATOR.validate(expedition, catalog, List.of());

        assertIssue(result, IssueSeverity.CRITICAL, "PERMIT");
    }

    @Test
    void approveUsesValidatorResult() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draft();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()));
        expedition.submitForReview();
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        ValidationResult result = VALIDATOR.validate(expedition, catalog, List.of());

        assertThrows(ExpeditionNotApprovable.class, () -> expedition.approve(result));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.status());
    }

    @Test
    void capacityWarningCanBeJustifiedAndApproved() {
        TransitPlan plan = crowdedTransit();
        plan.expedition.submitForReview();
        ValidationResult result = VALIDATOR.validate(plan.expedition, plan.catalog, List.of());
        result.warnings().forEach(warning -> plan.expedition.acceptWarning(
                new AcceptedWarning(warning, "extra trailer available", UUID.randomUUID())
        ));

        plan.expedition.approve(result);

        assertEquals(ExpeditionStatus.APPROVED, plan.expedition.status());
    }

    private static void finish(Expedition expedition, Activity activity) {
        expedition.submitForReview();
        expedition.approve(ValidationResult.empty());
        expedition.start();
        expedition.startActivity(activity.id(), DAY);
        expedition.finishActivity(activity.id(), DAY.plusSeconds(4 * 3600L), "samples stored");
        expedition.finish();
    }

    private static void assertIssue(ValidationResult result, IssueSeverity severity, String code) {
        boolean found = result.issues().stream()
                .anyMatch(issue -> issue.severity() == severity && issue.code().equals(code));
        assertTrue(found, () -> "expected " + severity + " " + code + " in " + result.issues());
    }

    private static void assertNo(ValidationResult result, String code) {
        boolean found = result.issues().stream().anyMatch(issue -> issue.code().equals(code));
        assertFalse(found, () -> "did not expect " + code + " in " + result.issues());
    }

    private static SamplingPlan samplingPlan(int fromHour, int toHour) {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        return samplingPlan(person, certification, fromHour, toHour);
    }

    private static SamplingPlan samplingPlan(Person person, Certification certification, int fromHour, int toHour) {
        Activity activity = sampling(certification.id(), fromHour, toHour);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), person.id()));
        expedition.addPermit(permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(person);
        catalog.add(permit);
        return new SamplingPlan(expedition, catalog, activity, person, certification);
    }

    private static Expedition samplingOn(SamplingPlan source, int fromHour, int toHour) {
        Activity activity = sampling(source.certification.id(), fromHour, toHour);
        Permit permit = permitFor(activity);
        Expedition expedition = draft();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), source.person.id()));
        expedition.addPermit(permit.id());
        source.catalog.add(permit);
        return expedition;
    }

    private static TransitPlan crowdedTransit() {
        Activity activity = transit(4, 6);
        Permit permit = permitFor(activity);
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(), Availability.always());
        Person bob = new Person(UUID.randomUUID(), "Bob", List.of(), Availability.always());
        Vehicle vehicle = new Vehicle(UUID.randomUUID(), new Quantity(1), Availability.always());
        Expedition expedition = draft();
        expedition.addActivity(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), ada.id()));
        expedition.addAssignment(new PersonAssignment(activity.id(), bob.id()));
        expedition.addAssignment(new VehicleAssignment(activity.id(), vehicle.id()));
        expedition.addPermit(permit.id());
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(bob);
        catalog.add(vehicle);
        catalog.add(permit);
        return new TransitPlan(expedition, catalog);
    }

    private static Expedition draft() {
        return Expedition.draft(
                UUID.randomUUID(),
                List.of(new Objective("Map wetland biodiversity")),
                week(),
                List.of(DELTA),
                List.of(UUID.randomUUID()),
                List.of(new Restriction("No night work"))
        );
    }

    private static Activity sampling(UUID certificationId, int fromHour, int toHour) {
        return new Activity(
                UUID.randomUUID(),
                "sample",
                new SamplingPolicy(certificationId),
                window(fromHour, toHour),
                Set.of(),
                DELTA
        );
    }

    private static Activity transit(int fromHour, int toHour) {
        return new Activity(
                UUID.randomUUID(),
                "transit",
                new TransitPolicy(),
                window(fromHour, toHour),
                Set.of(),
                DELTA
        );
    }

    private static Activity measurement(UUID certificationId, int fromHour, int toHour) {
        return new Activity(
                UUID.randomUUID(),
                "measure",
                new MeasurementPolicy(certificationId),
                window(fromHour, toHour),
                Set.of(),
                DELTA
        );
    }

    private static Permit permitFor(Activity activity) {
        return new Permit(UUID.randomUUID(), activity.zone(), activity.window());
    }

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static TimePeriod week() {
        return new TimePeriod(DAY, DAY.plusSeconds(86_400L * 5));
    }

    private record SamplingPlan(
            Expedition expedition,
            ResourceCatalog catalog,
            Activity activity,
            Person person,
            Certification certification
    ) {
    }

    private record TransitPlan(Expedition expedition, ResourceCatalog catalog) {
    }
}
