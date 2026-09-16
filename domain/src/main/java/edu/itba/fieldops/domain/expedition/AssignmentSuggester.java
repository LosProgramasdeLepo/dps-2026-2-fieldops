package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.TimePeriod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public final class AssignmentSuggester {
    public static List<Assignment> suggest(Expedition expedition, ResourceCatalog catalog, List<Expedition> others) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        List<TemporalBooking> taken = new ArrayList<>();
        taken.addAll(TemporalBooking.of(expedition));
        for (Expedition peer : expedition.occupyingPeers(others)) {
            taken.addAll(TemporalBooking.of(peer));
        }
        List<Assignment> suggestions = new ArrayList<>();
        for (Activity activity : expedition.itinerary()) {
            fillGaps(activity, expedition, catalog, taken, suggestions);
        }
        return List.copyOf(suggestions);
    }

    private static void fillGaps(
            Activity activity,
            Expedition expedition,
            ResourceCatalog catalog,
            List<TemporalBooking> taken,
            List<Assignment> suggestions
    ) {
        TimePeriod window = activity.window();
        List<Assignment> current = new ArrayList<>(expedition.assignmentsOf(activity.id()));
        if (activity.requirements().needsVehicle() && none(current, VehicleAssignment.class)) {
            catalog.vehicles().stream()
                    .filter(vehicle -> vehicle.availableDuring(window))
                    .filter(vehicle -> free(taken, TemporalBooking.Kind.VEHICLE, vehicle.id(), window))
                    .findFirst()
                    .ifPresent(vehicle -> take(suggestions, current, taken, new VehicleAssignment(activity.id(), vehicle.id()), window));
        }
        if (activity.requirements().needsInstrument() && none(current, InstrumentAssignment.class)) {
            catalog.instruments().stream()
                    .filter(instrument -> instrument.availableDuring(window))
                    .filter(instrument -> free(taken, TemporalBooking.Kind.INSTRUMENT, instrument.id(), window))
                    .findFirst()
                    .ifPresent(instrument -> take(suggestions, current, taken, new InstrumentAssignment(activity.id(), instrument.id()), window));
        }
        for (UUID certificationId : activity.requirements().certifications()) {
            if (heldBy(current, catalog, certificationId)) {
                continue;
            }
            catalog.people().stream()
                    .filter(person -> person.holds(certificationId))
                    .filter(person -> person.availableDuring(window))
                    .filter(person -> free(taken, TemporalBooking.Kind.PERSON, person.id(), window))
                    .map(Person::id)
                    .findFirst()
                    .ifPresent(personId -> take(suggestions, current, taken, new PersonAssignment(activity.id(), personId), window));
        }
    }

    private static void take(
            List<Assignment> suggestions,
            List<Assignment> current,
            List<TemporalBooking> taken,
            Assignment assignment,
            TimePeriod window
    ) {
        suggestions.add(assignment);
        current.add(assignment);
        TemporalBooking.of(assignment, window).ifPresent(taken::add);
    }

    private static boolean heldBy(List<Assignment> current, ResourceCatalog catalog, UUID certificationId) {
        return current.stream()
                .flatMap(assignment -> assignment instanceof PersonAssignment person
                        ? catalog.person(person.personId()).stream()
                        : Stream.empty())
                .anyMatch(person -> person.holds(certificationId));
    }

    private static boolean none(List<Assignment> current, Class<? extends Assignment> type) {
        return current.stream().noneMatch(type::isInstance);
    }

    private static boolean free(List<TemporalBooking> taken, TemporalBooking.Kind kind, UUID resourceId, TimePeriod window) {
        return taken.stream().noneMatch(slot -> slot.conflicts(kind, resourceId, window));
    }
}
