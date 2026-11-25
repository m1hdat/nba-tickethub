package com.example.nbatickethub.ui.arena;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.nbatickethub.data.AndroidAssetSource;
import com.example.nbatickethub.data.ArenaCatalog;
import com.example.nbatickethub.data.ArenaCatalogReader;
import com.example.nbatickethub.data.mock.MockArenaRepository;
import com.example.nbatickethub.geometry.ArenaSceneCompiler;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ArenaCameraInstrumentedTest {
    private void send(ArenaView view, int action, int[] ids, float... positions) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[ids.length];
        for (int i = 0; i < ids.length; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = ids[i];
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coordinates[i] = new MotionEvent.PointerCoords();
            coordinates[i].x = positions[2 * i];
            coordinates[i].y = positions[2 * i + 1];
            coordinates[i].pressure = coordinates[i].size = 1;
        }
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, action, ids.length, properties,
                coordinates, 0, 0, 1, 1, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0);
        assertTrue(view.onTouchEvent(event));
        event.recycle();
    }

    @Test public void pinchReleaseEitherFingerThenDragDoesNotJumpAndCacheIsReused() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AndroidAssetSource assets = new AndroidAssetSource(context.getAssets());
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
        ArenaRenderData data = new ArenaRenderData(new ArenaSceneCompiler().compile(catalog.profiles.get(0),
                new MockArenaRepository(assets).load(catalog.profiles.get(0).dataAsset)));
        Path cached = data.seatPaths[0];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArenaView view = new ArenaView(context);
            view.layout(0, 0, 1000, 800);
            view.setArenaTheme(catalog.themes.get(0));
            view.setRenderData(data);
            for (int lifted = 0; lifted < 2; lifted++) {
                view.viewport.reset();
                send(view, MotionEvent.ACTION_DOWN, new int[]{7}, 450, 400);
                send(view, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                        new int[]{7, 13}, 450, 400, 550, 400);
                send(view, MotionEvent.ACTION_MOVE, new int[]{7, 13}, 300, 400, 700, 400);
                assertEquals(4, view.viewport.scale() / view.viewport.fitScale(), .001f);
                float x = view.viewport.translationX(), y = view.viewport.translationY();
                send(view, MotionEvent.ACTION_POINTER_UP | (lifted << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                        new int[]{7, 13}, 300, 400, 700, 400);
                int id = lifted == 0 ? 13 : 7;
                float remainingX = lifted == 0 ? 700 : 300;
                send(view, MotionEvent.ACTION_MOVE, new int[]{id}, remainingX, 400);
                assertEquals(x, view.viewport.translationX(), .001f);
                assertEquals(y, view.viewport.translationY(), .001f);
                send(view, MotionEvent.ACTION_MOVE, new int[]{id}, remainingX + 20, 410);
                assertEquals(x + 20, view.viewport.translationX(), .001f);
                assertEquals(y + 10, view.viewport.translationY(), .001f);
                send(view, MotionEvent.ACTION_CANCEL, new int[]{id}, remainingX + 20, 410);
            }
            Bitmap bitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.viewport.reset();
            view.draw(canvas);
            assertEquals(0, view.drawnSeatBatches);
            assertTrue(view.drawnSections > 0);
            float[] focus = {data.seatBounds[0].centerX(), data.seatBounds[0].centerY()};
            view.viewport.worldToScreen(focus);
            view.viewport.zoom(10000, focus[0], focus[1], 500, 400);
            view.draw(canvas);
            assertTrue(view.drawnSeatBatches > 0);
            assertTrue(view.drawnSeatBatches < data.seatBatchCount());
            assertTrue(view.drawnSections < data.sectionCount());
            assertSame(cached, data.seatPaths[0]);
            assertTrue(view.lastDrawNanos > 0);
            bitmap.recycle();
        });
    }
}
