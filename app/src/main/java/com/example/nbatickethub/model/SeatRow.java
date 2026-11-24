package com.example.nbatickethub.model;

/** Immutable SeatRow data. */
public final class SeatRow {
    public final String id;
    public final String sectionId;
    public final String rowLabel;
    public final int order;
    public final AnnularSector boundary;
    public final int firstSeatIndex;
    public final int seatCount;

    public SeatRow(String id, String sectionId, String rowLabel, int order, AnnularSector boundary, int firstSeatIndex, int seatCount) {
        this.id = ModelChecks.id(id);
        this.sectionId = ModelChecks.id(sectionId);
        this.rowLabel = ModelChecks.text(rowLabel, "rowLabel");
        this.order = ModelChecks.positive(order, "order");
        this.boundary = java.util.Objects.requireNonNull(boundary, "boundary");
        this.firstSeatIndex = firstSeatIndex;
        this.seatCount = ModelChecks.positive(seatCount, "seatCount");
        if (firstSeatIndex < 0 || (long) firstSeatIndex + seatCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid seat index range");
        }
    }
}

