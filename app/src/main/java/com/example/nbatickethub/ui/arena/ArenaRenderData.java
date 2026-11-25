package com.example.nbatickethub.ui.arena;

import android.graphics.Path;
import android.graphics.RectF;
import com.example.nbatickethub.geometry.ArenaScene;
import com.example.nbatickethub.geometry.ArenaOutlines;
import com.example.nbatickethub.geometry.SeatPathBatches;
import com.example.nbatickethub.geometry.ArenaSeatMarkers;
import com.example.nbatickethub.geometry.SeatHitTester;
import com.example.nbatickethub.model.*;
import java.io.InterruptedIOException;
import java.util.*;

/** Worker-built paths, transferred once to the UI thread and never mutated after publication. */
public final class ArenaRenderData {
    final ArenaScene scene;
    final SeatHitTester hitTester;
    final Seat[] seatsByGeometry;
    final Path[] sectionPaths;
    final Path[] rowLines;
    final Path[] seatPaths;
    final int[] seatStatuses;
    final Path[] markerPaths;
    final Path[] statusDotPaths;
    final int[] seatLevelOrders;
    final int[] sectionLevelOrders;
    final RectF[] sectionBounds;
    final RectF[] seatBounds;
    final int[][] sectionBatches;
    final float minSeatWidth;
    final Path envelope = new Path();
    final String[] labels;
    final float[] labelPoints;

    public ArenaRenderData(ArenaScene scene) throws InterruptedIOException {
        this.scene = scene;
        hitTester = new SeatHitTester(scene);
        seatsByGeometry = new Seat[scene.seats.size()];
        for (Seat seat : scene.dataset.seatOrder) { seatsByGeometry[seat.geometryIndex] = seat; }
        sectionPaths = new Path[scene.dataset.sections.size()];
        sectionBounds = new RectF[sectionPaths.length];
        Map<String, Integer> sectionIndices = new LinkedHashMap<>();
        List<List<Integer>> batchesBySection = new ArrayList<>();
        float smallestSeat = Float.POSITIVE_INFINITY;
        for (SeatGeometry seat : scene.seats) {
            for (int vertex = 0; vertex < 4; vertex++) {
                int next = (vertex + 1) % 4;
                smallestSeat = Math.min(smallestSeat, (float) Math.hypot(
                        seat.x(next) - seat.x(vertex), seat.y(next) - seat.y(vertex)));
            }
        }
        minSeatWidth = smallestSeat;
        labels = new String[sectionPaths.length];
        sectionLevelOrders = new int[sectionPaths.length];
        labelPoints = new float[sectionPaths.length * 2];
        rowLines = new Path[scene.dataset.sections.size()];
        Map<String, Path> rowsBySection = new LinkedHashMap<>();
        float outer = 0;
        for (ArenaLevel level : scene.dataset.levels.values()) { outer = Math.max(outer, level.footprint.outerRadius); }
        AnnularSector full = new AnnularSector(scene.dataset.arena.worldBounds.centerX(),
                scene.dataset.arena.worldBounds.centerY(), 1, outer + 12, 0, 360);
        append(envelope, ArenaOutlines.arc(scene, full, outer + 12, false));
        envelope.close();
        int index = 0;
        for (Section section : scene.dataset.sections.values()) {
            Path path = new Path();
            append(path, ArenaOutlines.section(scene, section.boundary));
            path.close();
            sectionPaths[index] = path;
            sectionBounds[index] = new RectF();
            path.computeBounds(sectionBounds[index], true);
            sectionIndices.put(section.id, index);
            batchesBySection.add(new ArrayList<>());
            sectionLevelOrders[index] = scene.dataset.levels.get(section.levelId).order;
            labels[index] = section.code.replace("SIM-", "") + "*";
            float[] point = ArenaOutlines.label(scene, section.boundary);
            labelPoints[index * 2] = point[0];
            labelPoints[index * 2 + 1] = point[1];
            Path rows = new Path();
            rowLines[index] = rows;
            rowsBySection.put(section.id, rows);
            index++;
        }
        for (SeatRow row : scene.dataset.rows.values()) {
            append(rowsBySection.get(row.sectionId), ArenaOutlines.arc(scene, row.boundary,
                    (row.boundary.innerRadius + row.boundary.outerRadius) / 2, false));
        }
        List<SeatPathBatches.Batch> batches = SeatPathBatches.build(scene);
        seatPaths = new Path[batches.size()];
        seatBounds = new RectF[batches.size()];
        seatStatuses = new int[batches.size()];
        seatLevelOrders = new int[batches.size()];
        markerPaths = new Path[batches.size()];
        statusDotPaths = new Path[batches.size()];
        for (int b = 0; b < batches.size(); b++) {
            SeatPathBatches.Batch batch = batches.get(b);
            Path path = new Path();
            Path markers = new Path();
            Path dots = new Path();
            for (int i = 0; i < batch.size(); i++) {
                if (Thread.currentThread().isInterrupted()) { throw new InterruptedIOException("Render cache cancelled"); }
                SeatGeometry shape = scene.seats.get(batch.geometryIndex(i));
                path.moveTo(shape.x(0), shape.y(0));
                for (int vertex = 1; vertex < 4; vertex++) { path.lineTo(shape.x(vertex), shape.y(vertex)); }
                path.close();
                float[] marker = ArenaSeatMarkers.marker(shape);
                markers.addCircle(marker[0], marker[1], marker[2], Path.Direction.CW);
                dots.addCircle(marker[0], marker[1], marker[2] * .72f, Path.Direction.CW);
            }
            seatPaths[b] = path;
            seatBounds[b] = new RectF();
            path.computeBounds(seatBounds[b], true);
            batchesBySection.get(sectionIndices.get(batch.sectionId)).add(b);
            seatStatuses[b] = batch.status.ordinal();
            seatLevelOrders[b] = scene.dataset.levels.get(scene.dataset.sections.get(batch.sectionId).levelId).order;
            markerPaths[b] = markers;
            statusDotPaths[b] = dots;
        }
        sectionBatches = new int[sectionPaths.length][];
        for (int i = 0; i < sectionBatches.length; i++) {
            List<Integer> indices = batchesBySection.get(i);
            sectionBatches[i] = new int[indices.size()];
            for (int j = 0; j < indices.size(); j++) { sectionBatches[i][j] = indices.get(j); }
        }
    }

    private void append(Path path, float[] points) {
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) { path.lineTo(points[i], points[i + 1]); }
    }

    public String arenaId() { return scene.profile.id; }
    public int seatCount() { return scene.seats.size(); }
    public int sectionCount() { return scene.dataset.sections.size(); }
    public String homeTeamName() { return scene.profile.homeTeam.displayName; }
    public int seatBatchCount() { return seatPaths.length; }
    public int levelCount() { return scene.dataset.levels.size(); }
    public String levelName(int index) {
        return levelAt(index).code;
    }
    public int levelOrder(int index) { return levelAt(index).order; }
    public void validateStands() {
        for (ArenaLevel level : scene.dataset.levels.values()) { scene.profile.stands.tier(level.order); }
    }
    private ArenaLevel levelAt(int index) {
        int current = 0;
        for (ArenaLevel level : scene.dataset.levels.values()) {
            if (current++ == index) { return level; }
        }
        throw new IndexOutOfBoundsException("Unknown tier index: " + index);
    }
}
