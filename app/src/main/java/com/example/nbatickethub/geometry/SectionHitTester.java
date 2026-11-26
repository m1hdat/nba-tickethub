package com.example.nbatickethub.geometry;

/** Pure world-space section hit test, including nearest visible label hitboxes. */
public final class SectionHitTester {
    private SectionHitTester() { }

    public static int find(float x, float y, float radiusX, float radiusY,
                           float[][] polygons, float[] labels, boolean[] visible) {
        int label = -1;
        float nearest = Float.POSITIVE_INFINITY;
        for (int i = 0; i < polygons.length; i++) {
            if (!visible[i]) { continue; }
            float dx = x - labels[i * 2], dy = y - labels[i * 2 + 1];
            float distance = dx * dx + dy * dy;
            if (Math.abs(dx) <= radiusX && Math.abs(dy) <= radiusY && distance < nearest) {
                nearest = distance; label = i;
            }
        }
        if (label >= 0) { return label; }
        for (int i = 0; i < polygons.length; i++) {
            if (!visible[i]) { continue; }
            float[] polygon = polygons[i];
            boolean inside = false;
            for (int a = 0, b = polygon.length - 2; a < polygon.length; b = a, a += 2) {
                double cross = (double) (x - polygon[a]) * (polygon[b + 1] - polygon[a + 1])
                        - (double) (y - polygon[a + 1]) * (polygon[b] - polygon[a]);
                if (cross == 0 && x >= Math.min(polygon[a], polygon[b]) && x <= Math.max(polygon[a], polygon[b])
                        && y >= Math.min(polygon[a + 1], polygon[b + 1]) && y <= Math.max(polygon[a + 1], polygon[b + 1])) {
                    return i;
                }
                if ((polygon[a + 1] > y) != (polygon[b + 1] > y)
                        && x < (polygon[b] - polygon[a]) * (y - polygon[a + 1])
                        / (polygon[b + 1] - polygon[a + 1]) + polygon[a]) { inside = !inside; }
            }
            if (inside) { return i; }
        }
        return -1;
    }
}
