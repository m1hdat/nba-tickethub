package com.example.nbatickethub.state;

import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.MockArenaRepository;
import com.example.nbatickethub.geometry.*;
import com.example.nbatickethub.model.*;
import org.junit.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class ArenaSelectionTest {
    private static ArenaCatalog catalog;
    private static ArenaDataset msg, chase;
    private static ArenaScene scene;
    private ArenaSelection state;
    private static final List<String> available = new ArrayList<>();
    private static String booked, locked;
    @BeforeClass public static void load() throws Exception {
        Path root = Files.isDirectory(Path.of("src/main/assets")) ? Path.of("src/main/assets") : Path.of("app/src/main/assets");
        AssetSource assets = name -> Files.newInputStream(root.resolve(name));
        catalog = new ArenaCatalogReader().load(assets);
        msg = new MockArenaRepository(assets).load("mock/msg.json");
        chase = new MockArenaRepository(assets).load("mock/chase.json");
        scene = new ArenaSceneCompiler().compile(catalog.byId.get("msg"), msg);
        Map<String, GameSeat> inventory = msg.inventory.values().iterator().next();
        for (GameSeat offer : inventory.values()) {
            if (offer.baseStatus == SeatStatus.AVAILABLE) { available.add(offer.seatId); }
            else if (offer.baseStatus == SeatStatus.BOOKED) { booked = offer.seatId; }
            else { locked = offer.seatId; }
        }
    }
    @Before public void reset() { state = new ArenaSelection(); state.bind(msg, msg.games.values().iterator().next()); }

    @Test public void togglesIndependentlyAndComputesExactTotal() {
        String a = available.get(0), b = available.get(1);
        state.tap(a); state.tap(b);
        assertEquals(2, state.selectedIds().size());
        assertEquals(state.offer(a).price.amountMinor + state.offer(b).price.amountMinor, state.totalMinor());
        state.tap(a);
        assertEquals(Collections.singleton(b), state.selectedIds());
        assertEquals(state.offer(b).price.amountMinor, state.totalMinor());
        state.remove(a); state.remove(a);
        assertEquals(Collections.singleton(b), state.selectedIds());
    }
    @Test public void blankBookedAndLockedCannotDestroySelectionOrReplacement() {
        String a = available.get(0);
        state.tap(a); state.beginReplace();
        assertEquals(ArenaSelection.Result.EMPTY, state.tap(null));
        assertEquals(ArenaSelection.Result.UNAVAILABLE, state.tap(booked));
        assertEquals(ArenaSelection.Result.UNAVAILABLE, state.tap(locked));
        assertEquals(a, state.focusedId()); assertEquals(a, state.replacingId());
        assertEquals(Collections.singleton(a), state.selectedIds());
        state.cancelReplace();
        assertEquals(a, state.focusedId()); assertNull(state.replacingId());
        assertEquals(Collections.singleton(a), state.selectedIds());
    }
    @Test public void replacesOnlyAfterAValidNewSeatWithoutChangingOtherSeats() {
        String a = available.get(0), b = available.get(1), c = available.get(2);
        state.tap(c); state.tap(a); state.beginReplace(); state.tap(b);
        assertEquals(new LinkedHashSet<>(Arrays.asList(c, b)), state.selectedIds());
        assertEquals(b, state.focusedId()); assertNull(state.replacingId());
        assertEquals(state.offer(c).price.amountMinor + state.offer(b).price.amountMinor, state.totalMinor());
    }
    @Test public void duplicateOrOriginalReplacementDoesNotDropAnExistingTicket() {
        String a = available.get(0), b = available.get(1);
        state.tap(b); state.tap(a); state.beginReplace();
        assertEquals(ArenaSelection.Result.ALREADY_SELECTED, state.tap(b));
        assertEquals(2, state.selectedIds().size()); assertEquals(a, state.replacingId());
        state.tap(a);
        assertNull(state.replacingId()); assertEquals(2, state.selectedIds().size());
    }
    @Test public void filtersRetainSelectionAndFocusFromListClearsInvisibleFilter() {
        String a = available.get(0); state.tap(a);
        String originalSection = msg.rows.get(msg.seats.get(a).rowId).sectionId;
        Section different = msg.sections.values().stream().filter(s -> !s.id.equals(originalSection)).findFirst().get();
        state.filterLevel(different.levelId); state.filterSection(different.id);
        assertFalse(state.matches(a)); assertEquals(1, state.outsideFilterCount());
        assertEquals(ArenaSelection.Result.OUTSIDE_FILTER, state.tap(a));
        assertEquals(Collections.singleton(a), state.selectedIds());
        int expected = 0;
        for (Seat seat : msg.seatOrder) { if (state.matches(seat.id)) { expected++; } }
        assertEquals(expected, state.matchingCount(false));
        state.focus(a);
        assertNull(state.levelId()); assertNull(state.sectionId()); assertTrue(state.matches(a));
        assertEquals(msg.capacity(), state.matchingCount(false));
    }
    @Test public void changingLevelClearsIncompatibleSection() {
        Section first = msg.sections.values().iterator().next();
        state.filterLevel(first.levelId); state.filterSection(first.id);
        ArenaLevel other = msg.levels.values().stream().filter(l -> !l.id.equals(first.levelId)).findFirst().get();
        state.filterLevel(other.id);
        assertNull(state.sectionId());
        assertThrows(IllegalArgumentException.class, () -> state.filterSection(first.id));
    }
    @Test public void sameGameRebindKeepsRotationStateDifferentGameResetsIt() {
        String a = available.get(0); state.tap(a); state.beginReplace();
        Section section = msg.sections.get(msg.rows.get(msg.seats.get(a).rowId).sectionId);
        state.filterLevel(section.levelId); state.filterSection(section.id);
        state.bind(msg, msg.games.values().iterator().next());
        assertEquals(Collections.singleton(a), state.selectedIds()); assertEquals(a, state.replacingId());
        assertEquals(section.id, state.sectionId());
        state.bind(chase, chase.games.values().iterator().next());
        assertTrue(state.selectedIds().isEmpty()); assertNull(state.focusedId()); assertNull(state.sectionId());
    }
    @Test public void selectionHasNoArtificialSmallProfileLimitAndCanBeCleared() {
        long total = 0;
        for (int i = 0; i < 300; i++) { state.tap(available.get(i)); total += state.offer(available.get(i)).price.amountMinor; }
        assertEquals(300, state.selectedIds().size()); assertEquals(total, state.totalMinor());
        assertThrows(UnsupportedOperationException.class, () -> state.selectedIds().clear());
        state.clearSelection(); assertEquals(0, state.totalMinor()); assertNull(state.focusedId());
    }
    @Test public void twoSampleGamesResolveAccentsFromHomeTeamNotUiOverride() {
        Game a = msg.games.values().iterator().next(), b = chase.games.values().iterator().next();
        assertEquals("nyk", a.homeTeamId); assertEquals("gsw", b.homeTeamId);
        ArenaTheme first = GameThemeResolver.resolve(catalog, a), second = GameThemeResolver.resolve(catalog, b);
        assertEquals(a.homeTeamId, first.teamId); assertEquals(b.homeTeamId, second.teamId);
        assertNotEquals(first.primaryColor, second.primaryColor);
    }
    @Test public void soldOutSectionKeepsItsPositionsAndPreservesSelectionOutsideFilter() {
        Section target = msg.sections.values().iterator().next();
        List<ArenaLevel> levels = new ArrayList<>(msg.levels.values());
        List<GameSeat> offers = new ArrayList<>();
        for (Map<String, GameSeat> inventory : msg.inventory.values()) {
            for (GameSeat offer : inventory.values()) {
                boolean inSection = msg.rows.get(msg.seats.get(offer.seatId).rowId).sectionId.equals(target.id);
                offers.add(new GameSeat(offer.gameId, offer.seatId, inSection ? SeatStatus.BOOKED : offer.baseStatus,
                        offer.price, offer.provenance));
            }
        }
        ArenaDataset fixture = new ArenaDataset(msg.arena, new ArrayList<>(msg.teams.values()),
                new ArrayList<>(msg.games.values()), levels, new ArrayList<>(msg.sections.values()),
                new ArrayList<>(msg.rows.values()), msg.seatOrder, msg.geometry, offers);
        state.bind(fixture, fixture.games.values().iterator().next());
        String selected = available.stream().filter(id -> !msg.rows.get(msg.seats.get(id).rowId).sectionId.equals(target.id)).findFirst().get();
        state.tap(selected); long total = state.totalMinor();
        state.filterSection(target.id);
        assertTrue(state.matchingCount(false) > 0); assertEquals(0, state.matchingCount(true));
        assertThrows(IllegalArgumentException.class, () -> state.filterLevel("missing-level"));
        assertEquals(Collections.singleton(selected), state.selectedIds()); assertEquals(total, state.totalMinor());
        state.clearFilter(); assertEquals(fixture.capacity(), state.matchingCount(false));
    }
    @Test public void simulatedLowAndHighSeatsHaveDifferentElevationAndProjectedCourt() {
        Seat low = msg.seatOrder.get(0);
        Seat high = msg.seatOrder.get(msg.seatOrder.size() - 1);
        SeatPerspective a = new SeatPerspective(scene, low), b = new SeatPerspective(scene, high);
        assertTrue(b.eyeHeight > a.eyeHeight + 30);
        assertNotEquals(Arrays.toString(a.courtQuad()), Arrays.toString(b.courtQuad()));
        for (float value : a.courtQuad()) { assertTrue(Float.isFinite(value)); }
        for (float value : b.courtQuad()) { assertTrue(Float.isFinite(value)); }
        float[] center = new float[2]; a.project(47, 25, center, 0);
        assertArrayEquals(new float[]{.5f, .54f}, center, .0001f);
    }
}
