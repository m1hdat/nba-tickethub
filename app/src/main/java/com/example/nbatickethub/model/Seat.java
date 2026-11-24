package com.example.nbatickethub.model;

/** Immutable Seat data. */
public final class Seat {
    public final String id;
    public final String rowId;
    public final int positionNumber;
    public final PositionKind kind;
    public final int geometryIndex;

    public Seat(String id, String rowId, int positionNumber, PositionKind kind, int geometryIndex) {
        this.id = ModelChecks.id(id);
        this.rowId = ModelChecks.id(rowId);
        this.positionNumber = ModelChecks.positive(positionNumber, "positionNumber");
        this.kind = java.util.Objects.requireNonNull(kind, "kind");
        this.geometryIndex = geometryIndex;
        if (geometryIndex < 0) { throw new IllegalArgumentException("Invalid geometry index"); }
    }
}

