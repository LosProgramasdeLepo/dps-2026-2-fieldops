package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.ResourceCatalog;
import edu.itba.fieldops.domain.expedition.Assignment;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.shared.Quantity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class StockRule {
    private StockRule() {
    }

    static List<ValidationIssue> check(Expedition expedition, ResourceCatalog catalog, List<Expedition> occupying) {
        Map<UUID, Quantity> needed = Stream.concat(Stream.of(expedition), occupying.stream())
                .map(Expedition::assignments)
                .flatMap(List::stream)
                .map(Assignment::consumption)
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Quantity::plus));
        return needed.entrySet().stream()
                .map(entry -> catalog.consumable(entry.getKey())
                        .filter(consumable -> !consumable.hasAtLeast(entry.getValue()))
                        .map(consumable -> stockIssue(consumable, entry.getValue())))
                .flatMap(Optional::stream)
                .toList();
    }

    private static ValidationIssue stockIssue(Consumable consumable, Quantity needed) {
        return new ValidationIssue(
                IssueSeverity.CRITICAL,
                "STOCK",
                "consumable " + consumable.name()
                        + " stock " + consumable.stock().value()
                        + " is less than assigned " + needed.value()
        );
    }
}
