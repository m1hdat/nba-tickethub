package com.example.nbatickethub.model;

/** Immutable Arena data. */
public final class Arena {
    public final String id;
    public final String name;
    public final int schemaVersion;
    public final String geometryVersion;
    public final WorldRect worldBounds;
    public final WorldRect courtBounds;
    public final DataProvenance provenance;

    public Arena(String id, String name, int schemaVersion, String geometryVersion, WorldRect worldBounds, WorldRect courtBounds, DataProvenance provenance) {
        this.id = ModelChecks.id(id);
        this.name = ModelChecks.text(name, "name");
        this.schemaVersion = ModelChecks.positive(schemaVersion, "schemaVersion");
        this.geometryVersion = ModelChecks.text(geometryVersion, "geometryVersion");
        this.worldBounds = java.util.Objects.requireNonNull(worldBounds, "worldBounds");
        this.courtBounds = java.util.Objects.requireNonNull(courtBounds, "courtBounds");
        this.provenance = java.util.Objects.requireNonNull(provenance, "provenance");
        if (!worldBounds.encloses(courtBounds)) { throw new IllegalArgumentException("Court outside arena"); }
    }
}

