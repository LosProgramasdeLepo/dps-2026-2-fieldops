package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Vehicle;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentSuggesterTest {
    private final AssignmentSuggester suggester = new AssignmentSuggester();

    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void proposesCertifiedPersonForSampling() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);

        List<Assignment> suggestions = suggester.suggest(expedition, catalog, List.of());

        assertEquals(List.of(new PersonAssignment(activity.id(), ada.id())), suggestions);
    }

    @Test
    void doesNotProposeWhenRequirementsAreAlreadyAssigned() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(activity);
        expedition.addAssignment(new PersonAssignment(activity.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);

        List<Assignment> suggestions = suggester.suggest(expedition, catalog, List.of());

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void skipsPersonTakenByOccupyingOverlapAndPicksTheNext() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Person bob = new Person(UUID.randomUUID(), "Bob", List.of(certification), Availability.always());
        Activity firstActivity = sampling(certification.id(), 0, 4);
        Expedition occupying = draftWith(firstActivity);
        occupying.addAssignment(new PersonAssignment(firstActivity.id(), ada.id()));
        occupying.submitForReview();
        Activity secondActivity = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(secondActivity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(bob);

        List<Assignment> suggestions = suggester.suggest(expedition, catalog, List.of(occupying));

        assertEquals(List.of(new PersonAssignment(secondActivity.id(), bob.id())), suggestions);
    }

    @Test
    void reusesPersonOnAdjacentWindow() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity morning = sampling(certification.id(), 0, 4);
        Expedition occupying = draftWith(morning);
        occupying.addAssignment(new PersonAssignment(morning.id(), ada.id()));
        occupying.submitForReview();
        Activity afternoon = sampling(certification.id(), 4, 8);
        Expedition expedition = draftWith(afternoon);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);

        List<Assignment> suggestions = suggester.suggest(expedition, catalog, List.of(occupying));

        assertEquals(List.of(new PersonAssignment(afternoon.id(), ada.id())), suggestions);
    }

    @Test
    void proposesVehicleForTransit() {
        Vehicle vehicle = new Vehicle(UUID.randomUUID(), new Quantity(4), Availability.always());
        Activity activity = transit(0, 2);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(vehicle);

        List<Assignment> suggestions = suggester.suggest(expedition, catalog, List.of());

        assertEquals(List.of(new VehicleAssignment(activity.id(), vehicle.id())), suggestions);
    }

    @Test
    void proposesInstrumentAndOperatorForMeasurement() {
        Certification certification = new Certification(UUID.randomUUID(), "Operator");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Instrument meter = new Instrument(UUID.randomUUID(), "pH meter", Availability.always());
        Activity activity = measurement(certification.id(), 0, 3);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(meter);

        List<Assignment> suggestions = suggester.suggest(expedition, catalog, List.of());

        assertEquals(
                List.of(
                        new InstrumentAssignment(activity.id(), meter.id()),
                        new PersonAssignment(activity.id(), ada.id())
                ),
                suggestions
        );
    }

    @Test
    void doesNotProposeUnavailablePerson() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        TimePeriod morning = window(0, 4);
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), new Availability(List.of(morning)));
        Activity activity = sampling(certification.id(), 4, 8);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);

        List<Assignment> suggestions = suggester.suggest(expedition, catalog, List.of());

        assertTrue(suggestions.isEmpty());
    }

    @Test
    void applyingSuggestionsFillsSamplingRequirements() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity activity = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(activity);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);

        suggester.suggest(expedition, catalog, List.of()).forEach(expedition::addAssignment);

        assertEquals(List.of(new PersonAssignment(activity.id(), ada.id())), expedition.assignments());
    }

    private static Expedition draftWith(Activity activity) {
        Expedition expedition = Expedition.draft(
                UUID.randomUUID(),
                List.of(new Objective("Map wetland biodiversity")),
                week(),
                List.of(DELTA),
                List.of(UUID.randomUUID()),
                List.of(new Restriction("No night work"))
        );
        expedition.addActivity(activity);
        return expedition;
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

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static TimePeriod week() {
        return new TimePeriod(DAY, DAY.plusSeconds(86_400L * 5));
    }
}
