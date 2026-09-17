package edu.itba.fieldops.domain.assessment;

import edu.itba.fieldops.domain.shared.Texts;

import java.util.Objects;

public record ValidationIssue(IssueSeverity severity, String code, String message) {
    public ValidationIssue {
        Objects.requireNonNull(severity, "severity");
        code = Texts.required(code, "issue code");
        message = Texts.required(message, "issue message");
    }

    public boolean isCritical() {
        return severity == IssueSeverity.CRITICAL;
    }

    public boolean isWarning() {
        return severity == IssueSeverity.WARNING;
    }
}
