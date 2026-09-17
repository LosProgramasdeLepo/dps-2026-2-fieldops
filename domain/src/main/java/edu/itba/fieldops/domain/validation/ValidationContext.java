package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.expedition.Expedition;

import java.util.List;
import java.util.Objects;

public record ValidationContext(Expedition expedition, Catalog catalog, List<Expedition> occupying) {
    public ValidationContext {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        occupying = List.copyOf(occupying);
    }

    public static ValidationContext of(Expedition expedition, Catalog catalog, List<Expedition> others) {
        Objects.requireNonNull(expedition, "expedition");
        return new ValidationContext(expedition, catalog, expedition.occupyingPeers(others));
    }
}
