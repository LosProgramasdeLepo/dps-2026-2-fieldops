package edu.itba.fieldops.domain.shared;

public final class WorkZone {
    private final String name;
    private final String region;

    public WorkZone(String name) {
        this(name, null);
    }

    public WorkZone(String name, String region) {
        this.name = Texts.required(name, "zone name");
        this.region = region == null || region.isBlank() ? null : region.trim();
    }

    public String name() {
        return name;
    }

    public String region() {
        return region;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WorkZone zone && name.equals(zone.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return region == null ? name : name + " (" + region + ")";
    }
}
