package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.TemporalBooking;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class TemporalOverlapRule {
    public static List<ValidationIssue> check(Expedition expedition, ResourceCatalog catalog, List<Expedition> occupying) {
        List<TemporalBooking> own = TemporalBooking.of(expedition);
        Stream<ValidationIssue> unavailable = own.stream()
                .filter(booking -> !available(catalog, booking))
                .map(booking -> critical(
                        "AVAILABILITY",
                        booking.label() + " is not available during "
                                + booking.window().start() + "/" + booking.window().end()
                ));
        Stream<ValidationIssue> intra = conflicts(own, own, true);
        Stream<ValidationIssue> inter = occupying.stream()
                .map(TemporalBooking::of)
                .flatMap(other -> conflicts(own, other, false));
        return Stream.of(unavailable, intra, inter).flatMap(issues -> issues).toList();
    }

    private static Stream<ValidationIssue> conflicts(
            List<TemporalBooking> left,
            List<TemporalBooking> right,
            boolean intra
    ) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int i = 0; i < left.size(); i++) {
            for (int j = intra ? i + 1 : 0; j < right.size(); j++) {
                TemporalBooking first = left.get(i);
                TemporalBooking second = right.get(j);
                if (first.conflicts(second)) {
                    issues.add(critical(
                            "OVERLAP",
                            first.label() + " overlaps activities " + first.activityId() + " and " + second.activityId()
                    ));
                }
            }
        }
        return issues.stream();
    }

    private static boolean available(ResourceCatalog catalog, TemporalBooking booking) {
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

    private static ValidationIssue critical(String code, String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, code, message);
    }
}
