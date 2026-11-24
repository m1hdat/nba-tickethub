package com.example.nbatickethub.model;

/** Immutable Team data. */
public final class Team {
    public final String id;
    public final String displayName;
    public final String shortName;

    public Team(String id, String displayName, String shortName) {
        this.id = ModelChecks.id(id);
        this.displayName = ModelChecks.text(displayName, "displayName");
        this.shortName = ModelChecks.text(shortName, "shortName");
    }
}

