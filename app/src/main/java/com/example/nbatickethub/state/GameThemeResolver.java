package com.example.nbatickethub.state;

import com.example.nbatickethub.data.ArenaCatalog;
import com.example.nbatickethub.model.ArenaTheme;
import com.example.nbatickethub.model.Game;

/** Game identity, rather than a visual override, owns the screen's home-team accent. */
public final class GameThemeResolver {
    private GameThemeResolver() { }
    public static ArenaTheme resolve(ArenaCatalog catalog, Game game) {
        for (ArenaTheme theme : catalog.themes) {
            if (theme.teamId.equals(game.homeTeamId)) { return theme; }
        }
        throw new IllegalArgumentException("Missing home-team theme: " + game.homeTeamId);
    }
}
