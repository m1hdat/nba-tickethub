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
    private static final String STATE_ARENA = "previewArenaId";
    private static final String STATE_THEME = "previewThemeOverride";
    private ArenaPreviewLoader loader;
    private ArenaView arenaView;
    private TextView title, subtitle, status;
    private String selectedId;
    private String themeOverride;
    private ArenaCatalog arenaCatalog;
    private ArenaRenderData currentRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        arenaView = findViewById(R.id.arena_view);
        arenaView.setSeatInspectionListener((seat, row, section) -> {
            TextView debug = findViewById(R.id.seat_debug);
            debug.setText(seat == null ? getString(R.string.seat_debug_empty)
                    : getString(R.string.seat_debug_hit, section.code, row.rowLabel, seat.positionNumber));
        });
        findViewById(R.id.reset_view).setOnClickListener(view -> arenaView.fitArena());
        title = findViewById(R.id.venue_title);
        subtitle = findViewById(R.id.venue_subtitle);
        status = findViewById(R.id.preview_status);
        Spinner selector = findViewById(R.id.arena_selector);
        String restoredId = savedInstanceState == null ? null : savedInstanceState.getString(STATE_ARENA);
        themeOverride = savedInstanceState == null ? null : savedInstanceState.getString(STATE_THEME);
        loader = new ArenaPreviewLoader(new AndroidAssetSource(getApplicationContext().getAssets()));
        loader.catalog((catalog, error) -> {
            if (error != null) { showFailure(error); return; }
            arenaCatalog = catalog;
            setupThemes();
            ArrayAdapter<ArenaProfile> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, catalog.profiles);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            selector.setAdapter(adapter);
            int selected = 0;
            for (int i = 0; i < catalog.profiles.size(); i++) {
                if (catalog.profiles.get(i).id.equals(restoredId)) { selected = i; }
            }
            selector.setSelection(selected);
            selector.setEnabled(true);
            selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    ArenaProfile selected = catalog.profiles.get(position);
                    if (selected.id.equals(selectedId)) { return; }
                    themeOverride = null;
                    ((Spinner) findViewById(R.id.theme_selector)).setSelection(0);
                    loadArena(selected);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) { }
            });
            loadArena(catalog.profiles.get(selected));
        });
    }

    private void loadArena(ArenaProfile profile) {
        if (profile.id.equals(selectedId)) { return; }
        selectedId = profile.id;
        title.setText(profile.venueName);
        subtitle.setText(profile.homeTeam.displayName);
        status.setText(R.string.preview_loading);
        ((TextView) findViewById(R.id.tier_legend)).setText("");
        arenaView.setRenderData(null);
        findViewById(R.id.reset_view).setEnabled(false);
        currentRender = null;
        applyTheme();
        loader.arena(profile, (data, error) -> {
            if (error != null) { showFailure(error); return; }
            currentRender = data;
            applyTheme();
            arenaView.setRenderData(data);
            findViewById(R.id.reset_view).setEnabled(true);
            arenaView.setContentDescription(getString(R.string.arena_description, profile.venueName,
                    data.sectionCount(), data.seatCount()));
            status.setText(getString(R.string.preview_counts, data.sectionCount(), data.seatCount()));
        });
    }

    private void setupThemes() {
        Spinner themes = findViewById(R.id.theme_selector);
        java.util.List<String> names = new java.util.ArrayList<>();
        names.add(getString(R.string.theme_home_team));
        int restored = 0;
        for (int i = 0; i < arenaCatalog.themes.size(); i++) {
            ArenaTheme theme = arenaCatalog.themes.get(i);
            names.add(theme.name);
            if (theme.id.equals(themeOverride)) { restored = i + 1; }
        }
        if (restored == 0) { themeOverride = null; }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themes.setAdapter(adapter);
        themes.setSelection(restored);
        themes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                themeOverride = position == 0 ? null : arenaCatalog.themes.get(position - 1).id;
                if (themeOverride != null) {
                    ArenaTheme team = arenaCatalog.themesById.get(themeOverride);
                    ArenaProfile home = arenaCatalog.homeArena(team.teamId);
                    if (!home.id.equals(selectedId)) {
                        loadArena(home);
                        ((Spinner) findViewById(R.id.arena_selector)).setSelection(arenaCatalog.profiles.indexOf(home));
                    } else { applyTheme(); }
                } else { applyTheme(); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        themes.setEnabled(true);
    }

    private void applyTheme() {
        if (arenaCatalog == null || selectedId == null) { return; }
        ArenaTheme theme = arenaCatalog.resolveTheme(arenaCatalog.byId.get(selectedId), themeOverride);
        arenaView.setArenaTheme(theme, loader.court(theme.id));
        title.setText(theme.name);
        subtitle.setText(arenaCatalog.byId.get(selectedId).venueName);
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
        ((TextView) findViewById(R.id.status_legend)).setText(statusLegend);
    }

    private void showFailure(Exception error) {
        android.util.Log.e("ArenaPreview", "Cannot load preview", error);
        status.setText(R.string.preview_error);
        selectedId = null;
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(STATE_ARENA, selectedId);
        state.putString(STATE_THEME, themeOverride);
    }

    @Override protected void onDestroy() {
        if (loader != null) { loader.close(); }
        super.onDestroy();
    }
}
