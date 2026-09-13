package edu.itba.fieldops.domain.itinerary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Itinerary {
    private final List<Activity> activities = new ArrayList<>();

    public void add(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        requireUnknown(activity.id());
        requireKnownPredecessors(activity);
        requireAcyclic(activity);
        requirePredecessorsFinishBefore(activity);
        activities.add(activity);
    }

    public void remove(UUID activityId) {
        requireUnusedPredecessor(activityId);
        if (!activities.removeIf(activity -> activity.id().equals(activityId))) {
            throw new IllegalArgumentException("unknown activity: " + activityId);
        }
    }

    public void reorder(List<UUID> orderedIds) {
        Objects.requireNonNull(orderedIds, "activity order");
        if (orderedIds.size() != activities.size() || Set.copyOf(orderedIds).size() != activities.size()) {
            throw new IllegalArgumentException("activity order must list each itinerary activity once");
        }
        List<Activity> reordered = orderedIds.stream().map(this::activityOf).toList();
        activities.clear();
        activities.addAll(reordered);
    }

    public void addDependency(UUID activityId, UUID predecessorId) {
        Objects.requireNonNull(predecessorId, "predecessor id");
        Activity activity = activityOf(activityId);
        Activity predecessor = activityOf(predecessorId);
        if (activity.predecessors().contains(predecessor.id())) {
            throw new IllegalArgumentException("duplicate predecessor: " + predecessorId);
        }
        Activity updated = activity.withPredecessor(predecessor.id());
        requireAcyclic(updated);
        requirePredecessorsFinishBefore(updated);
        replace(updated);
    }

    public Activity activityOf(UUID activityId) {
        return activities.get(indexOf(activityId));
    }

    public List<Activity> activities() {
        return List.copyOf(activities);
    }

    private void requireUnknown(UUID activityId) {
        if (activities.stream().anyMatch(activity -> activity.id().equals(activityId))) {
            throw new IllegalArgumentException("duplicate activity: " + activityId);
        }
    }

    private void requireKnownPredecessors(Activity activity) {
        activity.predecessors().forEach(this::activityOf);
    }

    private void requireUnusedPredecessor(UUID activityId) {
        boolean used = activities.stream().anyMatch(activity -> activity.predecessors().contains(activityId));
        if (used) {
            throw new IllegalArgumentException("activity is a predecessor of another");
        }
    }

    private void requireAcyclic(Activity activity) {
        if (reaches(activity.id(), activity.predecessors(), new HashSet<>())) {
            throw new IllegalArgumentException("activity dependencies form a cycle");
        }
    }

    private boolean reaches(UUID target, Set<UUID> from, Set<UUID> seen) {
        for (UUID predecessorId : from) {
            if (predecessorId.equals(target)) {
                return true;
            }
            if (!seen.add(predecessorId)) {
                continue;
            }
            if (reaches(target, activityOf(predecessorId).predecessors(), seen)) {
                return true;
            }
        }
        return false;
    }

    private void requirePredecessorsFinishBefore(Activity activity) {
        for (UUID predecessorId : activity.predecessors()) {
            Activity predecessor = activityOf(predecessorId);
            if (!predecessor.window().finishesBeforeStartOf(activity.window())) {
                throw new IllegalArgumentException("predecessor must finish before activity starts: " + predecessorId);
            }
        }
    }

    private void replace(Activity updated) {
        activities.set(indexOf(updated.id()), updated);
    }

    private int indexOf(UUID activityId) {
        for (int index = 0; index < activities.size(); index++) {
            if (activities.get(index).id().equals(activityId)) {
                return index;
            }
        }
        throw new IllegalArgumentException("unknown activity: " + activityId);
    }
}
