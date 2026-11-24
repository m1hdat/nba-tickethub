package com.example.nbatickethub.data;

import java.io.IOException;
import java.io.InputStream;

/** Injectable stream boundary; production uses Android assets, JVM tests use the same JSON files. */
public interface AssetSource {
    InputStream open(String path) throws IOException;
}
