package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record OperationalReport(
        Duration duration,
        RiskLevel risk,
        Map<UUID, Quantity> consumption,
        List<Incident> incidents
) {
    public OperationalReport {
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(risk, "risk");
        consumption = Map.copyOf(consumption);
        incidents = List.copyOf(incidents);
    }

    public static OperationalReport of(Expedition expedition) {
        return new OperationalReport(
                totalDuration(expedition),
                highestRisk(expedition),
                consumption(expedition),
                expedition.incidents()
        );
    }

    private static Duration totalDuration(Expedition expedition) {
        return expedition.itinerary().stream()
                .map(Activity::estimatedDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    private static RiskLevel highestRisk(Expedition expedition) {
        return expedition.itinerary().stream()
                .map(Activity::risk)
                .max(Comparator.naturalOrder())
                .orElse(RiskLevel.LOW);
    }

    private static Map<UUID, Quantity> consumption(Expedition expedition) {
        Map<UUID, Quantity> totals = new HashMap<>();
        expedition.assignments().forEach(assignment ->
                assignment.consumption().forEach((id, quantity) -> totals.merge(id, quantity, Quantity::plus)));
        return totals;
    }
}
