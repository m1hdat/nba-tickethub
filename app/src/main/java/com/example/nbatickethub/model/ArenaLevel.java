package com.example.nbatickethub.model;

/** Immutable ArenaLevel data. */
public final class ArenaLevel {
    public final String id;
    public final String arenaId;
    public final String code;
    public final String name;
    public final int order;
    public final AnnularSector footprint;

    public ArenaLevel(String id, String arenaId, String code, String name, int order, AnnularSector footprint) {
        this.id = ModelChecks.id(id);
        this.arenaId = ModelChecks.id(arenaId);
        this.code = ModelChecks.text(code, "code");
        this.name = ModelChecks.text(name, "name");
        this.order = ModelChecks.positive(order, "order");
        this.footprint = java.util.Objects.requireNonNull(footprint, "footprint");
    }
}

