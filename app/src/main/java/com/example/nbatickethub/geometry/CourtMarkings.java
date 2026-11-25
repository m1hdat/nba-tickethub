package com.example.nbatickethub.geometry;

import com.example.nbatickethub.model.CourtDesign;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One canonical set of competition lines exported unchanged to both renderers. */
public final class CourtMarkings {
    private CourtMarkings() { }
    public static final List<CourtDesign.Primitive> ALL = build();
    private static List<CourtDesign.Primitive> build() {
        List<CourtDesign.Primitive> result = new ArrayList<>();
        line(result, false, 0, 0, 94, 0, 94, 50, 0, 50, 0, 0);
        line(result, false, 47, 0, 47, 50);
        arc(result, false, 47, 25, 6, 0, 360);
        arc(result, false, 47, 25, 2, 0, 360);
        for (int side = 0; side < 2; side++) {
            List<CourtDesign.Primitive> half = new ArrayList<>();
            line(half, false, 0, 17, 19, 17, 19, 33, 0, 33);
            arc(half, false, 19, 25, 6, 0, 360);
            arc(half, false, 5.25f, 25, 23.75f, -68, 68);
            line(half, false, 0, 3, 14.2f, 3);
            line(half, false, 0, 47, 14.2f, 47);
            arc(half, false, 5.25f, 25, 4, -90, 90);
            line(half, false, 4, 22, 4, 28);
            arc(half, true, 5.25f, 25, .75f, 0, 360);
            for (CourtDesign.Primitive p : half) {
                float[] points = p.copyPoints();
                if (side == 1) {
                    for (int i = 0; i < points.length; i += 2) {
                        points[i] = 94 - points[i]; points[i + 1] = 50 - points[i + 1];
                    }
                }
                result.add(new CourtDesign.Primitive(p.part, p.type, p.color, p.strokeWidth, points));
            }
        }
        return Collections.unmodifiableList(result);
    }
    private static void line(List<CourtDesign.Primitive> result, boolean rim, float... points) {
        result.add(new CourtDesign.Primitive(rim ? "key" : "decor", "polyline",
                rim ? 0xffe46b35 : 0xffffffff, .34f, points));
    }
    private static void arc(List<CourtDesign.Primitive> result, boolean rim, float x, float y,
                            float radius, float start, float end) {
        int steps = Math.max(8, (int) Math.ceil((end - start) / 4));
        float[] points = new float[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(start + (end - start) * i / steps);
            points[i * 2] = x + radius * (float) Math.cos(a);
            points[i * 2 + 1] = y + radius * (float) Math.sin(a);
        }
        line(result, rim, points);
    }
}
