package com.example.nbatickethub.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Arena-owned neutral surfaces and label contrast. Team-colored seats use ArenaTheme.seatTier(). */
public final class ArenaStands {
    public static final class Tier {
        public final int section, seatBackground, accent;
        public Tier(int section, int seatBackground, int accent) {
            this.section = section;
            this.seatBackground = seatBackground;
            this.accent = accent;
        }
    }
    public final String notice;
    public final int background, concourse, labelBackground, labelText, rowLine, sectionBorder;
    private final List<Tier> tiers;
    public ArenaStands(String notice, int background, int concourse, int labelBackground, int labelText,
                       int rowLine, int sectionBorder, List<Tier> tiers) {
        this.notice = ModelChecks.text(notice, "stands notice");
        if (ArenaTheme.contrast(labelText, labelBackground) < 4.5 || tiers.isEmpty()
                || tiers.size() > 16 || tiers.contains(null)) {
            throw new IllegalArgumentException("Invalid stands palette/label contrast");
        }
        this.background = background;
        this.concourse = concourse;
        this.labelBackground = labelBackground;
        this.labelText = labelText;
        this.rowLine = rowLine;
        this.sectionBorder = sectionBorder;
        this.tiers = Collections.unmodifiableList(new ArrayList<>(tiers));
    }
    /** Retained reference swatches; renderers must use ArenaTheme.seatTier for team-colored seats. */
    public Tier tier(int order) {
        if (order < 1 || order > tiers.size()) { throw new IllegalArgumentException("Missing arena tier palette"); }
        return tiers.get(order - 1);
    }
    public int tierCount() { return tiers.size(); }
}
