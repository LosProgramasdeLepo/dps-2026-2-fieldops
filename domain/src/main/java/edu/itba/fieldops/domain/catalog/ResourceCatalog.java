package edu.itba.fieldops.domain.catalog;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ResourceCatalog {
    private final Map<UUID, Person> people = new HashMap<>();
    private final Map<UUID, Vehicle> vehicles = new HashMap<>();
    private final Map<UUID, Instrument> instruments = new HashMap<>();
    private final Map<UUID, Consumable> consumables = new HashMap<>();
    private final Map<UUID, Permit> permits = new HashMap<>();

    public void add(Person person) {
        put(people, person.id(), person, "person");
    }

    public void add(Vehicle vehicle) {
        put(vehicles, vehicle.id(), vehicle, "vehicle");
    }

    public void add(Instrument instrument) {
        put(instruments, instrument.id(), instrument, "instrument");
    }

    public void add(Consumable consumable) {
        put(consumables, consumable.id(), consumable, "consumable");
    }

    public void add(Permit permit) {
        put(permits, permit.id(), permit, "permit");
    }

    public Optional<Person> person(UUID id) {
        return find(people, id);
    }

    public Optional<Vehicle> vehicle(UUID id) {
        return find(vehicles, id);
    }

    public Optional<Instrument> instrument(UUID id) {
        return find(instruments, id);
    }

    public Optional<Consumable> consumable(UUID id) {
        return find(consumables, id);
    }

    public Optional<Permit> permit(UUID id) {
        return find(permits, id);
    }

    private static <T> void put(Map<UUID, T> items, UUID id, T value, String type) {
        Objects.requireNonNull(value, type);
        if (items.putIfAbsent(id, value) != null) {
            throw new IllegalArgumentException("duplicate " + type + ": " + id);
        }
    }

    private static <T> Optional<T> find(Map<UUID, T> items, UUID id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(items.get(id));
    }
}
