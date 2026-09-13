package edu.itba.fieldops.domain.shared;

public final class Texts {
    private Texts() {
    }

    public static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
