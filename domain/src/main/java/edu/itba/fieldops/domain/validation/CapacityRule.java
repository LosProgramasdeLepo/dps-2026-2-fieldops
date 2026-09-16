package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.VehicleAssignment;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Quantity;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class CapacityRule {
    public static List<ValidationIssue> check(Expedition expedition, ResourceCatalog catalog) {
        return expedition.itinerary().stream()
                .map(activity -> issueFor(expedition, catalog, activity))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<ValidationIssue> issueFor(Expedition expedition, ResourceCatalog catalog, Activity activity) {
        List<Assignment> assigned = expedition.assignmentsOf(activity.id());
        boolean unknownVehicle = assigned.stream()
                .anyMatch(assignment -> assignment instanceof VehicleAssignment vehicle
                        && catalog.vehicle(vehicle.vehicleId()).isEmpty());
        if (unknownVehicle) {
            return Optional.empty();
        }
        List<Vehicle> vehicles = assigned.stream()
                .flatMap(assignment -> switch (assignment) {
                    case VehicleAssignment vehicle -> catalog.vehicle(vehicle.vehicleId()).stream();
                    default -> Stream.<Vehicle>empty();
                })
                .toList();
        if (vehicles.isEmpty()) {
            return Optional.empty();
        }
        int passengers = (int) assigned.stream()
                .filter(PersonAssignment.class::isInstance)
                .count();
        Quantity capacity = vehicles.stream()
                .map(Vehicle::capacity)
                .reduce(new Quantity(0), Quantity::plus);
        if (capacity.isAtLeast(new Quantity(passengers))) {
            return Optional.empty();
        }
        return Optional.of(new ValidationIssue(
                IssueSeverity.WARNING,
                "CAPACITY",
                "activity " + activity.name()
                        + " assigned " + passengers
                        + " people but vehicles can carry " + capacity.value()
        ));
    }
}
