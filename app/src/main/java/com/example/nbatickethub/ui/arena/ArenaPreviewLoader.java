package com.example.nbatickethub.ui.arena;

import android.os.Handler;
import android.os.Looper;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.MockArenaRepository;
import com.example.nbatickethub.geometry.ArenaSceneCompiler;
import com.example.nbatickethub.model.ArenaProfile;
import java.util.concurrent.*;

/** Single worker; cancellation and generation guard prevent stale arena publication. */
public final class ArenaPreviewLoader implements AutoCloseable {
    public interface Callback<T> { void complete(T value, Exception error); }
    private final AssetSource assets;
    private android.content.Context context;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Future<?> pending;
    private int generation;
    private boolean closed;
    public java.util.Map<String, Long> startingPrices = java.util.Collections.emptyMap();
    private final java.util.Map<String, android.graphics.Bitmap> logos = new java.util.HashMap<>();
    private volatile java.util.Map<String, CourtRenderData> courts = java.util.Collections.emptyMap();

    public ArenaPreviewLoader(AssetSource assets) { this.assets = assets; }
    public ArenaPreviewLoader(AssetSource assets, android.content.Context context) {
        this.assets = assets; this.context = context.getApplicationContext();
    }

    public void catalog(Callback<ArenaCatalog> callback) {
        submit(() -> {
            ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
            java.util.Map<String, CourtRenderData> prepared = new java.util.LinkedHashMap<>();
            for (com.example.nbatickethub.model.ArenaTheme theme : catalog.themes) {
                if (Thread.currentThread().isInterrupted()) { throw new java.io.InterruptedIOException("Courts cancelled"); }
                prepared.put(theme.id, new CourtRenderData(theme, assets));
                try (java.io.InputStream input = assets.open("logos/" + theme.teamId + ".png")) {
                    android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                    options.inSampleSize = 2;
                    logos.put(theme.teamId, android.graphics.BitmapFactory.decodeStream(input, null, options));
                } catch (java.io.IOException error) {
                    android.util.Log.w("TeamLogo", "Using existing court logo/code fallback: " + theme.teamId);
                }
            }
            courts = java.util.Collections.unmodifiableMap(prepared);
            startingPrices = com.example.nbatickethub.data.mock.MockMatchSchedule.startingPrices(catalog, assets);
            return catalog;
        }, callback);
    }

    public android.graphics.Bitmap teamLogo(com.example.nbatickethub.model.ArenaTheme theme) {
        android.graphics.Bitmap logo = logos.get(theme.teamId);
        return logo == null ? court(theme.id).logoBitmap() : logo;
    }

    public CourtRenderData court(String themeId) {
        CourtRenderData value = courts.get(themeId);
        if (value == null) { throw new IllegalArgumentException("Court was not prepared"); }
        return value;
    }

    public void arena(ArenaProfile profile, Callback<ArenaRenderData> callback) {
        arena(profile, null, null, callback);
    }

    public void arena(ArenaProfile profile, com.example.nbatickethub.model.Game match, ArenaCatalog catalog, Callback<ArenaRenderData> callback) {
        submit(() -> {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("Arena pipeline must not run on main thread");
            }
            long started = android.os.SystemClock.elapsedRealtime();
            ArenaDataset dataset;
            android.os.Trace.beginSection("Arena.loadValidateGenerate");
            try { dataset = new MockArenaRepository(assets).load(profile.dataAsset); }
            finally { android.os.Trace.endSection(); }
            if (match != null) { dataset = com.example.nbatickethub.data.mock.MockMatchSchedule.inventory(dataset, match, catalog); }
            if (match != null && context != null) {
                try (BookingStore store = new BookingStore(context)) { dataset = store.overlay(dataset, match); }
            }
            long loaded = android.os.SystemClock.elapsedRealtime();
            com.example.nbatickethub.geometry.ArenaScene scene;
            android.os.Trace.beginSection("Arena.project");
            try { scene = new ArenaSceneCompiler().compile(profile, dataset); }
            finally { android.os.Trace.endSection(); }
            long projected = android.os.SystemClock.elapsedRealtime();
            ArenaRenderData render;
            android.os.Trace.beginSection("Arena.paths");
            try { render = new ArenaRenderData(scene); }
            finally { android.os.Trace.endSection(); }
            long complete = android.os.SystemClock.elapsedRealtime();
            android.util.Log.i("ArenaPipeline", "arena=" + profile.id + " seats=" + dataset.capacity()
                    + " loadValidateGenerateMs=" + (loaded - started) + " projectMs=" + (projected - loaded)
                    + " pathsMs=" + (complete - projected) + " batches=" + render.seatBatchCount()
                    + " worker=" + Thread.currentThread().getName());
            return render;
        }, callback);
    }

    private <T> void submit(Callable<T> task, Callback<T> callback) {
        if (closed) { return; }
        if (pending != null) { pending.cancel(true); }
        int token = ++generation;
        pending = worker.submit(() -> {
            T value = null;
            Exception failure = null;
            try { value = task.call(); }
            catch (Exception error) { failure = error; }
            final T result = value;
            final Exception error = failure;
            main.post(() -> {
                if (!closed && token == generation) { callback.complete(result, error); }
            });
        });
    }

    @Override public void close() {
        closed = true;
        generation++;
        if (pending != null) { pending.cancel(true); }
        worker.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }
}
