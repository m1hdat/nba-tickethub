package com.example.nbatickethub.data;

import android.content.res.AssetManager;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Thin Android adapter. No Activity reference and no main-thread asset I/O. */
public final class AndroidAssetSource implements AssetSource {
    private final AssetManager assets;

    public AndroidAssetSource(AssetManager assets) {
        this.assets = Objects.requireNonNull(assets, "assets");
    }

    @Override
    public InputStream open(String path) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Load arena assets on a worker executor");
        }
        return assets.open(path, AssetManager.ACCESS_STREAMING);
    }
}
