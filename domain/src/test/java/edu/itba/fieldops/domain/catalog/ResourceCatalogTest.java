package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCatalogTest {
    @Test
    void storesAndFindsEachResourceType() {
        ResourceCatalog catalog = new ResourceCatalog();
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(), Availability.always());
        Vehicle vehicle = new Vehicle(UUID.randomUUID(), new Quantity(4), Availability.always());
        Instrument instrument = new Instrument(UUID.randomUUID(), "pH meter", Availability.always());
        Consumable vials = new Consumable(UUID.randomUUID(), "vials", new Quantity(20));
        Permit permit = new Permit(
                UUID.randomUUID(),
                new WorkZone("Delta"),
                new TimePeriod(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-08T00:00:00Z"))
        );

        catalog.add(person);
        catalog.add(vehicle);
        catalog.add(instrument);
        catalog.add(vials);
        catalog.add(permit);

        assertEquals(person, catalog.person(person.id()).orElseThrow());
        assertEquals(vehicle, catalog.vehicle(vehicle.id()).orElseThrow());
        assertEquals(instrument, catalog.instrument(instrument.id()).orElseThrow());
        assertEquals(vials, catalog.consumable(vials.id()).orElseThrow());
        assertEquals(permit, catalog.permit(permit.id()).orElseThrow());
        assertTrue(catalog.person(UUID.randomUUID()).isEmpty());
        assertEquals(List.of(person), catalog.people());
        assertEquals(List.of(vehicle), catalog.vehicles());
        assertEquals(List.of(instrument), catalog.instruments());
    }

    @Test
    void rejectsDuplicateId() {
        ResourceCatalog catalog = new ResourceCatalog();
        Person person = new Person(UUID.randomUUID(), "Ada", List.of(), Availability.always());
        catalog.add(person);

        assertThrows(IllegalArgumentException.class, () -> catalog.add(person));
    }
}
