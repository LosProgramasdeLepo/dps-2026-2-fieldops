package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.itinerary.Activity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CertificationRule {
    private CertificationRule() {
    }

    public static List<ValidationIssue> check(Expedition expedition, ResourceCatalog catalog) {
        return expedition.itinerary().stream()
                .flatMap(activity -> activity.requirements().certifications().stream()
                        .filter(certificationId -> uncertified(expedition, catalog, activity.id(), certificationId))
                        .map(certificationId -> issue(activity, certificationId)))
                .toList();
    }

    private static boolean uncertified(
            Expedition expedition,
            ResourceCatalog catalog,
            UUID activityId,
            UUID certificationId
    ) {
        boolean unknown = false;
        boolean known = false;
        for (Assignment assignment : expedition.assignmentsOf(activityId)) {
            if (!(assignment instanceof PersonAssignment person)) {
                continue;
            }
            Optional<Person> found = catalog.person(person.personId());
            if (found.isEmpty()) {
                unknown = true;
                continue;
            }
            known = true;
            if (found.get().holds(certificationId)) {
                return false;
            }
        }
        return known || !unknown;
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
