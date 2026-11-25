package com.example.nbatickethub.model;

import java.util.Objects;

/** Venue identity and geometry plus a theme reference; appearance is held separately. */
public final class ArenaProfile {
    public final String id, venueName, dataAsset, identitySource, referenceSource, retrievedOn, geometryNotice;
    public final Team homeTeam;
    public final ArenaGeometry geometry;
    public final ArenaStands stands;
    public final String defaultThemeId;
    public final String previewGameThemeId;

    public ArenaProfile(String id, String venueName, Team homeTeam, String dataAsset,
                        String identitySource, String referenceSource, String retrievedOn,
                        String geometryNotice, ArenaGeometry geometry, ArenaStands stands, String defaultThemeId) {
        this(id, venueName, homeTeam, dataAsset, identitySource, referenceSource, retrievedOn,
                geometryNotice, geometry, stands, defaultThemeId, null);
    }

    public ArenaProfile(String id, String venueName, Team homeTeam, String dataAsset,
                        String identitySource, String referenceSource, String retrievedOn,
                        String geometryNotice, ArenaGeometry geometry, ArenaStands stands, String defaultThemeId,
                        String previewGameThemeId) {
        this.id = ModelChecks.id(id);
        this.venueName = ModelChecks.text(venueName, "venueName");
        this.homeTeam = Objects.requireNonNull(homeTeam, "homeTeam");
        if (dataAsset == null || !dataAsset.matches("mock/[a-z0-9_-]+\\.json")) {
            throw new IllegalArgumentException("Invalid dataset asset");
        }
        if (identitySource == null || !identitySource.startsWith("https://")
                || referenceSource == null || !referenceSource.startsWith("https://")) {
            throw new IllegalArgumentException("Venue identity and reference need source URLs");
        }
        if (retrievedOn == null || !retrievedOn.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new IllegalArgumentException("Source retrieval date required");
        }
        this.dataAsset = dataAsset;
        this.identitySource = identitySource;
        this.referenceSource = referenceSource;
        this.retrievedOn = retrievedOn;
        this.geometryNotice = ModelChecks.text(geometryNotice, "geometryNotice");
        this.geometry = Objects.requireNonNull(geometry, "geometry");
        this.stands = Objects.requireNonNull(stands, "stands");
        this.defaultThemeId = ModelChecks.id(defaultThemeId);
        this.previewGameThemeId = previewGameThemeId == null ? null : ModelChecks.id(previewGameThemeId);
    }

    @Override public String toString() { return venueName; }
}
