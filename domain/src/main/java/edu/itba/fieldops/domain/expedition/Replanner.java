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

    public void cancel(Expedition expedition, UUID activityId, Catalog catalog, List<Expedition> others) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        ensureDraft(expedition);
        expedition.removeActivity(activityId);
        refill(expedition, catalog, others);
    }

    public void delay(
            Expedition expedition,
            UUID activityId,
            Duration delay,
            Catalog catalog,
            List<Expedition> others
    ) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        ensureDraft(expedition);
        expedition.delay(activityId, delay);
        dropInvalid(expedition, catalog, others);
        refill(expedition, catalog, others);
    }

    public void replaceUnavailable(Expedition expedition, Catalog catalog, List<Expedition> others) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        dropInvalid(expedition, catalog, others);
        refill(expedition, catalog, others);
    }

    private static void ensureDraft(Expedition expedition) {
        if (expedition.status() == ExpeditionStatus.DRAFT) {
            return;
        }
        expedition.returnToDraft();
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
