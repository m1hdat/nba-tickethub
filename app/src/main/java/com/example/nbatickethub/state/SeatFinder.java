package com.example.nbatickethub.state;

import com.example.nbatickethub.data.ArenaDataset;
import com.example.nbatickethub.model.*;
import java.util.*;

/** Pure Java search over the existing row layout and match inventory. */
public final class SeatFinder {
    public enum Priority { PRICE, COURT }
    public static final class Request {
        public final int people;
        public final long budget;
        public final String levelId, sectionId, category;
        public final Priority priority;
        public Request(int people, long budget, String levelId, String sectionId, String category, Priority priority) {
            if (people < 1 || budget < 0) throw new IllegalArgumentException("Invalid search");
            this.people = people; this.budget = budget; this.levelId = levelId;
            this.sectionId = sectionId; this.category = category; this.priority = Objects.requireNonNull(priority);
        }
    }
    public static final class Suggestion {
        public final String matchId;
        public final List<String> ids;
        public final long total;
        public final double distance;
        private Suggestion(String matchId, List<String> ids, long total, double distance) {
            this.matchId = matchId; this.ids = Collections.unmodifiableList(ids);
            this.total = total; this.distance = distance;
        }
    }

    // The compiler divides each row into equal angular slots. A group must use
    // neighboring slots in that SAME row, with no geometric gap larger than a slot.
    // This deliberately does not infer adjacency from printed seat numbers.
    public static boolean adjacent(ArenaDataset data, SeatRow row, Seat a, Seat b) {
        if (!a.rowId.equals(row.id) || !b.rowId.equals(row.id)) return false;
        WorldRect x = data.geometry.get(a.geometryIndex).bounds, y = data.geometry.get(b.geometryIndex).bounds;
        double ax = (x.left + x.right) / 2.0 - row.boundary.centerX;
        double ay = (x.top + x.bottom) / 2.0 - row.boundary.centerY;
        double bx = (y.left + y.right) / 2.0 - row.boundary.centerX;
        double by = (y.top + y.bottom) / 2.0 - row.boundary.centerY;
        double angle = Math.abs(Math.toDegrees(Math.atan2(ax * by - ay * bx, ax * bx + ay * by)));
        double pitch = (row.boundary.endDegrees - row.boundary.startDegrees) / row.seatCount;
        return angle > pitch * .5 && angle < pitch * 1.5;
    }

    public List<Suggestion> find(ArenaDataset data, String matchId, Request request) {
        Map<String, GameSeat> offers = data.inventory.get(matchId);
        if (offers == null) throw new IllegalArgumentException("Unknown match");
        Comparator<Suggestion> order = request.priority == Priority.PRICE
                ? Comparator.comparingLong((Suggestion s) -> s.total).thenComparingDouble(s -> s.distance)
                : Comparator.comparingDouble((Suggestion s) -> s.distance).thenComparingLong(s -> s.total);
        order = order.thenComparing(s -> s.ids.get(0));
        List<Suggestion> best = new ArrayList<>();
        for (SeatRow row : data.rows.values()) {
            Section section = data.sections.get(row.sectionId);
            if ((request.levelId != null && !request.levelId.equals(section.levelId))
                    || (request.sectionId != null && !request.sectionId.equals(section.id))) continue;
            List<Seat> run = new ArrayList<>();
            Seat previous = null;
            for (int i = row.firstSeatIndex; i < row.firstSeatIndex + row.seatCount; i++) {
                Seat seat = data.seatOrder.get(i); GameSeat offer = offers.get(seat.id);
                Section.Category category = offer.ticketCategory == null ? section.category : offer.ticketCategory;
                boolean eligible = offer.baseStatus == SeatStatus.AVAILABLE && offer.price.amountMinor <= request.budget
                        && (request.category == null || request.category.equals(category.name()))
                        && (request.people == 1 || seat.kind == PositionKind.SEATED);
                if (!eligible || (previous != null && !adjacent(data, row, previous, seat))) run.clear();
                if (eligible) run.add(seat);
                previous = seat;
                if (run.size() > request.people) run.remove(0);
                if (run.size() != request.people) continue;
                List<String> ids = new ArrayList<>(); long total = 0; double distance = 0;
                WorldRect court = data.arena.courtBounds;
                for (Seat member : run) {
                    ids.add(member.id); total = Math.addExact(total, offers.get(member.id).price.amountMinor);
                    WorldRect bounds = data.geometry.get(member.geometryIndex).bounds;
                    double cx = (bounds.left + bounds.right) / 2.0, cy = (bounds.top + bounds.bottom) / 2.0;
                    double dx = Math.max(Math.max(court.left - cx, 0), cx - court.right);
                    double dy = Math.max(Math.max(court.top - cy, 0), cy - court.bottom);
                    distance += Math.hypot(dx, dy);
                }
                best.add(new Suggestion(matchId, ids, total, distance / request.people));
                best.sort(order); if (best.size() > 3) best.remove(3);
            }
        }
        return Collections.unmodifiableList(best);
    }
}
