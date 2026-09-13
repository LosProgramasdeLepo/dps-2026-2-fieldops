package edu.itba.fieldops.domain.report;

import edu.itba.fieldops.domain.expedition.Expedition;
import edu.itba.fieldops.domain.expedition.ExpeditionStatus;
import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.Quantity;
import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record OperationalReport(
        ExpeditionStatus status,
        int plannedActivities,
        int startedActivities,
        int finishedActivities,
        Duration duration,
        RiskLevel risk,
        Map<UUID, Quantity> consumption,
        List<Incident> incidents,
        List<ActivityResult> activityResults
) {
    public OperationalReport {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(risk, "risk");
        consumption = Map.copyOf(consumption);
        incidents = List.copyOf(incidents);
        activityResults = List.copyOf(activityResults);
    }

    public static OperationalReport of(Expedition expedition) {
        List<ActivityExecution> executions = expedition.executions();
        return new OperationalReport(
                expedition.status(),
                expedition.itinerary().size(),
                executions.size(),
                (int) executions.stream().filter(ActivityExecution::isFinished).count(),
                totalDuration(expedition),
                highestRisk(expedition),
                consumption(expedition),
                expedition.incidents(),
                activityResults(executions)
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

    private static List<ActivityResult> activityResults(List<ActivityExecution> executions) {
        return executions.stream()
                .filter(ActivityExecution::isFinished)
                .map(execution -> new ActivityResult(execution.activityId(), execution.result()))
                .toList();
    }
}
