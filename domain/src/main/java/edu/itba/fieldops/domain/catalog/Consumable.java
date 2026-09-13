package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.Texts;

import java.util.Objects;
import java.util.UUID;

public final class Consumable {
    private final UUID id;
    private final String name;
    private final Quantity stock;

    public Consumable(UUID id, String name, Quantity stock) {
        this.id = Objects.requireNonNull(id, "consumable id");
        this.name = Texts.required(name, "consumable name");
        this.stock = Objects.requireNonNull(stock, "stock");
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Quantity stock() {
        return stock;
    }

    public boolean hasAtLeast(Quantity needed) {
        return stock.isAtLeast(needed);
    }
}
