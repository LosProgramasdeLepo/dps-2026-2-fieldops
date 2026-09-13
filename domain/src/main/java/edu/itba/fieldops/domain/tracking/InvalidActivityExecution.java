package edu.itba.fieldops.domain.tracking;

public final class InvalidActivityExecution extends RuntimeException {
    public InvalidActivityExecution(String reason) {
        super(reason);
    }
}
