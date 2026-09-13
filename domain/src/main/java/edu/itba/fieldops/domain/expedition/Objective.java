package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.Texts;

public record Objective(String text) {
    public Objective {
        text = Texts.required(text, "objective");
    }
}
