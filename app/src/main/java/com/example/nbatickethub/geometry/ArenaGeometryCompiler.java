package com.example.nbatickethub.geometry;

import com.example.nbatickethub.model.*;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Compiles deterministic logical rows/seats; no Canvas, camera or touch handling. */
public final class ArenaGeometryCompiler {
    public static final class SectionLayout {
        public final Section section;
        public final int rowCount;
        public final int seatsPerRow;

        public SectionLayout(Section section, int rowCount, int seatsPerRow) {
            this.section = Objects.requireNonNull(section, "section");
            if (rowCount <= 0 || seatsPerRow <= 0) {
                throw new IllegalArgumentException("Rows and seats per row must be positive");
            }
            this.rowCount = rowCount;
            this.seatsPerRow = seatsPerRow;
        }
    }

    public static final class Result {
        public final List<SeatRow> rows;
        public final List<Seat> seats;
        public final List<SeatGeometry> geometry;

        private Result(List<SeatRow> rows, List<Seat> seats, List<SeatGeometry> geometry) {
            this.rows = Collections.unmodifiableList(rows);
            this.seats = Collections.unmodifiableList(seats);
            this.geometry = Collections.unmodifiableList(geometry);
        }
    }

    public Result compile(List<SectionLayout> layouts, int seatBudget) throws InterruptedIOException {
        if (seatBudget <= 0) { throw new IllegalArgumentException("Seat budget must be positive"); }
        long capacity = 0;
        for (SectionLayout layout : layouts) {
            capacity = Math.addExact(capacity, (long) layout.rowCount * layout.seatsPerRow);
        }
        if (capacity > seatBudget) {
            throw new IllegalArgumentException("Generated capacity exceeds configured seat budget: "
                    + capacity + " > " + seatBudget);
        }
        List<SeatRow> rows = new ArrayList<>();
        List<Seat> seats = new ArrayList<>((int) capacity);
        List<SeatGeometry> geometry = new ArrayList<>((int) capacity);
        for (SectionLayout layout : layouts) {
            AnnularSector section = layout.section.boundary;
            float radialPitch = (section.outerRadius - section.innerRadius) / layout.rowCount;
            double angularPitch = (section.endDegrees - section.startDegrees) / layout.seatsPerRow;
            for (int r = 0; r < layout.rowCount; r++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException("Arena generation interrupted");
                }
                String rowId = layout.section.id + ":R" + (r + 1);
                float inner = section.innerRadius + radialPitch * r;
                float outer = r == layout.rowCount - 1 ? section.outerRadius
                        : section.innerRadius + radialPitch * (r + 1);
                AnnularSector rowBoundary = new AnnularSector(section.centerX, section.centerY,
                        inner, outer, section.startDegrees, section.endDegrees);
                rows.add(new SeatRow(rowId, layout.section.id, "R" + (r + 1), r + 1,
                        rowBoundary, seats.size(), layout.seatsPerRow));
                double radius = (inner + outer) / 2.0;
                // Clearance in radial and tangential directions also leaves visible aisles.
                double halfSize = Math.min(radialPitch * 0.28,
                        radius * Math.sin(Math.toRadians(angularPitch) / 2) * 0.4);
                for (int n = 0; n < layout.seatsPerRow; n++) {
                    double angle = Math.toRadians(section.startDegrees + angularPitch * (n + 0.5));
                    double x = section.centerX + radius * Math.cos(angle);
                    double y = section.centerY + radius * Math.sin(angle);
                    WorldRect bounds = new WorldRect((float) (x - halfSize), (float) (y - halfSize),
                            (float) (x + halfSize), (float) (y + halfSize));
                    int index = seats.size();
                    PositionKind kind = layout.section.category == Section.Category.STANDING
                            ? PositionKind.STANDING_SPOT : PositionKind.SEATED;
                    seats.add(new Seat(rowId + ":S" + (n + 1), rowId, n + 1, kind, index));
                    geometry.add(SeatGeometry.rectangle(bounds));
                }
            }
        }
        return new Result(rows, seats, geometry);
    }
}
