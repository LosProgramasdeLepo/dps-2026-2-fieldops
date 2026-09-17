package edu.itba.fieldops.domain.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Catalog {
    Optional<Person> person(UUID id);

    Optional<Vehicle> vehicle(UUID id);

    Optional<Instrument> instrument(UUID id);

    Optional<Consumable> consumable(UUID id);

    Optional<Permit> permit(UUID id);

    List<Person> people();

    List<Vehicle> vehicles();

    List<Instrument> instruments();
}
