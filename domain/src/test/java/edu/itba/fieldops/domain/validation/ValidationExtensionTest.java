package edu.itba.fieldops.domain.validation;

import edu.itba.fieldops.domain.assessment.IssueSeverity;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;
import edu.itba.fieldops.domain.catalog.Catalog;
import edu.itba.fieldops.domain.catalog.Consumable;
import edu.itba.fieldops.domain.catalog.Instrument;
import edu.itba.fieldops.domain.catalog.Permit;
import edu.itba.fieldops.domain.catalog.Person;
import edu.itba.fieldops.domain.catalog.Vehicle;
import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.Objective;
import edu.itba.fieldops.domain.expedition.PersonAssignment;
import edu.itba.fieldops.domain.expedition.Restriction;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.TransitPolicy;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationExtensionTest {
    private static final Instant DAY = Instant.parse("2026-03-02T06:00:00Z");
    private static final WorkZone ZONE = new WorkZone("Glaciar Norte");

    @Test
    void acceptsARuleThatTheValidatorDoesNotKnowAbout() {
        Expedition expedition = expeditionWithTransit();
        ValidationRule everyActivityNeedsABriefing = context -> context.expedition().itinerary().stream()
                .map(activity -> new ValidationIssue(
                        IssueSeverity.WARNING,
                        "BRIEFING",
                        "activity " + activity.name() + " has no safety briefing"
                ))
                .toList();

        ValidationResult result = new ExpeditionValidator(List.of(everyActivityNeedsABriefing))
                .validate(expedition, emptyCatalog(), List.of());

        assertEquals(1, result.issues().size());
        assertEquals("BRIEFING", result.issues().getFirst().code());
    }

    @Test
    void combinesACustomRuleWithAStandardRule() {
        Expedition expedition = expeditionWithTransit();
        expedition.addAssignment(new PersonAssignment(expedition.itinerary().getFirst().id(), UUID.randomUUID()));
        ValidationRule alwaysCritical = context -> List.of(
                new ValidationIssue(IssueSeverity.CRITICAL, "BRIEFING", "no briefing on file")
        );

        ValidationResult result = new ExpeditionValidator(List.of(new MissingResourceRule(), alwaysCritical))
                .validate(expedition, emptyCatalog(), List.of());

        assertTrue(result.hasCritical());
        assertTrue(hasCode(result, "RESOURCE"), () -> "expected RESOURCE in " + result.issues());
        assertTrue(hasCode(result, "BRIEFING"), () -> "expected BRIEFING in " + result.issues());
    }

    @Test
    void validatesAgainstAnyCatalogImplementation() {
        Expedition expedition = expeditionWithTransit();
        expedition.addAssignment(new PersonAssignment(expedition.itinerary().getFirst().id(), UUID.randomUUID()));

        ValidationResult result = ExpeditionValidator.withDefaultRules()
                .validate(expedition, emptyCatalog(), List.of());

        assertTrue(hasCode(result, "RESOURCE"), () -> "expected RESOURCE in " + result.issues());
    }

    @Test
    void doesNotObserveRulesAddedAfterConstruction() {
        Expedition expedition = expeditionWithTransit();
        ValidationRule noise = context -> List.of(
                new ValidationIssue(IssueSeverity.WARNING, "NOISE", "should not reach the validator")
        );
        List<ValidationRule> mutable = new ArrayList<>();
        ExpeditionValidator validator = new ExpeditionValidator(mutable);
        mutable.add(noise);

        ValidationResult result = validator.validate(expedition, emptyCatalog(), List.of());

        assertFalse(hasCode(result, "NOISE"), () -> "validator kept a live view of the rule list: " + result.issues());
    }

    private static boolean hasCode(ValidationResult result, String code) {
        return result.issues().stream().anyMatch(issue -> issue.code().equals(code));
    }

    private static Expedition expeditionWithTransit() {
        Expedition expedition = Expedition.draft(
                UUID.randomUUID(),
                List.of(new Objective("relevar el frente del glaciar")),
                new TimePeriod(DAY, DAY.plusSeconds(24 * 3600L)),
                List.of(ZONE),
                List.of(UUID.randomUUID()),
                List.of(new Restriction("sin vuelos nocturnos"))
        );
        expedition.addActivity(new Activity(
                UUID.randomUUID(),
                "Traslado al campamento",
                new TransitPolicy(),
                new TimePeriod(DAY, DAY.plusSeconds(4 * 3600L)),
                Set.of(),
                ZONE
        ));
        return expedition;
    }

    private static Catalog emptyCatalog() {
        return new Catalog() {
            @Override
            public Optional<Person> person(UUID id) {
                return Optional.empty();
            }

            @Override
            public Optional<Vehicle> vehicle(UUID id) {
                return Optional.empty();
            }

            @Override
            public Optional<Instrument> instrument(UUID id) {
                return Optional.empty();
            }

            @Override
            public Optional<Consumable> consumable(UUID id) {
                return Optional.empty();
            }

            @Override
            public Optional<Permit> permit(UUID id) {
                return Optional.empty();
            }

            @Override
            public List<Person> people() {
                return List.of();
            }

            @Override
            public List<Vehicle> vehicles() {
                return List.of();
            }

            @Override
            public List<Instrument> instruments() {
                return List.of();
            }
        };
    }
}
