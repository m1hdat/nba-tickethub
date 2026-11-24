package com.example.nbatickethub.model;

/** Immutable Game data. */
public final class Game {
    public final String id;
    public final String homeTeamId;
    public final String awayTeamId;
    public final String arenaId;
    public final String displayTime;
    public final DataProvenance provenance;

    public Game(String id, String homeTeamId, String awayTeamId, String arenaId, String displayTime, DataProvenance provenance) {
        this.id = ModelChecks.id(id);
        this.homeTeamId = ModelChecks.id(homeTeamId);
        this.awayTeamId = ModelChecks.id(awayTeamId);
        this.arenaId = ModelChecks.id(arenaId);
        this.displayTime = ModelChecks.text(displayTime, "displayTime");
        this.provenance = java.util.Objects.requireNonNull(provenance, "provenance");
        if (homeTeamId.equals(awayTeamId)) { throw new IllegalArgumentException("Teams must differ"); }
    }
}

