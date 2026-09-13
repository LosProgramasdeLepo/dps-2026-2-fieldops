package edu.itba.fieldops.domain.expedition;

public final class InvalidExpeditionTransition extends RuntimeException {
    public InvalidExpeditionTransition(ExpeditionStatus current, String action) {
        super("cannot " + action + " while expedition is " + current);
    }
}
