package com.example.nbatickethub.geometry;

import com.example.nbatickethub.model.AnnularSector;
import java.io.InterruptedIOException;

/** Shared logical geometry for Android paths and the standalone HTML exporter. No graphics API. */
public final class ArenaOutlines {
    private ArenaOutlines() { }

    public static float[] arc(ArenaScene scene, AnnularSector sector, float radius, boolean reverse)
            throws InterruptedIOException {
        int steps = Math.max(8, (int) Math.ceil((sector.endDegrees - sector.startDegrees) / 2));
        float[] points = new float[(steps + 1) * 2];
        float[] point = new float[2];
        for (int step = 0; step <= steps; step++) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedIOException("Outline cancelled"); }
            double fraction = (double) step / steps;
            double degrees = reverse
                    ? sector.endDegrees - fraction * (sector.endDegrees - sector.startDegrees)
                    : sector.startDegrees + fraction * (sector.endDegrees - sector.startDegrees);
            double angle = Math.toRadians(degrees);
            ArenaSceneCompiler.project(scene.profile, scene.dataset,
                    (float) (sector.centerX + radius * Math.cos(angle)),
                    (float) (sector.centerY + radius * Math.sin(angle)), point);
            points[step * 2] = point[0];
            points[step * 2 + 1] = point[1];
        }
        return points;
    }

    public static float[] section(ArenaScene scene, AnnularSector sector) throws InterruptedIOException {
        float[] outer = arc(scene, sector, sector.outerRadius, false);
        float[] inner = arc(scene, sector, sector.innerRadius, true);
        float[] polygon = new float[outer.length + inner.length];
        System.arraycopy(outer, 0, polygon, 0, outer.length);
        System.arraycopy(inner, 0, polygon, outer.length, inner.length);
        return polygon;
    }

    public static float[] label(ArenaScene scene, AnnularSector sector) {
        double angle = Math.toRadians((sector.startDegrees + sector.endDegrees) / 2);
        double radius = (sector.innerRadius + sector.outerRadius) / 2;
        float[] point = new float[2];
        ArenaSceneCompiler.project(scene.profile, scene.dataset,
                (float) (sector.centerX + radius * Math.cos(angle)),
                (float) (sector.centerY + radius * Math.sin(angle)), point);
        return point;
    }
}
