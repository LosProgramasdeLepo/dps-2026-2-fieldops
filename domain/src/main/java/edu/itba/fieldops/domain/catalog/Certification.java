package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.Texts;

import java.util.Objects;
import java.util.UUID;

public record Certification(UUID id, String name) {
    public Certification {
        Objects.requireNonNull(id, "certification id");
        name = Texts.required(name, "certification name");
    }
}
