package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Availability;
import edu.itba.fieldops.domain.catalog.Certification;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.SamplingPolicy;
import edu.itba.fieldops.domain.itinerary.TransitPolicy;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplannerTest {
    private static final Instant DAY = Instant.parse("2026-11-01T08:00:00Z");
    private static final WorkZone DELTA = new WorkZone("Delta");

    @Test
    void cancelRemovesActivityAndFillsRemainingGaps() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity sample = sampling(certification.id(), 0, 4);
        Activity ride = transit(4, 6);
        Expedition expedition = draftWith(sample);
        expedition.addActivity(ride);
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);

        Replanner.cancel(expedition, ride.id(), catalog, List.of());

        assertEquals(List.of(sample.id()), expedition.itinerary().stream().map(Activity::id).toList());
        assertEquals(List.of(new PersonAssignment(sample.id(), ada.id())), expedition.assignments());
    }

    @Test
    void replaceUnavailableDropsOccupiedPersonAndSuggestsTheNext() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Person bob = new Person(UUID.randomUUID(), "Bob", List.of(certification), Availability.always());
        Activity occupied = sampling(certification.id(), 0, 4);
        Expedition occupying = draftWith(occupied);
        occupying.addAssignment(new PersonAssignment(occupied.id(), ada.id()));
        occupying.submitForReview();
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(bob);

        Replanner.replaceUnavailable(expedition, catalog, List.of(occupying));

        assertEquals(List.of(new PersonAssignment(sample.id(), bob.id())), expedition.assignments());
    }

    @Test
    void delayShiftsDependentActivityAndKeepsValidAssignment() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Activity first = sampling(certification.id(), 0, 4);
        Activity second = sampling(certification.id(), 4, 8);
        Expedition expedition = draftWith(first);
        expedition.addActivity(second);
        expedition.addDependency(second.id(), first.id());
        expedition.addAssignment(new PersonAssignment(first.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);

        Replanner.delay(expedition, first.id(), Duration.ofHours(2), catalog, List.of());

        assertEquals(window(2, 6), expedition.activityOf(first.id()).window());
        assertEquals(window(6, 10), expedition.activityOf(second.id()).window());
        assertEquals(
                List.of(
                        new PersonAssignment(first.id(), ada.id()),
                        new PersonAssignment(second.id(), ada.id())
                ),
                expedition.assignments()
        );
    }

    @Test
    void delayReplacesPersonWhoWouldOverlapAnOccupyingExpedition() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Person ada = new Person(UUID.randomUUID(), "Ada", List.of(certification), Availability.always());
        Person bob = new Person(UUID.randomUUID(), "Bob", List.of(certification), Availability.always());
        Activity occupied = sampling(certification.id(), 4, 8);
        Expedition occupying = draftWith(occupied);
        occupying.addAssignment(new PersonAssignment(occupied.id(), ada.id()));
        occupying.submitForReview();
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        expedition.addAssignment(new PersonAssignment(sample.id(), ada.id()));
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.add(ada);
        catalog.add(bob);

        Replanner.delay(expedition, sample.id(), Duration.ofHours(4), catalog, List.of(occupying));

        assertEquals(window(4, 8), expedition.activityOf(sample.id()).window());
        assertEquals(List.of(new PersonAssignment(sample.id(), bob.id())), expedition.assignments());
    }

    @Test
    void delayOutsideExpeditionPeriodIsRejected() {
        Certification certification = new Certification(UUID.randomUUID(), "Sampling");
        Activity sample = sampling(certification.id(), 0, 4);
        Expedition expedition = draftWith(sample);
        ResourceCatalog catalog = new ResourceCatalog();

        assertThrows(
                IllegalArgumentException.class,
                () -> Replanner.delay(expedition, sample.id(), Duration.ofDays(10), catalog, List.of())
        );
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

    private static TimePeriod window(int fromHour, int toHour) {
        return new TimePeriod(DAY.plusSeconds(fromHour * 3600L), DAY.plusSeconds(toHour * 3600L));
    }

    private static TimePeriod week() {
        return new TimePeriod(DAY, DAY.plusSeconds(86_400L * 5));
    }
}
