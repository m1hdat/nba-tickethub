package com.example.nbatickethub;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.widget.Spinner;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.MockArenaRepository;
import com.example.nbatickethub.geometry.ArenaSceneCompiler;
import com.example.nbatickethub.model.ArenaProfile;
import com.example.nbatickethub.ui.arena.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import android.os.Looper;
import static org.junit.Assert.*;

/** Requires a device/emulator. Bitmap checks prove drawing, not visual quality or FPS. */
@RunWith(AndroidJUnit4.class)
public class ArenaPreviewInstrumentedTest {
    @Test public void assetIoRunsOnWorkerAndStaleLoadsCannotPublish() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AndroidAssetSource androidAssets = new AndroidAssetSource(context.getAssets());
        ArenaCatalog catalog = new ArenaCatalogReader().load(androidAssets);
        AtomicBoolean onlyWorker = new AtomicBoolean(true);
        AssetSource audited = name -> {
            if (Looper.myLooper() == Looper.getMainLooper()) { onlyWorker.set(false); }
            return androidAssets.open(name);
        };
        AtomicReference<ArenaPreviewLoader> loader = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        AtomicInteger callbacks = new AtomicInteger();
        CountDownLatch complete = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assertThrows(IllegalStateException.class, () -> androidAssets.open("venues/catalog.json"));
            loader.set(new ArenaPreviewLoader(audited));
            loader.get().arena(catalog.profiles.get(0), (data, error) -> callbacks.incrementAndGet());
            loader.get().arena(catalog.profiles.get(2), (data, error) -> {
                callbacks.incrementAndGet();
                failure.set(error);
                assertEquals(Looper.getMainLooper(), Looper.myLooper());
                if (data != null) { assertEquals("crypto", data.arenaId()); }
                complete.countDown();
            });
        });
        try {
            assertTrue("Worker load timed out", complete.await(30, TimeUnit.SECONDS));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertNull(failure.get());
            assertTrue(onlyWorker.get());
            assertEquals(1, callbacks.get());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> loader.get().close());
        }
    }

    @Test public void allThirtyScenesDrawOnAndroidCanvasInPortraitAndLandscape() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AndroidAssetSource assets = new AndroidAssetSource(context.getAssets());
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
        for (ArenaProfile profile : catalog.profiles) {
            ArenaRenderData render = new ArenaRenderData(new ArenaSceneCompiler().compile(profile,
                    new MockArenaRepository(assets).load(profile.dataAsset)));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                ArenaView view = new ArenaView(context);
                view.setArenaTheme(catalog.resolveTheme(profile, null));
                view.setRenderData(render);
                for (int[] size : new int[][]{{1080, 1400}, {1400, 800}}) {
                    view.layout(0, 0, size[0], size[1]);
                    Bitmap bitmap = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
                    view.draw(new Canvas(bitmap));
                    int background = bitmap.getPixel(0, 0);
                    assertNotEquals(background, bitmap.getPixel(size[0] / 2, size[1] / 2));
                    assertEquals(profile.id, view.displayedArenaId());
                    assertTrue(render.seatCount() > 15000);
                    bitmap.recycle();
                }
                view.setRenderData(null);
                assertNull(view.displayedArenaId());
            });
        }
    }

    @Test public void launcherSwitchesVenuesAndRestoresVenueAfterRecreate() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitArena(scenario, "msg");
            scenario.onActivity(activity -> ((Spinner) activity.findViewById(R.id.arena_selector)).setSelection(1));
            awaitArena(scenario, "chase");
            scenario.onActivity(activity -> ((Spinner) activity.findViewById(R.id.arena_selector)).setSelection(2));
            awaitArena(scenario, "crypto");
            scenario.onActivity(activity -> ((Spinner) activity.findViewById(R.id.theme_selector)).setSelection(20));
            awaitArena(scenario, "msg");
            scenario.recreate();
            awaitArena(scenario, "msg");
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.reset_view).isEnabled());
                assertTrue(activity.findViewById(R.id.arena_view).getHeight() > 0);
                assertEquals("nyk-demo", ((ArenaView) activity.findViewById(R.id.arena_view)).displayedThemeId());
            });
        }
    }

    @Test public void thirtyTeamSelectionsLoadTheirMappedHomeAndIdentity() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ArenaCatalog catalog = new ArenaCatalogReader().load(new AndroidAssetSource(context.getAssets()));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitArena(scenario, "msg");
            for (int i = 0; i < catalog.themes.size(); i++) {
                final int position = i + 1;
                com.example.nbatickethub.model.ArenaTheme theme = catalog.themes.get(i);
                scenario.onActivity(activity -> ((Spinner) activity.findViewById(R.id.theme_selector)).setSelection(position));
                awaitArena(scenario, catalog.homeArena(theme.teamId).id);
                scenario.onActivity(activity -> {
                    assertEquals(theme.id, ((ArenaView) activity.findViewById(R.id.arena_view)).displayedThemeId());
                    assertEquals(theme.name, ((android.widget.TextView) activity.findViewById(R.id.venue_title)).getText().toString());
                });
            }
        }
    }

    private void awaitArena(ActivityScenario<MainActivity> scenario, String id) {
        long deadline = SystemClock.uptimeMillis() + 20000;
        AtomicBoolean ready = new AtomicBoolean();
        do {
            scenario.onActivity(activity -> ready.set(id.equals(
                    ((ArenaView) activity.findViewById(R.id.arena_view)).displayedArenaId())));
            if (ready.get()) { return; }
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        fail("Timed out loading arena " + id);
    }
}
