package com.example.nbatickethub.model;

import java.net.URI;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/** Describes origin, not a claim that this application verified an external source. */
public final class DataProvenance {
    public enum OriginType { MOCK, REAL_SNAPSHOT, LIVE_VERIFIED }

    public final OriginType originType;
    public final String version;
    public final long simulationSeed;
    public final String notice;
    public final String sourceUrl;
    public final String collectedAt;

    public DataProvenance(OriginType originType, String version, long simulationSeed,
                          String notice, String sourceUrl, String collectedAt) {
        this.originType = java.util.Objects.requireNonNull(originType, "originType");
        this.version = ModelChecks.text(version, "version");
        this.simulationSeed = simulationSeed;
        this.notice = ModelChecks.text(notice, "notice");
        if (originType == OriginType.MOCK) {
            if (sourceUrl != null || collectedAt != null) {
                throw new IllegalArgumentException("Mock data must not claim an external source/time");
            }
        } else {
            URI uri = URI.create(ModelChecks.text(sourceUrl, "sourceUrl"));
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException("Real data requires an HTTPS source");
            }
            ModelChecks.text(collectedAt, "collectedAt");
            if (!collectedAt.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z")) {
                throw new IllegalArgumentException("Expected UTC collection timestamp");
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
            format.setLenient(false);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            ParsePosition position = new ParsePosition(0);
            if (format.parse(collectedAt, position) == null
                    || position.getIndex() != collectedAt.length()) {
                throw new IllegalArgumentException("Expected UTC collection timestamp");
            }
        }
        this.sourceUrl = sourceUrl;
        this.collectedAt = collectedAt;
    }
}

