package com.example.nbatickethub.geometry;

import com.example.nbatickethub.model.*;
import java.io.InterruptedIOException;
import java.util.*;

/** Bounds path complexity without dropping any seat or changing its world coordinates/status. */
public final class SeatPathBatches {
    public static final int MAX_SEATS_PER_PATH = 256;

    public static final class Batch {
        public final String sectionId;
        public final SeatStatus status;
        private final int[] geometryIndices;
        private Batch(String sectionId, SeatStatus status, List<Integer> indices) {
            this.sectionId = sectionId;
            this.status = status;
            geometryIndices = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) { geometryIndices[i] = indices.get(i); }
        }
        public int size() { return geometryIndices.length; }
        public int geometryIndex(int index) { return geometryIndices[index]; }
    }

    private SeatPathBatches() { }

    public static List<Batch> build(ArenaScene scene) throws InterruptedIOException {
        Map<String, EnumMap<SeatStatus, List<Integer>>> pending = new LinkedHashMap<>();
        List<Batch> result = new ArrayList<>();
        Map<String, GameSeat> inventory = scene.dataset.inventory.values().iterator().next();
        for (Seat seat : scene.dataset.seatOrder) {
            if (Thread.currentThread().isInterrupted()) { throw new InterruptedIOException("Batch cancelled"); }
            String sectionId = scene.dataset.rows.get(seat.rowId).sectionId;
            SeatStatus status = inventory.get(seat.id).baseStatus;
            EnumMap<SeatStatus, List<Integer>> byStatus = pending.computeIfAbsent(sectionId,
                    key -> new EnumMap<>(SeatStatus.class));
            List<Integer> indices = byStatus.computeIfAbsent(status, key -> new ArrayList<>(MAX_SEATS_PER_PATH));
            indices.add(seat.geometryIndex);
            if (indices.size() == MAX_SEATS_PER_PATH) {
                result.add(new Batch(sectionId, status, indices));
                indices.clear();
            }
        }
        pending.forEach((sectionId, byStatus) -> byStatus.forEach((status, indices) -> {
            if (!indices.isEmpty()) { result.add(new Batch(sectionId, status, indices)); }
        }));
        return Collections.unmodifiableList(result);
    }
}
