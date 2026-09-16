package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.Expedition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ExpeditionValidator {
    private ExpeditionValidator() {
    }

    public static ValidationResult validate(Expedition expedition, ResourceCatalog catalog, List<Expedition> others) {
        Objects.requireNonNull(expedition, "expedition");
        Objects.requireNonNull(catalog, "catalog");
        List<Expedition> occupying = expedition.occupyingPeers(others);
        return new ValidationResult(Stream.of(
                MissingResourceRule.check(expedition, catalog),
                TemporalOverlapRule.check(expedition, catalog, occupying),
                StockRule.check(expedition, catalog, occupying),
                CertificationRule.check(expedition, catalog),
                CapacityRule.check(expedition, catalog),
                PermitRule.check(expedition, catalog)
        ).flatMap(List::stream).toList());
    }
}
