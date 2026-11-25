package com.example.nbatickethub.ui.arena;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.nbatickethub.data.AndroidAssetSource;
import com.example.nbatickethub.data.ArenaCatalog;
import com.example.nbatickethub.data.ArenaCatalogReader;
import com.example.nbatickethub.data.mock.MockArenaRepository;
import com.example.nbatickethub.geometry.ArenaSceneCompiler;
import com.example.nbatickethub.model.ArenaProfile;
import com.example.nbatickethub.model.ArenaTheme;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Device-only proof that appearance switches reuse paths and change rendered court pixels. */
@RunWith(AndroidJUnit4.class)
public class ArenaThemeInstrumentedTest {
    @Test public void switchingThemesKeepsSceneAndCachedPaths() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AndroidAssetSource assets = new AndroidAssetSource(context.getAssets());
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
        ArenaProfile profile = catalog.profiles.get(0);
        ArenaRenderData render = new ArenaRenderData(new ArenaSceneCompiler().compile(profile,
                new MockArenaRepository(assets).load(profile.dataAsset)));
        Path seatPath = render.seatPaths[0], markerPath = render.markerPaths[0];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArenaView view = new ArenaView(context);
            view.setArenaTheme(catalog.resolveTheme(profile, null));
            view.setRenderData(render);
            view.layout(0, 0, 1200, 900);
            Set<Integer> courtPixels = new HashSet<>();
            for (ArenaTheme theme : catalog.themes) {
                view.setArenaTheme(theme);
                Bitmap bitmap = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888);
                view.draw(new Canvas(bitmap));
                // Inside court wood, away from center lettering/lines/paint.
                courtPixels.add(bitmap.getPixel(600 + 32, 450 - 40));
                assertEquals(theme.id, view.displayedThemeId());
                assertEquals(profile.id, view.displayedArenaId());
                assertSame(seatPath, render.seatPaths[0]);
                assertSame(markerPath, render.markerPaths[0]);
                bitmap.recycle();
            }
            assertTrue(courtPixels.size() >= 3);
        });
    }
}
