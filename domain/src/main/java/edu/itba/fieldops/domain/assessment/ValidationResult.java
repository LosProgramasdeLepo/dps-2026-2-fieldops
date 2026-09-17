package edu.itba.fieldops.domain.assessment;

import java.util.List;

public record ValidationResult(List<ValidationIssue> issues) {
    public ValidationResult {
        issues = List.copyOf(issues);
    }

    public static ValidationResult empty() {
        return new ValidationResult(List.of());
    }

    public boolean hasCritical() {
        return issues.stream().anyMatch(ValidationIssue::isCritical);
    }

    public List<ValidationIssue> warnings() {
        return issues.stream().filter(ValidationIssue::isWarning).toList();
    }
}
