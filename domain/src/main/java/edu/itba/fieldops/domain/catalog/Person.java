package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Texts;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Person {
    private final UUID id;
    private final String name;
    private final List<Certification> certifications;
    private final Availability availability;

    public Person(UUID id, String name, List<Certification> certifications, Availability availability) {
        this.id = Objects.requireNonNull(id, "person id");
        this.name = Texts.required(name, "person name");
        this.certifications = List.copyOf(certifications);
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<Certification> certifications() {
        return certifications;
    }

    public boolean holds(UUID certificationId) {
        Objects.requireNonNull(certificationId, "certification id");
        return certifications.stream().anyMatch(held -> held.id().equals(certificationId));
    }

    public boolean availableDuring(TimePeriod period) {
        return availability.covers(period);
    }
}
