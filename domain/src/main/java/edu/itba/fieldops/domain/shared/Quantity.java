package edu.itba.fieldops.domain.shared;

public record Quantity(int value) {
    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("quantity must not be negative: " + value);
        }
    }

    public Quantity plus(Quantity other) {
        return new Quantity(value + other.value);
    }

    public boolean isAtLeast(Quantity other) {
        return value >= other.value;
    }
}
