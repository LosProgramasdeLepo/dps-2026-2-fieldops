package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.ValidationIssue;

import java.util.List;

public interface ValidationRule {
    List<ValidationIssue> check(ValidationContext context);
}
