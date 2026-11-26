package com.example.nbatickethub.model;

/** Immutable per-game mock ticket offer; not a booking or issued ticket. */
public final class GameSeat {
    public final String gameId;
    public final String seatId;
    public final SeatStatus baseStatus;
    public final Money price;
    public final DataProvenance provenance;
    public final Section.Category ticketCategory;

    public GameSeat(String gameId, String seatId, SeatStatus baseStatus, Money price, DataProvenance provenance) {
        this(gameId, seatId, baseStatus, price, provenance, null);
    }

    public GameSeat(String gameId, String seatId, SeatStatus baseStatus, Money price, DataProvenance provenance, Section.Category ticketCategory) {
        this.ticketCategory = ticketCategory;
        this.gameId = ModelChecks.id(gameId);
        this.seatId = ModelChecks.id(seatId);
        this.baseStatus = java.util.Objects.requireNonNull(baseStatus, "baseStatus");
        this.price = java.util.Objects.requireNonNull(price, "price");
        this.provenance = java.util.Objects.requireNonNull(provenance, "provenance");
    }
}

