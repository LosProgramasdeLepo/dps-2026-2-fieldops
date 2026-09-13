package edu.itba.fieldops.domain.expedition;

public final class ExpeditionNotApprovable extends RuntimeException {
    public ExpeditionNotApprovable(String reason) {
        super(reason);
    }
}
