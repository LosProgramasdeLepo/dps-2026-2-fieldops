package edu.itba.fieldops.domain.expedition;

import edu.itba.fieldops.domain.itinerary.Activity;
import edu.itba.fieldops.domain.itinerary.Itinerary;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;
import edu.itba.fieldops.domain.tracking.ActivityExecution;
import edu.itba.fieldops.domain.tracking.Incident;
import edu.itba.fieldops.domain.tracking.Observation;
import edu.itba.fieldops.domain.assessment.ValidationIssue;
import edu.itba.fieldops.domain.assessment.ValidationResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Expedition {
    private final UUID id;
    private final int version;
    private final UUID supersedes;
    private final List<Objective> objectives;
    private final TimePeriod period;
    private final List<WorkZone> zones;
    private final List<UUID> responsibles;
    private final List<Restriction> restrictions;
    private final Itinerary itinerary;
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
        this.version = 1;
        this.supersedes = null;
        this.objectives = copyRequired(objectives, "objectives");
        this.period = Objects.requireNonNull(period, "period");
        this.zones = copyRequired(zones, "zones");
        this.responsibles = copyRequired(responsibles, "responsibles");
        this.restrictions = List.copyOf(restrictions);
        this.itinerary = new Itinerary();
        this.assignments = new ArrayList<>();
        this.permits = new ArrayList<>();
        this.acceptedWarnings = new ArrayList<>();
        this.incidents = new ArrayList<>();
        this.observations = new ArrayList<>();
        this.executions = new ArrayList<>();
        this.status = ExpeditionStatus.DRAFT;
    }

    private Expedition(Expedition source) {
        this.id = UUID.randomUUID();
        this.version = source.version + 1;
        this.supersedes = source.id;
        this.objectives = source.objectives;
        this.period = source.period;
        this.zones = source.zones;
        this.responsibles = source.responsibles;
        this.restrictions = source.restrictions;
        this.itinerary = source.itinerary.copy();
        this.assignments = new ArrayList<>(source.assignments);
        this.permits = new ArrayList<>(source.permits);
        this.acceptedWarnings = new ArrayList<>();
        this.incidents = new ArrayList<>();
        this.observations = new ArrayList<>();
        this.executions = new ArrayList<>();
        this.status = ExpeditionStatus.DRAFT;
    }

    public Expedition reviseAsDraft() {
        return new Expedition(this);
    }

    public void addActivity(Activity activity) {
        requireStatus(ExpeditionStatus.DRAFT, "add activity");
        Objects.requireNonNull(activity, "activity");
        requireKnownZone(activity.zone());
        requireWindowInsidePeriod(activity);
        itinerary.add(activity);
    }

    public void removeActivity(UUID activityId) {
        requireStatus(ExpeditionStatus.DRAFT, "remove activity");
        itinerary.remove(activityId);
        assignments.removeIf(assignment -> assignment.activityId().equals(activityId));
    }

    public void reorderActivities(List<UUID> orderedIds) {
        requireStatus(ExpeditionStatus.DRAFT, "reorder activities");
        itinerary.reorder(orderedIds);
    }

    public void addDependency(UUID activityId, UUID predecessorId) {
        requireStatus(ExpeditionStatus.DRAFT, "add dependency");
        itinerary.addDependency(activityId, predecessorId);
    }

    public void addAssignment(Assignment assignment) {
        requireEditable("assign resources");
        Objects.requireNonNull(assignment, "assignment");
        itinerary.activityOf(assignment.activityId());
        requireUnknownAssignment(assignment);
        assignments.add(assignment);
    }

    public void removeAssignment(Assignment assignment) {
        requireEditable("unassign resources");
        Objects.requireNonNull(assignment, "assignment");
        if (!assignments.remove(assignment)) {
            throw new IllegalArgumentException("unknown assignment");
        }
    }

    public void delay(UUID activityId, Duration delay) {
        requireStatus(ExpeditionStatus.DRAFT, "delay activity");
        Objects.requireNonNull(delay, "delay");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        for (Activity activity : itinerary.delayed(activityId, delay)) {
            requireWindowInsidePeriod(activity);
        }
        itinerary.delay(activityId, delay);
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
        Activity activity = itinerary.activityOf(activityId);
        requireNotStarted(activityId);
        requirePredecessorsFinished(activity, at);
        executions.add(new ActivityExecution(activityId, at));
    }

    public void finishActivity(UUID activityId, Instant at, String result) {
        requireStatus(ExpeditionStatus.IN_PROGRESS, "finish activity");
        for (int index = 0; index < executions.size(); index++) {
            ActivityExecution execution = executions.get(index);
            if (execution.activityId().equals(activityId)) {
                executions.set(index, execution.finish(at, result));
                return;
            }
        }
        throw new IllegalArgumentException("activity not started: " + activityId);
    }

    public void submitForReview() {
        transition(ExpeditionStatus.DRAFT, ExpeditionStatus.IN_REVIEW, "submit for review");
    }

    public void returnToDraft() {
        if (!status.canReturnToDraft()) {
            throw new InvalidExpeditionTransition(status, "return to draft");
        }
        if (status.isActive()) {
            executions.clear();
        }
        status = ExpeditionStatus.DRAFT;
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
        requireStatus(ExpeditionStatus.IN_PROGRESS, "finish");
        requireAllActivitiesFinished();
        status = ExpeditionStatus.FINISHED;
    }

    public UUID id() {
        return id;
    }

    public int version() {
        return version;
    }

    public Optional<UUID> supersedes() {
        return Optional.ofNullable(supersedes);
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
        return itinerary.activities();
    }

    public List<Assignment> assignments() {
        return List.copyOf(assignments);
    }

    public List<Assignment> assignmentsOf(UUID activityId) {
        Objects.requireNonNull(activityId, "activity id");
        return assignments.stream()
                .filter(assignment -> assignment.activityId().equals(activityId))
                .toList();
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

    public Activity activityOf(UUID activityId) {
        return itinerary.activityOf(activityId);
    }

    public List<Expedition> occupyingPeers(List<Expedition> others) {
        Objects.requireNonNull(others, "other expeditions");
        return List.copyOf(others).stream()
                .filter(peer -> !peer.id().equals(id))
                // a revision replaces the version it supersedes, so it does not compete with it for resources
                .filter(peer -> !peer.id().equals(supersedes))
                .filter(peer -> peer.status().occupiesResources())
                .toList();
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

    private void requireAllActivitiesFinished() {
        for (Activity activity : itinerary.activities()) {
            if (executionOf(activity.id()).filter(ActivityExecution::isFinished).isEmpty()) {
                throw new IllegalArgumentException("activity not finished: " + activity.id());
            }
        }
    }

    private void requirePredecessorsFinished(Activity activity, Instant at) {
        for (UUID predecessorId : activity.predecessors()) {
            boolean ready = executionOf(predecessorId)
                    .flatMap(ActivityExecution::finishedAt)
                    .filter(end -> !at.isBefore(end))
                    .isPresent();
            if (!ready) {
                throw new IllegalArgumentException("predecessor must finish before activity starts: " + predecessorId);
            }
        }
    }

    private void requireNotStarted(UUID activityId) {
        if (executionOf(activityId).isPresent()) {
            throw new IllegalArgumentException("activity already started: " + activityId);
        }
    }

    private Optional<ActivityExecution> executionOf(UUID activityId) {
        return executions.stream()
                .filter(execution -> execution.activityId().equals(activityId))
                .findFirst();
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
