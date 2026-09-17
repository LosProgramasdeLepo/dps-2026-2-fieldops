package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.expedition.Expedition;

import java.util.List;
import java.util.Objects;

public final class ExpeditionValidator {
    private final List<ValidationRule> rules;

    public ExpeditionValidator(List<ValidationRule> rules) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
    }

    public static ExpeditionValidator withDefaultRules() {
        return new ExpeditionValidator(List.of(
                new MissingResourceRule(),
                new TemporalOverlapRule(),
                new StockRule(),
                new CertificationRule(),
                new CapacityRule(),
                new PermitRule()
        ));
    }

    public ValidationResult validate(Expedition expedition, Catalog catalog, List<Expedition> others) {
        return validate(ValidationContext.of(expedition, catalog, others));
    }

    public ValidationResult validate(ValidationContext context) {
        Objects.requireNonNull(context, "context");
        return new ValidationResult(rules.stream()
                .map(rule -> rule.check(context))
                .flatMap(List::stream)
                .toList());
    }
}
