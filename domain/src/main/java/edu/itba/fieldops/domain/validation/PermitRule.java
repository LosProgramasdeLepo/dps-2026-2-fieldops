package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.itinerary.Activity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class PermitRule implements ValidationRule {
    @Override
    public List<ValidationIssue> check(ValidationContext context) {
        Expedition expedition = context.expedition();
        Catalog catalog = context.catalog();
        List<AttachedPermit> attached = expedition.permits().stream()
                .map(permitId -> new AttachedPermit(permitId, catalog.permit(permitId)))
                .toList();
        List<Permit> known = attached.stream()
                .map(AttachedPermit::permit)
                .flatMap(Optional::stream)
                .toList();
        Stream<ValidationIssue> unknown = attached.stream()
                .filter(attachedPermit -> attachedPermit.permit().isEmpty())
                .map(attachedPermit -> issue("RESOURCE", "unknown permit " + attachedPermit.id()));
        Stream<ValidationIssue> uncovered = known.isEmpty() && !attached.isEmpty()
                ? Stream.empty()
                : expedition.itinerary().stream()
                        .filter(activity -> known.stream().noneMatch(permit -> permit.covers(activity.zone(), activity.window())))
                        .map(PermitRule::uncovered);
        return Stream.concat(unknown, uncovered).toList();
    }

    private static ValidationIssue uncovered(Activity activity) {
        return issue(
                "PERMIT",
                "activity " + activity.name()
                        + " in zone " + activity.zone().name()
                        + " during " + activity.window().start()
                        + "/" + activity.window().end()
                        + " is not covered by attached permits"
        );
    }

    private static ValidationIssue issue(String code, String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, code, message);
    }

    private record AttachedPermit(UUID id, Optional<Permit> permit) {
    }
}
