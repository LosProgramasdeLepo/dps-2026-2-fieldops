package edu.itba.fieldops.domain.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultConstructionTest {
    @Test
    void distinguishesCriticalIssuesFromWarnings() {
        ValidationIssue critical = new ValidationIssue(IssueSeverity.CRITICAL, "PERMIT", "missing permit");
        ValidationIssue warning = new ValidationIssue(IssueSeverity.WARNING, "RISK", "high risk window");
        ValidationResult result = new ValidationResult(List.of(critical, warning));

        assertTrue(result.hasCritical());
        assertEquals(1, result.warnings().size());
        assertFalse(ValidationResult.empty().hasCritical());
    }
}
