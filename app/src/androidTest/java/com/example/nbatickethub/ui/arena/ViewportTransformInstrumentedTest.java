package com.example.nbatickethub.ui.arena;

import android.graphics.RectF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Uses the real Android Matrix; a JVM android.jar stub cannot prove these operations. */
@RunWith(AndroidJUnit4.class)
public class ViewportTransformInstrumentedTest {
    private ViewportTransform camera() {
        ViewportTransform camera = new ViewportTransform();
        camera.configure(100, -50, 1100, 750, 0, 0, 1000, 800, 20, true);
        return camera;
    }

    @Test public void fitsNonZeroBoundsAndRoundTrips() {
        ViewportTransform camera = camera();
        float[] corners = {100, -50, 1100, 750};
        camera.worldToScreen(corners);
        assertArrayEquals(new float[]{50, 40, 950, 760}, corners, .001f);
        camera.screenToWorld(corners);
        assertArrayEquals(new float[]{100, -50, 1100, 750}, corners, .001f);
        camera.zoom(8, 500, 400, 500, 400);
        camera.pan(-123, 97);
        float[] points = {150, -25, 900, 700, 600, 350};
        camera.worldToScreen(points);
        camera.screenToWorld(points);
        assertArrayEquals(new float[]{150, -25, 900, 700, 600, 350}, points, .002f);
    }

    @Test public void movingMidpointKeepsWorldAnchorIncludingClampedScale() {
        ViewportTransform camera = camera();
        camera.zoom(5, 500, 400, 500, 400);
        float[] anchor = {450, 360};
        camera.screenToWorld(anchor);
        camera.zoom(2, 450, 360, 480, 390);
        camera.worldToScreen(anchor);
        assertArrayEquals(new float[]{480, 390}, anchor, .002f);
        float[] clampedAnchor = {500, 400};
        camera.screenToWorld(clampedAnchor);
        camera.zoom(10000, 500, 400, 500, 400);
        assertEquals(20, camera.scale(), .001f);
        camera.worldToScreen(clampedAnchor);
        assertArrayEquals(new float[]{500, 400}, clampedAnchor, .002f);
    }

    @Test public void clampsPanZoomAndResetsExactly() {
        ViewportTransform camera = camera();
        camera.zoom(10000, 500, 400, 500, 400);
        camera.pan(1e7f, -1e7f);
        float[] bounds = {100, -50, 1100, 750};
        camera.worldToScreen(bounds);
        assertEquals(0, bounds[0], .001f);
        assertEquals(800, bounds[3], .001f);
        camera.pan(-2e7f, 2e7f);
        bounds = new float[]{100, -50, 1100, 750};
        camera.worldToScreen(bounds);
        assertEquals(1000, bounds[2], .001f);
        assertEquals(0, bounds[1], .001f);
        camera.zoom(.00001f, 500, 400, 500, 400);
        assertEquals(camera.fitScale(), camera.scale(), .001f);
        camera.pan(1000, -1000);
        bounds = new float[]{100, -50, 1100, 750};
        camera.worldToScreen(bounds);
        assertArrayEquals(new float[]{50, 40, 950, 760}, bounds, .001f);
        camera.zoom(4, 500, 400, 500, 400);
        camera.reset();
        assertEquals(.9f, camera.scale(), .001f);
    }

    @Test public void resizePreservesCenterAndRatioNewArenaRefits() {
        ViewportTransform camera = camera();
        camera.zoom(4, 500, 400, 500, 400);
        camera.pan(70, 30);
        float[] center = {500, 400};
        camera.screenToWorld(center);
        camera.configure(100, -50, 1100, 750, 0, 0, 800, 1000, 20, false);
        float[] resizedCenter = {400, 500};
        camera.screenToWorld(resizedCenter);
        assertArrayEquals(center, resizedCenter, .002f);
        assertEquals(4, camera.scale() / camera.fitScale(), .001f);
        camera.configure(-400, 200, 1600, 1200, 0, 0, 800, 1000, 30, true);
        float[] newCenter = {600, 700};
        camera.worldToScreen(newCenter);
        assertArrayEquals(new float[]{400, 500}, newCenter, .001f);
        assertEquals(camera.fitScale(), camera.scale(), .001f);
        RectF visible = new RectF();
        camera.visibleWorld(visible);
        assertTrue(visible.contains(-400, 200, 1600, 1200));
    }

    @Test public void animatedResetEndsAtTheExactFitCamera() {
        ViewportTransform camera = camera();
        camera.zoom(8, 500, 400, 500, 400);
        camera.pan(100, -80);
        float scale = camera.scale(), x = camera.translationX(), y = camera.translationY();
        camera.animateToFit(.5f, scale, x, y);
        assertTrue(camera.scale() > camera.fitScale());
        camera.animateToFit(1, scale, x, y);
        assertEquals(camera.fitScale(), camera.scale(), .001f);
        float[] bounds = {100, -50, 1100, 750};
        camera.worldToScreen(bounds);
        assertArrayEquals(new float[]{50, 40, 950, 760}, bounds, .001f);
    }

    @Test public void semanticSnapshotRestoresWorldCenterAndZoomAfterRecreation() {
        ViewportTransform original = camera();
        original.zoom(6, 500, 400, 500, 400); original.pan(50, -20);
        float[] saved = original.snapshot();
        ViewportTransform recreated = new ViewportTransform();
        recreated.configure(100, -50, 1100, 750, 0, 0, 800, 1000, 20, true);
        recreated.restore(saved);
        assertArrayEquals(saved, recreated.snapshot(), .002f);
    }
}
