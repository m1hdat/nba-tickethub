package com.example.nbatickethub;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.nbatickethub.state.*;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.model.Game;
import com.example.nbatickethub.ui.arena.ArenaView;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class PhaseSixInstrumentedTest {
    private ArenaSelectionViewModel state(MainActivity a) { return new ViewModelProvider(a).get(ArenaSelectionViewModel.class); }
    private BookingViewModel booking(MainActivity a) { return new ViewModelProvider(a).get(BookingViewModel.class); }
    private void waitFor(ActivityScenario<MainActivity> scenario, java.util.function.Predicate<MainActivity> condition) {
        AtomicBoolean ready = new AtomicBoolean(); long end = SystemClock.uptimeMillis() + 45000;
        while (!ready.get() && SystemClock.uptimeMillis() < end) { scenario.onActivity(a -> ready.set(condition.test(a))); if (!ready.get()) SystemClock.sleep(60); }
        assertTrue("UI did not become ready", ready.get());
    }
    private void openSeats(ActivityScenario<MainActivity> scenario) {
        waitFor(scenario, a -> state(a).sampleGames != null && ((LinearLayout)a.findViewById(R.id.featured_match)).getChildCount() > 0);
        scenario.onActivity(a -> { a.findViewById(R.id.nav_home).performClick(); ((LinearLayout)a.findViewById(R.id.featured_match)).getChildAt(0).findViewById(R.id.open_match).performClick(); });
        waitFor(scenario, a -> state(a).selection.game() != null && ((ArenaView)a.findViewById(R.id.arena_view)).displayedArenaId() != null);
    }
    private void capture(String name) throws Exception {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        for (String cmd : new String[]{"mkdir -p /sdcard/Download/phase6-qa", "screencap -p /sdcard/Download/phase6-qa/" + name + ".png"}) {
            try (android.os.ParcelFileDescriptor fd = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(cmd);
                 java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) { byte[] bytes = new byte[1024]; while (input.read(bytes) >= 0) { } }
        }
    }
    @Test public void conflictNamesBusySeatAndKeepsOtherSeatUnbooked() throws Exception {
        AtomicReference<ArenaDataset> data = new AtomicReference<>(); AtomicReference<Game> game = new AtomicReference<>();
        java.util.List<String> ids = new java.util.ArrayList<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openSeats(scenario);
            scenario.onActivity(a -> {
                ArenaSelection s = state(a).selection; data.set(s.dataset()); game.set(s.game());
                s.dataset().seatOrder.stream().map(v -> v.id).filter(s::available).limit(2).forEach(ids::add);
                for (String id : ids) { s.preview(id); s.confirm(); } state(a).changed();
                a.findViewById(R.id.booking_continue).performClick();
            });
            try (BookingStore store = new BookingStore(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext())) {
                store.book(data.get(), game.get(), java.util.Collections.singleton(ids.get(1)), "SIM-competing-" + java.util.UUID.randomUUID());
                int before = store.orders().size();
                onView(withId(R.id.booking_confirm)).perform(click());
                waitFor(scenario, a -> !booking(a).busy && booking(a).error != null);
                scenario.onActivity(a -> {
                    assertTrue(booking(a).error instanceof BookingStore.Conflict); assertNull(booking(a).result);
                    assertTrue(booking(a).error.getMessage().contains(BookingStore.seatLabel(data.get(), ids.get(1))));
                });
                assertEquals(before, store.orders().size());
                assertEquals(com.example.nbatickethub.model.SeatStatus.AVAILABLE, store.overlay(data.get(), game.get()).inventory.get(game.get().id).get(ids.get(0)).baseStatus);
                capture("conflict");
                onView(withText(R.string.booking_reselect)).perform(click());
                waitFor(scenario, a -> !state(a).selection.available(ids.get(1)));
                scenario.onActivity(a -> { assertTrue(state(a).selection.selectedIds().contains(ids.get(0))); assertFalse(state(a).selection.selectedIds().contains(ids.get(1))); });
            }
        }
    }
    @Test public void backRetainsSelectionRotateDoubleConfirmAndReopenTickets() throws Exception {
        AtomicReference<String> seat = new AtomicReference<>(), order = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            openSeats(scenario);
            scenario.onActivity(a -> {
                ArenaSelection s = state(a).selection;
                seat.set(s.dataset().seatOrder.stream().map(v -> v.id).filter(s::available).findFirst().get());
                s.preview(seat.get()); s.confirm(); state(a).changed();
            });
            waitFor(scenario, a -> { Rect r = new Rect(); return a.findViewById(R.id.booking_continue).getGlobalVisibleRect(r) && r.height() >= 47 * a.getResources().getDisplayMetrics().density; });
            scenario.onActivity(a -> {
                Rect visible = new Rect(); assertTrue(a.findViewById(R.id.booking_continue).getGlobalVisibleRect(visible));
                assertTrue(visible.height() >= 47 * a.getResources().getDisplayMetrics().density);
                a.findViewById(R.id.booking_continue).performClick();
            });
            onView(withText(R.string.booking_back)).perform(click());
            scenario.onActivity(a -> { assertTrue(state(a).selection.selectedIds().contains(seat.get())); a.findViewById(R.id.booking_continue).performClick(); });
            scenario.recreate();
            waitFor(scenario, a -> state(a).selection.game() != null && booking(a).showing && a.findViewById(R.id.booking_continue).hasOnClickListeners());
            capture("confirmation");
            onView(withId(R.id.booking_confirm)).perform(click());
            // Same event-loop retry must not create another request while the transaction is running.
            scenario.onActivity(a -> booking(a).confirm());
            waitFor(scenario, a -> booking(a).result != null || booking(a).error != null);
            scenario.onActivity(a -> { assertNull(booking(a).error); order.set(booking(a).result.optString("id")); booking(a).confirm(); });
            capture("success"); onView(withId(R.id.booking_confirm)).perform(click());
            waitFor(scenario, a -> !booking(a).loadingOrders && !booking(a).orders.isEmpty());
            scenario.onActivity(a -> {
                assertEquals(BrowseNavigation.Page.TICKETS, state(a).navigation.page);
                assertEquals(1, booking(a).orders.stream().filter(o -> order.get().equals(o.optString("id"))).count());
            });
            capture("my-tickets");
            waitFor(scenario, a -> !state(a).selection.available(seat.get()));
            scenario.onActivity(a -> { state(a).selection.preview(seat.get()); assertEquals(ArenaSelection.Result.UNAVAILABLE, state(a).selection.confirm()); });
        }
        try (ActivityScenario<MainActivity> reopened = ActivityScenario.launch(MainActivity.class)) {
            waitFor(reopened, a -> state(a).sampleGames != null && a.findViewById(R.id.nav_tickets).hasOnClickListeners());
            reopened.onActivity(a -> a.findViewById(R.id.nav_tickets).performClick());
            waitFor(reopened, a -> !booking(a).loadingOrders && booking(a).orders.stream().anyMatch(o -> order.get().equals(o.optString("id"))));
            openSeats(reopened);
            reopened.onActivity(a -> assertFalse(state(a).selection.available(seat.get())));
        }
    }
}
