package com.example.nbatickethub.model;

/** One catalog-wide status palette, shared by every theme. Seat base colors are independent. */
public final class SeatStatusPalette {
    public final int available, booked, locked;

    public SeatStatusPalette(int available, int booked, int locked) {
        if (available == booked || available == locked || booked == locked) {
            throw new IllegalArgumentException("Seat statuses need distinct colors");
        }
        this.available = available;
        this.booked = booked;
        this.locked = locked;
    }

    public int color(SeatStatus status) {
        switch (status) {
            case AVAILABLE: return available;
            case BOOKED: return booked;
            case LOCKED: return locked;
            default: throw new IllegalArgumentException("Unknown status");
        }
    }
}
