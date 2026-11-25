package com.example.nbatickethub.model;

import java.util.Objects;

/** Venue-owned dimensions and projection. No team, inventory or appearance data. */
public final class ArenaGeometry {
    public final WorldRect displayBounds;
    public final float scaleX, scaleY, bowlExponent;
    public final float courtLength, courtWidth, courtAngle;

    public ArenaGeometry(WorldRect displayBounds, float scaleX, float scaleY, float bowlExponent,
                         float courtLength, float courtWidth, float courtAngle) {
        this.displayBounds = Objects.requireNonNull(displayBounds, "displayBounds");
        this.scaleX = positive(scaleX);
        this.scaleY = positive(scaleY);
        this.bowlExponent = positive(bowlExponent);
        if (bowlExponent < 2 || bowlExponent > 8) {
            throw new IllegalArgumentException("Bowl exponent must be in [2,8]");
        }
        this.courtLength = positive(courtLength);
        this.courtWidth = positive(courtWidth);
        this.courtAngle = ModelChecks.finite(courtAngle, "courtAngle");
    }

    private static float positive(float value) {
        if (!Float.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("Invalid geometry dimension");
        }
        return value;
    }
}
