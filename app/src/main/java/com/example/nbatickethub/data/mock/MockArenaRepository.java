package com.example.nbatickethub.data.mock;

import com.example.nbatickethub.data.ArenaDataset;
import com.example.nbatickethub.data.ArenaRepository;
import com.example.nbatickethub.data.AssetSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Offline loading only; deliberately not connected to the template Activity in Phase 2. */
public final class MockArenaRepository implements ArenaRepository {
    private final AssetSource assets;
    private final MockArenaJsonReader parser;

    public MockArenaRepository(AssetSource assets) {
        this(assets, new MockArenaJsonReader());
    }

    public MockArenaRepository(AssetSource assets, MockArenaJsonReader parser) {
        this.assets = Objects.requireNonNull(assets, "assets");
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    @Override
    public ArenaDataset load(String assetPath) throws IOException {
        if (assetPath == null || !assetPath.matches("mock/[a-z0-9_-]+\\.json")) {
            throw new IOException("Expected a JSON manifest under assets/mock/");
        }
        try (InputStream stream = assets.open(assetPath)) {
            if (stream == null) { throw new IOException("AssetSource returned no stream"); }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT))) {
                return parser.read(reader);
            }
        } catch (IOException error) {
            throw new IOException("Cannot load " + assetPath + ": " + error.getMessage(), error);
        }
    }
}
