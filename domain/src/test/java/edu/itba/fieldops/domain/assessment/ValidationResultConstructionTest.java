package edu.itba.fieldops.domain.assessment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultConstructionTest {
    @Test
    void distinguishesCriticalIssuesFromWarnings() {
        ValidationIssue critical = new ValidationIssue(IssueSeverity.CRITICAL, "PERMIT", "missing permit");
        ValidationIssue warning = new ValidationIssue(IssueSeverity.WARNING, "RISK", "high risk window");
        ValidationResult result = new ValidationResult(List.of(critical, warning));

        assertAll(
                () -> assertTrue(result.hasCritical()),
                () -> assertEquals(List.of(warning), result.warnings())
        );
    }

    @Test
    void emptyResultHasNoCriticalIssues() {
        assertFalse(ValidationResult.empty().hasCritical());
    }
}
