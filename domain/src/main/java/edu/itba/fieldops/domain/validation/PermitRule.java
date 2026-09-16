package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.itinerary.Activity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class PermitRule {
    public static List<ValidationIssue> check(Expedition expedition, ResourceCatalog catalog) {
        List<AttachedPermit> attached = expedition.permits().stream()
                .map(permitId -> new AttachedPermit(permitId, catalog.permit(permitId)))
                .toList();
        List<Permit> known = attached.stream()
                .map(AttachedPermit::permit)
                .flatMap(Optional::stream)
                .toList();
        Stream<ValidationIssue> unknown = attached.stream()
                .filter(attachedPermit -> attachedPermit.permit().isEmpty())
                .map(attachedPermit -> critical("unknown permit " + attachedPermit.id()));
        Stream<ValidationIssue> uncovered = expedition.itinerary().stream()
                .filter(activity -> known.stream().noneMatch(permit -> permit.covers(activity.zone(), activity.window())))
                .map(PermitRule::uncovered);
        return Stream.concat(unknown, uncovered).toList();
    }

    private static ValidationIssue uncovered(Activity activity) {
        return critical(
                "activity " + activity.name()
                        + " in zone " + activity.zone().name()
                        + " during " + activity.window().start()
                        + "/" + activity.window().end()
                        + " is not covered by attached permits"
        );
    }

    private static ValidationIssue critical(String message) {
        return new ValidationIssue(IssueSeverity.CRITICAL, "PERMIT", message);
    }

    private record AttachedPermit(UUID id, Optional<Permit> permit) {
    }
}
