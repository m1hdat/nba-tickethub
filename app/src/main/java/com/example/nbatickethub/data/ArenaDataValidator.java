package com.example.nbatickethub.data;

import com.example.nbatickethub.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Data integrity only. Does not implement screen/touch hit-testing or seat selection. */
public final class ArenaDataValidator {
    private static final double GEOMETRY_TOLERANCE = 0.001;

    private ArenaDataValidator() { }

    public static void validate(ArenaDataset data) {
        require(data.arena.schemaVersion == 1, "Unsupported schema version");
        require(!data.games.isEmpty() && !data.levels.isEmpty() && !data.sections.isEmpty()
                && !data.rows.isEmpty() && !data.seats.isEmpty(), "Empty arena hierarchy");
        require(data.arena.courtBounds.centerX() == data.arena.worldBounds.centerX()
                && data.arena.courtBounds.centerY() == data.arena.worldBounds.centerY(),
                "Court must be at arena center");
        Set<String> unique = new HashSet<>();
        for (ArenaLevel level : data.levels.values()) {
            require(level.arenaId.equals(data.arena.id), "Unknown arena for level " + level.id);
            require(data.arena.worldBounds.encloses(level.footprint.bounds), "Level outside arena");
            require(unique.add("code:" + level.code) && unique.add("order:" + level.order),
                    "Duplicate level code/order");
            require(level.footprint.centerX == data.arena.worldBounds.centerX()
                    && level.footprint.centerY == data.arena.worldBounds.centerY(),
                    "Level must share arena center");
        }
        List<ArenaLevel> levels = new ArrayList<>(data.levels.values());
        for (int i = 0; i < levels.size(); i++) {
            for (int j = i + 1; j < levels.size(); j++) {
                require(!sectorOverlap(levels.get(i).footprint, levels.get(j).footprint),
                        "Unfolded arena levels overlap");
            }
        }
        Map<String, List<AnnularSector>> siblingSections = new HashMap<>();
        Set<String> sectionCodes = new HashSet<>();
        for (Section section : data.sections.values()) {
            ArenaLevel level = data.levels.get(section.levelId);
            require(level != null, "Unknown level for section " + section.id);
            require(encloses(level.footprint, section.boundary), "Section outside level");
            require(sectionCodes.add(section.levelId + "/" + section.code),
                    "Duplicate section code within level");
            List<AnnularSector> siblings = siblingSections.computeIfAbsent(section.levelId,
                    key -> new ArrayList<>());
            for (AnnularSector other : siblings) {
                require(!sectorOverlap(other, section.boundary), "Sections overlap");
            }
            siblings.add(section.boundary);
        }
        for (ArenaLevel level : levels) {
            require(siblingSections.containsKey(level.id), "Level has no sections");
        }
        require(data.geometry.size() == data.seats.size(), "Geometry/seat count mismatch");
        Set<String> rowLabels = new HashSet<>();
        Set<String> rowOrders = new HashSet<>();
        Set<String> sectionsWithRows = new HashSet<>();
        Map<String, Float> lastRowOuterRadius = new HashMap<>();
        Map<String, Integer> lastRowOrder = new HashMap<>();
        int expectedIndex = 0;
        for (SeatRow row : data.rows.values()) {
            Section section = data.sections.get(row.sectionId);
            require(section != null, "Unknown section for row " + row.id);
            require(encloses(section.boundary, row.boundary), "Row outside section");
            require(rowLabels.add(row.sectionId + "/" + row.rowLabel)
                    && rowOrders.add(row.sectionId + "/" + row.order), "Duplicate row label/order");
            Float previousOuterRadius = lastRowOuterRadius.put(row.sectionId, row.boundary.outerRadius);
            Integer previousOrder = lastRowOrder.put(row.sectionId, row.order);
            require(previousOuterRadius == null || row.boundary.innerRadius >= previousOuterRadius,
                    "Row footprints overlap or are out of radial order");
            require(previousOrder == null || row.order > previousOrder, "Rows are out of order");
            sectionsWithRows.add(section.id);
            require(row.firstSeatIndex == expectedIndex, "Row ranges must be contiguous");
            require((long) expectedIndex + row.seatCount <= data.seatOrder.size(),
                    "Row range exceeds seat array");
            Set<Integer> numbers = new HashSet<>();
            for (int i = expectedIndex; i < expectedIndex + row.seatCount; i++) {
                Seat seat = data.seatOrder.get(i);
                require(seat.rowId.equals(row.id), "Seat belongs to another row");
                require(seat.geometryIndex == i, "Geometry index does not match seat order");
                require(numbers.add(seat.positionNumber), "Duplicate seat number in row");
                require((seat.kind == PositionKind.STANDING_SPOT)
                        == (section.category == Section.Category.STANDING), "Wrong position kind");
                SeatGeometry geometry = data.geometry.get(i);
                require(geometry != null, "Missing seat geometry");
                require(data.arena.worldBounds.encloses(geometry.bounds), "Seat outside arena");
                for (int vertex = 0; vertex < 4; vertex++) {
                    require(vertexInside(row.boundary, geometry.x(vertex), geometry.y(vertex)),
                            "Seat geometry outside row " + seat.id);
                }
                require(edgesInside(row.boundary, geometry), "Seat edge outside row " + seat.id);
            }
            expectedIndex += row.seatCount;
        }
        require(expectedIndex == data.seatOrder.size(), "Unassigned seats");
        require(sectionsWithRows.size() == data.sections.size(), "Section has no rows");
        validateGeometryOverlap(data);
        require(data.inventory.size() == data.games.size(), "Inventory game count mismatch");
        for (Game game : data.games.values()) {
            require(data.teams.containsKey(game.homeTeamId)
                    && data.teams.containsKey(game.awayTeamId), "Unknown team in game " + game.id);
            require(game.arenaId.equals(data.arena.id), "Game uses another arena");
            Map<String, GameSeat> offers = data.inventory.get(game.id);
            require(offers != null && offers.size() == data.capacity(), "Incomplete game inventory");
            String currency = null;
            for (GameSeat offer : offers.values()) {
                require(data.seats.containsKey(offer.seatId), "Offer references unknown seat");
                if (currency == null) { currency = offer.price.currencyCode; }
                require(currency.equals(offer.price.currencyCode), "Mixed currencies within game");
            }
        }
    }

    private static boolean encloses(AnnularSector outer, AnnularSector inner) {
        return outer.centerX == inner.centerX && outer.centerY == inner.centerY
                && inner.innerRadius >= outer.innerRadius && inner.outerRadius <= outer.outerRadius
                && inner.startDegrees >= outer.startDegrees && inner.endDegrees <= outer.endDegrees;
    }

    private static boolean sectorOverlap(AnnularSector a, AnnularSector b) {
        return a.innerRadius < b.outerRadius && a.outerRadius > b.innerRadius
                && a.startDegrees < b.endDegrees && a.endDegrees > b.startDegrees;
    }

    private static boolean vertexInside(AnnularSector sector, float x, float y) {
        double dx = x - sector.centerX;
        double dy = y - sector.centerY;
        double radius = Math.hypot(dx, dy);
        double degrees = (Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360;
        return radius >= sector.innerRadius - GEOMETRY_TOLERANCE
                && radius <= sector.outerRadius + GEOMETRY_TOLERANCE
                && degrees >= sector.startDegrees - GEOMETRY_TOLERANCE
                && degrees <= sector.endDegrees + GEOMETRY_TOLERANCE;
    }

    private static boolean edgesInside(AnnularSector sector, SeatGeometry geometry) {
        // Corners outside the inner circle do not imply that the entire edge is outside it.
        // Also split at the angular boundary rays so reflex sectors are handled correctly.
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            double ax = geometry.x(i) - sector.centerX, ay = geometry.y(i) - sector.centerY;
            double bx = geometry.x(j) - sector.centerX, by = geometry.y(j) - sector.centerY;
            double dx = bx - ax, dy = by - ay;
            double lengthSquared = dx * dx + dy * dy;
            double t = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1,
                    -(ax * dx + ay * dy) / lengthSquared));
            if (Math.hypot(ax + t * dx, ay + t * dy)
                    < sector.innerRadius - GEOMETRY_TOLERANCE) { return false; }
            double[] cuts = {0, 1, 1, 1};
            int count = 2;
            for (double boundary : new double[]{sector.startDegrees, sector.endDegrees}) {
                double rx = Math.cos(Math.toRadians(boundary));
                double ry = Math.sin(Math.toRadians(boundary));
                double cross = dx * ry - dy * rx;
                if (Math.abs(cross) > 1e-12) {
                    double intersection = -(ax * ry - ay * rx) / cross;
                    if (intersection > 0 && intersection < 1) { cuts[count++] = intersection; }
                }
            }
            java.util.Arrays.sort(cuts, 0, count);
            for (int c = 0; c < count - 1; c++) {
                double middle = (cuts[c] + cuts[c + 1]) / 2;
                if (!vertexInside(sector, (float) (sector.centerX + ax + middle * dx),
                        (float) (sector.centerY + ay + middle * dy))) { return false; }
            }
        }
        return true;
    }

    private static void validateGeometryOverlap(ArenaDataset data) {
        // Private broad phase for validation, not the future interactive spatial index.
        float cellSize = 0;
        for (SeatGeometry geometry : data.geometry) {
            cellSize = Math.max(cellSize, Math.max(geometry.bounds.right - geometry.bounds.left,
                    geometry.bounds.bottom - geometry.bounds.top));
        }
        require(cellSize > 0, "No usable seat geometry");
        Map<Long, List<Integer>> cells = new HashMap<>();
        SeatGeometry court = SeatGeometry.rectangle(data.arena.courtBounds);
        for (int i = 0; i < data.geometry.size(); i++) {
            SeatGeometry seat = data.geometry.get(i);
            require(!interiorsOverlap(seat, court), "Seat overlaps court");
            int minX = (int) Math.floor(seat.bounds.left / cellSize);
            int maxX = (int) Math.floor(seat.bounds.right / cellSize);
            int minY = (int) Math.floor(seat.bounds.top / cellSize);
            int maxY = (int) Math.floor(seat.bounds.bottom / cellSize);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    long key = ((long) x << 32) ^ (y & 0xffffffffL);
                    List<Integer> occupants = cells.computeIfAbsent(key, ignored -> new ArrayList<>());
                    for (int other : occupants) {
                        require(!interiorsOverlap(seat, data.geometry.get(other)),
                                "Seats overlap: " + data.seatOrder.get(i).id + " and "
                                        + data.seatOrder.get(other).id);
                    }
                    occupants.add(i);
                }
            }
        }
    }

    private static boolean interiorsOverlap(SeatGeometry a, SeatGeometry b) {
        if (!a.bounds.overlaps(b.bounds)) { return false; }
        return !hasSeparatingAxis(a, b) && !hasSeparatingAxis(b, a);
    }

    private static boolean hasSeparatingAxis(SeatGeometry axes, SeatGeometry other) {
        for (int edge = 0; edge < 4; edge++) {
            int next = (edge + 1) % 4;
            double nx = -(axes.y(next) - axes.y(edge));
            double ny = axes.x(next) - axes.x(edge);
            double minA = Double.POSITIVE_INFINITY, maxA = Double.NEGATIVE_INFINITY;
            double minB = Double.POSITIVE_INFINITY, maxB = Double.NEGATIVE_INFINITY;
            for (int v = 0; v < 4; v++) {
                double pa = axes.x(v) * nx + axes.y(v) * ny;
                double pb = other.x(v) * nx + other.y(v) * ny;
                minA = Math.min(minA, pa); maxA = Math.max(maxA, pa);
                minB = Math.min(minB, pb); maxB = Math.max(maxB, pb);
            }
            if (maxA <= minB || maxB <= minA) { return true; }
        }
        return false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new IllegalArgumentException(message); }
    }
}
