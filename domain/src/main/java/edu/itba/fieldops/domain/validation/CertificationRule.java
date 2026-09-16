package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.itinerary.Activity;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public final class CertificationRule {
    public static List<ValidationIssue> check(Expedition expedition, ResourceCatalog catalog) {
        return expedition.itinerary().stream()
                .flatMap(activity -> activity.requirements().certifications().stream()
                        .filter(certificationId -> !heldByAssignedPeople(expedition, catalog, activity.id(), certificationId))
                        .map(certificationId -> issue(activity, certificationId)))
                .toList();
    }

    private static boolean heldByAssignedPeople(
            Expedition expedition,
            ResourceCatalog catalog,
            UUID activityId,
            UUID certificationId
    ) {
        return expedition.assignmentsOf(activityId).stream()
                .flatMap(assignment -> switch (assignment) {
                    case PersonAssignment person -> catalog.person(person.personId()).stream();
                    default -> Stream.<Person>empty();
                })
                .anyMatch(person -> person.holds(certificationId));
    }

    private static ValidationIssue issue(Activity activity, UUID certificationId) {
        return new ValidationIssue(
                IssueSeverity.CRITICAL,
                "CERTIFICATION",
                "activity " + activity.name()
                        + " requires certification " + certificationId
                        + " which assigned people do not hold"
        );
    }
}
