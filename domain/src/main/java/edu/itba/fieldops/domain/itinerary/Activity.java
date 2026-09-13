package edu.itba.fieldops.domain.itinerary;

import edu.itba.fieldops.domain.shared.RiskLevel;
import edu.itba.fieldops.domain.shared.Texts;
import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Activity {
    private final UUID id;
    private final String name;
    private final ActivityPolicy policy;
    private final TimePeriod window;
    private final Set<UUID> predecessors;
    private final WorkZone zone;

    public Activity(
            UUID id,
            String name,
            ActivityPolicy policy,
            TimePeriod window,
            Set<UUID> predecessors,
            WorkZone zone
    ) {
        this.id = Objects.requireNonNull(id, "activity id");
        this.name = Texts.required(name, "activity name");
        this.policy = Objects.requireNonNull(policy, "activity policy");
        this.window = Objects.requireNonNull(window, "activity window");
        this.predecessors = Set.copyOf(predecessors);
        this.zone = Objects.requireNonNull(zone, "activity zone");
        requireNoSelfPredecessor();
        requireWindowFitsEstimate();
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TimePeriod window() {
        return window;
    }

    public Set<UUID> predecessors() {
        return predecessors;
    }

    public WorkZone zone() {
        return zone;
    }

    public Duration estimatedDuration() {
        return policy.estimatedDuration();
    }

    public RiskLevel risk() {
        return policy.risk();
    }

    public ResourceRequirements requirements() {
        return policy.requirements();
    }

    private void requireNoSelfPredecessor() {
        if (predecessors.contains(id)) {
            throw new IllegalArgumentException("activity cannot precede itself");
        }
    }

    private void requireWindowFitsEstimate() {
        Duration length = Duration.between(window.start(), window.end());
        if (length.compareTo(policy.estimatedDuration()) < 0) {
            throw new IllegalArgumentException("activity window is shorter than estimated duration");
        }
    }
}
