package com.example.nbatickethub.state;

import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.*;
import com.example.nbatickethub.model.*;
import org.junit.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class MatchSeatFlowTest {
    private static ArenaCatalog catalog;
    private static ArenaDataset first, second, other;
    private static List<Game> games;
    private static AssetSource assets;
    private ArenaSelection state;
    @BeforeClass public static void load() throws Exception {
        Path root = Files.isDirectory(Path.of("src/main/assets")) ? Path.of("src/main/assets") : Path.of("app/src/main/assets");
        assets = name -> Files.newInputStream(root.resolve(name));
        catalog = new ArenaCatalogReader().load(assets); games = MockMatchSchedule.games(catalog);
        ArenaDataset base = new MockArenaRepository(assets).load("mock/msg.json");
        first = MockMatchSchedule.inventory(base,games.get(0),catalog);
        second = MockMatchSchedule.inventory(base,games.get(1),catalog);
        other = MockMatchSchedule.inventory(new MockArenaRepository(assets).load("mock/chase.json"),games.get(2),catalog);
    }
    @Before public void bind() { state = new ArenaSelection(); state.bind(first,games.get(0)); }
    private String available() { return first.seatOrder.stream().map(s -> s.id).filter(state::available).findFirst().get(); }
    @Test public void scheduleHasThirtyHomesAndTwoOpponentsAtSameVenue() throws Exception {
        assertTrue(games.size() >= 3); assertEquals(games.get(0).arenaId,games.get(1).arenaId);
        assertNotEquals(games.get(0).awayTeamId,games.get(1).awayTeamId);
        Set<String> teams = new HashSet<>();
        for (Game game : games) { teams.add(game.homeTeamId); assertEquals(catalog.homeArena(game.homeTeamId).id,game.arenaId); assertEquals(DataProvenance.OriginType.MOCK,game.provenance.originType); }
        assertEquals(30,teams.size());
        Map<String,Long> prices = MockMatchSchedule.startingPrices(catalog,assets);
        assertEquals(first.inventory.get(games.get(0).id).values().stream().mapToLong(o->o.price.amountMinor).min().getAsLong(),(long)prices.get(games.get(0).id));
        assertEquals(second.inventory.get(games.get(1).id).values().stream().mapToLong(o->o.price.amountMinor).min().getAsLong(),(long)prices.get(games.get(1).id));
    }
    @Test public void previewNeedsConfirmationAndBlockedSeatsCannotReplaceOldChoice() {
        String old = available(); state.preview(old); assertTrue(state.selectedIds().isEmpty());
        assertEquals(ArenaSelection.Result.CHANGED,state.confirm()); state.beginReplace();
        String blocked = first.seatOrder.stream().map(s->s.id).filter(id->!state.available(id)).findFirst().get();
        state.preview(blocked); assertEquals(ArenaSelection.Result.UNAVAILABLE,state.confirm());
        assertEquals(Collections.singleton(old),state.selectedIds()); assertEquals(old,state.replacingId());
        String next = first.seatOrder.stream().map(s->s.id).filter(id->!id.equals(old)&&state.available(id)).findFirst().get();
        state.preview(next); assertEquals(Collections.singleton(old),state.selectedIds());
        state.confirm(); assertEquals(Collections.singleton(next),state.selectedIds()); assertNull(state.replacingId());
    }
    @Test public void changingMatchRetainsArenaContextButClearsTicketIdentity() {
        String seat = available(); state.preview(seat); state.confirm();
        String section = first.rows.get(first.seats.get(seat).rowId).sectionId;
        state.browseSection(section); state.filterSection(section); state.filterTickets(null,25000);
        long price = state.offer(seat).price.amountMinor;
        state.bind(second,games.get(1));
        assertEquals(section,state.browsingSection()); assertEquals(section,state.sectionId()); assertEquals(25000,state.budget());
        assertTrue(state.selectedIds().isEmpty()); assertNull(state.candidateId()); assertNull(state.replacingId());
        assertNotEquals(price,state.offer(seat).price.amountMinor);
        assertNotEquals(first.inventory.get(games.get(0).id).values().stream().filter(o->o.baseStatus==SeatStatus.AVAILABLE).count(),second.availableCount(games.get(1).id));
        state.bind(other,games.get(2)); assertNull(state.browsingSection()); assertNull(state.sectionId()); assertEquals(Long.MAX_VALUE,state.budget());
    }
    @Test public void typeBudgetAndRotationKeepSelectionAndPreviewConsistent() {
        String seat = available(); state.preview(seat); state.confirm();
        String section = first.rows.get(first.seats.get(seat).rowId).sectionId; state.browseSection(section);
        state.filterTickets("VIP",10000);
        for (String id : state.tickets()) { assertEquals(Section.Category.VIP,state.ticketCategory(id)); assertTrue(state.offer(id).price.amountMinor<=10000); }
        assertEquals(Collections.singleton(seat),state.selectedIds());
        state.clearFilter(); state.preview(seat); state.bind(first,games.get(0));
        assertEquals(seat,state.candidateId()); assertEquals(section,state.browsingSection()); assertEquals(Collections.singleton(seat),state.selectedIds());
    }
    @Test public void matchOwnsTicketTypeStatusAndPriceWithoutChangingSeatGeometry() {
        assertSame(first.geometry.get(0),second.geometry.get(0));
        boolean typeChanged=false,statusChanged=false;
        ArenaSelection next=new ArenaSelection(); next.bind(second,games.get(1));
        for (Seat seat : first.seatOrder) {
            typeChanged |= state.ticketCategory(seat.id)!=next.ticketCategory(seat.id);
            statusChanged |= state.offer(seat.id).baseStatus!=next.offer(seat.id).baseStatus;
        }
        assertTrue(typeChanged); assertTrue(statusChanged);
        assertThrows(IllegalArgumentException.class,()->MockMatchSchedule.inventory(first,games.get(2),catalog));
    }
}
