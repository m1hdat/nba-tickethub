package com.example.nbatickethub.ui.arena;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.example.nbatickethub.R;
import com.example.nbatickethub.data.ArenaCatalog;
import com.example.nbatickethub.model.*;
import com.example.nbatickethub.state.*;
import java.util.*;
import java.util.function.Consumer;

/** Match UI only. Filtering is pure Java; arena/seat state remains in the existing ViewModel. */
public final class MatchPickerController {
    private final Activity activity;
    private final ArenaSelectionViewModel model;
    private final ArenaCatalog catalog;
    private final ArenaPreviewLoader loader;
    private final List<Game> games;
    private final Consumer<Game> open;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = () -> refresh();
    private boolean resumed;
    private AlertDialog teamDialog;
    private DatePickerDialog dateDialog;

    public MatchPickerController(Activity activity, ArenaSelectionViewModel model, ArenaCatalog catalog,
                                 ArenaPreviewLoader loader, List<Game> games, Consumer<Game> open) {
        this.activity = activity; this.model = model; this.catalog = catalog;
        this.loader = loader; this.games = games; this.open = open;
        for (int id : new int[]{R.id.match_seven_days, R.id.match_today}) {
            Button chip = activity.findViewById(id);
            chip.setBackgroundTintList(new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_selected},{}},
                    new int[]{activity.getColor(R.color.preview_accent),activity.getColor(R.color.preview_surface)}));
            chip.setTextColor(new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_selected},{}},
                    new int[]{0xffffffff,activity.getColor(R.color.preview_text)}));
        }
        activity.findViewById(R.id.match_seven_days).setOnClickListener(v -> { model.matchList.sevenDays(); refresh(); });
        activity.findViewById(R.id.match_today).setOnClickListener(v -> { model.matchList.today(); refresh(); });
        activity.findViewById(R.id.match_clear).setOnClickListener(v -> { model.matchList.clear(); refresh(); });
        activity.findViewById(R.id.match_date).setOnClickListener(v -> pickDate());
        activity.findViewById(R.id.match_team).setOnClickListener(v -> pickTeam());
        ((CheckBox) activity.findViewById(R.id.match_home_only)).setOnCheckedChangeListener((button, checked) -> {
            if (model.matchList.homeOnly != checked) { model.matchList.homeOnly = checked; refresh(); }
        });
    }

    public void resume() { resumed = true; refresh(); }
    public void pause() { resumed = false; handler.removeCallbacks(ticker); }
    public void close() { pause(); if (teamDialog != null) teamDialog.dismiss(); if (dateDialog != null) dateDialog.dismiss(); }

    public void refresh() {
        handler.removeCallbacks(ticker);
        if (resumed) { handler.postDelayed(ticker, nextRefresh()); }
        if (model.navigation.page != BrowseNavigation.Page.SCHEDULE) { return; }
        ScrollView scroll = activity.findViewById(R.id.game_picker);
        int scrollY = scroll.getScrollY();
        MatchListState state = model.matchList;
        TimeZone zone = TimeZone.getDefault(); long now = System.currentTimeMillis();
        ArenaTheme selectedTeam = theme(state.teamId);
        Button team = activity.findViewById(R.id.match_team);
        team.setText(selectedTeam == null ? activity.getString(R.string.all_teams) : selectedTeam.name);
        team.setCompoundDrawablesRelativeWithIntrinsicBounds(selectedTeam == null ? null : badge(selectedTeam, 32), null, null, null);
        CheckBox home = activity.findViewById(R.id.match_home_only);
        home.setVisibility(state.teamId == null ? View.GONE : View.VISIBLE); home.setChecked(state.homeOnly);
        activity.findViewById(R.id.match_seven_days).setSelected(state.range == MatchListState.Range.NEXT_SEVEN);
        activity.findViewById(R.id.match_today).setSelected(state.range == MatchListState.Range.TODAY);
        String period = state.range == MatchListState.Range.NEXT_SEVEN ? activity.getString(R.string.next_seven_days)
                : state.range == MatchListState.Range.TODAY ? activity.getString(R.string.match_today)
                : state.range == MatchListState.Range.DAY ? state.selectedDay : activity.getString(R.string.all_upcoming);
        ((Button) activity.findViewById(R.id.match_date)).setText(state.range == MatchListState.Range.DAY ? state.selectedDay : activity.getString(R.string.pick_match_day));
        ((TextView) activity.findViewById(R.id.match_filter_summary)).setText(activity.getString(R.string.match_filter_summary,
                period, team.getText(), state.homeOnly ? activity.getString(R.string.home_only) : activity.getString(R.string.home_away), zone.getID()));
        LinearLayout cards = activity.findViewById(R.id.game_cards); cards.removeAllViews();
        List<Game> visible = state.filter(games, now, zone);
        activity.findViewById(R.id.match_empty).setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        String lastDay = null;
        for (Game game : visible) {
            String day = MatchListState.dayKey(game.startsAtMillis, zone);
            if (!day.equals(lastDay)) {
                TextView group = new TextView(activity);
                group.setText(MatchListState.display(game.startsAtMillis, "EEEE, dd/MM/yyyy", zone));
                group.setTextColor(activity.getColor(R.color.browse_text)); group.setTextSize(16);
                group.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); group.setPadding(0, dp(16), 0, dp(8));
                cards.addView(group); lastDay = day;
            }
            cards.addView(card(game, zone));
        }
        // Restore at the same layout boundary as BrowseController. A posted old offset
        // can otherwise run after navigation restores the retained calendar offset.
        scroll.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                if (scroll.getViewTreeObserver().isAlive()) scroll.getViewTreeObserver().removeOnPreDrawListener(this);
                if (model.navigation.page == BrowseNavigation.Page.SCHEDULE) scroll.scrollTo(0, scrollY);
                return true;
            }
        });
    }

    private long nextRefresh() {
        long now = System.currentTimeMillis(), delay = 60000;
        for (Game game : games) if (game.startsAtMillis > now) delay = Math.min(delay, game.startsAtMillis - now + 1);
        return Math.max(1, delay);
    }

    private View card(Game game, TimeZone zone) {
        View card = activity.getLayoutInflater().inflate(R.layout.match_card, null);
        card.setTag(game.id);
        Runnable detail = () -> {
            model.matchListScroll = ((ScrollView) activity.findViewById(R.id.game_picker)).getScrollY(); open.accept(game);
        };
        MatchPoster.bind(activity, card, game, catalog, loader, activity.getString(R.string.browse_view_detail), detail);
        card.setOnClickListener(v -> detail.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(12); card.setLayoutParams(params);
        return card;
    }

    private void text(View parent, int id, String value, int color) { TextView view = parent.findViewById(id); view.setText(value); view.setTextColor(color); }
    private ArenaTheme theme(String id) { for (ArenaTheme theme : catalog.themes) if (theme.teamId.equals(id)) return theme; return null; }
    private TeamBadgeDrawable badge(ArenaTheme theme, int size) {
        return new TeamBadgeDrawable(loader.teamLogo(theme), theme.teamId.toUpperCase(Locale.ROOT), theme.primaryColor, dp(size));
    }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }

    private void pickDate() {
        Calendar value = Calendar.getInstance();
        if (model.matchList.selectedDay != null) {
            String[] parts = model.matchList.selectedDay.split("-");
            value.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        }
        dateDialog = new DatePickerDialog(activity, (picker, year, month, day) -> {
            model.matchList.selectDay(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)); refresh();
        }, value.get(Calendar.YEAR), value.get(Calendar.MONTH), value.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    private void pickTeam() {
        List<ArenaTheme> teams = new ArrayList<>(catalog.themes); teams.sort(Comparator.comparing(t -> t.name)); teams.add(0, null);
        ArrayAdapter<ArenaTheme> adapter = new ArrayAdapter<ArenaTheme>(activity, android.R.layout.simple_list_item_1, teams) {
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                ArenaTheme theme = getItem(position);
                LinearLayout row = new LinearLayout(activity); row.setGravity(Gravity.CENTER_VERTICAL); row.setMinimumHeight(dp(48)); row.setPadding(dp(16), dp(8), dp(16), dp(8));
                if (theme != null) { ImageView image = new ImageView(activity); image.setImageDrawable(badge(theme, 36)); row.addView(image, new LinearLayout.LayoutParams(dp(36), dp(36))); }
                TextView label = new TextView(activity); label.setText(theme == null ? activity.getString(R.string.all_teams) : theme.name);
                label.setTextColor(activity.getColor(R.color.preview_text)); label.setTextSize(16); label.setPadding(dp(12), 0, 0, 0); row.addView(label);
                return row;
            }
        };
        teamDialog = new AlertDialog.Builder(activity).setTitle(R.string.pick_team).setAdapter(adapter, (dialog, which) -> {
            ArenaTheme selected = teams.get(which); model.matchList.selectTeam(selected == null ? null : selected.teamId); refresh();
        }).setNegativeButton(R.string.cancel_change, null).create(); teamDialog.show();
    }
}
