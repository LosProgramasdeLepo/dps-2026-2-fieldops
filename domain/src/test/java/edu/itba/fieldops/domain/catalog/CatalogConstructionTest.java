package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogConstructionTest {
    private static final TimePeriod WEEK = new TimePeriod(
            Instant.parse("2026-11-01T00:00:00Z"),
            Instant.parse("2026-11-08T00:00:00Z")
    );

    @Test
    void personHoldsCertificationsAndAvailability() {
        Certification sampling = new Certification(UUID.randomUUID(), "Sampling");
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(sampling), Availability.always());

        assertTrue(person.holds(sampling));
        assertTrue(person.availableDuring(WEEK));
    }

    @Test
    void vehicleInstrumentAndConsumableHaveOwnBehavior() {
        Vehicle vehicle = new Vehicle(UUID.randomUUID(), new Quantity(4), Availability.always());
        Instrument instrument = new Instrument(UUID.randomUUID(), "pH meter", Availability.always());
        Consumable vials = new Consumable(UUID.randomUUID(), "vials", new Quantity(20));

        assertTrue(vehicle.canCarry(new Quantity(4)));
        assertTrue(instrument.availableDuring(WEEK));
        assertTrue(vials.hasAtLeast(new Quantity(20)));
    }

    @Test
    void permitCoversZoneByName() {
        Permit permit = new Permit(UUID.randomUUID(), new WorkZone("Delta", "Paraná"), WEEK);

        assertTrue(permit.covers(new WorkZone("Delta"), WEEK));
    }

    @Test
    void availabilityCoversWhenAPeriodContainsTheWindow() {
        Availability availability = new Availability(List.of(WEEK));

        assertTrue(availability.covers(WEEK));
    }
}
