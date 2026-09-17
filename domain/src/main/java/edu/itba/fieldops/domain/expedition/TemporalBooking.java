package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TemporalBooking(Kind kind, UUID resourceId, UUID activityId, TimePeriod window) {
    public TemporalBooking {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(resourceId, "resource id");
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(window, "window");
    }

    public static Optional<TemporalBooking> of(Assignment assignment, TimePeriod window) {
        return switch (assignment) {
            case PersonAssignment person -> Optional.of(
                    new TemporalBooking(Kind.PERSON, person.personId(), person.activityId(), window)
            );
            case VehicleAssignment vehicle -> Optional.of(
                    new TemporalBooking(Kind.VEHICLE, vehicle.vehicleId(), vehicle.activityId(), window)
            );
            case InstrumentAssignment instrument -> Optional.of(
                    new TemporalBooking(Kind.INSTRUMENT, instrument.instrumentId(), instrument.activityId(), window)
            );
            case ConsumableAssignment _ -> Optional.empty();
        };
    }

    public static List<TemporalBooking> of(Expedition expedition) {
        return expedition.assignments().stream()
                .map(assignment -> of(assignment, expedition.activityOf(assignment.activityId()).window()))
                .flatMap(Optional::stream)
                .toList();
    }

    public boolean availableIn(ResourceCatalog catalog) {
        return presence(catalog) == Presence.AVAILABLE;
    }

    public boolean unavailableIn(ResourceCatalog catalog) {
        return presence(catalog) == Presence.UNAVAILABLE;
    }

    private Presence presence(ResourceCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        return switch (kind) {
            case PERSON -> catalog.person(resourceId)
                    .map(person -> person.availableDuring(window) ? Presence.AVAILABLE : Presence.UNAVAILABLE)
                    .orElse(Presence.UNKNOWN);
            case VEHICLE -> catalog.vehicle(resourceId)
                    .map(vehicle -> vehicle.availableDuring(window) ? Presence.AVAILABLE : Presence.UNAVAILABLE)
                    .orElse(Presence.UNKNOWN);
            case INSTRUMENT -> catalog.instrument(resourceId)
                    .map(instrument -> instrument.availableDuring(window) ? Presence.AVAILABLE : Presence.UNAVAILABLE)
                    .orElse(Presence.UNKNOWN);
        };
    }

    public boolean conflicts(TemporalBooking other) {
        return conflicts(other.kind, other.resourceId, other.window);
    }

    boolean conflicts(Kind otherKind, UUID otherId, TimePeriod otherWindow) {
        return kind == otherKind && resourceId.equals(otherId) && window.overlaps(otherWindow);
    }

    public String label() {
        return kind.name().toLowerCase() + " " + resourceId;
    }

    public enum Kind { PERSON, VEHICLE, INSTRUMENT }

    private enum Presence { UNKNOWN, AVAILABLE, UNAVAILABLE }
}
