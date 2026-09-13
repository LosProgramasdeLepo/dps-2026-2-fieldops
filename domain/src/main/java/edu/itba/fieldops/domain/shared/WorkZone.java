package edu.itba.fieldops.domain.shared;

public record WorkZone(String name) {
    public WorkZone {
        name = Texts.required(name, "zone name");
    }
}
