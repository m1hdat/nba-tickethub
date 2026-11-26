package com.example.nbatickethub;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.nbatickethub.state.*;
import com.example.nbatickethub.ui.arena.ArenaView;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import static com.example.nbatickethub.state.BrowseNavigation.Page.*;

@RunWith(AndroidJUnit4.class)
public class PhaseFiveInstrumentedTest {
    private ArenaSelectionViewModel state(MainActivity a) { return new ViewModelProvider(a).get(ArenaSelectionViewModel.class); }
    private void waitFor(ActivityScenario<MainActivity> scenario, java.util.function.Predicate<MainActivity> condition) {
        AtomicBoolean ready = new AtomicBoolean(); long end = SystemClock.uptimeMillis() + 45000;
        while (!ready.get() && SystemClock.uptimeMillis() < end) { scenario.onActivity(a -> ready.set(condition.test(a))); if (!ready.get()) SystemClock.sleep(60); }
        assertTrue("UI did not become ready", ready.get());
    }
    private View tagged(ViewGroup root, String tag) {
        for (int i=0; i<root.getChildCount(); i++) {
            View child = root.getChildAt(i); if (tag.equals(child.getTag())) return child;
            if (child instanceof ViewGroup) { View found = tagged((ViewGroup) child, tag); if (found != null) return found; }
        }
        return null;
    }
    private void visible(View view) { Rect r = new Rect(); assertTrue("Control must be visible", view.getGlobalVisibleRect(r)); assertTrue(r.height() >= 40 * view.getResources().getDisplayMetrics().density); }
    private void capture(String name) throws Exception {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync(); SystemClock.sleep(350);
        for (String command : new String[]{"mkdir -p /sdcard/Download/phase5-qa", "screencap -p /sdcard/Download/phase5-qa/" + name + ".png"}) {
            try (android.os.ParcelFileDescriptor fd = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
                 java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) { byte[] buffer = new byte[1024]; while(input.read(buffer)>=0) { } }
        }
    }
    @Test public void homeDetailSeatBackAndTwoHomeThemesFit() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitFor(scenario, a -> state(a).sampleGames != null && ((LinearLayout)a.findViewById(R.id.featured_match)).getChildCount()>0);
            scenario.onActivity(a -> {
                assertEquals(HOME, state(a).navigation.page);
                View poster=((LinearLayout)a.findViewById(R.id.featured_match)).getChildAt(0);
                assertEquals("sample-nyk-bos", poster.getTag()); visible(poster.findViewById(R.id.open_match));
                assertEquals("Boston Celtics", ((TextView)poster.findViewById(R.id.match_away_name)).getText().toString());
                poster.performClick(); assertEquals(DETAIL, state(a).navigation.page);
            });
            capture("nyk-detail");
            scenario.onActivity(a -> { visible(a.findViewById(R.id.detail_choose_seats)); assertTrue(((TextView)a.findViewById(R.id.detail_price)).getText().toString().contains("86,25")); });
            scenario.recreate();
            waitFor(scenario, a -> state(a).sampleGames!=null && a.findViewById(R.id.detail_choose_seats).isShown());
            scenario.onActivity(a -> { assertEquals(DETAIL,state(a).navigation.page); a.findViewById(R.id.detail_choose_seats).performClick(); });
            waitFor(scenario, a -> "msg".equals(((ArenaView)a.findViewById(R.id.arena_view)).displayedArenaId()));
            scenario.onActivity(a -> { assertEquals("nyk",state(a).selection.game().homeTeamId); a.getOnBackPressedDispatcher().onBackPressed(); assertEquals(DETAIL,state(a).navigation.page); a.findViewById(R.id.detail_back).performClick(); assertEquals(HOME,state(a).navigation.page); });
            capture("home");
            scenario.onActivity(a -> { a.findViewById(R.id.nav_schedule).performClick(); View card=tagged(a.findViewById(R.id.game_cards),"sample-gsw-lal"); assertNotNull(card); card.performClick(); });
            capture("gsw-detail");
            scenario.onActivity(a -> { visible(a.findViewById(R.id.detail_choose_seats)); a.findViewById(R.id.detail_choose_seats).performClick(); });
            waitFor(scenario, a -> "chase".equals(((ArenaView)a.findViewById(R.id.arena_view)).displayedArenaId()));
            scenario.onActivity(a -> assertEquals("gsw-demo",((ArenaView)a.findViewById(R.id.arena_view)).displayedThemeId()));
        }
    }
    @Test public void thirtyTeamsAwayMatchUsesHomeArenaAndRestoresCalendar() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            waitFor(scenario, a -> state(a).sampleGames != null);
            scenario.onActivity(a -> {
                a.findViewById(R.id.nav_teams).performClick(); LinearLayout grid=a.findViewById(R.id.browse_team_grid);
                int count=0; for(int i=0;i<grid.getChildCount();i++)count+=((ViewGroup)grid.getChildAt(i)).getChildCount(); assertEquals(30,count);
                tagged(grid,"nyk").performClick(); assertEquals(SCHEDULE,state(a).navigation.page);
                assertEquals("nyk",state(a).matchList.teamId); assertEquals(MatchListState.Range.ALL_UPCOMING,state(a).matchList.range);
                assertNotNull(tagged(a.findViewById(R.id.game_cards),"sample-nyk-lal-future"));
            });
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a -> {
                ScrollView scroll=a.findViewById(R.id.game_picker); scroll.scrollTo(0,280);
                assertEquals("Test must scroll the laid-out calendar before opening a card",280,scroll.getScrollY());
                View card=tagged(a.findViewById(R.id.game_cards),"sample-bos"); assertNotNull(card); card.performClick();
                assertEquals("sample-bos",state(a).navigation.detailMatchId);
                assertEquals(280,state(a).matchListScroll);
            });
            scenario.recreate();
            waitFor(scenario, a -> state(a).sampleGames!=null && a.findViewById(R.id.detail_choose_seats).isShown());
            scenario.onActivity(a -> { assertEquals(280,state(a).matchListScroll); a.findViewById(R.id.detail_choose_seats).performClick(); });
            waitFor(scenario, a -> state(a).selection.game()!=null && "sample-bos".equals(state(a).selection.game().id) && ((ArenaView)a.findViewById(R.id.arena_view)).displayedArenaId()!=null);
            scenario.onActivity(a -> {
                assertEquals("bos",state(a).selection.game().homeTeamId); assertEquals("nyk",state(a).selection.game().awayTeamId);
                assertEquals(state(a).selection.game().arenaId,((ArenaView)a.findViewById(R.id.arena_view)).displayedArenaId());
                assertEquals("bos-demo",((ArenaView)a.findViewById(R.id.arena_view)).displayedThemeId());
                a.getOnBackPressedDispatcher().onBackPressed(); a.findViewById(R.id.detail_back).performClick();
            });
            waitFor(scenario, a -> state(a).navigation.page == SCHEDULE && ((ScrollView)a.findViewById(R.id.game_picker)).getScrollY() == 280);
            scenario.onActivity(a -> { assertEquals(SCHEDULE,state(a).navigation.page); assertEquals("nyk",state(a).matchList.teamId); assertEquals(280,((ScrollView)a.findViewById(R.id.game_picker)).getScrollY()); });
        }
    }
}
