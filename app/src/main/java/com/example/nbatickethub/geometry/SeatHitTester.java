package com.example.nbatickethub.geometry;

import com.example.nbatickethub.model.Seat;
import com.example.nbatickethub.model.SeatGeometry;
import com.example.nbatickethub.model.WorldRect;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable index over the final displayed quads, built once on the arena worker. */
public final class SeatHitTester {
    private final List<SeatGeometry> shapes;
    private final Grid[] sections;

    public SeatHitTester(ArenaScene scene) throws InterruptedIOException {
        this(scene.seats, sectionIds(scene));
    }

    // Package-visible fixture entry point: the same index handles synthetic and full arenas.
    SeatHitTester(List<SeatGeometry> shapes, int[] sectionIds) throws InterruptedIOException {
        if (shapes.size() != sectionIds.length) { throw new IllegalArgumentException("Section count mismatch"); }
        this.shapes = Collections.unmodifiableList(new ArrayList<>(shapes));
        Map<Integer, List<Integer>> grouped = new LinkedHashMap<>();
        for (int i = 0; i < shapes.size(); i++) {
            grouped.computeIfAbsent(sectionIds[i], key -> new ArrayList<>()).add(i);
        }
        sections = new Grid[grouped.size()];
        int index = 0;
        for (List<Integer> indices : grouped.values()) {
            checkCancelled();
            sections[index++] = new Grid(this.shapes, indices);
        }
    }

    /** Returns a geometry index, or -1 for aisles/court/outside. Never snaps to a nearby seat. */
    public int find(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) { return -1; }
        int found = -1;
        for (Grid section : sections) {
            if (!section.contains(x, y)) { continue; }
            int[] candidates = section.cells.get(cellKey(section.cellX(x), section.cellY(y)));
            if (candidates == null) { continue; }
            for (int candidate : candidates) {
                if ((found < 0 || candidate < found) && contains(shapes.get(candidate), x, y)) {
                    found = candidate;
                }
            }
        }
        return found;
    }

    /** Closed convex quad; exact shared edges deterministically resolve to the lowest index. */
    static boolean contains(SeatGeometry shape, float x, float y) {
        WorldRect bounds = shape.bounds;
        if (x < bounds.left || x > bounds.right || y < bounds.top || y > bounds.bottom) { return false; }
        boolean positive = false, negative = false;
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            double cross = ((double) shape.x(next) - shape.x(i)) * ((double) y - shape.y(i))
                    - ((double) shape.y(next) - shape.y(i)) * ((double) x - shape.x(i));
            positive |= cross > 0;
            negative |= cross < 0;
            if (positive && negative) { return false; }
        }
        return true;
    }

    private static int[] sectionIds(ArenaScene scene) {
        Map<String, Integer> ids = new LinkedHashMap<>();
        for (String id : scene.dataset.sections.keySet()) { ids.put(id, ids.size()); }
        int[] result = new int[scene.seats.size()];
        for (Seat seat : scene.dataset.seatOrder) {
            result[seat.geometryIndex] = ids.get(scene.dataset.rows.get(seat.rowId).sectionId);
        }
        return result;
    }

    private static long cellKey(int x, int y) { return ((long) x << 32) ^ (y & 0xffffffffL); }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) { throw new InterruptedIOException("Hit index cancelled"); }
    }

    private static final class Grid {
        private float left = Float.POSITIVE_INFINITY, top = Float.POSITIVE_INFINITY;
        private float right = Float.NEGATIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;
        private final float cellSize;
        private final Map<Long, int[]> cells = new HashMap<>();

        Grid(List<SeatGeometry> shapes, List<Integer> indices) throws InterruptedIOException {
            float[] sizes = new float[indices.size()];
            int position = 0;
            for (int index : indices) {
                WorldRect bounds = shapes.get(index).bounds;
                left = Math.min(left, bounds.left); top = Math.min(top, bounds.top);
                right = Math.max(right, bounds.right); bottom = Math.max(bottom, bounds.bottom);
                sizes[position++] = Math.max(bounds.right - bounds.left, bounds.bottom - bounds.top);
            }
            java.util.Arrays.sort(sizes);
            cellSize = sizes[sizes.length / 2] * 2;
            Map<Long, List<Integer>> pending = new HashMap<>();
            for (int index : indices) {
                checkCancelled();
                WorldRect bounds = shapes.get(index).bounds;
                // Include every cell the AABB touches, including the far boundary.
                for (int x = cellX(bounds.left); x <= cellX(bounds.right); x++) {
                    for (int y = cellY(bounds.top); y <= cellY(bounds.bottom); y++) {
                        pending.computeIfAbsent(cellKey(x, y), key -> new ArrayList<>()).add(index);
                    }
                }
            }
            for (Map.Entry<Long, List<Integer>> entry : pending.entrySet()) {
                checkCancelled();
                int[] values = new int[entry.getValue().size()];
                for (int i = 0; i < values.length; i++) { values[i] = entry.getValue().get(i); }
                cells.put(entry.getKey(), values);
            }
        }

        boolean contains(float x, float y) { return x >= left && x <= right && y >= top && y <= bottom; }
        int cellX(float x) { return (int) Math.floor(((double) x - left) / cellSize); }
        int cellY(float y) { return (int) Math.floor(((double) y - top) / cellSize); }
    }
}
