package com.example.nbatickethub.model;

/** Immutable convex quadrilateral, ready for later rendering; no touch hit-testing. */
public final class SeatGeometry {
    private final float[] vertices;
    public final WorldRect bounds;

    public SeatGeometry(float[] vertices) {
        if (vertices == null || vertices.length != 8) {
            throw new IllegalArgumentException("A seat requires four world-space vertices");
        }
        this.vertices = vertices.clone();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        double sign = 0;
        for (int i = 0; i < 4; i++) {
            float x = ModelChecks.finite(this.vertices[2 * i], "vertex x");
            float y = ModelChecks.finite(this.vertices[2 * i + 1], "vertex y");
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            int j = (i + 1) % 4;
            int k = (i + 2) % 4;
            double cross = (this.vertices[2 * j] - x)
                    * (this.vertices[2 * k + 1] - this.vertices[2 * j + 1])
                    - (this.vertices[2 * j + 1] - y)
                    * (this.vertices[2 * k] - this.vertices[2 * j]);
            if (!Double.isFinite(cross) || cross == 0 || (sign != 0 && sign * cross < 0)) {
                throw new IllegalArgumentException("Seat vertices must form a convex quadrilateral");
            }
            sign = cross;
        }
        this.bounds = new WorldRect(minX, minY, maxX, maxY);
    }

    public static SeatGeometry rectangle(WorldRect rect) {
        return new SeatGeometry(new float[]{rect.left, rect.top, rect.right, rect.top,
                rect.right, rect.bottom, rect.left, rect.bottom});
    }

    public float x(int vertex) {
        if (vertex < 0 || vertex >= 4) { throw new IndexOutOfBoundsException(); }
        return vertices[2 * vertex];
    }

    public float y(int vertex) {
        if (vertex < 0 || vertex >= 4) { throw new IndexOutOfBoundsException(); }
        return vertices[2 * vertex + 1];
    }

    public float[] copyVertices() { return vertices.clone(); }
}

