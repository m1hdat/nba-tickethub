package com.example.nbatickethub.geometry;

import com.example.nbatickethub.data.ArenaDataset;
import com.example.nbatickethub.model.ArenaProfile;
import com.example.nbatickethub.model.SeatGeometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Final immutable world-space display geometry. Future hit-testing must use this same snapshot. */
public final class ArenaScene {
    public final ArenaProfile profile;
    public final ArenaDataset dataset;
    public final List<SeatGeometry> seats;

    ArenaScene(ArenaProfile profile, ArenaDataset dataset, List<SeatGeometry> seats) {
        this.profile = profile;
        this.dataset = dataset;
        this.seats = Collections.unmodifiableList(new ArrayList<>(seats));
    }
}
