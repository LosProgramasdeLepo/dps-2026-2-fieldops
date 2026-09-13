package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.validation.ValidationIssue;
import edu.itba.fieldops.domain.validation.ValidationResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Expedition {
    private final UUID id;
    private final List<Objective> objectives;
    private final TimePeriod period;
    private final List<WorkZone> zones;
    private final List<UUID> responsibles;
    private final List<Restriction> restrictions;
    private final List<Activity> itinerary;
    private final List<Assignment> assignments;
    private final List<UUID> permits;
    private final List<AcceptedWarning> acceptedWarnings;
    private final List<Incident> incidents;
    private final List<Observation> observations;
    private final List<ActivityExecution> executions;
    private ExpeditionStatus status;

    public static Expedition draft(
            UUID id,
            List<Objective> objectives,
            TimePeriod period,
            List<WorkZone> zones,
            List<UUID> responsibles,
            List<Restriction> restrictions
    ) {
        return new Expedition(id, objectives, period, zones, responsibles, restrictions);
    }

    private Expedition(
            UUID id,
            List<Objective> objectives,
            TimePeriod period,
            List<WorkZone> zones,
            List<UUID> responsibles,
            List<Restriction> restrictions
    ) {
        this.id = Objects.requireNonNull(id, "expedition id");
        this.objectives = copyRequired(objectives, "objectives");
        this.period = Objects.requireNonNull(period, "period");
        this.zones = copyRequired(zones, "zones");
        this.responsibles = copyRequired(responsibles, "responsibles");
        this.restrictions = List.copyOf(restrictions);
        this.itinerary = new ArrayList<>();
        this.assignments = new ArrayList<>();
        this.permits = new ArrayList<>();
        this.acceptedWarnings = new ArrayList<>();
        this.incidents = new ArrayList<>();
        this.observations = new ArrayList<>();
        this.executions = new ArrayList<>();
        this.status = ExpeditionStatus.DRAFT;
    }

    public void addActivity(Activity activity) {
        requireStatus(ExpeditionStatus.DRAFT, "add activity");
        Objects.requireNonNull(activity, "activity");
        requireUnknownActivity(activity.id());
        requireKnownZone(activity.zone());
        requireWindowInsidePeriod(activity);
        requireKnownPredecessors(activity);
        itinerary.add(activity);
    }

    public void removeActivity(UUID activityId) {
        requireStatus(ExpeditionStatus.DRAFT, "remove activity");
        requireUnusedPredecessor(activityId);
        if (!itinerary.removeIf(activity -> activity.id().equals(activityId))) {
            throw new IllegalArgumentException("unknown activity: " + activityId);
        }
        assignments.removeIf(assignment -> assignment.activityId().equals(activityId));
    }

    public void addAssignment(Assignment assignment) {
        requireEditable("assign resources");
        Objects.requireNonNull(assignment, "assignment");
        requireKnownActivity(assignment.activityId());
        requireUnknownAssignment(assignment);
        assignments.add(assignment);
    }

    public void addPermit(UUID permitId) {
        requireEditable("attach permit");
        Objects.requireNonNull(permitId, "permit id");
        if (permits.contains(permitId)) {
            throw new IllegalArgumentException("duplicate permit: " + permitId);
        }
        permits.add(permitId);
    }

    public void acceptWarning(AcceptedWarning warning) {
        requireStatus(ExpeditionStatus.IN_REVIEW, "accept warning");
        Objects.requireNonNull(warning, "warning");
        requireUnknownWarning(warning);
        acceptedWarnings.add(warning);
    }

    public void addIncident(Incident incident) {
        requireActive("record incident");
        incidents.add(Objects.requireNonNull(incident, "incident"));
    }

    public void addObservation(Observation observation) {
        requireActive("record observation");
        observations.add(Objects.requireNonNull(observation, "observation"));
    }

    public void startActivity(UUID activityId, Instant at) {
        requireStatus(ExpeditionStatus.IN_PROGRESS, "start activity");
        requireKnownActivity(activityId);
        requireNotStarted(activityId);
        executions.add(new ActivityExecution(activityId, at));
    }

    public void finishActivity(UUID activityId, Instant at, String result) {
        requireStatus(ExpeditionStatus.IN_PROGRESS, "finish activity");
        executionOf(activityId).finish(at, result);
    }

    public void submitForReview() {
        transition(ExpeditionStatus.DRAFT, ExpeditionStatus.IN_REVIEW, "submit for review");
    }

    public void returnToDraft() {
        transition(ExpeditionStatus.IN_REVIEW, ExpeditionStatus.DRAFT, "return to draft");
        acceptedWarnings.clear();
    }

    public void approve(ValidationResult validation) {
        requireStatus(ExpeditionStatus.IN_REVIEW, "approve");
        requireApprovable(Objects.requireNonNull(validation, "validation"));
        status = ExpeditionStatus.APPROVED;
    }

    public void start() {
        transition(ExpeditionStatus.APPROVED, ExpeditionStatus.IN_PROGRESS, "start");
    }

    public void suspend() {
        if (!status.isSuspendable()) {
            throw new InvalidExpeditionTransition(status, "suspend");
        }
        status = ExpeditionStatus.SUSPENDED;
    }

    public void resume() {
        transition(ExpeditionStatus.SUSPENDED, ExpeditionStatus.IN_PROGRESS, "resume");
    }

    public void finish() {
        transition(ExpeditionStatus.IN_PROGRESS, ExpeditionStatus.FINISHED, "finish");
    }

    public UUID id() {
        return id;
    }

    public List<Objective> objectives() {
        return objectives;
    }

    public TimePeriod period() {
        return period;
    }

    public List<WorkZone> zones() {
        return zones;
    }

    public List<UUID> responsibles() {
        return responsibles;
    }

    public List<Restriction> restrictions() {
        return restrictions;
    }

    public ExpeditionStatus status() {
        return status;
    }

    public List<Activity> itinerary() {
        return List.copyOf(itinerary);
    }

    public List<Assignment> assignments() {
        return List.copyOf(assignments);
    }

    public List<UUID> permits() {
        return List.copyOf(permits);
    }

    public List<AcceptedWarning> acceptedWarnings() {
        return List.copyOf(acceptedWarnings);
    }

    public List<Incident> incidents() {
        return List.copyOf(incidents);
    }

    public List<Observation> observations() {
        return List.copyOf(observations);
    }

    public List<ActivityExecution> executions() {
        return List.copyOf(executions);
    }

    public void activityOf(UUID activityId) {
        itinerary.stream()
                .filter(activity -> activity.id().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown activity: " + activityId));
    }

    private void requireApprovable(ValidationResult validation) {
        if (validation.hasCritical()) {
            throw new ExpeditionNotApprovable("critical validation issues remain");
        }
        validation.warnings().forEach(this::requireAccepted);
    }

    private void requireAccepted(ValidationIssue warning) {
        boolean accepted = acceptedWarnings.stream().anyMatch(entry -> entry.issue().equals(warning));
        if (!accepted) {
            throw new ExpeditionNotApprovable("warning not justified: " + warning.code());
        }
    }

    private void requireUnknownActivity(UUID activityId) {
        if (itinerary.stream().anyMatch(activity -> activity.id().equals(activityId))) {
            throw new IllegalArgumentException("duplicate activity: " + activityId);
        }
    }

    private void requireUnknownAssignment(Assignment assignment) {
        if (assignments.contains(assignment)) {
            throw new IllegalArgumentException("duplicate assignment");
        }
    }

    private void requireUnknownWarning(AcceptedWarning warning) {
        boolean accepted = acceptedWarnings.stream().anyMatch(entry -> entry.issue().equals(warning.issue()));
        if (accepted) {
            throw new IllegalArgumentException("warning already accepted: " + warning.issue().code());
        }
    }

    private void requireKnownActivity(UUID activityId) {
        activityOf(activityId);
    }

    private void requireKnownZone(WorkZone zone) {
        if (!zones.contains(zone)) {
            throw new IllegalArgumentException("activity zone is not part of the expedition");
        }
    }

    private void requireWindowInsidePeriod(Activity activity) {
        if (!period.contains(activity.window())) {
            throw new IllegalArgumentException("activity window is outside the expedition period");
        }
    }

    private void requireKnownPredecessors(Activity activity) {
        activity.predecessors().forEach(this::requireKnownActivity);
    }

    private void requireUnusedPredecessor(UUID activityId) {
        boolean used = itinerary.stream().anyMatch(activity -> activity.predecessors().contains(activityId));
        if (used) {
            throw new IllegalArgumentException("activity is a predecessor of another");
        }
    }

    private void requireNotStarted(UUID activityId) {
        boolean started = executions.stream().anyMatch(execution -> execution.activityId().equals(activityId));
        if (started) {
            throw new IllegalArgumentException("activity already started: " + activityId);
        }
    }

    private ActivityExecution executionOf(UUID activityId) {
        return executions.stream()
                .filter(execution -> execution.activityId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("activity not started: " + activityId));
    }

    private void transition(ExpeditionStatus from, ExpeditionStatus to, String action) {
        requireStatus(from, action);
        status = to;
    }

    private void requireStatus(ExpeditionStatus expected, String action) {
        if (status != expected) {
            throw new InvalidExpeditionTransition(status, action);
        }
    }

    private void requireEditable(String action) {
        if (!status.isEditable()) {
            throw new InvalidExpeditionTransition(status, action);
        }
    }

    private void requireActive(String action) {
        if (!status.isActive()) {
            throw new InvalidExpeditionTransition(status, action);
        }
    }

    private static <T> List<T> copyRequired(List<T> values, String name) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return List.copyOf(values);
    }
}
