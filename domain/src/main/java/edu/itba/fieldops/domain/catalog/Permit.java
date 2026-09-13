package edu.itba.fieldops.domain.catalog;

import edu.itba.fieldops.domain.shared.TimePeriod;
import edu.itba.fieldops.domain.shared.WorkZone;

import java.util.Objects;
import java.util.UUID;

public record Permit(UUID id, WorkZone zone, TimePeriod validity) {
    public Permit {
        Objects.requireNonNull(id, "permit id");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(validity, "validity");
    }

    public boolean covers(WorkZone workZone, TimePeriod period) {
        return zone.equals(workZone) && validity.contains(period);
    }
}
