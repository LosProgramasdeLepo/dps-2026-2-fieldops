package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.Texts;

public record Restriction(String text) {
    public Restriction {
        text = Texts.required(text, "restriction");
    }
}
