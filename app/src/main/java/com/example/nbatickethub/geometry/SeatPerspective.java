package com.example.nbatickethub.geometry;

import com.example.nbatickethub.model.*;

/** Lightweight authored perspective, not a photograph or a calibrated arena sightline. */
public final class SeatPerspective {
    public final double eyeX, eyeY, eyeHeight;
    private final double fx, fy, fz, rx, ry, ux, uy, uz;

    public SeatPerspective(ArenaScene scene, Seat seat) {
        SeatRow row = scene.dataset.rows.get(seat.rowId);
        Section section = scene.dataset.sections.get(row.sectionId);
        int level = scene.dataset.levels.get(section.levelId).order;
        SeatGeometry quad = scene.seats.get(seat.geometryIndex);
        double x = 0, y = 0;
        for (int i = 0; i < 4; i++) { x += quad.x(i) / 4; y += quad.y(i) / 4; }
        ArenaGeometry arena = scene.profile.geometry;
        eyeX = (x - arena.displayBounds.centerX()) / arena.courtLength * 94;
        eyeY = (y - arena.displayBounds.centerY()) / arena.courtWidth * 50;
        eyeHeight = 4 + (level - 1) * 20 + row.order * .7;
        double distance = Math.sqrt(eyeX * eyeX + eyeY * eyeY + eyeHeight * eyeHeight);
        fx = -eyeX / distance; fy = -eyeY / distance; fz = -eyeHeight / distance;
        double horizontal = Math.hypot(fx, fy);
        rx = -fy / horizontal; ry = fx / horizontal;
        ux = -fz * ry; uy = fz * rx; uz = fx * ry - fy * rx;
    }

    /** Court-space feet → normalized preview, with the camera looking at center court. */
    public void project(float x, float y, float[] target, int offset) {
        project3d(x, y, 0, target, offset);
    }

    public void project3d(float x, float y, float z, float[] target, int offset) {
        double dx = x - 47 - eyeX, dy = y - 25 - eyeY, dz = z - eyeHeight;
        double depth = Math.max(.1, dx * fx + dy * fy + dz * fz);
        target[offset] = (float) (.5 + .68 * (dx * rx + dy * ry) / depth);
        target[offset + 1] = (float) (.54 - 1.21 * (dx * ux + dy * uy + dz * uz) / depth);
    }

    public float[] courtQuad() {
        float[] points = new float[8];
        project(0, 0, points, 0); project(94, 0, points, 2);
        project(94, 50, points, 4); project(0, 50, points, 6);
        return points;
    }
}
