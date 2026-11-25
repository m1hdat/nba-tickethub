package com.example.nbatickethub.ui.arena;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

/** Uniform world-to-view camera. No seat coordinates are modified. UI thread only. */
public final class ViewportTransform {
    private final Matrix matrix = new Matrix();
    private final Matrix inverse = new Matrix();
    private final float[] values = new float[9];
    private final float[] anchor = new float[2];
    private final RectF world = new RectF();
    private final RectF viewport = new RectF();
    private float scale = 1, fitScale = 1, maxScale = 1, tx, ty;
    private boolean ready;

    public boolean isReady() { return ready; }
    public float scale() { return scale; }
    public float fitScale() { return fitScale; }
    public float translationX() { return tx; }
    public float translationY() { return ty; }

    /** Reconfigure after a new arena or layout; retain world center/relative zoom on resize. */
    public void configure(float left, float top, float right, float bottom,
            float viewLeft, float viewTop, float viewRight, float viewBottom,
            float detailScale, boolean reset) {
        if (right <= left || bottom <= top || viewRight <= viewLeft || viewBottom <= viewTop) {
            ready = false;
            return;
        }
        float ratio = scale / fitScale;
        anchor[0] = viewport.centerX();
        anchor[1] = viewport.centerY();
        inverse.mapPoints(anchor);
        world.set(left, top, right, bottom);
        viewport.set(viewLeft, viewTop, viewRight, viewBottom);
        fitScale = .9f * Math.min(viewport.width() / world.width(), viewport.height() / world.height());
        maxScale = Math.max(fitScale, detailScale);
        boolean wasReady = ready;
        ready = true;
        if (reset || !wasReady) { reset(); }
        else {
            scale = Math.max(fitScale, Math.min(maxScale, fitScale * ratio));
            tx = viewport.centerX() - scale * anchor[0];
            ty = viewport.centerY() - scale * anchor[1];
            update();
        }
    }

    public void reset() {
        if (!ready) { return; }
        scale = fitScale;
        tx = viewport.centerX() - scale * world.centerX();
        ty = viewport.centerY() - scale * world.centerY();
        update();
    }

    public void pan(float dx, float dy) {
        if (!ready || !Float.isFinite(dx) || !Float.isFinite(dy)) { return; }
        tx += dx;
        ty += dy;
        update();
    }

    /** Map the previous focal point through the inverse, then anchor it at the new midpoint. */
    public void zoom(float factor, float oldX, float oldY, float newX, float newY) {
        if (!ready || !Float.isFinite(factor) || factor <= 0) { return; }
        anchor[0] = oldX;
        anchor[1] = oldY;
        inverse.mapPoints(anchor);
        scale = Math.max(fitScale, Math.min(maxScale, scale * factor));
        tx = newX - scale * anchor[0];
        ty = newY - scale * anchor[1];
        update();
    }

    void animateToFit(float fraction, float startScale, float startX, float startY) {
        scale = startScale + (fitScale - startScale) * fraction;
        tx = startX + (viewport.centerX() - fitScale * world.centerX() - startX) * fraction;
        ty = startY + (viewport.centerY() - fitScale * world.centerY() - startY) * fraction;
        update();
    }

    public void concat(Canvas canvas) { canvas.concat(matrix); }
    public void worldToScreen(float[] points) { matrix.mapPoints(points); }
    public void screenToWorld(float[] points) { inverse.mapPoints(points); }
    public void visibleWorld(RectF result) {
        result.set(viewport);
        inverse.mapRect(result);
    }

    private void update() {
        tx = clampTranslation(tx, world.left, world.right, viewport.left, viewport.right);
        ty = clampTranslation(ty, world.top, world.bottom, viewport.top, viewport.bottom);
        values[0] = scale; values[1] = 0; values[2] = tx;
        values[3] = 0; values[4] = scale; values[5] = ty;
        values[6] = 0; values[7] = 0; values[8] = 1;
        matrix.setValues(values);
        if (!matrix.invert(inverse)) {
            ready = false;
            throw new IllegalStateException("Non-invertible arena camera");
        }
    }

    private float clampTranslation(float value, float low, float high, float start, float end) {
        if ((high - low) * scale <= end - start) {
            return (start + end - scale * (low + high)) / 2;
        }
        return Math.max(end - scale * high, Math.min(start - scale * low, value));
    }
}
