package com.example.nbatickethub.ui.arena;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.ListView;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.nbatickethub.MainActivity;
import com.example.nbatickethub.R;
import com.example.nbatickethub.model.*;
import com.example.nbatickethub.state.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** Production MotionEvent, ListView and CTA paths. Requires an authorized Android device. */
@RunWith(AndroidJUnit4.class)
public class PhaseFourInstrumentedTest {
    private ArenaSelectionViewModel model(MainActivity a) { return new ViewModelProvider(a).get(ArenaSelectionViewModel.class); }
    private void choose(ActivityScenario<MainActivity> scenario, int index, String match) {
        AtomicBoolean ready = new AtomicBoolean(); long deadline = SystemClock.uptimeMillis() + 40000;
        while (!ready.get() && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(a -> {
                if (model(a).sampleGames == null) return;
                if (model(a).navigation.page == BrowseNavigation.Page.HOME) a.findViewById(R.id.nav_schedule).performClick();
                LinearLayout cards = a.findViewById(R.id.game_cards);
                if (a.findViewById(R.id.game_picker).getVisibility() == android.view.View.VISIBLE) {
                    for (int i=0;i<cards.getChildCount();i++) if (match.equals(cards.getChildAt(i).getTag())) { cards.getChildAt(i).performClick(); a.findViewById(R.id.detail_choose_seats).performClick(); break; }
                }
                ready.set(model(a).selection.game() != null && match.equals(model(a).selection.game().id)
                        && ((ArenaView)a.findViewById(R.id.arena_view)).displayedArenaId() != null);
            });
            if (!ready.get()) SystemClock.sleep(50);
        }
        assertTrue("Match timed out", ready.get());
    }
    private void tap(ArenaView view, float x, float y) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap)); bitmap.recycle();
        float[] point = {x,y}; view.viewport.worldToScreen(point);
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,point[0],point[1],0);
        MotionEvent up = MotionEvent.obtain(now,now+30,MotionEvent.ACTION_UP,point[0],point[1],0);
        view.dispatchTouchEvent(down);view.dispatchTouchEvent(up);down.recycle();up.recycle();
    }
    private void settle() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(700);
    }
    private void capture(String name) throws Exception {
        android.app.Instrumentation instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
        String directory = "/sdcard/Download/tickethub-qa";
        for (String command : new String[]{"mkdir -p " + directory, "screencap -p " + directory + "/" + name + ".png"}) {
            try (android.os.ParcelFileDescriptor descriptor = instrumentation.getUiAutomation().executeShellCommand(command);
                 java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
                byte[] buffer = new byte[1024]; while (input.read(buffer) >= 0) { }
            }
        }
    }
    @Test public void pinnedPerspectiveAndTicketsFitAndChangeFromLowToHigh() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            choose(scenario,0,"sample-nyk-bos"); settle(); capture("overview");
            AtomicReference<float[]> camera = new AtomicReference<>();
            AtomicInteger lowPixels = new AtomicInteger();
            scenario.onActivity(a -> {
                ArenaView view=a.findViewById(R.id.arena_view); ArenaRenderData data=view.dataForTest();
                camera.set(view.cameraSnapshot()); tap(view,data.labelPoints[0],data.labelPoints[1]);
                assertEquals(data.dataset().sections.values().iterator().next().id,model(a).selection.browsingSection());
            });
            settle(); capture("low-section");
            scenario.onActivity(a -> {
                ListView list=a.findViewById(R.id.selected_list);
                android.graphics.Rect visible = new android.graphics.Rect();
                assertTrue("Tickets must actually be visible", list.getGlobalVisibleRect(visible));
                assertTrue("At least one visible ticket must fit",visible.height()>=48*a.getResources().getDisplayMetrics().density);
                android.view.View confirm = a.findViewById(R.id.toggle_seat);
                assertEquals("CTA waits for a ticket preview",android.view.View.INVISIBLE,confirm.getVisibility());
                SeatPerspectiveView preview=a.findViewById(R.id.seat_perspective);
                assertEquals(16f/9,preview.getWidth()/(float)preview.getHeight(),.03f);
                Bitmap bitmap=Bitmap.createBitmap(preview.getWidth(),preview.getHeight(),Bitmap.Config.ARGB_8888);
                preview.draw(new Canvas(bitmap)); int[] pixels=new int[bitmap.getWidth()*bitmap.getHeight()];bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());lowPixels.set(Arrays.hashCode(pixels));bitmap.recycle();
                ArenaView view=a.findViewById(R.id.arena_view);ArenaRenderData data=view.dataForTest();int index=data.sectionCount()-1;
                tap(view,data.labelPoints[index*2],data.labelPoints[index*2+1]);
                assertArrayEquals(camera.get(),view.cameraSnapshot(),.001f);
            });
            settle();capture("high-section");
            scenario.onActivity(a -> {
                SeatPerspectiveView preview=a.findViewById(R.id.seat_perspective);
                Bitmap bitmap=Bitmap.createBitmap(preview.getWidth(),preview.getHeight(),Bitmap.Config.ARGB_8888);
                preview.draw(new Canvas(bitmap));int[] pixels=new int[bitmap.getWidth()*bitmap.getHeight()];bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());assertNotEquals(lowPixels.get(),Arrays.hashCode(pixels));bitmap.recycle();
                ArenaSelection state=model(a).selection;ListView list=a.findViewById(R.id.selected_list);
                String id=state.tickets().get(state.tickets().size()-1);list.performItemClick(null,state.tickets().size()-1,state.tickets().size()-1);list.setSelection(state.tickets().size()-1);
                assertEquals(id,state.candidateId());assertTrue(state.selectedIds().isEmpty());
            });
            settle();
            scenario.onActivity(a -> {
                android.graphics.Rect visible = new android.graphics.Rect();
                assertTrue("Ticket list remains visible after preview",a.findViewById(R.id.selected_list).getGlobalVisibleRect(visible));
                assertTrue(visible.height()>=48*a.getResources().getDisplayMetrics().density);
                assertTrue("CTA must actually be visible",a.findViewById(R.id.toggle_seat).getGlobalVisibleRect(visible));
                assertTrue("CTA must be fully in the screen",visible.height()>=47*a.getResources().getDisplayMetrics().density);
            });
            capture("ticket-preview");
            scenario.onActivity(a -> a.findViewById(R.id.fullscreen_view).performClick());
            settle(); capture("fullscreen");
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.fullscreen_close))
                    .perform(androidx.test.espresso.action.ViewActions.click());
            scenario.onActivity(a -> assertArrayEquals(camera.get(),((ArenaView)a.findViewById(R.id.arena_view)).cameraSnapshot(),.001f));
        }
    }

    @Test public void rotationKeepsCandidateFilterSelectionAndUsableTicketPanel() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            choose(scenario,0,"sample-nyk-bos");
            AtomicReference<String> id = new AtomicReference<>();
            scenario.onActivity(a -> {
                ArenaSelection state = model(a).selection;
                String seat = state.dataset().seatOrder.stream().map(value -> value.id).filter(state::available).findFirst().get();
                id.set(seat); state.preview(seat); state.confirm(); state.filterTickets(null,50000); model(a).changed();
                a.findViewById(R.id.toggle_sheet).performClick();
                a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            });
            choose(scenario,0,"sample-nyk-bos");settle();
            scenario.onActivity(a -> {
                assertEquals(android.content.res.Configuration.ORIENTATION_LANDSCAPE,a.getResources().getConfiguration().orientation);
                assertEquals(Collections.singleton(id.get()),model(a).selection.selectedIds());
                assertEquals(id.get(),model(a).selection.candidateId()); assertEquals(50000,model(a).selection.budget());
                android.graphics.Rect visible = new android.graphics.Rect();
                assertTrue(a.findViewById(R.id.selected_list).getGlobalVisibleRect(visible));
                assertTrue(visible.height()>=48*a.getResources().getDisplayMetrics().density);
            });
            capture("landscape");
            scenario.onActivity(a -> a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            choose(scenario,0,"sample-nyk-bos");settle();
            scenario.onActivity(a -> {
                assertEquals(Collections.singleton(id.get()),model(a).selection.selectedIds());
                assertEquals(50000,model(a).selection.budget());
            });
        }
    }

    @Test public void overviewSectionTicketPreviewConfirmationReplacementAndRotation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            choose(scenario,0,"sample-nyk-bos");
            AtomicReference<String> selected = new AtomicReference<>();
            AtomicReference<float[]> camera = new AtomicReference<>();
            scenario.onActivity(a -> {
                ArenaView view = a.findViewById(R.id.arena_view);
                float[] before = view.cameraSnapshot();
                int index = view.dataForTest().sectionCount() / 2;
                ArenaRenderData data = view.dataForTest();
                tap(view,data.labelPoints[index*2],data.labelPoints[index*2+1]);
                assertNotNull(model(a).selection.browsingSection());
                assertTrue(model(a).selection.selectedIds().isEmpty());
                assertTrue(model(a).sheetExpanded);
                assertArrayEquals(before,view.cameraSnapshot(),.001f);
                ArenaSelection state = model(a).selection;
                ListView list = a.findViewById(R.id.selected_list);
                int ticket = 0; while (!state.available(state.tickets().get(ticket))) ticket++;
                selected.set(state.tickets().get(ticket));
                list.performItemClick(null, ticket + list.getHeaderViewsCount(), ticket);
                assertEquals(selected.get(),state.candidateId()); assertTrue(state.selectedIds().isEmpty());
                a.findViewById(R.id.toggle_seat).performClick();
                assertEquals(Collections.singleton(selected.get()),state.selectedIds());
                a.findViewById(R.id.change_seat).performClick();
                String blocked = state.tickets().stream().filter(id -> !state.available(id)).findFirst().get();
                int blockedIndex = state.tickets().indexOf(blocked);
                list.performItemClick(null,blockedIndex+list.getHeaderViewsCount(),blockedIndex);
                assertFalse(a.findViewById(R.id.toggle_seat).isEnabled());
                assertEquals(Collections.singleton(selected.get()),state.selectedIds());
                a.findViewById(R.id.cancel_change).performClick();
                camera.set(view.cameraSnapshot());
                a.findViewById(R.id.toggle_sheet).performClick();
                assertArrayEquals(camera.get(),view.cameraSnapshot(),.001f);
            });
            scenario.recreate(); choose(scenario,0,"sample-nyk-bos");
            scenario.onActivity(a -> {
                assertEquals(Collections.singleton(selected.get()),model(a).selection.selectedIds());
                assertNotNull(model(a).selection.browsingSection());
                assertArrayEquals(camera.get(),((ArenaView)a.findViewById(R.id.arena_view)).cameraSnapshot(),.01f);
                a.findViewById(R.id.show_games).performClick();
            });
            choose(scenario,1,"sample-nyk-mia");
            scenario.onActivity(a -> {
                assertTrue(model(a).selection.selectedIds().isEmpty());
                assertNotNull(model(a).selection.browsingSection());
                assertArrayEquals(camera.get(),((ArenaView)a.findViewById(R.id.arena_view)).cameraSnapshot(),.01f);
                a.findViewById(R.id.show_games).performClick();
            });
            choose(scenario,2,"sample-gsw-lal");
            scenario.onActivity(a -> {
                assertNull(model(a).selection.browsingSection());
                assertEquals("gsw",model(a).selection.game().homeTeamId);
                assertTrue(model(a).selection.selectedIds().isEmpty());
            });
        }
    }
}
