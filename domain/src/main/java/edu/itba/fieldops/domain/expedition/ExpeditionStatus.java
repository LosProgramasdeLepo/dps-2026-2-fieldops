package edu.itba.fieldops.domain.expedition;

public enum ExpeditionStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    IN_PROGRESS,
    SUSPENDED,
    FINISHED;

    public boolean isEditable() {
        return this == DRAFT || this == IN_REVIEW;
    }

    public boolean isActive() {
        return this == IN_PROGRESS || this == SUSPENDED;
    }

    public boolean isSuspendable() {
        return this == APPROVED || this == IN_PROGRESS;
    }

    public boolean occupiesResources() {
        return this == IN_REVIEW || this == APPROVED || this == IN_PROGRESS || this == SUSPENDED;
    }
}
