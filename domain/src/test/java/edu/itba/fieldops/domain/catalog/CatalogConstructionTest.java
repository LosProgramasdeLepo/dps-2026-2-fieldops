package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogConstructionTest {
    private static final TimePeriod WEEK = new TimePeriod(
            Instant.parse("2026-11-01T00:00:00Z"),
            Instant.parse("2026-11-08T00:00:00Z")
    );

    @Test
    void personHoldsCertificationAndAvailability() {
        Certification sampling = new Certification(UUID.randomUUID(), "Sampling");
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(sampling), Availability.always());

        assertAll(
                () -> assertTrue(person.holds(sampling.id())),
                () -> assertFalse(person.holds(UUID.randomUUID())),
                () -> assertTrue(person.availableDuring(WEEK))
        );
    }

    @Test
    void consumableComparesAgainstStock() {
        Consumable vials = new Consumable(UUID.randomUUID(), "vials", new Quantity(20));

        assertAll(
                () -> assertTrue(vials.hasAtLeast(new Quantity(20))),
                () -> assertFalse(vials.hasAtLeast(new Quantity(21)))
        );
    }

    @Test
    void permitCoversMatchingZoneAndWindow() {
        Permit permit = new Permit(UUID.randomUUID(), new WorkZone("Delta"), WEEK);

        assertAll(
                () -> assertTrue(permit.covers(new WorkZone("Delta"), WEEK)),
                () -> assertFalse(permit.covers(new WorkZone("Coast"), WEEK))
        );
    }

    @Test
    void availabilityCoversWhenAPeriodContainsTheWindow() {
        Availability availability = new Availability(List.of(WEEK));
        TimePeriod december = new TimePeriod(
                Instant.parse("2026-12-01T00:00:00Z"),
                Instant.parse("2026-12-02T00:00:00Z")
        );

        assertAll(
                () -> assertTrue(availability.covers(WEEK)),
                () -> assertFalse(availability.covers(december))
        );
    }
}
