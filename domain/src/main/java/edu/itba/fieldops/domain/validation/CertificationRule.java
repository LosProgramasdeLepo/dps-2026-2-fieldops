package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.itinerary.Activity;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public final class CertificationRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(ValidationContext context) {
        Expedition expedition = context.expedition();
        Catalog catalog = context.catalog();
        return expedition.itinerary().stream()
                .flatMap(activity -> activity.requirements().certifications().stream()
                        .filter(certificationId -> uncertified(expedition, catalog, activity.id(), certificationId))
                        .map(certificationId -> issue(activity, certificationId)))
                .toList();
    }

    private static boolean uncertified(
            Expedition expedition,
            Catalog catalog,
            UUID activityId,
            UUID certificationId
    ) {
        List<Person> known = expedition.assignmentsOf(activityId).stream()
                .flatMap(assignment -> assignment instanceof PersonAssignment person
                        ? catalog.person(person.personId()).stream()
                        : Stream.empty())
                .toList();
        return !known.isEmpty() && known.stream().noneMatch(person -> person.holds(certificationId));
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
