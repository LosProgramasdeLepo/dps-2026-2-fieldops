package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.catalog.Catalog;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Replanner {
    private final AssignmentSuggester suggester;

    public Replanner(AssignmentSuggester suggester) {
        this.suggester = Objects.requireNonNull(suggester, "assignment suggester");
    }

    public Expedition cancel(Expedition expedition, UUID activityId, Catalog catalog, List<Expedition> others) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Expedition plan = editablePlanFor(expedition);
        plan.removeActivity(activityId);
        refill(plan, catalog, others);
        return plan;
    }

    public Expedition delay(
            Expedition expedition,
            UUID activityId,
            Duration delay,
            Catalog catalog,
            List<Expedition> others
    ) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Expedition plan = editablePlanFor(expedition);
        plan.delay(activityId, delay);
        dropInvalid(plan, catalog, others);
        refill(plan, catalog, others);
        return plan;
    }

    public Expedition replaceUnavailable(Expedition expedition, Catalog catalog, List<Expedition> others) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        Expedition plan = revisionOrSelf(expedition);
        dropInvalid(plan, catalog, others);
        refill(plan, catalog, others);
        return plan;
    }

    private static Expedition editablePlanFor(Expedition expedition) {
        Expedition plan = revisionOrSelf(expedition);
        if (plan.status() != ExpeditionStatus.DRAFT) {
            plan.returnToDraft();
        }
        return plan;
    }

    private static Expedition revisionOrSelf(Expedition expedition) {
        return expedition.status().hasBeenApproved() ? expedition.reviseAsDraft() : expedition;
    }

    private void refill(Expedition expedition, Catalog catalog, List<Expedition> others) {
        for (Assignment assignment : suggester.suggest(expedition, catalog, others)) {
            expedition.addAssignment(assignment);
        }
    }

    private static void dropInvalid(Expedition expedition, Catalog catalog, List<Expedition> others) {
        List<TemporalBooking> occupying = new ArrayList<>();
        for (Expedition peer : expedition.occupyingPeers(others)) {
            occupying.addAll(TemporalBooking.of(peer));
        }
        List<TemporalBooking> kept = new ArrayList<>();
        List<Assignment> drop = new ArrayList<>();
        for (Assignment assignment : expedition.assignments()) {
            Optional<TemporalBooking> booking = TemporalBooking.of(
                    assignment,
                    expedition.activityOf(assignment.activityId()).window()
            );
            if (booking.isEmpty()) {
                continue;
            }
            TemporalBooking slot = booking.get();
            boolean invalid = !slot.availableIn(catalog)
                    || occupying.stream().anyMatch(slot::conflicts)
                    || kept.stream().anyMatch(slot::conflicts);
            if (invalid) {
                drop.add(assignment);
            } else {
                kept.add(slot);
            }
        }
        for (Assignment assignment : drop) {
            expedition.removeAssignment(assignment);
        }
    }
}
