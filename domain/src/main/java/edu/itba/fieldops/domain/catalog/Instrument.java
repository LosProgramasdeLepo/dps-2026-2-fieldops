package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Texts;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.Objects;
import java.util.UUID;

public final class Instrument {
    private final UUID id;
    private final String kind;
    private final Availability availability;

    public Instrument(UUID id, String kind, Availability availability) {
        this.id = Objects.requireNonNull(id, "instrument id");
        this.kind = Texts.required(kind, "instrument kind");
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public UUID id() {
        return id;
    }

    public String kind() {
        return kind;
    }

    public boolean availableDuring(TimePeriod period) {
        return availability.covers(period);
    }
}
