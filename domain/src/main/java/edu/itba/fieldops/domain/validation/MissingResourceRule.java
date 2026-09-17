package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.ConsumableAssignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.InstrumentAssignment;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.itinerary.Activity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

final class MissingResourceRule {
    private MissingResourceRule() {
    }

    static List<ValidationIssue> check(Expedition expedition, ResourceCatalog catalog) {
        Stream<ValidationIssue> unknown = expedition.assignments().stream()
                .map(assignment -> unknown(catalog, assignment))
                .flatMap(Optional::stream);
        Stream<ValidationIssue> required = expedition.itinerary().stream()
                .flatMap(activity -> missingRequired(expedition, activity));
        return Stream.concat(unknown, required).toList();
    }

    private static Optional<ValidationIssue> unknown(ResourceCatalog catalog, Assignment assignment) {
        return switch (assignment) {
            case PersonAssignment person -> unknownIfAbsent(catalog.person(person.personId()), "person", person.personId());
            case VehicleAssignment vehicle -> unknownIfAbsent(catalog.vehicle(vehicle.vehicleId()), "vehicle", vehicle.vehicleId());
            case InstrumentAssignment instrument -> unknownIfAbsent(catalog.instrument(instrument.instrumentId()), "instrument", instrument.instrumentId());
            case ConsumableAssignment consumable -> unknownIfAbsent(catalog.consumable(consumable.consumableId()), "consumable", consumable.consumableId());
        };
    }

    private static Optional<ValidationIssue> unknownIfAbsent(Optional<?> found, String type, UUID id) {
        return found.isEmpty() ? Optional.of(critical("unknown " + type + " " + id)) : Optional.empty();
    }

    private static Stream<ValidationIssue> missingRequired(Expedition expedition, Activity activity) {
        return Stream.of(
                issueIf(activity.requirements().needsVehicle() && noneAssigned(expedition, activity.id(), VehicleAssignment.class),
                        "activity " + activity.name() + " requires a vehicle and has none assigned"),
                issueIf(activity.requirements().needsInstrument() && noneAssigned(expedition, activity.id(), InstrumentAssignment.class),
                        "activity " + activity.name() + " requires an instrument and has none assigned"),
                issueIf(!activity.requirements().certifications().isEmpty()
                                && noneAssigned(expedition, activity.id(), PersonAssignment.class),
                        "activity " + activity.name() + " requires certified personnel and has none assigned")
        ).flatMap(issues -> issues);
    }

    private static Stream<ValidationIssue> issueIf(boolean missing, String message) {
        return missing ? Stream.of(critical(message)) : Stream.empty();
    }

    private static boolean noneAssigned(Expedition expedition, UUID activityId, Class<? extends Assignment> type) {
        return expedition.assignmentsOf(activityId).stream().noneMatch(type::isInstance);
    }

    private static ValidationIssue critical(String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, "RESOURCE", message);
    }
}
