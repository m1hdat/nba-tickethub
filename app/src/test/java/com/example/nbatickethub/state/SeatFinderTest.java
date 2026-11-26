package com.example.nbatickethub.state;

import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.*;
import com.example.nbatickethub.model.*;
import org.junit.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class SeatFinderTest {
    private static ArenaDataset first, second;
    private static List<Game> games;
    private final SeatFinder finder = new SeatFinder();
    @BeforeClass public static void load() throws Exception {
        Path root = Files.isDirectory(Path.of("src/main/assets")) ? Path.of("src/main/assets") : Path.of("app/src/main/assets");
        AssetSource assets = name -> Files.newInputStream(root.resolve(name));
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
        games = MockMatchSchedule.games(catalog);
        ArenaDataset base = new MockArenaRepository(assets).load("mock/msg.json");
        first = MockMatchSchedule.inventory(base, games.get(0), catalog);
        second = MockMatchSchedule.inventory(base, games.get(1), catalog);
    }
    private SeatFinder.Request request(int n, long budget, SeatFinder.Priority priority) {
        return new SeatFinder.Request(n, budget, null, null, null, priority);
    }
    @Test public void lowestPriceAndBudgetUseActualMatchOffers() {
        List<SeatFinder.Suggestion> results = finder.find(first, games.get(0).id, request(1, 10000, SeatFinder.Priority.PRICE));
        assertEquals(3, results.size());
        long lowest = first.inventory.get(games.get(0).id).values().stream().filter(o -> o.baseStatus == SeatStatus.AVAILABLE)
                .mapToLong(o -> o.price.amountMinor).min().getAsLong();
        assertEquals(lowest, results.get(0).total);
        for (SeatFinder.Suggestion result : results) assertTrue(result.total <= 10000);
        assertTrue(results.get(0).total <= results.get(1).total);
    }
    @Test public void nearCourtUsesGeometryBeforePrice() {
        List<SeatFinder.Suggestion> close = finder.find(first, games.get(0).id, request(1, Long.MAX_VALUE, SeatFinder.Priority.COURT));
        List<SeatFinder.Suggestion> cheap = finder.find(first, games.get(0).id, request(1, Long.MAX_VALUE, SeatFinder.Priority.PRICE));
        assertTrue(close.get(0).distance <= cheap.get(0).distance);
        assertTrue(close.get(0).distance <= close.get(1).distance);
        assertNotEquals(close.get(0).ids, cheap.get(0).ids);
    }
    @Test public void threeSeatsShareRowAndGeometricSlotsWithExactTotal() {
        for (SeatFinder.Suggestion result : finder.find(first, games.get(0).id, request(3, Long.MAX_VALUE, SeatFinder.Priority.PRICE))) {
            assertEquals(3, result.ids.size()); long total = 0;
            Seat previous = null;
            for (String id : result.ids) {
                Seat seat = first.seats.get(id); GameSeat offer = first.inventory.get(games.get(0).id).get(id);
                assertEquals(SeatStatus.AVAILABLE, offer.baseStatus); total += offer.price.amountMinor;
                if (previous != null) assertTrue(SeatFinder.adjacent(first, first.rows.get(seat.rowId), previous, seat));
                previous = seat;
            }
            assertEquals(total, result.total);
        }
    }
    @Test public void consecutiveNumbersCannotBridgeMissingSlotOrSectionAisle() {
        SeatRow row = first.rows.values().iterator().next();
        Seat a = first.seatOrder.get(row.firstSeatIndex), third = first.seatOrder.get(row.firstSeatIndex + 2);
        Seat consecutiveAcrossGap = new Seat("gap", row.id, a.positionNumber + 1, PositionKind.SEATED, third.geometryIndex);
        assertFalse(SeatFinder.adjacent(first, row, a, consecutiveAcrossGap));
        Seat otherRow = first.seatOrder.get(row.firstSeatIndex + row.seatCount);
        assertFalse(SeatFinder.adjacent(first, row, a, otherRow));
        Seat otherSection = first.seatOrder.stream().filter(s -> !first.rows.get(s.rowId).sectionId.equals(row.sectionId)).findFirst().get();
        assertFalse(SeatFinder.adjacent(first, row, a, otherSection));
    }
    @Test public void emptyBudgetImpossibleGroupAndLocationFilter() {
        assertTrue(finder.find(first, games.get(0).id, request(1, 0, SeatFinder.Priority.PRICE)).isEmpty());
        assertTrue(finder.find(first, games.get(0).id, request(1000, Long.MAX_VALUE, SeatFinder.Priority.PRICE)).isEmpty());
        Section section = first.sections.values().iterator().next();
        SeatFinder.Request q = new SeatFinder.Request(1, Long.MAX_VALUE, section.levelId, section.id, null, SeatFinder.Priority.PRICE);
        for (SeatFinder.Suggestion result : finder.find(first, games.get(0).id, q)) {
            assertEquals(section.id, first.rows.get(first.seats.get(result.ids.get(0)).rowId).sectionId);
        }
        assertEquals(section.id, q.sectionId); assertEquals(Long.MAX_VALUE, q.budget);
    }
    @Test public void conflictDoesNotPartiallySelectAndSuccessfulGroupIsIdempotent() {
        ArenaSelection state = new ArenaSelection(); state.bind(first, games.get(0));
        SeatFinder.Suggestion group = finder.find(first, games.get(0).id, request(3, Long.MAX_VALUE, SeatFinder.Priority.PRICE)).get(0);
        String old = first.seatOrder.stream().map(s -> s.id).filter(state::available).filter(id -> !group.ids.contains(id)).findFirst().get();
        state.tap(old); state.beginReplace();
        assertFalse(state.selectSuggestion(group, id -> !id.equals(group.ids.get(1))));
        assertEquals(Collections.singleton(old), state.selectedIds()); assertEquals(old, state.replacingId());
        assertTrue(state.selectSuggestion(group, id -> true));
        assertTrue(state.selectSuggestion(group, id -> true));
        assertEquals(4, state.selectedIds().size()); assertEquals(group.total + state.offer(old).price.amountMinor, state.totalMinor());
    }
    @Test public void sameArenaUsesSeparateMatchStockAndRejectsStaleProposal() {
        List<SeatFinder.Suggestion> a = finder.find(first, games.get(0).id, request(3, Long.MAX_VALUE, SeatFinder.Priority.PRICE));
        List<SeatFinder.Suggestion> b = finder.find(second, games.get(1).id, request(3, Long.MAX_VALUE, SeatFinder.Priority.PRICE));
        ArenaSelection state = new ArenaSelection(); state.bind(second, games.get(1));
        assertFalse(state.selectSuggestion(a.get(0), id -> true)); assertTrue(state.selectedIds().isEmpty());
        for (SeatFinder.Suggestion group : b) for (String id : group.ids) {
            assertEquals(SeatStatus.AVAILABLE, second.inventory.get(games.get(1).id).get(id).baseStatus);
        }
        assertNotEquals(a.get(0).total, b.get(0).total);
    }
}
