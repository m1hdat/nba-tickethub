package com.example.nbatickethub.model;

import java.util.Objects;

/** Immutable, explicitly simulated appearance. Reusable with any compatible arena geometry. */
public final class ArenaTheme {
    public static final class Court {
        public final int wood, woodAlternate, lines, apron, paint, lettering, center;
        public final String baselineText, centerText;
        public final CourtDesign design;

        public Court(int wood, int woodAlternate, int lines, int apron, int paint,
                     int lettering, int center, String baselineText, String centerText, CourtDesign design) {
            this.design = Objects.requireNonNull(design, "design");
            this.wood = wood;
            this.woodAlternate = woodAlternate;
            this.lines = lines;
            this.apron = apron;
            this.paint = paint;
            this.lettering = lettering;
            this.center = center;
            this.baselineText = ModelChecks.text(baselineText, "baselineText");
            this.centerText = ModelChecks.text(centerText, "centerText");
            if (baselineText.length() > 32 || centerText.length() > 20) {
                throw new IllegalArgumentException("Court lettering is too long");
            }
            if (contrast(lettering, apron) < 4.5 || contrast(center, wood) < 4.5
                    || contrast(center, woodAlternate) < 4.5) {
                throw new IllegalArgumentException("Court text contrast must be at least 4.5:1");
            }
        }
    }

    public final String id, teamId, name, notice;
    public final Court court;
    public final SeatStatusPalette statuses;
    public final int primaryColor, secondaryColor;
    public final String seatThemeNotice;
    private final ArenaStands.Tier[] seatPalette = new ArenaStands.Tier[16];

    public ArenaTheme(String id, String teamId, String name, String notice, Court court,
                      SeatStatusPalette statuses) {
        this(id, teamId, name, notice, court, statuses, court.apron, court.paint,
                "DESIGNED TEAM SEAT COLORS: simulated theme palette, not physical seat colors.");
    }

    public ArenaTheme(String id, String teamId, String name, String notice, Court court,
                      SeatStatusPalette statuses, int primaryColor, int secondaryColor, String seatThemeNotice) {
        this.id = ModelChecks.id(id);
        this.teamId = ModelChecks.id(teamId);
        this.name = ModelChecks.text(name, "name");
        this.notice = ModelChecks.text(notice, "notice");
        this.court = Objects.requireNonNull(court, "court");
        this.statuses = Objects.requireNonNull(statuses, "statuses");
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.seatThemeNotice = ModelChecks.text(seatThemeNotice, "seatThemeNotice");
        for (int i = 0; i < seatPalette.length; i++) {
            int hue = i % 2 == 0 ? primaryColor : secondaryColor;
            seatPalette[i] = new ArenaStands.Tier(blend(hue, 0xffffffff, Math.max(.56f, .86f - i * .04f)),
                    blend(hue, 0xffffffff, Math.max(.08f, .36f - i * .05f)),
                    blend(hue, 0xff000000, Math.min(.30f, .06f + i * .035f)));
        }
    }
    public ArenaStands.Tier seatTier(int order) {
        if (order < 1 || order > seatPalette.length) { throw new IllegalArgumentException("Missing team seat tier"); }
        return seatPalette[order - 1];
    }
    private static int blend(int first, int second, float amount) {
        int result = 0xff000000;
        for (int shift : new int[]{16, 8, 0}) {
            int a = (first >> shift) & 255, b = (second >> shift) & 255;
            result |= Math.round(a * (1 - amount) + b * amount) << shift;
        }
        return result;
    }
    @Override public String toString() { return name; }

    public static double contrast(int first, int second) {
        double a = luminance(first), b = luminance(second);
        return (Math.max(a, b) + .05) / (Math.min(a, b) + .05);
    }
    private static double luminance(int color) {
        return .2126 * channel((color >> 16) & 255) + .7152 * channel((color >> 8) & 255)
                + .0722 * channel(color & 255);
    }
    private static double channel(int channel) {
        double value = channel / 255.0;
        return value <= .04045 ? value / 12.92 : Math.pow((value + .055) / 1.055, 2.4);
    }
}
