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
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Future<?> pending;
    private int generation;
    private boolean closed;
    private volatile java.util.Map<String, CourtRenderData> courts = java.util.Collections.emptyMap();

    public ArenaPreviewLoader(AssetSource assets) { this.assets = assets; }

    public void catalog(Callback<ArenaCatalog> callback) {
        submit(() -> {
            ArenaCatalog catalog = new ArenaCatalogReader().load(assets);
            java.util.Map<String, CourtRenderData> prepared = new java.util.LinkedHashMap<>();
            for (com.example.nbatickethub.model.ArenaTheme theme : catalog.themes) {
                if (Thread.currentThread().isInterrupted()) { throw new java.io.InterruptedIOException("Courts cancelled"); }
                prepared.put(theme.id, new CourtRenderData(theme, assets));
            }
            courts = java.util.Collections.unmodifiableMap(prepared);
            return catalog;
        }, callback);
    }

    public CourtRenderData court(String themeId) {
        CourtRenderData value = courts.get(themeId);
        if (value == null) { throw new IllegalArgumentException("Court was not prepared"); }
        return value;
    }

    public void arena(ArenaProfile profile, Callback<ArenaRenderData> callback) {
        submit(() -> {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw new IllegalStateException("Arena pipeline must not run on main thread");
            }
            long started = android.os.SystemClock.elapsedRealtime();
            ArenaDataset dataset;
            android.os.Trace.beginSection("Arena.loadValidateGenerate");
            try { dataset = new MockArenaRepository(assets).load(profile.dataAsset); }
            finally { android.os.Trace.endSection(); }
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
