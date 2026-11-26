package com.example.nbatickethub;

import android.os.SystemClock;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.model.*;
import com.example.nbatickethub.state.*;
import com.example.nbatickethub.ui.arena.ArenaView;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class SeatFinderInstrumentedTest {
    private ArenaSelectionViewModel model(MainActivity a) { return new ViewModelProvider(a).get(ArenaSelectionViewModel.class); }
    private void waitFor(ActivityScenario<MainActivity> scenario, java.util.function.Predicate<MainActivity> condition) {
        AtomicBoolean ready = new AtomicBoolean(); long end = SystemClock.uptimeMillis() + 45000;
        while (!ready.get() && SystemClock.uptimeMillis() < end) {
            scenario.onActivity(a -> ready.set(condition.test(a))); if (!ready.get()) SystemClock.sleep(80);
        }
        assertTrue("UI did not become ready", ready.get());
    }
    private void open(ActivityScenario<MainActivity> scenario) {
        waitFor(scenario, a -> model(a).sampleGames != null && ((LinearLayout)a.findViewById(R.id.featured_match)).getChildCount() > 0);
        scenario.onActivity(a -> ((LinearLayout)a.findViewById(R.id.featured_match)).getChildAt(0).findViewById(R.id.open_match).performClick());
        waitFor(scenario, a -> model(a).selection.game() != null && ((ArenaView)a.findViewById(R.id.arena_view)).displayedArenaId() != null);
        onView(withId(R.id.suggest_seats)).perform(click());
    }
    private void searchGroup() {
        onView(withId(R.id.finder_people_input)).perform(scrollTo(), replaceText("3")); androidx.test.espresso.Espresso.closeSoftKeyboard();
        onView(withId(R.id.finder_search_button)).perform(scrollTo(), click());
    }
    private void results() {
        long end = SystemClock.uptimeMillis() + 30000; AtomicBoolean ready = new AtomicBoolean();
        while (!ready.get() && SystemClock.uptimeMillis() < end) {
            onView(withId(R.id.finder_results)).check((v,e) -> { if (e != null) throw e; ready.set(((LinearLayout)v).getChildCount() == 6); });
            if (!ready.get()) SystemClock.sleep(100);
        }
        assertTrue("Expected three cards and selection buttons", ready.get());
    }
    private void capture(String name) {
        try {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            for (String command : new String[]{"mkdir -p /sdcard/Download/phase6b-qa", "screencap -p /sdcard/Download/phase6b-qa/" + name + ".png"}) {
                android.os.ParcelFileDescriptor fd = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
                try (java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
                    byte[] bytes = new byte[1024]; while (input.read(bytes) >= 0) { }
                }
            }
        } catch (Exception error) { throw new AssertionError(error); }
    }
    @Test public void sharedPanelRetainsQueryAfterRecreateAndUsesSixACheckout() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            open(scenario); searchGroup(); results();
            scenario.recreate();
            waitFor(scenario, a -> ((ArenaView)a.findViewById(R.id.arena_view)).displayedArenaId() != null);
            onView(withId(R.id.finder_people_input)).check((v,e) -> { if (e != null) throw e; assertEquals("3", ((EditText)v).getText().toString()); });
            results();
            onView(withId(R.id.finder_preview_first)).perform(scrollTo()); capture("group-results");
            onView(withId(R.id.finder_preview_first)).perform(click()); capture("perspective");
            onView(withText(R.string.fullscreen_close)).perform(click());
            onView(withId(R.id.finder_select_first)).perform(scrollTo(), click());
            waitFor(scenario, a -> model(a).selection.selectedIds().size() == 3);
            scenario.onActivity(a -> { assertTrue(model(a).selection.totalMinor() > 0); a.findViewById(R.id.booking_continue).performClick(); });
            waitFor(scenario, a -> new ViewModelProvider(a).get(BookingViewModel.class).draft != null);
            scenario.onActivity(a -> {
                org.json.JSONObject draft = new ViewModelProvider(a).get(BookingViewModel.class).draft;
                assertEquals(3, draft.optJSONArray("tickets").length()); assertEquals(model(a).selection.game().id, draft.optString("matchId"));
            });
        }
    }
    @Test public void sqliteConflictKeepsOldChoiceAndZeroBudgetKeepsInputs() throws Exception {
        AtomicReference<ArenaDataset> data = new AtomicReference<>(); AtomicReference<Game> game = new AtomicReference<>();
        AtomicReference<SeatFinder.Suggestion> proposal = new AtomicReference<>(); AtomicReference<Set<String>> previous = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            open(scenario); searchGroup(); results();
            scenario.onActivity(a -> {
                ArenaSelection state = model(a).selection; data.set(state.dataset()); game.set(state.game());
                proposal.set(new SeatFinder().find(data.get(), game.get().id, new SeatFinder.Request(3, Long.MAX_VALUE, null, null, null, SeatFinder.Priority.PRICE)).get(0));
                String old = data.get().seatOrder.stream().map(s -> s.id).filter(state::available).filter(id -> !proposal.get().ids.contains(id)).findFirst().get();
                state.tap(old); previous.set(new HashSet<>(state.selectedIds())); model(a).changed();
            });
            try (BookingStore store = new BookingStore(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext())) {
                store.book(data.get(), game.get(), Collections.singleton(proposal.get().ids.get(1)), "SIM-6B-" + UUID.randomUUID());
            }
            onView(withId(R.id.finder_select_first)).perform(scrollTo(), click());
            long end = SystemClock.uptimeMillis() + 10000; AtomicBoolean conflict = new AtomicBoolean();
            while (!conflict.get() && SystemClock.uptimeMillis() < end) {
                onView(withId(R.id.finder_message)).check((v,e) -> conflict.set(((TextView)v).getText().toString().contains("Giữ nguyên")));
                if (!conflict.get()) SystemClock.sleep(100);
            }
            assertTrue(conflict.get()); scenario.onActivity(a -> assertEquals(previous.get(), model(a).selection.selectedIds()));
            capture("conflict");
            onView(withId(R.id.finder_budget_input)).perform(scrollTo(), replaceText("0")); androidx.test.espresso.Espresso.closeSoftKeyboard();
            onView(withId(R.id.finder_search_button)).perform(scrollTo(), click());
            AtomicBoolean empty = new AtomicBoolean(); end = SystemClock.uptimeMillis() + 10000;
            while (!empty.get() && SystemClock.uptimeMillis() < end) {
                onView(withId(R.id.finder_message)).check((v,e) -> empty.set(((TextView)v).getText().toString().contains("Không có dãy")));
                if (!empty.get()) SystemClock.sleep(100);
            }
            assertTrue(empty.get()); capture("empty");
            onView(withId(R.id.finder_budget_input)).check((v,e) -> assertEquals("0", ((EditText)v).getText().toString()));
        }
    }
}
