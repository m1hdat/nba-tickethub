package com.example.nbatickethub.ui.arena;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import androidx.core.graphics.ColorUtils;
import com.example.nbatickethub.R;
import com.example.nbatickethub.data.ArenaCatalog;
import com.example.nbatickethub.model.*;
import com.example.nbatickethub.state.*;
import java.util.*;
import java.util.function.Consumer;
import static com.example.nbatickethub.state.BrowseNavigation.Page.*;

/** Owns browse Views only; the Phase 4 Canvas/selection pipeline remains independent. */
public final class BrowseController {
    private final Activity activity;
    private final ArenaSelectionViewModel model;
    private final ArenaCatalog catalog;
    private final ArenaPreviewLoader loader;
    private final List<Game> games;
    private final Consumer<Game> seats;
    private final Runnable beforeNavigate, refreshCalendar;
    private BrowseNavigation.Page lastPage;
    private Runnable ticketsAction = () -> { };
    public void setTicketsAction(Runnable action) { ticketsAction = action; }

    public BrowseController(Activity activity, ArenaSelectionViewModel model, ArenaCatalog catalog,
                            ArenaPreviewLoader loader, List<Game> games, Consumer<Game> seats,
                            Runnable beforeNavigate, Runnable refreshCalendar) {
        this.activity = activity; this.model = model; this.catalog = catalog; this.loader = loader;
        this.games = games; this.seats = seats; this.beforeNavigate = beforeNavigate;
        this.refreshCalendar = refreshCalendar;
        activity.findViewById(R.id.nav_home).setOnClickListener(v -> tab(HOME));
        activity.findViewById(R.id.nav_schedule).setOnClickListener(v -> tab(SCHEDULE));
        activity.findViewById(R.id.nav_teams).setOnClickListener(v -> tab(TEAMS));
        activity.findViewById(R.id.nav_tickets).setOnClickListener(v -> tab(TICKETS));
        activity.findViewById(R.id.browse_all_matches).setOnClickListener(v -> tab(SCHEDULE));
        activity.findViewById(R.id.detail_back).setOnClickListener(v -> back());
        activity.findViewById(R.id.detail_choose_seats).setOnClickListener(v -> {
            Game game = game(model.navigation.detailMatchId);
            if (game != null && game.startsAtMillis > System.currentTimeMillis()) { capture(); seats.accept(game); }
        });
        populateTeams(R.id.browse_team_grid);
        populateTeams(R.id.home_teams);
        render();
    }
    public void capture() {
        BrowseNavigation n = model.navigation;
        if (n.page == HOME) n.homeScroll = ((ScrollView) activity.findViewById(R.id.browse_home)).getScrollY();
        if (n.page == TEAMS) n.teamsScroll = ((ScrollView) activity.findViewById(R.id.browse_teams)).getScrollY();
        if (n.page == DETAIL) n.detailScroll = ((ScrollView) activity.findViewById(R.id.detail_scroll)).getScrollY();
        if (n.page == SCHEDULE) model.matchListScroll = ((ScrollView) activity.findViewById(R.id.game_picker)).getScrollY();
        beforeNavigate.run();
    }
    public void tab(BrowseNavigation.Page page) { capture(); model.navigation.tab(page); render(); }
    public void detail(Game game) { capture(); model.navigation.detail(game.id); render(); }
    public boolean back() { capture(); if (!model.navigation.back()) return false; render(); return true; }

    public void render() {
        BrowseNavigation n = model.navigation;
        model.choosingGame = n.page != ARENA;
        visible(R.id.browse_shell, n.page != ARENA);
        visible(R.id.arena_screen, n.page == ARENA);
        visible(R.id.browse_home, n.page == HOME);
        visible(R.id.game_picker, n.page == SCHEDULE);
        visible(R.id.browse_teams, n.page == TEAMS);
        visible(R.id.my_tickets, n.page == TICKETS);
        if (n.page == TICKETS) ticketsAction.run();
        visible(R.id.browse_detail, n.page == DETAIL);
        visible(R.id.browse_nav, n.page != DETAIL);
        int[] ids = {R.id.nav_home, R.id.nav_schedule, R.id.nav_teams, R.id.nav_tickets};
        BrowseNavigation.Page[] pages = {HOME, SCHEDULE, TEAMS, TICKETS};
        for (int i = 0; i < ids.length; i++) {
            Button button = activity.findViewById(ids[i]); button.setSelected(n.page == pages[i]);
            button.setTextColor(activity.getColor(n.page == pages[i] ? R.color.browse_accent : R.color.browse_muted));
        }
        if (n.page == HOME) { renderHome(); restore(R.id.browse_home, n.homeScroll); }
        if (n.page == TEAMS) restore(R.id.browse_teams, n.teamsScroll);
        if (n.page == SCHEDULE) { refreshCalendar.run(); restore(R.id.game_picker, model.matchListScroll); }
        if (n.page == DETAIL) { renderDetail(game(n.detailMatchId)); restore(R.id.detail_scroll, n.detailScroll); }
        if (lastPage != null && lastPage != n.page && n.page != ARENA) {
            int active = n.page == HOME ? R.id.browse_home : n.page == TEAMS ? R.id.browse_teams
                    : n.page == DETAIL ? R.id.browse_detail : n.page == TICKETS ? R.id.my_tickets : R.id.game_picker;
            View page = activity.findViewById(active);
            page.animate().cancel(); page.setAlpha(0f); page.animate().alpha(1f).setDuration(140).start();
        }
        lastPage = n.page;
    }
    private void renderHome() {
        List<Game> upcoming;
        // Home feature is the nearest upcoming game; calendar filters do not change it.
        MatchListState all = new MatchListState(); all.range = MatchListState.Range.ALL_UPCOMING;
        upcoming = all.filter(games, System.currentTimeMillis(), TimeZone.getDefault());
        LinearLayout feature = activity.findViewById(R.id.featured_match), list = activity.findViewById(R.id.upcoming_matches);
        feature.removeAllViews(); list.removeAllViews();
        for (int i = 0; i < Math.min(4, upcoming.size()); i++) {
            Game game = upcoming.get(i);
            View card = poster(game, i == 0 ? activity.getString(R.string.browse_choose_seats) : activity.getString(R.string.browse_view_detail),
                    i == 0 ? () -> { capture(); seats.accept(game); } : () -> detail(game));
            card.setOnClickListener(v -> detail(game));
            (i == 0 ? feature : list).addView(card);
        }
        if (upcoming.isEmpty()) {
            TextView empty = new TextView(activity); empty.setText(R.string.no_matching_games); empty.setTextColor(activity.getColor(R.color.browse_text)); feature.addView(empty);
        }
    }
    private View poster(Game game, String action, Runnable open) {
        View card = activity.getLayoutInflater().inflate(R.layout.match_card, null);
        MatchPoster.bind(activity, card, game, catalog, loader, action, open);
        return card;
    }
    private void renderDetail(Game game) {
        if (game == null) return;
        ArenaTheme home = MatchPoster.team(catalog, game.homeTeamId);
        activity.findViewById(R.id.browse_detail).setBackgroundColor(ColorUtils.blendARGB(home.primaryColor, 0xff0c111a, .82f));
        LinearLayout holder = activity.findViewById(R.id.detail_poster); holder.removeAllViews(); holder.addView(poster(game, null, () -> { }));
        ((TextView) activity.findViewById(R.id.detail_price)).setText(MatchPoster.price(activity, loader, game));
        activity.findViewById(R.id.detail_choose_seats).setEnabled(game.startsAtMillis > System.currentTimeMillis());
    }
    private void populateTeams(int id) {
        LinearLayout holder = activity.findViewById(id);
        List<ArenaTheme> teams = new ArrayList<>(catalog.themes); teams.sort(Comparator.comparing(t -> t.name));
        for (int i = 0; i < teams.size(); i += 2) {
            LinearLayout row = new LinearLayout(activity); row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = i; j < Math.min(i + 2, teams.size()); j++) {
                ArenaTheme team = teams.get(j);
                TextView tile = new TextView(activity); tile.setText(team.name); tile.setTextSize(14);
                tile.setTextColor(activity.getColor(R.color.browse_text)); tile.setTypeface(Typeface.DEFAULT_BOLD);
                tile.setGravity(Gravity.CENTER); tile.setPadding(dp(8), dp(12), dp(8), dp(12));
                tile.setCompoundDrawablesRelativeWithIntrinsicBounds(null, MatchPoster.badge(activity, loader, team, 56), null, null);
                tile.setCompoundDrawablePadding(dp(8)); tile.setBackgroundColor(activity.getColor(R.color.browse_surface));
                tile.setTag(team.teamId); tile.setContentDescription(activity.getString(R.string.browse_team_schedule, team.name));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1); params.setMargins(dp(4), dp(4), dp(4), dp(4)); row.addView(tile, params);
                tile.setOnClickListener(v -> { capture(); model.matchList.selectTeam(team.teamId); model.matchListScroll = 0; model.navigation.tab(SCHEDULE); render(); });
            }
            holder.addView(row);
        }
    }
    private Game game(String id) { for (Game game : games) if (game.id.equals(id)) return game; return null; }
    private void visible(int id, boolean show) { activity.findViewById(id).setVisibility(show ? View.VISIBLE : View.GONE); }
    private void restore(int id, int y) {
        ScrollView scroll = activity.findViewById(id);
        BrowseNavigation.Page expected = model.navigation.page;
        // Wait until newly attached poster/card children have a measured scroll range.
        scroll.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                if (scroll.getViewTreeObserver().isAlive()) scroll.getViewTreeObserver().removeOnPreDrawListener(this);
                if (model.navigation.page == expected) scroll.scrollTo(0, y);
                return true;
            }
        });
    }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
}
