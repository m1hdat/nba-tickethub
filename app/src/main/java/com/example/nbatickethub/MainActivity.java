package com.example.nbatickethub;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.example.nbatickethub.data.AndroidAssetSource;
import com.example.nbatickethub.data.ArenaCatalog;
import com.example.nbatickethub.model.ArenaProfile;
import com.example.nbatickethub.model.ArenaTheme;
import com.example.nbatickethub.ui.arena.ArenaRenderData;
import com.example.nbatickethub.ui.arena.ArenaPreviewLoader;
import com.example.nbatickethub.ui.arena.ArenaView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private com.example.nbatickethub.state.ArenaSelectionViewModel model;
    private com.example.nbatickethub.ui.arena.SeatSheetController sheet;
    private com.example.nbatickethub.ui.arena.ArenaFilterController filters;
    private static final String STATE_ARENA = "previewArenaId";
    private static final String STATE_THEME = "previewThemeOverride";
    private ArenaPreviewLoader loader;
    private com.example.nbatickethub.ui.arena.MatchPickerController matchPicker;
    private com.example.nbatickethub.ui.arena.BrowseController browse;
    private com.example.nbatickethub.ui.arena.BookingController booking;
    private com.example.nbatickethub.ui.arena.SeatFinderController finder;
    private ArenaView arenaView;
    private TextView title, subtitle, status;
    private String selectedId;
    private String themeOverride;
    private ArenaCatalog arenaCatalog;
    private ArenaRenderData currentRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = new androidx.lifecycle.ViewModelProvider(this).get(com.example.nbatickethub.state.ArenaSelectionViewModel.class);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        arenaView = findViewById(R.id.arena_view);
        sheet = new com.example.nbatickethub.ui.arena.SeatSheetController(this, model, arenaView);
        finder = new com.example.nbatickethub.ui.arena.SeatFinderController(this, model, arenaView, sheet);
        filters = new com.example.nbatickethub.ui.arena.ArenaFilterController(this, model);
        arenaView.setSectionListener(id -> {
            if (currentRender == null) { return; }
            model.selection.browseSection(id); model.changed(); sheet.expand();
        });
        arenaView.setSeatInspectionListener((seat, row, section) -> {
            if (currentRender == null || seat == null) { return; }
            model.selection.preview(seat.id); model.changed(); sheet.expand();
        });
        findViewById(R.id.show_games).setOnClickListener(view -> showGames());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (browse != null && browse.back()) { return; }
                setEnabled(false); getOnBackPressedDispatcher().onBackPressed();
            }
        });
        model.changes().observe(this, revision -> renderSelection());
        findViewById(R.id.reset_view).setOnClickListener(view -> arenaView.fitArena());
        title = findViewById(R.id.venue_title);
        subtitle = findViewById(R.id.venue_subtitle);
        status = findViewById(R.id.preview_status);
        loader = new ArenaPreviewLoader(new AndroidAssetSource(getApplicationContext().getAssets()), getApplicationContext());
        loader.catalog((catalog, error) -> {
            if (error != null) { showFailure(error); return; }
            arenaCatalog = catalog;
            if (model.sampleGames == null) model.sampleGames = com.example.nbatickethub.data.mock.MockMatchSchedule.games(catalog);
            games = model.sampleGames;
            matchPicker = new com.example.nbatickethub.ui.arena.MatchPickerController(this, model, catalog, loader, games, game -> browse.detail(game));
            com.example.nbatickethub.state.BrowseNavigation.Page restoredPage = model.navigation.page;
            browse = new com.example.nbatickethub.ui.arena.BrowseController(this, model, catalog, loader, games,
                    this::selectMatch, this::captureCamera, () -> matchPicker.refresh());
            if (getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) matchPicker.resume();
            if (model.matchId != null) {
                for (com.example.nbatickethub.model.Game game : games) {
                    if (game.id.equals(model.matchId)) { selectMatch(game); break; }
                }
            }
            model.navigation.page = restoredPage;
            booking = new com.example.nbatickethub.ui.arena.BookingController(this, model, catalog,
                    () -> { if (currentGame != null) { com.example.nbatickethub.model.Game game = currentGame; currentGame = null; selectMatch(game); } },
                    () -> browse.tab(com.example.nbatickethub.state.BrowseNavigation.Page.TICKETS));
            browse.setTicketsAction(() -> booking.tickets());
            browse.render();
        });
    }

    private java.util.List<com.example.nbatickethub.model.Game> games;
    private com.example.nbatickethub.model.Game currentGame;
    private void showGames() {
        if (browse != null) browse.tab(com.example.nbatickethub.state.BrowseNavigation.Page.SCHEDULE);
    }

    private void captureCamera() {
        if (currentRender != null && arenaView.cameraSnapshot() != null) {
            model.camera = arenaView.cameraSnapshot(); model.cameraArenaId = selectedId;
        }
    }

    private void selectMatch(com.example.nbatickethub.model.Game game) {
        boolean sameArena = game.arenaId.equals(selectedId);
        if (sameArena && arenaView.cameraSnapshot() != null) {
            model.camera = arenaView.cameraSnapshot(); model.cameraArenaId = selectedId;
        } else if (selectedId != null) { model.camera = null; model.cameraArenaId = null; }
        model.navigation.arena();
        model.choosingGame = false;
        findViewById(R.id.browse_shell).setVisibility(View.GONE);
        findViewById(R.id.game_picker).setVisibility(View.GONE);
        findViewById(R.id.arena_screen).setVisibility(View.VISIBLE);
        if (currentGame != null && currentGame.id.equals(game.id) && currentRender != null) { return; }
        currentGame = game; model.matchId = game.id;
        selectedId = game.arenaId; model.arenaId = selectedId;
        ArenaProfile profile = arenaCatalog.byId.get(selectedId);
        status.setText(R.string.preview_loading);
        findViewById(R.id.booking_continue).setEnabled(false);
        currentRender = null; filters.setData(null); sheet.setData(null, null); arenaView.setRenderData(null);
        findViewById(R.id.reset_view).setEnabled(false);
        loader.arena(profile, game, arenaCatalog, (data, error) -> {
            if (error != null) { showFailure(error); return; }
            currentRender = data; model.selection.bind(data.dataset(), game);
            arenaView.setRenderData(data); applyTheme();
            if (profile.id.equals(model.cameraArenaId) && model.camera != null) { arenaView.restoreCamera(model.camera); }
            filters.setData(data); sheet.setData(data, loader.court(arenaView.displayedThemeId()));
            renderSelection(); findViewById(R.id.reset_view).setEnabled(true);
            if (model.finderOpen) finder.show();
            arenaView.setContentDescription(getString(R.string.arena_description, profile.venueName, data.sectionCount(), data.seatCount()));
        });
    }

    private void applyTheme() {
        if (arenaCatalog == null || selectedId == null) { return; }
        ArenaTheme theme = currentRender == null
                ? arenaCatalog.resolveTheme(arenaCatalog.byId.get(selectedId), null)
                : com.example.nbatickethub.state.GameThemeResolver.resolve(arenaCatalog, currentRender.previewGame());
        model.themeId = themeOverride;
        int ink = androidx.core.graphics.ColorUtils.calculateLuminance(theme.primaryColor) > .179
                ? android.graphics.Color.BLACK : android.graphics.Color.WHITE;
        findViewById(R.id.arena_header).setBackgroundColor(theme.primaryColor);
        title.setTextColor(ink); subtitle.setTextColor(ink);
        ((android.widget.Button) findViewById(R.id.show_games)).setTextColor(ink);
        findViewById(R.id.show_games).setBackgroundTintList(android.content.res.ColorStateList.valueOf(theme.primaryColor));
        findViewById(R.id.reset_view).setBackgroundTintList(android.content.res.ColorStateList.valueOf(theme.primaryColor));
        ((android.widget.Button) findViewById(R.id.reset_view)).setTextColor(ink);
        sheet.setAccent(theme.primaryColor, ink);
        finder.setAccent(theme.primaryColor);
        findViewById(R.id.suggest_seats).setBackgroundTintList(android.content.res.ColorStateList.valueOf(theme.primaryColor));
        ((android.widget.Button) findViewById(R.id.suggest_seats)).setTextColor(ink);
        findViewById(R.id.booking_continue).setBackgroundTintList(android.content.res.ColorStateList.valueOf(theme.primaryColor));
        ((android.widget.Button) findViewById(R.id.booking_continue)).setTextColor(ink);
        arenaView.setArenaTheme(theme, loader.court(theme.id));
        arenaView.setBackground(com.example.nbatickethub.ui.arena.TeamAppearance.background(theme, true));
        findViewById(R.id.arena_screen).setBackground(com.example.nbatickethub.ui.arena.TeamAppearance.background(theme, true));
        findViewById(R.id.arena_header).setBackground(com.example.nbatickethub.ui.arena.TeamAppearance.background(theme, false));
        title.setTypeface(com.example.nbatickethub.ui.arena.TeamAppearance.heading(currentGame.homeTeamId));
        title.setText(getString(R.string.match_title, currentGame.awayTeamId.toUpperCase(java.util.Locale.ROOT), currentGame.homeTeamId.toUpperCase(java.util.Locale.ROOT)));
        subtitle.setText(currentRender == null ? arenaCatalog.byId.get(selectedId).venueName
                : getString(R.string.game_context, arenaCatalog.byId.get(selectedId).venueName, com.example.nbatickethub.state.MatchListState.display(currentGame.startsAtMillis, "dd/MM/yyyy · HH:mm (z)", java.util.TimeZone.getDefault())));
        android.text.SpannableStringBuilder legend = new android.text.SpannableStringBuilder();
        if (currentRender != null) {
            for (int i = 0; i < currentRender.levelCount(); i++) {
                if (i > 0) { legend.append("   "); }
                int start = legend.length();
                legend.append(getString(R.string.tier_legend_item, currentRender.levelName(i)));
                legend.setSpan(new android.text.style.ForegroundColorSpan(theme.seatTier(currentRender.levelOrder(i)).accent),
                        start, start + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        ((TextView) findViewById(R.id.tier_legend)).setText(legend);
        android.text.SpannableStringBuilder statusLegend = new android.text.SpannableStringBuilder();
        com.example.nbatickethub.model.SeatStatus[] statuses = com.example.nbatickethub.model.SeatStatus.values();
        int[] labels = {R.string.status_available, R.string.status_booked, R.string.status_locked};
        for (int i = 0; i < statuses.length; i++) {
            if (i > 0) { statusLegend.append("   "); }
            int start = statusLegend.length();
            statusLegend.append(getString(labels[i]));
            statusLegend.setSpan(new android.text.style.ForegroundColorSpan(theme.statuses.color(statuses[i])),
                    start, start + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int selectedStart = statusLegend.length();
        statusLegend.append("   ").append(getString(R.string.status_selected));
        statusLegend.setSpan(new android.text.style.ForegroundColorSpan(getColor(R.color.seat_selected)),
                selectedStart, statusLegend.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(R.id.status_legend)).setText(statusLegend);
    }

    private void renderSelection() {
        if (currentRender == null) { return; }
        if (booking != null) booking.selectionChanged();
        filters.refresh();
        com.example.nbatickethub.state.ArenaSelection state = model.selection;
        arenaView.setFilter(state.levelId(), state.sectionId());
        arenaView.setSelection(state.selectedIds(), state.candidateId() == null ? state.focusedId() : state.candidateId());
        arenaView.setTicketFilter(state);
        arenaView.highlightSection(state.browsingSection());
        sheet.render();
        status.setText(state.replacingId() != null ? getString(R.string.changing_hint)
                : getString(R.string.section_tap_hint));
    }

    private void showFailure(Exception error) {
        android.util.Log.e("ArenaPreview", "Cannot load preview", error);
        status.setText(R.string.preview_error);
        selectedId = null;
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        finder.saveState();
        float[] snapshot = arenaView.cameraSnapshot();
        if (snapshot != null) { model.camera = snapshot; model.cameraArenaId = selectedId; }
        model.arenaId = selectedId; model.themeId = themeOverride;
        super.onSaveInstanceState(state);
        state.putString(STATE_ARENA, selectedId);
        state.putString(STATE_THEME, themeOverride);
    }

    @Override protected void onResume() {
        super.onResume();
        if (matchPicker != null) matchPicker.resume();
        if (currentRender != null) applyTheme();
        if (browse != null) browse.render();
    }

    @Override protected void onPause() {
        if (browse != null) browse.capture();
        if (matchPicker != null) matchPicker.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (matchPicker != null) matchPicker.close();
        if (booking != null) booking.close();
        if (finder != null) finder.close();
        if (sheet != null) { sheet.close(); }
        if (loader != null) { loader.close(); }
        super.onDestroy();
    }
}
