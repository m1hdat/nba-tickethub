package com.example.nbatickethub;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.*;
import com.example.nbatickethub.model.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BookingStoreInstrumentedTest {
    private static Context context;
    private static ArenaDataset first, second;
    private static Game a, b;
    private BookingStore store;
    private String database;
    private List<String> free;
    @BeforeClass public static void fixtures() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AssetSource assets = new AndroidAssetSource(context.getAssets());
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
        List<Game> games = MockMatchSchedule.games(catalog); a = games.get(0); b = games.get(1);
        ArenaDataset base = new MockArenaRepository(assets).load("mock/msg.json");
        first = MockMatchSchedule.inventory(base, a, catalog); second = MockMatchSchedule.inventory(base, b, catalog);
    }
    @Before public void setup() {
        database = "phase6-test-" + UUID.randomUUID() + ".db"; store = new BookingStore(context, database);
        free = new ArrayList<>();
        for (GameSeat offer : first.inventory.get(a.id).values()) if (offer.baseStatus == SeatStatus.AVAILABLE) free.add(offer.seatId);
    }
    @After public void cleanup() { store.close(); context.deleteDatabase(database); }
    @Test public void savesEntireOrderAndBlocksDuplicateRequestsAndSeatKeys() throws Exception {
        List<String> seats = free.subList(0, 2);
        JSONObject saved = store.book(first, a, seats, "SIM-success");
        assertEquals(2, saved.getJSONArray("tickets").length());
        long expected = first.inventory.get(a.id).get(seats.get(0)).price.amountMinor + first.inventory.get(a.id).get(seats.get(1)).price.amountMinor;
        assertEquals(expected, saved.getLong("total"));
        assertEquals(saved.toString(), store.book(first, a, seats, "SIM-success").toString());
        try { store.book(first, a, seats, "SIM-duplicate"); fail("Duplicate inventory must fail"); }
        catch (BookingStore.Conflict e) { assertEquals(2, e.seats.size()); }
        assertEquals(1, store.orders().size());
        assertEquals(SeatStatus.BOOKED, store.overlay(first, a).inventory.get(a.id).get(seats.get(0)).baseStatus);
    }
    @Test public void oneBusySeatRejectsWholeMultiSeatOrder() throws Exception {
        store.book(first, a, Collections.singleton(free.get(1)), "SIM-existing");
        try { store.book(first, a, free.subList(0, 3), "SIM-partial"); fail(); }
        catch (BookingStore.Conflict e) { assertEquals(Collections.singletonList(BookingStore.seatLabel(first, free.get(1))), e.seats); }
        assertEquals(1, store.orders().size());
        assertEquals(SeatStatus.AVAILABLE, store.overlay(first, a).inventory.get(a.id).get(free.get(0)).baseStatus);
        String blocked = first.inventory.get(a.id).values().stream().filter(o -> o.baseStatus != SeatStatus.AVAILABLE).findFirst().get().seatId;
        try { store.book(first, a, Arrays.asList(free.get(0), blocked), "SIM-base-busy"); fail(); }
        catch (BookingStore.Conflict e) { assertEquals(Collections.singletonList(BookingStore.seatLabel(first, blocked)), e.seats); }
        assertEquals(1, store.orders().size());
    }
    @Test public void reopenPreservesSnapshotAndInventory() throws Exception {
        JSONObject saved = store.book(first, a, free.subList(0, 2), "SIM-reopen");
        store.close(); store = new BookingStore(context, database);
        assertEquals(saved.toString(), store.orders().get(0).toString());
        assertEquals(a.startsAtMillis, store.orders().get(0).getLong("startsAt"));
        assertEquals(SeatStatus.BOOKED, store.overlay(first, a).inventory.get(a.id).get(free.get(0)).baseStatus);
    }
    @Test public void sameArenaHasIndependentMatchInventory() throws Exception {
        String id = free.stream().filter(s -> second.inventory.get(b.id).get(s).baseStatus == SeatStatus.AVAILABLE).findFirst().get();
        store.book(first, a, Collections.singleton(id), "SIM-match-a");
        assertEquals(SeatStatus.AVAILABLE, store.overlay(second, b).inventory.get(b.id).get(id).baseStatus);
        store.book(second, b, Collections.singleton(id), "SIM-match-b");
        assertEquals(2, store.orders().size());
    }
    @Test public void concurrentHelpersCannotBookSameSeatTwice() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try (BookingStore other = new BookingStore(context, database)) {
            store.getWritableDatabase(); other.getWritableDatabase();
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int index = i;
                results.add(pool.submit(() -> { start.await(); try {
                    (index == 0 ? store : other).book(first, a, Collections.singleton(free.get(0)), "SIM-race-" + index); return true;
                } catch (BookingStore.Conflict e) { return false; } }));
            }
            start.countDown(); int successes = 0; for (Future<Boolean> result : results) if (result.get(15, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes); assertEquals(1, store.orders().size());
        } finally { pool.shutdownNow(); }
    }
    @Test public void storageFailureRollsBackOrderAndAllTickets() throws Exception {
        store.getWritableDatabase().execSQL("CREATE TRIGGER reject_second BEFORE INSERT ON tickets WHEN NEW.seat_id='" + free.get(1).replace("'", "''") + "' BEGIN SELECT RAISE(ABORT, 'test storage failure'); END");
        try { store.book(first, a, free.subList(0, 2), "SIM-failure"); fail(); }
        catch (android.database.sqlite.SQLiteException expected) { }
        assertTrue(store.orders().isEmpty());
        assertEquals(SeatStatus.AVAILABLE, store.overlay(first, a).inventory.get(a.id).get(free.get(0)).baseStatus);
    }
}
