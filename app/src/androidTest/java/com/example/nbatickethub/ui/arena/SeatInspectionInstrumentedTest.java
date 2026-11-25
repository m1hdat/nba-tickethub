package com.example.nbatickethub.ui.arena;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.InputDevice;
import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.MockArenaRepository;
import com.example.nbatickethub.geometry.ArenaSceneCompiler;
import com.example.nbatickethub.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Real Matrix + MotionEvent + Canvas integration. Requires Android runtime. */
@RunWith(AndroidJUnit4.class)
public class SeatInspectionInstrumentedTest {
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
        MotionEvent event = MotionEvent.obtain(now, now, action, ids.length, properties, coordinates,
                0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        assertTrue(view.onTouchEvent(event));
        event.recycle();
    }

    private void tap(ArenaView view, float[] screen) {
        send(view, MotionEvent.ACTION_DOWN, new int[]{7}, screen[0], screen[1]);
        send(view, MotionEvent.ACTION_UP, new int[]{7}, screen[0], screen[1]);
    }

    private float[] center(SeatGeometry shape) {
        float[] result = new float[2];
        for (int v = 0; v < 4; v++) { result[0] += shape.x(v) / 4; result[1] += shape.y(v) / 4; }
        return result;
    }

    private float[] focus(ArenaView view, float[] world) {
        float[] screen = world.clone();
        view.viewport.worldToScreen(screen);
        view.viewport.zoom(10000, screen[0], screen[1], 500, 400);
        screen = world.clone();
        view.viewport.worldToScreen(screen);
        return screen;
    }

    @Test public void inverseTapTracksSeatsRejectsAislesAndClearsOldArenaInspection() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AndroidAssetSource assets = new AndroidAssetSource(context.getAssets());
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
        List<ArenaRenderData> scenes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ArenaProfile profile = catalog.profiles.get(i);
            scenes.add(new ArenaRenderData(new ArenaSceneCompiler().compile(profile,
                    new MockArenaRepository(assets).load(profile.dataAsset))));
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArenaView view = new ArenaView(context);
            view.layout(0, 0, 1000, 800);
            Bitmap bitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            AtomicInteger callbacks = new AtomicInteger();
            for (ArenaRenderData data : scenes) {
                view.setArenaTheme(catalog.resolveTheme(data.scene.profile, null));
                view.setRenderData(data);
                assertNull(view.highlightedSeatId());
                Seat target = data.seatsByGeometry[data.seatsByGeometry.length / 3];
                float[] world = center(data.scene.seats.get(target.geometryIndex));
                view.setSeatInspectionListener((seat, row, section) -> {
                    callbacks.incrementAndGet();
                    if (seat != null) {
                        assertEquals(target.id, seat.id);
                        assertEquals(target.rowId, row.id);
                        assertEquals(data.scene.dataset.rows.get(target.rowId).sectionId, section.id);
                    }
                });
                view.draw(canvas);
                float[] screen = world.clone();
                view.viewport.worldToScreen(screen);
                int before = callbacks.get();
                tap(view, screen); // Overview has real seat coordinates but no individual seats drawn.
                assertEquals(before, callbacks.get());
                screen = focus(view, world);
                tap(view, screen); // Camera changed but the corresponding frame has not been drawn yet.
                assertEquals(before, callbacks.get());
                view.draw(canvas);
                tap(view, screen);
                assertEquals(before + 1, callbacks.get());
                assertEquals(target.id, view.highlightedSeatId());
                view.draw(canvas);
                assertFalse(view.drawnSeatBatches == 0);

                view.viewport.zoom(.9f, screen[0], screen[1], screen[0], screen[1]);
                view.viewport.pan(25, -20);
                view.draw(canvas);
                screen = world.clone();
                view.viewport.worldToScreen(screen);
                tap(view, screen);
                assertEquals(before + 2, callbacks.get());
                assertEquals(target.id, view.highlightedSeatId());

                // The projected midpoint of an actual section aisle, not a seat AABB corner.
                List<Section> sections = new ArrayList<>(data.scene.dataset.sections.values());
                AnnularSector first = sections.get(0).boundary, next = sections.get(1).boundary;
                double angle = Math.toRadians((first.endDegrees + next.startDegrees) / 2);
                float radius = (first.innerRadius + first.outerRadius) / 2;
                float[] aisle = new float[2];
                ArenaSceneCompiler.project(data.scene.profile, data.scene.dataset,
                        first.centerX + radius * (float) Math.cos(angle),
                        first.centerY + radius * (float) Math.sin(angle), aisle);
                screen = focus(view, aisle);
                view.draw(canvas);
                tap(view, screen);
                assertEquals(before + 3, callbacks.get());
                assertNull(view.highlightedSeatId());

                screen = focus(view, world);
                view.draw(canvas);
                tap(view, screen);
                assertEquals(target.id, view.highlightedSeatId());
                view.setSeatInspectionListener(null);
                // Next loop switches the dataset while a seat is still highlighted.
            }
            view.setRenderData(null);
            assertNull(view.highlightedSeatId());
            bitmap.recycle();
        });
    }

    @Test public void dragCancelAndPinchReleaseEitherFingerNeverProduceTap() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AndroidAssetSource assets = new AndroidAssetSource(context.getAssets());
        ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
        ArenaProfile profile = catalog.profiles.get(0);
        ArenaRenderData data = new ArenaRenderData(new ArenaSceneCompiler().compile(profile,
                new MockArenaRepository(assets).load(profile.dataAsset)));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArenaView view = new ArenaView(context);
            view.layout(0, 0, 1000, 800);
            view.setArenaTheme(catalog.resolveTheme(profile, null));
            view.setRenderData(data);
            Bitmap bitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            float[] world = center(data.scene.seats.get(data.scene.seats.size() / 3));
            float[] screen = focus(view, world);
            view.draw(canvas);
            AtomicInteger callbacks = new AtomicInteger();
            view.setSeatInspectionListener((seat, row, section) -> callbacks.incrementAndGet());
            send(view, MotionEvent.ACTION_DOWN, new int[]{7}, screen[0], screen[1]);
            send(view, MotionEvent.ACTION_MOVE, new int[]{7}, screen[0] + 100, screen[1]);
            view.draw(canvas);
            send(view, MotionEvent.ACTION_UP, new int[]{7}, screen[0] + 100, screen[1]);
            assertEquals(0, callbacks.get());
            screen = focus(view, world);
            view.draw(canvas);
            send(view, MotionEvent.ACTION_DOWN, new int[]{7}, screen[0] + 100, screen[1]);
            send(view, MotionEvent.ACTION_UP, new int[]{7}, screen[0], screen[1]); // No MOVE delivered.
            assertEquals(0, callbacks.get());
            send(view, MotionEvent.ACTION_DOWN, new int[]{7}, screen[0], screen[1]);
            send(view, MotionEvent.ACTION_CANCEL, new int[]{7}, screen[0], screen[1]);
            send(view, MotionEvent.ACTION_UP, new int[]{7}, screen[0], screen[1]);
            assertEquals(0, callbacks.get());
            send(view, MotionEvent.ACTION_DOWN, new int[]{7}, screen[0], screen[1]);
            long now = android.os.SystemClock.uptimeMillis();
            MotionEvent batched = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE,
                    screen[0] + 100, screen[1], 0);
            batched.addBatch(now + 1, screen[0], screen[1], 1, 1, 0);
            assertTrue(view.onTouchEvent(batched));
            batched.recycle();
            send(view, MotionEvent.ACTION_UP, new int[]{7}, screen[0], screen[1]);
            assertEquals(0, callbacks.get()); // A coalesced excursion must not turn back into a tap.
            for (int lifted = 0; lifted < 2; lifted++) {
                focus(view, world);
                view.draw(canvas);
                send(view, MotionEvent.ACTION_DOWN, new int[]{7}, 480, 400);
                send(view, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                        new int[]{7, 13}, 480, 400, 520, 400);
                send(view, MotionEvent.ACTION_MOVE, new int[]{7, 13}, 470, 400, 530, 400);
                view.draw(canvas);
                send(view, MotionEvent.ACTION_POINTER_UP | (lifted << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                        new int[]{7, 13}, 470, 400, 530, 400);
                int remaining = lifted == 0 ? 13 : 7;
                float x = lifted == 0 ? 530 : 470;
                send(view, MotionEvent.ACTION_UP, new int[]{remaining}, x, 400);
                assertEquals(0, callbacks.get());
            }
            screen = focus(view, world);
            view.draw(canvas);
            tap(view, screen); // Suppression resets for the next independent single tap.
            assertEquals(1, callbacks.get());
            assertNotNull(view.highlightedSeatId());
            bitmap.recycle();
        });
    }
}
