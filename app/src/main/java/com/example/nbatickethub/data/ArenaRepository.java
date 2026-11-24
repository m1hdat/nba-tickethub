package com.example.nbatickethub.data;

import java.io.IOException;

public interface ArenaRepository {
    /** Blocking load: caller must use a worker thread. Returns only fully validated datasets. */
    ArenaDataset load(String assetPath) throws IOException;
}
