package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class TemporalOverlapRule {
    public static List<ValidationIssue> check(Expedition expedition, ResourceCatalog catalog, List<Expedition> occupying) {
        List<Booking> own = bookings(expedition);
        Stream<ValidationIssue> unavailable = own.stream()
                .filter(booking -> !available(catalog, booking))
                .map(booking -> critical(
                        "AVAILABILITY",
                        booking.label() + " is not available during "
                                + booking.window().start() + "/" + booking.window().end()
                ));
        Stream<ValidationIssue> intra = conflicts(own, own, true);
        Stream<ValidationIssue> inter = occupying.stream()
                .map(TemporalOverlapRule::bookings)
                .flatMap(other -> conflicts(own, other, false));
        return Stream.of(unavailable, intra, inter).flatMap(issues -> issues).toList();
    }

    private static Stream<ValidationIssue> conflicts(List<Booking> left, List<Booking> right, boolean intra) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int i = 0; i < left.size(); i++) {
            for (int j = intra ? i + 1 : 0; j < right.size(); j++) {
                Booking first = left.get(i);
                Booking second = right.get(j);
                if (first.kind() == second.kind()
                        && first.resourceId().equals(second.resourceId())
                        && first.window().overlaps(second.window())) {
                    issues.add(critical(
                            "OVERLAP",
                            first.label() + " overlaps activities " + first.activityId() + " and " + second.activityId()
                    ));
                }
            }
        }
        return issues.stream();
    }

    private static boolean available(ResourceCatalog catalog, Booking booking) {
        return switch (booking.kind()) {
            case PERSON -> catalog.person(booking.resourceId())
                    .map(person -> person.availableDuring(booking.window()))
                    .orElse(true);
            case VEHICLE -> catalog.vehicle(booking.resourceId())
                    .map(vehicle -> vehicle.availableDuring(booking.window()))
                    .orElse(true);
            case INSTRUMENT -> catalog.instrument(booking.resourceId())
                    .map(instrument -> instrument.availableDuring(booking.window()))
                    .orElse(true);
        };
    }

    private static List<Booking> bookings(Expedition expedition) {
        return expedition.assignments().stream()
                .map(assignment -> booking(expedition, assignment))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<Booking> booking(Expedition expedition, Assignment assignment) {
        TimePeriod window = expedition.activityOf(assignment.activityId()).window();
        return switch (assignment) {
            case PersonAssignment person -> Optional.of(
                    new Booking(Kind.PERSON, person.personId(), person.activityId(), window)
            );
            case VehicleAssignment vehicle -> Optional.of(
                    new Booking(Kind.VEHICLE, vehicle.vehicleId(), vehicle.activityId(), window)
            );
            case InstrumentAssignment instrument -> Optional.of(
                    new Booking(Kind.INSTRUMENT, instrument.instrumentId(), instrument.activityId(), window)
            );
            case ConsumableAssignment _ -> Optional.empty();
        };
    }

    private static ValidationIssue critical(String code, String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, code, message);
    }

    private enum Kind { PERSON, VEHICLE, INSTRUMENT }

    private record Booking(Kind kind, UUID resourceId, UUID activityId, TimePeriod window) {
        String label() {
            return kind.name().toLowerCase() + " " + resourceId;
        }
    }
}
