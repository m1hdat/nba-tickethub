package com.example.nbatickethub.data;

import com.example.nbatickethub.data.mock.MockArenaRepository;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class RepositoryTest {
    @Test
    public void readsTheAssetThroughTheInjectedSourceAndClosesIt() throws IOException {
        AtomicBoolean closed = new AtomicBoolean();
        String json = TestAssets.text("arena-small");
        MockArenaRepository repository = new MockArenaRepository(path -> {
            assertEquals("mock/arena-small.json", path);
            return tracking(json.getBytes(StandardCharsets.UTF_8), closed);
        });
        assertEquals(200, repository.load("mock/arena-small.json").capacity());
        assertTrue(closed.get());
    }

    @Test
    public void malformedJsonClosesStreamAndReportsAssetName() {
        AtomicBoolean closed = new AtomicBoolean();
        MockArenaRepository repository = new MockArenaRepository(path -> tracking(new byte[]{'{'}, closed));
        IOException error = assertThrows(IOException.class, () -> repository.load("mock/bad.json"));
        assertTrue(error.getMessage().contains("mock/bad.json"));
        assertTrue(closed.get());
    }

    @Test
    public void invalidUtf8IsNotSilentlyReplaced() {
        MockArenaRepository repository = new MockArenaRepository(path ->
                new ByteArrayInputStream(new byte[]{'"', (byte) 0xc3, '"'}));
        assertThrows(IOException.class, () -> repository.load("mock/bad.json"));
    }

    @Test
    public void rejectsTraversalWithoutOpeningSource() {
        AtomicBoolean opened = new AtomicBoolean();
        MockArenaRepository repository = new MockArenaRepository(path -> {
            opened.set(true);
            throw new IOException("Not expected");
        });
        for (String path : new String[]{"../secret.json", "mock/../arena-small.json", "/mock/a.json", null}) {
            assertThrows(IOException.class, () -> repository.load(path));
        }
        assertFalse(opened.get());
    }

    @Test
    public void missingAssetAndReadErrorsPropagate() {
        MockArenaRepository repository = new MockArenaRepository(path -> { throw new IOException("Missing"); });
        assertThrows(IOException.class, () -> repository.load("mock/missing.json"));
    }

    private ByteArrayInputStream tracking(byte[] bytes, AtomicBoolean closed) {
        return new ByteArrayInputStream(bytes) {
            @Override
            public void close() throws IOException { closed.set(true); super.close(); }
        };
    }
}
