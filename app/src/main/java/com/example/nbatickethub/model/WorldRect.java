package com.example.nbatickethub.model;

/** Immutable logical bounds, independent of Android screen coordinates. */
public final class WorldRect {
    public final float left;
    public final float top;
    public final float right;
    public final float bottom;

    public WorldRect(float left, float top, float right, float bottom) {
        this.left = ModelChecks.finite(left, "left");
        this.top = ModelChecks.finite(top, "top");
        this.right = ModelChecks.finite(right, "right");
        this.bottom = ModelChecks.finite(bottom, "bottom");
        if (left >= right || top >= bottom) {
            throw new IllegalArgumentException("Bounds must have positive width and height");
        }
    }

    public float centerX() { return left / 2f + right / 2f; }
    public float centerY() { return top / 2f + bottom / 2f; }

    public boolean encloses(WorldRect other) {
        return other.left >= left && other.right <= right
                && other.top >= top && other.bottom <= bottom;
    }

    public boolean overlaps(WorldRect other) {
        return left < other.right && right > other.left
                && top < other.bottom && bottom > other.top;
    }
}

