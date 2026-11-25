package com.example.nbatickethub.data;

import com.example.nbatickethub.model.ArenaProfile;
import com.example.nbatickethub.model.ArenaTheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data-driven catalog: no fixed array of three arenas or thirty venues. */
public final class ArenaCatalog {
    public final List<ArenaProfile> profiles;
    public final Map<String, ArenaProfile> byId;
    public final Map<String, ArenaProfile> byHomeTeamId;
    public final List<ArenaTheme> themes;
    public final Map<String, ArenaTheme> themesById;
    private final Map<String, ArenaTheme> themesByTeam;

    public ArenaCatalog(List<ArenaProfile> profiles, List<ArenaTheme> themes) {
        this(profiles, themes, null);
    }

    public ArenaCatalog(List<ArenaProfile> profiles, List<ArenaTheme> themes, Map<String, String> homeArenas) {
        if (profiles.isEmpty()) { throw new IllegalArgumentException("Catalog is empty"); }
        Map<String, ArenaTheme> themeIds = new LinkedHashMap<>();
        Map<String, ArenaTheme> teamThemes = new LinkedHashMap<>();
        if (themes.isEmpty()) { throw new IllegalArgumentException("Theme catalog is empty"); }
        for (ArenaTheme theme : themes) {
            if (themeIds.put(theme.id, theme) != null) {
                throw new IllegalArgumentException("Duplicate theme ID");
            }
            teamThemes.putIfAbsent(theme.teamId, theme);
            if (theme.statuses != themes.get(0).statuses) {
                throw new IllegalArgumentException("Themes must share one status palette");
            }
        }
        Map<String, ArenaProfile> ids = new LinkedHashMap<>();
        Map<String, ArenaProfile> teams = new LinkedHashMap<>();
        for (ArenaProfile profile : profiles) {
            if (!themeIds.containsKey(profile.defaultThemeId)) {
                throw new IllegalArgumentException("Unknown default theme");
            }
            if (profile.previewGameThemeId != null && !themeIds.containsKey(profile.previewGameThemeId)) {
                throw new IllegalArgumentException("Unknown preview game theme");
            }
            if (ids.put(profile.id, profile) != null
                    || teams.put(profile.homeTeam.id, profile) != null) {
                throw new IllegalArgumentException("Duplicate profile/home team mapping");
            }
        }
        this.profiles = Collections.unmodifiableList(new ArrayList<>(profiles));
        this.byId = Collections.unmodifiableMap(ids);
        if (homeArenas != null) {
            teams.clear();
            for (Map.Entry<String, String> entry : homeArenas.entrySet()) {
                if (!teamThemes.containsKey(entry.getKey()) || !ids.containsKey(entry.getValue())) {
                    throw new IllegalArgumentException("Unknown team/arena in home mapping");
                }
                teams.put(entry.getKey(), ids.get(entry.getValue()));
            }
            if (!teams.keySet().equals(teamThemes.keySet())) {
                throw new IllegalArgumentException("Home mapping must cover every team");
            }
        }
        this.byHomeTeamId = Collections.unmodifiableMap(teams);
        this.themes = Collections.unmodifiableList(new ArrayList<>(themes));
        this.themesById = Collections.unmodifiableMap(themeIds);
        this.themesByTeam = Collections.unmodifiableMap(teamThemes);
    }

    public ArenaProfile homeArena(String teamId) {
        ArenaProfile arena = byHomeTeamId.get(teamId);
        if (arena == null) { throw new IllegalArgumentException("No home arena for team: " + teamId); }
        return arena;
    }

    /** Explicit game theme wins; otherwise resolve home team, then venue fallback. */
    public ArenaTheme resolveTheme(ArenaProfile profile, String gameThemeId) {
        if (gameThemeId == null) { gameThemeId = profile.previewGameThemeId; }
        if (gameThemeId != null) {
            ArenaTheme theme = themesById.get(gameThemeId);
            if (theme == null) { throw new IllegalArgumentException("Unknown game theme: " + gameThemeId); }
            return theme;
        }
        ArenaTheme fallback = themesById.get(profile.defaultThemeId);
        if (fallback.teamId.equals(profile.homeTeam.id)) { return fallback; }
        ArenaTheme home = themesByTeam.get(profile.homeTeam.id);
        return home != null ? home : themesById.get(profile.defaultThemeId);
    }
}
