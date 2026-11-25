package com.example.nbatickethub.geometry;

import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.MockArenaRepository;
import com.example.nbatickethub.model.*;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.Assert.*;

public class SeatHitTesterTest {
    private SeatGeometry rect(float left, float top, float right, float bottom) {
        return SeatGeometry.rectangle(new WorldRect(left, top, right, bottom));
    }

    @Test public void exactRotatedQuadRejectsEmptyCornersOfItsBoundingBox() throws Exception {
        SeatGeometry diamond = new SeatGeometry(new float[]{0, -2, 2, 0, 0, 2, -2, 0});
        SeatHitTester index = new SeatHitTester(Arrays.asList(diamond, rect(4, -1, 6, 1)), new int[]{0, 1});
        assertEquals(0, index.find(0, 0));
        assertEquals(-1, index.find(1.5f, 1.5f));
        assertEquals(-1, index.find(3, 0));
        assertEquals(1, index.find(5, 0));
        assertEquals(-1, index.find(Float.NaN, 0));
        assertEquals(-1, index.find(Float.POSITIVE_INFINITY, 0));
    }

    @Test public void closedEdgesAndCornersHaveDeterministicOwnershipWithoutExpandingHitbox() throws Exception {
        SeatHitTester index = new SeatHitTester(Arrays.asList(rect(-2, -2, 0, 0), rect(0, -2, 2, 0)),
                new int[]{0, 1});
        assertEquals(0, index.find(-2, -2));
        assertEquals(0, index.find(0, -1));
        assertEquals(0, index.find(0, 0));
        assertEquals(1, index.find(Math.nextUp(0f), -1));
        assertEquals(-1, index.find(Math.nextDown(-2f), -1));
        assertEquals(-1, index.find(2, Math.nextUp(0f)));
    }

    @Test public void gridIncludesLargeSeatsAcrossAllTheirCells() throws Exception {
        List<SeatGeometry> shapes = new ArrayList<>();
        shapes.add(rect(-40, -2, 40, 2));
        for (int i = 0; i < 20; i++) { shapes.add(rect(-30 + i * 3, 10, -29 + i * 3, 11)); }
        SeatHitTester index = new SeatHitTester(shapes, new int[shapes.size()]);
        for (float x = -40; x <= 40; x += .5f) { assertEquals(0, index.find(x, 1)); }
        assertEquals(-1, index.find(40.01f, 1));
    }

    @Test public void gridMatchesIndependentRayCrossingOracleWithRotationsAndGaps() throws Exception {
        List<SeatGeometry> shapes = new ArrayList<>();
        int[] groups = new int[240];
        for (int i = 0; i < groups.length; i++) {
            float cx = (i % 20) * 9 - 90, cy = (i / 20) * 8 - 48;
            double angle = i * .31;
            float[] points = new float[8];
            for (int v = 0; v < 4; v++) {
                double x = v == 0 || v == 3 ? -2 : 2;
                double y = v < 2 ? -1 : 1;
                points[2 * v] = cx + (float) (x * Math.cos(angle) - y * Math.sin(angle));
                points[2 * v + 1] = cy + (float) (x * Math.sin(angle) + y * Math.cos(angle));
            }
            shapes.add(new SeatGeometry(points));
            groups[i] = i % 4;
        }
        SeatHitTester index = new SeatHitTester(shapes, groups);
        Random random = new Random(3726);
        for (int sample = 0; sample < 5000; sample++) {
            float x = random.nextFloat() * 200 - 100, y = random.nextFloat() * 120 - 60;
            assertEquals(oracle(shapes, x, y), index.find(x, y));
        }
    }

    @Test public void allThirtyArenaIndexesFindEverySeatCenterAndRejectAislesAndCourt() throws Exception {
        Path assetsRoot = Path.of("src/main/assets");
        if (!Files.isDirectory(assetsRoot)) { assetsRoot = Path.of("app/src/main/assets"); }
        final Path root = assetsRoot;
        AssetSource assets = name -> Files.newInputStream(root.resolve(name));
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
        int checked = 0;
        for (ArenaProfile profile : catalog.profiles) {
            ArenaDataset dataset = new MockArenaRepository(assets).load(profile.dataAsset);
            ArenaScene scene = new ArenaSceneCompiler().compile(profile, dataset);
            SeatHitTester index = new SeatHitTester(scene);
            for (int i = 0; i < scene.seats.size(); i++) {
                SeatGeometry quad = scene.seats.get(i);
                float x = 0, y = 0;
                for (int v = 0; v < 4; v++) { x += quad.x(v) / 4; y += quad.y(v) / 4; }
                assertEquals(profile.id + " seat " + i, i, index.find(x, y));
                checked++;
            }
            assertEquals(-1, index.find(profile.geometry.displayBounds.centerX(), profile.geometry.displayBounds.centerY()));
            List<Section> sections = new ArrayList<>(dataset.sections.values());
            Section first = sections.get(0), next = sections.get(1);
            assertEquals(first.levelId, next.levelId);
            float angle = (first.boundary.endDegrees + next.boundary.startDegrees) / 2;
            float radius = (first.boundary.innerRadius + first.boundary.outerRadius) / 2;
            float[] aisle = new float[2];
            ArenaSceneCompiler.project(profile, dataset,
                    first.boundary.centerX + radius * (float) Math.cos(Math.toRadians(angle)),
                    first.boundary.centerY + radius * (float) Math.sin(Math.toRadians(angle)), aisle);
            assertEquals(profile.id + " aisle", -1, index.find(aisle[0], aisle[1]));
            Random random = new Random(37);
            for (int sample = 0; sample < 100; sample++) {
                WorldRect bounds = profile.geometry.displayBounds;
                float x = bounds.left + random.nextFloat() * (bounds.right - bounds.left);
                float y = bounds.top + random.nextFloat() * (bounds.bottom - bounds.top);
                assertEquals(profile.id, oracle(scene.seats, x, y), index.find(x, y));
            }
        }
        assertEquals(30, catalog.profiles.size());
        assertEquals(626904, checked);
    }

    @Test public void cancelledWorkerDoesNotPublishAnIndex() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(java.io.InterruptedIOException.class, () ->
                    new SeatHitTester(Collections.singletonList(rect(0, 0, 1, 1)), new int[]{0}));
        } finally { Thread.interrupted(); }
    }

    // Independent odd/even ray crossing, rather than the production cross-sign predicate.
    private int oracle(List<SeatGeometry> shapes, float x, float y) {
        for (int index = 0; index < shapes.size(); index++) {
            SeatGeometry shape = shapes.get(index);
            boolean inside = false;
            for (int i = 0, j = 3; i < 4; j = i++) {
                if ((shape.y(i) > y) != (shape.y(j) > y)
                        && x < (double) (shape.x(j) - shape.x(i)) * (y - shape.y(i))
                        / (shape.y(j) - shape.y(i)) + shape.x(i)) { inside = !inside; }
            }
            if (inside) { return index; }
        }
        return -1;
    }
}
