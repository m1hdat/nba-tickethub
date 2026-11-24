package com.example.nbatickethub.model;

/** Mock stand/row footprint. Degrees run clockwise in a y-down logical world. */
public final class AnnularSector {
    public final float centerX;
    public final float centerY;
    public final float innerRadius;
    public final float outerRadius;
    public final float startDegrees;
    public final float endDegrees;
    public final WorldRect bounds;

    public AnnularSector(float centerX, float centerY, float innerRadius,
                         float outerRadius, float startDegrees, float endDegrees) {
        this.centerX = ModelChecks.finite(centerX, "centerX");
        this.centerY = ModelChecks.finite(centerY, "centerY");
        this.innerRadius = ModelChecks.finite(innerRadius, "innerRadius");
        this.outerRadius = ModelChecks.finite(outerRadius, "outerRadius");
        this.startDegrees = ModelChecks.finite(startDegrees, "startDegrees");
        this.endDegrees = ModelChecks.finite(endDegrees, "endDegrees");
        if (innerRadius <= 0 || outerRadius <= innerRadius
                || startDegrees < 0 || endDegrees > 360 || startDegrees >= endDegrees) {
            throw new IllegalArgumentException("Invalid annular sector");
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double[] angles = {startDegrees, endDegrees, 0, 90, 180, 270, 360};
        for (double angle : angles) {
            if (angle < startDegrees || angle > endDegrees) { continue; }
            double radians = Math.toRadians(angle);
            for (float radius : new float[]{innerRadius, outerRadius}) {
                double x = centerX + radius * Math.cos(radians);
                double y = centerY + radius * Math.sin(radians);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        // Round outwards so float conversion never excludes the exact geometry.
        this.bounds = new WorldRect(Math.nextDown((float) minX), Math.nextDown((float) minY),
                Math.nextUp((float) maxX), Math.nextUp((float) maxY));
    }
}

