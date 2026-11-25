package com.example.nbatickethub.geometry;

import com.example.nbatickethub.model.SeatGeometry;

/** World-space center/radius for a small common status dot, inside each themed seat base. */
public final class ArenaSeatMarkers {
    private ArenaSeatMarkers() { }
    public static float[] marker(SeatGeometry seat) {
        float x = 0, y = 0, edge = Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            x += seat.x(i) / 4;
            y += seat.y(i) / 4;
            int next = (i + 1) % 4;
            edge = Math.min(edge, (float) Math.hypot(seat.x(i) - seat.x(next), seat.y(i) - seat.y(next)));
        }
        // Small enough to remain inside the conservative near-rectangular seat quad.
        return new float[]{x, y, edge * .22f};
    }
}
