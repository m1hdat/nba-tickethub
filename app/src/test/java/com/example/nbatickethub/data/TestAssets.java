package com.example.nbatickethub.data;

import com.example.nbatickethub.data.mock.MockArenaJsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class TestAssets {
    private TestAssets() { }

    static String text(String name) throws IOException {
        Path base = Paths.get("src/main/assets/mock");
        if (!Files.isDirectory(base)) { base = Paths.get("app/src/main/assets/mock"); }
        return new String(Files.readAllBytes(base.resolve(name + ".json")), StandardCharsets.UTF_8);
    }

    static ArenaDataset parse(String json) throws IOException {
        return new MockArenaJsonReader().read(new StringReader(json));
    }

    static ArenaDataset small() throws IOException { return parse(text("arena-small")); }
}
