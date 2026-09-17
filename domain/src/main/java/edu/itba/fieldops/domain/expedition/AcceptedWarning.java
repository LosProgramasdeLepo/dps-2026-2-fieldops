package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.shared.Texts;
import edu.itba.fieldops.domain.assessment.ValidationIssue;

import java.util.Objects;
import java.util.UUID;

public record AcceptedWarning(ValidationIssue issue, String justification, UUID acceptedBy) {
    public AcceptedWarning {
        Objects.requireNonNull(issue, "accepted issue");
        if (issue.isCritical()) {
            throw new IllegalArgumentException("critical issues cannot be accepted as warnings");
        }
        justification = Texts.required(justification, "justification");
        Objects.requireNonNull(acceptedBy, "accepted by");
    }
}
