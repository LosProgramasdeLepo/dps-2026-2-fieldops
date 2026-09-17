package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;
import java.util.UUID;

public final class Vehicle {
    private final UUID id;
    private final Quantity capacity;
    private final Availability availability;

    public Vehicle(UUID id, Quantity capacity, Availability availability) {
        this.id = Objects.requireNonNull(id, "vehicle id");
        this.capacity = Objects.requireNonNull(capacity, "capacity");
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public UUID id() {
        return id;
    }

    public Quantity capacity() {
        return capacity;
    }

    public boolean availableDuring(TimePeriod period) {
        return availability.covers(period);
    }
}
