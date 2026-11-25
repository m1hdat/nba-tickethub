package com.example.nbatickethub.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Authored vector artwork in 94x50 court units; independent of venue seating geometry. */
public final class CourtDesign {
    public static final class Primitive {
        public final String part, type;
        public final int color;
        public final float strokeWidth;
        private final float[] points;

        public Primitive(String part, String type, int color, float strokeWidth, float[] points) {
            if (!part.matches("wood|decor|key|border") || !type.matches("polygon|polyline|ellipse")) {
                throw new IllegalArgumentException("Unknown court primitive");
            }
            if (points.length % 2 != 0 || points.length > 256
                    || (type.equals("polygon") ? points.length < 6 : points.length < 4)
                    || (type.equals("ellipse") && points.length != 4)) {
                throw new IllegalArgumentException("Invalid primitive points");
            }
            for (int i = 0; i < points.length; i++) {
                if (!Float.isFinite(points[i]) || points[i] < -6
                        || points[i] > (i % 2 == 0 ? 100 : 56)) {
                    throw new IllegalArgumentException("Court artwork outside allowed bounds");
                }
            }
            if (type.equals("ellipse") && (points[2] <= points[0] || points[3] <= points[1])) {
                throw new IllegalArgumentException("Invalid ellipse");
            }
            if (!Float.isFinite(strokeWidth) || strokeWidth < 0 || strokeWidth > 4) {
                throw new IllegalArgumentException("Invalid artwork stroke");
            }
            this.part = part;
            this.type = type;
            this.color = color;
            this.strokeWidth = strokeWidth;
            this.points = points.clone();
        }
        public float[] copyPoints() { return points.clone(); }
    }

    public static final class Logo {
        public final String asset, source, license, fallback;
        public final float width, height;
        public Logo(String asset, String source, String license, String fallback, float width, float height) {
            if (asset != null && (!asset.matches("logos/[a-z0-9_-]+\\.png")
                    || source == null || !source.startsWith("https://") || license == null || license.isEmpty())) {
                throw new IllegalArgumentException("Logo asset requires a local PNG path and usage provenance");
            }
            this.fallback = ModelChecks.text(fallback, "logo fallback");
            if (!fallback.matches("[A-Z0-9]{2,4}") || !Float.isFinite(width) || !Float.isFinite(height)
                    || width < 6 || height < 6 || width > 28 || height > 28) {
                throw new IllegalArgumentException("Invalid center mark");
            }
            this.asset = asset;
            this.source = source;
            this.license = license;
            this.width = width;
            this.height = height;
        }
        /** Aspect-preserving contain rectangle, exactly centered at (47,25). */
        public float[] fit(int imageWidth, int imageHeight) {
            if (imageWidth <= 0 || imageHeight <= 0) { throw new IllegalArgumentException("Empty logo image"); }
            float scale = Math.min(width / imageWidth, height / imageHeight);
            float w = imageWidth * scale, h = imageHeight * scale;
            return new float[]{47 - w / 2, 25 - h / 2, 47 + w / 2, 25 + h / 2};
        }
    }

    public final String woodFeature, graphicFeature;
    public final List<Primitive> primitives;
    public final Logo logo;

    public CourtDesign(String woodFeature, String graphicFeature, List<Primitive> primitives, Logo logo) {
        this.woodFeature = ModelChecks.text(woodFeature, "wood feature");
        this.graphicFeature = ModelChecks.text(graphicFeature, "graphic feature");
        if (woodFeature.equals(graphicFeature) || primitives.isEmpty() || primitives.size() > 600
                || primitives.contains(null) || logo == null) {
            throw new IllegalArgumentException("Court needs two features, artwork and a center mark");
        }
        boolean wood = false, graphic = false;
        for (Primitive p : primitives) {
            wood |= p.part.equals("wood");
            graphic |= p.part.equals("decor");
        }
        if (!wood || !graphic) { throw new IllegalArgumentException("Missing wood/graphic artwork"); }
        this.primitives = Collections.unmodifiableList(new ArrayList<>(primitives));
        this.logo = logo;
    }
}
