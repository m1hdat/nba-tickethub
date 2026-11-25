package com.example.nbatickethub.geometry;

import com.example.nbatickethub.data.ArenaDataset;
import com.example.nbatickethub.model.ArenaProfile;
import com.example.nbatickethub.model.SeatGeometry;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

/** One-time, JSON-configured superellipse projection; never called for pan/zoom or onDraw. */
public final class ArenaSceneCompiler {
    public ArenaScene compile(ArenaProfile profile, ArenaDataset data) throws InterruptedIOException {
        if (!profile.id.equals(data.arena.id)) {
            throw new IllegalArgumentException("Profile and dataset identity mismatch");
        }
        validateCourt(profile, data);
        List<SeatGeometry> projected = new ArrayList<>(data.capacity());
        float[] point = new float[2];
        for (SeatGeometry seat : data.geometry) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedIOException("Scene cancelled"); }
            float[] vertices = new float[8];
            for (int i = 0; i < 4; i++) {
                project(profile, data, seat.x(i), seat.y(i), point);
                vertices[2 * i] = point[0];
                vertices[2 * i + 1] = point[1];
            }
            SeatGeometry geometry = new SeatGeometry(vertices);
            if (!profile.geometry.displayBounds.encloses(geometry.bounds)) {
                throw new IllegalArgumentException("Projected seat outside display bounds");
            }
            projected.add(geometry);
        }
        return new ArenaScene(profile, data, projected);
    }

    private void validateCourt(ArenaProfile profile, ArenaDataset data) {
        double inner = Double.POSITIVE_INFINITY;
        for (com.example.nbatickethub.model.ArenaLevel level : data.levels.values()) {
            inner = Math.min(inner, level.footprint.innerRadius);
        }
        double angle = Math.toRadians(profile.geometry.courtAngle);
        // Include the four-foot colored apron drawn by ArenaView.
        double halfLength = profile.geometry.courtLength * 51 / 94;
        double halfWidth = profile.geometry.courtWidth * 29 / 50;
        for (int xSign : new int[]{-1, 1}) {
            for (int ySign : new int[]{-1, 1}) {
                double dx = xSign * halfLength * Math.cos(angle) - ySign * halfWidth * Math.sin(angle);
                double dy = xSign * halfLength * Math.sin(angle) + ySign * halfWidth * Math.cos(angle);
                double x = profile.geometry.displayBounds.centerX() + dx;
                double y = profile.geometry.displayBounds.centerY() + dy;
                double radius = Math.pow(Math.pow(Math.abs(dx / profile.geometry.scaleX), profile.geometry.bowlExponent)
                        + Math.pow(Math.abs(dy / profile.geometry.scaleY), profile.geometry.bowlExponent), 1 / profile.geometry.bowlExponent);
                // The superellipse norm is convex for n >= 2: containing the corners contains
                // the entire court rectangle, separating it from the stands' inner ring.
                if (radius >= inner || x < profile.geometry.displayBounds.left || x > profile.geometry.displayBounds.right
                        || y < profile.geometry.displayBounds.top || y > profile.geometry.displayBounds.bottom) {
                    throw new IllegalArgumentException("Court/apron outside central arena clearance");
                }
            }
        }
    }

    public static void project(ArenaProfile profile, ArenaDataset data, float x, float y, float[] result) {
        double dx = x - data.arena.worldBounds.centerX();
        double dy = y - data.arena.worldBounds.centerY();
        double radius = Math.hypot(dx, dy);
        double angle = Math.atan2(dy, dx);
        double cosine = Math.cos(angle), sine = Math.sin(angle);
        // Intersect each polar ray with a superellipse. Unlike the former angle-power
        // mapping, this has no infinite angular derivative at the axes: sideline rows
        // stay straight/rounded and section boundaries do not bunch into thin spikes.
        double boundary = Math.pow(Math.pow(Math.abs(cosine), profile.geometry.bowlExponent)
                + Math.pow(Math.abs(sine), profile.geometry.bowlExponent), -1.0 / profile.geometry.bowlExponent);
        result[0] = (float) (profile.geometry.displayBounds.centerX() + radius * profile.geometry.scaleX
                * cosine * boundary);
        result[1] = (float) (profile.geometry.displayBounds.centerY() + radius * profile.geometry.scaleY
                * sine * boundary);
    }
}
