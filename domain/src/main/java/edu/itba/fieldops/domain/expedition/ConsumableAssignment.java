package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.Quantity;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ConsumableAssignment(UUID activityId, UUID consumableId, Quantity quantity) implements Assignment {
    public ConsumableAssignment {
        Objects.requireNonNull(activityId, "activity id");
        Objects.requireNonNull(consumableId, "consumable id");
        Objects.requireNonNull(quantity, "quantity");
    }

    @Override
    public Map<UUID, Quantity> consumption() {
        return Map.of(consumableId, quantity);
    }
}
